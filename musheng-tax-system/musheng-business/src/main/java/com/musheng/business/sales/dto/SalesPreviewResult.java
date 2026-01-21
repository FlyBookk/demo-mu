package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 销售数据预览结果
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "销售数据预览结果")
public class SalesPreviewResult {
    
    @Schema(description = "总数据行数", example = "5000")
    private Integer totalRows;
    
    @Schema(description = "预览行数（最多10行）", example = "10")
    private Integer previewRows;
    
    @Schema(description = "列元数据")
    private List<ColumnMeta> columns;
    
    @Schema(description = "预览数据")
    private List<Map<String, Object>> data;
    
    @Schema(description = "映射状态统计")
    private MappingStatus mappingStatus;
    
    @Schema(description = "警告信息列表")
    private List<String> warnings;
    
}
