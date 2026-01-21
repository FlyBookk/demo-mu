package com.musheng.business.common.service.upload;

import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传控制器
 * P0需求: 分片上传接口(uploadChunk/mergeChunks)
 */
@Tag(name = "分片上传", description = "大文件分片上传接口")
@RestController
@RequestMapping("/v1/upload")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @Operation(summary = "上传分片", description = "上传大文件的单个分片")
    @PostMapping("/chunk")
    public Result<ChunkUploadResult> uploadChunk(
            @Parameter(description = "分片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件标识(MD5)") @RequestParam("fileId") String fileId,
            @Parameter(description = "分片序号(从0开始)") @RequestParam("chunkNumber") Integer chunkNumber,
            @Parameter(description = "总分片数") @RequestParam("totalChunks") Integer totalChunks,
            @Parameter(description = "原始文件名") @RequestParam("fileName") String fileName) {

        ChunkUploadResult result = chunkUploadService.uploadChunk(file, fileId, chunkNumber, totalChunks, fileName);
        return Result.success(result);
    }

    @Operation(summary = "合并分片", description = "将所有分片合并为一个文件")
    @PostMapping("/merge")
    public Result<ChunkMergeResult> mergeChunks(
            @Parameter(description = "文件标识(MD5)") @RequestParam("fileId") String fileId,
            @Parameter(description = "总分片数") @RequestParam("totalChunks") Integer totalChunks,
            @Parameter(description = "原始文件名") @RequestParam("fileName") String fileName) {

        ChunkMergeResult result = chunkUploadService.mergeChunks(fileId, totalChunks, fileName);
        return Result.success(result);
    }

    @Operation(summary = "检查分片", description = "检查分片是否已存在(用于断点续传)")
    @GetMapping("/chunk/check")
    public Result<Boolean> checkChunkExists(
            @Parameter(description = "文件标识(MD5)") @RequestParam("fileId") String fileId,
            @Parameter(description = "分片序号") @RequestParam("chunkNumber") Integer chunkNumber) {

        boolean exists = chunkUploadService.checkChunkExists(fileId, chunkNumber);
        return Result.success(exists);
    }

    @Operation(summary = "已上传分片列表", description = "获取已上传的分片序号列表")
    @GetMapping("/chunks")
    public Result<Integer[]> getUploadedChunks(
            @Parameter(description = "文件标识(MD5)") @RequestParam("fileId") String fileId) {

        Integer[] chunks = chunkUploadService.getUploadedChunks(fileId);
        return Result.success(chunks);
    }

    @Operation(summary = "清理分片", description = "清理未完成的上传分片")
    @DeleteMapping("/chunks")
    public Result<Void> cleanupChunks(
            @Parameter(description = "文件标识(MD5)") @RequestParam("fileId") String fileId) {

        chunkUploadService.cleanupChunks(fileId);
        return Result.success();
    }

    @Operation(summary = "简单上传", description = "直接上传小文件(无需分片)")
    @PostMapping("/simple")
    public Result<ChunkMergeResult> simpleUpload(
            @Parameter(description = "待上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "数据类型(SALES/SHIPPING/RATE/ADVERTISING)") @RequestParam(value = "dataType", required = false) String dataType,
            @Parameter(description = "站点ID") @RequestParam(value = "marketplaceId", required = false) Long marketplaceId) {

        ChunkMergeResult result = chunkUploadService.simpleUpload(file, dataType, marketplaceId);
        return Result.success(result);
    }
}
