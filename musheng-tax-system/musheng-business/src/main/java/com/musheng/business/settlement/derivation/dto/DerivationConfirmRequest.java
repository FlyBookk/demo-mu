package com.musheng.business.settlement.derivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推导结果确认写入请求（按季度）
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推导结果确认写入请求（按季度）")
public class DerivationConfirmRequest {

    /** 开始季度，格式如 2025-Q3 */
    @NotBlank(message = "开始季度不能为空")
    @Schema(description = "开始季度，格式如 2025-Q3")
    private String startQuarter;

    /** 结束季度，格式如 2025-Q3 */
    @NotBlank(message = "结束季度不能为空")
    @Schema(description = "结束季度，格式如 2025-Q3")
    private String endQuarter;

    /** 是否覆盖已有数据 */
    @Schema(description = "是否覆盖已有数据")
    private boolean overwrite;

    /** 各站点确认数据列表 */
    @Schema(description = "各站点确认数据列表")
    private List<SiteConfirmData> siteDataList;
}
