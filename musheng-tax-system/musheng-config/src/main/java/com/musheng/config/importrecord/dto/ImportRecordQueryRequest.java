package com.musheng.config.importrecord.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Import Record Query Request DTO
 */
@Data
@Schema(description = "Import Record Query Request")
public class ImportRecordQueryRequest {

    @Schema(description = "Batch number", example = "20240101120000001")
    private String batchNo;

    @Schema(description = "Data type (sales/shipping/advertising/rate)", example = "sales")
    private String dataType;

    @Schema(description = "File name", example = "sales_report.csv")
    private String fileName;

    @Schema(description = "Import status (pending/processing/success/partial/fail)", example = "success")
    private String importStatus;

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
}
