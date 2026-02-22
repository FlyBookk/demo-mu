package com.musheng.business.rate.strategy;

import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.currency.mapper.CurrencyMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSV 汇率导入策略
 * 
 * 处理 CSV 格式的汇率文件导入。
 * 
 * CSV 文件格式要求：
 * - 必须包含表头行
 * - 必须包含以下列：rate_date/日期/date, currency_code/货币/currency, rate/汇率/exchange_rate
 * 
 * ⚠️ 重要：所有逻辑必须与原 RateServiceImpl.importCsvData() 完全一致，
 * 以确保重构不改变任何业务输出。
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Component
public class RateCsvImportStrategy extends AbstractRateImportStrategy {
    
    /**
     * 构造函数
     * 
     * @param currencyMapper 货币 Mapper
     * @param exchangeRateMapper 汇率 Mapper
     */
    public RateCsvImportStrategy(CurrencyMapper currencyMapper, 
                                  ExchangeRateMapper exchangeRateMapper) {
        super(currencyMapper, exchangeRateMapper);
    }
    
    /**
     * 判断是否支持该文件类型
     * 
     * @param fileName 文件名
     * @return 支持 .csv 文件返回 true
     */
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }
    
    /**
     * 解析 CSV 文件内容
     * 
     * @param file 上传的文件
     * @param context 导入上下文
     * @return 解析后的汇率列表
     * @throws IOException 文件读取异常
     */
    @Override
    public List<ExchangeRate> parse(MultipartFile file, ImportContext context) throws IOException {
        List<ExchangeRate> rates = new ArrayList<>();
        Set<String> configuredCurrencies = getConfiguredCurrencyCodes();
        Set<String> uniqueKeys = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);
            List<String> headers = parser.getHeaderNames();

            // Detect column indices
            int dateIdx = findColumnIndex(headers, "rate_date", "日期", "date");
            int currencyIdx = findColumnIndex(headers, "currency_code", "货币", "currency");
            int rateIdx = findColumnIndex(headers, "rate", "汇率", "exchange_rate");

            if (dateIdx < 0 || currencyIdx < 0 || rateIdx < 0) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND,
                        "Required columns not found: rate_date, currency_code, rate");
            }

            for (CSVRecord record : parser) {
                try {
                    String dateStr = record.get(dateIdx).trim();
                    String currency = record.get(currencyIdx).trim().toUpperCase();
                    String rateStr = record.get(rateIdx).trim();

                    // 检查货币是否已配置（未配置则跳过）
                    if (!configuredCurrencies.contains(currency)) {
                        continue;
                    }

                    // Parse date
                    LocalDate rateDate = parseRateDate(dateStr);

                    // Parse rate
                    BigDecimal rate = new BigDecimal(rateStr.replace(",", ""));

                    // 检查文件内重复
                    String uniqueKey = rateDate + "_" + currency;
                    if (uniqueKeys.contains(uniqueKey)) {
                        continue;
                    }
                    uniqueKeys.add(uniqueKey);

                    // 创建待导入对象
                    ExchangeRate exchangeRate = new ExchangeRate();
                    exchangeRate.setRateDate(rateDate);
                    exchangeRate.setCurrencyCode(currency);
                    exchangeRate.setRate(rate);
                    exchangeRate.setSource("IMPORT");

                    rates.add(exchangeRate);

                } catch (Exception e) {
                    log.warn("Failed to parse CSV record: {}", e.getMessage());
                }
            }
        }
        
        return rates;
    }
    
    /**
     * 执行 CSV 导入并保存
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.importCsvData() 完全一致
     * 
     * @param file 上传的文件
     * @param context 导入上下文
     * @return 导入结果
     */
    @Override
    public Map<String, Object> importAndSave(MultipartFile file, ImportContext context) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;  // 跳过未配置货币的数量
        AtomicInteger existsCount = new AtomicInteger();

        // 收集待导入的数据
        List<ExchangeRate> ratesToImport = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();  // 用于检测文件内重复
        Set<String> skippedCurrencies = new HashSet<>();  // 记录跳过的货币

        // 获取已配置的货币列表
        Set<String> configuredCurrencies = getConfiguredCurrencyCodes();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);
            List<String> headers = parser.getHeaderNames();

            // Detect column indices
            int dateIdx = findColumnIndex(headers, "rate_date", "日期", "date");
            int currencyIdx = findColumnIndex(headers, "currency_code", "货币", "currency");
            int rateIdx = findColumnIndex(headers, "rate", "汇率", "exchange_rate");

            if (dateIdx < 0 || currencyIdx < 0 || rateIdx < 0) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND,
                        "Required columns not found: rate_date, currency_code, rate");
            }

            int rowNum = 0;
            for (CSVRecord record : parser) {
                rowNum++;
                totalCount++;
                try {
                    String dateStr = record.get(dateIdx).trim();
                    String currency = record.get(currencyIdx).trim().toUpperCase();
                    String rateStr = record.get(rateIdx).trim();

                    // 检查货币是否已配置（未配置则跳过）
                    if (!configuredCurrencies.contains(currency)) {
                        skipCount++;
                        skippedCurrencies.add(currency);
                        continue;
                    }

                    // Parse date
                    LocalDate rateDate = parseRateDate(dateStr);

                    // Parse rate
                    BigDecimal rate = new BigDecimal(rateStr.replace(",", ""));

                    // 检查文件内重复
                    String uniqueKey = rateDate + "_" + currency;
                    if (uniqueKeys.contains(uniqueKey)) {
                        failCount++;
                        errors.add(String.format("Row %d: Duplicate date+currency in file: %s, %s",
                                rowNum, rateDate, currency));
                        continue;
                    }
                    uniqueKeys.add(uniqueKey);

                    // 创建待导入对象
                    ExchangeRate exchangeRate = new ExchangeRate();
                    exchangeRate.setRateDate(rateDate);
                    exchangeRate.setCurrencyCode(currency);
                    exchangeRate.setRate(rate);
                    exchangeRate.setSource("IMPORT");

                    ratesToImport.add(exchangeRate);

                } catch (Exception e) {
                    failCount++;
                    errors.add(String.format("Row %d: %s", rowNum, e.getMessage()));
                    log.warn("Failed to parse row {}: {}", rowNum, e.getMessage());
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to import CSV file", e);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Failed to parse file: " + e.getMessage());
        }

        // 执行批量去重和插入
        int[] insertResult = batchCheckAndInsert(ratesToImport, existsCount);
        successCount = insertResult[0];

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("existsCount", existsCount);
        result.put("failCount", failCount);
        result.put("skipCount", skipCount);
        result.put("skippedCurrencies", skippedCurrencies);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);

        log.info("CSV import completed: total={}, success={}, exists={}, fail={}, skip={}, skippedCurrencies={}",
                totalCount, successCount, existsCount, failCount, skipCount, skippedCurrencies);

        return result;
    }
}
