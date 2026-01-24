package com.musheng.business.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 报税汇总数据 - 税务申报用
 */
@Data
@Schema(description = "报税汇总数据")
public class TaxReportSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础信息 ==========

    @Schema(description = "站点编码", example = "US")
    private String siteCode;

    @Schema(description = "站点名称", example = "美国站")
    private String siteName;

    @Schema(description = "年季度", example = "2024-Q1")
    private String yearQuarter;

    @Schema(description = "原币种", example = "USD")
    private String currencyCode;

    // ========== 收入数据（按发货单计算） ==========

    @Schema(description = "收入总额（原币）")
    private BigDecimal totalRevenue;

    @Schema(description = "收入总额（人民币）")
    private BigDecimal totalRevenueCny;

    @Schema(description = "发货订单数")
    private Integer shippingOrderCount;

    // ========== 退款数据（双维度） ==========

    // 维度一：按结算时间统计
    @Schema(description = "退款金额-按结算时间（原币）")
    private BigDecimal refundBySettlement;

    @Schema(description = "退款金额-按结算时间（人民币）")
    private BigDecimal refundBySettlementCny;

    @Schema(description = "退款笔数-按结算时间")
    private Integer refundCountBySettlement;

    // 维度二：按发货订单归属
    @Schema(description = "退款金额-按发货归属（原币）")
    private BigDecimal refundByShipment;

    @Schema(description = "退款金额-按发货归属（人民币）")
    private BigDecimal refundByShipmentCny;

    @Schema(description = "退款笔数-按发货归属")
    private Integer refundCountByShipment;

    // ========== 净收入（两个维度） ==========

    @Schema(description = "净收入-按结算维度（人民币）")
    private BigDecimal netIncomeBySettlement;

    @Schema(description = "净收入-按发货维度（人民币）")
    private BigDecimal netIncomeByShipment;

    // ========== 费用汇总 ==========

    @Schema(description = "亚马逊服务费合计（原币）")
    private BigDecimal totalServiceFee;

    @Schema(description = "亚马逊服务费合计（人民币）")
    private BigDecimal totalServiceFeeCny;

    @Schema(description = "广告费（原币）")
    private BigDecimal advertisingCost;

    @Schema(description = "广告费（人民币）")
    private BigDecimal advertisingCostCny;

    // ========== 成本计算 ==========

    @Schema(description = "总成本（人民币）= 服务费 + 广告费 + 采购金额")
    private BigDecimal totalCost;

    @Schema(description = "采购金额（人民币）= 净收入 × 96% - 服务费 - 广告费")
    private BigDecimal purchaseAmount;
}
