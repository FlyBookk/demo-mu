package com.musheng.common.dto;

import lombok.Data;

import java.util.Map;

/**
 * 目标字段信息
 * 通用的目标字段元数据对象，用于字段映射功能
 */
@Data
public class TargetFieldInfo {

    /**
     * 字段名（数据库字段名，下划线格式）
     */
    private String field;

    /**
     * 中文标签
     */
    private String label;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 字段类型：string, number, datetime, boolean
     */
    private String type;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 最大长度（String 类型有效）
     */
    private Integer maxLength;

    /**
     * 精度（Number 类型有效）
     */
    private Integer precision;

    /**
     * 站点别名
     */
    private Map<String, String> siteAliases;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
