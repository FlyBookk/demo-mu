package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易分类枚举
 */
@Getter
@AllArgsConstructor
public enum TransactionCategory {

    INCOME("income", "收入"),
    REFUND("refund", "退款"),
    FEE("fee", "费用"),
    OTHER_ADJUSTMENT("otherAdjustment", "其它调整"),
    ADJUSTMENT("adjustment", "调整"),
    TRANSFER("transfer", "划转"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    public static TransactionCategory fromCode(String code) {
        for (TransactionCategory category : values()) {
            if (category.getCode().equalsIgnoreCase(code)) {
                return category;
            }
        }
        return OTHER;
    }
}
