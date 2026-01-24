package com.musheng.business.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 报税汇总数据 - 税务申报用
 * V2版本：按新的费用计算逻辑
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

    // 维度一：按发货订单归属（主要报税维度）
    @Schema(description = "退款金额-按发货归属（原币）")
    private BigDecimal refundByShipment;

    @Schema(description = "退款金额-按发货归属（人民币）")
    private BigDecimal refundByShipmentCny;

    @Schema(description = "退款笔数-按发货归属")
    private Integer refundCountByShipment;

    // 维度二：按结算时间统计（辅助参考）
    @Schema(description = "退款金额-按结算时间（原币）")
    private BigDecimal refundBySettlement;

    @Schema(description = "退款金额-按结算时间（人民币）")
    private BigDecimal refundBySettlementCny;

    @Schema(description = "退款笔数-按结算时间")
    private Integer refundCountBySettlement;

    // ========== 消费税（平台代扣税） ==========

    @Schema(description = "消费税/平台代扣税（原币）")
    private BigDecimal consumptionTax;

    @Schema(description = "消费税/平台代扣税（人民币）")
    private BigDecimal consumptionTaxCny;

    // ========== 佣金/服务费（拆分明细） ==========

    @Schema(description = "销售费用（原币）")
    private BigDecimal sellingFees;

    @Schema(description = "销售费用（人民币）")
    private BigDecimal sellingFeesCny;

    @Schema(description = "FBA费用（原币）")
    private BigDecimal fbaFees;

    @Schema(description = "FBA费用（人民币）")
    private BigDecimal fbaFeesCny;

    @Schema(description = "其他交易费（原币）")
    private BigDecimal otherTransactionFees;

    @Schema(description = "其他交易费（人民币）")
    private BigDecimal otherTransactionFeesCny;

    @Schema(description = "其他（原币）")
    private BigDecimal otherAmount;

    @Schema(description = "其他（人民币）")
    private BigDecimal otherAmountCny;

    @Schema(description = "佣金/服务费合计（原币）= sellingFees + fbaFees + otherTransactionFees + otherAmount")
    private BigDecimal totalServiceFee;

    @Schema(description = "佣金/服务费合计（人民币）")
    private BigDecimal totalServiceFeeCny;

    // ========== 其他费（非收入/退款类型） ==========

    @Schema(description = "其他费（原币）- 非收入/退款类型的total汇总")
    private BigDecimal miscFees;

    @Schema(description = "其他费（人民币）")
    private BigDecimal miscFeesCny;

    @Schema(description = "其他费笔数")
    private Integer miscFeesCount;

    // ========== 广告费 ==========

    @Schema(description = "广告费（原币）")
    private BigDecimal advertisingCost;

    @Schema(description = "广告费（人民币）")
    private BigDecimal advertisingCostCny;

    // ========== 成本汇总 ==========

    @Schema(description = "总成本（人民币）= 佣金/服务费 + 其他费 + 广告费")
    private BigDecimal totalCost;
}
