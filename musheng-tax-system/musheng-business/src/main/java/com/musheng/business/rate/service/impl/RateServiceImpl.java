package com.musheng.business.rate.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.rate.dto.RateConvertRequest;
import com.musheng.business.rate.dto.RateConvertResultDTO;
import com.musheng.business.rate.dto.RateRequest;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.mapper.HolidayMapper;
import com.musheng.business.rate.service.RateService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Exchange Rate Service Implementation
 * Implements holiday deferral logic for exchange rate lookup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateServiceImpl implements RateService {

    private final ExchangeRateMapper exchangeRateMapper;
    private final HolidayMapper holidayMapper;

    /**
     * Maximum days to defer for holiday
     */
    private static final int MAX_DEFER_DAYS = 10;

    @Override
    public Page<ExchangeRate> list(String currencyCode, LocalDate startDate, LocalDate endDate, String source, int page, int size) {
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
    public ExchangeRate getById(Long id) {
        ExchangeRate rate = exchangeRateMapper.selectById(id);
        if (rate == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "汇率数据不存在");
        }
        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeRate create(RateRequest request) {
        // Check if rate already exists for this date and currency
        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getRateDate, request.getRateDate())
                .eq(ExchangeRate::getCurrencyCode, request.getCurrencyCode());

        Long count = exchangeRateMapper.selectCount(wrapper);
        if (count > 0) {
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

        exchangeRateMapper.insert(rate);
        log.info("Created new exchange rate: {} - {} = {}",
                rate.getCurrencyCode(), rate.getRateDate(), rate.getRate());

        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeRate update(Long id, RateRequest request) {
        ExchangeRate existing = getById(id);

        // Check for duplicate if date or currency changed
        if (!existing.getRateDate().equals(request.getRateDate())
                || !existing.getCurrencyCode().equals(request.getCurrencyCode())) {
            LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExchangeRate::getRateDate, request.getRateDate())
                    .eq(ExchangeRate::getCurrencyCode, request.getCurrencyCode())
                    .ne(ExchangeRate::getId, id);

            Long count = exchangeRateMapper.selectCount(wrapper);
            if (count > 0) {
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

        exchangeRateMapper.updateById(existing);
        log.info("Updated exchange rate: id={}, {} - {} = {}",
                id, existing.getCurrencyCode(), existing.getRateDate(), existing.getRate());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExchangeRate rate = getById(id);
        exchangeRateMapper.deleteById(id);
        log.info("Deleted exchange rate: id={}, {} - {}",
                id, rate.getCurrencyCode(), rate.getRateDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID列表不能为空");
        }
        int count = exchangeRateMapper.deleteBatchIds(ids);
        log.info("Batch deleted {} exchange rates", count);
    }

    @Override
    public BigDecimal getRate(String currencyCode, String date) {
        // Parse the date
        LocalDate queryDate = LocalDate.parse(date);

        // Apply holiday deferral logic - find next workday if current date is holiday/weekend
        LocalDate actualRateDate = getActualRateDate(queryDate);

        log.debug("Rate query: original date={}, actual rate date={}", date, actualRateDate);

        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                .eq(ExchangeRate::getRateDate, actualRateDate);

        ExchangeRate rate = exchangeRateMapper.selectOne(wrapper);

        if (rate == null) {
            // Try to find the closest rate before the actual date
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                    .le(ExchangeRate::getRateDate, actualRateDate)
                    .orderByDesc(ExchangeRate::getRateDate)
                    .last("LIMIT 1");

            rate = exchangeRateMapper.selectOne(wrapper);
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
     */
    public BigDecimal getRate(String sourceCurrency, String targetCurrency, String date) {
        // Target is always CNY in this system
        return getRate(sourceCurrency, date);
    }

    /**
     * Get rate with holiday deferral - returns rate and actual date used
     */
    public RateWithDate getRateWithDate(String sourceCurrency, String targetCurrency, LocalDate date) {
        LocalDate actualRateDate = getActualRateDate(date);
        BigDecimal rate = getRate(sourceCurrency, targetCurrency, actualRateDate.toString());
        return new RateWithDate(rate, actualRateDate);
    }

    /**
     * Get actual rate date after applying holiday deferral logic
     * If the date is a weekend or holiday, defer to the next workday
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
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if date is a holiday (from database)
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

        // 判断文件类型
        if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
            return importExcelData(file);
        } else if (fileName.toLowerCase().endsWith(".csv")) {
            return importCsvData(file);
        } else {
            throw new BusinessException(ErrorCode.IMPORT_FILE_FORMAT_ERROR,
                    "Unsupported file format. Please use .xlsx, .xls or .csv file");
        }
    }

    /**
     * Import exchange rates from CSV file
     * Optimized: batch insert to avoid N+1 query problem
     * - Skip if date+currency already exists (no update)
     * - Batch insert new records
     * - Return: totalCount, successCount, existsCount, failCount
     */
    private Map<String, Object> importCsvData(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        AtomicInteger existsCount = new AtomicInteger();

        // 收集待导入的数据
        List<ExchangeRate> ratesToImport = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();  // 用于检测文件内重复

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
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
                    exchangeRate.setIsWorkday(isWeekend(rateDate) ? 0 : 1);

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

        // 批量检查已存在的数据（避免N+1查询）
        if (!ratesToImport.isEmpty()) {
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
            if (!newRates.isEmpty()) {
                // MyBatis-Plus批量插入，每批1000条
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
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("existsCount", existsCount);
        result.put("failCount", failCount);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);

        log.info("CSV import completed: total={}, success={}, exists={}, fail={}",
                totalCount, successCount, existsCount, failCount);

        return result;
    }

    /**
     * Import exchange rates from Excel file (matrix format)
     * Format: First column is date, other columns are currency pairs (e.g., USD/CNY, EUR/CNY)
     * Optimized: batch insert to avoid N+1 query problem
     * - Skip if date+currency already exists (no update)
     * - Batch insert new records
     * - Return: totalCount, successCount, existsCount, failCount
     */
    private Map<String, Object> importExcelData(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        AtomicInteger existsCount = new AtomicInteger();

        // 收集待导入的数据
        List<ExchangeRate> ratesToImport = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();  // 用于检测文件内重复

        try {
            // Read Excel file with headers
            List<Map<Integer, String>> allRows = EasyExcel.read(file.getInputStream())
                    .sheet(0)
                    .headRowNumber(0)  // No header row initially
                    .doReadSync();

            if (allRows.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_FILE_EMPTY, "Excel file is empty");
            }

            // Get headers from first row
            Map<Integer, String> headerRow = allRows.get(0);
            if (headerRow == null || headerRow.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND, "Header row not found");
            }

            // Data rows start from index 1
            List<Map<Integer, String>> dataList = allRows.subList(1, allRows.size());

            // Parse headers: first column should be date, others are currency pairs
            String dateColumnName = headerRow.get(0);
            if (!isDateColumn(dateColumnName)) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND,
                        "First column should be date column (e.g., '日期', 'Date', '时间')");
            }

            // Get currency column mappings (column index -> currency code)
            Map<Integer, String> currencyColumns = new HashMap<>();
            for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
                int colIndex = entry.getKey();
                if (colIndex == 0) continue; // Skip date column

                String columnName = entry.getValue();
                String currencyCode = parseCurrencyCode(columnName);
                if (currencyCode != null) {
                    currencyColumns.put(colIndex, currencyCode);
                }
            }

            if (currencyColumns.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND,
                        "No valid currency columns found. Expected format: USD/CNY, EUR/CNY, etc.");
            }

            log.info("Found {} currency columns: {}", currencyColumns.size(), currencyColumns.values());

            // Process each row
            for (Map<Integer, String> row : dataList) {
                String dateStr = row.get(0);
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    continue; // Skip empty rows
                }

                // Skip data source rows (e.g., "数据来源：中国货币网")
                if (dateStr.contains("数据来源") || dateStr.contains("www.")) {
                    continue;
                }

                try {
                    LocalDate rateDate = parseRateDate(dateStr);

                    // Process each currency column
                    for (Map.Entry<Integer, String> currencyEntry : currencyColumns.entrySet()) {
                        int colIndex = currencyEntry.getKey();
                        String currencyCode = currencyEntry.getValue();
                        String rateStr = row.get(colIndex);

                        if (rateStr == null || rateStr.trim().isEmpty()) {
                            continue; // Skip empty cells
                        }

                        try {
                            totalCount++;
                            BigDecimal rate = new BigDecimal(rateStr.trim().replace(",", ""));

                            // 检查文件内重复
                            String uniqueKey = rateDate + "_" + currencyCode;
                            if (uniqueKeys.contains(uniqueKey)) {
                                failCount++;
                                errors.add(String.format("Date %s, Currency %s: Duplicate in file",
                                        rateDate, currencyCode));
                                continue;
                            }
                            uniqueKeys.add(uniqueKey);

                            // 创建待导入对象
                            ExchangeRate exchangeRate = new ExchangeRate();
                            exchangeRate.setRateDate(rateDate);
                            exchangeRate.setCurrencyCode(currencyCode);
                            exchangeRate.setRate(rate);
                            exchangeRate.setSource("IMPORT");
                            exchangeRate.setIsWorkday(isWeekend(rateDate) ? 0 : 1);

                            ratesToImport.add(exchangeRate);

                        } catch (Exception e) {
                            failCount++;
                            errors.add(String.format("Date %s, Currency %s: %s",
                                    dateStr, currencyCode, e.getMessage()));
                            log.warn("Failed to import rate: date={}, currency={}, error={}",
                                    dateStr, currencyCode, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add(String.format("Date %s: %s", dateStr, e.getMessage()));
                    log.warn("Failed to parse date: {}", dateStr, e);
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to import Excel file", e);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                    "Failed to parse Excel file: " + e.getMessage());
        }

        // 批量检查已存在的数据（避免N+1查询）
        if (!ratesToImport.isEmpty()) {
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
            if (!newRates.isEmpty()) {
                // MyBatis-Plus批量插入，每批1000条
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
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("existsCount", existsCount);
        result.put("failCount", failCount);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);

        log.info("Excel import completed: total={}, success={}, exists={}, fail={}",
                totalCount, successCount, existsCount, failCount);

        return result;
    }

    /**
     * Check if column name indicates a date column
     */
    private boolean isDateColumn(String columnName) {
        if (columnName == null) return false;
        String lower = columnName.toLowerCase().trim();
        return lower.contains("日期") || lower.contains("date") ||
                lower.contains("时间") || lower.contains("time");
    }

    /**
     * Parse currency code from column name
     * Examples: "USD/CNY" -> "USD", "EUR/CNY" -> "EUR"
     */
    private String parseCurrencyCode(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return null;
        }

        String trimmed = columnName.trim();

        // Pattern: XXX/CNY or XXX/RMB
        if (trimmed.contains("/")) {
            String[] parts = trimmed.split("/");
            if (parts.length >= 2) {
                String firstPart = parts[0].trim();
                // Remove numeric prefix (e.g., "100JPY" -> "JPY")
                firstPart = firstPart.replaceAll("^\\d+", "");
                return firstPart.toUpperCase();
            }
        }

        // Pattern: direct currency code (USD, EUR, etc.)
        if (trimmed.matches("[A-Z]{3}")) {
            return trimmed.toUpperCase();
        }

        return null;
    }

    /**
     * Find column index by possible names
     */
    private int findColumnIndex(List<String> headers, String... possibleNames) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            for (String name : possibleNames) {
                if (header.contains(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Parse rate date from various formats
     */
    private LocalDate parseRateDate(String dateStr) {
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
            // Use latest rate
            LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
                    .orderByDesc(ExchangeRate::getRateDate)
                    .last("LIMIT 1");

            ExchangeRate latestRate = exchangeRateMapper.selectOne(wrapper);
            if (latestRate == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                        String.format("未找到货币 %s 的汇率数据", currencyCode));
            }

            rate = latestRate.getRate();
            actualRateDate = latestRate.getRateDate().toString();
        }

        // Calculate converted amount (to CNY)
        BigDecimal convertedAmount = amount.multiply(rate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

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
