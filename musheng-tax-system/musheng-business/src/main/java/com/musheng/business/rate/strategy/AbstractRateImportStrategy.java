package com.musheng.business.rate.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.strategy.FileImportStrategy;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.mapper.CurrencyMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 汇率导入策略抽象基类
 * 
 * 提取 CSV 和 Excel 导入的公共逻辑，包括：
 * - 获取已配置的货币代码
 * - 批量检查重复并插入
 * - 解析日期
 * - 判断是否周末
 * - 查找列索引
 * 
 * ⚠️ 重要：所有方法必须与原 RateServiceImpl 中的逻辑完全一致，
 * 以确保重构不改变任何业务输出。
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
public abstract class AbstractRateImportStrategy implements FileImportStrategy<ExchangeRate> {
    
    protected final CurrencyMapper currencyMapper;
    protected final ExchangeRateMapper exchangeRateMapper;
    
    /**
     * 构造函数
     * 
     * @param currencyMapper 货币 Mapper
     * @param exchangeRateMapper 汇率 Mapper
     */
    protected AbstractRateImportStrategy(CurrencyMapper currencyMapper, 
                                         ExchangeRateMapper exchangeRateMapper) {
        this.currencyMapper = currencyMapper;
        this.exchangeRateMapper = exchangeRateMapper;
    }
    
    /**
     * 获取已配置的货币代码集合（状态为启用，排除CNY）
     * CNY 是基准货币，所有汇率都是外币兑换CNY，CNY本身不应该有汇率记录
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.getConfiguredCurrencyCodes() 完全一致
     * 
     * @return 已配置的货币代码集合
     */
    protected Set<String> getConfiguredCurrencyCodes() {
        LambdaQueryWrapper<Currency> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Currency::getStatus, 1)  // 只取启用状态的货币
               .ne(Currency::getCurrencyCode, "CNY");  // 排除CNY
        List<Currency> currencies = currencyMapper.selectList(wrapper);
        return currencies.stream()
                .map(Currency::getCurrencyCode)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
    
    /**
     * 批量检查重复并插入新数据
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.batchCheckAndInsert() 完全一致
     * 
     * @param ratesToImport 待导入的汇率列表
     * @param existsCount 已存在计数器
     * @return [成功插入数量]
     */
    protected int[] batchCheckAndInsert(List<ExchangeRate> ratesToImport, AtomicInteger existsCount) {
        if (ratesToImport.isEmpty()) {
            return new int[]{0};
        }
        
        // 提取所有日期和货币
        Set<LocalDate> dates = ratesToImport.stream()
                .map(ExchangeRate::getRateDate)
                .collect(Collectors.toSet());
        Set<String> currencies = ratesToImport.stream()
                .map(ExchangeRate::getCurrencyCode)
                .collect(Collectors.toSet());

        // 一次性查询所有可能存在的记录
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ExchangeRate::getRateDate, dates)
                .in(ExchangeRate::getCurrencyCode, currencies);
        List<ExchangeRate> existingRates = exchangeRateMapper.selectList(wrapper);

        // 构建已存在的key集合
        Set<String> existingKeys = existingRates.stream()
                .map(r -> r.getRateDate() + "_" + r.getCurrencyCode())
                .collect(Collectors.toSet());

        // 过滤出不存在的数据
        List<ExchangeRate> newRates = ratesToImport.stream()
                .filter(r -> {
                    String key = r.getRateDate() + "_" + r.getCurrencyCode();
                    if (existingKeys.contains(key)) {
                        existsCount.getAndIncrement();
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 批量插入新数据
        int successCount = 0;
        if (!newRates.isEmpty()) {
            int batchSize = 1000;
            for (int i = 0; i < newRates.size(); i += batchSize) {
                int end = Math.min(i + batchSize, newRates.size());
                List<ExchangeRate> batch = newRates.subList(i, end);
                for (ExchangeRate rate : batch) {
                    exchangeRateMapper.insert(rate);
                }
            }
            successCount = newRates.size();
            log.info("Batch inserted {} exchange rates", successCount);
        }
        
        return new int[]{successCount};
    }
    
    /**
     * 解析汇率日期，支持多种格式
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.parseRateDate() 完全一致
     * 
     * @param dateStr 日期字符串
     * @return 解析后的 LocalDate
     * @throws IllegalArgumentException 如果无法解析日期
     */
    protected LocalDate parseRateDate(String dateStr) {
        // Try common date formats
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Cannot parse date: " + dateStr);
    }
    
    /**
     * 判断日期是否为周末
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.isWeekend() 完全一致
     * 
     * @param date 日期
     * @return 是周末返回 true，否则返回 false
     */
    protected boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
    
    /**
     * 根据可能的列名查找列索引
     * 
     * 匹配逻辑：
     * 1. 优先精确匹配（header == name）
     * 2. 其次包含匹配（header contains name），但排除已被更精确匹配的列
     * 
     * @param headers 表头列表
     * @param possibleNames 可能的列名
     * @return 列索引，未找到返回 -1
     */
    protected int findColumnIndex(List<String> headers, String... possibleNames) {
        // 第一轮：精确匹配
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            for (String name : possibleNames) {
                if (header.equals(name.toLowerCase())) {
                    return i;
                }
            }
        }
        
        // 第二轮：包含匹配（用于中文列名等场景）
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            for (String name : possibleNames) {
                // 避免 "rate" 匹配到 "rate_date"：检查是否是完整单词
                String lowerName = name.toLowerCase();
                if (header.contains(lowerName)) {
                    // 如果 header 比 name 长，检查是否是独立单词
                    if (header.length() == lowerName.length()) {
                        return i;
                    }
                    // 检查 name 前后是否有分隔符（下划线、空格等）
                    int idx = header.indexOf(lowerName);
                    boolean startOk = idx == 0 || !Character.isLetterOrDigit(header.charAt(idx - 1));
                    boolean endOk = idx + lowerName.length() == header.length() 
                            || !Character.isLetterOrDigit(header.charAt(idx + lowerName.length()));
                    if (startOk && endOk) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
