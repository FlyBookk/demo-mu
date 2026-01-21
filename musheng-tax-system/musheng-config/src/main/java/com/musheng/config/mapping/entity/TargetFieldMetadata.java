package com.musheng.config.mapping.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 目标字段元数据实体
 * 映射到 t_target_field_metadata 表
 */
@Data
@TableName(value = "t_target_field_metadata", autoResultMap = true)
public class TargetFieldMetadata {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据类型：SALES, SHIPPING, ADVERTISING, RATE
     */
    private String dataType;

    /**
     * 子类型：ORIGINAL, ERP（仅SALES有效）
     */
    private String subType;

    /**
     * 字段名（数据库字段）
     */
    private String fieldName;

    /**
     * 中文标签
     */
    private String fieldLabel;

    /**
     * 字段描述
     */
    private String fieldDescription;

    /**
     * 字段类型：string, number, datetime, boolean
     */
    private String fieldType;

    /**
     * 是否必填：0-否, 1-是
     */
    private Boolean required;

    /**
     * 最大长度（string类型）
     */
    private Integer maxLength;

    /**
     * 精度（number类型）
     */
    @TableField("precision_value")
    private Integer precision;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 站点字段别名
     * 格式: {"US": "order id", "DE": "Bestellnummer"}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> siteAliases;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
