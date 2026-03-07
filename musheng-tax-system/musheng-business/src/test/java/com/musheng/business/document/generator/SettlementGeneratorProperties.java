package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.enums.SiteCode;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementGenerator 属性测试
 *
 * <p>使用 jqwik 框架验证结算单生成器的通用正确性属性。
 * 覆盖属性10（站点拆分与序号映射）、属性11（金额计算精确性）、
 * 属性12（MSKU去重汇总与排序）、属性5+6（合计行invariant + 幂等性）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SettlementGeneratorProperties {

    /** 固定结算周期起始日（单周期，避免多周期复杂性） */
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 9, 2);

    /** 固定结算周期结束日 */
    private static final LocalDate PERIOD_END = LocalDate.of(2025, 9, 8);

    // ==================== 自定义 Arbitrary 生成器 ====================

    /**
     * 生成随机MSKU编码后缀
     *
     * @return MSKU后缀 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<String> mskuSuffixes() {
        return Arbitraries.strings()
                .alpha().numeric().ofMinLength(3).ofMaxLength(8);
    }

    /**
     * 生成随机单价（0.01 ~ 999.99）
     *
     * @return 单价 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<BigDecimal> unitPrices() {
        return Arbitraries.integers().between(1, 99999)
                .map(i -> BigDecimal.valueOf(i).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    /**
     * 生成4站点结算数据（每个站点至少1条MSKU数据）
     *
     * <p>用于属性10：站点拆分与序号映射测试。
     * 每个站点生成1~3条MSKU数据，MSKU前缀分别为 MSUS-/MSCA-/MSUK-/MSEU-。</p>
     *
     * @return SettlementInput Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<SettlementInput> fourSiteInputs() {
        return Combinators.combine(
                siteItems(SiteCode.US),
                siteItems(SiteCode.CA),
                siteItems(SiteCode.UK),
                siteItems(SiteCode.EU)
        ).as((usItems, caItems, ukItems, euItems) -> {
            List<SettlementInput.SettlementDataItem> allItems = new ArrayList<>();
            allItems.addAll(usItems);
            allItems.addAll(caItems);
            allItems.addAll(ukItems);
            allItems.addAll(euItems);
            return SettlementInput.builder()
                    .periodStart(PERIOD_START)
                    .periodEnd(PERIOD_END)
                    .items(allItems)
                    .build();
        });
    }

    /**
     * 生成指定站点的结算数据明细列表（1~3条，MSKU唯一）
     *
     * @param site 站点
     * @return 明细列表 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Arbitrary<List<SettlementInput.SettlementDataItem>> siteItems(SiteCode site) {
        return Arbitraries.integers().between(1, 3).flatMap(count ->
                Combinators.combine(
                        mskuSuffixes().list().ofSize(count),
                        unitPrices().list().ofSize(count),
                        Arbitraries.integers().between(1, 1000).list().ofSize(count)
                ).as((suffixes, prices, quantities) -> {
                    List<SettlementInput.SettlementDataItem> items = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        // 添加索引后缀确保MSKU唯一
                        String msku = site.getMskuPrefix() + suffixes.get(i) + "-" + i;
                        items.add(SettlementInput.SettlementDataItem.builder()
                                .siteCode(site.name())
                                .msku(msku)
                                .currency(site.getCurrency())
                                .unitPrice(prices.get(i))
                                .quantity(quantities.get(i))
                                .build());
                    }
                    return items;
                })
        );
    }

    /**
     * 生成单站点结算数据（包含重复MSKU，用于去重汇总测试）
     *
     * <p>用于属性12：MSKU去重汇总与排序测试。
     * 生成2~5条MSKU数据，其中部分MSKU重复。</p>
     *
     * @return SettlementInput Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<SettlementInput> singleSiteWithDuplicateMskuInputs() {
        // 生成2~4个不同的MSKU后缀
        Arbitrary<List<String>> distinctSuffixes = mskuSuffixes().list().ofMinSize(2).ofMaxSize(4);

        return distinctSuffixes.flatMap(suffixes -> {
            // 从已有MSKU中随机选取，构造包含重复的列表
            int totalItems = suffixes.size() + Arbitraries.integers().between(1, 3).sample();
            return Combinators.combine(
                    Arbitraries.integers().between(0, suffixes.size() - 1)
                            .list().ofSize(Math.min(totalItems, 8)),
                    unitPrices().list().ofSize(1),
                    Arbitraries.integers().between(1, 1000)
                            .list().ofSize(Math.min(totalItems, 8))
            ).as((indices, prices, quantities) -> {
                BigDecimal fixedPrice = prices.get(0);
                List<SettlementInput.SettlementDataItem> items = new ArrayList<>();
                for (int i = 0; i < indices.size(); i++) {
                    String msku = SiteCode.US.getMskuPrefix() + suffixes.get(indices.get(i));
                    items.add(SettlementInput.SettlementDataItem.builder()
                            .siteCode("US")
                            .msku(msku)
                            .currency("USD")
                            .unitPrice(fixedPrice)
                            .quantity(quantities.get(i))
                            .build());
                }
                return SettlementInput.builder()
                        .periodStart(PERIOD_START)
                        .periodEnd(PERIOD_END)
                        .items(items)
                        .build();
            });
        });
    }

    /**
     * 生成单条结算明细数据（用于金额精确性测试）
     *
     * @return SettlementInput Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<SettlementInput> singleItemInputs() {
        return Combinators.combine(
                mskuSuffixes(),
                unitPrices(),
                Arbitraries.integers().between(1, 1000)
        ).as((suffix, price, qty) -> {
            String msku = SiteCode.US.getMskuPrefix() + suffix;
            List<SettlementInput.SettlementDataItem> items = List.of(
                    SettlementInput.SettlementDataItem.builder()
                            .siteCode("US")
                            .msku(msku)
                            .currency("USD")
                            .unitPrice(price)
                            .quantity(qty)
                            .build()
            );
            return SettlementInput.builder()
                    .periodStart(PERIOD_START)
                    .periodEnd(PERIOD_END)
                    .items(items)
                    .build();
        });
    }


    // ==================== 属性 10：站点拆分与序号映射 ====================

    // Feature: fba-document-generation, Property 10: 站点拆分与序号映射
    // 拆分后恰好4份，序号001-004对应USD/CAD/GBP/EUR
    /**
     * 属性10-1：4站点数据拆分后应恰好产生4份结算单，序号001-004对应USD/CAD/GBP/EUR
     *
     * <p>对于任意包含4个站点的结算数据，生成的结算单应恰好4份，
     * 序号分别为001/002/003/004，对应USD/CAD/GBP/EUR。</p>
     *
     * <p><b>Validates: Requirements 4.2, 9.4</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertySiteSplitShouldProduceFourSettlements(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then - 恰好4份
        assertEquals(4, results.size(),
                "4站点数据应生成恰好4份结算单");

        // 验证序号和站点映射
        Map<String, String> expectedMapping = Map.of(
                "001", "USD",
                "002", "CAD",
                "003", "GBP",
                "004", "EUR"
        );

        for (SettlementGenerateResult result : results) {
            DocumentSettlement settlement = result.getSettlement();
            String seq = settlement.getSiteSequence();
            String siteCode = settlement.getSiteCode();

            assertTrue(expectedMapping.containsKey(seq),
                    "序号 " + seq + " 不在预期范围 001-004 内");
            assertEquals(expectedMapping.get(seq), siteCode,
                    "序号 " + seq + " 应对应 " + expectedMapping.get(seq) + "，实际为 " + siteCode);
        }
    }

    // Feature: fba-document-generation, Property 10: 站点拆分与序号映射
    // 每份结算单中MSKU前缀与站点一致
    /**
     * 属性10-2：每份结算单中的MSKU前缀应与对应站点一致
     *
     * <p>MSUS-→001/USD, MSCA-→002/CAD, MSUK-→003/GBP, MSEU-→004/EUR。</p>
     *
     * <p><b>Validates: Requirements 4.2, 9.4</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyMskuPrefixShouldMatchSite(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then - 每份结算单中MSKU前缀与站点一致
        Map<String, String> seqToPrefix = Map.of(
                "001", "MSUS-",
                "002", "MSCA-",
                "003", "MSUK-",
                "004", "MSEU-"
        );

        for (SettlementGenerateResult result : results) {
            String seq = result.getSettlement().getSiteSequence();
            String expectedPrefix = seqToPrefix.get(seq);

            for (DocumentSettlementItem item : result.getItems()) {
                assertTrue(item.getMsku().startsWith(expectedPrefix),
                        "序号 " + seq + " 的结算单中 MSKU " + item.getMsku()
                                + " 应以 " + expectedPrefix + " 开头");
            }
        }
    }

    // ==================== 属性 11：金额计算精确性 ====================

    // Feature: fba-document-generation, Property 11: 金额计算精确性
    // amount = quantity × unitPrice（BigDecimal精确比较）
    /**
     * 属性11：每条明细的 amount 应精确等于 quantity × unitPrice
     *
     * <p>对于任意结算数据，生成的每条明细行的金额应精确等于
     * 数量乘以单价（BigDecimal精确比较，无浮点误差）。</p>
     *
     * <p><b>Validates: Requirements 4.3, 9.7</b></p>
     *
     * @param input 随机生成的单条结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyAmountShouldEqualQuantityTimesUnitPrice(
            @ForAll("singleItemInputs") SettlementInput input) {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then - 检查所有非空结算单的明细
        for (SettlementGenerateResult result : results) {
            for (DocumentSettlementItem item : result.getItems()) {
                BigDecimal expectedAmount = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .setScale(4, RoundingMode.HALF_UP);

                assertEquals(0, expectedAmount.compareTo(item.getAmount()),
                        "MSKU " + item.getMsku() + " 金额不正确：期望 "
                                + expectedAmount + "，实际 " + item.getAmount()
                                + "（单价=" + item.getUnitPrice() + "，数量=" + item.getQuantity() + "）");
            }
        }
    }

    // ==================== 属性 12：MSKU去重汇总与排序 ====================

    // Feature: fba-document-generation, Property 12: MSKU去重汇总与排序
    // 同一MSKU只出现一次，数量为汇总值；明细按MSKU字母升序排列
    /**
     * 属性12-1：同一MSKU在结算单中只出现一次，数量为汇总值
     *
     * <p>对于任意包含重复MSKU的结算数据，生成的结算单中
     * 同一MSKU应只出现一次，数量为所有记录的汇总值。</p>
     *
     * <p><b>Validates: Requirements 4.8, 4.11, 9.8</b></p>
     *
     * @param input 随机生成的包含重复MSKU的结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyDuplicateMskuShouldBeAggregated(
            @ForAll("singleSiteWithDuplicateMskuInputs") SettlementInput input) {
        // Given - 计算输入中每个MSKU的汇总数量
        Map<String, Integer> expectedQuantities = input.getItems().stream()
                .collect(Collectors.groupingBy(
                        SettlementInput.SettlementDataItem::getMsku,
                        Collectors.summingInt(SettlementInput.SettlementDataItem::getQuantity)
                ));

        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then - 找到US站点的结算单（输入数据全部为US站点）
        SettlementGenerateResult usResult = results.stream()
                .filter(r -> "001".equals(r.getSettlement().getSiteSequence()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到US站点结算单"));

        // 验证MSKU唯一性
        List<String> mskuList = usResult.getItems().stream()
                .map(DocumentSettlementItem::getMsku)
                .collect(Collectors.toList());
        Set<String> mskuSet = new HashSet<>(mskuList);
        assertEquals(mskuSet.size(), mskuList.size(),
                "结算单中存在重复MSKU");

        // 验证汇总数量
        for (DocumentSettlementItem item : usResult.getItems()) {
            Integer expectedQty = expectedQuantities.get(item.getMsku());
            assertNotNull(expectedQty,
                    "结算单中出现了输入数据中不存在的MSKU: " + item.getMsku());
            assertEquals(expectedQty.intValue(), item.getQuantity().intValue(),
                    "MSKU " + item.getMsku() + " 汇总数量不正确：期望 "
                            + expectedQty + "，实际 " + item.getQuantity());
        }
    }

    // Feature: fba-document-generation, Property 12: MSKU去重汇总与排序
    // 明细按MSKU字母升序排列
    /**
     * 属性12-2：结算单明细应按MSKU字母升序排列
     *
     * <p>对于任意结算数据，生成的结算单明细应按MSKU编码字母升序排列，
     * 确保排序确定性。</p>
     *
     * <p><b>Validates: Requirements 4.11, 9.8</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyItemsShouldBeSortedByMskuAscending(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then - 每份结算单的明细应按MSKU升序排列
        for (SettlementGenerateResult result : results) {
            List<DocumentSettlementItem> items = result.getItems();
            for (int i = 1; i < items.size(); i++) {
                String prev = items.get(i - 1).getMsku();
                String curr = items.get(i).getMsku();
                assertTrue(prev.compareTo(curr) <= 0,
                        "MSKU排序不正确：" + prev + " 应在 " + curr + " 之前");
            }
        }
    }


    // ==================== 属性 5+6：合计行 invariant + 幂等性 ====================

    // Feature: fba-document-generation, Property 5: 合计行数量/金额 invariant（结算单部分）
    // totalQuantity = sum of all item quantities
    // totalAmount = sum of all item amounts（BigDecimal精确比较）
    /**
     * 属性5：合计行总数量和总金额应等于所有明细行之和
     *
     * <p>对于任意结算数据，生成的每份结算单的 totalQuantity 应等于
     * 所有明细行 quantity 之和，totalAmount 应等于所有明细行 amount 之和
     * （BigDecimal精确比较）。</p>
     *
     * <p><b>Validates: Requirements 4.6</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyTotalsShouldEqualSumOfItems(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

        // Then
        for (SettlementGenerateResult result : results) {
            DocumentSettlement settlement = result.getSettlement();
            List<DocumentSettlementItem> items = result.getItems();

            // 验证总数量
            int sumQuantity = items.stream()
                    .mapToInt(DocumentSettlementItem::getQuantity)
                    .sum();
            assertEquals(sumQuantity, settlement.getTotalQuantity().intValue(),
                    "结算单 " + settlement.getDocumentNo()
                            + " 合计数量 " + settlement.getTotalQuantity()
                            + " 不等于明细数量之和 " + sumQuantity);

            // 验证总金额（BigDecimal精确比较）
            BigDecimal sumAmount = items.stream()
                    .map(DocumentSettlementItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sumAmount.compareTo(settlement.getTotalAmount()),
                    "结算单 " + settlement.getDocumentNo()
                            + " 合计金额 " + settlement.getTotalAmount()
                            + " 不等于明细金额之和 " + sumAmount);
        }
    }

    // Feature: fba-document-generation, Property 6: 单据生成幂等性（结算单部分）
    // 相同输入多次调用输出完全一致
    /**
     * 属性6：相同输入多次调用generate方法，输出应完全一致
     *
     * <p>对于任意相同的结算数据输入，多次调用 SettlementGenerator.generate 方法，
     * 输出的结算日、编号、明细排序、数量、金额应完全一致。</p>
     *
     * <p><b>Validates: Requirements 4.10</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertySameInputShouldProduceIdenticalOutput(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // When - 调用两次
        List<SettlementGenerateResult> results1 = SettlementGenerator.generate(input, 1);
        List<SettlementGenerateResult> results2 = SettlementGenerator.generate(input, 1);

        // Then - 输出应完全一致
        assertEquals(results1.size(), results2.size(),
                "两次调用生成的结算单数量不一致");

        for (int i = 0; i < results1.size(); i++) {
            DocumentSettlement s1 = results1.get(i).getSettlement();
            DocumentSettlement s2 = results2.get(i).getSettlement();

            assertEquals(s1.getDocumentNo(), s2.getDocumentNo(),
                    "第 " + (i + 1) + " 份结算单编号不一致");
            assertEquals(s1.getSettlementDate(), s2.getSettlementDate(),
                    "第 " + (i + 1) + " 份结算单结算日不一致");
            assertEquals(s1.getSiteCode(), s2.getSiteCode(),
                    "第 " + (i + 1) + " 份结算单站点代码不一致");
            assertEquals(s1.getSiteSequence(), s2.getSiteSequence(),
                    "第 " + (i + 1) + " 份结算单站点序号不一致");
            assertEquals(s1.getTotalQuantity(), s2.getTotalQuantity(),
                    "第 " + (i + 1) + " 份结算单总数量不一致");
            assertEquals(0, s1.getTotalAmount().compareTo(s2.getTotalAmount()),
                    "第 " + (i + 1) + " 份结算单总金额不一致");

            // 验证明细一致性
            List<DocumentSettlementItem> items1 = results1.get(i).getItems();
            List<DocumentSettlementItem> items2 = results2.get(i).getItems();
            assertEquals(items1.size(), items2.size(),
                    "第 " + (i + 1) + " 份结算单明细数量不一致");

            for (int j = 0; j < items1.size(); j++) {
                DocumentSettlementItem it1 = items1.get(j);
                DocumentSettlementItem it2 = items2.get(j);

                assertEquals(it1.getMsku(), it2.getMsku(),
                        "第 " + (i + 1) + " 份结算单第 " + (j + 1) + " 行MSKU不一致");
                assertEquals(it1.getQuantity(), it2.getQuantity(),
                        "第 " + (i + 1) + " 份结算单第 " + (j + 1) + " 行数量不一致");
                assertEquals(0, it1.getUnitPrice().compareTo(it2.getUnitPrice()),
                        "第 " + (i + 1) + " 份结算单第 " + (j + 1) + " 行单价不一致");
                assertEquals(0, it1.getAmount().compareTo(it2.getAmount()),
                        "第 " + (i + 1) + " 份结算单第 " + (j + 1) + " 行金额不一致");
            }
        }
    }
}
