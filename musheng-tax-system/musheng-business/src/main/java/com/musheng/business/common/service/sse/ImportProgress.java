package com.musheng.business.common.service.sse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Import Progress DTO for SSE
 * P0 Requirement: Import progress SSE API
 */
@Data
@Builder
@Schema(description = "Import Progress")
public class ImportProgress {

    @Schema(description = "Import batch number")
    private String batchNo;

    @Schema(description = "Current status (pending/processing/success/partial/fail)")
    private String status;

    @Schema(description = "Total record count")
    private Integer totalCount;

    @Schema(description = "Processed count")
    private Integer processedCount;

    @Schema(description = "Success count")
    private Integer successCount;

    @Schema(description = "Fail count")
    private Integer failCount;

    @Schema(description = "Progress percentage (0-100)")
    private Integer percentage;

    @Schema(description = "Current processing message")
    private String message;

    @Schema(description = "Error details (if any)")
    private String errorDetails;

    @Schema(description = "Estimated remaining seconds")
    private Integer estimatedSeconds;

    /**
     * Create initial progress
     */
    public static ImportProgress initial(String batchNo, int totalCount) {
        return ImportProgress.builder()
                .batchNo(batchNo)
                .status("processing")
                .totalCount(totalCount)
                .processedCount(0)
                .successCount(0)
                .failCount(0)
                .percentage(0)
                .message("Starting import...")
                .build();
    }

    /**
     * Create progress update
     */
    public static ImportProgress update(String batchNo, int totalCount, int processedCount,
                                         int successCount, int failCount, String message) {
        int percentage = totalCount > 0 ? (processedCount * 100 / totalCount) : 0;
        return ImportProgress.builder()
                .batchNo(batchNo)
                .status("processing")
                .totalCount(totalCount)
                .processedCount(processedCount)
                .successCount(successCount)
                .failCount(failCount)
                .percentage(percentage)
                .message(message)
                .build();
    }

    /**
     * Create completed progress
     */
    public static ImportProgress completed(String batchNo, int totalCount, int successCount,
                                            int failCount) {
        String status = failCount == 0 ? "success" : (successCount == 0 ? "fail" : "partial");
        return ImportProgress.builder()
                .batchNo(batchNo)
                .status(status)
                .totalCount(totalCount)
                .processedCount(totalCount)
                .successCount(successCount)
                .failCount(failCount)
                .percentage(100)
                .message("Import completed")
                .build();
    }

    /**
     * Create failed progress
     */
    public static ImportProgress failed(String batchNo, String errorDetails) {
        return ImportProgress.builder()
                .batchNo(batchNo)
                .status("fail")
                .percentage(0)
                .message("Import failed")
                .errorDetails(errorDetails)
                .build();
    }
}
