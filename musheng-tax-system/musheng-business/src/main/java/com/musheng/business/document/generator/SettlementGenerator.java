package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.enums.SiteCode;
import com.musheng.business.document.utils.DocumentNumberCalculator;
import com.musheng.business.document.utils.WorkingDayCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结算单生成器
 *
 * <p>纯函数设计，无状态，不依赖系统时间，确保确定性输出。
 * 核心逻辑：按7天拆分子周期 → 每个子周期按站点拆分4份 → MSKU去重汇总 →
 * MSKU字母升序排列 → BigDecimal精确计算金额 → 生成编号（001-004）→ 计算合计。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class SettlementGenerator {

    /** 买方名称 */
    private static final String BUYER_NAME = "东莞市慕声商贸有限公司";

    /** 买方地址 */
    private static final String BUYER_ADDRESS = "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房";

    /** 卖方名称 */
    private static final String SELLER_NAME = "Hong Kong Andeo Group Limited";

    /** 金额精度（小数位数） */
    private static final int AMOUNT_SCALE = 4;

    private SettlementGenerator() {
        // 工具类，禁止实例化
    }

    /** 每份结算单覆盖的天数 */
    private static final int PERIOD_DAYS = 7;

    /**
     * 根据结算数据生成结算单（按7天拆分子周期，每个子周期按站点生成4份）
     *
     * <p>算法流程：
     * 1. 将大周期按7天拆分为多个子周期
     * 2. 每个子周期按站点拆分4份（使用 SiteCode 枚举）
     * 3. 同一站点内 MSKU 去重汇总
     * 4. MSKU 按字母升序排列
     * 5. BigDecimal 精确计算金额
     * 6. 每个子周期的结算日 = 子周期结束日的下一个工作日
     * 7. 同一结算日的4份结算单序号为 001-004</p>
     *
     * @param input 结算数据输入，不能为 null
     * @param startSequence 起始编号序号（用于同一天多份结算单的序号递增）
     * @return 结算单生成结果列表，按子周期和站点排列
     * @throws IllegalArgumentException 如果 input 为 null
     * @author wanhua
     * 20:00 2026年03月07日
     */
    public static List<SettlementGenerateResult> generate(SettlementInput input, int startSequence) {
        if (input == null) {
            throw new IllegalArgumentException("结算数据输入不能为 null");
        }
        if (input.getItems() == null || input.getItems().isEmpty()) {
            return List.of();
        }

        LocalDate periodStart = input.getPeriodStart();
        LocalDate periodEnd = input.getPeriodEnd();

        // 按站点分组全部数据
        Map<SiteCode, List<SettlementInput.SettlementDataItem>> siteGroups =
                groupBySite(input.getItems());

        // 按7天拆分子周期，每个子周期生成4份结算单
        List<SettlementGenerateResult> allResults = new ArrayList<>();
        LocalDate subStart = periodStart;

        while (!subStart.isAfter(periodEnd)) {
            LocalDate subEnd = subStart.plusDays(PERIOD_DAYS - 1);
            if (subEnd.isAfter(periodEnd)) {
                subEnd = periodEnd;
            }

            // 该子周期的结算日
            LocalDate settlementDate = WorkingDayCalculator.nextWorkingDay(subEnd);
            int sequence = startSequence;

            // 为每个站点生成一份结算单（使用全量数据，因为结算数据不按日期细分）
            for (SiteCode site : SiteCode.values()) {
                List<SettlementInput.SettlementDataItem> siteItems = siteGroups.get(site);

                SettlementGenerateResult result = buildSettlement(
                        subStart, subEnd, settlementDate, site, siteItems, sequence);
                allResults.add(result);
                sequence++;
            }

            subStart = subEnd.plusDays(1);
        }

        return allResults;
    }

    /**
     * 按站点分组结算数据
     *
     * <p>优先使用导入数据中的 siteCode（货币代码）字段匹配站点，
     * 如果 siteCode 为空则回退到 MSKU 前缀匹配。
     * 无法匹配的数据将被忽略。</p>
     *
     * @param items 结算数据明细列表
     * @return 站点 → 明细列表的映射
     * @author wanhua
     * 00:30 2026年03月02日
     */
    private static Map<SiteCode, List<SettlementInput.SettlementDataItem>> groupBySite(
            List<SettlementInput.SettlementDataItem> items) {
        Map<SiteCode, List<SettlementInput.SettlementDataItem>> groups = new LinkedHashMap<>();
        // 初始化所有站点
        for (SiteCode site : SiteCode.values()) {
            groups.put(site, new ArrayList<>());
        }
        // 优先按 siteCode（货币代码）分组，回退到 MSKU 前缀
        for (SettlementInput.SettlementDataItem item : items) {
            SiteCode site = null;
            // 优先使用 siteCode 字段匹配：先尝试货币代码（USD/GBP），再尝试站点名称（US/UK）
            if (item.getSiteCode() != null && !item.getSiteCode().isEmpty()) {
                site = SiteCode.fromCurrency(item.getSiteCode());
                if (site == null) {
                    site = SiteCode.fromSiteCodeName(item.getSiteCode());
                }
            }
            // 回退到 MSKU 前缀匹配
            if (site == null) {
                site = SiteCode.fromMskuPrefix(item.getMsku());
            }
            if (site != null) {
                groups.get(site).add(item);
            }
        }
        return groups;
    }

    /**
     * 构建单份结算单
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @param settlementDate 结算日
     * @param site 站点
     * @param siteItems 该站点的结算数据（可能为空列表）
     * @param sequence 编号序号
     * @return 结算单生成结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static SettlementGenerateResult buildSettlement(
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate settlementDate,
            SiteCode site,
            List<SettlementInput.SettlementDataItem> siteItems,
            int sequence) {

        // 生成编号
        String documentNo = DocumentNumberCalculator.generate(settlementDate, sequence);

        // MSKU 去重汇总 + 字母升序排列
        List<MskuAggregation> aggregated = aggregateAndSort(siteItems);

        // 构建明细列表
        List<DocumentSettlementItem> items = new ArrayList<>();
        int lineNo = 1;
        for (MskuAggregation agg : aggregated) {
            DocumentSettlementItem item = new DocumentSettlementItem();
            item.setLineNo(lineNo);
            item.setMsku(agg.msku);
            item.setCurrency(site.getCurrency());
            item.setUnitPrice(agg.unitPrice);
            item.setQuantity(agg.totalQuantity);
            item.setAmount(agg.unitPrice.multiply(BigDecimal.valueOf(agg.totalQuantity))
                    .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
            items.add(item);
            lineNo++;
        }

        // 计算合计
        int totalQuantity = items.stream().mapToInt(DocumentSettlementItem::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(DocumentSettlementItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 构建结算单主表
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo(documentNo);
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setSiteCode(site.getCurrency());
        settlement.setSiteSequence(site.getSequence());
        settlement.setBuyerName(BUYER_NAME);
        settlement.setBuyerAddress(BUYER_ADDRESS);
        settlement.setSellerName(SELLER_NAME);
        settlement.setTotalQuantity(totalQuantity);
        settlement.setTotalAmount(totalAmount);

        return SettlementGenerateResult.builder()
                .settlement(settlement)
                .items(items)
                .build();
    }

    /**
     * MSKU 去重汇总并按字母升序排列
     *
     * <p>相同 MSKU 的 quantity 相加，unitPrice 取第一条记录的值。
     * 最终按 MSKU 字母升序排列。</p>
     *
     * @param siteItems 同一站点的结算数据列表
     * @return 去重汇总后的 MSKU 列表（已排序）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static List<MskuAggregation> aggregateAndSort(
            List<SettlementInput.SettlementDataItem> siteItems) {
        if (siteItems == null || siteItems.isEmpty()) {
            return List.of();
        }

        // 按 MSKU 分组汇总
        Map<String, MskuAggregation> aggregationMap = new LinkedHashMap<>();
        for (SettlementInput.SettlementDataItem item : siteItems) {
            aggregationMap.compute(item.getMsku(), (msku, existing) -> {
                if (existing == null) {
                    return new MskuAggregation(msku, item.getUnitPrice(), item.getQuantity());
                }
                existing.totalQuantity += item.getQuantity();
                return existing;
            });
        }

        // 按 MSKU 字母升序排列
        return aggregationMap.values().stream()
                .sorted(Comparator.comparing(a -> a.msku))
                .collect(Collectors.toList());
    }

    /**
     * MSKU 汇总中间数据
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static class MskuAggregation {
        final String msku;
        final BigDecimal unitPrice;
        int totalQuantity;

        MskuAggregation(String msku, BigDecimal unitPrice, int totalQuantity) {
            this.msku = msku;
            this.unitPrice = unitPrice;
            this.totalQuantity = totalQuantity;
        }
    }
}
