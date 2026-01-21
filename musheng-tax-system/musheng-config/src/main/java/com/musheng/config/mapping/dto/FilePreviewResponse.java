package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 文件预览响应
 */
@Data
@Schema(description = "文件预览响应")
public class FilePreviewResponse {

    @Schema(description = "预览行列表")
    private List<FilePreviewRow> rows;

    @Schema(description = "总行数", example = "1000")
    private Integer totalRows;

    @Schema(description = "检测到的编码", example = "UTF-8")
    private String encoding;

    @Schema(description = "检测到的分隔符", example = ",")
    private String delimiter;

    @Schema(description = "Sheet名称（Excel文件）", example = "Sheet1")
    private String sheetName;

    @Schema(description = "所有Sheet名称列表（Excel文件）")
    private List<String> sheets;

    /**
     * 文件预览行
     */
    @Data
    @Schema(description = "文件预览行")
    public static class FilePreviewRow {

        @Schema(description = "行号", example = "1")
        private Integer rowNum;

        @Schema(description = "行内容", example = "order_id\tsku\tamount")
        private String content;

        @Schema(description = "单元格列表")
        private List<String> cells;
    }
}
