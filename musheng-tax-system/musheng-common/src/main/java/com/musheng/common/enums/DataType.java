package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据类型枚举 - 用于导入数据类型
 */
@Getter
@AllArgsConstructor
public enum DataType {

    SALES("sales", "销售数据"),
    SHIPPING("shipping", "配送数据"),
    ADVERTISING("advertising", "广告数据"),
    RATE("rate", "汇率数据");

    private final String code;
    private final String description;

    public static DataType fromCode(String code) {
        for (DataType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
