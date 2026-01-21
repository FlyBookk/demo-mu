package com.musheng.config.mapping.service;

import com.musheng.config.mapping.dto.FilePreviewResponse;
import com.musheng.config.mapping.dto.ParseFieldsResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件解析服务接口
 */
public interface FileParseService {

    /**
     * 预览文件
     *
     * @param file        文件
     * @param previewRows 预览行数
     * @return 预览结果
     */
    FilePreviewResponse previewFile(MultipartFile file, Integer previewRows);

    /**
     * 解析文件获取源字段
     *
     * @param file      文件
     * @param headerRow 表头行号
     * @param sheetName Sheet名称（Excel可选）
     * @return 解析结果
     */
    ParseFieldsResponse parseFields(MultipartFile file, Integer headerRow, String sheetName);
}
