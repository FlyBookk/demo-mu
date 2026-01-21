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
     * 数据类型(sales/shipping)
     */
    private String dataType;

    /**
     * 映射配置(JSON格式)
     */
    @TableField("mapping_config")
    private String mappingConfig;

    /**
     * 是否默认模板(1是/0否)
     */
    private Boolean isDefault;
}
