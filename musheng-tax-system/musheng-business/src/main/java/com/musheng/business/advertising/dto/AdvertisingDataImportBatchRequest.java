package com.musheng.business.advertising.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 广告数据批量导入请求DTO
 * 包含多条广告数据记录
 *
 * @author 后端研发团队
 * @since 2026-01-20
 */
@Data
@Schema(description = "广告数据批量导入请求")
public class AdvertisingDataImportBatchRequest {

    @NotEmpty(message = "导入数据不能为空")
    @Valid
    @Schema(description = "广告数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AdvertisingDataImportRequest> data;

    @Schema(description = "导入批次ID（可选，系统自动生成）")
    private String importBatchId;
}
