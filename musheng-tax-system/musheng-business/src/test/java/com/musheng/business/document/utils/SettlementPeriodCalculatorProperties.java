package com.musheng.business.document.utils;

import com.musheng.business.document.utils.SettlementPeriodCalculator.SettlementPeriod;
import net.jqwik.api.*;
import net.jqwik.time.api.Dates;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementPeriodCalculator 属性测试
 *
 * <p>使用 jqwik 框架验证结算周期计算器的通用正确性属性。
 * 每个属性测试运行100次迭代，覆盖2020-2030年日期范围。</p>
 *
 * <p><b>Validates: Requirements 4.1</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SettlementPeriodCalculatorProperties {

    /**
     * 提供有效的日期对（start <= end），范围2020-01-01到2030-12-31
     *
     * @return 日期对 Arbitrary [start, end]
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate[]> validDatePairs() {
        return Dates.dates()
                .between(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31))
                .tuple2()
                .map(t -> {
                    LocalDate d1 = t.get1();
                    LocalDate d2 = t.get2();
                    // 确保 start <= end
                    if (d1.isAfter(d2)) {
                        return new LocalDate[]{d2, d1};
                    }
                    return new LocalDate[]{d1, d2};
                });
    }

    // ==================== 属性 9：结算周期划分正确性 ====================

    // Feature: fba-document-generation, Property 9: 结算周期划分正确性
    // 每个周期 ≤ 7天（periodEnd - periodStart + 1 ≤ 7）
    /**
     * 属性9-1：每个周期天数不超过7天
     *
     * <p>每个结算周期的天数（periodEnd - periodStart + 1）应 ≤ 7。</p>
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     *
     * @param datePair 随机生成的有效日期对
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void eachPeriodShouldBeAtMost7Days(@ForAll("validDatePairs") LocalDate[] datePair) {
        LocalDate start = datePair[0];
        LocalDate end = datePair[1];

        List<SettlementPeriod> periods = SettlementPeriodCalculator.calculatePeriods(start, end);

        for (int i = 0; i < periods.size(); i++) {
            SettlementPeriod period = periods.get(i);
            long days = ChronoUnit.DAYS.between(period.getPeriodStart(), period.getPeriodEnd()) + 1;
            assertTrue(days >= 1 && days <= 7,
                    "第 " + (i + 1) + " 个周期天数为 " + days + "（"
                            + period.getPeriodStart() + " ~ " + period.getPeriodEnd()
                            + "），应在1到7天之间");
        }
    }

    // Feature: fba-document-generation, Property 9: 结算周期划分正确性
    // 所有周期无重叠：后一个周期的 periodStart = 前一个周期的 periodEnd + 1天
    /**
     * 属性9-2：所有周期无重叠且连续
     *
     * <p>后一个周期的 periodStart 应等于前一个周期的 periodEnd + 1天。</p>
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     *
     * @param datePair 随机生成的有效日期对
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void periodsShouldBeContiguousWithNoOverlap(@ForAll("validDatePairs") LocalDate[] datePair) {
        LocalDate start = datePair[0];
        LocalDate end = datePair[1];

        List<SettlementPeriod> periods = SettlementPeriodCalculator.calculatePeriods(start, end);

        for (int i = 1; i < periods.size(); i++) {
            SettlementPeriod prev = periods.get(i - 1);
            SettlementPeriod curr = periods.get(i);
            LocalDate expectedStart = prev.getPeriodEnd().plusDays(1);
            assertEquals(expectedStart, curr.getPeriodStart(),
                    "第 " + (i + 1) + " 个周期的 periodStart 应为前一个周期 periodEnd + 1天。"
                            + " 期望 " + expectedStart + "，实际 " + curr.getPeriodStart());
        }
    }

    // Feature: fba-document-generation, Property 9: 结算周期划分正确性
    // 完整覆盖日期范围：第一个周期的 periodStart = start，最后一个周期的 periodEnd = end
    /**
     * 属性9-3：周期完整覆盖日期范围
     *
     * <p>第一个周期的 periodStart 应等于 start，最后一个周期的 periodEnd 应等于 end。</p>
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     *
     * @param datePair 随机生成的有效日期对
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void periodsShouldFullyCoverDateRange(@ForAll("validDatePairs") LocalDate[] datePair) {
        LocalDate start = datePair[0];
        LocalDate end = datePair[1];

        List<SettlementPeriod> periods = SettlementPeriodCalculator.calculatePeriods(start, end);

        assertFalse(periods.isEmpty(),
                "日期范围 " + start + " ~ " + end + " 应至少产生一个周期");

        assertEquals(start, periods.get(0).getPeriodStart(),
                "第一个周期的 periodStart 应等于起始日期 " + start);

        assertEquals(end, periods.get(periods.size() - 1).getPeriodEnd(),
                "最后一个周期的 periodEnd 应等于结束日期 " + end);
    }

    // Feature: fba-document-generation, Property 9: 结算周期划分正确性
    // 结算日为周期结束日之后的下一个工作日：settlementDate > periodEnd 且 isWorkingDay(settlementDate) 为 true
    /**
     * 属性9-4：结算日为周期结束日之后的下一个工作日
     *
     * <p>settlementDate 应大于 periodEnd，且 isWorkingDay(settlementDate) 为 true。</p>
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     *
     * @param datePair 随机生成的有效日期对
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void settlementDateShouldBeNextWorkingDayAfterPeriodEnd(
            @ForAll("validDatePairs") LocalDate[] datePair) {
        LocalDate start = datePair[0];
        LocalDate end = datePair[1];

        List<SettlementPeriod> periods = SettlementPeriodCalculator.calculatePeriods(start, end);

        for (int i = 0; i < periods.size(); i++) {
            SettlementPeriod period = periods.get(i);

            // 结算日必须大于周期结束日
            assertTrue(period.getSettlementDate().isAfter(period.getPeriodEnd()),
                    "第 " + (i + 1) + " 个周期的结算日 " + period.getSettlementDate()
                            + " 应大于周期结束日 " + period.getPeriodEnd());

            // 结算日本身必须是工作日
            assertTrue(WorkingDayCalculator.isWorkingDay(period.getSettlementDate()),
                    "第 " + (i + 1) + " 个周期的结算日 " + period.getSettlementDate()
                            + " 应为工作日");
        }
    }

    // Feature: fba-document-generation, Property 9: 结算周期划分正确性
    // 结算日与周期结束日之间无其他工作日（即 settlementDate 是 periodEnd 之后的第一个工作日）
    /**
     * 属性9-5：结算日是周期结束日之后的第一个工作日，中间无其他工作日
     *
     * <p>periodEnd 与 settlementDate 之间不应存在其他工作日。</p>
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     *
     * @param datePair 随机生成的有效日期对
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void settlementDateShouldBeTheClosestWorkingDayAfterPeriodEnd(
            @ForAll("validDatePairs") LocalDate[] datePair) {
        LocalDate start = datePair[0];
        LocalDate end = datePair[1];

        List<SettlementPeriod> periods = SettlementPeriodCalculator.calculatePeriods(start, end);

        for (int i = 0; i < periods.size(); i++) {
            SettlementPeriod period = periods.get(i);

            // 检查 periodEnd 与 settlementDate 之间不存在其他工作日
            LocalDate check = period.getPeriodEnd().plusDays(1);
            while (check.isBefore(period.getSettlementDate())) {
                assertFalse(WorkingDayCalculator.isWorkingDay(check),
                        "第 " + (i + 1) + " 个周期的 periodEnd " + period.getPeriodEnd()
                                + " 与结算日 " + period.getSettlementDate()
                                + " 之间发现了工作日 " + check
                                + "，说明结算日不是紧邻的下一个工作日");
                check = check.plusDays(1);
            }
        }
    }
}
