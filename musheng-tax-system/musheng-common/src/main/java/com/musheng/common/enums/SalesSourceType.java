package com.musheng.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 销售数据源类型枚举
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Getter
public enum SalesSourceType {
    
    /**
     * 亚马逊原始数据
     * 特点：按国家分文件，表头为英文/德文，每行是完整订单信息
     */
    ORIGINAL("ORIGINAL", "亚马逊原始数据"),
    
    /**
     * ERP结算数据
     * 特点：多国家合并，中文表头，每行是费用明细（需要行转列聚合）
     */
    ERP("ERP", "ERP结算数据");
    
    @JsonValue
    private final String code;
    
    private final String description;
    
    SalesSourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static SalesSourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SalesSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SalesSourceType: " + code);
    }
    
    /**
     * 判断是否为亚马逊原始数据
     */
    public boolean isOriginal() {
        return this == ORIGINAL;
    }
    
    /**
     * 判断是否为ERP数据
     */
    public boolean isErp() {
        return this == ERP;
    }
}
