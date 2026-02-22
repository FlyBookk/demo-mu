package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 亚马逊 SP-API 费用类型枚举
 * 对应 Finances API 中 FeeComponent.FeeType 字段
 * 参考：https://developer-docs.amazon.com/sp-api/docs/finances-api-v2024-06-19-reference
 *
 * @author wanhua
 * 10:30 2026年02月23日
 */
@Getter
@AllArgsConstructor
public enum AmazonFeeType {

    // ============== 销售佣金类 ==============

    /** 销售佣金/推荐费，按品类百分比收取 */
    COMMISSION("Commission", "销售佣金"),

    /** 推荐费（与 Commission 同义，部分站点使用此名称） */
    REFERRAL_FEE("ReferralFee", "推荐费"),

    /** 固定成交费，媒体类商品每笔订单收取 */
    FIXED_CLOSING_FEE("FixedClosingFee", "固定成交费"),

    /** 可变成交费 */
    VARIABLE_CLOSING_FEE("VariableClosingFee", "可变成交费"),

    /** 每件商品费用（个人卖家计划） */
    PER_ITEM_FEE("PerItemFee", "每件商品费"),

    // ============== FBA 配送费类 ==============

    /** FBA 每件配送费 */
    FBA_PER_UNIT_FULFILLMENT_FEE("FBAPerUnitFulfillmentFee", "FBA每件配送费"),

    /** FBA 每单配送费 */
    FBA_PER_ORDER_FULFILLMENT_FEE("FBAPerOrderFulfillmentFee", "FBA每单配送费"),

    /** FBA 重量附加费 */
    FBA_WEIGHT_BASED_FEE("FBAWeightBasedFee", "FBA重量附加费"),

    /** FBA 入库运输费 */
    FBA_INBOUND_TRANSPORTATION_FEE("FBAInboundTransportationFee", "FBA入库运输费"),

    // ============== FBA 仓储费类 ==============

    /** FBA 长期仓储费（超过365天的库存） */
    FBA_LONG_TERM_STORAGE_FEE("FBALongTermStorageFee", "FBA长期仓储费"),

    // ============== FBA 退货费类 ==============

    /** FBA 客户退货每件费 */
    FBA_CUSTOMER_RETURN_PER_UNIT_FEE("FBACustomerReturnPerUnitFee", "FBA客户退货每件费"),

    /** FBA 客户退货每单费 */
    FBA_CUSTOMER_RETURN_PER_ORDER_FEE("FBACustomerReturnPerOrderFee", "FBA客户退货每单费"),

    /** FBA 客户退货重量费 */
    FBA_CUSTOMER_RETURN_WEIGHT_BASED_FEE("FBACustomerReturnWeightBasedFee", "FBA客户退货重量费"),

    // ============== FBA 移除/处置费类 ==============

    /** FBA 移除费（将库存退回卖家） */
    FBA_REMOVAL_FEE("FBARemovalFee", "FBA移除费"),

    /** FBA 处置费（销毁库存） */
    FBA_DISPOSAL_FEE("FBADisposalFee", "FBA处置费"),

    // ============== 运费相关 ==============

    /** 运费退款/回扣 */
    SHIPPING_CHARGEBACK("ShippingChargeback", "运费退款"),

    /** 运费回扣 */
    SHIPPING_HB("ShippingHB", "运费回扣"),

    // ============== 礼品包装 ==============

    /** 礼品包装退款 */
    GIFTWRAP_CHARGEBACK("GiftwrapChargeback", "礼品包装退款"),

    // ============== 促销/活动费类 ==============

    /** Lightning Deal 促销费 */
    RUN_LIGHTNING_DEAL_FEE("RunLightningDealFee", "秒杀活动费"),

    // ============== 未知/其他 ==============

    /** 未识别的费用类型 */
    UNKNOWN("Unknown", "未知费用类型");

    /** SP-API 返回的费用类型编码 */
    private final String code;

    /** 中文描述 */
    private final String description;

    /**
     * 根据 SP-API 返回的 FeeType 编码查找枚举
     *
     * @param code SP-API FeeType 编码
     * @return 对应的枚举值，未匹配时返回 UNKNOWN
     */
    public static AmazonFeeType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (AmazonFeeType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
