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

    // ========== 配送匹配数据 ==========

    @Schema(description = "配送数据匹配到的订单笔数")
    private Integer shippingMatchCount;

    // ========== 退款数据（单一配送日期口径） ==========

    @Schema(description = "退款金额（原币）- 配送日期口径")
    private BigDecimal refundAmount;

    @Schema(description = "退款金额（人民币）- 配送日期口径")
    private BigDecimal refundAmountCny;

    @Schema(description = "退款笔数")
    private Integer refundCount;

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

    // ========== 佣金服务费合计（方式一+方式二，3项：sellingFees+fbaFees+otherTransactionFees） ==========

    @Schema(description = "佣金服务费合计（原币）= 方式一(3项) + 方式二(3项)")
    private BigDecimal totalCommissionFee;

    @Schema(description = "佣金服务费合计（人民币）")
    private BigDecimal totalCommissionFeeCny;

    // ========== 其他费合计（方式一other + 方式二other） ==========

    @Schema(description = "其他费合计（原币）= 方式一(other) + 方式二(other)")
    private BigDecimal totalOtherFee;

    @Schema(description = "其他费合计（人民币）")
    private BigDecimal totalOtherFeeCny;

    // ========== 广告费 ==========

    @Schema(description = "广告费（原币）")
    private BigDecimal advertisingCost;

    @Schema(description = "广告费（人民币）")
    private BigDecimal advertisingCostCny;

    // ========== 平台支出与采购成本计算（按图片公式） ==========

    @Schema(description = "平台支出合计（人民币）= 消费税 + 佣金服务费 + 其他")
    private BigDecimal platformExpenses;

    @Schema(description = "平台支出合计（人民币）")
    private BigDecimal platformExpensesCny;

    @Schema(description = "4%利润（人民币）= 收入净额 × 4%")
    private BigDecimal profit4Percent;

    @Schema(description = "4%利润（人民币）")
    private BigDecimal profit4PercentCny;

    @Schema(description = "采购成本（人民币）= 收入净额 − 平台支出合计 − 4%利润")
    private BigDecimal procurementCost;

    @Schema(description = "采购成本（人民币）")
    private BigDecimal procurementCostCny;
}
