package com.musheng.business.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 费用分类统计
 */
@Data
@Schema(description = "费用分类统计")
public class FeeBreakdown implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "站点编码", example = "US")
    private String siteCode;

    @Schema(description = "年季度", example = "2024-Q1")
    private String yearQuarter;

    @Schema(description = "费用类型（原始值）", example = "FBA fees")
    private String feeType;

    @Schema(description = "费用分类", example = "fee")
    private String feeCategory;

    @Schema(description = "金额（原币）")
    private BigDecimal amount;

    @Schema(description = "金额（人民币）")
    private BigDecimal amountCny;

    @Schema(description = "交易笔数")
    private Integer transactionCount;
}
