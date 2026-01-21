package com.musheng.config.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Currency Query Request DTO
 */
@Data
@Schema(description = "Currency Query Request")
public class CurrencyQueryRequest {

    @Schema(description = "Currency code", example = "USD")
    private String currencyCode;

    @Schema(description = "Currency name", example = "Dollar")
    private String currencyName;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
}
