package com.musheng.config.currency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 货币实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_currency")
public class Currency extends BaseEntity {

    /**
     * 货币编码(USD/EUR/GBP/CAD)
     */
    private String currencyCode;

    /**
     * 货币名称
     */
    private String currencyName;

    /**
     * 货币符号($, €等)
     */
    private String currencySymbol;

    /**
     * 小数位数
     */
    private Integer decimalPlaces;

    /**
     * 状态(1启用, 0禁用)
     */
    private Integer status;

    /**
     * 货币对方向(DIRECT=XXX/CNY, REVERSE=CNY/XXX)
     * DIRECT: 外币对人民币（如 USD/CNY, EUR/CNY）
     * REVERSE: 人民币对外币（如 CNY/MOP, CNY/THB）
     */
    private String pairDirection;
}
