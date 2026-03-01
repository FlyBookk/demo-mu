package com.musheng.business.rate.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 汇率导出行
 */
@Data
public class ExchangeRateExportRow {

    @ExcelProperty("汇率日期")
    private LocalDate rateDate;

    @ExcelProperty("货币编码")
    private String currencyCode;

    @ExcelProperty("汇率")
    private BigDecimal rate;

    @ExcelProperty("数据来源")
    private String source;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
