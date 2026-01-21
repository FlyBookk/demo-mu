package com.musheng.business.rate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 节假日实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_holiday")
public class Holiday extends BaseEntity {

    /**
     * 节假日日期
     */
    private LocalDate holidayDate;

    /**
     * 节假日名称
     */
    private String holidayName;

    /**
     * 类型: 1-周末, 2-法定节假日, 3-调休
     */
    private Integer holidayType;

    /**
     * 年份
     */
    private Integer year;
}
