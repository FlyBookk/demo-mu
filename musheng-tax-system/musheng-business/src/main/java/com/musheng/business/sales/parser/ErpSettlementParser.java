package com.musheng.business.sales.parser;

import com.musheng.business.sales.entity.SalesData;
import com.musheng.common.enums.ErpSourceType;
import com.musheng.common.enums.SalesSourceType;
import lombok.Data;
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
 * ERP结算数据解析器
 * 处理ERP汇总数据，需要将多行费用明细聚合为订单维度
 * 
 * ERP数据特点：
 * 1. 中文表头
 * 2. 每行是一笔费用明细（一个订单对应多行）
 * 3. 通过"交易类型"字段区分费用类型，对应不同的金额字段
 * 4. 需要按 订单号+站点+SKU 聚合为一条记录
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Slf4j
@Component
public class ErpSettlementParser implements SalesDataParser {
    
    /**
     * ERP数据默认的列名映射
     */
    private static final Map<String, String> ERP_HEADER_MAPPING = new HashMap<>();
    
    /**
     * ERP交易类型到金额字段的默认映射
     */
    private static final Map<String, String> ERP_TYPE_TO_FIELD = new HashMap<>();
    
    static {
        // ERP数据的中文表头映射
        ERP_HEADER_MAPPING.put("结算编号", "settlementId");
        ERP_HEADER_MAPPING.put("订单号", "orderId");
        ERP_HEADER_MAPPING.put("店铺", "storeName");
        ERP_HEADER_MAPPING.put("国家", "siteCode");
        ERP_HEADER_MAPPING.put("报告类型", "reportType");
        ERP_HEADER_MAPPING.put("配送方式", "fulfillment");
        ERP_HEADER_MAPPING.put("来源", "source");
        ERP_HEADER_MAPPING.put("MSKU", "msku");
        ERP_HEADER_MAPPING.put("交易类型", "transactionType");
        ERP_HEADER_MAPPING.put("结算时间", "settlementTime");
        ERP_HEADER_MAPPING.put("币种", "currencyCode");
        ERP_HEADER_MAPPING.put("金额", "amount");
        ERP_HEADER_MAPPING.put("数量", "quantity");
        ERP_HEADER_MAPPING.put("结算状态", "settlementStatus");
        ERP_HEADER_MAPPING.put("转账状态", "transferStatus");
        ERP_HEADER_MAPPING.put("Settlement ID", "amazonSettlementId");
        ERP_HEADER_MAPPING.put("SKU", "sku");
        ERP_HEADER_MAPPING.put("品名", "productName");
        ERP_HEADER_MAPPING.put("FNSKU", "fnsku");
        
        // ========== ERP交易类型到SalesData金额字段的映射 ==========
        
        // === 商品销售收入 (productSales) ===
        ERP_TYPE_TO_FIELD.put("Principal", "productSales");
        
        // === 商品销售税 (productSalesTax) ===
        ERP_TYPE_TO_FIELD.put("Tax", "productSalesTax");
        ERP_TYPE_TO_FIELD.put("TaxAmount", "productSalesTax");
        ERP_TYPE_TO_FIELD.put("TaxAmountAdjustment", "productSalesTax");
        ERP_TYPE_TO_FIELD.put("BaseTax", "productSalesTax");
        
        // === 运费收入 (shippingCredits) ===
        ERP_TYPE_TO_FIELD.put("ShippingCharge", "shippingCredits");
        ERP_TYPE_TO_FIELD.put("ShippingDiscount", "shippingCredits");
        ERP_TYPE_TO_FIELD.put("ShippingChargeback", "shippingCredits");
        
        // === 运费税 (shippingCreditsTax) ===
        ERP_TYPE_TO_FIELD.put("ShippingTax", "shippingCreditsTax");
        ERP_TYPE_TO_FIELD.put("ShippingDiscountTax", "shippingCreditsTax");
        
        // === 礼品包装 (giftWrapCredits / giftWrapCreditsTax) ===
        ERP_TYPE_TO_FIELD.put("GiftWrap", "giftWrapCredits");
        ERP_TYPE_TO_FIELD.put("GiftwrapChargeback", "giftWrapCredits");
        ERP_TYPE_TO_FIELD.put("GiftWrapTax", "giftWrapCreditsTax");
        
        // === 促销折扣 (promotionalRebates / promotionalRebatesTax) ===
        ERP_TYPE_TO_FIELD.put("PromotionDiscount", "promotionalRebates");
        ERP_TYPE_TO_FIELD.put("PromotionDiscountTax", "promotionalRebatesTax");
        
        // === 平台代扣税 (marketplaceWithheldTax) ===
        ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorVAT-Principal", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorTax-Principal", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorVAT-Shipping", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorTax-Shipping", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorTax-Other", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("TaxWithheld", "marketplaceWithheldTax");
        ERP_TYPE_TO_FIELD.put("TaxWithheldAdjustment", "marketplaceWithheldTax");
        
        // === 销售费用/佣金 (sellingFees) ===
        ERP_TYPE_TO_FIELD.put("Commission", "sellingFees");
        ERP_TYPE_TO_FIELD.put("RefundCommission", "sellingFees");
        
        // === FBA费用 (fbaFees) ===
        ERP_TYPE_TO_FIELD.put("FBAPerUnitFulfillmentFee", "fbaFees");
        ERP_TYPE_TO_FIELD.put("FBAWeightBasedFee", "fbaFees");
        ERP_TYPE_TO_FIELD.put("FBACustomerReturnPerUnitFee", "fbaFees");
        ERP_TYPE_TO_FIELD.put("FBAStorageFee", "fbaFees");
        ERP_TYPE_TO_FIELD.put("FBALongTermStorageFee", "fbaFees");
        ERP_TYPE_TO_FIELD.put("FBADisposalFee", "fbaFees");
        
        // === 其他交易费用 (otherTransactionFees) ===
        ERP_TYPE_TO_FIELD.put("DigitalServicesFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("DigitalServicesFeeFBA", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("CouponRedemptionFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("CouponParticipationFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("CouponPerformanceFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("DealParticipationFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("DealPerformanceFee", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("Subscription", "otherTransactionFees");
        ERP_TYPE_TO_FIELD.put("VineFee", "otherTransactionFees");
        
        // === 其他金额 (other) ===
        ERP_TYPE_TO_FIELD.put("Revenue", "other");
        ERP_TYPE_TO_FIELD.put("RevenueAdjustment", "other");
        ERP_TYPE_TO_FIELD.put("FeeAmount", "other");
        ERP_TYPE_TO_FIELD.put("feeAmount", "other");  // 小写版本
        ERP_TYPE_TO_FIELD.put("baseValue", "other");
        ERP_TYPE_TO_FIELD.put("REVERSAL_REIMBURSEMENT", "other");
        ERP_TYPE_TO_FIELD.put("COMPENSATED_CLAWBACK", "other");
        ERP_TYPE_TO_FIELD.put("WAREHOUSE_LOST", "other");
        ERP_TYPE_TO_FIELD.put("WAREHOUSE_DAMAGE", "other");
        ERP_TYPE_TO_FIELD.put("REMOVAL_ORDER_LOST", "other");
        ERP_TYPE_TO_FIELD.put("FREE_REPLACEMENT_REFUND_ITEMS", "other");
        ERP_TYPE_TO_FIELD.put("INCORRECT_FEES_NON_ITEMIZED", "other");
        ERP_TYPE_TO_FIELD.put("RestockingFee", "other");
        ERP_TYPE_TO_FIELD.put("Goodwill", "other");
        ERP_TYPE_TO_FIELD.put("ReserveDebit", "other");
        ERP_TYPE_TO_FIELD.put("ReserveCredit", "other");
        ERP_TYPE_TO_FIELD.put("BuyerRecharge", "other");
        ERP_TYPE_TO_FIELD.put("Debt Adjustment", "other");
    }
    
    @Override
    public SalesSourceType getSourceType() {
        return SalesSourceType.ERP;
    }
    
    @Override
    public ParseResult parse(ParseContext context) {
        log.info("开始解析ERP结算数据: {}", context.getFilePath());
        
        // 用于聚合的Map：key = orderId + siteCode + sku
        Map<String, ErpAggregateRow> aggregateMap = new LinkedHashMap<>();
        List<ParseResult.ParseError> errors = new ArrayList<>();
        Set<String> detectedSites = new HashSet<>();
        int totalRows = 0;
        int failRows = 0;
        
        boolean isPreview = Boolean.TRUE.equals(context.getPreviewMode());
        int previewLimit = context.getPreviewRows() != null ? context.getPreviewRows() : 10;
        
        try {
            Charset charset = context.getEncoding() != null 
                    ? Charset.forName(context.getEncoding()) 
                    : StandardCharsets.UTF_8;
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();
                
                try (CSVParser parser = new CSVParser(reader, format)) {
                    Map<String, Integer> headerMap = parser.getHeaderMap();
                    log.info("解析到ERP表头字段: {}", headerMap.keySet());
                    
                    for (CSVRecord record : parser) {
                        totalRows++;
                        
                        try {
                            // 解析ERP行数据
                            ErpRow erpRow = parseErpRow(record, headerMap, context);
                            
                            // 收集站点信息
                            if (erpRow.getSiteCode() != null) {
                                detectedSites.add(erpRow.getSiteCode());
                            }
                            
                            // 聚合key
                            String aggregateKey = buildAggregateKey(erpRow);
                            
                            // 聚合处理
                            ErpAggregateRow aggregateRow = aggregateMap.computeIfAbsent(
                                    aggregateKey, 
                                    k -> createAggregateRow(erpRow)
                            );
                            
                            // 根据交易类型累加金额
                            accumulateAmount(aggregateRow, erpRow);
                            
                        } catch (Exception e) {
                            failRows++;
                            errors.add(ParseResult.ParseError.builder()
                                    .row((int) record.getRecordNumber())
                                    .message(e.getMessage())
                                    .build());
                            log.warn("解析第{}行失败: {}", record.getRecordNumber(), e.getMessage());
                        }
                        
                        // 预览模式限制行数
                        if (isPreview && aggregateMap.size() >= previewLimit) {
                            break;
                        }
                    }
                }
            }
            
            // 将聚合结果转换为SalesData列表
            List<SalesData> dataList = new ArrayList<>();
            List<Map<String, Object>> previewData = new ArrayList<>();
            
            for (ErpAggregateRow aggregateRow : aggregateMap.values()) {
                SalesData salesData = convertToSalesData(aggregateRow, context);
                dataList.add(salesData);
                
                if (isPreview) {
                    previewData.add(convertToMap(salesData));
                }
            }
            
            ParseResult result = ParseResult.builder()
                    .success(true)
                    .totalRows(totalRows)
                    .successRows(dataList.size())
                    .failRows(failRows)
                    .skipRows(0)
                    .dataList(dataList)
                    .previewData(isPreview ? previewData : null)
                    .errors(errors)
                    .warnings(new ArrayList<>())
                    .detectedSiteCodes(new ArrayList<>(detectedSites))
                    .build();
            
            log.info("ERP结算数据解析完成: 原始行数={}, 聚合后记录数={}, 失败行数={}, 检测到站点={}", 
                    totalRows, dataList.size(), failRows, detectedSites);
            
            return result;
            
        } catch (IOException e) {
            log.error("读取ERP文件失败: {}", e.getMessage(), e);
            return ParseResult.fail("读取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public ParseResult parse(String content, ParseContext context, int maxRows) {
        log.info("开始解析ERP结算数据（从内容）: maxRows={}", maxRows);
        
        // 用于聚合的Map：key = orderId + siteCode + sku
        Map<String, ErpAggregateRow> aggregateMap = new LinkedHashMap<>();
        List<ParseResult.ParseError> errors = new ArrayList<>();
        Set<String> detectedSites = new HashSet<>();
        int totalRows = 0;
        int failRows = 0;
        
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();
            
            try (CSVParser parser = CSVParser.parse(content, format)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                log.info("解析到ERP表头字段: {}", headerMap.keySet());
                
                for (CSVRecord record : parser) {
                    totalRows++;
                    
                    try {
                        // 解析ERP行数据
                        ErpRow erpRow = parseErpRow(record, headerMap, context);
                        
                        // 收集站点信息
                        if (erpRow.getSiteCode() != null) {
                            detectedSites.add(erpRow.getSiteCode());
                        }
                        
                        // 聚合key
                        String aggregateKey = buildAggregateKey(erpRow);
                        
                        // 聚合处理
                        ErpAggregateRow aggregateRow = aggregateMap.computeIfAbsent(
                                aggregateKey, 
                                k -> createAggregateRow(erpRow)
                        );
                        
                        // 根据交易类型累加金额
                        accumulateAmount(aggregateRow, erpRow);
                        
                    } catch (Exception e) {
                        failRows++;
                        errors.add(ParseResult.ParseError.builder()
                                .row((int) record.getRecordNumber())
                                .message(e.getMessage())
                                .build());
                        log.warn("解析第{}行失败: {}", record.getRecordNumber(), e.getMessage());
                    }
                    
                    // 限制聚合后的记录数
                    if (aggregateMap.size() >= maxRows) {
                        break;
                    }
                }
            }
            
            // 将聚合结果转换为SalesData列表
            List<SalesData> dataList = new ArrayList<>();
            
            for (ErpAggregateRow aggregateRow : aggregateMap.values()) {
                SalesData salesData = convertToSalesData(aggregateRow, context);
                dataList.add(salesData);
            }
            
            ParseResult result = ParseResult.builder()
                    .success(true)
                    .totalRows(totalRows)
                    .successRows(dataList.size())
                    .failRows(failRows)
                    .skipRows(0)
                    .dataList(dataList)
                    .errors(errors)
                    .warnings(new ArrayList<>())
                    .detectedSiteCodes(new ArrayList<>(detectedSites))
                    .build();
            
            log.info("ERP结算数据解析完成: 原始行数={}, 聚合后记录数={}, 失败行数={}", 
                    totalRows, dataList.size(), failRows);
            
            return result;
            
        } catch (IOException e) {
            log.error("解析ERP内容失败: {}", e.getMessage(), e);
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
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();
                
                try (CSVParser parser = new CSVParser(reader, format)) {
                    Map<String, Integer> headerMap = parser.getHeaderMap();
                    
                    // 查找"国家"列
                    Integer siteIndex = headerMap.get("国家");
                    if (siteIndex == null) {
                        return new ArrayList<>();
                    }
                    
                    int count = 0;
                    for (CSVRecord record : parser) {
                        String siteCode = record.get(siteIndex);
                        if (siteCode != null && !siteCode.trim().isEmpty()) {
                            sites.add(siteCode.trim().toUpperCase());
                        }
                        
                        if (++count >= 1000) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("检测ERP站点失败: {}", e.getMessage());
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
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    return false;
                }
                
                // 检查是否包含ERP特有的中文表头
                return headerLine.contains("订单号") && headerLine.contains("交易类型") && headerLine.contains("金额");
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 解析ERP行数据
     */
    private ErpRow parseErpRow(CSVRecord record, Map<String, Integer> headerMap, ParseContext context) {
        ErpRow row = new ErpRow();
        
        // 使用 Settlement ID（亚马逊原始结算ID）作为 settlementId，便于与原始数据统一去重
        // 如果没有 Settlement ID，则使用 结算编号（ERP系统ID）作为备用
        String amazonSettlementId = getFieldValue(record, headerMap, "Settlement ID");
        String erpSettlementId = getFieldValue(record, headerMap, "结算编号");
        row.setSettlementId(amazonSettlementId != null && !amazonSettlementId.isEmpty() 
                ? amazonSettlementId : erpSettlementId);
        
        row.setOrderId(cleanOrderId(getFieldValue(record, headerMap, "订单号")));
        row.setStoreName(getFieldValue(record, headerMap, "店铺"));
        row.setSiteCode(getFieldValue(record, headerMap, "国家"));
        row.setFulfillment(getFieldValue(record, headerMap, "配送方式"));
        row.setMsku(getFieldValue(record, headerMap, "MSKU"));
        row.setTransactionType(getFieldValue(record, headerMap, "交易类型"));
        row.setCurrencyCode(getFieldValue(record, headerMap, "币种"));
        row.setSku(getFieldValue(record, headerMap, "SKU"));
        row.setProductName(getFieldValue(record, headerMap, "品名"));
        row.setSettlementStatus(getFieldValue(record, headerMap, "结算状态"));
        row.setTransferStatus(getFieldValue(record, headerMap, "转账状态"));
        row.setSource(getFieldValue(record, headerMap, "来源"));
        
        // 解析金额
        String amountStr = getFieldValue(record, headerMap, "金额");
        row.setAmount(NumberConverter.parseAuto(amountStr));
        
        // 解析数量
        String quantityStr = getFieldValue(record, headerMap, "数量");
        if (quantityStr != null && !quantityStr.isEmpty()) {
            try {
                row.setQuantity(Integer.parseInt(quantityStr.trim()));
            } catch (NumberFormatException e) {
                row.setQuantity(0);
            }
        }
        
        // 解析结算时间
        String settlementTime = getFieldValue(record, headerMap, "结算时间");
        row.setSettlementTime(DateConverter.parse(settlementTime, "ERP"));
        
        return row;
    }
    
    /**
     * 获取字段值
     */
    private String getFieldValue(CSVRecord record, Map<String, Integer> headerMap, String fieldName) {
        Integer index = headerMap.get(fieldName);
        if (index == null) {
            return null;
        }
        String value = record.get(index);
        return value != null ? value.trim() : null;
    }
    
    /**
     * 清理订单号（移除可能的前导引号）
     */
    private String cleanOrderId(String orderId) {
        if (orderId == null) {
            return null;
        }
        return orderId.replaceAll("^'", "").trim();
    }
    
    /**
     * 构建聚合key
     * 新增来源维度，同一订单在不同来源下分别聚合
     * 格式：source|orderId|siteCode|sku
     */
    private String buildAggregateKey(ErpRow row) {
        return String.format("%s|%s|%s|%s", 
                row.getSource() != null ? row.getSource() : "",
                row.getOrderId() != null ? row.getOrderId() : "",
                row.getSiteCode() != null ? row.getSiteCode() : "",
                row.getSku() != null ? row.getSku() : "");
    }
    
    /**
     * 创建聚合行
     */
    private ErpAggregateRow createAggregateRow(ErpRow firstRow) {
        ErpAggregateRow aggregate = new ErpAggregateRow();
        aggregate.setOrderId(firstRow.getOrderId());
        aggregate.setSiteCode(firstRow.getSiteCode());
        aggregate.setSku(firstRow.getSku());
        aggregate.setMsku(firstRow.getMsku());
        aggregate.setSettlementId(firstRow.getSettlementId());
        aggregate.setStoreName(firstRow.getStoreName());
        aggregate.setFulfillment(firstRow.getFulfillment());
        aggregate.setCurrencyCode(firstRow.getCurrencyCode());
        aggregate.setSettlementTime(firstRow.getSettlementTime());
        aggregate.setProductName(firstRow.getProductName());
        aggregate.setSettlementStatus(firstRow.getSettlementStatus());
        aggregate.setTransferStatus(firstRow.getTransferStatus());
        aggregate.setQuantity(firstRow.getQuantity());
        
        // 设置来源和结算类型
        aggregate.setSource(firstRow.getSource());
        ErpSourceType sourceType = ErpSourceType.fromSourceValue(firstRow.getSource());
        aggregate.setSettlementCategory(sourceType.getSettlementCategory());
        
        // 初始化金额字段
        aggregate.setProductSales(BigDecimal.ZERO);
        aggregate.setProductSalesTax(BigDecimal.ZERO);
        aggregate.setShippingCredits(BigDecimal.ZERO);
        aggregate.setShippingCreditsTax(BigDecimal.ZERO);
        aggregate.setGiftWrapCredits(BigDecimal.ZERO);
        aggregate.setGiftWrapCreditsTax(BigDecimal.ZERO);
        aggregate.setPromotionalRebates(BigDecimal.ZERO);
        aggregate.setPromotionalRebatesTax(BigDecimal.ZERO);
        aggregate.setMarketplaceWithheldTax(BigDecimal.ZERO);
        aggregate.setSellingFees(BigDecimal.ZERO);
        aggregate.setFbaFees(BigDecimal.ZERO);
        aggregate.setOtherTransactionFees(BigDecimal.ZERO);
        aggregate.setOther(BigDecimal.ZERO);
        
        return aggregate;
    }
    
    /**
     * 根据交易类型累加金额
     */
    private void accumulateAmount(ErpAggregateRow aggregate, ErpRow row) {
        if (row.getAmount() == null || row.getTransactionType() == null) {
            return;
        }
        
        String targetField = ERP_TYPE_TO_FIELD.get(row.getTransactionType());
        if (targetField == null) {
            // 未映射的交易类型归入other
            aggregate.setOther(aggregate.getOther().add(row.getAmount()));
            return;
        }
        
        BigDecimal amount = row.getAmount();
        
        switch (targetField) {
            case "productSales":
                aggregate.setProductSales(aggregate.getProductSales().add(amount));
                break;
            case "productSalesTax":
                aggregate.setProductSalesTax(aggregate.getProductSalesTax().add(amount));
                break;
            case "shippingCredits":
                aggregate.setShippingCredits(aggregate.getShippingCredits().add(amount));
                break;
            case "shippingCreditsTax":
                aggregate.setShippingCreditsTax(aggregate.getShippingCreditsTax().add(amount));
                break;
            case "giftWrapCredits":
                aggregate.setGiftWrapCredits(aggregate.getGiftWrapCredits().add(amount));
                break;
            case "giftWrapCreditsTax":
                aggregate.setGiftWrapCreditsTax(aggregate.getGiftWrapCreditsTax().add(amount));
                break;
            case "promotionalRebates":
                aggregate.setPromotionalRebates(aggregate.getPromotionalRebates().add(amount));
                break;
            case "promotionalRebatesTax":
                aggregate.setPromotionalRebatesTax(aggregate.getPromotionalRebatesTax().add(amount));
                break;
            case "marketplaceWithheldTax":
                aggregate.setMarketplaceWithheldTax(aggregate.getMarketplaceWithheldTax().add(amount));
                break;
            case "sellingFees":
                aggregate.setSellingFees(aggregate.getSellingFees().add(amount));
                break;
            case "fbaFees":
                aggregate.setFbaFees(aggregate.getFbaFees().add(amount));
                break;
            case "otherTransactionFees":
                aggregate.setOtherTransactionFees(aggregate.getOtherTransactionFees().add(amount));
                break;
            case "other":
                aggregate.setOther(aggregate.getOther().add(amount));
                break;
        }
    }
    
    /**
     * 将聚合行转换为SalesData
     */
    private SalesData convertToSalesData(ErpAggregateRow aggregate, ParseContext context) {
        SalesData data = new SalesData();
        
        data.setImportBatchId(context.getImportBatchId());
        data.setOrderId(aggregate.getOrderId());
        data.setSku(aggregate.getSku());
        data.setSiteCode(aggregate.getSiteCode());
        // 根据站点代码设置 marketplace（ERP数据中没有marketplace字段，需要根据站点推断）
        data.setMarketplace(getMarketplaceBySiteCode(aggregate.getSiteCode()));
        data.setCurrencyCode(aggregate.getCurrencyCode());
        data.setSettlementId(aggregate.getSettlementId());
        data.setFulfillment(aggregate.getFulfillment());
        data.setDescription(aggregate.getProductName());
        data.setQuantity(aggregate.getQuantity());
        data.setTransactionDate(aggregate.getSettlementTime());
        
        // 设置结算类型（来源值存储在 transactionType 中，不再单独设置 source）
        data.setSettlementCategory(aggregate.getSettlementCategory());
        
        // ERP特有字段
        data.setStoreName(aggregate.getStoreName());
        data.setSettlementStatus(aggregate.getSettlementStatus());
        data.setTransferStatus(aggregate.getTransferStatus());
        
        // 金额字段
        data.setProductSales(aggregate.getProductSales());
        data.setProductSalesTax(aggregate.getProductSalesTax());
        data.setShippingCredits(aggregate.getShippingCredits());
        data.setShippingCreditsTax(aggregate.getShippingCreditsTax());
        data.setGiftWrapCredits(aggregate.getGiftWrapCredits());
        data.setGiftWrapCreditsTax(aggregate.getGiftWrapCreditsTax());
        data.setPromotionalRebates(aggregate.getPromotionalRebates());
        data.setPromotionalRebatesTax(aggregate.getPromotionalRebatesTax());
        data.setMarketplaceWithheldTax(aggregate.getMarketplaceWithheldTax());
        data.setSellingFees(aggregate.getSellingFees());
        data.setFbaFees(aggregate.getFbaFees());
        data.setOtherTransactionFees(aggregate.getOtherTransactionFees());
        data.setOther(aggregate.getOther());
        
        // 计算总计
        BigDecimal total = aggregate.getProductSales()
                .add(aggregate.getProductSalesTax())
                .add(aggregate.getShippingCredits())
                .add(aggregate.getShippingCreditsTax())
                .add(aggregate.getGiftWrapCredits())
                .add(aggregate.getGiftWrapCreditsTax())
                .add(aggregate.getPromotionalRebates())
                .add(aggregate.getPromotionalRebatesTax())
                .add(aggregate.getMarketplaceWithheldTax())
                .add(aggregate.getSellingFees())
                .add(aggregate.getFbaFees())
                .add(aggregate.getOtherTransactionFees())
                .add(aggregate.getOther());
        data.setTotal(total);
        
        // 确定交易分类（基于结算类型和来源）
        ErpSourceType sourceType = ErpSourceType.fromSourceValue(aggregate.getSource());
        if (sourceType.isOrder()) {
            data.setTransactionCategory("income");
        } else if (sourceType.isRefund()) {
            data.setTransactionCategory("refund");
        } else if (sourceType.isFee()) {
            data.setTransactionCategory("fee");
        } else if (sourceType.isAdjustment()) {
            data.setTransactionCategory("adjustment");
        } else {
            data.setTransactionCategory("other");
        }
        
        // 交易类型设置为来源值，便于追溯
        data.setTransactionType(aggregate.getSource() != null ? aggregate.getSource() : "ERP_SETTLEMENT");
        
        return data;
    }
    
    /**
     * 根据站点代码获取 marketplace 域名
     * ERP数据中没有 marketplace 字段，需要根据站点代码推断
     */
    private String getMarketplaceBySiteCode(String siteCode) {
        if (siteCode == null) {
            return "amazon.com";
        }
        return switch (siteCode.toUpperCase()) {
            case "US" -> "amazon.com";
            case "CA" -> "amazon.ca";
            case "MX" -> "amazon.com.mx";
            case "UK", "GB" -> "amazon.co.uk";
            case "DE" -> "amazon.de";
            case "FR" -> "amazon.fr";
            case "IT" -> "amazon.it";
            case "ES" -> "amazon.es";
            case "NL" -> "amazon.nl";
            case "SE" -> "amazon.se";
            case "PL" -> "amazon.pl";
            case "JP" -> "amazon.co.jp";
            case "AU" -> "amazon.com.au";
            case "IN" -> "amazon.in";
            case "AE" -> "amazon.ae";
            case "SA" -> "amazon.sa";
            case "BR" -> "amazon.com.br";
            case "SG" -> "amazon.sg";
            default -> "amazon.com";
        };
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
        map.put("settlementCategory", data.getSettlementCategory());
        map.put("transactionCategory", data.getTransactionCategory());
        map.put("quantity", data.getQuantity());
        map.put("productSales", data.getProductSales());
        map.put("sellingFees", data.getSellingFees());
        map.put("fbaFees", data.getFbaFees());
        map.put("total", data.getTotal());
        return map;
    }
    
    /**
     * ERP行数据
     */
    @Data
    private static class ErpRow {
        private String settlementId;
        private String orderId;
        private String storeName;
        private String siteCode;
        private String fulfillment;
        private String msku;
        private String transactionType;
        private String currencyCode;
        private BigDecimal amount;
        private Integer quantity;
        private LocalDateTime settlementTime;
        private String sku;
        private String productName;
        private String settlementStatus;
        private String transferStatus;
        /** ERP来源（Shipment/Refund/ServiceFee等） */
        private String source;
    }
    
    /**
     * ERP聚合行数据
     */
    @Data
    private static class ErpAggregateRow {
        private String orderId;
        private String siteCode;
        private String sku;
        private String msku;
        private String settlementId;
        private String storeName;
        private String fulfillment;
        private String currencyCode;
        private LocalDateTime settlementTime;
        private String productName;
        private String settlementStatus;
        private String transferStatus;
        private Integer quantity;
        /** ERP来源（Shipment/Refund/ServiceFee等） */
        private String source;
        /** 结算类型（ORDER/REFUND/SERVICE_FEE等） */
        private String settlementCategory;
        
        // 聚合后的金额字段
        private BigDecimal productSales;
        private BigDecimal productSalesTax;
        private BigDecimal shippingCredits;
        private BigDecimal shippingCreditsTax;
        private BigDecimal giftWrapCredits;
        private BigDecimal giftWrapCreditsTax;
        private BigDecimal promotionalRebates;
        private BigDecimal promotionalRebatesTax;
        private BigDecimal marketplaceWithheldTax;
        private BigDecimal sellingFees;
        private BigDecimal fbaFees;
        private BigDecimal otherTransactionFees;
        private BigDecimal other;
    }
}
