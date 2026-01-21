package com.musheng.config.mapping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 交易类型映射实体
 * 映射到 t_transaction_type_mapping 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_transaction_type_mapping")
public class TransactionTypeMapping extends BaseEntity {

    /**
     * 站点编码(US/CA/UK/DE, NULL表示通用)
     */
    private String siteCode;

    /**
     * 亚马逊原始交易类型
     */
    private String originalType;

    /**
     * 标准分类(income/refund/fee/adjustment/other)
     */
    @TableField("standard_category")
    private String standardCategory;

    /**
     * 分类说明
     */
    @TableField("category_desc")
    private String categoryDesc;

    /**
     * 映射后的交易类型
     */
    private String mappedType;

    /**
     * 状态(1启用, 0禁用)
     */
    private Integer status;
}
