package com.musheng.business.document.utils;

import com.musheng.business.fbashipment.entity.FbaShipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DnPeriodCalculator 单元测试
 *
 * <p>覆盖 DN 日期序列计算和货件按 DN 周期分组两个核心方法。
 * 测试场景包括：正常21天间隔、非工作日顺延、rangeEnd 在锚点之前、
 * 单周期/多周期货件分组、null/空参数异常。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("DnPeriodCalculator DN周期计算器测试")
class DnPeriodCalculatorTest {

    // ==================== calculateDnDates 测试 ====================

    @Nested
    @DisplayName("calculateDnDates - 计算DN日期序列")
    class CalculateDnDatesTest {

        @Test
        @DisplayName("锚点2025-05-22，rangeEnd 2025-07-01，应产生 [2025-05-22, 2025-06-12]")
        void testCalculateDnDates_NormalRange_ShouldReturn21DayIntervals() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            LocalDate rangeEnd = LocalDate.of(2025, 7, 1);

            // When
            List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

            // Then
            assertEquals(2, dnDates.size());
            assertEquals(LocalDate.of(2025, 5, 22), dnDates.get(0));
            assertEquals(LocalDate.of(2025, 6, 12), dnDates.get(1));
        }

        @Test
        @DisplayName("锚点2025-05-22，rangeEnd 2025-07-10，应产生3个DN日期")
        void testCalculateDnDates_LongerRange_ShouldReturnThreeDates() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            LocalDate rangeEnd = LocalDate.of(2025, 7, 10);

            // When
            List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

            // Then
            // 2025-05-22, 2025-06-12, 2025-07-03
            assertEquals(3, dnDates.size());
            assertEquals(LocalDate.of(2025, 5, 22), dnDates.get(0));
            assertEquals(LocalDate.of(2025, 6, 12), dnDates.get(1));
            assertEquals(LocalDate.of(2025, 7, 3), dnDates.get(2));
        }

        @Test
        @DisplayName("锚点遇非工作日应顺延到最近工作日")
        void testCalculateDnDates_AnchorOnNonWorkingDay_ShouldAdjust() {
            // Given — 2025-05-31 是周六且是端午节假日
            LocalDate anchor = LocalDate.of(2025, 5, 31);
            // rangeEnd 足够远以产生第二个DN日期
            LocalDate rangeEnd = LocalDate.of(2025, 7, 1);

            // When
            List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

            // Then
            // 锚点 2025-05-31 → nearestWorkingDay → 2025-06-03（周二，跳过5/31周六、6/1周日端午、6/2端午）
            // 第二个：2025-05-31 + 21 = 2025-06-21（周六）→ nearestWorkingDay → 2025-06-23（周一）
            assertFalse(dnDates.isEmpty());
            assertTrue(WorkingDayCalculator.isWorkingDay(dnDates.get(0)),
                    "第一个DN日期应为工作日");
            // 所有DN日期都应为工作日
            for (LocalDate dnDate : dnDates) {
                assertTrue(WorkingDayCalculator.isWorkingDay(dnDate),
                        "DN日期 " + dnDate + " 应为工作日");
            }
        }

        @Test
        @DisplayName("rangeEnd 在锚点之前应返回空列表")
        void testCalculateDnDates_RangeEndBeforeAnchor_ShouldReturnEmpty() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 7, 1);
            LocalDate rangeEnd = LocalDate.of(2025, 5, 22);

            // When
            List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

            // Then
            assertTrue(dnDates.isEmpty());
        }

        @Test
        @DisplayName("rangeEnd 等于锚点应返回仅包含锚点的列表")
        void testCalculateDnDates_RangeEndEqualsAnchor_ShouldReturnSingleDate() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            LocalDate rangeEnd = LocalDate.of(2025, 5, 22);

            // When
            List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

            // Then
            assertEquals(1, dnDates.size());
            assertEquals(LocalDate.of(2025, 5, 22), dnDates.get(0));
        }

        @Test
        @DisplayName("anchor 为 null 应抛出 IllegalArgumentException")
        void testCalculateDnDates_NullAnchor_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnPeriodCalculator.calculateDnDates(null, LocalDate.of(2025, 7, 1)));
        }

        @Test
        @DisplayName("rangeEnd 为 null 应抛出 IllegalArgumentException")
        void testCalculateDnDates_NullRangeEnd_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnPeriodCalculator.calculateDnDates(LocalDate.of(2025, 5, 22), null));
        }
    }


    // ==================== groupByDnPeriod 测试 ====================

    @Nested
    @DisplayName("groupByDnPeriod - 按DN周期分组货件")
    class GroupByDnPeriodTest {

        @Test
        @DisplayName("单个DN周期内的货件分组")
        void testGroupByDnPeriod_SinglePeriod_ShouldGroupCorrectly() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            List<FbaShipment> shipments = List.of(
                    createShipment("SHP001", LocalDateTime.of(2025, 5, 20, 10, 0)),
                    createShipment("SHP002", LocalDateTime.of(2025, 5, 22, 14, 0))
            );

            // When
            Map<LocalDate, List<FbaShipment>> result = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

            // Then
            // 第一个DN日期 2025-05-22，范围 (-∞, 2025-05-22]
            // SHP001 (5/20) 和 SHP002 (5/22) 都在此范围内
            assertEquals(1, result.size());
            assertTrue(result.containsKey(LocalDate.of(2025, 5, 22)));
            assertEquals(2, result.get(LocalDate.of(2025, 5, 22)).size());
        }

        @Test
        @DisplayName("多个DN周期的货件分组")
        void testGroupByDnPeriod_MultiplePeriods_ShouldGroupCorrectly() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            List<FbaShipment> shipments = List.of(
                    createShipment("SHP001", LocalDateTime.of(2025, 5, 20, 10, 0)),
                    createShipment("SHP002", LocalDateTime.of(2025, 5, 25, 14, 0)),
                    createShipment("SHP003", LocalDateTime.of(2025, 6, 10, 9, 0))
            );

            // When
            Map<LocalDate, List<FbaShipment>> result = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

            // Then
            // DN日期序列：2025-05-22, 2025-06-12
            // 第一个DN (5/22)：范围 (-∞, 5/22] → SHP001 (5/20)
            // 第二个DN (6/12)：范围 (5/22, 6/12] → SHP002 (5/25), SHP003 (6/10)
            assertEquals(2, result.size());
            assertEquals(1, result.get(LocalDate.of(2025, 5, 22)).size());
            assertEquals("SHP001", result.get(LocalDate.of(2025, 5, 22)).get(0).getShipmentId());
            assertEquals(2, result.get(LocalDate.of(2025, 6, 12)).size());
        }

        @Test
        @DisplayName("货件按 createdDate 正确分配到对应周期")
        void testGroupByDnPeriod_ShipmentAssignment_ShouldMatchPeriodBoundary() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            // 创建一个恰好在第一个DN日期当天的货件和一个在第二天的货件
            List<FbaShipment> shipments = List.of(
                    createShipment("SHP_ON_DN", LocalDateTime.of(2025, 5, 22, 23, 59)),
                    createShipment("SHP_AFTER_DN", LocalDateTime.of(2025, 5, 23, 0, 1))
            );

            // When
            Map<LocalDate, List<FbaShipment>> result = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

            // Then
            // SHP_ON_DN (5/22) → 第一个DN (5/22)，范围 (-∞, 5/22]
            // SHP_AFTER_DN (5/23) → 第二个DN (6/12)，范围 (5/22, 6/12]
            assertEquals(2, result.size());
            assertEquals(1, result.get(LocalDate.of(2025, 5, 22)).size());
            assertEquals("SHP_ON_DN", result.get(LocalDate.of(2025, 5, 22)).get(0).getShipmentId());
            assertEquals(1, result.get(LocalDate.of(2025, 6, 12)).size());
            assertEquals("SHP_AFTER_DN", result.get(LocalDate.of(2025, 6, 12)).get(0).getShipmentId());
        }

        @Test
        @DisplayName("没有货件的DN日期不出现在结果中")
        void testGroupByDnPeriod_EmptyPeriod_ShouldBeExcluded() {
            // Given
            LocalDate anchor = LocalDate.of(2025, 5, 22);
            // 所有货件都在第一个DN周期内，第二个DN周期无货件
            List<FbaShipment> shipments = List.of(
                    createShipment("SHP001", LocalDateTime.of(2025, 5, 20, 10, 0))
            );

            // When — rangeEnd 由最晚 createdDate 决定 (5/20)，只会产生一个DN日期 (5/22)
            Map<LocalDate, List<FbaShipment>> result = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.containsKey(LocalDate.of(2025, 5, 22)));
        }

        @Test
        @DisplayName("anchor 为 null 应抛出 IllegalArgumentException")
        void testGroupByDnPeriod_NullAnchor_ShouldThrowException() {
            List<FbaShipment> shipments = List.of(
                    createShipment("SHP001", LocalDateTime.of(2025, 5, 20, 10, 0))
            );
            assertThrows(IllegalArgumentException.class,
                    () -> DnPeriodCalculator.groupByDnPeriod(null, shipments));
        }

        @Test
        @DisplayName("shipments 为 null 应抛出 IllegalArgumentException")
        void testGroupByDnPeriod_NullShipments_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnPeriodCalculator.groupByDnPeriod(LocalDate.of(2025, 5, 22), null));
        }

        @Test
        @DisplayName("shipments 为空列表应抛出 IllegalArgumentException")
        void testGroupByDnPeriod_EmptyShipments_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnPeriodCalculator.groupByDnPeriod(LocalDate.of(2025, 5, 22), new ArrayList<>()));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用 FbaShipment 对象
     *
     * @param shipmentId 货件单号
     * @param createdDate 创建时间
     * @return FbaShipment 实例
     */
    private static FbaShipment createShipment(String shipmentId, LocalDateTime createdDate) {
        FbaShipment shipment = new FbaShipment();
        shipment.setShipmentId(shipmentId);
        shipment.setCreatedDate(createdDate);
        return shipment;
    }
}
