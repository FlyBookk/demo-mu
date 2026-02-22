package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字段映射模板请求 DTO
 */
@Data
@Schema(description = "字段映射模板请求")
public class FieldMappingTemplateRequest {

    @Schema(description = "模板名称", example = "德国站销售数据模板")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "站点编码", example = "DE")
    @NotBlank(message = "站点编码不能为空")
    private String siteCode;

    @Schema(description = "数据类型", example = "SALES")
    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    @Schema(description = "数据源类型（仅销售数据有效）：ORIGINAL/ERP", example = "ORIGINAL")
    private String sourceType;

    @Schema(description = "映射配置(JSON格式)", example = "[{\"source\":\"order id\",\"target\":\"order_id\",\"sourceIndex\":0}]")
    private String mappingConfig;

    @Schema(description = "源字段列表(JSON格式)", example = "[{\"name\":\"order id\",\"sample\":\"DE-123\",\"index\":0}]")
    private String sourceFields;

    @Schema(description = "表头行号", example = "1")
    private Integer headerRow;

    @Schema(description = "默认值配置(JSON格式)", example = "[{\"field\":\"currency_code\",\"value\":\"EUR\"}]")
    private String defaultValues;

    @Schema(description = "是否默认模板", example = "false")
    private Boolean isDefault;

    @Schema(description = "是否在列表中显示", example = "true")
    private Boolean isVisible;
}
