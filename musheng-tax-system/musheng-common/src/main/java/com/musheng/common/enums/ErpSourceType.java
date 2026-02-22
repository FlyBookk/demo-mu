package com.musheng.common.enums;

import lombok.Getter;

/**
 * ERP数据来源类型枚举
 * 对应亚马逊交易类型，定义ERP结算数据中的来源字段值
 * 数据来源：亚马逊的交易类型
 *
 * @author BACKEND_AGENT
 * @since 2026-01-24
 */
@Getter
public enum ErpSourceType {

    /**
     * 配送 - 亚马逊订单交易，包含订单收入及订单费用
     */
    SHIPMENT("Shipment", "ORDER", "配送", "亚马逊订单交易，包含亚马逊的订单收入及订单费用"),

    /**
     * 退款 - 亚马逊订单退款
     */
    REFUND("Refund", "REFUND", "退款", "亚马逊订单退款，包含亚马逊的订单退款及订单退款"),

    /**
     * 调整 - 亚马逊库存赔偿、费用调整及预留金额
     */
    ADJUSTMENT("Adjustment", "ADJUSTMENT", "调整", "亚马逊库存赔偿、亚马逊费用调整及预留金额"),

    /**
     * 拒付 - 买家信用卡拒付
     */
    CHARGEBACK("Chargeback", "CHARGEBACK", "拒付", "买家信用卡拒付"),

    /**
     * 广告 - CPC广告服务
     */
    PRODUCT_ADS_PAYMENT("ProductAdsPayment", "ADS_PAYMENT", "广告", "CPC广告服务，若卖家的广告服务使用卖家账户扣款则在此服务中结算"),

    /**
     * 信用卡扣款 - 应收不足时执行信用卡扣款
     */
    DEBT_RECOVERY("DebtRecovery", "DEBT_RECOVERY", "信用卡扣款", "当用户应收金额不足以支付账单费用，在此类型中执行信用卡扣款业务"),

    /**
     * AToZ亚马逊赔偿
     */
    GUARANTEE_CLAIM("GuranteeClaim", "GUARANTEE_CLAIM", "AToZ亚马逊赔偿", "AToZ亚马逊赔偿"),

    /**
     * 早期评论人计划
     */
    SELLER_REVIEW_ENROLLMENT("SellerReviewEnrollment", "SERVICE_FEE", "早期评论人计划", "亚马逊早期评论人计划扣款"),

    /**
     * 清算调整费用 - 对应summary中Liquidation Adjustment
     */
    REMOVAL_SHIPMENT("RemovalShipment", "REMOVAL", "清算调整费用", "对应summary中Liquidation Adjustment"),

    /**
     * 服务费 - 店铺或账号维度的服务费
     */
    SERVICE_FEE("ServiceFee", "SERVICE_FEE", "服务费", "亚马逊店铺或账号维度的服务费，主要包括订阅费、促销费等"),

    /**
     * 秒杀费用
     */
    SELLER_DEAL_PAYMENT("SellerDealPayment", "DEAL_PAYMENT", "秒杀费用", "亚马逊Lightning Deal Fee费用"),

    /**
     * 亚马逊支付 - 第三方收款手续费
     */
    PAY_WITH_AMAZON("PayWithAmazon", "SERVICE_FEE", "亚马逊支付", "用户使用亚马逊账户绑定第三方平台进行收款，亚马逊会提供收款服务在此服务中扣除手续费"),

    /**
     * 优惠券手续费
     */
    COUPON_PAYMENT("CouponPayment", "COUPON", "优惠券手续费", "产生促销订单后扣除优惠券的手续费用，0.06美元（美国）或60日元（日本）"),

    /**
     * 亚马逊库存清算服务
     */
    FBA_LIQUIDATION("FBALiquidation", "REMOVAL", "亚马逊库存清算服务", "移除中类型为清算的订单，费用在此类型中结算"),

    /**
     * 赔偿撤销
     */
    RETROCHARGE("Retrocharge", "RETROCHARGE", "赔偿撤销", "分两种 order撤销和赔偿撤销。属于已经赔偿后重新撤销的金额，目前此事件含税费"),

    /**
     * 移除发货调整（兼容历史数据）
     */
    REMOVAL_SHIPMENT_ADJUSTMENT("RemovalShipmentAdjustment", "REMOVAL_ADJUSTMENT", "移除调整", "移除相关税费调整"),

    /**
     * 未知/其他来源
     */
    OTHER("Other", "OTHER", "其他", "未分类结算");
    
    /**
     * ERP数据中的来源值
     */
    private final String sourceValue;
    
    /**
     * 结算类型代码
     */
    private final String settlementCategory;
    
    /**
     * 结算类型名称
     */
    private final String settlementName;
    
    /**
     * 描述说明
     */
    private final String description;
    
    ErpSourceType(String sourceValue, String settlementCategory, String settlementName, String description) {
        this.sourceValue = sourceValue;
        this.settlementCategory = settlementCategory;
        this.settlementName = settlementName;
        this.description = description;
    }
    
    /**
     * 根据ERP来源值获取枚举
     * 
     * @param sourceValue ERP数据中的来源字段值
     * @return 对应的枚举，未匹配返回OTHER
     */
    public static ErpSourceType fromSourceValue(String sourceValue) {
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            return OTHER;
        }
        String trimmed = sourceValue.trim();
        for (ErpSourceType type : values()) {
            if (type.sourceValue.equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据结算类型代码获取枚举
     * 
     * @param settlementCategory 结算类型代码
     * @return 对应的枚举，未匹配返回OTHER
     */
    public static ErpSourceType fromSettlementCategory(String settlementCategory) {
        if (settlementCategory == null || settlementCategory.trim().isEmpty()) {
            return OTHER;
        }
        for (ErpSourceType type : values()) {
            if (type.settlementCategory.equalsIgnoreCase(settlementCategory)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 判断是否为订单类型（正常销售）
     */
    public boolean isOrder() {
        return this == SHIPMENT;
    }
    
    /**
     * 判断是否为退款类型
     */
    public boolean isRefund() {
        return this == REFUND || this == CHARGEBACK;
    }
    
    /**
     * 判断是否为费用类型
     */
    public boolean isFee() {
        return this == SERVICE_FEE || this == COUPON_PAYMENT
                || this == SELLER_DEAL_PAYMENT || this == PRODUCT_ADS_PAYMENT
                || this == SELLER_REVIEW_ENROLLMENT || this == PAY_WITH_AMAZON;
    }

    /**
     * 判断是否为调整类型
     */
    public boolean isAdjustment() {
        return this == ADJUSTMENT || this == RETROCHARGE
                || this == REMOVAL_SHIPMENT_ADJUSTMENT || this == GUARANTEE_CLAIM
                || this == DEBT_RECOVERY;
    }

    /**
     * 判断是否为移除类型
     */
    public boolean isRemoval() {
        return this == REMOVAL_SHIPMENT || this == REMOVAL_SHIPMENT_ADJUSTMENT
                || this == FBA_LIQUIDATION;
    }
}
