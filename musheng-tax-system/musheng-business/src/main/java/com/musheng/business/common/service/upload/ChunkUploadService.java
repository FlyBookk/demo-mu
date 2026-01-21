package com.musheng.business.common.service.upload;

import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传服务接口
 * P0需求: 分片上传接口(uploadChunk/mergeChunks)
 */
public interface ChunkUploadService {

    /**
     * 上传单个分片
     *
     * @param file        分片文件
     * @param fileId      文件标识(通常为整个文件的MD5)
     * @param chunkNumber 当前分片序号(从0开始)
     * @param totalChunks 总分片数
     * @param fileName    原始文件名
     * @return 上传结果
     */
    ChunkUploadResult uploadChunk(MultipartFile file, String fileId, Integer chunkNumber,
                                   Integer totalChunks, String fileName);

    /**
     * 合并所有分片为一个文件
     *
     * @param fileId      文件标识
     * @param totalChunks 总分片数
     * @param fileName    原始文件名
     * @return 合并结果
     */
    ChunkMergeResult mergeChunks(String fileId, Integer totalChunks, String fileName);

    /**
     * 检查分片是否已存在(用于断点续传)
     *
     * @param fileId      文件标识
     * @param chunkNumber 分片序号
     * @return 是否存在
     */
    boolean checkChunkExists(String fileId, Integer chunkNumber);

    /**
     * 获取已上传的分片列表
     *
     * @param fileId 文件标识
     * @return 已上传分片序号数组
     */
    Integer[] getUploadedChunks(String fileId);

    /**
     * 清理未完成的上传分片
     *
     * @param fileId 文件标识
     */
    void cleanupChunks(String fileId);

    /**
     * 简单文件上传(用于小文件无需分片)
     *
     * @param file          待上传文件
     * @param dataType      数据类型(SALES/SHIPPING/RATE/ADVERTISING)
     * @param marketplaceId 站点ID(可选)
     * @return 上传结果
     */
    ChunkMergeResult simpleUpload(MultipartFile file, String dataType, Long marketplaceId);
}
