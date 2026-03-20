package com.musheng.business.sales.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售数据原始导出行（全字段，用于导出原始数据）
 */
@Data
public class SalesDataExportRow {

    @ExcelProperty("数据源类型")
    private String sourceType;

    @ExcelProperty("店铺名称")
    private String storeName;

    @ExcelProperty("结算日期")
    private LocalDateTime transactionDate;

    @ExcelProperty("结算ID")
    private String settlementId;

    @ExcelProperty("ERP结算编号")
    private String erpSettlementId;

    @ExcelProperty("交易类型")
    private String transactionType;

    @ExcelProperty("交易分类")
    private String transactionCategory;

    @ExcelProperty("订单号")
    private String orderId;

    @ExcelProperty("SKU")
    private String sku;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("站点")
    private String siteCode;

    @ExcelProperty("站点域名")
    private String marketplace;

    @ExcelProperty("货币")
    private String currencyCode;

    @ExcelProperty("配送方式")
    private String fulfillment;

    @ExcelProperty("产品销售额")
    private BigDecimal productSales;

    @ExcelProperty("产品税")
    private BigDecimal productSalesTax;

    @ExcelProperty("运费收入")
    private BigDecimal shippingCredits;

    @ExcelProperty("运费税")
    private BigDecimal shippingCreditsTax;

    @ExcelProperty("礼品包装收入")
    private BigDecimal giftWrapCredits;

    @ExcelProperty("礼品包装税")
    private BigDecimal giftWrapCreditsTax;

    @ExcelProperty("监管费")
    private BigDecimal regulatoryFee;

    @ExcelProperty("监管费税")
    private BigDecimal regulatoryFeeTax;

    @ExcelProperty("促销折扣")
    private BigDecimal promotionalRebates;

    @ExcelProperty("促销折扣税")
    private BigDecimal promotionalRebatesTax;

    @ExcelProperty("平台代扣税")
    private BigDecimal marketplaceWithheldTax;

    @ExcelProperty("销售费用")
    private BigDecimal sellingFees;

    @ExcelProperty("FBA费用")
    private BigDecimal fbaFees;

    @ExcelProperty("其他交易费")
    private BigDecimal otherTransactionFees;

    @ExcelProperty("其他")
    private BigDecimal other;

    @ExcelProperty("合计")
    private BigDecimal total;

    @ExcelProperty("汇率")
    private BigDecimal exchangeRate;

    @ExcelProperty("汇率日期")
    private LocalDate exchangeRateDate;
}
