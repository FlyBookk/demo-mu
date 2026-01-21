package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.business.common.service.csv.CsvHeaderResult;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.service.SalesDataService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import com.musheng.config.mapping.entity.FieldMappingTemplate;
import com.musheng.config.mapping.entity.TransactionTypeMapping;
import com.musheng.config.mapping.mapper.FieldMappingTemplateMapper;
import com.musheng.config.mapping.mapper.TransactionTypeMappingMapper;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Sales Data Service Implementation
 * Implements CSV import with field mapping and transaction type mapping
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataServiceImpl implements SalesDataService {

    private final SalesDataMapper salesDataMapper;
    private final FieldMappingTemplateMapper fieldMappingTemplateMapper;
    private final TransactionTypeMappingMapper transactionTypeMappingMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final ImportRecordMapper importRecordMapper;
    private final CsvParseServiceImpl csvParseService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SalesData> list(String siteCode, String transactionCategory, String transactionType, String orderId, int page, int size) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(transactionCategory)) {
            wrapper.eq(SalesData::getTransactionCategory, transactionCategory);
        }
        if (StringUtils.hasText(transactionType)) {
            wrapper.eq(SalesData::getTransactionType, transactionType);
        }
        if (StringUtils.hasText(orderId)) {
            wrapper.like(SalesData::getOrderId, orderId);
        }

        wrapper.orderByDesc(SalesData::getCreateTime);

        return salesDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public SalesData getById(Long id) {
        SalesData entity = salesDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Sales data not found");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(String siteCode, MultipartFile file) {
        log.info("Importing sales data: siteCode={}, fileName={}",
                siteCode, file.getOriginalFilename());

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        // Create import record
        ImportRecord importRecord = new ImportRecord();
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("sales");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // Get marketplace config
            LambdaQueryWrapper<Marketplace> mpWrapper = new LambdaQueryWrapper<>();
            mpWrapper.eq(Marketplace::getSiteCode, siteCode);
            Marketplace marketplace = marketplaceMapper.selectOne(mpWrapper);

            if (marketplace == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found: " + siteCode);
            }

            // Get field mapping template
            Map<String, String> fieldMapping = getFieldMapping(siteCode, "sales");

            // Get transaction type mappings
            Map<String, String> transactionTypeMapping = getTransactionTypeMapping(siteCode);

            // Parse CSV file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                // Detect and skip header rows
                CsvHeaderResult headerResult = csvParseService.parseHeaders(file);
                List<String> headers = headerResult.getHeaders();
                int headerRowIndex = headerResult.getHeaderRowIndex();

                // Reset input stream and skip to data rows
                try (BufferedReader dataReader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                    // Skip header rows
                    for (int i = 0; i <= headerRowIndex; i++) {
                        dataReader.readLine();
                    }

                    CSVParser parser = CSVFormat.DEFAULT.parse(dataReader);

                    for (CSVRecord record : parser) {
                        totalCount++;
                        try {
                            SalesData salesData = parseSalesRecord(record, headers, fieldMapping,
                                    transactionTypeMapping, siteCode, marketplace, totalCount);

                            if (salesData != null) {
                                // Check for duplicate
                                if (!isDuplicate(salesData)) {
                                    salesData.setImportBatchId(importRecord.getId());
                                    salesDataMapper.insert(salesData);
                                    successCount++;
                                } else {
                                    failCount++;
                                    errors.add(String.format("Row %d: Duplicate order %s", totalCount, salesData.getOrderId()));
                                }
                            }
                        } catch (Exception e) {
                            failCount++;
                            errors.add(String.format("Row %d: %s", totalCount, e.getMessage()));
                            log.warn("Failed to import sales data at row {}: {}", totalCount, e.getMessage());
                        }
                    }
                }
            }

            // Update import record
            importRecord.setTotalCount(totalCount);
            importRecord.setSuccessCount(successCount);
            importRecord.setFailCount(failCount);
            importRecord.setImportStatus(failCount == 0 ? "success" : (successCount > 0 ? "partial" : "fail"));
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors.subList(0, Math.min(10, errors.size()))));
            }
            importRecordMapper.updateById(importRecord);

        } catch (BusinessException e) {
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw e;
        } catch (Exception e) {
            log.error("Failed to import sales data", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Failed to parse file: " + e.getMessage());
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
        result.put("batchNo", importRecord.getBatchNo());

        log.info("Sales data import completed: total={}, success={}, fail={}", totalCount, successCount, failCount);

        return result;
    }

    /**
     * Parse a single CSV record into SalesData entity
     */
    private SalesData parseSalesRecord(CSVRecord record, List<String> headers,
                                       Map<String, String> fieldMapping, Map<String, String> transactionTypeMapping,
                                       String siteCode, Marketplace marketplace, int rowNum) {

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), record.size()); i++) {
            rowData.put(headers.get(i).toLowerCase().trim(), record.get(i).trim());
        }

        SalesData salesData = new SalesData();
        salesData.setSiteCode(siteCode);
        salesData.setMarketplace(marketplace.getMarketplaceId());
        salesData.setCurrencyCode(marketplace.getCurrencyCode());

        // Map fields using field mapping configuration
        String orderId = getMappedValue(rowData, fieldMapping, "order_id");
        if (!StringUtils.hasText(orderId)) {
            return null; // Skip empty rows
        }
        salesData.setOrderId(orderId);

        // Parse transaction type and map to category
        String transactionType = getMappedValue(rowData, fieldMapping, "transaction_type");
        salesData.setTransactionType(transactionType);

        // Map transaction type to standard category
        String category = transactionTypeMapping.getOrDefault(transactionType, "other");
        salesData.setTransactionCategory(category);

        // Parse date
        String dateStr = getMappedValue(rowData, fieldMapping, "date_time");
        if (StringUtils.hasText(dateStr)) {
            salesData.setOriginalDateStr(dateStr);
            LocalDateTime dateTime = csvParseService.parseDate(dateStr, siteCode);
            if (dateTime != null) {
                salesData.setTransactionDate(dateTime);
            }
        }

        // Parse other fields
        salesData.setSettlementId(getMappedValue(rowData, fieldMapping, "settlement_id"));
        salesData.setSku(getMappedValue(rowData, fieldMapping, "sku"));
        salesData.setDescription(getMappedValue(rowData, fieldMapping, "description"));
        salesData.setFulfillment(getMappedValue(rowData, fieldMapping, "fulfillment"));
        salesData.setOrderCity(getMappedValue(rowData, fieldMapping, "order_city"));
        salesData.setOrderState(getMappedValue(rowData, fieldMapping, "order_state"));
        salesData.setOrderPostal(getMappedValue(rowData, fieldMapping, "order_postal"));
        salesData.setTaxCollectionModel(getMappedValue(rowData, fieldMapping, "tax_collection_model"));

        // Parse quantity
        String quantityStr = getMappedValue(rowData, fieldMapping, "quantity");
        if (StringUtils.hasText(quantityStr)) {
            try {
                salesData.setQuantity(Integer.parseInt(quantityStr.replace(",", "")));
            } catch (NumberFormatException e) {
                salesData.setQuantity(0);
            }
        }

        // Parse amount fields
        salesData.setProductSales(parseDecimalField(rowData, fieldMapping, "product_sales", siteCode));
        salesData.setProductSalesTax(parseDecimalField(rowData, fieldMapping, "product_sales_tax", siteCode));
        salesData.setShippingCredits(parseDecimalField(rowData, fieldMapping, "shipping_credits", siteCode));
        salesData.setShippingCreditsTax(parseDecimalField(rowData, fieldMapping, "shipping_credits_tax", siteCode));
        salesData.setGiftWrapCredits(parseDecimalField(rowData, fieldMapping, "gift_wrap_credits", siteCode));
        salesData.setGiftWrapCreditsTax(parseDecimalField(rowData, fieldMapping, "gift_wrap_credits_tax", siteCode));
        salesData.setPromotionalRebates(parseDecimalField(rowData, fieldMapping, "promotional_rebates", siteCode));
        salesData.setPromotionalRebatesTax(parseDecimalField(rowData, fieldMapping, "promotional_rebates_tax", siteCode));
        salesData.setMarketplaceWithheldTax(parseDecimalField(rowData, fieldMapping, "marketplace_withheld_tax", siteCode));
        salesData.setSellingFees(parseDecimalField(rowData, fieldMapping, "selling_fees", siteCode));
        salesData.setFbaFees(parseDecimalField(rowData, fieldMapping, "fba_fees", siteCode));
        salesData.setOtherTransactionFees(parseDecimalField(rowData, fieldMapping, "other_transaction_fees", siteCode));
        salesData.setOther(parseDecimalField(rowData, fieldMapping, "other", siteCode));
        salesData.setTotal(parseDecimalField(rowData, fieldMapping, "total", siteCode));

        return salesData;
    }

    /**
     * Parse decimal field from row data
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, Map<String, String> fieldMapping, 
                                         String targetField, String siteCode) {
        String value = getMappedValue(rowData, fieldMapping, targetField);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get mapped value from row data using field mapping
     */
    private String getMappedValue(Map<String, String> rowData, Map<String, String> fieldMapping, String targetField) {
        String sourceField = fieldMapping.get(targetField);
        if (sourceField != null) {
            return rowData.getOrDefault(sourceField.toLowerCase(), "");
        }

        // Try direct match with common field names
        String[] commonNames = getCommonFieldNames(targetField);
        for (String name : commonNames) {
            String value = rowData.get(name.toLowerCase());
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    /**
     * Get common field names for a target field
     */
    private String[] getCommonFieldNames(String targetField) {
        return switch (targetField) {
            case "order_id" -> new String[]{"order id", "order-id", "orderid", "bestellnummer"};
            case "date_time" -> new String[]{"date/time", "date", "datetime", "datum/uhrzeit"};
            case "transaction_type" -> new String[]{"type", "transaction type", "typ"};
            case "settlement_id" -> new String[]{"settlement id", "settlementid", "abrechnungsnummer"};
            case "sku" -> new String[]{"sku", "asin"};
            case "description" -> new String[]{"description", "product name", "beschreibung"};
            case "quantity" -> new String[]{"quantity", "qty", "menge"};
            case "total" -> new String[]{"total", "sum", "gesamt"};
            case "product_sales" -> new String[]{"product sales", "productsales"};
            case "shipping_credits" -> new String[]{"shipping credits", "shippingcredits"};
            default -> new String[]{targetField};
        };
    }

    /**
     * Get field mapping configuration for site and data type
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getFieldMapping(String siteCode, String dataType) {
        LambdaQueryWrapper<FieldMappingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMappingTemplate::getSiteCode, siteCode)
                .eq(FieldMappingTemplate::getDataType, dataType)
                .eq(FieldMappingTemplate::getIsDefault, 1);

        FieldMappingTemplate template = fieldMappingTemplateMapper.selectOne(wrapper);

        if (template != null && template.getMappingConfig() != null) {
            try {
                return objectMapper.readValue(template.getMappingConfig(),
                        new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse field mapping config", e);
            }
        }

        // Return empty map if no mapping found - will use default field names
        return new HashMap<>();
    }

    /**
     * Get transaction type mapping for site
     */
    private Map<String, String> getTransactionTypeMapping(String siteCode) {
        Map<String, String> mapping = new HashMap<>();

        // Get site-specific mappings
        LambdaQueryWrapper<TransactionTypeMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionTypeMapping::getSiteCode, siteCode)
                .eq(TransactionTypeMapping::getStatus, 1);
        List<TransactionTypeMapping> siteList = transactionTypeMappingMapper.selectList(wrapper);

        for (TransactionTypeMapping m : siteList) {
            mapping.put(m.getOriginalType(), m.getStandardCategory());
        }

        // Get universal mappings
        wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(TransactionTypeMapping::getSiteCode)
                .eq(TransactionTypeMapping::getStatus, 1);
        List<TransactionTypeMapping> universalList = transactionTypeMappingMapper.selectList(wrapper);

        for (TransactionTypeMapping m : universalList) {
            mapping.putIfAbsent(m.getOriginalType(), m.getStandardCategory());
        }

        return mapping;
    }

    /**
     * Check if sales data is duplicate
     * Uses order_id + transaction_category as unique key
     */
    private boolean isDuplicate(SalesData salesData) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getOrderId, salesData.getOrderId())
                .eq(SalesData::getTransactionCategory, salesData.getTransactionCategory());
        return salesDataMapper.selectCount(wrapper) > 0;
    }

    /**
     * Generate unique batch number
     */
    private String generateBatchNo() {
        return "SALES-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalesData entity = salesDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Sales data not found");
        }

        salesDataMapper.deleteById(id);
        log.info("Deleted sales data: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        salesDataMapper.deleteBatchIds(ids);
        log.info("Batch deleted sales data: ids={}", ids);
    }

    @Override
    public Map<String, Object> getSummary(String siteCode, String transactionCategory, String startDate, String endDate) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(transactionCategory)) {
            wrapper.eq(SalesData::getTransactionCategory, transactionCategory);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDateTime start = java.time.LocalDate.parse(startDate).atStartOfDay();
                wrapper.ge(SalesData::getTransactionDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDateTime end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                wrapper.le(SalesData::getTransactionDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", dataList.size());
        summary.put("totalQuantity", dataList.stream()
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum());
        summary.put("totalProductSales", dataList.stream()
                .map(d -> d.getProductSales() != null ? d.getProductSales() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalSellingFees", dataList.stream()
                .map(d -> d.getSellingFees() != null ? d.getSellingFees() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalFbaFees", dataList.stream()
                .map(d -> d.getFbaFees() != null ? d.getFbaFees() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalOtherFees", dataList.stream()
                .map(d -> d.getOtherTransactionFees() != null ? d.getOtherTransactionFees() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalAmount", dataList.stream()
                .map(d -> d.getTotal() != null ? d.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("currencyCode", dataList.isEmpty() ? "USD" : dataList.get(0).getCurrencyCode());

        return summary;
    }

    @Override
    public List<Map<String, Object>> getStatByType(String siteCode, String startDate, String endDate) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDateTime start = java.time.LocalDate.parse(startDate).atStartOfDay();
                wrapper.ge(SalesData::getTransactionDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDateTime end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                wrapper.le(SalesData::getTransactionDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        // Group by transaction category
        Map<String, List<SalesData>> grouped = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getTransactionCategory() != null ? d.getTransactionCategory() : "other"
                ));

        List<Map<String, Object>> stats = new ArrayList<>();
        int totalCount = dataList.size();

        for (Map.Entry<String, List<SalesData>> entry : grouped.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("transactionCategory", entry.getKey());
            stat.put("count", entry.getValue().size());
            stat.put("totalAmount", entry.getValue().stream()
                    .map(d -> d.getTotal() != null ? d.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stat.put("percentage", totalCount > 0 ? (entry.getValue().size() * 100.0 / totalCount) : 0);
            stats.add(stat);
        }

        return stats;
    }

    @Override
    public void exportData(String siteCode, String transactionCategory, String startDate, String endDate,
                           jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(transactionCategory)) {
            wrapper.eq(SalesData::getTransactionCategory, transactionCategory);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDateTime start = java.time.LocalDate.parse(startDate).atStartOfDay();
                wrapper.ge(SalesData::getTransactionDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDateTime end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                wrapper.le(SalesData::getTransactionDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        wrapper.orderByDesc(SalesData::getTransactionDate);
        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        try {
            String fileName = "sales_data_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sales Data");

                // Create header row
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] headers = {"订单号", "站点", "交易日期", "交易类型", "交易分类", "SKU",
                        "数量", "产品销售", "销售费用", "FBA费用", "合计", "货币"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Fill data rows
                int rowNum = 1;
                for (SalesData data : dataList) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data.getOrderId());
                    row.createCell(1).setCellValue(data.getSiteCode());
                    row.createCell(2).setCellValue(data.getTransactionDate() != null ? data.getTransactionDate().toString() : "");
                    row.createCell(3).setCellValue(data.getTransactionType());
                    row.createCell(4).setCellValue(data.getTransactionCategory());
                    row.createCell(5).setCellValue(data.getSku());
                    row.createCell(6).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
                    row.createCell(7).setCellValue(data.getProductSales() != null ? data.getProductSales().doubleValue() : 0);
                    row.createCell(8).setCellValue(data.getSellingFees() != null ? data.getSellingFees().doubleValue() : 0);
                    row.createCell(9).setCellValue(data.getFbaFees() != null ? data.getFbaFees().doubleValue() : 0);
                    row.createCell(10).setCellValue(data.getTotal() != null ? data.getTotal().doubleValue() : 0);
                    row.createCell(11).setCellValue(data.getCurrencyCode());
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (java.io.IOException e) {
            log.error("Failed to export sales data", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }
}
