package com.musheng.business.rate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率请求DTO（新增/修改）
 */
@Data
public class RateRequest {

    /**
     * 汇率日期
     */
    @NotNull(message = "汇率日期不能为空")
    private LocalDate rateDate;

    /**
     * 货币编码
     */
    @NotBlank(message = "货币编码不能为空")
    private String currencyCode;

    /**
     * 汇率中间价(对人民币)
     */
    @NotNull(message = "汇率不能为空")
    private BigDecimal rate;

    /**
     * 是否工作日(1是/0否)
     */
    private Integer isWorkday;

    /**
     * 数据来源
     */
    private String source;
}
