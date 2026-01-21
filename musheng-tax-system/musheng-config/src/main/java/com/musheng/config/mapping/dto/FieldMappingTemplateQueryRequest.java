package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Field Mapping Template Query Request DTO
 */
@Data
@Schema(description = "Field Mapping Template Query Request")
public class FieldMappingTemplateQueryRequest {

    @Schema(description = "Template name", example = "US Sales")
    private String templateName;

    @Schema(description = "Site code", example = "US")
    private String siteCode;

    @Schema(description = "Data type", example = "sales")
    private String dataType;

    @Schema(description = "Is default template")
    private Boolean isDefault;

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
}
