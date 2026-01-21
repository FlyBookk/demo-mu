package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Field Mapping Template Request DTO
 */
@Data
@Schema(description = "Field Mapping Template Request")
public class FieldMappingTemplateRequest {

    @Schema(description = "Template name", example = "US Sales Template")
    @NotBlank(message = "Template name is required")
    private String templateName;

    @Schema(description = "Site code", example = "US")
    @NotBlank(message = "Site code is required")
    private String siteCode;

    @Schema(description = "Data type", example = "sales")
    @NotBlank(message = "Data type is required")
    private String dataType;

    @Schema(description = "Mapping configuration (JSON)")
    private String mappingConfig;

    @Schema(description = "Is default template", example = "false")
    private Boolean isDefault;
}
