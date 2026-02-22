package com.musheng.business.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.annotation.FieldMapping;
import com.musheng.common.entity.BaseEntityWithoutUpdateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售数据实体
 * 映射到 t_sales_data 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sales_data")
public class SalesData extends BaseEntityWithoutUpdateTime {

    /**
     * 店铺ID（用于数据隔离）
     */
    @FieldMapping(label = "店铺ID", ignore = true)
    private Long shopId;

    /**
     * 导入批次ID
     */
    @FieldMapping(label = "导入批次ID", ignore = true)
    private Long importBatchId;
    
    /**
     * 数据源类型(ORIGINAL-亚马逊原始数据, ERP-ERP结算数据)
     */
    @FieldMapping(label = "数据源类型", ignore = true)
    private String sourceType;
    
    /**
     * 店铺名称(ERP数据使用，预留字段)
     */
    @FieldMapping(label = "店铺名称", description = "店铺名称（预留）", order = 5)
    private String storeName;

    /**
     * 交易日期时间
     */
    @FieldMapping(label = "交易日期", description = "交易发生时间", required = true, order = 1)
    private LocalDateTime transactionDate;

    /**
     * 结算编号
     */
    @FieldMapping(label = "结算ID", description = "结算批次编号", required = true, maxLength = 50, order = 2)
    private String settlementId;

    /**
     * ERP结算编号（ERP数据专用，用于按结算编号合并及重复导入校验）
     */
    @FieldMapping(label = "ERP结算编号", description = "ERP系统结算编号", ignore = true)
    private String erpSettlementId;

    /**
     * 交易类型(保留原始值)
     */
    @FieldMapping(label = "交易类型", description = "订单/退款/服务费等", required = true, maxLength = 50, order = 3)
    private String transactionType;

    /**
     * 交易分类(income/refund/fee/adjustment/other)
     */
    @FieldMapping(label = "交易分类", description = "收入/退款/费用/调整/其他", ignore = true)
    private String transactionCategory;

    /**
     * 订单号
     */
    @FieldMapping(label = "订单ID", description = "亚马逊订单编号", required = true, maxLength = 50, order = 4)
    private String orderId;

    /**
     * SKU
     */
    @FieldMapping(label = "商品SKU", description = "SKU编码", required = true, maxLength = 100, order = 10)
    private String sku;

    /**
     * 商品描述
     */
    @FieldMapping(label = "商品描述", description = "商品名称描述", maxLength = 500, order = 11)
    private String description;

    /**
     * 数量
     */
    @FieldMapping(label = "数量", description = "商品数量", order = 12)
    private Integer quantity;

    /**
     * 站点编码
     */
    @FieldMapping(label = "站点编码", ignore = true)
    private String siteCode;

    /**
     * 原始marketplace值
     */
    @FieldMapping(label = "站点域名", description = "亚马逊站点域名", required = true, maxLength = 50, order = 8)
    private String marketplace;

    /**
     * 货币编码
     */
    @FieldMapping(label = "货币编码", description = "交易货币", maxLength = 10, order = 9)
    private String currencyCode;

    /**
     * 配送方式
     */
    @FieldMapping(label = "配送方式", description = "FBA/FBM", maxLength = 50, order = 13)
    private String fulfillment;

    /**
     * 产品销售额
     */
    @FieldMapping(label = "产品销售额", description = "产品销售金额", precision = 2, order = 20)
    private BigDecimal productSales;

    /**
     * 产品税
     */
    @FieldMapping(label = "产品税", description = "产品销售税", precision = 2, order = 21)
    private BigDecimal productSalesTax;

    /**
     * 运费收入
     */
    @FieldMapping(label = "运费收入", description = "运费抵扣金额", precision = 2, order = 22)
    private BigDecimal shippingCredits;

    /**
     * 运费税
     */
    @FieldMapping(label = "运费税", description = "运费税金", precision = 2, order = 23)
    private BigDecimal shippingCreditsTax;

    /**
     * 礼品包装费
     */
    @FieldMapping(label = "礼品包装收入", description = "礼品包装费用", precision = 2, order = 24)
    private BigDecimal giftWrapCredits;

    /**
     * 礼品包装税
     */
    @FieldMapping(label = "礼品包装税", description = "礼品包装税金", precision = 2, order = 25)
    private BigDecimal giftWrapCreditsTax;

    /**
     * 监管费(UK/DE默认0)
     */
    @FieldMapping(label = "监管费", description = "UK/DE默认0", precision = 2, order = 26)
    private BigDecimal regulatoryFee;

    /**
     * 监管费税(UK/DE默认0)
     */
    @FieldMapping(label = "监管费税", description = "UK/DE默认0", precision = 2, order = 27)
    private BigDecimal regulatoryFeeTax;

    /**
     * 促销折扣
     */
    @FieldMapping(label = "促销折扣", description = "促销优惠金额", precision = 2, order = 28)
    private BigDecimal promotionalRebates;

    /**
     * 促销折扣税
     */
    @FieldMapping(label = "促销折扣税", description = "促销折扣税金", precision = 2, order = 29)
    private BigDecimal promotionalRebatesTax;

    /**
     * 平台代扣税
     */
    @FieldMapping(label = "平台代扣税", description = "平台代扣税金", precision = 2, order = 30)
    private BigDecimal marketplaceWithheldTax;

    /**
     * 销售费用
     */
    @FieldMapping(label = "销售费用", description = "平台销售佣金", precision = 2, order = 31)
    private BigDecimal sellingFees;

    /**
     * FBA费用
     */
    @FieldMapping(label = "FBA费用", description = "FBA配送费用", precision = 2, order = 32)
    private BigDecimal fbaFees;

    /**
     * 其他交易费
     */
    @FieldMapping(label = "其他费用", description = "其他交易费用", precision = 2, order = 33)
    private BigDecimal otherTransactionFees;

    /**
     * 其他
     */
    @FieldMapping(label = "其他", description = "其他金额", precision = 2, order = 34)
    private BigDecimal other;

    /**
     * 合计
     */
    @FieldMapping(label = "合计", description = "订单合计金额", required = true, precision = 2, order = 35)
    private BigDecimal total;

    /**
     * 交易日期当天汇率（对人民币）
     * 如果当天是节假日/周末，则取下一个工作日的汇率
     */
    @FieldMapping(label = "汇率", ignore = true)
    private BigDecimal exchangeRate;

    /**
     * 汇率实际取值日期
     * 如果交易日期是节假日，则为下一个工作日日期
     */
    @FieldMapping(label = "汇率日期", ignore = true)
    private java.time.LocalDate exchangeRateDate;
}
