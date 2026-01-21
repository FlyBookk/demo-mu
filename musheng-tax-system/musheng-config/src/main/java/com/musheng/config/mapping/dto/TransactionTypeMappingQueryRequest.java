package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Transaction Type Mapping Query Request DTO
 */
@Data
@Schema(description = "Transaction Type Mapping Query Request")
public class TransactionTypeMappingQueryRequest {

    @Schema(description = "Site code", example = "US")
    private String siteCode;

    @Schema(description = "Original transaction type", example = "Order")
    private String originalType;

    @Schema(description = "Mapped transaction type", example = "")
    private String mappedType;

    @Schema(description = "Standard category (income/refund/fee/adjustment/other)", example = "income")
    private String standardCategory;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
}
