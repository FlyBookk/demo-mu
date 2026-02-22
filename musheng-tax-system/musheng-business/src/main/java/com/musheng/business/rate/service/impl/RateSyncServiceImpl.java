package com.musheng.business.rate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.musheng.business.rate.client.ChinaMoneyClient;
import com.musheng.business.rate.dto.RateSyncResultDTO;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.service.RateSyncService;
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
 * 仅支持 curl 同步
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

            java.util.Set<String> enabledCurrencyCodes = currencyService.getEnabled().stream()
                    .map(c -> c.getCurrencyCode())
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
