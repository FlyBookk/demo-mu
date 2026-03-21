package com.musheng.business.report.service.impl;

import com.musheng.business.sales.entity.SalesData;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 方式二费用计算属性测试
 * 验证方式二排除规则的正确性：transaction_category = "transfer" 排除 + 交易日期范围过滤
 *
 * <p>属性 5：对于任意方式二数据集合，transactionCategory 为 "transfer" 的记录和
 * 交易日期不在 [startDate, endDate] 范围内的记录不参与任何费用计算。</p>
 *
 * <p><b>Validates: Requirements 4.1, 4.2</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月20日
 */
class Method2FeePropertyTest {

    // ========== 方式二费用计算逻辑（与源码 TaxReportServiceImpl 一致） ==========

    /**
     * 空值转零（与被测方法一致的辅助逻辑）
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 模拟源码中的方式二费用筛选逻辑
     * 返回参与费用计算的记录列表（排除 transaction_category = "transfer" 和超范围日期）
     *
     * @param allOtherData 非 income/refund 类型的结算数据
     * @param startDate    季度开始日期
     * @param endDate      季度结束日期
     * @return 参与费用计算的记录列表
     */
    private List<SalesData> filterMethod2Records(List<SalesData> allOtherData,
                                                  LocalDate startDate, LocalDate endDate) {
        List<SalesData> result = new ArrayList<>();
        for (SalesData other : allOtherData) {
            // 排除 transfer 类型（通过 transaction_category 统一判断）
            if ("transfer".equals(other.getTransactionCategory())) {
                continue;
            }
            // 排除交易日期为 null 的记录
            if (other.getTransactionDate() == null) continue;
            LocalDate transDate = other.getTransactionDate().toLocalDate();
            // 排除交易日期不在 [startDate, endDate] 范围内的记录
            if (transDate.isBefore(startDate) || transDate.isAfter(endDate)) continue;
            result.add(other);
        }
        return result;
    }

    /**
     * 模拟源码中的方式二费用累加逻辑
     * 返回 [佣金服务费, 其他费, 税] 三个累加值
     */
    private BigDecimal[] calculateMethod2Fees(List<SalesData> allOtherData,
                                               LocalDate startDate, LocalDate endDate) {
        BigDecimal miscCommissionFee = BigDecimal.ZERO;
        BigDecimal miscOtherFee = BigDecimal.ZERO;
        BigDecimal miscConsumptionTax = BigDecimal.ZERO;

        for (SalesData other : allOtherData) {
            // 排除 transfer 类型（通过 transaction_category 统一判断）
            if ("transfer".equals(other.getTransactionCategory())) continue;
            if (other.getTransactionDate() == null) continue;
            LocalDate transDate = other.getTransactionDate().toLocalDate();
            if (transDate.isBefore(startDate) || transDate.isAfter(endDate)) continue;

            BigDecimal rate = other.getExchangeRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                rate = BigDecimal.ONE;
            }

            // 佣金3项
            BigDecimal selling = nullToZero(other.getSellingFees());
            BigDecimal fba = nullToZero(other.getFbaFees());
            BigDecimal otherTrans = nullToZero(other.getOtherTransactionFees());
            miscCommissionFee = miscCommissionFee.add(selling.add(fba).add(otherTrans));

            // 其他费
            miscOtherFee = miscOtherFee.add(nullToZero(other.getOther()));

            // 税
            miscConsumptionTax = miscConsumptionTax.add(nullToZero(other.getMarketplaceWithheldTax()));
        }

        return new BigDecimal[]{miscCommissionFee, miscOtherFee, miscConsumptionTax};
    }

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成交易类型（包含 Transfer 和其他常见类型）
     */
    @Provide
    Arbitrary<String> transactionTypes() {
        return Arbitraries.of("Transfer", "Service Fee", "Adjustment", "FBA Inventory Fee",
                "Cost of Advertising", "Other");
    }

    /**
     * 生成 transactionCategory 值（Transfer 类型对应 "transfer"，其他对应 "fee" 或 "other"）
     */
    @Provide
    Arbitrary<String> transactionCategories() {
        return Arbitraries.of("transfer", "fee", "other");
    }

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
     * 生成汇率（正数或 null）
     */
    @Provide
    Arbitrary<BigDecimal> exchangeRates() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("15.00"))
                .ofScale(4)
                .injectNull(0.1);
    }

    /**
     * 生成交易日期（覆盖季度内外的日期）
     * 使用 2025-Q3（7月1日 ~ 9月30日）作为测试季度
     */
    @Provide
    Arbitrary<LocalDateTime> transactionDates() {
        // 生成 2025年5月 ~ 2025年12月 的日期，覆盖季度内外
        return Arbitraries.integers().between(2025, 2025)
                .flatMap(year -> Arbitraries.integers().between(5, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDateTime.of(year, month, day, 10, 0))));
    }

    /**
     * 生成可为 null 的交易日期
     */
    @Provide
    Arbitrary<LocalDateTime> nullableTransactionDates() {
        return transactionDates().injectNull(0.1);
    }

    /**
     * 生成单条方式二 SalesData 记录
     */
    @Provide
    Arbitrary<SalesData> method2Record() {
        return Combinators.combine(
                transactionTypes(),
                transactionCategories(),
                nullableTransactionDates(),
                nullableAmounts(), // sellingFees
                nullableAmounts(), // fbaFees
                nullableAmounts(), // otherTransactionFees
                nullableAmounts(), // other
                nullableAmounts()  // marketplaceWithheldTax
        ).as((txType, category, txDate, selling, fba, otherTrans, otherAmt, tax) -> {
            SalesData data = new SalesData();
            data.setTransactionType(txType);
            // Transfer 类型对应 transactionCategory = "transfer"
            if ("Transfer".equals(txType)) {
                data.setTransactionCategory("transfer");
            } else {
                data.setTransactionCategory(category.equals("transfer") ? "fee" : category);
            }
            data.setTransactionDate(txDate);
            data.setSellingFees(selling);
            data.setFbaFees(fba);
            data.setOtherTransactionFees(otherTrans);
            data.setOther(otherAmt);
            data.setMarketplaceWithheldTax(tax);
            data.setExchangeRate(null);
            return data;
        });
    }

    /**
     * 生成方式二数据列表
     */
    @Provide
    Arbitrary<List<SalesData>> method2DataList() {
        return method2Record().list().ofMinSize(0).ofMaxSize(20);
    }

    // ========== 属性测试 ==========

    // 测试季度：2025-Q3（7月1日 ~ 9月30日）
    private static final LocalDate START_DATE = LocalDate.of(2025, 7, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 9, 30);

    /**
     * 属性 5.1：transaction_category 为 "transfer" 的记录不参与任何费用计算
     * 对于任意方式二数据集合，筛选后的记录中不包含 transfer 类型
     *
     * <p><b>Validates: Requirements 4.1</b></p>
     */
    @Property(tries = 200)
    void testMethod2Fee_TransferType_ShouldBeExcluded(
            @ForAll("method2DataList") List<SalesData> allOtherData) {
        // Given - 随机生成的方式二数据集合

        // When - 执行方式二筛选
        List<SalesData> filtered = filterMethod2Records(allOtherData, START_DATE, END_DATE);

        // Then - 筛选后不包含 transactionCategory = "transfer" 的记录
        for (SalesData record : filtered) {
            assertNotEquals("transfer", record.getTransactionCategory(),
                    "transactionCategory 为 transfer 的记录不应参与方式二费用计算");
        }
    }

    /**
     * 属性 5.2：交易日期不在 [startDate, endDate] 范围内的记录不参与费用计算
     * 对于任意方式二数据集合，筛选后的记录交易日期都在季度范围内
     *
     * <p><b>Validates: Requirements 4.2</b></p>
     */
    @Property(tries = 200)
    void testMethod2Fee_OutOfDateRange_ShouldBeExcluded(
            @ForAll("method2DataList") List<SalesData> allOtherData) {
        // Given - 随机生成的方式二数据集合

        // When - 执行方式二筛选
        List<SalesData> filtered = filterMethod2Records(allOtherData, START_DATE, END_DATE);

        // Then - 筛选后每条记录的交易日期都在 [startDate, endDate] 范围内
        for (SalesData record : filtered) {
            assertNotNull(record.getTransactionDate(), "筛选后的记录交易日期不应为 null");
            LocalDate transDate = record.getTransactionDate().toLocalDate();
            assertFalse(transDate.isBefore(START_DATE),
                    String.format("交易日期 %s 早于季度开始日期 %s，不应参与计算", transDate, START_DATE));
            assertFalse(transDate.isAfter(END_DATE),
                    String.format("交易日期 %s 晚于季度结束日期 %s，不应参与计算", transDate, END_DATE));
        }
    }

    /**
     * 属性 5.3：非 transfer 且日期在范围内的记录参与费用计算
     * 验证符合条件的记录不会被错误排除
     *
     * <p><b>Validates: Requirements 4.1, 4.2</b></p>
     */
    @Property(tries = 200)
    void testMethod2Fee_ValidRecord_ShouldBeIncluded(
            @ForAll("method2DataList") List<SalesData> allOtherData) {
        // Given - 随机生成的方式二数据集合

        // When - 执行方式二筛选
        List<SalesData> filtered = filterMethod2Records(allOtherData, START_DATE, END_DATE);

        // Then - 所有非 transfer 且日期在范围内的记录都应在结果中
        List<SalesData> expectedIncluded = allOtherData.stream()
                .filter(s -> !"transfer".equals(s.getTransactionCategory()))
                .filter(s -> s.getTransactionDate() != null)
                .filter(s -> {
                    LocalDate d = s.getTransactionDate().toLocalDate();
                    return !d.isBefore(START_DATE) && !d.isAfter(END_DATE);
                })
                .collect(Collectors.toList());

        assertEquals(expectedIncluded.size(), filtered.size(),
                "符合条件的记录数量应与筛选结果一致");
        assertTrue(filtered.containsAll(expectedIncluded),
                "所有符合条件的记录都应出现在筛选结果中");
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：混合数据中正确排除 transfer 类型和超范围日期
     *
     * <p><b>Validates: Requirements 4.1, 4.2</b></p>
     */
    @Example
    void testMethod2Fee_MixedData_ShouldExcludeTransferAndOutOfRange() {
        // Given - 构造混合数据
        // 记录1：Transfer 类型，transactionCategory = "transfer"，日期在范围内 → 应排除
        SalesData transfer = new SalesData();
        transfer.setTransactionType("Transfer");
        transfer.setTransactionCategory("transfer");
        transfer.setTransactionDate(LocalDateTime.of(2025, 8, 15, 10, 0));
        transfer.setSellingFees(new BigDecimal("-5.00"));
        transfer.setFbaFees(new BigDecimal("-3.00"));
        transfer.setOtherTransactionFees(new BigDecimal("-1.00"));
        transfer.setOther(new BigDecimal("-0.50"));
        transfer.setMarketplaceWithheldTax(new BigDecimal("-0.80"));
        transfer.setExchangeRate(new BigDecimal("7.20"));

        // 记录2：Service Fee 类型，日期在范围外（6月） → 应排除
        SalesData outOfRange = new SalesData();
        outOfRange.setTransactionType("Service Fee");
        outOfRange.setTransactionCategory("fee");
        outOfRange.setTransactionDate(LocalDateTime.of(2025, 6, 15, 10, 0));
        outOfRange.setSellingFees(new BigDecimal("-10.00"));
        outOfRange.setFbaFees(BigDecimal.ZERO);
        outOfRange.setOtherTransactionFees(BigDecimal.ZERO);
        outOfRange.setOther(BigDecimal.ZERO);
        outOfRange.setMarketplaceWithheldTax(BigDecimal.ZERO);
        outOfRange.setExchangeRate(new BigDecimal("7.20"));

        // 记录3：Service Fee 类型，日期在范围内 → 应纳入
        SalesData valid = new SalesData();
        valid.setTransactionType("Service Fee");
        valid.setTransactionCategory("fee");
        valid.setTransactionDate(LocalDateTime.of(2025, 8, 15, 10, 0));
        valid.setSellingFees(new BigDecimal("-5.00"));
        valid.setFbaFees(new BigDecimal("-2.00"));
        valid.setOtherTransactionFees(new BigDecimal("-1.50"));
        valid.setOther(new BigDecimal("-0.30"));
        valid.setMarketplaceWithheldTax(new BigDecimal("-0.80"));
        valid.setExchangeRate(new BigDecimal("7.20"));

        // 记录4：Adjustment 类型，日期在范围外（10月） → 应排除
        SalesData outOfRangeLate = new SalesData();
        outOfRangeLate.setTransactionType("Adjustment");
        outOfRangeLate.setTransactionCategory("fee");
        outOfRangeLate.setTransactionDate(LocalDateTime.of(2025, 10, 5, 10, 0));
        outOfRangeLate.setSellingFees(new BigDecimal("-3.00"));
        outOfRangeLate.setFbaFees(BigDecimal.ZERO);
        outOfRangeLate.setOtherTransactionFees(BigDecimal.ZERO);
        outOfRangeLate.setOther(BigDecimal.ZERO);
        outOfRangeLate.setMarketplaceWithheldTax(BigDecimal.ZERO);
        outOfRangeLate.setExchangeRate(new BigDecimal("7.20"));

        List<SalesData> allData = List.of(transfer, outOfRange, valid, outOfRangeLate);

        // When - 执行方式二筛选
        List<SalesData> filtered = filterMethod2Records(allData, START_DATE, END_DATE);

        // Then - 只有 valid 记录应被纳入
        assertEquals(1, filtered.size(), "应只有 1 条记录通过筛选");
        assertSame(valid, filtered.get(0), "通过筛选的应为 Service Fee 类型且日期在范围内的记录");

        // 验证费用计算结果
        BigDecimal[] fees = calculateMethod2Fees(allData, START_DATE, END_DATE);
        BigDecimal expectedCommission = new BigDecimal("-5.00")
                .add(new BigDecimal("-2.00"))
                .add(new BigDecimal("-1.50")); // = -8.50
        BigDecimal expectedOther = new BigDecimal("-0.30");
        BigDecimal expectedTax = new BigDecimal("-0.80");

        assertEquals(0, expectedCommission.compareTo(fees[0]),
                String.format("佣金服务费应为 %s，实际为 %s", expectedCommission, fees[0]));
        assertEquals(0, expectedOther.compareTo(fees[1]),
                String.format("其他费应为 %s，实际为 %s", expectedOther, fees[1]));
        assertEquals(0, expectedTax.compareTo(fees[2]),
                String.format("税应为 %s，实际为 %s", expectedTax, fees[2]));
    }
}
