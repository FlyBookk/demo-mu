package com.musheng.business.document.generator;

import com.musheng.business.common.config.DocumentPartyProperties;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentInvItem;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.enums.SiteCode;
import com.musheng.business.document.utils.WorkingDayCalculator;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InvGenerator 属性测试
 *
 * <p>使用 jqwik 框架验证INV发票生成器的通用正确性属性。
 * 覆盖属性13（结算单与INV数据一致性）、属性5（合计行invariant）、
 * 属性6（幂等性）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class InvGeneratorProperties {

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
     * 生成4站点结算数据输入（用于端到端测试）
     *
     * <p>每个站点生成1~3条MSKU数据，通过 SettlementGenerator 生成结算单，
     * 再传给 InvGenerator 生成INV，实现端到端验证。</p>
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

    // ==================== 属性 13：结算单与INV数据一致性 ====================

    // Feature: fba-document-generation, Property 13: 结算单与INV数据一致性
    // MSKU列表、数量、单价、金额与结算单完全一致
    // INV日期 = nextWorkingDay(结算日)
    /**
     * 属性13-1：INV的MSKU列表、数量、单价、金额应与结算单完全一致
     *
     * <p>对于任意结算数据，先生成结算单再生成INV后，
     * 每份INV的明细应与对应结算单的明细完全一致（MSKU、数量、单价、金额）。</p>
     *
     * <p><b>Validates: Requirements 5.1, 5.2, 9.1, 9.3</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyInvDataShouldMatchSettlement(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // Given - 生成结算单
        List<SettlementGenerateResult> settlementResults = SettlementGenerator.generate(input, 1);

        // When - 基于结算单生成INV
        List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, createTestParty());

        // Then - INV数量应与结算单数量一致
        assertEquals(settlementResults.size(), invResults.size(),
                "INV数量应与结算单数量一致");

        for (int i = 0; i < settlementResults.size(); i++) {
            SettlementGenerateResult sResult = settlementResults.get(i);
            InvGenerateResult invResult = invResults.get(i);

            List<DocumentSettlementItem> sItems = sResult.getItems();
            List<DocumentInvItem> invItems = invResult.getItems();

            // 明细数量一致
            assertEquals(sItems.size(), invItems.size(),
                    "第 " + (i + 1) + " 份INV明细数量应与结算单一致");

            // 逐行比较MSKU、数量、单价、金额
            for (int j = 0; j < sItems.size(); j++) {
                DocumentSettlementItem sItem = sItems.get(j);
                DocumentInvItem invItem = invItems.get(j);

                assertEquals(sItem.getMsku(), invItem.getMsku(),
                        "第 " + (i + 1) + " 份第 " + (j + 1) + " 行MSKU不一致");
                assertEquals(sItem.getQuantity(), invItem.getQuantity(),
                        "第 " + (i + 1) + " 份第 " + (j + 1) + " 行数量不一致");
                assertEquals(0, sItem.getUnitPrice().compareTo(invItem.getUnitPrice()),
                        "第 " + (i + 1) + " 份第 " + (j + 1) + " 行单价不一致");
                assertEquals(0, sItem.getAmount().compareTo(invItem.getAmount()),
                        "第 " + (i + 1) + " 份第 " + (j + 1) + " 行金额不一致");
            }
        }
    }

    // Feature: fba-document-generation, Property 13: 结算单与INV数据一致性
    // INV日期 = nextWorkingDay(结算日)
    /**
     * 属性13-2：INV日期应等于结算日的下一个工作日
     *
     * <p>对于任意结算数据，生成的每份INV的日期应等于
     * 对应结算单结算日的下一个工作日（nextWorkingDay）。</p>
     *
     * <p><b>Validates: Requirements 5.1, 5.2, 9.1, 9.3</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyInvDateShouldBeNextWorkingDayAfterSettlementDate(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // Given - 生成结算单
        List<SettlementGenerateResult> settlementResults = SettlementGenerator.generate(input, 1);

        // When - 基于结算单生成INV
        List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, createTestParty());

        // Then - 每份INV日期 = nextWorkingDay(结算日)
        for (int i = 0; i < settlementResults.size(); i++) {
            DocumentSettlement settlement = settlementResults.get(i).getSettlement();
            DocumentInv inv = invResults.get(i).getInv();

            LocalDate expectedInvDate = WorkingDayCalculator.nextWorkingDay(settlement.getSettlementDate());
            assertEquals(expectedInvDate, inv.getInvDate(),
                    "第 " + (i + 1) + " 份INV日期应为结算日 "
                            + settlement.getSettlementDate() + " 的下一个工作日 "
                            + expectedInvDate + "，实际为 " + inv.getInvDate());
        }
    }

    // ==================== 属性 5：合计行数量/金额 invariant（INV部分） ====================

    // Feature: fba-document-generation, Property 5: 合计行数量/金额 invariant（INV部分）
    // totalQuantity = sum of all item quantities
    // totalAmount = sum of all item amounts（BigDecimal精确比较）
    /**
     * 属性5：INV合计行总数量和总金额应等于所有明细行之和
     *
     * <p>对于任意结算数据，生成的每份INV的 totalQuantity 应等于
     * 所有明细行 quantity 之和，totalAmount 应等于所有明细行 amount 之和
     * （BigDecimal精确比较）。</p>
     *
     * <p><b>Validates: Requirements 5.6</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyInvTotalsShouldEqualSumOfItems(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // Given - 生成结算单
        List<SettlementGenerateResult> settlementResults = SettlementGenerator.generate(input, 1);

        // When - 基于结算单生成INV
        List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, createTestParty());

        // Then
        for (InvGenerateResult invResult : invResults) {
            DocumentInv inv = invResult.getInv();
            List<DocumentInvItem> items = invResult.getItems();

            // 验证总数量
            int sumQuantity = items.stream()
                    .mapToInt(DocumentInvItem::getQuantity)
                    .sum();
            assertEquals(sumQuantity, inv.getTotalQuantity().intValue(),
                    "INV " + inv.getDocumentNo()
                            + " 合计数量 " + inv.getTotalQuantity()
                            + " 不等于明细数量之和 " + sumQuantity);

            // 验证总金额（BigDecimal精确比较）
            BigDecimal sumAmount = items.stream()
                    .map(DocumentInvItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sumAmount.compareTo(inv.getTotalAmount()),
                    "INV " + inv.getDocumentNo()
                            + " 合计金额 " + inv.getTotalAmount()
                            + " 不等于明细金额之和 " + sumAmount);
        }
    }

    // ==================== 属性 6：单据生成幂等性（INV部分） ====================

    // Feature: fba-document-generation, Property 6: 单据生成幂等性（INV部分）
    // 相同输入多次调用输出完全一致
    /**
     * 属性6：相同输入多次调用generate方法，输出应完全一致
     *
     * <p>对于任意相同的结算数据输入，多次调用 InvGenerator.generate 方法，
     * 输出的INV日期、编号、明细排序、数量、金额应完全一致。</p>
     *
     * <p><b>Validates: Requirements 5.10</b></p>
     *
     * @param input 随机生成的4站点结算数据
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void propertyInvSameInputShouldProduceIdenticalOutput(
            @ForAll("fourSiteInputs") SettlementInput input) {
        // Given - 生成结算单（同一份输入）
        List<SettlementGenerateResult> settlementResults = SettlementGenerator.generate(input, 1);

        // When - 调用两次INV生成
        List<InvGenerateResult> results1 = InvGenerator.generate(settlementResults, 1, createTestParty());
        List<InvGenerateResult> results2 = InvGenerator.generate(settlementResults, 1, createTestParty());

        // Then - 输出应完全一致
        assertEquals(results1.size(), results2.size(),
                "两次调用生成的INV数量不一致");

        for (int i = 0; i < results1.size(); i++) {
            DocumentInv inv1 = results1.get(i).getInv();
            DocumentInv inv2 = results2.get(i).getInv();

            assertEquals(inv1.getDocumentNo(), inv2.getDocumentNo(),
                    "第 " + (i + 1) + " 份INV编号不一致");
            assertEquals(inv1.getInvDate(), inv2.getInvDate(),
                    "第 " + (i + 1) + " 份INV日期不一致");
            assertEquals(inv1.getSiteCode(), inv2.getSiteCode(),
                    "第 " + (i + 1) + " 份INV站点代码不一致");
            assertEquals(inv1.getSiteSequence(), inv2.getSiteSequence(),
                    "第 " + (i + 1) + " 份INV站点序号不一致");
            assertEquals(inv1.getTotalQuantity(), inv2.getTotalQuantity(),
                    "第 " + (i + 1) + " 份INV总数量不一致");
            assertEquals(0, inv1.getTotalAmount().compareTo(inv2.getTotalAmount()),
                    "第 " + (i + 1) + " 份INV总金额不一致");

            // 验证明细一致性
            List<DocumentInvItem> items1 = results1.get(i).getItems();
            List<DocumentInvItem> items2 = results2.get(i).getItems();
            assertEquals(items1.size(), items2.size(),
                    "第 " + (i + 1) + " 份INV明细数量不一致");

            for (int j = 0; j < items1.size(); j++) {
                DocumentInvItem it1 = items1.get(j);
                DocumentInvItem it2 = items2.get(j);

                assertEquals(it1.getMsku(), it2.getMsku(),
                        "第 " + (i + 1) + " 份INV第 " + (j + 1) + " 行MSKU不一致");
                assertEquals(it1.getQuantity(), it2.getQuantity(),
                        "第 " + (i + 1) + " 份INV第 " + (j + 1) + " 行数量不一致");
                assertEquals(0, it1.getUnitPrice().compareTo(it2.getUnitPrice()),
                        "第 " + (i + 1) + " 份INV第 " + (j + 1) + " 行单价不一致");
                assertEquals(0, it1.getAmount().compareTo(it2.getAmount()),
                        "第 " + (i + 1) + " 份INV第 " + (j + 1) + " 行金额不一致");
            }
        }
    }

    private DocumentPartyProperties createTestParty() {
        DocumentPartyProperties party = new DocumentPartyProperties();
        party.setSellerName("Hong Kong Andeo Group Limited");
        party.setSellerAddress("Hong Kong");
        party.setSellerPhone("00852-54682464");
        party.setBuyerName("Dongguan Musheng Trade Co., Ltd.");
        party.setBuyerAddress("Dongguan, Guangdong, China");
        party.setBuyerPhone("18575382420");
        party.setBankAccountName("Hong Kong Andeo Group Limited");
        party.setBankAccountNumber("012-878-0-001234-5");
        party.setBankName("Bank of China (Hong Kong) Limited");
        party.setBankAddress("Bank of China Tower, 1 Garden Road, Central, Hong Kong");
        party.setSwiftCode("BKCHHKHH");
        return party;
    }
}
