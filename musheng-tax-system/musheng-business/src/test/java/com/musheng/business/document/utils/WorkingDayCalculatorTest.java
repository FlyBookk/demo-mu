package com.musheng.business.document.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkingDayCalculator 单元测试
 *
 * <p>覆盖工作日判定、下一个工作日、最近工作日三个核心方法。
 * 测试场景包括：普通工作日、周末、节假日、null参数。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("WorkingDayCalculator 工作日计算器测试")
class WorkingDayCalculatorTest {

    // ==================== isWorkingDay 测试 ====================

    @Nested
    @DisplayName("isWorkingDay - 判断是否为工作日")
    class IsWorkingDayTest {

        @Test
        @DisplayName("普通工作日（2025-09-01 周一）应返回 true")
        void testIsWorkingDay_NormalMonday_ShouldReturnTrue() {
            // Given
            LocalDate monday = LocalDate.of(2025, 9, 1);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(monday);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("周六（2025-09-06）应返回 false")
        void testIsWorkingDay_Saturday_ShouldReturnFalse() {
            // Given
            LocalDate saturday = LocalDate.of(2025, 9, 6);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(saturday);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("周日（2025-09-07）应返回 false")
        void testIsWorkingDay_Sunday_ShouldReturnFalse() {
            // Given
            LocalDate sunday = LocalDate.of(2025, 9, 7);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(sunday);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("圣诞节（2025-12-25 周四）应返回 false")
        void testIsWorkingDay_Christmas_ShouldReturnFalse() {
            // Given
            LocalDate christmas = LocalDate.of(2025, 12, 25);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(christmas);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("国庆节（2025-10-01 周三）应返回 false")
        void testIsWorkingDay_NationalDay_ShouldReturnFalse() {
            // Given
            LocalDate nationalDay = LocalDate.of(2025, 10, 1);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(nationalDay);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("元旦（2025-01-01）应返回 false")
        void testIsWorkingDay_NewYear_ShouldReturnFalse() {
            // Given
            LocalDate newYear = LocalDate.of(2025, 1, 1);

            // When
            boolean result = WorkingDayCalculator.isWorkingDay(newYear);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("null 参数应抛出 IllegalArgumentException")
        void testIsWorkingDay_Null_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> WorkingDayCalculator.isWorkingDay(null));
        }
    }

    // ==================== nextWorkingDay 测试 ====================

    @Nested
    @DisplayName("nextWorkingDay - 返回下一个工作日")
    class NextWorkingDayTest {

        @Test
        @DisplayName("周五（2025-09-05）的下一个工作日应为周一（2025-09-08）")
        void testNextWorkingDay_Friday_ShouldReturnNextMonday() {
            // Given
            LocalDate friday = LocalDate.of(2025, 9, 5);

            // When
            LocalDate result = WorkingDayCalculator.nextWorkingDay(friday);

            // Then
            assertEquals(LocalDate.of(2025, 9, 8), result);
        }

        @Test
        @DisplayName("圣诞节（2025-12-25 周四）的下一个工作日应为 2025-12-29（周一，跳过12-26节假日+周末）")
        void testNextWorkingDay_Christmas_ShouldSkipHolidayAndWeekend() {
            // Given
            LocalDate christmas = LocalDate.of(2025, 12, 25);

            // When
            LocalDate result = WorkingDayCalculator.nextWorkingDay(christmas);

            // Then
            assertEquals(LocalDate.of(2025, 12, 29), result);
        }

        @Test
        @DisplayName("普通工作日周一（2025-09-01）的下一个工作日应为周二（2025-09-02）")
        void testNextWorkingDay_Monday_ShouldReturnTuesday() {
            // Given
            LocalDate monday = LocalDate.of(2025, 9, 1);

            // When
            LocalDate result = WorkingDayCalculator.nextWorkingDay(monday);

            // Then
            assertEquals(LocalDate.of(2025, 9, 2), result);
        }

        @Test
        @DisplayName("周六（2025-09-06）的下一个工作日应为周一（2025-09-08）")
        void testNextWorkingDay_Saturday_ShouldReturnMonday() {
            // Given
            LocalDate saturday = LocalDate.of(2025, 9, 6);

            // When
            LocalDate result = WorkingDayCalculator.nextWorkingDay(saturday);

            // Then
            assertEquals(LocalDate.of(2025, 9, 8), result);
        }

        @Test
        @DisplayName("null 参数应抛出 IllegalArgumentException")
        void testNextWorkingDay_Null_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> WorkingDayCalculator.nextWorkingDay(null));
        }
    }

    // ==================== nearestWorkingDay 测试 ====================

    @Nested
    @DisplayName("nearestWorkingDay - 返回最近工作日")
    class NearestWorkingDayTest {

        @Test
        @DisplayName("工作日（2025-09-01 周一）应返回当天")
        void testNearestWorkingDay_WorkingDay_ShouldReturnSameDay() {
            // Given
            LocalDate monday = LocalDate.of(2025, 9, 1);

            // When
            LocalDate result = WorkingDayCalculator.nearestWorkingDay(monday);

            // Then
            assertEquals(monday, result);
        }

        @Test
        @DisplayName("周六（2025-09-06）应返回下周一（2025-09-08）")
        void testNearestWorkingDay_Saturday_ShouldReturnNextMonday() {
            // Given
            LocalDate saturday = LocalDate.of(2025, 9, 6);

            // When
            LocalDate result = WorkingDayCalculator.nearestWorkingDay(saturday);

            // Then
            assertEquals(LocalDate.of(2025, 9, 8), result);
        }

        @Test
        @DisplayName("周日（2025-09-07）应返回下周一（2025-09-08）")
        void testNearestWorkingDay_Sunday_ShouldReturnNextMonday() {
            // Given
            LocalDate sunday = LocalDate.of(2025, 9, 7);

            // When
            LocalDate result = WorkingDayCalculator.nearestWorkingDay(sunday);

            // Then
            assertEquals(LocalDate.of(2025, 9, 8), result);
        }

        @Test
        @DisplayName("国庆节（2025-10-01）应返回 2025-10-08（跳过整个国庆假期）")
        void testNearestWorkingDay_NationalDay_ShouldSkipHoliday() {
            // Given
            LocalDate nationalDay = LocalDate.of(2025, 10, 1);

            // When
            LocalDate result = WorkingDayCalculator.nearestWorkingDay(nationalDay);

            // Then
            assertEquals(LocalDate.of(2025, 10, 8), result);
        }

        @Test
        @DisplayName("null 参数应抛出 IllegalArgumentException")
        void testNearestWorkingDay_Null_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> WorkingDayCalculator.nearestWorkingDay(null));
        }
    }
}
