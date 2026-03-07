package com.musheng.business.document.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 结算周期计算器
 *
 * <p>纯函数工具类，无状态，确定性输出。
 * 按7天为一个结算周期（周二到下周一）划分日期范围，
 * 并计算每个周期的结算日（周期结束后的下一个工作日）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class SettlementPeriodCalculator {

    private SettlementPeriodCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 结算周期数据类
     *
     * <p>包含周期起始日、结束日和结算日。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementPeriod {
        /** 周期起始日 */
        private LocalDate periodStart;
        /** 周期结束日 */
        private LocalDate periodEnd;
        /** 结算日（周期结束后的下一个工作日） */
        private LocalDate settlementDate;
    }

    /**
     * 计算结算周期列表
     *
     * <p>按7天为一个周期划分日期范围。第一个周期从 start 开始，到 start+6天结束。
     * 最后一个周期如果不足7天，按实际天数处理（periodEnd = end）。
     * 每个周期的结算日为周期结束日之后的下一个工作日。</p>
     *
     * @param start 起始日期，不能为 null
     * @param end 结束日期，不能为 null，不能早于 start
     * @return 结算周期列表
     * @throws IllegalArgumentException 如果参数为 null 或 start 晚于 end
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static List<SettlementPeriod> calculatePeriods(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new IllegalArgumentException("起始日期参数不能为 null");
        }
        if (end == null) {
            throw new IllegalArgumentException("结束日期参数不能为 null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "起始日期不能晚于结束日期，start=" + start + ", end=" + end);
        }

        List<SettlementPeriod> periods = new ArrayList<>();
        LocalDate periodStart = start;

        while (!periodStart.isAfter(end)) {
            // 周期结束日 = 起始日 + 6天，但不能超过 end
            LocalDate periodEnd = periodStart.plusDays(6);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }

            // 结算日 = 周期结束日之后的下一个工作日
            LocalDate settlementDate = calculateSettlementDate(periodEnd);

            periods.add(SettlementPeriod.builder()
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .settlementDate(settlementDate)
                    .build());

            // 下一个周期从当前周期结束日的下一天开始
            periodStart = periodEnd.plusDays(1);
        }

        return periods;
    }

    /**
     * 计算结算日
     *
     * <p>结算日为周期结束日之后的下一个工作日。</p>
     *
     * @param periodEnd 周期结束日，不能为 null
     * @return 结算日
     * @throws IllegalArgumentException 如果 periodEnd 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static LocalDate calculateSettlementDate(LocalDate periodEnd) {
        if (periodEnd == null) {
            throw new IllegalArgumentException("周期结束日参数不能为 null");
        }
        return WorkingDayCalculator.nextWorkingDay(periodEnd);
    }
}
