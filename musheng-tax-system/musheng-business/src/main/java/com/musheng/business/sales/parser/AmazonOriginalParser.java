package com.musheng.business.sales.parser;

import com.musheng.business.sales.entity.SalesData;
import com.musheng.common.enums.SalesSourceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 亚马逊原始数据解析器
 * 处理按国家分散的CSV文件，每行是一笔完整订单信息
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Slf4j
@Component
public class AmazonOriginalParser implements SalesDataParser {
    
    /**
     * 默认跳过的表头行数（亚马逊原始数据通常有7-8行说明性内容）
     */
    private static final int DEFAULT_SKIP_ROWS = 7;
    
    /**
     * marketplace字段名（用于自动识别站点）
     */
    private static final String MARKETPLACE_FIELD = "marketplace";
    
    @Override
    public SalesSourceType getSourceType() {
        return SalesSourceType.ORIGINAL;
    }
    
    @Override
    public ParseResult parse(ParseContext context) {
        log.info("开始解析亚马逊原始数据: {}", context.getFilePath());
        
        List<SalesData> dataList = new ArrayList<>();
        List<ParseResult.ParseError> errors = new ArrayList<>();
        Set<String> detectedSites = new HashSet<>();
        int totalRows = 0;
        int successRows = 0;
        int failRows = 0;
        
        boolean isPreview = Boolean.TRUE.equals(context.getPreviewMode());
        int previewLimit = context.getPreviewRows() != null ? context.getPreviewRows() : 10;
        List<Map<String, Object>> previewData = new ArrayList<>();
        
        try {
            Charset charset = context.getEncoding() != null 
                    ? Charset.forName(context.getEncoding()) 
                    : StandardCharsets.UTF_8;
            
            int headerRow = context.getHeaderRow() != null ? context.getHeaderRow() : DEFAULT_SKIP_ROWS + 1;
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                // 跳过说明性行
                for (int i = 1; i < headerRow; i++) {
                    reader.readLine();
                }
                
                // 解析CSV
                CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();
                
                try (CSVParser parser = new CSVParser(reader, format)) {
                    Map<String, Integer> headerMap = parser.getHeaderMap();
                    log.info("解析到表头字段: {}", headerMap.keySet());
                    
                    for (CSVRecord record : parser) {
                        totalRows++;
                        
                        // 预览模式限制
                        if (isPreview && successRows >= previewLimit) {
                            break;
                        }
                        
                        try {
                            // 获取marketplace并识别站点
                            String marketplace = getFieldValue(record, headerMap, MARKETPLACE_FIELD);
                            String recordSiteCode = SiteCodeResolver.getSiteCode(marketplace);
                            if (recordSiteCode != null) {
                                detectedSites.add(recordSiteCode);
                            }
                            
                            // 使用上下文中的站点编码，如果未指定则使用检测到的
                            String siteCode = context.getSiteCode() != null ? context.getSiteCode() : recordSiteCode;
                            
                            // 解析数据行
                            SalesData salesData = parseRecord(record, headerMap, siteCode, context);
                            salesData.setImportBatchId(context.getImportBatchId());
                            
                            dataList.add(salesData);
                            successRows++;
                            
                            // 预览模式生成Map数据
                            if (isPreview) {
                                previewData.add(convertToMap(salesData));
                            }
                            
                        } catch (Exception e) {
                            failRows++;
                            errors.add(ParseResult.ParseError.builder()
                                    .row((int) record.getRecordNumber())
                                    .message(e.getMessage())
                                    .build());
                            log.warn("解析第{}行失败: {}", record.getRecordNumber(), e.getMessage());
                        }
                    }
                }
            }
            
            ParseResult result = ParseResult.builder()
                    .success(true)
                    .totalRows(totalRows)
                    .successRows(successRows)
                    .failRows(failRows)
                    .skipRows(0)
                    .dataList(dataList)
                    .previewData(isPreview ? previewData : null)
                    .errors(errors)
                    .warnings(new ArrayList<>())
                    .detectedSiteCodes(new ArrayList<>(detectedSites))
                    .build();
            
            log.info("亚马逊原始数据解析完成: 总行数={}, 成功={}, 失败={}, 检测到站点={}", 
                    totalRows, successRows, failRows, detectedSites);
            
            return result;
            
        } catch (IOException e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
            return ParseResult.fail("读取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public ParseResult parse(String content, ParseContext context, int maxRows) {
        log.info("开始解析亚马逊原始数据（从内容）: maxRows={}", maxRows);
        
        List<SalesData> dataList = new ArrayList<>();
        List<ParseResult.ParseError> errors = new ArrayList<>();
        Set<String> detectedSites = new HashSet<>();
        int totalRows = 0;
        int successRows = 0;
        int failRows = 0;
        
        try {
            // 使用更健壮的换行符分割（兼容 Windows/Unix/Mac）
            String[] lines = content.split("\\r?\\n|\\r");
            
            // 找到表头行 - 使用更精确的检测逻辑
            int headerRowIndex = -1;
            for (int i = 0; i < Math.min(15, lines.length); i++) {
                String line = lines[i].trim();
                String lineLower = line.toLowerCase();
                
                // 检查是否是 CSV 格式行（以引号开头，包含多个字段）
                boolean isCsvFormat = line.startsWith("\"") && line.contains("\",\"");
                if (!isCsvFormat) {
                    continue;
                }
                
                // 表头通常包含时间字段和数据字段
                boolean hasDateField = lineLower.contains("\"date/time\"") || 
                    lineLower.contains("\"datum/uhrzeit\"");
                boolean hasDataFields = lineLower.contains("\"sku\"") || 
                    lineLower.contains("\"order id\"") ||
                    lineLower.contains("\"settlement id\"") ||
                    lineLower.contains("\"abrechnungsnummer\"");
                
                if (hasDateField && hasDataFields) {
                    headerRowIndex = i;
                    break;
                }
            }
            
            // 如果严格匹配失败，尝试宽松匹配
            if (headerRowIndex < 0) {
                for (int i = 0; i < Math.min(15, lines.length); i++) {
                    String lineLower = lines[i].toLowerCase();
                    if (lineLower.contains("marketplace") && lineLower.contains("sku")) {
                        headerRowIndex = i;
                        break;
                    }
                }
            }
            
            if (headerRowIndex < 0) {
                return ParseResult.fail("未找到有效的表头行");
            }
            
            log.info("检测到表头行索引: {}", headerRowIndex);
            
            // 构建从表头开始的内容
            StringBuilder csvContent = new StringBuilder();
            for (int i = headerRowIndex; i < lines.length; i++) {
                csvContent.append(lines[i]).append("\n");
            }
            
            // 解析CSV
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();
            
            try (CSVParser parser = CSVParser.parse(csvContent.toString(), format)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                log.info("解析到表头字段: {}", headerMap.keySet());
                
                for (CSVRecord record : parser) {
                    totalRows++;
                    
                    // 限制解析行数
                    if (successRows >= maxRows) {
                        break;
                    }
                    
                    try {
                        // 获取marketplace并识别站点
                        String marketplace = getFieldValue(record, headerMap, MARKETPLACE_FIELD);
                        String recordSiteCode = SiteCodeResolver.getSiteCode(marketplace);
                        if (recordSiteCode != null) {
                            detectedSites.add(recordSiteCode);
                        }
                        
                        // 使用上下文中的站点编码，如果未指定则使用检测到的
                        String siteCode = context.getSiteCode() != null ? context.getSiteCode() : recordSiteCode;
                        
                        // 解析数据行
                        SalesData salesData = parseRecord(record, headerMap, siteCode, context);
                        salesData.setImportBatchId(context.getImportBatchId());
                        
                        dataList.add(salesData);
                        successRows++;
                        
                    } catch (Exception e) {
                        failRows++;
                        errors.add(ParseResult.ParseError.builder()
                                .row((int) record.getRecordNumber())
                                .message(e.getMessage())
                                .build());
                        log.warn("解析第{}行失败: {}", record.getRecordNumber(), e.getMessage());
                    }
                }
            }
            
            // 计算实际总行数
            int actualTotalRows = lines.length - headerRowIndex - 1;
            
            ParseResult result = ParseResult.builder()
                    .success(true)
                    .totalRows(actualTotalRows)
                    .successRows(successRows)
                    .failRows(failRows)
                    .skipRows(0)
                    .dataList(dataList)
                    .errors(errors)
                    .warnings(new ArrayList<>())
                    .detectedSiteCodes(new ArrayList<>(detectedSites))
                    .build();
            
            log.info("亚马逊原始数据解析完成: 总行数={}, 成功={}, 失败={}", actualTotalRows, successRows, failRows);
            
            return result;
            
        } catch (IOException e) {
            log.error("解析内容失败: {}", e.getMessage(), e);
            return ParseResult.fail("解析内容失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> detectSiteCodes(ParseContext context) {
        Set<String> sites = new HashSet<>();
        
        try {
            Charset charset = context.getEncoding() != null 
                    ? Charset.forName(context.getEncoding()) 
                    : StandardCharsets.UTF_8;
            
            int headerRow = context.getHeaderRow() != null ? context.getHeaderRow() : DEFAULT_SKIP_ROWS + 1;
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                // 跳过说明性行
                for (int i = 1; i < headerRow; i++) {
                    reader.readLine();
                }
                
                CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();
                
                try (CSVParser parser = new CSVParser(reader, format)) {
                    Map<String, Integer> headerMap = parser.getHeaderMap();
                    
                    int count = 0;
                    for (CSVRecord record : parser) {
                        String marketplace = getFieldValue(record, headerMap, MARKETPLACE_FIELD);
                        String siteCode = SiteCodeResolver.getSiteCode(marketplace);
                        if (siteCode != null) {
                            sites.add(siteCode);
                        }
                        
                        // 只检查前100行
                        if (++count >= 100) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("检测站点失败: {}", e.getMessage());
        }
        
        return new ArrayList<>(sites);
    }
    
    @Override
    public boolean validateFormat(ParseContext context) {
        try {
            Charset charset = context.getEncoding() != null 
                    ? Charset.forName(context.getEncoding()) 
                    : StandardCharsets.UTF_8;
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                // 跳过说明行
                int headerRow = context.getHeaderRow() != null ? context.getHeaderRow() : DEFAULT_SKIP_ROWS + 1;
                for (int i = 1; i < headerRow; i++) {
                    reader.readLine();
                }
                
                // 读取表头行
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    return false;
                }
                
                // 检查是否包含关键字段
                String lowerHeader = headerLine.toLowerCase();
                return lowerHeader.contains("order id") || lowerHeader.contains("bestellnummer");
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 解析单条记录
     */
    private SalesData parseRecord(CSVRecord record, Map<String, Integer> headerMap, 
                                   String siteCode, ParseContext context) {
        SalesData data = new SalesData();
        
        Map<String, String> fieldMapping = context.getFieldMapping();
        
        // 基础字段
        data.setSiteCode(siteCode);
        data.setCurrencyCode(SiteCodeResolver.getCurrencyCode(siteCode));
        
        // 通过字段映射获取值
        data.setOrderId(getMappedValue(record, headerMap, fieldMapping, "orderId"));
        data.setSku(getMappedValue(record, headerMap, fieldMapping, "sku"));
        data.setDescription(getMappedValue(record, headerMap, fieldMapping, "description"));
        data.setSettlementId(getMappedValue(record, headerMap, fieldMapping, "settlementId"));
        data.setMarketplace(getMappedValue(record, headerMap, fieldMapping, "marketplace"));
        data.setFulfillment(getMappedValue(record, headerMap, fieldMapping, "fulfillment"));
        
        // 交易类型
        String transactionType = getMappedValue(record, headerMap, fieldMapping, "transactionType");
        data.setTransactionType(transactionType);
        
        // 映射交易分类
        if (context.getTransactionTypeMapping() != null && transactionType != null) {
            String category = context.getTransactionTypeMapping().get(transactionType);
            data.setTransactionCategory(category != null ? category : "other");
        }
        
        // 日期字段
        String dateStr = getMappedValue(record, headerMap, fieldMapping, "transactionDate");
        LocalDateTime parsedDate = DateConverter.parse(dateStr, siteCode);
        // 如果日期解析失败，使用当前时间作为默认值（确保数据库插入不会失败）
        data.setTransactionDate(parsedDate != null ? parsedDate : LocalDateTime.now());
        
        // 数量
        String quantityStr = getMappedValue(record, headerMap, fieldMapping, "quantity");
        if (quantityStr != null && !quantityStr.isEmpty()) {
            try {
                data.setQuantity(Integer.parseInt(quantityStr.trim()));
            } catch (NumberFormatException e) {
                data.setQuantity(0);
            }
        }
        
        // 金额字段
        data.setProductSales(parseAmount(record, headerMap, fieldMapping, "productSales", siteCode));
        data.setProductSalesTax(parseAmount(record, headerMap, fieldMapping, "productSalesTax", siteCode));
        data.setShippingCredits(parseAmount(record, headerMap, fieldMapping, "shippingCredits", siteCode));
        data.setShippingCreditsTax(parseAmount(record, headerMap, fieldMapping, "shippingCreditsTax", siteCode));
        data.setGiftWrapCredits(parseAmount(record, headerMap, fieldMapping, "giftWrapCredits", siteCode));
        data.setGiftWrapCreditsTax(parseAmount(record, headerMap, fieldMapping, "giftWrapCreditsTax", siteCode));
        data.setRegulatoryFee(parseAmount(record, headerMap, fieldMapping, "regulatoryFee", siteCode));
        data.setRegulatoryFeeTax(parseAmount(record, headerMap, fieldMapping, "regulatoryFeeTax", siteCode));
        data.setPromotionalRebates(parseAmount(record, headerMap, fieldMapping, "promotionalRebates", siteCode));
        data.setPromotionalRebatesTax(parseAmount(record, headerMap, fieldMapping, "promotionalRebatesTax", siteCode));
        data.setMarketplaceWithheldTax(parseAmount(record, headerMap, fieldMapping, "marketplaceWithheldTax", siteCode));
        data.setSellingFees(parseAmount(record, headerMap, fieldMapping, "sellingFees", siteCode));
        data.setFbaFees(parseAmount(record, headerMap, fieldMapping, "fbaFees", siteCode));
        data.setOtherTransactionFees(parseAmount(record, headerMap, fieldMapping, "otherTransactionFees", siteCode));
        data.setOther(parseAmount(record, headerMap, fieldMapping, "other", siteCode));
        data.setTotal(parseAmount(record, headerMap, fieldMapping, "total", siteCode));
        
        return data;
    }
    
    /**
     * 通过字段映射获取值
     * fieldMapping 格式：Map<sourceField, targetField>
     * 需要找出映射到指定 targetField 的 sourceField
     */
    private String getMappedValue(CSVRecord record, Map<String, Integer> headerMap,
                                   Map<String, String> fieldMapping, String targetField) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            return null;
        }
        
        // 遍历映射，找出目标字段对应的源字段
        String sourceField = null;
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            if (targetField.equals(entry.getValue())) {
                sourceField = entry.getKey();
                break;
            }
        }
        
        if (sourceField == null) {
            return null;
        }
        return getFieldValue(record, headerMap, sourceField);
    }
    
    /**
     * 获取字段值（支持大小写不敏感匹配）
     */
    private String getFieldValue(CSVRecord record, Map<String, Integer> headerMap, String fieldName) {
        if (fieldName == null) {
            return null;
        }
        
        // 精确匹配
        Integer index = headerMap.get(fieldName);
        if (index != null) {
            return record.get(index);
        }
        
        // 大小写不敏感匹配
        String lowerFieldName = fieldName.toLowerCase();
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            if (entry.getKey().toLowerCase().equals(lowerFieldName)) {
                return record.get(entry.getValue());
            }
        }
        
        return null;
    }
    
    /**
     * 解析金额字段
     */
    private BigDecimal parseAmount(CSVRecord record, Map<String, Integer> headerMap,
                                    Map<String, String> fieldMapping, String targetField, String siteCode) {
        String valueStr = getMappedValue(record, headerMap, fieldMapping, targetField);
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = NumberConverter.parse(valueStr, siteCode);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    /**
     * 将SalesData转换为Map（用于预览）
     */
    private Map<String, Object> convertToMap(SalesData data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", data.getOrderId());
        map.put("sku", data.getSku());
        map.put("transactionDate", data.getTransactionDate());
        map.put("siteCode", data.getSiteCode());
        map.put("transactionType", data.getTransactionType());
        map.put("transactionCategory", data.getTransactionCategory());
        map.put("quantity", data.getQuantity());
        map.put("productSales", data.getProductSales());
        map.put("sellingFees", data.getSellingFees());
        map.put("fbaFees", data.getFbaFees());
        map.put("total", data.getTotal());
        return map;
    }
}
