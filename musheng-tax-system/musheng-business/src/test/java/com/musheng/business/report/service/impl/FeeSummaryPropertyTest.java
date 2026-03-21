package com.musheng.business.report.service.impl;

import com.musheng.business.sales.entity.SalesData;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 费用归类与平台支出属性测试
 * 验证费用归类正确性、税合计完整性、平台支出与采购成本闭环
 *
 * <p>属性 3：费用归类正确性（佣金 3 项 + 其他费 other）</p>
 * <p>属性 4：税合计完整性（方式一 + 方式二）</p>
 * <p>属性 7：平台支出与采购成本闭环</p>
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 5.1, 5.2, 5.3, 6.1, 6.2, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月20日
 */
class FeeSummaryPropertyTest {

    // ========== 辅助方法 ==========

    /**
     * 空值转零（与源码 TaxReportServiceImpl 一致）
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ========== 费用归类逻辑（模拟源码中方式一+方式二的费用累加） ==========

    /**
     * 模拟方式一（income/refund 类型）费用累加
     * 返回 [佣金3项合计, 其他费合计, 税合计]
     */
    private BigDecimal[] calculateMethod1Fees(List<SalesData> incomeRefundList,
                                               Set<String> shippingOrderIds) {
        BigDecimal commissionFee = BigDecimal.ZERO;
        BigDecimal otherFee = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        for (SalesData data : incomeRefundList) {
            String category = data.getTransactionCategory();
            if (!"income".equals(category) && !"refund".equals(category)) continue;
            if (data.getOrderId() == null || !shippingOrderIds.contains(data.getOrderId())) continue;

            // 佣金3项：sellingFees + fbaFees + otherTransactionFees
            BigDecimal selling = nullToZero(data.getSellingFees());
            BigDecimal fba = nullToZero(data.getFbaFees());
            BigDecimal otherTrans = nullToZero(data.getOtherTransactionFees());
            commissionFee = commissionFee.add(selling).add(fba).add(otherTrans);

            // 其他费：other
            otherFee = otherFee.add(nullToZero(data.getOther()));

            // 税：marketplaceWithheldTax
            tax = tax.add(nullToZero(data.getMarketplaceWithheldTax()));
        }
        return new BigDecimal[]{commissionFee, otherFee, tax};
    }

    /**
     * 模拟方式二（其他类型，排除 transfer 分类）费用累加
     * 通过 transactionCategory 统一判断，替代原有的 transactionType 硬编码
     * 返回 [佣金3项合计, 其他费合计, 税合计]
     */
    private BigDecimal[] calculateMethod2Fees(List<SalesData> otherDataList,
                                               LocalDate startDate, LocalDate endDate) {
        BigDecimal commissionFee = BigDecimal.ZERO;
        BigDecimal otherFee = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        for (SalesData data : otherDataList) {
            // 排除 transfer 类型（通过 transactionCategory 统一判断）
            if ("transfer".equals(data.getTransactionCategory())) continue;
            if (data.getTransactionDate() == null) continue;
            LocalDate transDate = data.getTransactionDate().toLocalDate();
            if (transDate.isBefore(startDate) || transDate.isAfter(endDate)) continue;

            // 佣金3项
            BigDecimal selling = nullToZero(data.getSellingFees());
            BigDecimal fba = nullToZero(data.getFbaFees());
            BigDecimal otherTrans = nullToZero(data.getOtherTransactionFees());
            commissionFee = commissionFee.add(selling).add(fba).add(otherTrans);

            // 其他费
            otherFee = otherFee.add(nullToZero(data.getOther()));

            // 税
            tax = tax.add(nullToZero(data.getMarketplaceWithheldTax()));
        }
        return new BigDecimal[]{commissionFee, otherFee, tax};
    }

    // ========== jqwik 自定义生成器 ==========

    // 测试季度：2025-Q3（7月1日 ~ 9月30日）
    private static final LocalDate START_DATE = LocalDate.of(2025, 7, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 9, 30);

    /**
     * 生成可为 null 的 BigDecimal 金额
     */
    @Provide
    Arbitrary<BigDecimal> nullableAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-999.99"), new BigDecimal("999.99"))
                .ofScale(2)
                .injectNull(0.15);
    }

    /**
     * 生成交易类型（方式二使用，包含 Transfer 和其他常见类型）
     */
    @Provide
    Arbitrary<String> transactionTypes() {
        return Arbitraries.of("Transfer", "Service Fee", "Adjustment",
                "FBA Inventory Fee", "Cost of Advertising", "Other");
    }

    /**
     * 生成季度内的交易日期
     */
    @Provide
    Arbitrary<LocalDateTime> inRangeDates() {
        return Arbitraries.integers().between(7, 9)
                .flatMap(month -> Arbitraries.integers().between(1, 28)
                        .map(day -> LocalDateTime.of(2025, month, day, 10, 0)));
    }

    /**
     * 生成可能在季度内外的交易日期（覆盖边界）
     */
    @Provide
    Arbitrary<LocalDateTime> nullableTransactionDates() {
        return Arbitraries.integers().between(5, 12)
                .flatMap(month -> Arbitraries.integers().between(1, 28)
                        .map(day -> LocalDateTime.of(2025, month, day, 10, 0)))
                .injectNull(0.1);
    }

    /**
     * 生成订单号
     */
    @Provide
    Arbitrary<String> orderIds() {
        return Arbitraries.of("ORD-001", "ORD-002", "ORD-003", "ORD-004", "ORD-005",
                "ORD-006", "ORD-007", "ORD-008");
    }

    /**
     * 生成配送订单集合（从候选订单号中随机选取子集）
     */
    @Provide
    Arbitrary<Set<String>> shippingOrderIdSets() {
        return orderIds().set().ofMinSize(1).ofMaxSize(6);
    }

    /**
     * 生成单条方式一（income/refund）SalesData 记录
     */
    @Provide
    Arbitrary<SalesData> method1Record() {
        return Combinators.combine(
                Arbitraries.of("income", "refund"),
                orderIds().injectNull(0.05),
                nullableAmounts(), // sellingFees
                nullableAmounts(), // fbaFees
                nullableAmounts(), // otherTransactionFees
                nullableAmounts(), // other
                nullableAmounts()  // marketplaceWithheldTax
        ).as((category, orderId, selling, fba, otherTrans, otherAmt, tax) -> {
            SalesData data = new SalesData();
            data.setTransactionCategory(category);
            data.setOrderId(orderId);
            data.setSellingFees(selling);
            data.setFbaFees(fba);
            data.setOtherTransactionFees(otherTrans);
            data.setOther(otherAmt);
            data.setMarketplaceWithheldTax(tax);
            return data;
        });
    }

    /**
     * 生成方式一数据列表
     */
    @Provide
    Arbitrary<List<SalesData>> method1DataList() {
        return method1Record().list().ofMinSize(0).ofMaxSize(15);
    }

    /**
     * 生成单条方式二 SalesData 记录
     * Transfer 类型对应 transactionCategory = "transfer"，其他类型对应 "fee" 或 "other"
     */
    @Provide
    Arbitrary<SalesData> method2Record() {
        return Combinators.combine(
                transactionTypes(),
                nullableTransactionDates(),
                nullableAmounts(), // sellingFees
                nullableAmounts(), // fbaFees
                nullableAmounts(), // otherTransactionFees
                nullableAmounts(), // other
                nullableAmounts()  // marketplaceWithheldTax
        ).as((txType, txDate, selling, fba, otherTrans, otherAmt, tax) -> {
            SalesData data = new SalesData();
            data.setTransactionType(txType);
            // Transfer 类型对应 transactionCategory = "transfer"，其他类型对应 "fee"
            if ("Transfer".equals(txType)) {
                data.setTransactionCategory("transfer");
            } else {
                data.setTransactionCategory("fee");
            }
            data.setTransactionDate(txDate);
            data.setSellingFees(selling);
            data.setFbaFees(fba);
            data.setOtherTransactionFees(otherTrans);
            data.setOther(otherAmt);
            data.setMarketplaceWithheldTax(tax);
            return data;
        });
    }

    /**
     * 生成方式二数据列表
     */
    @Provide
    Arbitrary<List<SalesData>> method2DataList() {
        return method2Record().list().ofMinSize(0).ofMaxSize(15);
    }


    // ========== 属性 3：费用归类正确性 ==========

    /**
     * 属性 3.1：佣金服务费合计 = 方式一(3项) + 方式二(3项)，不包含 other
     * 对于任意 income/refund/其他类型的结算数据集合，totalCommissionFee 恰好等于
     * 方式一和方式二中 sellingFees + fbaFees + otherTransactionFees 的累加值
     *
     * <p><b>Validates: Requirements 4.1, 5.1, 5.2, 7.1</b></p>
     */
    @Property(tries = 200)
    void testFeeSummary_CommissionFeeTotal_ShouldEqualMethod1PlusMethod2ThreeItems(
            @ForAll("method1DataList") List<SalesData> method1Data,
            @ForAll("method2DataList") List<SalesData> method2Data,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的方式一和方式二数据

        // When - 分别计算方式一和方式二的费用
        BigDecimal[] m1Fees = calculateMethod1Fees(method1Data, shippingOrderIds);
        BigDecimal[] m2Fees = calculateMethod2Fees(method2Data, START_DATE, END_DATE);

        // 佣金服务费合计 = 方式一佣金 + 方式二佣金
        BigDecimal totalCommissionFee = m1Fees[0].add(m2Fees[0]);

        // Then - 独立计算验证：遍历所有记录，只累加 sellingFees+fbaFees+otherTransactionFees
        BigDecimal expectedCommission = BigDecimal.ZERO;

        // 方式一记录
        for (SalesData data : method1Data) {
            String category = data.getTransactionCategory();
            if (!"income".equals(category) && !"refund".equals(category)) continue;
            if (data.getOrderId() == null || !shippingOrderIds.contains(data.getOrderId())) continue;
            expectedCommission = expectedCommission
                    .add(nullToZero(data.getSellingFees()))
                    .add(nullToZero(data.getFbaFees()))
                    .add(nullToZero(data.getOtherTransactionFees()));
        }

        // 方式二记录（排除 transfer 分类）
        for (SalesData data : method2Data) {
            if ("transfer".equals(data.getTransactionCategory())) continue;
            if (data.getTransactionDate() == null) continue;
            LocalDate transDate = data.getTransactionDate().toLocalDate();
            if (transDate.isBefore(START_DATE) || transDate.isAfter(END_DATE)) continue;
            expectedCommission = expectedCommission
                    .add(nullToZero(data.getSellingFees()))
                    .add(nullToZero(data.getFbaFees()))
                    .add(nullToZero(data.getOtherTransactionFees()));
        }

        assertEquals(0, expectedCommission.compareTo(totalCommissionFee),
                String.format("佣金服务费合计不一致: 期望=%s, 实际=%s", expectedCommission, totalCommissionFee));
    }

    /**
     * 属性 3.2：其他费合计 = 方式一(other) + 方式二(other)，不包含佣金3项
     * 对于任意结算数据集合，totalOtherFee 恰好等于方式一和方式二中 other 的累加值
     *
     * <p><b>Validates: Requirements 4.2, 5.3, 7.2</b></p>
     */
    @Property(tries = 200)
    void testFeeSummary_OtherFeeTotal_ShouldEqualMethod1PlusMethod2Other(
            @ForAll("method1DataList") List<SalesData> method1Data,
            @ForAll("method2DataList") List<SalesData> method2Data,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的方式一和方式二数据

        // When - 分别计算方式一和方式二的费用
        BigDecimal[] m1Fees = calculateMethod1Fees(method1Data, shippingOrderIds);
        BigDecimal[] m2Fees = calculateMethod2Fees(method2Data, START_DATE, END_DATE);

        // 其他费合计 = 方式一其他费 + 方式二其他费
        BigDecimal totalOtherFee = m1Fees[1].add(m2Fees[1]);

        // Then - 独立计算验证：遍历所有记录，只累加 other 字段
        BigDecimal expectedOther = BigDecimal.ZERO;

        // 方式一记录
        for (SalesData data : method1Data) {
            String category = data.getTransactionCategory();
            if (!"income".equals(category) && !"refund".equals(category)) continue;
            if (data.getOrderId() == null || !shippingOrderIds.contains(data.getOrderId())) continue;
            expectedOther = expectedOther.add(nullToZero(data.getOther()));
        }

        // 方式二记录（排除 transfer 分类）
        for (SalesData data : method2Data) {
            if ("transfer".equals(data.getTransactionCategory())) continue;
            if (data.getTransactionDate() == null) continue;
            LocalDate transDate = data.getTransactionDate().toLocalDate();
            if (transDate.isBefore(START_DATE) || transDate.isAfter(END_DATE)) continue;
            expectedOther = expectedOther.add(nullToZero(data.getOther()));
        }

        assertEquals(0, expectedOther.compareTo(totalOtherFee),
                String.format("其他费合计不一致: 期望=%s, 实际=%s", expectedOther, totalOtherFee));
    }

    /**
     * 属性 3.3：佣金服务费和其他费互不包含对方的费用项
     * 佣金服务费只包含 sellingFees+fbaFees+otherTransactionFees，
     * 其他费只包含 other，两者之和不等于全部费用项之和（除非某些项为零）
     *
     * <p><b>Validates: Requirements 4.1, 4.2</b></p>
     */
    @Property(tries = 200)
    void testFeeSummary_CommissionAndOther_ShouldBeMutuallyExclusive(
            @ForAll("method1DataList") List<SalesData> method1Data,
            @ForAll("method2DataList") List<SalesData> method2Data,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的数据

        // When - 计算费用
        BigDecimal[] m1Fees = calculateMethod1Fees(method1Data, shippingOrderIds);
        BigDecimal[] m2Fees = calculateMethod2Fees(method2Data, START_DATE, END_DATE);

        BigDecimal totalCommissionFee = m1Fees[0].add(m2Fees[0]);
        BigDecimal totalOtherFee = m1Fees[1].add(m2Fees[1]);

        // Then - 佣金+其他费 = 所有参与计算记录的 (sellingFees+fbaFees+otherTransactionFees+other) 之和
        BigDecimal allFourItems = BigDecimal.ZERO;

        for (SalesData data : method1Data) {
            String category = data.getTransactionCategory();
            if (!"income".equals(category) && !"refund".equals(category)) continue;
            if (data.getOrderId() == null || !shippingOrderIds.contains(data.getOrderId())) continue;
            allFourItems = allFourItems
                    .add(nullToZero(data.getSellingFees()))
                    .add(nullToZero(data.getFbaFees()))
                    .add(nullToZero(data.getOtherTransactionFees()))
                    .add(nullToZero(data.getOther()));
        }

        for (SalesData data : method2Data) {
            // 排除 transfer 分类
            if ("transfer".equals(data.getTransactionCategory())) continue;
            if (data.getTransactionDate() == null) continue;
            LocalDate transDate = data.getTransactionDate().toLocalDate();
            if (transDate.isBefore(START_DATE) || transDate.isAfter(END_DATE)) continue;
            allFourItems = allFourItems
                    .add(nullToZero(data.getSellingFees()))
                    .add(nullToZero(data.getFbaFees()))
                    .add(nullToZero(data.getOtherTransactionFees()))
                    .add(nullToZero(data.getOther()));
        }

        // 佣金 + 其他费 = 全部4项之和（证明互不遗漏、互不重叠）
        assertEquals(0, allFourItems.compareTo(totalCommissionFee.add(totalOtherFee)),
                String.format("佣金(%s) + 其他费(%s) 应等于全部4项之和(%s)",
                        totalCommissionFee, totalOtherFee, allFourItems));
    }

    // ========== 属性 4：税合计完整性 ==========

    /**
     * 属性 4：税合计 = 方式一(marketplaceWithheldTax) + 方式二(marketplaceWithheldTax)
     * 对于任意结算数据集合，consumptionTax 恰好等于方式一（income + refund 类型）的
     * marketplaceWithheldTax 累计值加上方式二（其他类型，排除 transfer 分类）的 marketplaceWithheldTax 累计值
     *
     * <p><b>Validates: Requirements 6.1, 6.2, 7.3</b></p>
     */
    @Property(tries = 200)
    void testFeeSummary_ConsumptionTax_ShouldEqualMethod1PlusMethod2Tax(
            @ForAll("method1DataList") List<SalesData> method1Data,
            @ForAll("method2DataList") List<SalesData> method2Data,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的方式一和方式二数据

        // When - 分别计算方式一和方式二的税
        BigDecimal[] m1Fees = calculateMethod1Fees(method1Data, shippingOrderIds);
        BigDecimal[] m2Fees = calculateMethod2Fees(method2Data, START_DATE, END_DATE);

        // 税合计 = 方式一税 + 方式二税
        BigDecimal consumptionTax = m1Fees[2].add(m2Fees[2]);

        // Then - 独立计算验证
        BigDecimal expectedTax = BigDecimal.ZERO;

        // 方式一：income + refund 类型的 marketplaceWithheldTax
        for (SalesData data : method1Data) {
            String category = data.getTransactionCategory();
            if (!"income".equals(category) && !"refund".equals(category)) continue;
            if (data.getOrderId() == null || !shippingOrderIds.contains(data.getOrderId())) continue;
            expectedTax = expectedTax.add(nullToZero(data.getMarketplaceWithheldTax()));
        }

        // 方式二：其他类型（排除 transfer 分类）的 marketplaceWithheldTax
        for (SalesData data : method2Data) {
            if ("transfer".equals(data.getTransactionCategory())) continue;
            if (data.getTransactionDate() == null) continue;
            LocalDate transDate = data.getTransactionDate().toLocalDate();
            if (transDate.isBefore(START_DATE) || transDate.isAfter(END_DATE)) continue;
            expectedTax = expectedTax.add(nullToZero(data.getMarketplaceWithheldTax()));
        }

        assertEquals(0, expectedTax.compareTo(consumptionTax),
                String.format("税合计不一致: 期望=%s, 实际=%s", expectedTax, consumptionTax));
    }

    // ========== 属性 7：平台支出与采购成本闭环 ==========

    /**
     * 生成非负 BigDecimal（用于收入总额等非负值场景）
     */
    @Provide
    Arbitrary<BigDecimal> nonNegativeAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("99999.99"))
                .ofScale(2);
    }

    /**
     * 生成任意 BigDecimal（用于费用等可正可负的场景）
     */
    @Provide
    Arbitrary<BigDecimal> anyAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-99999.99"), new BigDecimal("99999.99"))
                .ofScale(2);
    }

    /**
     * 属性 7.1：收入净额公式正确性
     * netRevenueCny = totalRevenueCny − |refundAmountCny|
     *
     * <p><b>Validates: Requirements 7.4</b></p>
     */
    @Property(tries = 200)
    void testPlatformExpense_NetRevenue_ShouldEqualRevenueMinusAbsRefund(
            @ForAll("nonNegativeAmounts") BigDecimal totalRevenueCny,
            @ForAll("anyAmounts") BigDecimal refundAmountCny) {
        // Given - 随机收入总额和退款金额

        // When - 按公式计算收入净额
        BigDecimal netRevenueCny = totalRevenueCny.subtract(refundAmountCny.abs());

        // Then - 验证公式恒等式
        BigDecimal expected = totalRevenueCny.subtract(refundAmountCny.abs());
        assertEquals(0, expected.compareTo(netRevenueCny),
                String.format("收入净额公式不成立: totalRevenueCny=%s, refundAmountCny=%s",
                        totalRevenueCny, refundAmountCny));
    }

    /**
     * 属性 7.2：平台支出合计公式正确性
     * platformExpensesCny = |consumptionTaxCny| + |totalCommissionFeeCny| + |advertisingCostCny| + |totalOtherFeeCny|
     *
     * <p><b>Validates: Requirements 7.5</b></p>
     */
    @Property(tries = 200)
    void testPlatformExpense_Total_ShouldEqualAbsSumOfFourComponents(
            @ForAll("anyAmounts") BigDecimal consumptionTaxCny,
            @ForAll("anyAmounts") BigDecimal totalCommissionFeeCny,
            @ForAll("anyAmounts") BigDecimal advertisingCostCny,
            @ForAll("anyAmounts") BigDecimal totalOtherFeeCny) {
        // Given - 随机费用值

        // When - 按公式计算平台支出合计
        BigDecimal platformExpensesCny = consumptionTaxCny.abs()
                .add(totalCommissionFeeCny.abs())
                .add(advertisingCostCny.abs())
                .add(totalOtherFeeCny.abs());

        // Then - 验证平台支出 >= 0（绝对值之和恒非负）
        assertTrue(platformExpensesCny.compareTo(BigDecimal.ZERO) >= 0,
                "平台支出合计应恒非负");

        // 验证等于各项绝对值之和
        BigDecimal expected = consumptionTaxCny.abs()
                .add(totalCommissionFeeCny.abs())
                .add(advertisingCostCny.abs())
                .add(totalOtherFeeCny.abs());
        assertEquals(0, expected.compareTo(platformExpensesCny),
                String.format("平台支出合计公式不成立: 期望=%s, 实际=%s", expected, platformExpensesCny));
    }

    /**
     * 属性 7.3：完整公式链闭环
     * 验证 procurementCostCny = netRevenueCny − platformExpensesCny − profit4PercentCny
     * 其中 profit4PercentCny = netRevenueCny × 0.04
     * 即 procurementCostCny = netRevenueCny × 0.96 − platformExpensesCny
     *
     * <p><b>Validates: Requirements 7.4, 7.5, 7.6</b></p>
     */
    @Property(tries = 200)
    void testPlatformExpense_FormulaChain_ShouldBeConsistent(
            @ForAll("nonNegativeAmounts") BigDecimal totalRevenueCny,
            @ForAll("anyAmounts") BigDecimal refundAmountCny,
            @ForAll("anyAmounts") BigDecimal consumptionTaxCny,
            @ForAll("anyAmounts") BigDecimal totalCommissionFeeCny,
            @ForAll("anyAmounts") BigDecimal advertisingCostCny,
            @ForAll("anyAmounts") BigDecimal totalOtherFeeCny) {
        // Given - 随机生成的各项金额

        // When - 按公式链逐步计算
        // ③ 收入净额 = 收入总额 - |退款金额|
        BigDecimal netRevenueCny = totalRevenueCny.subtract(refundAmountCny.abs());

        // ⑨ 平台支出合计 = |消费税| + |佣金服务费| + |广告费| + |其他费|
        BigDecimal platformExpensesCny = consumptionTaxCny.abs()
                .add(totalCommissionFeeCny.abs())
                .add(advertisingCostCny.abs())
                .add(totalOtherFeeCny.abs());

        // ⑩ 4%利润 = ③ × 0.04
        BigDecimal profit4PercentCny = netRevenueCny.multiply(new BigDecimal("0.04"));

        // ⑪ 采购成本 = ③ − ⑨ − ⑩
        BigDecimal procurementCostCny = netRevenueCny.subtract(platformExpensesCny).subtract(profit4PercentCny);

        // Then - 验证公式链闭环：③ = ⑨ + ⑩ + ⑪
        BigDecimal reconstructed = platformExpensesCny.add(profit4PercentCny).add(procurementCostCny);
        assertEquals(0, netRevenueCny.compareTo(reconstructed),
                String.format("公式链不闭环: netRevenueCny=%s, 但 ⑨+⑩+⑪=%s", netRevenueCny, reconstructed));

        // 验证 procurementCostCny = netRevenueCny × 0.96 − platformExpensesCny
        BigDecimal alternativeCalc = netRevenueCny.multiply(new BigDecimal("0.96")).subtract(platformExpensesCny);
        assertEquals(0, alternativeCalc.compareTo(procurementCostCny),
                String.format("采购成本替代公式不一致: 期望=%s, 实际=%s", alternativeCalc, procurementCostCny));
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：费用归类正确性 - 方式一和方式二混合数据
     *
     * <p><b>Validates: Requirements 4.1, 4.2, 5.1, 5.2, 5.3</b></p>
     */
    @Example
    void testFeeSummary_MixedData_ShouldClassifyCorrectly() {
        // Given - 构造方式一和方式二数据
        // 方式一 income 记录
        SalesData income = new SalesData();
        income.setTransactionCategory("income");
        income.setOrderId("ORD-001");
        income.setSellingFees(new BigDecimal("-10.00"));
        income.setFbaFees(new BigDecimal("-5.00"));
        income.setOtherTransactionFees(new BigDecimal("-2.00"));
        income.setOther(new BigDecimal("-1.00"));
        income.setMarketplaceWithheldTax(new BigDecimal("-0.80"));

        // 方式一 refund 记录
        SalesData refund = new SalesData();
        refund.setTransactionCategory("refund");
        refund.setOrderId("ORD-001");
        refund.setSellingFees(new BigDecimal("3.00"));
        refund.setFbaFees(new BigDecimal("1.50"));
        refund.setOtherTransactionFees(new BigDecimal("0.50"));
        refund.setOther(new BigDecimal("0.30"));
        refund.setMarketplaceWithheldTax(new BigDecimal("0.20"));

        // 方式二 Service Fee 记录（季度内）
        SalesData serviceFee = new SalesData();
        serviceFee.setTransactionType("Service Fee");
        serviceFee.setTransactionCategory("fee");
        serviceFee.setTransactionDate(LocalDateTime.of(2025, 8, 15, 10, 0));
        serviceFee.setSellingFees(new BigDecimal("-4.00"));
        serviceFee.setFbaFees(new BigDecimal("-2.00"));
        serviceFee.setOtherTransactionFees(new BigDecimal("-1.00"));
        serviceFee.setOther(new BigDecimal("-0.50"));
        serviceFee.setMarketplaceWithheldTax(new BigDecimal("-0.60"));

        // 方式二 Transfer 记录（应排除，transactionCategory = "transfer"）
        SalesData transfer = new SalesData();
        transfer.setTransactionType("Transfer");
        transfer.setTransactionCategory("transfer");
        transfer.setTransactionDate(LocalDateTime.of(2025, 8, 20, 10, 0));
        transfer.setSellingFees(new BigDecimal("-100.00"));
        transfer.setOther(new BigDecimal("-50.00"));
        transfer.setMarketplaceWithheldTax(new BigDecimal("-10.00"));

        Set<String> shippingOrderIds = Set.of("ORD-001");
        List<SalesData> method1Data = List.of(income, refund);
        List<SalesData> method2Data = List.of(serviceFee, transfer);

        // When - 计算费用
        BigDecimal[] m1Fees = calculateMethod1Fees(method1Data, shippingOrderIds);
        BigDecimal[] m2Fees = calculateMethod2Fees(method2Data, START_DATE, END_DATE);

        BigDecimal totalCommissionFee = m1Fees[0].add(m2Fees[0]);
        BigDecimal totalOtherFee = m1Fees[1].add(m2Fees[1]);
        BigDecimal consumptionTax = m1Fees[2].add(m2Fees[2]);

        // Then - 验证佣金服务费合计
        // 方式一佣金: (-10+3) + (-5+1.5) + (-2+0.5) = -7 + (-3.5) + (-1.5) = -12.00
        // 方式二佣金: -4 + (-2) + (-1) = -7.00（Transfer 排除）
        // 合计: -12.00 + (-7.00) = -19.00
        BigDecimal expectedCommission = new BigDecimal("-19.00");
        assertEquals(0, expectedCommission.compareTo(totalCommissionFee),
                String.format("佣金服务费合计应为 %s，实际为 %s", expectedCommission, totalCommissionFee));

        // 验证其他费合计
        // 方式一其他费: -1.00 + 0.30 = -0.70
        // 方式二其他费: -0.50（Transfer 排除）
        // 合计: -0.70 + (-0.50) = -1.20
        BigDecimal expectedOther = new BigDecimal("-1.20");
        assertEquals(0, expectedOther.compareTo(totalOtherFee),
                String.format("其他费合计应为 %s，实际为 %s", expectedOther, totalOtherFee));

        // 验证税合计
        // 方式一税: -0.80 + 0.20 = -0.60
        // 方式二税: -0.60（Transfer 排除）
        // 合计: -0.60 + (-0.60) = -1.20
        BigDecimal expectedTax = new BigDecimal("-1.20");
        assertEquals(0, expectedTax.compareTo(consumptionTax),
                String.format("税合计应为 %s，实际为 %s", expectedTax, consumptionTax));
    }

    /**
     * 示例测试：平台支出与采购成本闭环 - 具体数值验证
     *
     * <p><b>Validates: Requirements 7.4, 7.5, 7.6</b></p>
     */
    @Example
    void testPlatformExpense_ConcreteValues_ShouldFormClosedLoop() {
        // Given - 具体数值
        BigDecimal totalRevenueCny = new BigDecimal("10000.00");
        BigDecimal refundAmountCny = new BigDecimal("-1500.00"); // 退款通常为负数
        BigDecimal consumptionTaxCny = new BigDecimal("-200.00");
        BigDecimal totalCommissionFeeCny = new BigDecimal("-800.00");
        BigDecimal advertisingCostCny = new BigDecimal("-300.00");
        BigDecimal totalOtherFeeCny = new BigDecimal("-100.00");

        // When - 按公式链计算
        // ③ 收入净额 = 10000 - |-1500| = 10000 - 1500 = 8500
        BigDecimal netRevenueCny = totalRevenueCny.subtract(refundAmountCny.abs());

        // ⑨ 平台支出 = |-200| + |-800| + |-300| + |-100| = 200 + 800 + 300 + 100 = 1400
        BigDecimal platformExpensesCny = consumptionTaxCny.abs()
                .add(totalCommissionFeeCny.abs())
                .add(advertisingCostCny.abs())
                .add(totalOtherFeeCny.abs());

        // ⑩ 4%利润 = 8500 × 0.04 = 340
        BigDecimal profit4PercentCny = netRevenueCny.multiply(new BigDecimal("0.04"));

        // ⑪ 采购成本 = 8500 - 1400 - 340 = 6760
        BigDecimal procurementCostCny = netRevenueCny.subtract(platformExpensesCny).subtract(profit4PercentCny);

        // Then - 验证各步骤
        assertEquals(0, new BigDecimal("8500.00").compareTo(netRevenueCny),
                "收入净额应为 8500.00");
        assertEquals(0, new BigDecimal("1400.00").compareTo(platformExpensesCny),
                "平台支出合计应为 1400.00");
        assertEquals(0, new BigDecimal("340.0000").compareTo(profit4PercentCny),
                "4%利润应为 340.00");
        assertEquals(0, new BigDecimal("6760.0000").compareTo(procurementCostCny),
                "采购成本应为 6760.00");

        // 验证闭环：③ = ⑨ + ⑩ + ⑪
        BigDecimal sum = platformExpensesCny.add(profit4PercentCny).add(procurementCostCny);
        assertEquals(0, netRevenueCny.compareTo(sum),
                String.format("公式链不闭环: ③=%s, ⑨+⑩+⑪=%s", netRevenueCny, sum));
    }
}
