package com.musheng.business.rate.strategy;

import com.alibaba.excel.EasyExcel;
import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.currency.mapper.CurrencyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel 汇率导入策略
 * 
 * 处理 Excel 格式（.xlsx, .xls）的汇率文件导入。
 * 
 * Excel 文件格式要求（矩阵格式）：
 * - 第一列为日期列（列名包含"日期"、"Date"、"时间"等）
 * - 其他列为货币对（如 USD/CNY, EUR/CNY）
 * - 每行一个日期，每列一个货币的汇率
 * 
 * ⚠️ 重要：所有逻辑必须与原 RateServiceImpl.importExcelData() 完全一致，
 * 以确保重构不改变任何业务输出。
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Component
public class RateExcelImportStrategy extends AbstractRateImportStrategy {
    
    /**
     * 构造函数
     * 
     * @param currencyMapper 货币 Mapper
     * @param exchangeRateMapper 汇率 Mapper
     */
    public RateExcelImportStrategy(CurrencyMapper currencyMapper, 
                                    ExchangeRateMapper exchangeRateMapper) {
        super(currencyMapper, exchangeRateMapper);
    }
    
    /**
     * 判断是否支持该文件类型
     * 
     * @param fileName 文件名
     * @return 支持 .xlsx 或 .xls 文件返回 true
     */
    @Override
    public boolean supports(String fileName) {
        return fileName != null && 
               (fileName.toLowerCase().endsWith(".xlsx") || 
                fileName.toLowerCase().endsWith(".xls"));
    }
    
    /**
     * 解析 Excel 文件内容
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
        
        try {
            // Read Excel file with headers
            List<Map<Integer, String>> allRows = EasyExcel.read(file.getInputStream())
                    .sheet(0)
                    .headRowNumber(0)  // No header row initially
                    .doReadSync();

            if (allRows.isEmpty()) {
                return rates;
            }

            // Get headers from first row
            Map<Integer, String> headerRow = allRows.get(0);
            if (headerRow == null || headerRow.isEmpty()) {
                return rates;
            }

            // Data rows start from index 1
            List<Map<Integer, String>> dataList = allRows.subList(1, allRows.size());

            // Parse headers: first column should be date, others are currency pairs
            String dateColumnName = headerRow.get(0);
            if (!isDateColumn(dateColumnName)) {
                return rates;
            }

            // Get currency column mappings (column index -> currency code)
            Map<Integer, String> currencyColumns = new HashMap<>();
            for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
                int colIndex = entry.getKey();
                if (colIndex == 0) continue; // Skip date column

                String columnName = entry.getValue();
                String currencyCode = parseCurrencyCode(columnName);
                if (currencyCode != null && configuredCurrencies.contains(currencyCode.toUpperCase())) {
                    currencyColumns.put(colIndex, currencyCode.toUpperCase());
                }
            }

            // Process each row
            for (Map<Integer, String> row : dataList) {
                String dateStr = row.get(0);
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    continue;
                }

                // Skip data source rows
                if (dateStr.contains("数据来源") || dateStr.contains("www.")) {
                    continue;
                }

                try {
                    LocalDate rateDate = parseRateDate(dateStr);

                    for (Map.Entry<Integer, String> currencyEntry : currencyColumns.entrySet()) {
                        int colIndex = currencyEntry.getKey();
                        String currencyCode = currencyEntry.getValue();
                        String rateStr = row.get(colIndex);

                        if (rateStr == null || rateStr.trim().isEmpty()) {
                            continue;
                        }

                        try {
                            BigDecimal rate = new BigDecimal(rateStr.trim().replace(",", ""));

                            String uniqueKey = rateDate + "_" + currencyCode;
                            if (uniqueKeys.contains(uniqueKey)) {
                                continue;
                            }
                            uniqueKeys.add(uniqueKey);

                            ExchangeRate exchangeRate = new ExchangeRate();
                            exchangeRate.setRateDate(rateDate);
                            exchangeRate.setCurrencyCode(currencyCode);
                            exchangeRate.setRate(rate);
                            exchangeRate.setSource("IMPORT");
                            exchangeRate.setIsWorkday(isWeekend(rateDate) ? 0 : 1);

                            rates.add(exchangeRate);
                        } catch (Exception e) {
                            log.warn("Failed to parse rate: date={}, currency={}, error={}",
                                    dateStr, currencyCode, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse date: {}", dateStr, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Excel file", e);
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }
        
        return rates;
    }
    
    /**
     * 执行 Excel 导入并保存
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.importExcelData() 完全一致
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
            // 只保留已配置的货币
            Map<Integer, String> currencyColumns = new HashMap<>();
            Set<String> unconfiguredCurrenciesInHeader = new HashSet<>();
            for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
                int colIndex = entry.getKey();
                if (colIndex == 0) continue; // Skip date column

                String columnName = entry.getValue();
                String currencyCode = parseCurrencyCode(columnName);
                if (currencyCode != null) {
                    // 检查货币是否已配置
                    if (configuredCurrencies.contains(currencyCode.toUpperCase())) {
                        currencyColumns.put(colIndex, currencyCode.toUpperCase());
                    } else {
                        unconfiguredCurrenciesInHeader.add(currencyCode);
                    }
                }
            }

            if (currencyColumns.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND,
                        "No configured currency columns found. Found currencies in file: " + unconfiguredCurrenciesInHeader +
                        ". Please configure these currencies first or use a file with configured currencies.");
            }

            log.info("Found {} configured currency columns: {}, unconfigured currencies skipped: {}",
                    currencyColumns.size(), currencyColumns.values(), unconfiguredCurrenciesInHeader);
            skippedCurrencies.addAll(unconfiguredCurrenciesInHeader);

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

        log.info("Excel import completed: total={}, success={}, exists={}, fail={}, skip={}, skippedCurrencies={}",
                totalCount, successCount, existsCount, failCount, skipCount, skippedCurrencies);

        return result;
    }
    
    /**
     * 检查列名是否为日期列
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.isDateColumn() 完全一致
     * 
     * @param columnName 列名
     * @return 是日期列返回 true
     */
    private boolean isDateColumn(String columnName) {
        if (columnName == null) return false;
        String lower = columnName.toLowerCase().trim();
        return lower.contains("日期") || lower.contains("date") ||
                lower.contains("时间") || lower.contains("time");
    }
    
    /**
     * 从列名解析货币代码
     * 
     * 支持格式：
     * - "USD/CNY" -> "USD"
     * - "EUR/CNY" -> "EUR"
     * - "100JPY/CNY" -> "JPY"
     * - "USD" -> "USD"
     * 
     * ⚠️ 逻辑与原 RateServiceImpl.parseCurrencyCode() 完全一致
     * 
     * @param columnName 列名
     * @return 货币代码，无法解析返回 null
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
}
