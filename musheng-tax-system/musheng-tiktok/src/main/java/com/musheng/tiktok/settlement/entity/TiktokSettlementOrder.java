package com.musheng.tiktok.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TK结算订单明细实体（Order details sheet）
 *
 * @author wanhua
 * 19:42 2026年05月14日
 */
@Data
@TableName("t_tiktok_settlement_order")
public class TiktokSettlementOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;
    private String siteCode;
    private Long importBatchId;
    private String statementId;
    private LocalDate statementDate;
    private String currency;
    private String type;
    private String orderId;
    private String skuId;
    /** 导入时从商品库映射写入 */
    private String msku;
    /** 导入时从商品库映射写入 */
    private String productId;
    private Integer quantity;
    private String productName;
    private String skuName;

    // 金额字段
    private BigDecimal totalSettlementAmount;
    private BigDecimal totalRevenue;
    private BigDecimal subtotalAfterDiscount;
    private BigDecimal subtotalBeforeDiscount;
    private BigDecimal sellerDiscount;
    private BigDecimal refundAfterDiscount;
    private BigDecimal refundBeforeDiscount;
    private BigDecimal refundOfSellerDiscount;

    // 费用归类
    private BigDecimal commissionFee;
    private BigDecimal logisticsFee;
    private BigDecimal affiliateFee;
    private BigDecimal promotionFee;
    private BigDecimal taxFee;
    private BigDecimal otherFee;

    // 报税关键字段
    private BigDecimal referralFee;
    private BigDecimal sellerShippingFee;
    private BigDecimal fbtFulfillmentFee;
    private BigDecimal refundAdminFee;
    private BigDecimal actualReturnShippingFee;
    private BigDecimal returnShippingReimb;

    // 原始数据
    private String rawFeeJson;

    private LocalDateTime createTime;
}
