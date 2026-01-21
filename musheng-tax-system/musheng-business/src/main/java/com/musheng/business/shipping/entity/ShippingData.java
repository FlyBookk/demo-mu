package com.musheng.business.shipping.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntityWithoutUpdateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 配送数据实体
 * 映射到 t_shipping_data 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shipping_data")
public class ShippingData extends BaseEntityWithoutUpdateTime {

    /**
     * 导入批次ID
     */
    private Long importBatchId;

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 发货日期(配送日期)
     */
    private LocalDate shipDate;

    /**
     * 站点编码
     */
    private String siteCode;

    /**
     * 原始marketplace值(销售渠道)
     */
    private String marketplace;

    /**
     * 货币编码
     */
    private String currencyCode;

    /**
     * 商品价格
     */
    private BigDecimal productPrice;

    /**
     * 商品税
     */
    private BigDecimal productTax;

    /**
     * 运费
     */
    private BigDecimal shippingPrice;

    /**
     * 运费税
     */
    private BigDecimal shippingTax;

    /**
     * 礼品包装价格
     */
    private BigDecimal giftWrapPrice;

    /**
     * 礼品包装价格税
     */
    private BigDecimal giftWrapTax;

    /**
     * 商品促销折扣
     */
    private BigDecimal productPromotionDiscount;

    /**
     * 货件促销折扣
     */
    private BigDecimal shipmentPromotionDiscount;

    /**
     * 物流费用(成本)
     */
    private BigDecimal shippingCost;

    /**
     * 收入总额(计算值)
     */
    private BigDecimal revenueTotal;

    /**
     * SKU
     */
    private String sku;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 承运商
     */
    private String carrier;

    /**
     * 物流单号
     */
    private String trackingNumber;
}
