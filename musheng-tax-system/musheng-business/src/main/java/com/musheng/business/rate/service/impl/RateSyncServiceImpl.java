package com.musheng.business.rate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.rate.client.ChinaMoneyClient;
import com.musheng.business.rate.dto.RateSyncResultDTO;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.mapper.HolidayMapper;
import com.musheng.business.rate.service.RateSyncService;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.service.CurrencyService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 汇率同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateSyncServiceImpl implements RateSyncService {

    private final ChinaMoneyClient chinaMoneyClient;
    private final ExchangeRateMapper exchangeRateMapper;
    private final HolidayMapper holidayMapper;
    private final CurrencyService currencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RateSyncResultDTO syncFromChinaMoney(LocalDate startDate, LocalDate endDate) {
        log.info("Starting rate sync from China Money: startDate={}, endDate={}", startDate, endDate);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有启用的货币
            List<Currency> enabledCurrencies = currencyService.getEnabled();
            if (enabledCurrencies.isEmpty()) {
                return RateSyncResultDTO.builder()
                        .success(false)
                        .startDate(startDate)
                        .endDate(endDate)
                        .message("No enabled currencies found")
                        .build();
            }

            List<String> currencyCodes = enabledCurrencies.stream()
                    .map(Currency::getCurrencyCode)
                    .collect(Collectors.toList());

            log.info("Syncing rates for enabled currencies: {}", currencyCodes);

            // 2. 从外汇中心获取汇率数据
            List<ChinaMoneyClient.RateData> rateDataList = chinaMoneyClient.fetchRates(
                    startDate, endDate, enabledCurrencies);

            if (rateDataList.isEmpty()) {
                return RateSyncResultDTO.builder()
                        .success(true)
                        .startDate(startDate)
                        .endDate(endDate)
                        .currencyCodes(currencyCodes)
                        .totalCount(0)
                        .insertCount(0)
                        .updateCount(0)
                        .failCount(0)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .message("No rate data returned from China Money")
                        .build();
            }

            // 3. 保存或更新汇率数据
            int insertCount = 0;
            int updateCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (ChinaMoneyClient.RateData rateData : rateDataList) {
                try {
                    // 检查是否已存在
                    LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(ExchangeRate::getRateDate, rateData.date())
                            .eq(ExchangeRate::getCurrencyCode, rateData.currencyCode());

                    ExchangeRate existing = exchangeRateMapper.selectOne(wrapper);

                    BigDecimal rate = BigDecimal.valueOf(rateData.rate())
                            .setScale(6, RoundingMode.HALF_UP);

                    if (existing != null) {
                        // 更新现有记录
                        existing.setRate(rate);
                        existing.setSource("CHINA_MONEY");
                        exchangeRateMapper.updateById(existing);
                        updateCount++;
                    } else {
                        // 插入新记录
                        ExchangeRate newRate = new ExchangeRate();
                        newRate.setRateDate(rateData.date());
                        newRate.setCurrencyCode(rateData.currencyCode());
                        newRate.setRate(rate);
                        newRate.setSource("CHINA_MONEY");
                        newRate.setIsWorkday(isWeekend(rateData.date()) ? 0 : 1);
                        newRate.setActualRateDate(rateData.date());
                        exchangeRateMapper.insert(newRate);
                        insertCount++;
                    }

                } catch (Exception e) {
                    failCount++;
                    String errorMsg = String.format("Failed to save rate: date=%s, currency=%s, error=%s",
                            rateData.date(), rateData.currencyCode(), e.getMessage());
                    errors.add(errorMsg);
                    log.warn(errorMsg, e);
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;

            log.info("Rate sync completed: total={}, insert={}, update={}, fail={}, duration={}ms",
                    rateDataList.size(), insertCount, updateCount, failCount, durationMs);

            return RateSyncResultDTO.builder()
                    .success(true)
                    .startDate(startDate)
                    .endDate(endDate)
                    .currencyCodes(currencyCodes)
                    .totalCount(rateDataList.size())
                    .insertCount(insertCount)
                    .updateCount(updateCount)
                    .failCount(failCount)
                    .errors(errors.size() > 10 ? errors.subList(0, 10) : errors)
                    .durationMs(durationMs)
                    .message(String.format("Synced %d rates (%d new, %d updated, %d failed)",
                            rateDataList.size(), insertCount, updateCount, failCount))
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate sync failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Rate sync failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RateSyncResultDTO syncSpecificCurrencies(LocalDate startDate, LocalDate endDate,
                                                     List<String> currencyCodes) {
        log.info("Starting rate sync for specific currencies: {}", currencyCodes);

        long startTime = System.currentTimeMillis();

        try {
            // 验证货币编码是否存在且启用
            List<Currency> enabledCurrencies = currencyService.getEnabled();
            List<String> enabledCodes = enabledCurrencies.stream()
                    .map(Currency::getCurrencyCode)
                    .collect(Collectors.toList());

            // 过滤出有效的货币编码
            List<String> validCodes = currencyCodes.stream()
                    .filter(enabledCodes::contains)
                    .collect(Collectors.toList());

            if (validCodes.isEmpty()) {
                return RateSyncResultDTO.builder()
                        .success(false)
                        .startDate(startDate)
                        .endDate(endDate)
                        .currencyCodes(currencyCodes)
                        .message("No valid enabled currencies found in request")
                        .build();
            }

            // 获取有效的货币对象列表
            List<Currency> validCurrencies = enabledCurrencies.stream()
                    .filter(c -> validCodes.contains(c.getCurrencyCode()))
                    .collect(Collectors.toList());

            // 从外汇中心获取汇率数据
            List<ChinaMoneyClient.RateData> rateDataList = chinaMoneyClient.fetchRates(
                    startDate, endDate, validCurrencies);

            // 保存数据的逻辑与syncFromChinaMoney相同
            int insertCount = 0;
            int updateCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (ChinaMoneyClient.RateData rateData : rateDataList) {
                try {
                    LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(ExchangeRate::getRateDate, rateData.date())
                            .eq(ExchangeRate::getCurrencyCode, rateData.currencyCode());

                    ExchangeRate existing = exchangeRateMapper.selectOne(wrapper);
                    BigDecimal rate = BigDecimal.valueOf(rateData.rate())
                            .setScale(6, RoundingMode.HALF_UP);

                    if (existing != null) {
                        existing.setRate(rate);
                        existing.setSource("CHINA_MONEY");
                        exchangeRateMapper.updateById(existing);
                        updateCount++;
                    } else {
                        ExchangeRate newRate = new ExchangeRate();
                        newRate.setRateDate(rateData.date());
                        newRate.setCurrencyCode(rateData.currencyCode());
                        newRate.setRate(rate);
                        newRate.setSource("CHINA_MONEY");
                        newRate.setIsWorkday(isWeekend(rateData.date()) ? 0 : 1);
                        newRate.setActualRateDate(rateData.date());
                        exchangeRateMapper.insert(newRate);
                        insertCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add(String.format("Failed: %s-%s", rateData.date(), rateData.currencyCode()));
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;

            return RateSyncResultDTO.builder()
                    .success(true)
                    .startDate(startDate)
                    .endDate(endDate)
                    .currencyCodes(validCodes)
                    .totalCount(rateDataList.size())
                    .insertCount(insertCount)
                    .updateCount(updateCount)
                    .failCount(failCount)
                    .errors(errors)
                    .durationMs(durationMs)
                    .message(String.format("Synced %d rates", rateDataList.size()))
                    .build();

        } catch (Exception e) {
            log.error("Rate sync failed for specific currencies", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Rate sync failed: " + e.getMessage());
        }
    }

    @Override
    public RateSyncResultDTO syncRecentDays(int days) {
        if (days <= 0 || days > 365) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "Days must be between 1 and 365");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        log.info("Syncing recent {} days: {} to {}", days, startDate, endDate);

        return syncFromChinaMoney(startDate, endDate);
    }

    /**
     * 检查是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
