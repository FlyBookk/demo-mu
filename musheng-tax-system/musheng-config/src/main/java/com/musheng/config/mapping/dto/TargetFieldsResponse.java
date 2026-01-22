package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 目标字段响应
 */
@Data
@Schema(description = "目标字段响应")
public class TargetFieldsResponse {

    @Schema(description = "数据类型", example = "SALES")
    private String dataType;

    @Schema(description = "数据源类型", example = "ORIGINAL")
    private String sourceType;

    @Schema(description = "数据源类型名称", example = "亚马逊原始数据")
    private String sourceTypeName;

    @Schema(description = "字段列表")
    private List<TargetFieldVO> fields;

    @Schema(description = "聚合配置（仅ERP数据有）")
    private AggregateConfig aggregateConfig;

    /**
     * 目标字段VO
     */
    @Data
    @Schema(description = "目标字段")
    public static class TargetFieldVO {

        @Schema(description = "字段名", example = "order_id")
        private String field;

        @Schema(description = "中文标签", example = "订单ID")
        private String label;

        @Schema(description = "字段描述", example = "亚马逊订单编号")
        private String description;

        @Schema(description = "字段类型", example = "string")
        private String type;

        @Schema(description = "是否必填", example = "true")
        private Boolean required;

        @Schema(description = "最大长度", example = "50")
        private Integer maxLength;

        @Schema(description = "精度", example = "2")
        private Integer precision;

        @Schema(description = "站点别名")
        private Map<String, String> siteAliases;

        @Schema(description = "排序序号", example = "1")
        private Integer sortOrder;
    }

    /**
     * 聚合配置
     */
    @Data
    @Schema(description = "ERP数据聚合配置")
    public static class AggregateConfig {

        @Schema(description = "分组字段", example = "[\"order_id\", \"sku\", \"site_code\"]")
        private List<String> groupBy;

        @Schema(description = "透视字段（交易类型）", example = "transaction_type")
        private String pivotField;

        @Schema(description = "值字段（金额）", example = "amount")
        private String valueField;

        @Schema(description = "透视映射（交易类型 -> 目标字段）")
        private Map<String, String> pivotMapping;
    }
}
