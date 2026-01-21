package com.musheng.business.common.service.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Chunk Merge Result DTO
 * P0 Requirement: Chunk upload API (uploadChunk/mergeChunks)
 */
@Data
@Builder
@Schema(description = "Chunk Merge Result")
public class ChunkMergeResult {

    @Schema(description = "File identifier (MD5)")
    private String fileId;

    @Schema(description = "Final file path")
    private String filePath;

    @Schema(description = "File name")
    private String fileName;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "Whether merge is successful")
    private Boolean success;

    @Schema(description = "Message")
    private String message;
}
