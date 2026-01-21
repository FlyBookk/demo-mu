package com.musheng.config.mapping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ERP数据聚合规则实体
 * 映射到 t_erp_aggregate_rule 表
 */
@Data
@TableName("t_erp_aggregate_rule")
public class ErpAggregateRule {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * ERP交易类型
     */
    private String transactionType;

    /**
     * 目标金额字段
     */
    private String targetField;

    /**
     * 说明
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
