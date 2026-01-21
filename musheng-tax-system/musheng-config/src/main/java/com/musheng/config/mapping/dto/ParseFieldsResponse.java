package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 解析字段响应
 */
@Data
@Schema(description = "解析字段响应")
public class ParseFieldsResponse {

    @Schema(description = "字段列表")
    private List<SourceFieldVO> fields;

    @Schema(description = "总列数", example = "25")
    private Integer totalColumns;

    @Schema(description = "表头行号", example = "1")
    private Integer headerRow;

    @Schema(description = "检测到的编码", example = "UTF-8")
    private String encoding;

    @Schema(description = "检测到的分隔符", example = ",")
    private String delimiter;

    /**
     * 源字段VO
     */
    @Data
    @Schema(description = "源字段")
    public static class SourceFieldVO {

        @Schema(description = "字段名", example = "order_id")
        private String name;

        @Schema(description = "示例值", example = "028-7890123-1234567")
        private String sample;

        @Schema(description = "列索引", example = "0")
        private Integer index;
    }
}
