package com.musheng.business.rate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率及实际取值日期
 * 用于返回从汇率表查到的汇率及实际使用的日期（交易日期无汇率时顺延到下一个有汇率的日期）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateWithDateDTO {

    private BigDecimal rate;
    private LocalDate actualDate;
}
