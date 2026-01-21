package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段映射模板选项DTO（用于下拉选择）
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字段映射模板选项")
public class FieldMappingTemplateOptionDTO {
    
    @Schema(description = "模板ID")
    private Long id;
    
    @Schema(description = "模板名称")
    private String templateName;
    
    @Schema(description = "站点编码")
    private String siteCode;
    
    @Schema(description = "数据类型")
    private String dataType;
    
    @Schema(description = "数据源类型")
    private String sourceType;
    
    @Schema(description = "是否默认模板")
    private Boolean isDefault;
    
    @Schema(description = "映射字段数量")
    private Integer mappingCount;
}
