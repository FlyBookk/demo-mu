package com.musheng.business.common.service.upload;

import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunk Upload Service Implementation
 * P0 Requirement: Chunk upload API (uploadChunk/mergeChunks)
 */
@Slf4j
@Service
public class ChunkUploadServiceImpl implements ChunkUploadService {

    @Value("${app.upload.chunk-dir:./uploads/chunks}")
    private String chunkDir;

    @Value("${app.upload.final-dir:./uploads/files}")
    private String finalDir;

    @Override
    public ChunkUploadResult uploadChunk(MultipartFile file, String fileId, Integer chunkNumber,
                                          Integer totalChunks, String fileName) {
        try {
            // Create chunk directory
            Path chunkPath = getChunkDir(fileId);
            Files.createDirectories(chunkPath);

            // Save chunk file
            File chunkFile = chunkPath.resolve(String.valueOf(chunkNumber)).toFile();
            file.transferTo(chunkFile);

            // Check if all chunks are uploaded
            boolean allUploaded = checkAllChunksUploaded(fileId, totalChunks);

            log.info("Chunk uploaded: fileId={}, chunk={}/{}, allUploaded={}",
                    fileId, chunkNumber + 1, totalChunks, allUploaded);

            return ChunkUploadResult.builder()
                    .fileId(fileId)
                    .chunkNumber(chunkNumber)
                    .success(true)
                    .allUploaded(allUploaded)
                    .message("Chunk uploaded successfully")
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload chunk: fileId={}, chunk={}", fileId, chunkNumber, e);
            throw new BusinessException(ErrorCode.UPLOAD_CHUNK_ERROR, "Failed to upload chunk: " + e.getMessage());
        }
    }

    @Override
    public ChunkMergeResult mergeChunks(String fileId, Integer totalChunks, String fileName) {
        try {
            // Create final directory
            Path finalPath = Paths.get(finalDir);
            Files.createDirectories(finalPath);

            // Generate final file path
            String finalFileName = fileId + "_" + fileName;
            File finalFile = finalPath.resolve(finalFileName).toFile();

            // Merge chunks
            Path chunkPath = getChunkDir(fileId);
            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                for (int i = 0; i < totalChunks; i++) {
                    File chunkFile = chunkPath.resolve(String.valueOf(i)).toFile();
                    if (!chunkFile.exists()) {
                        throw new BusinessException(ErrorCode.UPLOAD_MERGE_ERROR,
                                "Missing chunk: " + i);
                    }
                    byte[] bytes = Files.readAllBytes(chunkFile.toPath());
                    fos.write(bytes);
                }
            }

            // Clean up chunks
            cleanupChunks(fileId);

            log.info("Chunks merged: fileId={}, totalChunks={}, finalFile={}",
                    fileId, totalChunks, finalFile.getAbsolutePath());

            return ChunkMergeResult.builder()
                    .fileId(fileId)
                    .filePath(finalFile.getAbsolutePath())
                    .fileName(fileName)
                    .fileSize(finalFile.length())
                    .success(true)
                    .message("Chunks merged successfully")
                    .build();

        } catch (IOException e) {
            log.error("Failed to merge chunks: fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.UPLOAD_MERGE_ERROR, "Failed to merge chunks: " + e.getMessage());
        }
    }

    @Override
    public boolean checkChunkExists(String fileId, Integer chunkNumber) {
        Path chunkPath = getChunkDir(fileId).resolve(String.valueOf(chunkNumber));
        return Files.exists(chunkPath);
    }

    @Override
    public Integer[] getUploadedChunks(String fileId) {
        Path chunkPath = getChunkDir(fileId);
        if (!Files.exists(chunkPath)) {
            return new Integer[0];
        }

        File dir = chunkPath.toFile();
        File[] files = dir.listFiles();
        if (files == null) {
            return new Integer[0];
        }

        List<Integer> chunks = new ArrayList<>();
        for (File file : files) {
            try {
                chunks.add(Integer.parseInt(file.getName()));
            } catch (NumberFormatException ignored) {
            }
        }

        return chunks.toArray(new Integer[0]);
    }

    @Override
    public void cleanupChunks(String fileId) {
        try {
            Path chunkPath = getChunkDir(fileId);
            if (Files.exists(chunkPath)) {
                Files.walk(chunkPath)
                        .map(Path::toFile)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(File::delete);
                log.info("Cleaned up chunks: fileId={}", fileId);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup chunks: fileId={}", fileId, e);
        }
    }

    /**
     * Get chunk directory path
     */
    private Path getChunkDir(String fileId) {
        return Paths.get(chunkDir, fileId);
    }

    /**
     * Check if all chunks are uploaded
     */
    private boolean checkAllChunksUploaded(String fileId, Integer totalChunks) {
        Integer[] uploaded = getUploadedChunks(fileId);
        return uploaded.length >= totalChunks;
    }

    @Override
    public ChunkMergeResult simpleUpload(MultipartFile file, String dataType, Long marketplaceId) {
        try {
            // Create final directory
            Path finalPath = Paths.get(finalDir);
            Files.createDirectories(finalPath);

            // Generate file ID and final file name
            String originalFileName = file.getOriginalFilename();
            String fileId = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            String finalFileName = fileId + "_" + (originalFileName != null ? originalFileName : "file");
            File finalFile = finalPath.resolve(finalFileName).toFile();

            // Save file directly
            file.transferTo(finalFile);

            log.info("Simple upload completed: fileId={}, fileName={}, size={}, dataType={}",
                    fileId, originalFileName, file.getSize(), dataType);

            return ChunkMergeResult.builder()
                    .fileId(fileId)
                    .filePath(finalFile.getAbsolutePath())
                    .fileName(originalFileName)
                    .fileSize(file.getSize())
                    .success(true)
                    .message("File uploaded successfully")
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.UPLOAD_CHUNK_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }
}
