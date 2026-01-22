package com.musheng.business.rate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.annotation.FieldMapping;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率实体
 * 映射到 t_exchange_rate 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_exchange_rate")
public class ExchangeRate extends BaseEntity {

    /**
     * 汇率日期
     */
    @FieldMapping(label = "汇率日期", description = "汇率生效日期", required = true, order = 1)
    private LocalDate rateDate;

    /**
     * 货币编码
     */
    @FieldMapping(label = "货币编码", description = "货币代码", required = true, maxLength = 10, order = 2)
    private String currencyCode;

    /**
     * 汇率中间价(对人民币)
     */
    @FieldMapping(label = "汇率", description = "汇率中间价(对人民币)", required = true, precision = 6, order = 3)
    private BigDecimal rate;

    /**
     * 是否工作日(1是/0否)
     */
    @FieldMapping(label = "是否工作日", ignore = true)
    private Integer isWorkday;

    /**
     * 实际汇率日期(节假日顺延后)
     */
    @FieldMapping(label = "实际汇率日期", ignore = true)
    private LocalDate actualRateDate;

    /**
     * 数据来源(PBOC-中国人民银行)
     */
    @FieldMapping(label = "数据来源", description = "PBOC-中国人民银行", maxLength = 50, order = 4)
    private String source;
}
