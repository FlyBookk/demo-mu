package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 销售数据导入结果
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Schema(description = "销售数据导入结果")
public class SalesImportResult {
    
    @Schema(description = "导入批次号", example = "IMP20260121143052001")
    private String batchNo;
    
    @Schema(description = "导入状态", example = "PROCESSING")
    private String status;
    
    @Schema(description = "总记录数", example = "5000")
    private Integer totalCount;
    
    @Schema(description = "成功数", example = "0")
    private Integer successCount;
    
    @Schema(description = "失败数", example = "0")
    private Integer failCount;
    
    @Schema(description = "跳过数（重复）", example = "0")
    private Integer skipCount;
    
    @Schema(description = "导入记录ID")
    private Long importRecordId;
    
    @Schema(description = "是否异步处理")
    private Boolean async;
    
    @Schema(description = "预估耗时（秒）")
    private Integer estimatedSeconds;
    
    @Schema(description = "错误信息列表（最多10条）")
    private java.util.List<String> errors;
}
