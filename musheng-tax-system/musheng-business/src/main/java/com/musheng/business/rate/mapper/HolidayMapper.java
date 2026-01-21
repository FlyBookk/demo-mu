package com.musheng.business.rate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.rate.entity.Holiday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * Holiday Mapper
 */
@Mapper
public interface HolidayMapper extends BaseMapper<Holiday> {

    /**
     * Check if date is a holiday
     */
    @Select("SELECT COUNT(*) > 0 FROM t_holiday WHERE holiday_date = #{date}")
    boolean isHoliday(@Param("date") LocalDate date);

    /**
     * Get next workday after given date
     */
    @Select("SELECT MIN(d.date_val) FROM (" +
            "SELECT DATE_ADD(#{date}, INTERVAL t.n DAY) AS date_val " +
            "FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 " +
            "UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t " +
            ") d " +
            "WHERE d.date_val > #{date} AND d.date_val NOT IN (SELECT holiday_date FROM t_holiday)")
    LocalDate getNextWorkday(@Param("date") LocalDate date);
}
