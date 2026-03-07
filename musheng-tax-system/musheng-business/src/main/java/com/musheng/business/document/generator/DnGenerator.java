package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentDnItem;
import com.musheng.business.document.utils.DnPeriodCalculator;
import com.musheng.business.document.utils.DocumentNumberCalculator;
import com.musheng.business.document.utils.WorkingDayCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * DN送货单生成器
 *
 * <p>纯函数设计，无状态，不依赖系统时间，确保确定性输出。
 * 核心逻辑：按DN周期分组货件 → 每个MSKU独立一行编号 →
 * 备注列标注货件编号 → 生成编号 → 计算合计。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class DnGenerator {

    /** 供应商名称 */
    private static final String SUPPLIER_NAME = "Hong Kong Andeo Group Limited";

    /** 客户名称（繁体中文） */
    private static final String CUSTOMER_NAME = "東莞市慕聲商貿有限公司";

    /** DN周期间隔天数 */
    private static final int DN_PERIOD_DAYS = 21;

    private DnGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 根据锚点日期和货件数据生成DN送货单
     *
     * <p>算法流程：
     * 1. 按货件创建时间升序排序
     * 2. 计算DN日期序列（锚点 + N × 21天，非工作日顺延）
     * 3. 将货件按DN周期分组
     * 4. 对每个DN周期：每个MSKU独立一行，按顺序编号
     * 5. 备注列标注对应的FBA货件编号
     * 6. 生成DN编号，计算合计</p>
     *
     * @param anchor 锚点日期，不能为 null
     * @param shipments 货件输入数据列表，不能为 null
     * @param startSequence 起始编号序号
     * @return DN生成结果列表，按DN日期升序排列
     * @throws IllegalArgumentException 如果 anchor 或 shipments 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static List<DnGenerateResult> generate(LocalDate anchor, List<ShipmentInput> shipments,
                                                   int startSequence) {
        if (anchor == null) {
            throw new IllegalArgumentException("锚点日期不能为 null");
        }
        if (shipments == null) {
            throw new IllegalArgumentException("货件列表不能为 null");
        }
        if (shipments.isEmpty()) {
            return List.of();
        }

        // 1. 按货件创建时间升序排序
        List<ShipmentInput> sorted = shipments.stream()
                .sorted(Comparator.comparing(ShipmentInput::getCreateTime))
                .collect(Collectors.toList());

        // 2. 按DN周期分组货件
        Map<LocalDate, List<ShipmentInput>> groupedByDnDate = groupByDnPeriod(anchor, sorted);

        // 3. 为每个DN周期生成DN
        List<DnGenerateResult> results = new ArrayList<>();
        int sequence = startSequence;

        for (Map.Entry<LocalDate, List<ShipmentInput>> entry : groupedByDnDate.entrySet()) {
            LocalDate dnDate = entry.getKey();
            List<ShipmentInput> periodShipments = entry.getValue();

            DnGenerateResult result = buildDn(dnDate, periodShipments, sequence, groupedByDnDate, anchor);
            results.add(result);
            sequence++;
        }

        return results;
    }

    /**
     * 将货件按DN周期分组（基于ShipmentInput而非FbaShipment）
     *
     * <p>使用 DnPeriodCalculator.calculateDnDates 获取DN日期序列，
     * 然后自己实现基于 ShipmentInput 的分组逻辑。</p>
     *
     * @param anchor 锚点日期
     * @param sortedShipments 已按创建时间排序的货件列表
     * @return DN日期 → 货件列表的映射
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static Map<LocalDate, List<ShipmentInput>> groupByDnPeriod(
            LocalDate anchor, List<ShipmentInput> sortedShipments) {
        // 取最晚的创建日期作为 rangeEnd
        LocalDate latestDate = sortedShipments.stream()
                .map(s -> s.getCreateTime().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(anchor);

        // 扩展 rangeEnd：取锚点日期和最晚货件日期中较大的，再加一个周期
        // 修复：当锚点日期晚于所有货件创建日期时，确保能生成包含锚点的DN日期序列
        LocalDate baseDate = latestDate.isAfter(anchor) ? latestDate : anchor;
        LocalDate rangeEnd = baseDate.plusDays(DN_PERIOD_DAYS);

        // 计算DN日期序列
        List<LocalDate> dnDates = DnPeriodCalculator.calculateDnDates(anchor, rangeEnd);

        // 按DN周期分组
        Map<LocalDate, List<ShipmentInput>> result = new TreeMap<>();

        for (ShipmentInput shipment : sortedShipments) {
            LocalDate shipmentDate = shipment.getCreateTime().toLocalDate();
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
     * @param dnDates DN日期序列
     * @param shipmentDate 货件创建日期
     * @return 目标DN日期
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

    /**
     * 构建单份DN
     *
     * @param dnDate DN日期
     * @param shipments 该DN周期下的货件列表
     * @param sequence 编号序号
     * @param allGroups 所有DN分组（用于计算周期起止日）
     * @param anchor 锚点日期
     * @return DN生成结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static DnGenerateResult buildDn(LocalDate dnDate, List<ShipmentInput> shipments,
                                             int sequence,
                                             Map<LocalDate, List<ShipmentInput>> allGroups,
                                             LocalDate anchor) {
        // 生成编号
        String documentNo = DocumentNumberCalculator.generate(dnDate, sequence);

        // 构建明细列表：每个MSKU独立一行
        List<DocumentDnItem> items = new ArrayList<>();
        int lineNo = 1;

        for (ShipmentInput shipment : shipments) {
            for (ShipmentInput.MskuItem mskuItem : shipment.getItems()) {
                DocumentDnItem dnItem = new DocumentDnItem();
                dnItem.setLineNo(lineNo);
                dnItem.setMsku(mskuItem.getMsku());
                dnItem.setQuantity(mskuItem.getQuantity());
                dnItem.setShipmentNo(shipment.getShipmentNo());

                items.add(dnItem);
                lineNo++;
            }
        }

        // 计算合计
        int totalQuantity = items.stream().mapToInt(DocumentDnItem::getQuantity).sum();

        // 计算周期起止日
        LocalDate periodStart = calculatePeriodStart(dnDate, allGroups, anchor);
        LocalDate periodEnd = dnDate;

        // 构建DN主表
        DocumentDn dn = new DocumentDn();
        dn.setDocumentNo(documentNo);
        dn.setDnDate(dnDate);
        dn.setSupplierName(SUPPLIER_NAME);
        dn.setCustomerName(CUSTOMER_NAME);
        dn.setTotalQuantity(totalQuantity);
        dn.setPeriodStart(periodStart);
        dn.setPeriodEnd(periodEnd);

        return DnGenerateResult.builder()
                .dn(dn)
                .items(items)
                .build();
    }

    /**
     * 计算DN周期的起始日
     *
     * <p>第一个DN周期的起始日为最早货件创建日期，
     * 后续DN周期的起始日为上一个DN日期的下一天。</p>
     *
     * @param dnDate 当前DN日期
     * @param allGroups 所有DN分组
     * @param anchor 锚点日期
     * @return 周期起始日
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static LocalDate calculatePeriodStart(LocalDate dnDate,
                                                   Map<LocalDate, List<ShipmentInput>> allGroups,
                                                   LocalDate anchor) {
        List<LocalDate> sortedDnDates = new ArrayList<>(allGroups.keySet());
        int index = sortedDnDates.indexOf(dnDate);

        if (index <= 0) {
            // 第一个DN周期：起始日为锚点日期之前21天（或最早货件日期）
            List<ShipmentInput> firstGroupShipments = allGroups.get(dnDate);
            if (firstGroupShipments != null && !firstGroupShipments.isEmpty()) {
                return firstGroupShipments.stream()
                        .map(s -> s.getCreateTime().toLocalDate())
                        .min(LocalDate::compareTo)
                        .orElse(anchor);
            }
            return anchor;
        } else {
            // 后续DN周期：起始日为上一个DN日期的下一天
            return sortedDnDates.get(index - 1).plusDays(1);
        }
    }
}
