package com.musheng.business.settlement.derivation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 推导结果 VO（按季度）
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推导结果（按季度）")
public class DerivationResultVO {

    /** 开始季度 */
    @Schema(description = "开始季度，如 2025-Q3")
    private String startQuarter;

    /** 结束季度 */
    @Schema(description = "结束季度，如 2025-Q3")
    private String endQuarter;

    /** 实际查询的起始日期（季度对应的第一天） */
    @Schema(description = "实际查询的起始日期")
    private LocalDate periodStart;

    /** 实际查询的结束日期（季度对应的最后一天） */
    @Schema(description = "实际查询的结束日期")
    private LocalDate periodEnd;

    /** 各站点推导结果列表 */
    @Schema(description = "各站点推导结果列表")
    private List<SiteDerivationResult> siteResults;

    /** 各货币的周期平均汇率（货币代码 → 平均汇率） */
    @Schema(description = "各货币的周期平均汇率（货币代码 → 平均汇率）")
    private Map<String, BigDecimal> averageRates;
}
