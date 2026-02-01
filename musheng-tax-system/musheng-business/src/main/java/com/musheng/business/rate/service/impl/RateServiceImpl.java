package com.musheng.business.rate.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.strategy.FileImportStrategy;
import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.dto.RateConvertRequest;
import com.musheng.business.rate.dto.RateConvertResultDTO;
import com.musheng.business.rate.dto.RateRequest;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.mapper.HolidayMapper;
import com.musheng.business.rate.repository.ExchangeRateRepository;
import com.musheng.business.rate.service.RateService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
@RequiredArgsConstructor
public class RateServiceImpl implements RateService {

    // 使用 Repository 替代直接使用 Mapper
    private final ExchangeRateRepository exchangeRateRepository;
    
    // 保留 Mapper 用于 update 操作（Repository 暂不支持）
    private final ExchangeRateMapper exchangeRateMapper;
    private final HolidayMapper holidayMapper;
    
    // 导入策略列表（策略模式）
    private final List<FileImportStrategy<ExchangeRate>> importStrategies;

    /**
     * Maximum days to defer for holiday
     */
    private static final int MAX_DEFER_DAYS = 10;

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

        // Set defaults
        if (rate.getIsWorkday() == null) {
            rate.setIsWorkday(isWeekend(request.getRateDate()) ? 0 : 1);
        }
        if (!StringUtils.hasText(rate.getSource())) {
            rate.setSource("MANUAL");
        }
        rate.setActualRateDate(request.getRateDate());

        // ⚠️ 委托给 Repository 保存
        exchangeRateRepository.save(rate);
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

        if (existing.getIsWorkday() == null) {
            existing.setIsWorkday(isWeekend(request.getRateDate()) ? 0 : 1);
        }
        existing.setActualRateDate(request.getRateDate());

        // 使用 Mapper 进行更新（Repository 暂不支持 update）
        exchangeRateMapper.updateById(existing);
        log.info("Updated exchange rate: id={}, {} - {} = {}",
                id, existing.getCurrencyCode(), existing.getRateDate(), existing.getRate());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExchangeRate rate = getById(id);
        // ⚠️ 委托给 Repository 删除
        exchangeRateRepository.deleteById(id);
        log.info("Deleted exchange rate: id={}, {} - {}",
                id, rate.getCurrencyCode(), rate.getRateDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID列表不能为空");
        }
        // ⚠️ 委托给 Repository 批量删除
        exchangeRateRepository.deleteByIds(ids);
        log.info("Batch deleted {} exchange rates", ids.size());
    }

    @Override
    public BigDecimal getRate(String currencyCode, String date) {
        // Parse the date
        LocalDate queryDate = LocalDate.parse(date);

        // Apply holiday deferral logic - find next workday if current date is holiday/weekend
        LocalDate actualRateDate = getActualRateDate(queryDate);

        log.debug("Rate query: original date={}, actual rate date={}", date, actualRateDate);

        // ⚠️ 委托给 Repository 查询，逻辑不变
        ExchangeRate rate = exchangeRateRepository.findByCurrencyAndDate(currencyCode, actualRateDate);

        if (rate == null) {
            // Try to find the closest rate before the actual date
            // ⚠️ 委托给 Repository 查询最近的汇率
            rate = exchangeRateRepository.findLatestBefore(currencyCode, actualRateDate);
        }

        if (rate == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                    String.format("Exchange rate not found for %s on %s (actual: %s)",
                            currencyCode, date, actualRateDate));
        }

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

    /**
     * Get rate with holiday deferral - returns rate and actual date used
     *
     * @param sourceCurrency 源货币代码
     * @param targetCurrency 目标货币代码
     * @param date 日期
     * @return 汇率和实际日期
     */
    public RateWithDate getRateWithDate(String sourceCurrency, String targetCurrency, LocalDate date) {
        LocalDate actualRateDate = getActualRateDate(date);
        BigDecimal rate = getRate(sourceCurrency, targetCurrency, actualRateDate.toString());
        return new RateWithDate(rate, actualRateDate);
    }

    /**
     * Get actual rate date after applying holiday deferral logic
     * If the date is a weekend or holiday, defer to the next workday
     *
     * @param date 原始日期
     * @return 实际汇率日期（工作日）
     */
    private LocalDate getActualRateDate(LocalDate date) {
        LocalDate currentDate = date;
        int deferCount = 0;

        while (deferCount < MAX_DEFER_DAYS) {
            // Check if it's a weekend
            if (isWeekend(currentDate)) {
                currentDate = currentDate.plusDays(1);
                deferCount++;
                continue;
            }

            // Check if it's a holiday in database
            if (isHoliday(currentDate)) {
                currentDate = currentDate.plusDays(1);
                deferCount++;
                continue;
            }

            // Found a workday
            break;
        }

        if (deferCount >= MAX_DEFER_DAYS) {
            log.warn("Reached max defer days for rate date: original={}, final={}", date, currentDate);
        }

        return currentDate;
    }

    /**
     * Check if date is a weekend
     *
     * @param date 日期
     * @return 是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if date is a holiday (from database)
     *
     * @param date 日期
     * @return 是否为节假日
     */
    private boolean isHoliday(LocalDate date) {
        try {
            return holidayMapper.isHoliday(date);
        } catch (Exception e) {
            log.warn("Failed to check holiday for date: {}, assuming not holiday", date, e);
            return false;
        }
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
        
        // 委托给策略执行导入
        return strategy.importAndSave(file, new ImportContext());
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

    /**
     * Rate with date result
     */
    public record RateWithDate(BigDecimal rate, LocalDate actualDate) {}
}
