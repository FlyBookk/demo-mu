package com.musheng.business.sales.parser;

import com.musheng.business.common.config.MarketplaceConfigService;
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
import java.util.regex.Pattern;

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

    private final MarketplaceConfigService marketplaceConfigService;

    public ErpSettlementParser(MarketplaceConfigService marketplaceConfigService) {
        this.marketplaceConfigService = marketplaceConfigService;
    }

    /**
     * 亚马逊标准订单号正则（格式：XXX-1234567-1234567）
     * 不符合此格式的订单号视为非标订单，不进行合并，单条存储
     */
    private static final Pattern ORDER_ID_PATTERN_LOOSE = Pattern.compile("[A-Z0-9]{3}-\\d{7}-\\d{7}");
    
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
        
        boolean isPreview = Boolean.TRUE.equals(context.getPreviewMode());
        int maxRows = isPreview ? (context.getPreviewRows() != null ? context.getPreviewRows() : 10) : Integer.MAX_VALUE;
        
        try {
            Charset charset = context.getEncoding() != null 
                    ? Charset.forName(context.getEncoding()) 
                    : StandardCharsets.UTF_8;
            
            try (BufferedReader reader = Files.newBufferedReader(context.getFilePath(), charset)) {
                CSVFormat format = buildCsvFormat();
                try (CSVParser parser = new CSVParser(reader, format)) {
                    return doParse(parser, context, maxRows, isPreview);
                }
            }
        } catch (IOException e) {
            log.error("读取ERP文件失败: {}", e.getMessage(), e);
            return ParseResult.fail("读取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public ParseResult parse(String content, ParseContext context, int maxRows) {
        log.info("开始解析ERP结算数据（从内容）: maxRows={}", maxRows);
        
        try {
            CSVFormat format = buildCsvFormat();
            try (CSVParser parser = CSVParser.parse(content, format)) {
                return doParse(parser, context, maxRows, false);
            }
        } catch (IOException e) {
            log.error("解析ERP内容失败: {}", e.getMessage(), e);
            return ParseResult.fail("解析内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建统一的CSV格式配置
     */
    private CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
    }
    
    /**
     * 执行实际的解析逻辑（公共方法）
     */
    private ParseResult doParse(CSVParser parser, ParseContext context, int maxRows, boolean isPreview) {
        Map<String, ErpAggregateRow> aggregateMap = new LinkedHashMap<>();
        List<ParseResult.ParseError> errors = new ArrayList<>();
        Set<String> detectedSites = new HashSet<>();
        int totalRows = 0;
        int failRows = 0;
        
        Map<String, Integer> headerMap = parser.getHeaderMap();
        log.info("解析到ERP表头字段: {}", headerMap.keySet());
        
        for (CSVRecord record : parser) {
            totalRows++;
            
            try {
                ErpRow erpRow = parseErpRow(record, headerMap, context, (int) record.getRecordNumber());
                
                if (erpRow.getSiteCode() != null) {
                    detectedSites.add(erpRow.getSiteCode());
                }
                
                String aggregateKey = buildAggregateKey(erpRow);
                ErpAggregateRow aggregateRow = aggregateMap.computeIfAbsent(
                        aggregateKey, k -> createAggregateRow(erpRow));
                accumulateAmount(aggregateRow, erpRow);
                
            } catch (Exception e) {
                failRows++;
                errors.add(ParseResult.ParseError.builder()
                        .row((int) record.getRecordNumber())
                        .message(e.getMessage())
                        .build());
                log.warn("解析第{}行失败: {}", record.getRecordNumber(), e.getMessage());
            }
            
            if (aggregateMap.size() >= maxRows) {
                break;
            }
        }
        
        List<SalesData> dataList = new ArrayList<>();
        List<Map<String, Object>> previewData = isPreview ? new ArrayList<>() : null;
        
        for (ErpAggregateRow aggregateRow : aggregateMap.values()) {
            SalesData salesData = convertToSalesData(aggregateRow, context);
            dataList.add(salesData);
            if (isPreview && previewData != null) {
                previewData.add(convertToMap(salesData));
            }
        }
        
        log.info("ERP结算数据解析完成: 原始行数={}, 聚合后记录数={}, 失败行数={}, 检测到站点={}", 
                totalRows, dataList.size(), failRows, detectedSites);
        
        return ParseResult.builder()
                .success(true)
                .totalRows(totalRows)
                .successRows(dataList.size())
                .failRows(failRows)
                .skipRows(0)
                .dataList(dataList)
                .previewData(previewData)
                .errors(errors)
                .warnings(new ArrayList<>())
                .detectedSiteCodes(new ArrayList<>(detectedSites))
                .build();
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
     * @param recordNumber CSV行号，用于订单号为空时生成唯一聚合键（不聚合）
     */
    private ErpRow parseErpRow(CSVRecord record, Map<String, Integer> headerMap, ParseContext context, int recordNumber) {
        ErpRow row = new ErpRow();
        
        // 使用 Settlement ID（亚马逊原始结算ID）作为 settlementId，便于与原始数据统一去重
        // 如果没有 Settlement ID，则使用 结算编号（ERP系统ID）作为备用
        String amazonSettlementId = getFieldValue(record, headerMap, "Settlement ID");
        String erpSettlementId = getFieldValue(record, headerMap, "结算编号");
        row.setSettlementId(amazonSettlementId != null && !amazonSettlementId.isEmpty() 
                ? amazonSettlementId : erpSettlementId);
        // 单独记录 ERP 结算编号，用于重复导入校验；无订单号时用于聚合
        row.setErpSettlementId(cleanOrderId(erpSettlementId));
        
        row.setOrderId(cleanOrderId(getFieldValue(record, headerMap, "订单号")));
        row.setStoreName(getFieldValue(record, headerMap, "店铺"));
        row.setSiteCode(getFieldValue(record, headerMap, "国家"));
        row.setFulfillment(getFieldValue(record, headerMap, "配送方式"));
        row.setMsku(getFieldValue(record, headerMap, "MSKU"));
        row.setTransactionType(getFieldValue(record, headerMap, "交易类型"));
        row.setCurrencyCode(getFieldValue(record, headerMap, "币种"));
        // ERP的SKU字段不使用，改用MSKU映射到数据库的sku字段
        // row.setSku(getFieldValue(record, headerMap, "SKU"));
        row.setProductName(getFieldValue(record, headerMap, "品名"));
        row.setSettlementStatus(getFieldValue(record, headerMap, "结算状态"));
        row.setTransferStatus(getFieldValue(record, headerMap, "转账状态"));
        row.setSource(getFieldValue(record, headerMap, "来源"));
        row.setRecordNumber(recordNumber);
        
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
     * 判断是否为亚马逊标准订单号（格式：XXX-1234567-1234567）
     */
    private static boolean isStandardOrderId(String orderId) {
        return orderId != null && !orderId.isEmpty() && ORDER_ID_PATTERN_LOOSE.matcher(orderId).matches();
    }

    /**
     * 构建聚合key
     * 有标准订单号时按 orderId 聚合（一订单多行费用明细合并为一条）；
     * 无订单号或非标订单号时不再聚合，每行独立
     * 格式：标准订单号 source|orderId|siteCode|msku；无订单号/非标 source|orderId|siteCode|msku|recordNumber
     */
    private String buildAggregateKey(ErpRow row) {
        boolean hasOrderId = row.getOrderId() != null && !row.getOrderId().isEmpty();
        boolean isStandard = hasOrderId && isStandardOrderId(row.getOrderId());
        String mergeKey = hasOrderId
                ? row.getOrderId()
                : (row.getErpSettlementId() != null && !row.getErpSettlementId().isEmpty()
                        ? row.getErpSettlementId() : "");
        if (hasOrderId && isStandard) {
            return String.format("%s|%s|%s|%s",
                    row.getSource() != null ? row.getSource() : "",
                    mergeKey,
                    row.getSiteCode() != null ? row.getSiteCode() : "",
                    row.getMsku() != null ? row.getMsku() : "");
        }
        // 订单号为空或非标订单号：每行独立，不聚合
        return String.format("%s|%s|%s|%s|%d",
                row.getSource() != null ? row.getSource() : "",
                mergeKey,
                row.getSiteCode() != null ? row.getSiteCode() : "",
                row.getMsku() != null ? row.getMsku() : "",
                row.getRecordNumber());
    }
    
    /**
     * 创建聚合行
     */
    private ErpAggregateRow createAggregateRow(ErpRow firstRow) {
        ErpAggregateRow aggregate = new ErpAggregateRow();
        aggregate.setOrderId(firstRow.getOrderId());
        aggregate.setSiteCode(firstRow.getSiteCode());
        aggregate.setNonStandardOrder(!isStandardOrderId(firstRow.getOrderId()));
        // ERP数据使用MSKU作为sku字段值
        aggregate.setSku(firstRow.getMsku());
        aggregate.setMsku(firstRow.getMsku());
        aggregate.setSettlementId(firstRow.getSettlementId());
        aggregate.setErpSettlementId(firstRow.getErpSettlementId());
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
        aggregate.setTransactionType(firstRow.getTransactionType());
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
     * 字段名到累加器的映射（避免冗长的switch语句）
     */
    private static final Map<String, java.util.function.BiConsumer<ErpAggregateRow, BigDecimal>> FIELD_ACCUMULATORS = Map.ofEntries(
            Map.entry("productSales", (agg, amt) -> agg.setProductSales(agg.getProductSales().add(amt))),
            Map.entry("productSalesTax", (agg, amt) -> agg.setProductSalesTax(agg.getProductSalesTax().add(amt))),
            Map.entry("shippingCredits", (agg, amt) -> agg.setShippingCredits(agg.getShippingCredits().add(amt))),
            Map.entry("shippingCreditsTax", (agg, amt) -> agg.setShippingCreditsTax(agg.getShippingCreditsTax().add(amt))),
            Map.entry("giftWrapCredits", (agg, amt) -> agg.setGiftWrapCredits(agg.getGiftWrapCredits().add(amt))),
            Map.entry("giftWrapCreditsTax", (agg, amt) -> agg.setGiftWrapCreditsTax(agg.getGiftWrapCreditsTax().add(amt))),
            Map.entry("promotionalRebates", (agg, amt) -> agg.setPromotionalRebates(agg.getPromotionalRebates().add(amt))),
            Map.entry("promotionalRebatesTax", (agg, amt) -> agg.setPromotionalRebatesTax(agg.getPromotionalRebatesTax().add(amt))),
            Map.entry("marketplaceWithheldTax", (agg, amt) -> agg.setMarketplaceWithheldTax(agg.getMarketplaceWithheldTax().add(amt))),
            Map.entry("sellingFees", (agg, amt) -> agg.setSellingFees(agg.getSellingFees().add(amt))),
            Map.entry("fbaFees", (agg, amt) -> agg.setFbaFees(agg.getFbaFees().add(amt))),
            Map.entry("otherTransactionFees", (agg, amt) -> agg.setOtherTransactionFees(agg.getOtherTransactionFees().add(amt))),
            Map.entry("other", (agg, amt) -> agg.setOther(agg.getOther().add(amt)))
    );
    
    /**
     * 根据交易类型累加金额
     */
    private void accumulateAmount(ErpAggregateRow aggregate, ErpRow row) {
        if (row.getAmount() == null || row.getTransactionType() == null) {
            return;
        }
        
        String targetField = ERP_TYPE_TO_FIELD.get(row.getTransactionType());
        if (targetField == null) {
            targetField = "other";  // 未映射的交易类型归入other
        }
        
        var accumulator = FIELD_ACCUMULATORS.get(targetField);
        if (accumulator != null) {
            accumulator.accept(aggregate, row.getAmount());
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
        data.setErpSettlementId(aggregate.getErpSettlementId());
        data.setFulfillment(aggregate.getFulfillment());
        // 描述字段统一赋值：ERP表来源列-交易类型列
        String source = aggregate.getSource() != null ? aggregate.getSource() : "";
        String txType = aggregate.getTransactionType() != null ? aggregate.getTransactionType() : "";
        data.setDescription(source + "-" + txType);
        data.setQuantity(aggregate.getQuantity());
        data.setTransactionDate(aggregate.getSettlementTime());
        
        // ERP特有字段（仅保留店铺名称）
        data.setStoreName(aggregate.getStoreName());
        
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
        } else if (sourceType.isTransfer()) {
            // 资金划转类型，不属于费用
            data.setTransactionCategory("transfer");
        } else if (sourceType.isFee()) {
            data.setTransactionCategory("fee");
        } else if (sourceType.isAdjustment()) {
            data.setTransactionCategory("adjustment");
        } else {
            data.setTransactionCategory("other");
        }
        
        // 交易类型：统一用来源值（source），与订单号不为空场景保持一致
        // transactionType 存 Shipment/Refund/ServiceFee 等结算类型，transactionCategory 存 income/refund/fee 等标准分类
        data.setTransactionType(aggregate.getSource() != null ? aggregate.getSource() : "ERP_SETTLEMENT");
        
        return data;
    }
    
    /**
     * 根据站点代码获取 marketplace 域名（从 t_marketplace 动态查询）
     */
    private String getMarketplaceBySiteCode(String siteCode) {
        if (siteCode == null) return null;
        return marketplaceConfigService.getDomainBySiteCode(siteCode);
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
    
    /**
     * ERP行数据
     */
    @Data
    private static class ErpRow {
        private String settlementId;
        /** ERP结算编号（结算编号列），用于按结算编号合并 */
        private String erpSettlementId;
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
        /** CSV行号，订单号为空时用于生成唯一聚合键（不聚合） */
        private int recordNumber;
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
        private String erpSettlementId;
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
        /** 交易类型（Principal/Commission等），订单号为空时用于去重 */
        private String transactionType;
        /** 是否非标订单（订单号不符合亚马逊格式），非标订单单条存储 */
        private boolean nonStandardOrder;
        
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
