package com.musheng.business.settlement.derivation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 结算周期 VO
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算周期")
public class SettlementPeriodVO {

    /** 周期起始日（周二） */
    @Schema(description = "周期起始日（周二）")
    private LocalDate periodStart;

    /** 周期结束日（下周一） */
    @Schema(description = "周期结束日（下周一）")
    private LocalDate periodEnd;

    /** 周期标签，如"2026-02-24 ~ 2026-03-02" */
    @Schema(description = "周期标签，如 2026-02-24 ~ 2026-03-02")
    private String label;
}
