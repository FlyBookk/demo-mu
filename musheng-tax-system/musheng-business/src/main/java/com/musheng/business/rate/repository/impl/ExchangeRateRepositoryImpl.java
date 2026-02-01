package com.musheng.business.rate.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率数据仓储实现类
 * 
 * 封装汇率数据的数据访问逻辑，保持与原有 RateServiceImpl 中的数据访问逻辑完全一致。
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将数据访问逻辑从 Service 移动到 Repository
 *
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {

    private final ExchangeRateMapper exchangeRateMapper;

    @Override
    public Page<ExchangeRate> findByQuery(String currencyCode, LocalDate startDate, 
                                           LocalDate endDate, String source, int page, int size) {
        // ⚠️ 逻辑与原 RateServiceImpl.list() 方法完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(currencyCode)) {
            wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode);
        }
        if (startDate != null) {
            wrapper.ge(ExchangeRate::getRateDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ExchangeRate::getRateDate, endDate);
        }
        if (StringUtils.hasText(source)) {
            wrapper.eq(ExchangeRate::getSource, source);
        }

        wrapper.orderByDesc(ExchangeRate::getRateDate);

        return exchangeRateMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public ExchangeRate findById(Long id) {
        return exchangeRateMapper.selectById(id);
    }

    @Override
    public ExchangeRate findByCurrencyAndDate(String currencyCode, LocalDate date) {
        // ⚠️ 逻辑与原 RateServiceImpl.getRate() 方法中的查询完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                .eq(ExchangeRate::getRateDate, date);
        return exchangeRateMapper.selectOne(wrapper);
    }

    @Override
    public boolean existsByCurrencyAndDate(String currencyCode, LocalDate date) {
        // ⚠️ 逻辑与原 RateServiceImpl.create() 方法中的重复检查完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getRateDate, date)
                .eq(ExchangeRate::getCurrencyCode, currencyCode);
        return exchangeRateMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByCurrencyAndDateExcludeId(String currencyCode, LocalDate date, Long excludeId) {
        // ⚠️ 逻辑与原 RateServiceImpl.update() 方法中的重复检查完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getRateDate, date)
                .eq(ExchangeRate::getCurrencyCode, currencyCode)
                .ne(ExchangeRate::getId, excludeId);
        return exchangeRateMapper.selectCount(wrapper) > 0;
    }

    @Override
    public ExchangeRate findLatestBefore(String currencyCode, LocalDate date) {
        // ⚠️ 逻辑与原 RateServiceImpl.getRate() 方法中的回退查询完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                .le(ExchangeRate::getRateDate, date)
                .orderByDesc(ExchangeRate::getRateDate)
                .last("LIMIT 1");
        return exchangeRateMapper.selectOne(wrapper);
    }

    @Override
    public ExchangeRate findLatestByCurrency(String currencyCode) {
        // ⚠️ 逻辑与原 RateServiceImpl.convertCurrency() 方法中的最新汇率查询完全一致
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                .orderByDesc(ExchangeRate::getRateDate)
                .last("LIMIT 1");
        return exchangeRateMapper.selectOne(wrapper);
    }

    @Override
    public void save(ExchangeRate rate) {
        exchangeRateMapper.insert(rate);
        log.info("Saved exchange rate: {} - {} = {}", 
                rate.getCurrencyCode(), rate.getRateDate(), rate.getRate());
    }

    @Override
    public void saveBatch(List<ExchangeRate> rates) {
        if (rates == null || rates.isEmpty()) {
            return;
        }
        // 使用循环插入，保持与原有逻辑一致
        for (ExchangeRate rate : rates) {
            exchangeRateMapper.insert(rate);
        }
        log.info("Batch saved {} exchange rates", rates.size());
    }

    @Override
    public void deleteById(Long id) {
        exchangeRateMapper.deleteById(id);
        log.info("Deleted exchange rate: id={}", id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = exchangeRateMapper.deleteBatchIds(ids);
        log.info("Batch deleted {} exchange rates", count);
    }

    @Override
    public long countByCurrencyAndDate(String currencyCode, LocalDate date) {
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getRateDate, date)
                .eq(ExchangeRate::getCurrencyCode, currencyCode);
        return exchangeRateMapper.selectCount(wrapper);
    }
}
