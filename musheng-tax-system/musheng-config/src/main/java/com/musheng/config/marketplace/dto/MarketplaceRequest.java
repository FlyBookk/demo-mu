package com.musheng.config.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Marketplace Request DTO
 */
@Data
@Schema(description = "Marketplace Request")
public class MarketplaceRequest {

    @Schema(description = "Site code", example = "US")
    @NotBlank(message = "Site code is required")
    private String siteCode;

    @Schema(description = "Site name", example = "United States")
    @NotBlank(message = "Site name is required")
    private String siteName;

    @Schema(description = "Marketplace ID", example = "ATVPDKIKX0DER")
    private String marketplaceId;

    @Schema(description = "Currency code", example = "USD")
    private String currencyCode;

    @Schema(description = "Seller ID", example = "A1234567890")
    private String sellerId;

    @Schema(description = "Header language", example = "EN")
    private String headerLanguage;

    @Schema(description = "Date format", example = "MM/dd/yyyy")
    private String dateFormat;

    @Schema(description = "Number format", example = ".")
    private String numberFormat;

    @Schema(description = "Timezone", example = "America/Los_Angeles")
    private String timezone;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;
}
