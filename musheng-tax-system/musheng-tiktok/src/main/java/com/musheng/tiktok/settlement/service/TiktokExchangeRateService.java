package com.musheng.tiktok.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/**
 * TK汇率查询服务
 * 规则：取销售发生月的下一个季度首个工作日汇率
 *
 * @author wanhua
 * 20:40 2026年05月14日
 */
@Service
@Slf4j
public class TiktokExchangeRateService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取指定月份对应的报税汇率
     * 规则：取该月所属季度的下一个季度首个工作日的USD汇率
     *
     * @param month 销售发生月份（如 2025-07）
     * @return 汇率值，null表示未找到
     */
    public BigDecimal getReportExchangeRate(String month) {
        LocalDate date = LocalDate.parse(month + "-01");
        // 计算下一个季度的首日
        LocalDate nextQuarterStart = getNextQuarterStart(date);

        // 查询该日期起的第一个工作日汇率
        String sql = "SELECT rate FROM t_exchange_rate " +
                "WHERE currency_code = 'USD' AND rate_date >= ? AND is_workday = 1 " +
                "ORDER BY rate_date ASC LIMIT 1";

        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, nextQuarterStart);
        } catch (Exception e) {
            log.warn("未找到汇率: month={}, nextQuarterStart={}", month, nextQuarterStart);
            return null;
        }
    }

    /**
     * 计算给定日期所属季度的下一个季度首日
     * Q1(1-3月) → 下一季度首日 = 4月1日
     * Q2(4-6月) → 下一季度首日 = 7月1日
     * Q3(7-9月) → 下一季度首日 = 10月1日
     * Q4(10-12月) → 下一季度首日 = 次年1月1日
     */
    private LocalDate getNextQuarterStart(LocalDate date) {
        int month = date.getMonthValue();
        int year = date.getYear();
        if (month <= 3) {
            return LocalDate.of(year, 4, 1);
        } else if (month <= 6) {
            return LocalDate.of(year, 7, 1);
        } else if (month <= 9) {
            return LocalDate.of(year, 10, 1);
        } else {
            return LocalDate.of(year + 1, 1, 1);
        }
    }
}
