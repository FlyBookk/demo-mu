package com.musheng.business.rate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.rate.client.ChinaMoneyClient;
import com.musheng.business.rate.dto.RateSyncResultDTO;
import com.musheng.business.rate.entity.ExchangeRate;
import com.github.benmanes.caffeine.cache.Cache;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.service.RateSyncService;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.service.CurrencyService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 汇率同步服务实现
 */
@Slf4j
@Service
public class RateSyncServiceImpl implements RateSyncService {

    private final ChinaMoneyClient chinaMoneyClient;
    private final ExchangeRateMapper exchangeRateMapper;
    private final CurrencyService currencyService;
    private final Cache<String, Object> exchangeRateCache;

    public RateSyncServiceImpl(ChinaMoneyClient chinaMoneyClient,
                               ExchangeRateMapper exchangeRateMapper,
                               CurrencyService currencyService,
                               @Qualifier("exchangeRateCache") Cache<String, Object> exchangeRateCache) {
        this.chinaMoneyClient = chinaMoneyClient;
        this.exchangeRateMapper = exchangeRateMapper;
        this.currencyService = currencyService;
        this.exchangeRateCache = exchangeRateCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RateSyncResultDTO syncFromChinaMoney(LocalDate startDate, LocalDate endDate, String cookie) {
        log.info("Starting rate sync from China Money: startDate={}, endDate={}", startDate, endDate);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有启用的货币（排除CNY，CNY是基准货币）
            List<Currency> enabledCurrencies = currencyService.getEnabled().stream()
                    .filter(c -> !"CNY".equals(c.getCurrencyCode()))
                    .collect(Collectors.toList());
            if (enabledCurrencies.isEmpty()) {
                return RateSyncResultDTO.builder()
                        .success(false)
                        .startDate(startDate)
                        .endDate(endDate)
                        .message("No enabled currencies found (excluding CNY)")
                        .build();
            }

            List<String> currencyCodes = enabledCurrencies.stream()
                    .map(Currency::getCurrencyCode)
                    .collect(Collectors.toList());

            log.info("Syncing rates for enabled currencies: {}", currencyCodes);

            // 2. 从外汇中心获取汇率数据（支持传入 Cookie）
            List<ChinaMoneyClient.RateData> rateDataList = chinaMoneyClient.fetchRates(
                    startDate, endDate, enabledCurrencies, cookie);

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

            exchangeRateCache.invalidateAll();

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
                                                     List<String> currencyCodes, String cookie) {
        log.info("Starting rate sync for specific currencies: {}", currencyCodes);

        long startTime = System.currentTimeMillis();

        try {
            // 验证货币编码是否存在且启用（排除CNY）
            List<Currency> enabledCurrencies = currencyService.getEnabled().stream()
                    .filter(c -> !"CNY".equals(c.getCurrencyCode()))
                    .collect(Collectors.toList());
            List<String> enabledCodes = enabledCurrencies.stream()
                    .map(Currency::getCurrencyCode)
                    .collect(Collectors.toList());

            // 过滤出有效的货币编码（排除CNY）
            List<String> validCodes = currencyCodes.stream()
                    .filter(code -> !"CNY".equals(code))
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

            // 从外汇中心获取汇率数据（支持传入 Cookie）
            List<ChinaMoneyClient.RateData> rateDataList = chinaMoneyClient.fetchRates(
                    startDate, endDate, validCurrencies, cookie);

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

            exchangeRateCache.invalidateAll();

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
    public RateSyncResultDTO syncRecentDays(int days, String cookie) {
        if (days <= 0 || days > 365) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "Days must be between 1 and 365");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        log.info("Syncing recent {} days: {} to {}", days, startDate, endDate);

        return syncFromChinaMoney(startDate, endDate, cookie);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RateSyncResultDTO syncFromCurl(String curlCommand) {
        log.info("Starting rate sync from curl command");

        long startTime = System.currentTimeMillis();

        try {
            List<ChinaMoneyClient.RateData> rateDataList = chinaMoneyClient.executeCurlAndParse(curlCommand);

            if (rateDataList.isEmpty()) {
                return RateSyncResultDTO.builder()
                        .success(true)
                        .startDate(null)
                        .endDate(null)
                        .totalCount(0)
                        .insertCount(0)
                        .updateCount(0)
                        .failCount(0)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .message("未解析到汇率数据，请检查 curl 是否来自 CcprHisNew 请求")
                        .build();
            }

            // 只同步系统已有的货币（排除 CNY）
            java.util.Set<String> enabledCurrencyCodes = currencyService.getEnabled().stream()
                    .map(Currency::getCurrencyCode)
                    .filter(c -> !"CNY".equals(c))
                    .collect(Collectors.toSet());

            List<ChinaMoneyClient.RateData> filteredByCurrency = rateDataList.stream()
                    .filter(r -> enabledCurrencyCodes.contains(r.currencyCode()))
                    .collect(Collectors.toList());

            int skippedCurrency = rateDataList.size() - filteredByCurrency.size();
            if (skippedCurrency > 0) {
                log.info("Filtered out {} rates for currencies not in system", skippedCurrency);
            }

            LocalDate minDate = filteredByCurrency.isEmpty() ? null
                    : filteredByCurrency.stream().map(ChinaMoneyClient.RateData::date).min(LocalDate::compareTo).orElse(null);
            LocalDate maxDate = filteredByCurrency.isEmpty() ? null
                    : filteredByCurrency.stream().map(ChinaMoneyClient.RateData::date).max(LocalDate::compareTo).orElse(null);

            int insertCount = 0;
            int skipExistingCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (ChinaMoneyClient.RateData rateData : filteredByCurrency) {
                try {
                    LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(ExchangeRate::getRateDate, rateData.date())
                            .eq(ExchangeRate::getCurrencyCode, rateData.currencyCode());

                    ExchangeRate existing = exchangeRateMapper.selectOne(wrapper);

                    if (existing != null) {
                        skipExistingCount++;
                        continue;
                    }

                    BigDecimal rate = BigDecimal.valueOf(rateData.rate()).setScale(6, RoundingMode.HALF_UP);
                    ExchangeRate newRate = new ExchangeRate();
                    newRate.setRateDate(rateData.date());
                    newRate.setCurrencyCode(rateData.currencyCode());
                    newRate.setRate(rate);
                    newRate.setSource("CHINA_MONEY");
                    newRate.setActualRateDate(rateData.date());
                    exchangeRateMapper.insert(newRate);
                    insertCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add(String.format("Failed: %s-%s", rateData.date(), rateData.currencyCode()));
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;
            exchangeRateCache.invalidateAll();

            String msg = String.format("同步完成：解析 %d 条，过滤非系统货币 %d 条，跳过已存在 %d 条，新增 %d 条，失败 %d 条",
                    rateDataList.size(), skippedCurrency, skipExistingCount, insertCount, failCount);

            return RateSyncResultDTO.builder()
                    .success(true)
                    .startDate(minDate)
                    .endDate(maxDate)
                    .totalCount(filteredByCurrency.size())
                    .insertCount(insertCount)
                    .updateCount(0)
                    .failCount(failCount)
                    .errors(errors.size() > 10 ? errors.subList(0, 10) : errors)
                    .durationMs(durationMs)
                    .message(msg)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate sync from curl failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "同步失败: " + e.getMessage());
        }
    }
}
