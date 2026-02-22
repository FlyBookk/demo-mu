package com.musheng.config.mapping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段映射模板实体
 * 映射到 t_field_mapping_template 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_field_mapping_template")
public class FieldMappingTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 站点编码(US/CA/UK/DE)
     */
    private String siteCode;

    /**
     * 数据类型(SALES/SHIPPING/ADVERTISING/RATE)
     */
    private String dataType;

    /**
     * 数据源类型（仅销售数据有效）：ORIGINAL-原始数据, ERP-ERP数据
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 映射配置(JSON格式)
     * 格式: [{"source": "xxx", "target": "yyy", "sourceIndex": 0}]
     */
    @TableField("mapping_config")
    private String mappingConfig;

    /**
     * 源字段列表(JSON格式，可选保存)
     * 格式: [{"name": "xxx", "sample": "yyy", "index": 0}]
     */
    @TableField("source_fields")
    private String sourceFields;

    /**
     * 表头行号
     */
    private Integer headerRow;

    /**
     * 默认值配置(JSON格式)
     * 格式: [{"field": "xxx", "value": "yyy"}]
     */
    @TableField("default_values")
    private String defaultValues;

    /**
     * 是否默认模板(1是/0否)
     */
    private Boolean isDefault;

    /**
     * 是否在列表中显示(1-是/0-否)
     * 用于控制模板在管理列表中的可见性，不影响导入时的选择
     */
    private Boolean isVisible;
}
