package com.musheng.config.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Currency Request DTO
 */
@Data
@Schema(description = "Currency Request")
public class CurrencyRequest {

    @Schema(description = "Currency code", example = "USD")
    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @Schema(description = "Currency name", example = "US Dollar")
    @NotBlank(message = "Currency name is required")
    private String currencyName;

    @Schema(description = "Currency symbol", example = "$")
    private String currencySymbol;

    @Schema(description = "Decimal places", example = "2")
    private Integer decimalPlaces;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;
}
