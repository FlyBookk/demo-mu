package com.musheng.config.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 智能匹配请求
 */
@Data
@Schema(description = "智能匹配请求")
public class AutoMatchRequest {

    @NotBlank(message = "数据类型不能为空")
    @Schema(description = "数据类型", example = "SALES", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataType;

    @Schema(description = "数据源类型：ORIGINAL/ERP", example = "ORIGINAL")
    private String sourceType;

    @NotEmpty(message = "源字段列表不能为空")
    @Schema(description = "源字段列表", example = "[\"order_id\", \"sku\", \"amount\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sourceFields;

    @Schema(description = "站点编码（用于别名匹配）", example = "DE")
    private String siteCode;
}
