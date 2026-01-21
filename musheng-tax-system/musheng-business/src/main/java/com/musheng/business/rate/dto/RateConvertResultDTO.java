package com.musheng.business.rate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 汇率转换结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "汇率转换结果")
public class RateConvertResultDTO {

    @Schema(description = "原始金额", example = "100")
    private BigDecimal originalAmount;

    @Schema(description = "转换后金额(CNY)", example = "724.50")
    private BigDecimal convertedAmount;

    @Schema(description = "货币编码", example = "USD")
    private String currencyCode;

    @Schema(description = "使用的汇率", example = "7.245")
    private BigDecimal rate;

    @Schema(description = "汇率日期(YYYY-MM-DD)", example = "2025-01-15")
    private String rateDate;
}
