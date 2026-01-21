package com.musheng.business.rate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 汇率转换请求
 */
@Data
@Schema(description = "汇率转换请求")
public class RateConvertRequest {

    @Schema(description = "原始金额", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负数")
    private BigDecimal amount;

    @Schema(description = "货币编码", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "货币编码不能为空")
    private String currencyCode;

    @Schema(description = "汇率日期(YYYY-MM-DD)，不传则使用最新汇率", example = "2025-01-15")
    private String rateDate;
}
