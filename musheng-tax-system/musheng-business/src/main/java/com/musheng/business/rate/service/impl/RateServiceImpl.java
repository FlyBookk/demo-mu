package com.musheng.business.rate.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.strategy.FileImportStrategy;
import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.dto.RateConvertRequest;
import com.musheng.business.rate.dto.RateConvertResultDTO;
import com.musheng.business.rate.dto.RateRequest;
import com.musheng.business.rate.dto.RateWithDateDTO;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.musheng.business.rate.repository.ExchangeRateRepository;
import com.musheng.business.rate.service.RateService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 汇率服务实现类
 * 
 * 职责：
 * 1. 汇率 CRUD 操作
 * 2. 汇率查询（含节假日顺延逻辑）
 * 3. 汇率导入（委托给策略模式）
 * 4. 货币转换
 * 
 * ⚠️ 重构说明：
 * - 数据访问逻辑已委托给 ExchangeRateRepository
 * - 业务逻辑保持不变
 * - 输出结果保持不变
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Service
public class RateServiceImpl implements RateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    private final List<FileImportStrategy<ExchangeRate>> importStrategies;
    private final Cache<String, Object> exchangeRateCache;

    private static final String CACHE_KEY_FORMAT = "%s_%s";

    public RateServiceImpl(ExchangeRateRepository exchangeRateRepository,
                           ExchangeRateMapper exchangeRateMapper,
                           List<FileImportStrategy<ExchangeRate>> importStrategies,
                           @Qualifier("exchangeRateCache") Cache<String, Object> exchangeRateCache) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateMapper = exchangeRateMapper;
        this.importStrategies = importStrategies;
        this.exchangeRateCache = exchangeRateCache;
    }

    @Override
    public Page<ExchangeRate> list(String currencyCode, LocalDate startDate, LocalDate endDate, String source, int page, int size) {
        // ⚠️ 委托给 Repository，逻辑不变
        return exchangeRateRepository.findByQuery(currencyCode, startDate, endDate, source, page, size);
    }

    @Override
    public ExchangeRate getById(Long id) {
        // ⚠️ 委托给 Repository，逻辑不变
        ExchangeRate rate = exchangeRateRepository.findById(id);
        if (rate == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "汇率数据不存在");
        }
        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeRate create(RateRequest request) {
        // ⚠️ 使用 Repository 检查重复，逻辑不变
        if (exchangeRateRepository.existsByCurrencyAndDate(request.getCurrencyCode(), request.getRateDate())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST,
                    String.format("该日期(%s)和货币(%s)的汇率已存在",
                            request.getRateDate(), request.getCurrencyCode()));
        }

        ExchangeRate rate = new ExchangeRate();
        BeanUtils.copyProperties(request, rate);

        if (!StringUtils.hasText(rate.getSource())) {
            rate.setSource("MANUAL");
        }
        rate.setActualRateDate(request.getRateDate());

        exchangeRateRepository.save(rate);
        exchangeRateCache.invalidateAll();
        log.info("Created new exchange rate: {} - {} = {}",
                rate.getCurrencyCode(), rate.getRateDate(), rate.getRate());

        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeRate update(Long id, RateRequest request) {
        ExchangeRate existing = getById(id);

        // ⚠️ 使用 Repository 检查重复（排除当前ID），逻辑不变
        if (!existing.getRateDate().equals(request.getRateDate())
                || !existing.getCurrencyCode().equals(request.getCurrencyCode())) {
            if (exchangeRateRepository.existsByCurrencyAndDateExcludeId(
                    request.getCurrencyCode(), request.getRateDate(), id)) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST,
                        String.format("该日期(%s)和货币(%s)的汇率已存在",
                                request.getRateDate(), request.getCurrencyCode()));
            }
        }

        BeanUtils.copyProperties(request, existing, "id", "createTime", "createUser");

        existing.setActualRateDate(request.getRateDate());

        exchangeRateMapper.updateById(existing);
        exchangeRateCache.invalidateAll();
        log.info("Updated exchange rate: id={}, {} - {} = {}",
                id, existing.getCurrencyCode(), existing.getRateDate(), existing.getRate());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExchangeRate rate = getById(id);
        exchangeRateRepository.deleteById(id);
        exchangeRateCache.invalidateAll();
        log.info("Deleted exchange rate: id={}, {} - {}",
                id, rate.getCurrencyCode(), rate.getRateDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID列表不能为空");
        }
        exchangeRateRepository.deleteByIds(ids);
        exchangeRateCache.invalidateAll();
        log.info("Batch deleted {} exchange rates", ids.size());
    }

    @Override
    public BigDecimal getRate(String currencyCode, String date) {
        String cacheKey = String.format(CACHE_KEY_FORMAT, currencyCode, date);
        ExchangeRate cached = (ExchangeRate) exchangeRateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Rate cache hit: {}", cacheKey);
            return cached.getRate();
        }

        LocalDate queryDate = LocalDate.parse(date);
        ExchangeRate rate = exchangeRateRepository.findEarliestOnOrAfter(currencyCode, queryDate);
        if (rate == null) {
            rate = exchangeRateRepository.findLatestBefore(currencyCode, queryDate);
        }
        if (rate == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                    String.format("未找到汇率：货币 %s，日期 %s 及附近无汇率数据，请先在【汇率管理】中导入或同步该日期的汇率", currencyCode, date));
        }

        exchangeRateCache.put(cacheKey, rate);
        log.debug("Rate query: date={}, actual rate date={}", date, rate.getRateDate());
        return rate.getRate();
    }

    /**
     * Get rate for currency to CNY (legacy method for compatibility)
     *
     * @param sourceCurrency 源货币代码
     * @param targetCurrency 目标货币代码（本系统固定为CNY）
     * @param date 日期
     * @return 汇率
     */
    public BigDecimal getRate(String sourceCurrency, String targetCurrency, String date) {
        // Target is always CNY in this system
        return getRate(sourceCurrency, date);
    }

    @Override
    public RateWithDateDTO getRateWithDate(String currencyCode, LocalDate date) {
        String cacheKey = String.format(CACHE_KEY_FORMAT, currencyCode, date);
        ExchangeRate cached = (ExchangeRate) exchangeRateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Rate cache hit: {}", cacheKey);
            return RateWithDateDTO.builder()
                    .rate(cached.getRate())
                    .actualDate(cached.getRateDate())
                    .build();
        }

        ExchangeRate rate = exchangeRateRepository.findEarliestOnOrAfter(currencyCode, date);
        if (rate == null) {
            rate = exchangeRateRepository.findLatestBefore(currencyCode, date);
        }
        if (rate == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                    String.format("未找到汇率：货币 %s，日期 %s 及附近无汇率数据，请先在【汇率管理】中导入或同步该日期的汇率", currencyCode, date));
        }
        exchangeRateCache.put(cacheKey, rate);
        return RateWithDateDTO.builder()
                .rate(rate.getRate())
                .actualDate(rate.getRateDate())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("Importing exchange rates: fileName={}", file.getOriginalFilename());

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_FORMAT_ERROR, "File name is empty");
        }

        // 使用策略模式选择合适的导入策略
        FileImportStrategy<ExchangeRate> strategy = importStrategies.stream()
                .filter(s -> s.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_FILE_FORMAT_ERROR,
                        "Unsupported file format. Please use .xlsx, .xls or .csv file"));

        log.info("Using import strategy: {}", strategy.getClass().getSimpleName());

        Map<String, Object> result = strategy.importAndSave(file, new ImportContext());
        exchangeRateCache.invalidateAll();
        return result;
    }

    @Override
    public RateConvertResultDTO convertCurrency(RateConvertRequest request) {
        String currencyCode = request.getCurrencyCode();
        BigDecimal amount = request.getAmount();
        String rateDate = request.getRateDate();

        // Get exchange rate
        BigDecimal rate;
        String actualRateDate;

        if (StringUtils.hasText(rateDate)) {
            // Use specified date
            try {
                rate = getRate(currencyCode, rateDate);
                actualRateDate = rateDate;
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                        String.format("未找到 %s 在 %s 的汇率数据", currencyCode, rateDate));
            }
        } else {
            // ⚠️ 委托给 Repository 查询最新汇率
            ExchangeRate latestRate = exchangeRateRepository.findLatestByCurrency(currencyCode);
            if (latestRate == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                        String.format("未找到货币 %s 的汇率数据", currencyCode));
            }

            rate = latestRate.getRate();
            actualRateDate = latestRate.getRateDate().toString();
        }

        // Calculate converted amount (to CNY)
        BigDecimal convertedAmount = amount.multiply(rate)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        log.info("Currency conversion: {} {} -> {} CNY (rate: {}, date: {})",
                amount, currencyCode, convertedAmount, rate, actualRateDate);

        return RateConvertResultDTO.builder()
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .currencyCode(currencyCode)
                .rate(rate)
                .rateDate(actualRateDate)
                .build();
    }

}
