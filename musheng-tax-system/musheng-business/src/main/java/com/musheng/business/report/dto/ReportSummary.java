package com.musheng.business.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 汇总报表DTO
 */
@Data
@Schema(description = "汇总报表")
public class ReportSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 站点编码
     */
    @Schema(description = "站点编码", example = "DE")
    private String siteCode;

    /**
     * 站点名称
     */
    @Schema(description = "站点名称", example = "德国站")
    private String siteName;

    /**
     * 年季度
     */
    @Schema(description = "年季度", example = "2024-Q1")
    private String yearQuarter;

    /**
     * 销售总额(原币)
     */
    @Schema(description = "销售总额(原币)")
    private BigDecimal totalSalesAmount;

    /**
     * 销售总额(人民币)
     */
    @Schema(description = "销售总额(人民币)")
    private BigDecimal totalSalesAmountEur;

    /**
     * 物流成本(原币)
     */
    @Schema(description = "物流成本(原币)")
    private BigDecimal totalShippingCost;

    /**
     * 物流成本(人民币)
     */
    @Schema(description = "物流成本(人民币)")
    private BigDecimal totalShippingCostEur;

    /**
     * 广告费(原币)
     */
    @Schema(description = "广告费(原币)")
    private BigDecimal totalAdvertisingCost;

    /**
     * 广告费(人民币)
     */
    @Schema(description = "广告费(人民币)")
    private BigDecimal totalAdvertisingCostEur;

    /**
     * 净额(人民币)
     */
    @Schema(description = "净额(人民币)")
    private BigDecimal netAmountEur;

    /**
     * 税额(人民币)
     */
    @Schema(description = "税额(人民币)")
    private BigDecimal vatAmountEur;

    /**
     * 交易数量
     */
    @Schema(description = "交易数量")
    private Integer transactionCount;

    /**
     * 货币编码
     */
    @Schema(description = "货币编码", example = "EUR")
    private String currencyCode;
}
