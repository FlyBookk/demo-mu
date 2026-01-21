package com.musheng.business.sales.dto;

import com.musheng.common.enums.SalesSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 销售数据上传请求
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Schema(description = "销售数据上传请求")
public class SalesUploadRequest {
    
    @Schema(description = "上传文件", required = true)
    @NotNull(message = "文件不能为空")
    private MultipartFile file;
    
    @Schema(description = "数据源类型", required = true, example = "ORIGINAL")
    @NotNull(message = "数据源类型不能为空")
    private SalesSourceType sourceType;
    
    @Schema(description = "站点编码（ERP数据需要预选，ORIGINAL自动识别）", example = "US")
    private String siteCode;
}
