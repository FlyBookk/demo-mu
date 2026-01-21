package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 销售数据上传结果
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Schema(description = "销售数据上传结果")
public class SalesUploadResult {
    
    @Schema(description = "文件唯一标识", example = "f8c3d2a1-5e4b-4c3a-9d8f-1a2b3c4d5e6f")
    private String fileId;
    
    @Schema(description = "原始文件名", example = "慕声美国2025年第三季度交易联合报告.csv")
    private String fileName;
    
    @Schema(description = "文件大小(bytes)", example = "1048576")
    private Long fileSize;
    
    @Schema(description = "服务器存储路径")
    private String filePath;
    
    @Schema(description = "总行数（不含表头）", example = "5000")
    private Integer totalRows;
    
    @Schema(description = "表头所在行号（从1开始）", example = "8")
    private Integer headerRow;
    
    @Schema(description = "解析出的源字段列表")
    private List<String> sourceFields;
    
    @Schema(description = "自动识别的站点编码", example = "US")
    private String detectedSiteCode;
    
    @Schema(description = "文件中包含的所有站点（多站点文件）")
    private List<String> detectedSiteCodes;
    
    @Schema(description = "文件编码", example = "UTF-8")
    private String encoding;
    
    @Schema(description = "前5行样例数据")
    private List<Map<String, Object>> sampleData;
}
