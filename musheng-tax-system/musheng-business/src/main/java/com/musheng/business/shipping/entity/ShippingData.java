package com.musheng.business.shipping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.annotation.FieldMapping;
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
     * 订单号
     */
    @FieldMapping(label = "订单ID", description = "亚马逊订单编号", required = true, maxLength = 50, order = 1)
    private String orderId;

    /**
     * 发货日期(配送日期)
     */
    @FieldMapping(label = "发货日期", description = "配送日期", required = true, order = 2)
    private LocalDate shipDate;

    /**
     * 站点编码
     */
    @FieldMapping(label = "站点编码", ignore = true)
    private String siteCode;

    /**
     * 原始marketplace值(销售渠道)
     */
    @FieldMapping(label = "站点域名", description = "销售渠道", required = true, maxLength = 50, order = 3)
    private String marketplace;

    /**
     * 货币编码
     */
    @FieldMapping(label = "货币编码", description = "交易货币", maxLength = 10, order = 4)
    private String currencyCode;

    /**
     * 商品价格
     */
    @FieldMapping(label = "商品价格", description = "商品售价", precision = 2, order = 10)
    private BigDecimal productPrice;

    /**
     * 商品税
     */
    @FieldMapping(label = "商品税", description = "商品税金", precision = 2, order = 11)
    private BigDecimal productTax;

    /**
     * 运费
     */
    @FieldMapping(label = "运费", description = "运费收入", precision = 2, order = 12)
    private BigDecimal shippingPrice;

    /**
     * 运费税
     */
    @FieldMapping(label = "运费税", description = "运费税金", precision = 2, order = 13)
    private BigDecimal shippingTax;

    /**
     * 礼品包装价格
     */
    @FieldMapping(label = "礼品包装价格", description = "礼品包装费用", precision = 2, order = 14)
    private BigDecimal giftWrapPrice;

    /**
     * 礼品包装价格税
     */
    @FieldMapping(label = "礼品包装税", description = "礼品包装税金", precision = 2, order = 15)
    private BigDecimal giftWrapTax;

    /**
     * 商品促销折扣
     */
    @FieldMapping(label = "商品促销折扣", description = "商品促销优惠", precision = 2, order = 16)
    private BigDecimal productPromotionDiscount;

    /**
     * 货件促销折扣
     */
    @FieldMapping(label = "货件促销折扣", description = "配送促销优惠", precision = 2, order = 17)
    private BigDecimal shipmentPromotionDiscount;

    /**
     * 物流费用(成本)
     */
    @FieldMapping(label = "物流费用", description = "物流成本", precision = 2, order = 18)
    private BigDecimal shippingCost;

    /**
     * 总计费用（导入时由各分项计算：商品价格+税+运费+礼品包装+促销折扣）
     */
    @TableField("total_amount")
    @FieldMapping(label = "总计费用", description = "导入时由各分项计算", precision = 2, order = 19)
    private BigDecimal totalAmount;

    /**
     * SKU
     */
    @FieldMapping(label = "商品SKU", description = "SKU编码", required = true, maxLength = 100, order = 5)
    private String sku;

    /**
     * 数量
     */
    @FieldMapping(label = "数量", description = "商品数量", order = 6)
    private Integer quantity;

    /**
     * 承运商
     */
    @FieldMapping(label = "承运商", description = "物流承运商", maxLength = 100, order = 7)
    private String carrier;

    /**
     * 物流单号
     */
    @FieldMapping(label = "物流单号", description = "快递追踪号", maxLength = 100, order = 8)
    private String trackingNumber;

    /**
     * 配送日期当天汇率（对人民币）
     * 如果当天是节假日/周末，则取下一个工作日的汇率
     */
    @FieldMapping(label = "汇率", ignore = true)
    private BigDecimal exchangeRate;

    /**
     * 汇率实际取值日期
     * 如果配送日期是节假日，则为下一个工作日日期
     */
    @FieldMapping(label = "汇率日期", ignore = true)
    private LocalDate exchangeRateDate;

    /**
     * 是否本站销售数据：1=是（Amazon 订单），0=否（Non-Amazon 订单）
     */
    @FieldMapping(label = "是否本站", ignore = true)
    private Integer isOwnSite;
}
