package com.musheng.business.common.service.csv;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * CSV Header Parse Result
 */
@Data
@Builder
@Schema(description = "CSV Header Parse Result")
public class CsvHeaderResult {

    @Schema(description = "Header row index (0-based)")
    private Integer headerRowIndex;

    @Schema(description = "Header column names")
    private List<String> headers;

    @Schema(description = "Detected site code")
    private String detectedSiteCode;

    @Schema(description = "Detected header language (EN/DE/CN)")
    private String headerLanguage;

    @Schema(description = "Total row count (including header)")
    private Integer totalRows;

    @Schema(description = "Data row count (excluding header)")
    private Integer dataRows;

    @Schema(description = "Sample data rows (first 5 rows)")
    private List<Map<String, String>> sampleData;

    @Schema(description = "Detected file charset (UTF-8/GBK)")
    private String charset;
}
