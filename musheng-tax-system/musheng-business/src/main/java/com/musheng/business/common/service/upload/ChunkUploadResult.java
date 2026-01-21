package com.musheng.business.common.service.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Chunk Upload Result DTO
 * P0 Requirement: Chunk upload API (uploadChunk/mergeChunks)
 */
@Data
@Builder
@Schema(description = "Chunk Upload Result")
public class ChunkUploadResult {

    @Schema(description = "File identifier (MD5)")
    private String fileId;

    @Schema(description = "Chunk number")
    private Integer chunkNumber;

    @Schema(description = "Whether upload is successful")
    private Boolean success;

    @Schema(description = "Whether all chunks are uploaded")
    private Boolean allUploaded;

    @Schema(description = "Message")
    private String message;
}
