package com.musheng.business.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
     * 导入批次ID
     */
    private Long importBatchId;
    
    /**
     * 数据源类型(ORIGINAL-亚马逊原始数据, ERP-ERP结算数据)
     */
    private String sourceType;
    
    /**
     * 店铺名称(ERP数据使用)
     */
    private String storeName;
    
    /**
     * 结算状态(ERP数据使用)
     */
    private String settlementStatus;
    
    /**
     * 转账状态(ERP数据使用)
     */
    private String transferStatus;

    /**
     * 交易日期时间
     */
    private LocalDateTime transactionDate;

    /**
     * 原始日期字符串
     */
    private String originalDateStr;

    /**
     * 原始时区
     */
    private String originalTimezone;

    /**
     * 结算编号
     */
    private String settlementId;

    /**
     * 交易类型(保留原始值)
     */
    private String transactionType;

    /**
     * 交易分类(income/refund/fee/adjustment/other)
     */
    private String transactionCategory;

    /**
     * 订单号
     */
    private String orderId;

    /**
     * SKU
     */
    private String sku;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 站点编码
     */
    private String siteCode;

    /**
     * 原始marketplace值
     */
    private String marketplace;

    /**
     * 货币编码
     */
    private String currencyCode;

    /**
     * 账户类型(仅US有)
     */
    private String accountType;

    /**
     * 配送方式
     */
    private String fulfillment;

    /**
     * 订单城市
     */
    private String orderCity;

    /**
     * 订单省/州
     */
    private String orderState;

    /**
     * 订单邮编
     */
    private String orderPostal;

    /**
     * 税收模式
     */
    private String taxCollectionModel;

    /**
     * 产品销售额
     */
    private BigDecimal productSales;

    /**
     * 产品税
     */
    private BigDecimal productSalesTax;

    /**
     * 运费收入
     */
    private BigDecimal shippingCredits;

    /**
     * 运费税
     */
    private BigDecimal shippingCreditsTax;

    /**
     * 礼品包装费
     */
    private BigDecimal giftWrapCredits;

    /**
     * 礼品包装税
     */
    private BigDecimal giftWrapCreditsTax;

    /**
     * 监管费(UK/DE默认0)
     */
    private BigDecimal regulatoryFee;

    /**
     * 监管费税(UK/DE默认0)
     */
    private BigDecimal regulatoryFeeTax;

    /**
     * 促销折扣
     */
    private BigDecimal promotionalRebates;

    /**
     * 促销折扣税
     */
    private BigDecimal promotionalRebatesTax;

    /**
     * 平台代扣税
     */
    private BigDecimal marketplaceWithheldTax;

    /**
     * 销售费用
     */
    private BigDecimal sellingFees;

    /**
     * FBA费用
     */
    private BigDecimal fbaFees;

    /**
     * 其他交易费
     */
    private BigDecimal otherTransactionFees;

    /**
     * 其他
     */
    private BigDecimal other;

    /**
     * 合计
     */
    private BigDecimal total;
}
