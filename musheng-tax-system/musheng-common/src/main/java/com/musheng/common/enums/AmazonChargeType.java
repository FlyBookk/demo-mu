package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 亚马逊 SP-API 收费/扣款类型枚举
 * 对应 Finances API 中 ChargeComponent.ChargeType 字段
 * 参考：https://developer-docs.amazon.com/sp-api/docs/finances-api-v2024-06-19-reference
 *
 * @author wanhua
 * 10:30 2026年02月23日
 */
@Getter
@AllArgsConstructor
public enum AmazonChargeType {

    // ============== 商品金额类 ==============

    /** 商品售价（单价 × 数量） */
    PRINCIPAL("Principal", "商品售价"),

    /** 卖家代收的商品税 */
    TAX("Tax", "商品税"),

    /** 促销折扣金额 */
    DISCOUNT("Discount", "促销折扣"),

    /** 促销折扣对应的税额减免 */
    TAX_DISCOUNT("TaxDiscount", "促销折扣税额"),

    // ============== 运费类 ==============

    /** 运费（向买家收取的配送费） */
    SHIPPING_CHARGE("ShippingCharge", "运费"),

    /** 运费税 */
    SHIPPING_TAX("ShippingTax", "运费税"),

    // ============== 礼品包装类 ==============

    /** 礼品包装费 */
    GIFTWRAP("Giftwrap", "礼品包装费"),

    /** 礼品包装税 */
    GIFTWRAP_TAX("GiftwrapTax", "礼品包装税"),

    // ============== 平台代扣税类（Marketplace Facilitator Tax） ==============

    /** 平台代扣的商品税 */
    MF_TAX_PRINCIPAL("MarketplaceFacilitatorTax-Principal", "平台代扣商品税"),

    /** 平台代扣的运费税 */
    MF_TAX_SHIPPING("MarketplaceFacilitatorTax-Shipping", "平台代扣运费税"),

    /** 平台代扣的礼品包装税 */
    MF_TAX_GIFTWRAP("MarketplaceFacilitatorTax-Giftwrap", "平台代扣礼品包装税"),

    /** 平台代扣的其他税 */
    MF_TAX_OTHER("MarketplaceFacilitatorTax-Other", "平台代扣其他税"),

    // ============== 货到付款类（COD） ==============

    /** 货到付款商品费 */
    COD_ITEM_CHARGE("CODItemCharge", "COD商品费"),

    /** 货到付款商品税 */
    COD_ITEM_TAX_CHARGE("CODItemTaxCharge", "COD商品税"),

    /** 货到付款订单费 */
    COD_ORDER_CHARGE("CODOrderCharge", "COD订单费"),

    /** 货到付款订单税 */
    COD_ORDER_TAX_CHARGE("CODOrderTaxCharge", "COD订单税"),

    /** 货到付款运费 */
    COD_SHIPPING_CHARGE("CODShippingCharge", "COD运费"),

    /** 货到付款运费税 */
    COD_SHIPPING_TAX_CHARGE("CODShippingTaxCharge", "COD运费税"),

    // ============== 补偿/退款类 ==============

    /** 善意补偿（买家体验补偿） */
    GOODWILL("Goodwill", "善意补偿"),

    /** 退货重新上架费（向买家收取） */
    RESTOCKING_FEE("RestockingFee", "退货重新上架费"),

    /** 退货运费补偿（亚马逊过错时补偿买家） */
    RETURN_SHIPPING("ReturnShipping", "退货运费补偿"),

    /** 免费换货退货运费补偿 */
    FREE_REPLACEMENT_RETURN_SHIPPING("FreeReplacementReturnShipping", "免费换货退货运费"),

    /** SAFE-T 索赔赔偿金额 */
    SAFE_T_REIMBURSEMENT("SAFE-TReimbursement", "SAFE-T索赔赔偿"),

    // ============== 扣款/手续费类 ==============

    /** 通用坏账扣除 */
    GENERIC_DEDUCTION("GenericDeduction", "通用坏账扣除"),

    /** Amazon Points 积分扣除 */
    POINTS_FEE("PointsFee", "积分扣除"),

    /** 支付方式手续费（部分站点特定支付方式） */
    PAYMENT_METHOD_FEE("PaymentMethodFee", "支付方式手续费"),

    /** 出口关税（Amazon Global 跨境订单） */
    EXPORT_CHARGE("ExportCharge", "出口关税"),

    // ============== 印度站 TCS 税类 ==============

    /** TCS 中央商品服务税 */
    TCS_CGST("TCS-CGST", "TCS中央商品服务税"),

    /** TCS 邦商品服务税 */
    TCS_SGST("TCS-SGST", "TCS邦商品服务税"),

    /** TCS 综合商品服务税 */
    TCS_IGST("TCS-IGST", "TCS综合商品服务税"),

    /** TCS 联邦直辖区商品服务税 */
    TCS_UTGST("TCS-UTGST", "TCS联邦直辖区商品服务税"),

    // ============== EBT 支付 ==============

    /** EBT（电子福利转账）支付金额 */
    PAID_THROUGH_EBT("PaidthroughEBT", "EBT支付金额"),

    // ============== 未知/其他 ==============

    /** 未识别的收费类型 */
    UNKNOWN("Unknown", "未知收费类型");

    /** SP-API 返回的收费类型编码 */
    private final String code;

    /** 中文描述 */
    private final String description;

    /**
     * 根据 SP-API 返回的 ChargeType 编码查找枚举
     *
     * @param code SP-API ChargeType 编码
     * @return 对应的枚举值，未匹配时返回 UNKNOWN
     */
    public static AmazonChargeType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (AmazonChargeType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
