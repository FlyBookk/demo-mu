package com.musheng.business.advertising.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Advertising Data Request DTO
 * P0 Requirement: Advertising data CRUD API
 */
@Data
@Schema(description = "Advertising Data Request")
public class AdvertisingDataRequest {

    @NotBlank(message = "Site code is required")
    @Schema(description = "Site code", example = "US", requiredMode = Schema.RequiredMode.REQUIRED)
    private String siteCode;

    @NotBlank(message = "Year-month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Year-month format must be YYYY-MM")
    @Schema(description = "Year-month", example = "2025-07", requiredMode = Schema.RequiredMode.REQUIRED)
    private String yearMonth;

    @Schema(description = "Invoice number", example = "INV-2025-001")
    private String invoiceNo;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0", message = "Amount must be positive")
    @Schema(description = "Advertising amount (original currency)", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Schema(description = "Currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currencyCode;

    @Schema(description = "Invoice attachment path")
    private String attachmentPath;

    @Schema(description = "Remark")
    private String remark;
}
