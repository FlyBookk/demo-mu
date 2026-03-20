package com.musheng.business.report.service.impl;

import com.musheng.business.report.dto.TaxReportSummary;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * hasAnyTaxData 属性测试
 * 验证判断站点+季度是否有有效数据的正确性
 *
 * <p>属性 8：对于任意 TaxReportSummary 对象，hasAnyTaxData 返回 true 当且仅当
 * totalRevenueCny、totalRevenue、refundCount（>0）、refundAmountCny、
 * totalCommissionFeeCny、consumptionTaxCny、advertisingCostCny、
 * platformExpensesCny、profit4PercentCny 中至少有一个为非零值。</p>
 *
 * <p><b>Validates: Requirements 11.1, 11.2, 11.3</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月21日
 */
class HasAnyTaxDataPropertyTest {

    // ========== 反射工具方法 ==========

    /**
     * 通过反射调用 private static hasAnyTaxData 方法
     *
     * @param summary 报税汇总数据
     * @return 是否有有效数据
     */
    private boolean invokeHasAnyTaxData(TaxReportSummary summary) throws Exception {
        Method method = TaxReportServiceImpl.class.getDeclaredMethod("hasAnyTaxData", TaxReportSummary.class);
        method.setAccessible(true);
        // hasAnyTaxData 是 static 方法，invoke 第一个参数传 null
        return (boolean) method.invoke(null, summary);
    }

    /**
     * 创建所有核心字段为零/null 的 TaxReportSummary（基线对象）
     */
    private TaxReportSummary createZeroSummary() {
        TaxReportSummary s = new TaxReportSummary();
        s.setTotalRevenueCny(BigDecimal.ZERO);
        s.setTotalRevenue(BigDecimal.ZERO);
        s.setRefundCount(0);
        s.setRefundAmountCny(BigDecimal.ZERO);
        s.setTotalCommissionFeeCny(BigDecimal.ZERO);
        s.setConsumptionTaxCny(BigDecimal.ZERO);
        s.setAdvertisingCostCny(BigDecimal.ZERO);
        s.setPlatformExpensesCny(BigDecimal.ZERO);
        s.setProfit4PercentCny(BigDecimal.ZERO);
        return s;
    }

    /**
     * 独立计算 hasAnyTaxData 的期望结果（作为测试预言机）
     * 与源码逻辑一致：任一核心字段非零则返回 true
     */
    private boolean expectedHasAnyTaxData(TaxReportSummary s) {
        return isNonZero(s.getTotalRevenueCny())
                || isNonZero(s.getTotalRevenue())
                || (s.getRefundCount() != null && s.getRefundCount() > 0)
                || isNonZero(s.getRefundAmountCny())
                || isNonZero(s.getTotalCommissionFeeCny())
                || isNonZero(s.getConsumptionTaxCny())
                || isNonZero(s.getAdvertisingCostCny())
                || isNonZero(s.getPlatformExpensesCny())
                || isNonZero(s.getProfit4PercentCny());
    }

    private boolean isNonZero(BigDecimal b) {
        return b != null && b.compareTo(BigDecimal.ZERO) != 0;
    }

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成可为 null 的 BigDecimal（模拟字段可能为 null 的情况）
     */
    @Provide
    Arbitrary<BigDecimal> nullableBigDecimal() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-9999.99"), new BigDecimal("9999.99"))
                .ofScale(2)
                .injectNull(0.2);
    }

    /**
     * 生成可为 null 的 Integer（模拟 refundCount 字段）
     */
    @Provide
    Arbitrary<Integer> nullableRefundCount() {
        return Arbitraries.integers().between(-5, 100).injectNull(0.2);
    }

    /**
     * 生成非零 BigDecimal（用于属性 8b 测试）
     */
    @Provide
    Arbitrary<BigDecimal> nonZeroBigDecimal() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-9999.99"), new BigDecimal("9999.99"))
                .ofScale(2)
                .filter(bd -> bd.compareTo(BigDecimal.ZERO) != 0);
    }

    /**
     * 生成随机 TaxReportSummary（所有核心字段随机值）
     */
    @Provide
    Arbitrary<TaxReportSummary> randomSummary() {
        Arbitrary<BigDecimal> amounts = nullableBigDecimal();
        Arbitrary<Integer> counts = nullableRefundCount();

        return Combinators.combine(
                amounts, amounts, counts, amounts,
                amounts, amounts, amounts, amounts
        ).as((totalRevenueCny, totalRevenue, refundCount, refundAmountCny,
              totalCommissionFeeCny, consumptionTaxCny, advertisingCostCny, platformExpensesCny) -> {
            TaxReportSummary s = new TaxReportSummary();
            s.setTotalRevenueCny(totalRevenueCny);
            s.setTotalRevenue(totalRevenue);
            s.setRefundCount(refundCount);
            s.setRefundAmountCny(refundAmountCny);
            s.setTotalCommissionFeeCny(totalCommissionFeeCny);
            s.setConsumptionTaxCny(consumptionTaxCny);
            s.setAdvertisingCostCny(advertisingCostCny);
            s.setPlatformExpensesCny(platformExpensesCny);
            // profit4PercentCny 需要单独设置（超过 8 参数限制）
            s.setProfit4PercentCny(BigDecimal.ZERO);
            return s;
        }).flatMap(s -> nullableBigDecimal().map(profit -> {
            s.setProfit4PercentCny(profit);
            return s;
        }));
    }

    /**
     * 核心 BigDecimal 字段的 setter 列表（用于属性 8b 逐一设置非零值）
     */
    private static final List<BiConsumer<TaxReportSummary, BigDecimal>> BIG_DECIMAL_SETTERS = List.of(
            TaxReportSummary::setTotalRevenueCny,
            TaxReportSummary::setTotalRevenue,
            TaxReportSummary::setRefundAmountCny,
            TaxReportSummary::setTotalCommissionFeeCny,
            TaxReportSummary::setConsumptionTaxCny,
            TaxReportSummary::setAdvertisingCostCny,
            TaxReportSummary::setPlatformExpensesCny,
            TaxReportSummary::setProfit4PercentCny
    );

    // ========== 属性测试 ==========

    /**
     * 属性 8a：全零/null 字段返回 false
     * 生成所有核心字段为零或 null 的 TaxReportSummary，验证返回 false
     *
     * <p><b>Validates: Requirements 11.3</b></p>
     */
    @Property(tries = 200)
    void testHasAnyTaxData_AllZeroOrNullFields_ShouldReturnFalse(
            @ForAll("zeroOrNullBigDecimal") BigDecimal totalRevenueCny,
            @ForAll("zeroOrNullBigDecimal") BigDecimal totalRevenue,
            @ForAll("zeroOrNullRefundCount") Integer refundCount,
            @ForAll("zeroOrNullBigDecimal") BigDecimal refundAmountCny,
            @ForAll("zeroOrNullBigDecimal") BigDecimal totalCommissionFeeCny,
            @ForAll("zeroOrNullBigDecimal") BigDecimal consumptionTaxCny,
            @ForAll("zeroOrNullBigDecimal") BigDecimal advertisingCostCny,
            @ForAll("zeroOrNullBigDecimal") BigDecimal platformExpensesCny
    ) throws Exception {
        // Given - 所有核心字段为零或 null
        TaxReportSummary s = new TaxReportSummary();
        s.setTotalRevenueCny(totalRevenueCny);
        s.setTotalRevenue(totalRevenue);
        s.setRefundCount(refundCount);
        s.setRefundAmountCny(refundAmountCny);
        s.setTotalCommissionFeeCny(totalCommissionFeeCny);
        s.setConsumptionTaxCny(consumptionTaxCny);
        s.setAdvertisingCostCny(advertisingCostCny);
        s.setPlatformExpensesCny(platformExpensesCny);
        s.setProfit4PercentCny(BigDecimal.ZERO); // 固定为零

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then - 所有字段为零/null 时应返回 false
        assertFalse(result, "所有核心字段为零或 null 时，hasAnyTaxData 应返回 false");
    }

    @Provide
    Arbitrary<BigDecimal> zeroOrNullBigDecimal() {
        return Arbitraries.of(BigDecimal.ZERO, null);
    }

    @Provide
    Arbitrary<Integer> zeroOrNullRefundCount() {
        // refundCount <= 0 或 null 都不算有数据
        return Arbitraries.of(null, 0, -1, -5);
    }

    /**
     * 属性 8b：任一 BigDecimal 字段非零返回 true
     * 随机选择一个核心 BigDecimal 字段设为非零值，其余为零，验证返回 true
     *
     * <p><b>Validates: Requirements 11.1, 11.2</b></p>
     */
    @Property(tries = 200)
    void testHasAnyTaxData_AnyBigDecimalFieldNonZero_ShouldReturnTrue(
            @ForAll @IntRange(min = 0, max = 7) int fieldIndex,
            @ForAll("nonZeroBigDecimal") BigDecimal nonZeroValue
    ) throws Exception {
        // Given - 基线全零对象，随机选一个 BigDecimal 字段设为非零
        TaxReportSummary s = createZeroSummary();
        BIG_DECIMAL_SETTERS.get(fieldIndex).accept(s, nonZeroValue);

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then - 任一字段非零应返回 true
        assertTrue(result, String.format(
                "字段索引 %d 设为 %s 时，hasAnyTaxData 应返回 true", fieldIndex, nonZeroValue));
    }

    /**
     * 属性 8c：refundCount 特殊处理
     * refundCount > 0 时返回 true，refundCount <= 0 或 null 时（其他字段为零）返回 false
     *
     * <p><b>Validates: Requirements 11.1, 11.2</b></p>
     */
    @Property(tries = 200)
    void testHasAnyTaxData_RefundCountPositive_ShouldReturnTrue(
            @ForAll @IntRange(min = 1, max = 1000) int positiveCount
    ) throws Exception {
        // Given - 其他字段全零，仅 refundCount > 0
        TaxReportSummary s = createZeroSummary();
        s.setRefundCount(positiveCount);

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then - refundCount > 0 应返回 true
        assertTrue(result, String.format(
                "refundCount=%d 时，hasAnyTaxData 应返回 true", positiveCount));
    }

    @Property(tries = 100)
    void testHasAnyTaxData_RefundCountZeroOrNegativeOrNull_ShouldReturnFalse(
            @ForAll("zeroOrNullRefundCount") Integer refundCount
    ) throws Exception {
        // Given - 其他字段全零，refundCount <= 0 或 null
        TaxReportSummary s = createZeroSummary();
        s.setRefundCount(refundCount);

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then - refundCount <= 0 或 null 时（其他字段为零）应返回 false
        assertFalse(result, String.format(
                "refundCount=%s 且其他字段为零时，hasAnyTaxData 应返回 false", refundCount));
    }

    /**
     * 属性 8 综合：对于任意 TaxReportSummary，hasAnyTaxData 的返回值与独立预言机一致
     *
     * <p><b>Validates: Requirements 11.1, 11.2, 11.3</b></p>
     */
    @Property(tries = 300)
    void testHasAnyTaxData_AnyRandomSummary_ShouldMatchOracle(
            @ForAll("randomSummary") TaxReportSummary summary
    ) throws Exception {
        // Given - 随机生成的 TaxReportSummary

        // When - 调用被测方法
        boolean actual = invokeHasAnyTaxData(summary);

        // Then - 与独立预言机比较
        boolean expected = expectedHasAnyTaxData(summary);
        assertEquals(expected, actual, String.format(
                "hasAnyTaxData 结果与预言机不一致: 期望=%s, 实际=%s\n" +
                "totalRevenueCny=%s, totalRevenue=%s, refundCount=%s, refundAmountCny=%s, " +
                "totalCommissionFeeCny=%s, consumptionTaxCny=%s, advertisingCostCny=%s, " +
                "platformExpensesCny=%s, profit4PercentCny=%s",
                expected, actual,
                summary.getTotalRevenueCny(), summary.getTotalRevenue(),
                summary.getRefundCount(), summary.getRefundAmountCny(),
                summary.getTotalCommissionFeeCny(), summary.getConsumptionTaxCny(),
                summary.getAdvertisingCostCny(), summary.getPlatformExpensesCny(),
                summary.getProfit4PercentCny()));
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：全 null 字段返回 false
     *
     * <p><b>Validates: Requirements 11.3</b></p>
     */
    @Example
    void testHasAnyTaxData_AllFieldsNull_ShouldReturnFalse() throws Exception {
        // Given - 所有字段为 null 的 TaxReportSummary
        TaxReportSummary s = new TaxReportSummary();

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then
        assertFalse(result, "所有字段为 null 时应返回 false");
    }

    /**
     * 示例测试：仅 totalRevenueCny 非零返回 true
     *
     * <p><b>Validates: Requirements 11.1, 11.2</b></p>
     */
    @Example
    void testHasAnyTaxData_OnlyTotalRevenueCnyNonZero_ShouldReturnTrue() throws Exception {
        // Given - 仅 totalRevenueCny 为非零值
        TaxReportSummary s = createZeroSummary();
        s.setTotalRevenueCny(new BigDecimal("12345.67"));

        // When
        boolean result = invokeHasAnyTaxData(s);

        // Then
        assertTrue(result, "仅 totalRevenueCny 非零时应返回 true");
    }
}
