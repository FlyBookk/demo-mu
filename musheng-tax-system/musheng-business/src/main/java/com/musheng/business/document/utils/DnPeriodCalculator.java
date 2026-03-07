package com.musheng.business.document.utils;

import com.musheng.business.fbashipment.entity.FbaShipment;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DN周期计算器
 *
 * <p>纯函数工具类，无状态，确定性输出。
 * 基于锚点日期计算DN日期序列（锚点 + N × 21天），
 * 非工作日顺延到最近工作日。支持将货件按DN周期分组。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class DnPeriodCalculator {

    /** DN周期间隔天数 */
    private static final int DN_PERIOD_DAYS = 21;

    private DnPeriodCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 基于锚点日期计算DN日期序列
     *
     * <p>DN日期序列 = anchor + N × 21天（N=0,1,2,...）。
     * 如果计算出的日期是非工作日，顺延到最近工作日（使用 nearestWorkingDay）。
     * 生成日期直到超过 rangeEnd 为止。</p>
     *
     * @param anchor 锚点日期，不能为 null
     * @param rangeEnd 范围结束日期，不能为 null
     * @return DN日期序列（可能为空列表，当 rangeEnd 在锚点之前时）
     * @throws IllegalArgumentException 如果 anchor 或 rangeEnd 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static List<LocalDate> calculateDnDates(LocalDate anchor, LocalDate rangeEnd) {
        if (anchor == null) {
            throw new IllegalArgumentException("锚点日期参数不能为 null");
        }
        if (rangeEnd == null) {
            throw new IllegalArgumentException("范围结束日期参数不能为 null");
        }

        List<LocalDate> dnDates = new ArrayList<>();
        int n = 0;

        while (true) {
            // 计算原始DN日期：anchor + N × 21天
            LocalDate rawDate = anchor.plusDays((long) n * DN_PERIOD_DAYS);

            // 如果原始日期已超过 rangeEnd，停止生成
            if (rawDate.isAfter(rangeEnd)) {
                break;
            }

            // 非工作日顺延到最近工作日
            LocalDate adjustedDate = WorkingDayCalculator.nearestWorkingDay(rawDate);
            dnDates.add(adjustedDate);

            n++;
        }

        return dnDates;
    }

    /**
     * 将货件按DN周期分组
     *
     * <p>先计算DN日期序列（rangeEnd 取货件列表中最晚的 createdDate 的日期部分），
     * 然后将货件按 createdDate 分配到对应的DN周期中。</p>
     *
     * <p>分组规则：
     * <ul>
     *   <li>第一个DN日期的范围是 (-∞, firstDnDate]</li>
     *   <li>后续DN日期的范围是 (prevDnDate, currentDnDate]</li>
     *   <li>如果某个DN日期没有对应的货件，该日期不出现在结果中</li>
     * </ul>
     * </p>
     *
     * @param anchor 锚点日期，不能为 null
     * @param shipments 货件列表，不能为 null 或空
     * @return DN日期 → 货件列表的映射（使用 TreeMap 保持日期有序）
     * @throws IllegalArgumentException 如果 anchor 为 null，或 shipments 为 null/空
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static Map<LocalDate, List<FbaShipment>> groupByDnPeriod(
            LocalDate anchor, List<FbaShipment> shipments) {
        if (anchor == null) {
            throw new IllegalArgumentException("锚点日期参数不能为 null");
        }
        if (CollectionUtils.isEmpty(shipments)) {
            throw new IllegalArgumentException("货件列表不能为 null 或空");
        }

        // 按 createdDate 升序排序
        List<FbaShipment> sorted = shipments.stream()
                .sorted(Comparator.comparing(FbaShipment::getCreatedDate))
                .toList();

        // 取最晚的 createdDate 作为 rangeEnd
        LocalDate latestDate = sorted.get(sorted.size() - 1).getCreatedDate().toLocalDate();

        // 扩展 rangeEnd：取锚点日期和最晚货件日期中较大的，再加一个周期
        // 修复：当锚点日期晚于所有货件创建日期时，确保能生成包含锚点的DN日期序列
        LocalDate baseDate = latestDate.isAfter(anchor) ? latestDate : anchor;
        LocalDate rangeEnd = baseDate.plusDays(DN_PERIOD_DAYS);

        // 计算DN日期序列
        List<LocalDate> dnDates = calculateDnDates(anchor, rangeEnd);

        // 按DN周期分组
        Map<LocalDate, List<FbaShipment>> result = new TreeMap<>();

        for (FbaShipment shipment : sorted) {
            LocalDate shipmentDate = shipment.getCreatedDate().toLocalDate();
            LocalDate targetDnDate = findTargetDnDate(dnDates, shipmentDate);
            if (targetDnDate != null) {
                result.computeIfAbsent(targetDnDate, k -> new ArrayList<>()).add(shipment);
            }
        }

        return result;
    }

    /**
     * 查找货件应归属的DN日期
     *
     * <p>遍历DN日期序列，找到第一个满足条件的DN日期：
     * <ul>
     *   <li>第一个DN日期：shipmentDate <= firstDnDate</li>
     *   <li>后续DN日期：shipmentDate > prevDnDate 且 shipmentDate <= currentDnDate</li>
     * </ul>
     * </p>
     *
     * @param dnDates DN日期序列
     * @param shipmentDate 货件创建日期
     * @return 目标DN日期，如果找不到则返回最后一个DN日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static LocalDate findTargetDnDate(List<LocalDate> dnDates, LocalDate shipmentDate) {
        if (dnDates.isEmpty()) {
            return null;
        }

        for (int i = 0; i < dnDates.size(); i++) {
            LocalDate currentDnDate = dnDates.get(i);
            if (i == 0) {
                // 第一个DN日期：范围 (-∞, firstDnDate]
                if (!shipmentDate.isAfter(currentDnDate)) {
                    return currentDnDate;
                }
            } else {
                // 后续DN日期：范围 (prevDnDate, currentDnDate]
                LocalDate prevDnDate = dnDates.get(i - 1);
                if (shipmentDate.isAfter(prevDnDate) && !shipmentDate.isAfter(currentDnDate)) {
                    return currentDnDate;
                }
            }
        }

        // 如果货件日期超过所有DN日期，归入最后一个DN
        return dnDates.get(dnDates.size() - 1);
    }
}
