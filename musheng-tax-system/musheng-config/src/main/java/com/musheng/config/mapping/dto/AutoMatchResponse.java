package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能匹配响应
 */
@Data
@Schema(description = "智能匹配响应")
public class AutoMatchResponse {

    @Schema(description = "匹配建议列表")
    private List<MatchSuggestion> mappings;

    @Schema(description = "未匹配的源字段")
    private List<String> unmatchedSource;

    @Schema(description = "未匹配的目标字段")
    private List<String> unmatchedTarget;

    /**
     * 匹配建议
     */
    @Data
    @Schema(description = "匹配建议")
    public static class MatchSuggestion {

        @Schema(description = "源字段名", example = "order_id")
        private String source;

        @Schema(description = "目标字段名", example = "order_id")
        private String target;

        @Schema(description = "匹配置信度(0-1)", example = "0.95")
        private Double confidence;

        @Schema(description = "匹配类型", example = "exact")
        private String matchType;
    }
}
