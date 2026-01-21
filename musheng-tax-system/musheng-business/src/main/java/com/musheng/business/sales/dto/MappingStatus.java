package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 映射状态统计
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "映射状态统计")
public class MappingStatus {
    
    @Schema(description = "总目标字段数", example = "20")
    private Integer totalFields;
    
    @Schema(description = "已映射字段数", example = "18")
    private Integer mappedFields;
    
    @Schema(description = "缺失的必填字段")
    private List<String> requiredMissing;
}
