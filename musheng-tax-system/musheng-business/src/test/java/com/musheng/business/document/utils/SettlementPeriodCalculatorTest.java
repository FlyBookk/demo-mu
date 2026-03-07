package com.musheng.business.document.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementPeriodCalculator 单元测试
 *
 * <p>覆盖结算周期计算和结算日计算两个核心方法。
 * 测试场景包括：完整7天周期、多周期、不足7天周期、单天周期、
 * 结算日遇节假日顺延、null参数、start > end 异常。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("SettlementPeriodCalculator 结算周期计算器测试")
class SettlementPeriodCalculatorTest {

    // ==================== calculatePeriods 测试 ====================

    @Nested
    @DisplayName("calculatePeriods - 计算结算周期列表")
    class CalculatePeriodsTest {

        @Test
        @DisplayName("完整7天周期（2025-06-03 周二到 2025-06-09 周一）应产生1个周期")
        void testCalculatePeriods_SingleFullPeriod_ShouldReturnOnePeriod() {
            // Given
            LocalDate start = LocalDate.of(2025, 6, 3); // 周二
            LocalDate end = LocalDate.of(2025, 6, 9);   // 周一

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(start, end);

            // Then
            assertEquals(1, periods.size());
            SettlementPeriodCalculator.SettlementPeriod period = periods.get(0);
            assertEquals(start, period.getPeriodStart());
            assertEquals(end, period.getPeriodEnd());
            // 结算日 = nextWorkingDay(2025-06-09 周一) = 2025-06-10 周二
            assertEquals(LocalDate.of(2025, 6, 10), period.getSettlementDate());
        }

        @Test
        @DisplayName("多个完整周期（2025-06-03 到 2025-06-23）应产生3个周期")
        void testCalculatePeriods_MultipleFullPeriods_ShouldReturnThreePeriods() {
            // Given
            LocalDate start = LocalDate.of(2025, 6, 3);  // 周二
            LocalDate end = LocalDate.of(2025, 6, 23);    // 周一

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(start, end);

            // Then
            assertEquals(3, periods.size());

            // 第一个周期：06-03 到 06-09
            assertEquals(LocalDate.of(2025, 6, 3), periods.get(0).getPeriodStart());
            assertEquals(LocalDate.of(2025, 6, 9), periods.get(0).getPeriodEnd());
            assertEquals(LocalDate.of(2025, 6, 10), periods.get(0).getSettlementDate());

            // 第二个周期：06-10 到 06-16
            assertEquals(LocalDate.of(2025, 6, 10), periods.get(1).getPeriodStart());
            assertEquals(LocalDate.of(2025, 6, 16), periods.get(1).getPeriodEnd());
            assertEquals(LocalDate.of(2025, 6, 17), periods.get(1).getSettlementDate());

            // 第三个周期：06-17 到 06-23
            assertEquals(LocalDate.of(2025, 6, 17), periods.get(2).getPeriodStart());
            assertEquals(LocalDate.of(2025, 6, 23), periods.get(2).getPeriodEnd());
            assertEquals(LocalDate.of(2025, 6, 24), periods.get(2).getSettlementDate());
        }

        @Test
        @DisplayName("最后一个周期不足7天（2025-06-03 到 2025-06-12）应产生2个周期，第二个仅3天")
        void testCalculatePeriods_LastPeriodIncomplete_ShouldHandlePartialPeriod() {
            // Given
            LocalDate start = LocalDate.of(2025, 6, 3);  // 周二
            LocalDate end = LocalDate.of(2025, 6, 12);    // 周四

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(start, end);

            // Then
            assertEquals(2, periods.size());

            // 第一个周期：06-03 到 06-09（完整7天）
            assertEquals(LocalDate.of(2025, 6, 3), periods.get(0).getPeriodStart());
            assertEquals(LocalDate.of(2025, 6, 9), periods.get(0).getPeriodEnd());

            // 第二个周期：06-10 到 06-12（不足7天，按实际天数）
            assertEquals(LocalDate.of(2025, 6, 10), periods.get(1).getPeriodStart());
            assertEquals(LocalDate.of(2025, 6, 12), periods.get(1).getPeriodEnd());
            // 结算日 = nextWorkingDay(2025-06-12 周四) = 2025-06-13 周五
            assertEquals(LocalDate.of(2025, 6, 13), periods.get(1).getSettlementDate());
        }

        @Test
        @DisplayName("单天周期（start == end）应产生1个周期")
        void testCalculatePeriods_SingleDay_ShouldReturnOnePeriod() {
            // Given
            LocalDate date = LocalDate.of(2025, 6, 3); // 周二

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(date, date);

            // Then
            assertEquals(1, periods.size());
            assertEquals(date, periods.get(0).getPeriodStart());
            assertEquals(date, periods.get(0).getPeriodEnd());
            // 结算日 = nextWorkingDay(2025-06-03 周二) = 2025-06-04 周三
            assertEquals(LocalDate.of(2025, 6, 4), periods.get(0).getSettlementDate());
        }

        @Test
        @DisplayName("结算日遇周末应顺延（周期结束在周五，结算日为下周一）")
        void testCalculatePeriods_SettlementDateOnWeekend_ShouldPostpone() {
            // Given: 2025-06-07 周六到 2025-06-13 周五（7天周期）
            LocalDate start = LocalDate.of(2025, 6, 7);
            LocalDate end = LocalDate.of(2025, 6, 13);

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(start, end);

            // Then
            assertEquals(1, periods.size());
            // 结算日 = nextWorkingDay(2025-06-13 周五) = 2025-06-16 周一
            assertEquals(LocalDate.of(2025, 6, 16), periods.get(0).getSettlementDate());
        }

        @Test
        @DisplayName("结算日遇节假日应顺延（周期结束在国庆前）")
        void testCalculatePeriods_SettlementDateOnHoliday_ShouldPostpone() {
            // Given: 2025-09-24 到 2025-09-30（周期结束在9月30日周二）
            LocalDate start = LocalDate.of(2025, 9, 24);
            LocalDate end = LocalDate.of(2025, 9, 30);

            // When
            List<SettlementPeriodCalculator.SettlementPeriod> periods =
                    SettlementPeriodCalculator.calculatePeriods(start, end);

            // Then
            assertEquals(1, periods.size());
            // nextWorkingDay(2025-09-30 周二) = 2025-10-08 周三
            // 因为 10-01 到 10-07 是国庆假期
            assertEquals(LocalDate.of(2025, 10, 8), periods.get(0).getSettlementDate());
        }

        @Test
        @DisplayName("null start 参数应抛出 IllegalArgumentException")
        void testCalculatePeriods_NullStart_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> SettlementPeriodCalculator.calculatePeriods(null, LocalDate.of(2025, 6, 9)));
        }

        @Test
        @DisplayName("null end 参数应抛出 IllegalArgumentException")
        void testCalculatePeriods_NullEnd_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> SettlementPeriodCalculator.calculatePeriods(LocalDate.of(2025, 6, 3), null));
        }

        @Test
        @DisplayName("start > end 应抛出 IllegalArgumentException")
        void testCalculatePeriods_StartAfterEnd_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> SettlementPeriodCalculator.calculatePeriods(
                            LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 3)));
        }
    }

    // ==================== calculateSettlementDate 测试 ====================

    @Nested
    @DisplayName("calculateSettlementDate - 计算结算日")
    class CalculateSettlementDateTest {

        @Test
        @DisplayName("周一结束的周期，结算日为周二")
        void testCalculateSettlementDate_Monday_ShouldReturnTuesday() {
            // Given
            LocalDate periodEnd = LocalDate.of(2025, 6, 9); // 周一

            // When
            LocalDate result = SettlementPeriodCalculator.calculateSettlementDate(periodEnd);

            // Then
            assertEquals(LocalDate.of(2025, 6, 10), result); // 周二
        }

        @Test
        @DisplayName("周五结束的周期，结算日为下周一（跳过周末）")
        void testCalculateSettlementDate_Friday_ShouldReturnNextMonday() {
            // Given
            LocalDate periodEnd = LocalDate.of(2025, 6, 13); // 周五

            // When
            LocalDate result = SettlementPeriodCalculator.calculateSettlementDate(periodEnd);

            // Then
            assertEquals(LocalDate.of(2025, 6, 16), result); // 下周一
        }

        @Test
        @DisplayName("圣诞节前结束的周期，结算日应跳过圣诞节和周末")
        void testCalculateSettlementDate_BeforeChristmas_ShouldSkipHoliday() {
            // Given
            LocalDate periodEnd = LocalDate.of(2025, 12, 24); // 周三

            // When
            LocalDate result = SettlementPeriodCalculator.calculateSettlementDate(periodEnd);

            // Then
            // 12-25 圣诞节（节假日），12-26 节假日，12-27 周六，12-28 周日 → 12-29 周一
            assertEquals(LocalDate.of(2025, 12, 29), result);
        }

        @Test
        @DisplayName("null 参数应抛出 IllegalArgumentException")
        void testCalculateSettlementDate_Null_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> SettlementPeriodCalculator.calculateSettlementDate(null));
        }
    }
}
