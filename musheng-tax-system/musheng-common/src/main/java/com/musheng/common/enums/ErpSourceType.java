package com.musheng.common.enums;

import lombok.Getter;

/**
 * ERP数据来源类型枚举
 * 定义ERP结算数据中的来源字段值及其对应的结算类型
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-24
 */
@Getter
public enum ErpSourceType {
    
    /**
     * 正常发货/销售
     */
    SHIPMENT("Shipment", "ORDER", "订单结算", "正常销售发货"),
    
    /**
     * 退款
     */
    REFUND("Refund", "REFUND", "退款结算", "客户退货退款"),
    
    /**
     * 服务费
     */
    SERVICE_FEE("ServiceFee", "SERVICE_FEE", "服务费结算", "FBA服务费、优惠券费等"),
    
    /**
     * 移除货物发货
     */
    REMOVAL_SHIPMENT("RemovalShipment", "REMOVAL", "移除结算", "库存移除发货"),
    
    /**
     * 调整（赔偿、追回等）
     */
    ADJUSTMENT("Adjustment", "ADJUSTMENT", "调整结算", "赔偿、追回、资金调整"),
    
    /**
     * 追溯收费（税费补缴）
     */
    RETROCHARGE("Retrocharge", "RETROCHARGE", "追溯结算", "税费追溯补缴"),
    
    /**
     * 优惠券支付费用
     */
    COUPON_PAYMENT("CouponPayment", "COUPON", "优惠券结算", "优惠券兑换费用"),
    
    /**
     * 移除发货调整
     */
    REMOVAL_SHIPMENT_ADJUSTMENT("RemovalShipmentAdjustment", "REMOVAL_ADJUSTMENT", "移除调整", "移除相关税费调整"),
    
    /**
     * 卖家促销付款
     */
    SELLER_DEAL_PAYMENT("SellerDealPayment", "DEAL_PAYMENT", "促销结算", "卖家促销活动费用"),
    
    /**
     * 退单/拒付
     */
    CHARGEBACK("Chargeback", "CHARGEBACK", "拒付结算", "买家拒付/退单"),
    
    /**
     * 产品广告付款
     */
    PRODUCT_ADS_PAYMENT("ProductAdsPayment", "ADS_PAYMENT", "广告结算", "产品广告费用"),
    
    /**
     * 未知/其他来源
     */
    OTHER("Other", "OTHER", "其他结算", "未分类结算");
    
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
        return this == SERVICE_FEE || this == COUPON_PAYMENT || 
               this == SELLER_DEAL_PAYMENT || this == PRODUCT_ADS_PAYMENT;
    }
    
    /**
     * 判断是否为调整类型
     */
    public boolean isAdjustment() {
        return this == ADJUSTMENT || this == RETROCHARGE || 
               this == REMOVAL_SHIPMENT_ADJUSTMENT;
    }
    
    /**
     * 判断是否为移除类型
     */
    public boolean isRemoval() {
        return this == REMOVAL_SHIPMENT || this == REMOVAL_SHIPMENT_ADJUSTMENT;
    }
}
