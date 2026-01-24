package com.musheng.business.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 首页仪表盘数据
 */
@Data
@Schema(description = "首页仪表盘数据")
public class DashboardData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 核心指标 ==========

    @Schema(description = "当前季度")
    private String currentQuarter;

    @Schema(description = "本季度收入（人民币）")
    private BigDecimal totalRevenueCny;

    @Schema(description = "本季度退款（人民币）- 按发货归属")
    private BigDecimal refundCny;

    @Schema(description = "本季度净收入（人民币）")
    private BigDecimal netIncomeCny;

    @Schema(description = "发货订单数")
    private Integer shippingOrderCount;

    // ========== 环比数据 ==========

    @Schema(description = "收入环比增长率")
    private BigDecimal revenueGrowthRate;

    @Schema(description = "退款环比增长率")
    private BigDecimal refundGrowthRate;

    @Schema(description = "净收入环比增长率")
    private BigDecimal netIncomeGrowthRate;

    // ========== 各站点数据（图表用） ==========

    @Schema(description = "各站点收入数据")
    private List<SiteRevenue> siteRevenues;

    // ========== 季度趋势（图表用） ==========

    @Schema(description = "季度趋势数据")
    private List<QuarterTrend> quarterTrends;

    /**
     * 站点收入
     */
    @Data
    public static class SiteRevenue implements Serializable {
        private String siteCode;
        private String siteName;
        private BigDecimal revenue;
        private BigDecimal refund;
        private BigDecimal netIncome;
    }

    /**
     * 季度趋势
     */
    @Data
    public static class QuarterTrend implements Serializable {
        private String quarter;
        private BigDecimal revenue;
        private BigDecimal refund;
        private BigDecimal netIncome;
    }
}
