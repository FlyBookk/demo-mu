package com.musheng.business.settlement.derivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 结算数据推导请求（按季度）
 *
 * <p>前端传入开始季度和结束季度（格式如 2025-Q3），
 * 后端自动计算对应的日期范围进行推导。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算数据推导请求（按季度）")
public class DerivationRequest {

    /** 开始季度，格式如 2025-Q3 */
    @NotBlank(message = "开始季度不能为空")
    @Schema(description = "开始季度，格式如 2025-Q3")
    private String startQuarter;

    /** 结束季度，格式如 2025-Q3 */
    @NotBlank(message = "结束季度不能为空")
    @Schema(description = "结束季度，格式如 2025-Q3")
    private String endQuarter;

    /** 各站点采购成本列表（人民币） */
    @Schema(description = "各站点采购成本列表（人民币）")
    private List<SiteCostInput> siteCosts;
}
