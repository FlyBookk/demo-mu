package com.musheng.business.document.utils;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.time.api.DateTimes;
import net.jqwik.time.api.Dates;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkingDayCalculator 属性测试
 *
 * <p>使用 jqwik 框架验证工作日计算器的通用正确性属性。
 * 每个属性测试运行100次迭代，覆盖2025-2026年日期范围。</p>
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.4</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class WorkingDayCalculatorProperties {

    /**
     * 提供2025-2026年范围内的随机日期
     *
     * @return 日期 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate> dateIn2025To2026() {
        return Dates.dates()
                .between(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31));
    }

    /**
     * 提供2025-2026年范围内的周六日期
     *
     * @return 周六日期 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate> saturdayDates() {
        return Dates.dates()
                .between(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31))
                .onlyDaysOfWeek(DayOfWeek.SATURDAY);
    }

    /**
     * 提供2025-2026年范围内的周日日期
     *
     * @return 周日日期 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate> sundayDates() {
        return Dates.dates()
                .between(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31))
                .onlyDaysOfWeek(DayOfWeek.SUNDAY);
    }

    /**
     * 提供节假日列表中的随机日期
     *
     * @return 节假日日期 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate> holidayDates() {
        java.util.List<LocalDate> allHolidays = new java.util.ArrayList<>();
        allHolidays.addAll(HolidayConstants.HOLIDAYS_2025);
        allHolidays.addAll(HolidayConstants.HOLIDAYS_2026);
        return Arbitraries.of(allHolidays);
    }

    // ==================== 属性 14：工作日判定正确性 ====================

    // Feature: fba-document-generation, Property 14: 工作日判定正确性
    // 周六 isWorkingDay 返回 false
    /**
     * 属性14-1：周六 isWorkingDay 应返回 false
     *
     * <p><b>Validates: Requirements 7.1</b></p>
     *
     * @param saturday 随机生成的周六日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void saturdayShouldNotBeWorkingDay(@ForAll("saturdayDates") LocalDate saturday) {
        assertFalse(WorkingDayCalculator.isWorkingDay(saturday),
                "周六 " + saturday + " 不应该是工作日");
    }

    // Feature: fba-document-generation, Property 14: 工作日判定正确性
    // 周日 isWorkingDay 返回 false
    /**
     * 属性14-2：周日 isWorkingDay 应返回 false
     *
     * <p><b>Validates: Requirements 7.1</b></p>
     *
     * @param sunday 随机生成的周日日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void sundayShouldNotBeWorkingDay(@ForAll("sundayDates") LocalDate sunday) {
        assertFalse(WorkingDayCalculator.isWorkingDay(sunday),
                "周日 " + sunday + " 不应该是工作日");
    }

    // Feature: fba-document-generation, Property 14: 工作日判定正确性
    // 节假日 isWorkingDay 返回 false
    /**
     * 属性14-3：节假日 isWorkingDay 应返回 false
     *
     * <p><b>Validates: Requirements 7.1</b></p>
     *
     * @param holiday 随机选取的节假日日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void holidayShouldNotBeWorkingDay(@ForAll("holidayDates") LocalDate holiday) {
        assertFalse(WorkingDayCalculator.isWorkingDay(holiday),
                "节假日 " + holiday + " 不应该是工作日");
    }

    // Feature: fba-document-generation, Property 14: 工作日判定正确性
    // nextWorkingDay 返回值 > 输入日期且本身是工作日
    /**
     * 属性14-4：nextWorkingDay 返回值大于输入日期且本身是工作日
     *
     * <p><b>Validates: Requirements 7.2, 7.4</b></p>
     *
     * @param date 随机生成的日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void nextWorkingDayShouldBeAfterInputAndBeWorkingDay(
            @ForAll("dateIn2025To2026") LocalDate date) {
        LocalDate result = WorkingDayCalculator.nextWorkingDay(date);

        // 返回值必须大于输入日期
        assertTrue(result.isAfter(date),
                "nextWorkingDay(" + date + ") = " + result + " 应大于输入日期");

        // 返回值本身必须是工作日
        assertTrue(WorkingDayCalculator.isWorkingDay(result),
                "nextWorkingDay(" + date + ") = " + result + " 本身应该是工作日");
    }

    // Feature: fba-document-generation, Property 14: 工作日判定正确性
    // nextWorkingDay 返回值与输入日期之间无其他工作日（即返回的是紧邻的下一个工作日）
    /**
     * 属性14-5：nextWorkingDay 返回的是紧邻的下一个工作日，中间无其他工作日
     *
     * <p><b>Validates: Requirements 7.2, 7.4</b></p>
     *
     * @param date 随机生成的日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void nextWorkingDayShouldBeTheClosestWorkingDay(
            @ForAll("dateIn2025To2026") LocalDate date) {
        LocalDate result = WorkingDayCalculator.nextWorkingDay(date);

        // 检查输入日期与返回日期之间不存在其他工作日
        LocalDate check = date.plusDays(1);
        while (check.isBefore(result)) {
            assertFalse(WorkingDayCalculator.isWorkingDay(check),
                    "在 " + date + " 和 nextWorkingDay 结果 " + result
                            + " 之间发现了工作日 " + check + "，说明返回的不是紧邻的下一个工作日");
            check = check.plusDays(1);
        }
    }
}
