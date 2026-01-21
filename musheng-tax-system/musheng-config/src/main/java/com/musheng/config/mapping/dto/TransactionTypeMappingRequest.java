package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Transaction Type Mapping Request DTO
 */
@Data
@Schema(description = "Transaction Type Mapping Request")
public class TransactionTypeMappingRequest {

    @Schema(description = "Site code (NULL for universal)", example = "US")
    private String siteCode;

    @Schema(description = "Original transaction type", example = "Order")
    @NotBlank(message = "Original type is required")
    private String originalType;

    @Schema(description = "Standard category (income/refund/fee/adjustment/other)", example = "income")
    @NotBlank(message = "Standard category is required")
    private String standardCategory;

    @Schema(description = "Category description", example = "Income")
    private String categoryDesc;

    @Schema(description = "Mapped transaction type", example = "")
    private String mappedType;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;
}
