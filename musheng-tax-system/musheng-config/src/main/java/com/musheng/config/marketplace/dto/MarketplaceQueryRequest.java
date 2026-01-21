package com.musheng.config.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Marketplace Query Request DTO
 */
@Data
@Schema(description = "Marketplace Query Request")
public class MarketplaceQueryRequest {

    @Schema(description = "Site code", example = "US")
    private String siteCode;

    @Schema(description = "Site name", example = "United States")
    private String siteName;

    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
}
