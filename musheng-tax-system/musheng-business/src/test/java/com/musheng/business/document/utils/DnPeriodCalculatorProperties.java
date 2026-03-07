package com.musheng.business.document.utils;

import com.musheng.business.fbashipment.entity.FbaShipment;
import net.jqwik.api.*;
import net.jqwik.time.api.Dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DnPeriodCalculator 属性测试
 *
 * <p>使用 jqwik 框架验证 DN 周期计算器的通用正确性属性。
 * 每个属性测试运行100次迭代，覆盖2020-2030年日期范围。</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class DnPeriodCalculatorProperties {

    /** DN周期间隔天数 */
    private static final int DN_PERIOD_DAYS = 21;

    // ==================== Arbitrary 提供器 ====================

    /**
     * 提供有效的锚点日期和范围结束日期对（anchor <= rangeEnd），范围2020-01-01到2030-12-31
     *
     * @return 日期对 Arbitrary [anchor, rangeEnd]
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate[]> validAnchorAndRangeEnd() {
        return Dates.dates()
                .between(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31))
                .tuple2()
                .map(t -> {
                    LocalDate d1 = t.get1();
                    LocalDate d2 = t.get2();
                    if (d1.isAfter(d2)) {
                        return new LocalDate[]{d2, d1};
                    }
                    return new LocalDate[]{d1, d2};
                });
    }

    /**
     * 提供锚点日期和随机货件列表的组合
     *
     * <p>锚点日期在2020-2030范围内，货件列表1-10个，
     * 每个货件的 createdDate 在锚点日期当天到锚点后90天范围内。</p>
     *
     * @return [锚点日期, 货件列表] 的组合 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<Object[]> anchorAndShipments() {
        return Dates.dates()
                .between(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 6, 30))
                .flatMap(anchor -> {
                    // 货件 createdDate 从锚点当天到锚点后90天范围内
                    // 确保货件日期在锚点之后，这样 groupByDnPeriod 能正确分组
                    LocalDate shipmentStart = anchor;
                    LocalDate shipmentEnd = anchor.plusDays(90);
                    // 使用 AtomicInteger 确保每个货件的 shipmentId 唯一
                    AtomicInteger counter = new AtomicInteger(0);
                    Arbitrary<FbaShipment> shipmentArb = Dates.dates()
                            .between(shipmentStart, shipmentEnd)
                            .flatMap(date -> Arbitraries.integers().between(0, 23)
                                    .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                            .map(minute -> {
                                                int idx = counter.getAndIncrement();
                                                FbaShipment s = new FbaShipment();
                                                s.setShipmentId("SHP-" + date + "-" + String.format("%02d%02d", hour, minute) + "-" + idx);
                                                s.setCreatedDate(LocalDateTime.of(date, LocalTime.of(hour, minute)));
                                                return s;
                                            })));
                    return shipmentArb.list().ofMinSize(1).ofMaxSize(10)
                            .map(shipments -> new Object[]{anchor, shipments});
                });
    }

    // ==================== 属性 8：DN周期确定性 — calculateDnDates ====================

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // 相邻DN日期间隔恰好21天（工作日调整前）：对于生成的DN日期序列，
    // 每对相邻日期对应的原始日期（anchor + N*21）间隔恰好21天
    /**
     * 属性8-1：相邻DN日期对应的原始日期间隔恰好21天
     *
     * <p>DN日期序列中第N个日期对应的原始日期为 anchor + N*21天，
     * 相邻原始日期间隔恰好21天。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param datePair 随机生成的有效日期对 [anchor, rangeEnd]
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void adjacentDnDatesRawIntervalShouldBe21Days(
            @ForAll("validAnchorAndRangeEnd") LocalDate[] datePair) {
        LocalDate anchor = datePair[0];
        LocalDate rangeEnd = datePair[1];

        List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

        // 验证每个DN日期对应的原始日期（anchor + N*21）间隔恰好21天
        for (int i = 1; i < dnDates.size(); i++) {
            LocalDate prevRaw = anchor.plusDays((long) (i - 1) * DN_PERIOD_DAYS);
            LocalDate currRaw = anchor.plusDays((long) i * DN_PERIOD_DAYS);
            long rawInterval = java.time.temporal.ChronoUnit.DAYS.between(prevRaw, currRaw);
            assertEquals(DN_PERIOD_DAYS, rawInterval,
                    "第 " + (i - 1) + " 和第 " + i + " 个DN日期对应的原始日期间隔应为21天，"
                            + "原始日期分别为 " + prevRaw + " 和 " + currRaw);
        }
    }

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // 所有DN日期应为工作日：isWorkingDay(dnDate) 为 true
    /**
     * 属性8-2：所有DN日期应为工作日
     *
     * <p>calculateDnDates 返回的每个日期都应满足 isWorkingDay 为 true。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param datePair 随机生成的有效日期对 [anchor, rangeEnd]
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void allDnDatesShouldBeWorkingDays(
            @ForAll("validAnchorAndRangeEnd") LocalDate[] datePair) {
        LocalDate anchor = datePair[0];
        LocalDate rangeEnd = datePair[1];

        List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

        for (int i = 0; i < dnDates.size(); i++) {
            assertTrue(WorkingDayCalculator.isWorkingDay(dnDates.get(i)),
                    "第 " + (i + 1) + " 个DN日期 " + dnDates.get(i) + " 应为工作日");
        }
    }

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // DN日期序列单调递增
    /**
     * 属性8-3：DN日期序列单调递增
     *
     * <p>DN日期序列中每个日期应严格大于前一个日期。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param datePair 随机生成的有效日期对 [anchor, rangeEnd]
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void dnDatesShouldBeStrictlyIncreasing(
            @ForAll("validAnchorAndRangeEnd") LocalDate[] datePair) {
        LocalDate anchor = datePair[0];
        LocalDate rangeEnd = datePair[1];

        List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

        for (int i = 1; i < dnDates.size(); i++) {
            assertTrue(dnDates.get(i).isAfter(dnDates.get(i - 1)),
                    "DN日期序列应单调递增，但第 " + i + " 个日期 " + dnDates.get(i)
                            + " 不大于第 " + (i - 1) + " 个日期 " + dnDates.get(i - 1));
        }
    }

    // ==================== 属性 8：DN周期确定性 — groupByDnPeriod ====================

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // 每份DN包含的货件集合互不重叠：同一个货件不会出现在两个不同DN日期的列表中
    /**
     * 属性8-4：每份DN包含的货件集合互不重叠
     *
     * <p>同一个货件（按 shipmentId 判断）不会出现在两个不同DN日期的列表中。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param data 随机生成的 [锚点日期, 货件列表] 组合
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SuppressWarnings("unchecked")
    @Property(tries = 100)
    void dnPeriodShipmentsShouldNotOverlap(@ForAll("anchorAndShipments") Object[] data) {
        LocalDate anchor = (LocalDate) data[0];
        List<FbaShipment> shipments = (List<FbaShipment>) data[1];

        Map<LocalDate, List<FbaShipment>> grouped = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

        // 收集所有已分配的货件 shipmentId，检查是否有重复
        Set<String> seenShipmentIds = new HashSet<>();
        for (Map.Entry<LocalDate, List<FbaShipment>> entry : grouped.entrySet()) {
            for (FbaShipment shipment : entry.getValue()) {
                boolean added = seenShipmentIds.add(shipment.getShipmentId());
                assertTrue(added,
                        "货件 " + shipment.getShipmentId() + " 出现在多个DN周期中，"
                                + "当前DN日期为 " + entry.getKey());
            }
        }
    }

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // 覆盖所有货件：所有输入货件都被分配到某个DN日期中
    /**
     * 属性8-5：所有输入货件都被分配到某个DN日期中
     *
     * <p>groupByDnPeriod 返回的所有货件数量之和应等于输入货件数量。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param data 随机生成的 [锚点日期, 货件列表] 组合
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SuppressWarnings("unchecked")
    @Property(tries = 100)
    void allShipmentsShouldBeCovered(@ForAll("anchorAndShipments") Object[] data) {
        LocalDate anchor = (LocalDate) data[0];
        List<FbaShipment> shipments = (List<FbaShipment>) data[1];

        Map<LocalDate, List<FbaShipment>> grouped = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

        // 统计分组后的货件总数
        int totalGrouped = grouped.values().stream()
                .mapToInt(List::size)
                .sum();

        assertEquals(shipments.size(), totalGrouped,
                "分组后的货件总数 " + totalGrouped + " 应等于输入货件数量 " + shipments.size());
    }

    // Feature: fba-document-generation, Property 8: DN周期确定性
    // 幂等性：相同输入多次调用，输出完全一致
    /**
     * 属性8-6：groupByDnPeriod 幂等性
     *
     * <p>相同的锚点日期和货件列表，多次调用 groupByDnPeriod 应返回完全一致的结果。</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     *
     * @param data 随机生成的 [锚点日期, 货件列表] 组合
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SuppressWarnings("unchecked")
    @Property(tries = 100)
    void groupByDnPeriodShouldBeIdempotent(@ForAll("anchorAndShipments") Object[] data) {
        LocalDate anchor = (LocalDate) data[0];
        List<FbaShipment> shipments = (List<FbaShipment>) data[1];

        // 调用两次
        Map<LocalDate, List<FbaShipment>> result1 = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);
        Map<LocalDate, List<FbaShipment>> result2 = DnPeriodCalculator.groupByDnPeriod(anchor, shipments);

        // 验证 key 集合一致
        assertEquals(result1.keySet(), result2.keySet(),
                "两次调用的DN日期集合应完全一致");

        // 验证每个DN日期下的货件列表一致（按 shipmentId 比较）
        for (LocalDate dnDate : result1.keySet()) {
            List<FbaShipment> list1 = result1.get(dnDate);
            List<FbaShipment> list2 = result2.get(dnDate);
            assertEquals(list1.size(), list2.size(),
                    "DN日期 " + dnDate + " 下的货件数量应一致");
            for (int i = 0; i < list1.size(); i++) {
                assertEquals(list1.get(i).getShipmentId(), list2.get(i).getShipmentId(),
                        "DN日期 " + dnDate + " 下第 " + (i + 1) + " 个货件的 shipmentId 应一致");
            }
        }
    }
}
