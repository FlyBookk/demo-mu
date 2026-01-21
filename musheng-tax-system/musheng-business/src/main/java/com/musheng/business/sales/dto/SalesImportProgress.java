package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 销售数据导入进度
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Schema(description = "销售数据导入进度")
public class SalesImportProgress {
    
    @Schema(description = "批次号")
    private String batchNo;
    
    @Schema(description = "状态：PENDING/PROCESSING/SUCCESS/PARTIAL/FAIL")
    private String status;
    
    @Schema(description = "总记录数")
    private Integer totalCount;
    
    @Schema(description = "已处理数")
    private Integer processedCount;
    
    @Schema(description = "成功数")
    private Integer successCount;
    
    @Schema(description = "失败数")
    private Integer failCount;
    
    @Schema(description = "跳过数")
    private Integer skipCount;
    
    @Schema(description = "进度百分比(0-100)")
    private Integer percentage;
    
    @Schema(description = "进度(0-100)，与percentage同义")
    private Integer progress;
    
    @Schema(description = "当前处理信息")
    private String message;
    
    @Schema(description = "当前错误信息")
    private String currentError;
    
    @Schema(description = "错误详情（最多50条）")
    private List<ErrorDetail> errorDetails;
    
    @Schema(description = "开始时间")
    private String startTime;
    
    @Schema(description = "结束时间")
    private String endTime;
    
    /**
     * 错误详情
     */
    @Data
    @Builder
    @Schema(description = "错误详情")
    public static class ErrorDetail {
        @Schema(description = "行号")
        private Integer row;
        
        @Schema(description = "订单ID")
        private String orderId;
        
        @Schema(description = "错误信息")
        private String error;
    }
}
