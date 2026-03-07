package com.musheng.business.document.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 单据类型枚举
 *
 * <p>定义FBA单据自动生成功能中的四种单据类型：
 * PO采购订单、DN送货单、结算单、INV发票。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Getter
@AllArgsConstructor
public enum DocumentType {

    /** 采购订单 */
    PO("PO", "采购订单"),

    /** 送货单 */
    DN("DN", "送货单"),

    /** 结算单 */
    SETTLEMENT("SETTLEMENT", "结算单"),

    /** 发票 */
    INV("INV", "发票");

    /** 单据类型代码 */
    private final String code;

    /** 单据类型描述 */
    private final String description;

    /**
     * 根据代码查找单据类型
     *
     * @param code 单据类型代码
     * @return 匹配的单据类型，未找到返回 null
     */
    public static DocumentType fromCode(String code) {
        for (DocumentType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
