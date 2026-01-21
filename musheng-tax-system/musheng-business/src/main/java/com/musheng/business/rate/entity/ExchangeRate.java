package com.musheng.business.rate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
    private LocalDate rateDate;

    /**
     * 货币编码
     */
    private String currencyCode;

    /**
     * 汇率中间价(对人民币)
     */
    private BigDecimal rate;

    /**
     * 是否工作日(1是/0否)
     */
    private Integer isWorkday;

    /**
     * 实际汇率日期(节假日顺延后)
     */
    private LocalDate actualRateDate;

    /**
     * 数据来源(PBOC-中国人民银行)
     */
    private String source;
}
