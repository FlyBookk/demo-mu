package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
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
 * 核心逻辑：按自然月拆分子周期 → 每个月按站点拆分4份 → MSKU去重汇总 →
 * MSKU字母升序排列 → BigDecimal精确计算金额 → 生成编号（001-004）→ 计算合计。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class SettlementGenerator {

    /** 金额精度（小数位数） */
    private static final int AMOUNT_SCALE = 4;

    private SettlementGenerator() {
        // 工具类，禁止实例化
    }

    /** 结算日为下个月的第几日 */
    private static final int SETTLEMENT_DAY_OF_MONTH = 5;

    /**
     * 根据结算数据生成结算单（按自然月拆分，每月按站点生成多份）
     *
     * <p>算法流程：
     * 1. 将大周期按自然月拆分（如 2025-01-01 ~ 2025-03-31 → 1月、2月、3月）
     * 2. 每个自然月只取该月内的交易数据（按 transactionDate 过滤）
     * 3. 每个自然月按站点拆分（使用 SiteCode 枚举）
     * 4. 同一站点内 MSKU 去重汇总
     * 5. MSKU 按字母升序排列
     * 6. BigDecimal 精确计算金额
     * 7. 每月的结算日 = 下个月 5 日（非工作日顺延）
     * 8. 同一结算日的多份结算单序号为 001-004</p>
     *
     * @param input 结算数据输入，不能为 null
     * @param startSequence 起始编号序号
     * @param party 交易方配置（买方/卖方信息），不能为 null
     * @return 结算单生成结果列表，按月份和站点排列
     * @throws IllegalArgumentException 如果 input 或 party 为 null
     * @author wanhua
     * 10:30 2026年03月07日
     */
    /**
     * 根据结算数据生成结算单（使用默认交易方配置，仅用于测试）
     *
     * @param input 结算数据输入，不能为 null
     * @param startSequence 起始编号序号
     * @return 结算单生成结果列表
     * @author wanhua
     * 10:30 2026年03月22日
     */
    public static List<SettlementGenerateResult> generate(SettlementInput input, int startSequence) {
        DocumentPartyConfig defaultParty = new DocumentPartyConfig();
        defaultParty.setBuyerName("东莞市慕声商贸有限公司");
        defaultParty.setBuyerAddress("广东省东莞市");
        defaultParty.setSellerName("Hong Kong Andeo Group Limited");
        return generate(input, startSequence, defaultParty);
    }

    /**
     * 根据结算数据生成结算单（按自然月拆分，每月按站点生成多份）
     *
     * @param input 结算数据输入，不能为 null
     * @param startSequence 起始编号序号
     * @param party 交易方配置（买方/卖方信息），不能为 null
     * @return 结算单生成结果列表，按月份和站点排列
     * @throws IllegalArgumentException 如果 input 或 party 为 null
     * @author wanhua
     * 10:30 2026年03月07日
     */
    public static List<SettlementGenerateResult> generate(SettlementInput input, int startSequence,
                                                           DocumentPartyConfig party) {
        if (input == null) {
            throw new IllegalArgumentException("结算数据输入不能为 null");
        }
        if (party == null) {
            throw new IllegalArgumentException("交易方配置不能为 null");
        }
        if (input.getItems() == null || input.getItems().isEmpty()) {
            return List.of();
        }
        // selectedSiteCodes 为空时：从 items 中提取去重的站点列表作为兜底
        if (input.getSelectedSiteCodes() == null || input.getSelectedSiteCodes().isEmpty()) {
            List<String> derived = input.getItems().stream()
                    .map(SettlementInput.SettlementDataItem::getSiteCode)
                    .filter(s -> s != null && !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            SettlementInput fallback = SettlementInput.builder()
                    .periodStart(input.getPeriodStart())
                    .periodEnd(input.getPeriodEnd())
                    .selectedSiteCodes(derived)
                    .siteCurrencyMap(input.getSiteCurrencyMap())
                    .items(input.getItems())
                    .build();
            return generate(fallback, startSequence, party);
        }

        LocalDate periodStart = input.getPeriodStart();
        LocalDate periodEnd = input.getPeriodEnd();

        List<SettlementGenerateResult> allResults = new ArrayList<>();

        // 按自然月拆分：从 periodStart 所在月到 periodEnd 所在月
        LocalDate monthStart = periodStart.withDayOfMonth(1);
        while (!monthStart.isAfter(periodEnd)) {
            // 当月最后一天
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            // 实际周期起止：与请求范围取交集
            LocalDate subStart = monthStart.isBefore(periodStart) ? periodStart : monthStart;
            LocalDate subEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;

            // 结算日 = 下个月 5 日（非工作日顺延）
            LocalDate nextMonth5th = monthStart.plusMonths(1).withDayOfMonth(SETTLEMENT_DAY_OF_MONTH);
            LocalDate settlementDate = WorkingDayCalculator.nearestWorkingDay(nextMonth5th);

            // 过滤出本月的交易数据
            final LocalDate finalSubStart = subStart;
            final LocalDate finalSubEnd = subEnd;
            List<SettlementInput.SettlementDataItem> monthItems = input.getItems().stream()
                    .filter(item -> {
                        if (item.getTransactionDate() == null) return true; // 无日期的数据归入当前月
                        return !item.getTransactionDate().isBefore(finalSubStart)
                                && !item.getTransactionDate().isAfter(finalSubEnd);
                    })
                    .collect(Collectors.toList());

            // 按站点分组本月数据（动态，基于 siteCurrencyMap）
            Map<String, List<SettlementInput.SettlementDataItem>> siteGroups =
                    groupBySite(monthItems, input.getSelectedSiteCodes(), input.getSiteCurrencyMap());

            // 只对前端传入的站点生成，每个站点独立编号（同一结算日同一站点只有一份）
            for (String siteCode : input.getSelectedSiteCodes()) {
                String currency = resolveCurrency(siteCode, input.getSiteCurrencyMap(),
                        siteGroups.getOrDefault(siteCode, List.of()));
                List<SettlementInput.SettlementDataItem> siteItems =
                        siteGroups.getOrDefault(siteCode, List.of());
                SettlementGenerateResult result = buildSettlement(
                        subStart, subEnd, settlementDate, siteCode, currency, siteItems, startSequence, party);
                allResults.add(result);
            }

            monthStart = monthStart.plusMonths(1);
        }

        return allResults;
    }

    /**
     * 按站点分组结算数据（动态版，基于 t_marketplace 配置，不依赖枚举）
     *
     * <p>将每条明细归到 selectedSiteCodes 中的某个站点：
     * 1. 优先直接匹配 item.siteCode（如 "US"）
     * 2. 若 item.siteCode 是货币代码（如 "USD"），通过 siteCurrencyMap 反查 siteCode
     * 3. 无法匹配的数据忽略</p>
     */
    private static Map<String, List<SettlementInput.SettlementDataItem>> groupBySite(
            List<SettlementInput.SettlementDataItem> items,
            List<String> selectedSiteCodes,
            Map<String, String> siteCurrencyMap) {
        // 初始化各站点的空列表
        Map<String, List<SettlementInput.SettlementDataItem>> groups = new LinkedHashMap<>();
        for (String sc : selectedSiteCodes) {
            groups.put(sc, new ArrayList<>());
        }

        // 构建货币→站点反查表（USD→US），用于 item.siteCode 是货币代码时的回退匹配
        Map<String, String> currencyToSite = new LinkedHashMap<>();
        if (siteCurrencyMap != null) {
            siteCurrencyMap.forEach((sc, currency) -> currencyToSite.putIfAbsent(currency, sc));
        }

        for (SettlementInput.SettlementDataItem item : items) {
            String matched = null;
            String itemSite = item.getSiteCode();
            if (itemSite != null && !itemSite.isEmpty()) {
                if (groups.containsKey(itemSite)) {
                    matched = itemSite;                          // 直接匹配（US→US）
                } else {
                    String reversed = currencyToSite.get(itemSite);
                    if (reversed != null && groups.containsKey(reversed)) {
                        matched = reversed;                      // 货币反查（USD→US）
                    }
                }
            }
            if (matched != null) {
                groups.get(matched).add(item);
            }
        }
        return groups;
    }

    /**
     * 动态获取站点的货币代码（来自 t_marketplace）
     *
     * <p>优先从 siteCurrencyMap 查找；找不到时从明细数据中取第一条记录的货币。</p>
     */
    private static String resolveCurrency(String siteCode, Map<String, String> siteCurrencyMap,
                                          List<SettlementInput.SettlementDataItem> siteItems) {
        if (siteCurrencyMap != null && siteCurrencyMap.containsKey(siteCode)) {
            return siteCurrencyMap.get(siteCode);
        }
        // 回退：从明细数据取第一条有货币的记录
        if (siteItems != null) {
            for (SettlementInput.SettlementDataItem item : siteItems) {
                if (item.getCurrency() != null && !item.getCurrency().isEmpty()) {
                    return item.getCurrency();
                }
            }
        }
        return "";
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
     * @param party 交易方配置
     * @return 结算单生成结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static SettlementGenerateResult buildSettlement(
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate settlementDate,
            String siteCode,
            String currency,
            List<SettlementInput.SettlementDataItem> siteItems,
            int sequence,
            DocumentPartyConfig party) {

        // 生成编号：同一站点同一结算日只有一份，固定从 startSequence 开始
        String documentNo = DocumentNumberCalculator.generate(settlementDate, sequence);
        // siteSequence 直接存站点编码（US/UK/JP...），动态配置，不依赖固定序号
        String siteSequence = siteCode;

        // MSKU 去重汇总 + 字母升序排列
        List<MskuAggregation> aggregated = aggregateAndSort(siteItems);

        // 构建明细列表
        List<DocumentSettlementItem> items = new ArrayList<>();
        int lineNo = 1;
        for (MskuAggregation agg : aggregated) {
            DocumentSettlementItem item = new DocumentSettlementItem();
            item.setLineNo(lineNo);
            item.setMsku(agg.msku);
            item.setCurrency(currency);
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

        // 构建结算单主表（siteCode 存站点代码如 US/CA/UK/DE，siteSequence 按选择顺序）
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo(documentNo);
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setSiteCode(siteCode);
        settlement.setSiteSequence(siteSequence);
        settlement.setBuyerName(party.getBuyerName());
        settlement.setBuyerAddress(party.getBuyerAddress());
        settlement.setSellerName(party.getSellerName());
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
