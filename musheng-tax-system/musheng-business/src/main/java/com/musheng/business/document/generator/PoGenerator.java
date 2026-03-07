package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentPoItem;
import com.musheng.business.document.utils.DocumentNumberCalculator;
import com.musheng.business.document.utils.WorkingDayCalculator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PO采购订单生成器
 *
 * <p>纯函数设计，无状态，不依赖系统时间，确保确定性输出。
 * 核心逻辑：按货件创建时间排序 → 推算PO日期 → 按PO日期分组 →
 * 按货件分组明细 → 首行填FBA地址后续留空 → 生成编号 → 计算合计。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class PoGenerator {

    /** 买方名称 */
    private static final String BUYER_NAME = "东莞市慕声商贸有限公司";

    /** 买方地址 */
    private static final String BUYER_ADDRESS = "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房";

    /** 卖方名称 */
    private static final String SELLER_NAME = "Hong Kong Andeo Group Limited";

    private PoGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 根据货件数据生成PO采购订单
     *
     * <p>算法流程：
     * 1. 按货件创建时间升序排序
     * 2. 推算每个货件的PO日期（创建时间所在周的下一个周二，如果当天是周二则取当天）
     * 3. 如果PO日期是非工作日，顺延到下一个工作日
     * 4. 同一PO日期的货件合并到同一份PO
     * 5. 按货件分组明细，首行填FBA地址后续留空
     * 6. 生成编号，计算合计</p>
     *
     * @param shipments 货件输入数据列表，不能为 null
     * @param startSequence 起始编号序号（用于同一天多份PO的序号递增）
     * @return PO生成结果列表，按PO日期升序排列
     * @throws IllegalArgumentException 如果 shipments 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static List<PoGenerateResult> generate(List<ShipmentInput> shipments, int startSequence) {
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

        // 2. 推算PO日期并按PO日期分组（保持插入顺序）
        Map<LocalDate, List<ShipmentInput>> groupedByPoDate = new LinkedHashMap<>();
        for (ShipmentInput shipment : sorted) {
            LocalDate poDate = calculatePoDate(shipment.getCreateTime());
            groupedByPoDate.computeIfAbsent(poDate, k -> new ArrayList<>()).add(shipment);
        }

        // 3. 为每个PO日期组生成PO
        List<PoGenerateResult> results = new ArrayList<>();
        int sequence = startSequence;

        for (Map.Entry<LocalDate, List<ShipmentInput>> entry : groupedByPoDate.entrySet()) {
            LocalDate poDate = entry.getKey();
            List<ShipmentInput> groupShipments = entry.getValue();

            PoGenerateResult result = buildPo(poDate, groupShipments, sequence);
            results.add(result);
            sequence++;
        }

        return results;
    }

    /**
     * 推算PO日期
     *
     * <p>规则：
     * 1. PO日期 = 货件创建时间所在周的下一个周二
     * 2. 如果创建日本身是周二则取当天
     * 3. 如果PO日期是非工作日，顺延到下一个工作日</p>
     *
     * @param createTime 货件创建时间
     * @return PO日期
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static LocalDate calculatePoDate(LocalDateTime createTime) {
        LocalDate createDate = createTime.toLocalDate();
        DayOfWeek dayOfWeek = createDate.getDayOfWeek();

        LocalDate poDate;
        if (dayOfWeek == DayOfWeek.TUESDAY) {
            // 创建日本身是周二，取当天
            poDate = createDate;
        } else if (dayOfWeek.getValue() < DayOfWeek.TUESDAY.getValue()) {
            // 周一 → 同周周二（+1天）
            poDate = createDate.plusDays(DayOfWeek.TUESDAY.getValue() - dayOfWeek.getValue());
        } else {
            // 周三~周日 → 下周周二
            int daysUntilNextTuesday = 7 - dayOfWeek.getValue() + DayOfWeek.TUESDAY.getValue();
            poDate = createDate.plusDays(daysUntilNextTuesday);
        }

        // 如果PO日期是非工作日，顺延到下一个工作日
        return WorkingDayCalculator.nearestWorkingDay(poDate);
    }

    /**
     * 构建单份PO
     *
     * @param poDate PO日期
     * @param shipments 该PO日期下的货件列表（已按创建时间排序）
     * @param sequence 编号序号
     * @return PO生成结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static PoGenerateResult buildPo(LocalDate poDate, List<ShipmentInput> shipments, int sequence) {
        // 生成编号
        String documentNo = DocumentNumberCalculator.generate(poDate, sequence);

        // 构建明细列表
        List<DocumentPoItem> items = new ArrayList<>();
        int sortOrder = 1;

        for (ShipmentInput shipment : shipments) {
            boolean isFirstItem = true;
            for (ShipmentInput.MskuItem mskuItem : shipment.getItems()) {
                DocumentPoItem poItem = new DocumentPoItem();
                poItem.setShipmentNo(shipment.getShipmentNo());
                poItem.setMsku(mskuItem.getMsku());
                poItem.setQuantity(mskuItem.getQuantity());
                poItem.setSortOrder(sortOrder);

                // 首行填FBA地址，后续行留空
                if (isFirstItem) {
                    poItem.setFbaAddress(shipment.getFullAddress());
                    isFirstItem = false;
                } else {
                    poItem.setFbaAddress("");
                }

                items.add(poItem);
                sortOrder++;
            }
        }

        // 计算合计
        int totalQuantity = items.stream().mapToInt(DocumentPoItem::getQuantity).sum();

        // 构建PO主表
        DocumentPo po = new DocumentPo();
        po.setDocumentNo(documentNo);
        po.setPoDate(poDate);
        po.setBuyerName(BUYER_NAME);
        po.setBuyerAddress(BUYER_ADDRESS);
        po.setSellerName(SELLER_NAME);
        po.setTotalQuantity(totalQuantity);
        po.setShipmentCount(shipments.size());

        return PoGenerateResult.builder()
                .po(po)
                .items(items)
                .build();
    }
}
