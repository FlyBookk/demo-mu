package com.musheng.business.sales.dto;

import com.musheng.common.enums.SalesSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 销售数据预览请求
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Schema(description = "销售数据预览请求")
public class SalesPreviewRequest {
    
    @Schema(description = "文件ID（上传返回）", required = true)
    @NotBlank(message = "文件ID不能为空")
    private String fileId;
    
    @Schema(description = "数据源类型", required = true)
    @NotNull(message = "数据源类型不能为空")
    private SalesSourceType sourceType;
    
    @Schema(description = "站点编码", required = true, example = "US")
    @NotBlank(message = "站点编码不能为空")
    private String siteCode;
    
    @Schema(description = "字段映射模板ID", required = true)
    @NotNull(message = "模板ID不能为空")
    private Long templateId;
    
    @Schema(description = "数据所属季度", example = "2025-Q3")
    private String quarter;
}
