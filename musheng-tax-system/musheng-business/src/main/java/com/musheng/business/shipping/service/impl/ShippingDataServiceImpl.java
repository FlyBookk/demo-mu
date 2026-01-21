package com.musheng.business.shipping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.service.csv.CsvHeaderResult;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.business.shipping.service.ShippingDataService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
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
 * Shipping Data Service Implementation
 * Implements CSV import with duplicate check (BUG-007)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingDataServiceImpl implements ShippingDataService {

    private final ShippingDataMapper shippingDataMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final ImportRecordMapper importRecordMapper;
    private final CsvParseServiceImpl csvParseService;

    @Override
    public Page<ShippingData> list(String siteCode, String trackingNumber, String orderId, int page, int size) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(trackingNumber)) {
            wrapper.like(ShippingData::getTrackingNumber, trackingNumber);
        }
        if (StringUtils.hasText(orderId)) {
            wrapper.like(ShippingData::getOrderId, orderId);
        }

        wrapper.orderByDesc(ShippingData::getCreateTime);

        return shippingDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public ShippingData getById(Long id) {
        ShippingData entity = shippingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Shipping data not found");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("Importing shipping data: fileName={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;

        // Create import record
        ImportRecord importRecord = new ImportRecord();
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("shipping");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // Detect headers and charset
            log.info("Detecting CSV headers...");
            CsvHeaderResult headerResult = csvParseService.parseHeaders(file);
            log.info("Headers detected: language={}, charset={}, headerRowIndex={}, columns={}",
                    headerResult.getHeaderLanguage(),
                    headerResult.getCharset(),
                    headerResult.getHeaderRowIndex(),
                    headerResult.getHeaders().size());

            List<String> headers = headerResult.getHeaders();
            int headerRowIndex = headerResult.getHeaderRowIndex();
            String charsetName = headerResult.getCharset();
            java.nio.charset.Charset charset = charsetName != null ?
                    java.nio.charset.Charset.forName(charsetName) : StandardCharsets.UTF_8;

            // Step 1: Parse all records into a list (避免N+1查询)
            List<ShippingData> parsedRecords = new ArrayList<>();
            Map<Integer, String> rowErrorMap = new HashMap<>(); // Track errors by row number

            // Pre-load all marketplace configurations to avoid N+1 queries
            List<Marketplace> allMarketplaces = marketplaceMapper.selectList(null);
            Map<String, Marketplace> marketplaceMap = allMarketplaces.stream()
                    .collect(java.util.stream.Collectors.toMap(Marketplace::getSiteCode, m -> m));
            log.info("Pre-loaded {} marketplace configurations", marketplaceMap.size());

            // Reset input stream and skip to data rows using detected charset
            try (BufferedReader dataReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), charset))) {

                // Skip header rows
                for (int i = 0; i <= headerRowIndex; i++) {
                    dataReader.readLine();
                }

                CSVParser parser = CSVFormat.DEFAULT.parse(dataReader);

                for (CSVRecord record : parser) {
                    totalCount++;
                    try {
                        ShippingData shippingData = parseShippingRecord(record, headers, totalCount, marketplaceMap);

                        if (shippingData != null) {
                            shippingData.setImportBatchId(importRecord.getId());
                            parsedRecords.add(shippingData);
                        }
                    } catch (Exception e) {
                        failCount++;
                        rowErrorMap.put(totalCount, e.getMessage());
                        log.warn("Failed to parse row {}: {}", totalCount, e.getMessage());
                    }
                }
            }

            log.info("Parsed {} valid records from {} total rows", parsedRecords.size(), totalCount);

            // Step 2: Batch check for duplicates (单次查询避免N+1)
            List<ShippingData> toInsert = new ArrayList<>();
            List<ShippingData> duplicates = new ArrayList<>();

            if (!parsedRecords.isEmpty()) {
                // Build batch duplicate check query
                Set<String> existingKeys = batchCheckDuplicates(parsedRecords);

                for (ShippingData data : parsedRecords) {
                    String uniqueKey = buildUniqueKey(data);
                    if (existingKeys.contains(uniqueKey)) {
                        duplicates.add(data);
                        duplicateCount++;
                    } else {
                        toInsert.add(data);
                    }
                }
            }

            log.info("Duplicate check completed: {} to insert, {} duplicates", toInsert.size(), duplicates.size());

            // Step 3: Batch insert (使用MyBatis Plus批量插入)
            if (!toInsert.isEmpty()) {
                int batchSize = 500; // 每批500条
                for (int i = 0; i < toInsert.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, toInsert.size());
                    List<ShippingData> batch = toInsert.subList(i, end);

                    // MyBatis Plus batch insert
                    batch.forEach(shippingDataMapper::insert);
                    successCount += batch.size();

                    log.info("Batch inserted records {}-{}/{}", i + 1, end, toInsert.size());
                }
            }

            // Add duplicate errors to error list
            for (ShippingData dup : duplicates) {
                if (errors.size() < 10) { // Limit error messages
                    errors.add(String.format("Duplicate: order=%s, site=%s, tracking=%s",
                            dup.getOrderId(), dup.getSiteCode(), dup.getTrackingNumber()));
                }
            }

            // Add parsing errors to error list
            for (Map.Entry<Integer, String> entry : rowErrorMap.entrySet()) {
                if (errors.size() < 10) {
                    errors.add(String.format("Row %d: %s", entry.getKey(), entry.getValue()));
                }
            }

            // Update import record
            importRecord.setTotalCount(totalCount);
            importRecord.setSuccessCount(successCount);
            importRecord.setFailCount(failCount);
            importRecord.setImportStatus(failCount == 0 && duplicateCount == 0 ? "success" :
                    (successCount > 0 ? "partial" : "fail"));
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors));
            }
            importRecordMapper.updateById(importRecord);

        } catch (BusinessException e) {
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw e;
        } catch (Exception e) {
            log.error("Failed to import shipping data", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Failed to parse file: " + e.getMessage());
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("duplicateCount", duplicateCount);
        result.put("errors", errors);
        result.put("batchNo", importRecord.getBatchNo());

        log.info("Shipping data import completed: total={}, success={}, fail={}, duplicate={}",
                totalCount, successCount, failCount, duplicateCount);

        return result;
    }

    /**
     * Parse a single CSV record into ShippingData entity (自动识别销售渠道)
     * @param marketplaceMap Pre-loaded marketplace configurations to avoid N+1 queries
     */
    private ShippingData parseShippingRecord(CSVRecord record, List<String> headers, int rowNum,
                                             Map<String, Marketplace> marketplaceMap) {

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), record.size()); i++) {
            rowData.put(headers.get(i).toLowerCase().trim(), record.get(i).trim());
        }

        // 从CSV中读取销售渠道
        String salesChannel = getFieldValue(rowData, "sales channel", "销售渠道", "saleschannel");
        if (!StringUtils.hasText(salesChannel)) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Missing sales channel");
        }

        // 将销售渠道映射为站点编码
        String siteCode = mapSalesChannelToSiteCode(salesChannel);

        // 从预加载的配置中获取marketplace (避免N+1查询)
        Marketplace marketplace = marketplaceMap.get(siteCode);

        if (marketplace == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found for site: " + siteCode);
        }

        ShippingData shippingData = new ShippingData();
        shippingData.setSiteCode(siteCode);
        shippingData.setMarketplace(marketplace.getMarketplaceId());

        // Parse currency code (优先使用CSV中的货币，否则使用marketplace默认值)
        String currencyCode = getFieldValue(rowData, "currency", "货币", "currencycode");
        if (StringUtils.hasText(currencyCode)) {
            shippingData.setCurrencyCode(currencyCode);
        } else {
            shippingData.setCurrencyCode(marketplace.getCurrencyCode());
        }

        // Parse order ID (支持中文列名)
        String orderId = getFieldValue(rowData, "order id", "order-id", "orderid", "bestellnummer", "亚马逊订单编号");
        if (!StringUtils.hasText(orderId)) {
            return null; // Skip empty rows
        }
        shippingData.setOrderId(orderId);

        // Parse ship date (支持中文列名)
        String dateStr = getFieldValue(rowData, "ship date", "shipment date", "shipped date", "versanddatum", "配送日期");
        if (StringUtils.hasText(dateStr)) {
            LocalDateTime dateTime = csvParseService.parseDate(dateStr, siteCode);
            if (dateTime != null) {
                shippingData.setShipDate(dateTime.toLocalDate());
            }
        }

        // Parse tracking number (支持中文列名)
        shippingData.setTrackingNumber(getFieldValue(rowData, "tracking number", "tracking", "trackingnumber", "追踪编码"));

        // Parse carrier (支持中文列名)
        shippingData.setCarrier(getFieldValue(rowData, "carrier", "shipping carrier", "versandunternehmen", "承运人"));

        // Parse SKU (支持中文列名)
        shippingData.setSku(getFieldValue(rowData, "sku", "asin", "product sku", "卖家 sku"));

        // Parse quantity (支持中文列名)
        String quantityStr = getFieldValue(rowData, "quantity", "qty", "menge", "已发货数量");
        if (StringUtils.hasText(quantityStr)) {
            try {
                shippingData.setQuantity(Integer.parseInt(quantityStr.replace(",", "")));
            } catch (NumberFormatException e) {
                shippingData.setQuantity(0);
            }
        }

        // Parse price fields (支持中文列名)
        shippingData.setProductPrice(parseDecimalFieldMulti(rowData, siteCode, "product price", "item price", "商品价格"));
        shippingData.setProductTax(parseDecimalFieldMulti(rowData, siteCode, "product tax", "item tax", "商品税"));
        shippingData.setShippingPrice(parseDecimalFieldMulti(rowData, siteCode, "shipping price", "shipping", "运费"));
        shippingData.setShippingTax(parseDecimalFieldMulti(rowData, siteCode, "shipping tax", "运费税"));
        shippingData.setGiftWrapPrice(parseDecimalFieldMulti(rowData, siteCode, "gift wrap price", "giftwrap", "礼品包装价格"));
        shippingData.setGiftWrapTax(parseDecimalFieldMulti(rowData, siteCode, "gift wrap tax", "礼品包装税费"));
        shippingData.setProductPromotionDiscount(parseDecimalFieldMulti(rowData, siteCode, "item promotion discount", "product promotion", "商品促销折扣"));
        shippingData.setShipmentPromotionDiscount(parseDecimalFieldMulti(rowData, siteCode, "ship promotion discount", "shipment promotion", "货件促销折扣"));
        shippingData.setShippingCost(parseDecimalFieldMulti(rowData, siteCode, "shipping cost", "cost"));

        return shippingData;
    }

    /**
     * 将销售渠道映射为站点编码
     * 例如: amazon.de -> DE, amazon.co.uk -> UK, amazon.fr -> FR
     */
    private String mapSalesChannelToSiteCode(String salesChannel) {
        if (!StringUtils.hasText(salesChannel)) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Sales channel is empty");
        }

        String channel = salesChannel.toLowerCase().trim();

        // 映射规则
        if (channel.contains("amazon.de")) {
            return "DE";
        } else if (channel.contains("amazon.co.uk") || channel.contains("amazon.uk")) {
            return "UK";
        } else if (channel.contains("amazon.fr")) {
            return "FR";
        } else if (channel.contains("amazon.it")) {
            return "IT";
        } else if (channel.contains("amazon.es")) {
            return "ES";
        } else if (channel.contains("amazon.nl")) {
            return "NL";
        } else if (channel.contains("amazon.pl")) {
            return "PL";
        } else if (channel.contains("amazon.se")) {
            return "SE";
        } else if (channel.contains("amazon.ca")) {
            return "CA";
        } else if (channel.contains("amazon.com")) {
            return "US";
        } else {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                    "Unsupported sales channel: " + salesChannel);
        }
    }

    /**
     * Parse decimal field from row data
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, String fieldName, String siteCode) {
        String value = getFieldValue(rowData, fieldName);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse decimal field with alternate names
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, String fieldName, String altName, String siteCode) {
        String value = getFieldValue(rowData, fieldName, altName);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse decimal field with multiple possible names (支持多个列名)
     */
    private BigDecimal parseDecimalFieldMulti(Map<String, String> rowData, String siteCode, String... possibleNames) {
        String value = getFieldValue(rowData, possibleNames);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get field value from row data with multiple possible column names
     */
    private String getFieldValue(Map<String, String> rowData, String... possibleNames) {
        for (String name : possibleNames) {
            String value = rowData.get(name.toLowerCase());
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    /**
     * Batch check for duplicates to avoid N+1 query problem
     * Returns a set of unique keys that already exist in database
     * Unique constraint: order_id + site_code + tracking_number
     */
    private Set<String> batchCheckDuplicates(List<ShippingData> records) {
        if (records.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> existingKeys = new HashSet<>();

        // Build conditions for batch query
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        // Group records by site code for efficient querying
        Map<String, List<ShippingData>> bySite = records.stream()
                .filter(r -> StringUtils.hasText(r.getOrderId()))
                .collect(java.util.stream.Collectors.groupingBy(ShippingData::getSiteCode));

        for (Map.Entry<String, List<ShippingData>> entry : bySite.entrySet()) {
            String siteCode = entry.getKey();
            List<ShippingData> siteRecords = entry.getValue();

            // Get all order IDs for this site
            Set<String> orderIds = siteRecords.stream()
                    .map(ShippingData::getOrderId)
                    .filter(StringUtils::hasText)
                    .collect(java.util.stream.Collectors.toSet());

            if (orderIds.isEmpty()) {
                continue;
            }

            // Query existing records for this site
            LambdaQueryWrapper<ShippingData> siteWrapper = new LambdaQueryWrapper<>();
            siteWrapper.eq(ShippingData::getSiteCode, siteCode)
                    .in(ShippingData::getOrderId, orderIds)
                    .select(ShippingData::getOrderId, ShippingData::getSiteCode, ShippingData::getTrackingNumber);

            List<ShippingData> existing = shippingDataMapper.selectList(siteWrapper);

            // Build set of existing unique keys
            for (ShippingData existingData : existing) {
                existingKeys.add(buildUniqueKey(existingData));
            }
        }

        log.info("Batch duplicate check: found {} existing records", existingKeys.size());
        return existingKeys;
    }

    /**
     * Build unique key for duplicate detection
     * Format: orderId|siteCode|trackingNumber
     */
    private String buildUniqueKey(ShippingData data) {
        String orderId = data.getOrderId() != null ? data.getOrderId() : "";
        String siteCode = data.getSiteCode() != null ? data.getSiteCode() : "";
        String trackingNumber = data.getTrackingNumber() != null ? data.getTrackingNumber() : "";
        return orderId + "|" + siteCode + "|" + trackingNumber;
    }

    /**
     * Generate unique batch number
     */
    private String generateBatchNo() {
        return "SHIP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShippingData entity = shippingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Shipping data not found");
        }

        shippingDataMapper.deleteById(id);
        log.info("Deleted shipping data: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        shippingDataMapper.deleteBatchIds(ids);
        log.info("Batch deleted shipping data: ids={}", ids);
    }

    @Override
    public Map<String, Object> getSummary(String siteCode, String startDate, String endDate) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(ShippingData::getShipDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(ShippingData::getShipDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        List<ShippingData> dataList = shippingDataMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", dataList.size());
        summary.put("totalQuantity", dataList.stream()
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum());
        summary.put("totalProductPrice", dataList.stream()
                .map(d -> d.getProductPrice() != null ? d.getProductPrice() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("totalShippingPrice", dataList.stream()
                .map(d -> d.getShippingPrice() != null ? d.getShippingPrice() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("totalShippingCost", dataList.stream()
                .map(d -> d.getShippingCost() != null ? d.getShippingCost() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("totalRevenueTotal", dataList.stream()
                .map(d -> d.getRevenueTotal() != null ? d.getRevenueTotal() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("currencyCode", dataList.isEmpty() ? "USD" : dataList.get(0).getCurrencyCode());

        return summary;
    }

    @Override
    public void exportData(String siteCode, String startDate, String endDate,
                           jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(ShippingData::getShipDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(ShippingData::getShipDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        wrapper.orderByDesc(ShippingData::getShipDate);
        List<ShippingData> dataList = shippingDataMapper.selectList(wrapper);

        try {
            String fileName = "shipping_data_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Shipping Data");

                // Create header row
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] headers = {"订单号", "站点", "发货日期", "SKU", "数量", "产品价格",
                        "运费价格", "运费成本", "收入合计", "货币", "承运商", "追踪号"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Fill data rows
                int rowNum = 1;
                for (ShippingData data : dataList) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data.getOrderId());
                    row.createCell(1).setCellValue(data.getSiteCode());
                    row.createCell(2).setCellValue(data.getShipDate() != null ? data.getShipDate().toString() : "");
                    row.createCell(3).setCellValue(data.getSku());
                    row.createCell(4).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
                    row.createCell(5).setCellValue(data.getProductPrice() != null ? data.getProductPrice().doubleValue() : 0);
                    row.createCell(6).setCellValue(data.getShippingPrice() != null ? data.getShippingPrice().doubleValue() : 0);
                    row.createCell(7).setCellValue(data.getShippingCost() != null ? data.getShippingCost().doubleValue() : 0);
                    row.createCell(8).setCellValue(data.getRevenueTotal() != null ? data.getRevenueTotal().doubleValue() : 0);
                    row.createCell(9).setCellValue(data.getCurrencyCode());
                    row.createCell(10).setCellValue(data.getCarrier());
                    row.createCell(11).setCellValue(data.getTrackingNumber());
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (java.io.IOException e) {
            log.error("Failed to export shipping data", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }
}
