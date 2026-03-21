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
 * 方式二费用计算仅排除 transfer 类型记录 - 属性测试
 *
 * 验证方式二费用计算中，通过 transaction_category 字段排除资金划转记录的正确性。
 * 提取 TaxReportServiceImpl 中方式二费用计算的过滤逻辑，验证过滤行为。
 *
 * // Feature: transfer-category-mapping, Property 5: 方式二费用计算仅排除 transfer 类型记录
 *
 * <p><b>Validates: Requirements 4.1, 4.4, 4.5, 6.3</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月20日
 */
class Method2FeeTransferExclusionPropertyTest {

    // 测试季度：2025-Q3（7月1日 ~ 9月30日）
    private static final LocalDate START_DATE = LocalDate.of(2025, 7, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 9, 30);

    // ========== 方式二费用过滤逻辑（与 TaxReportServiceImpl 一致） ==========

    /**
     * 模拟源码中方式二费用计算的过滤逻辑
     * 返回参与费用计算的记录列表（排除 transfer 类型和超范围日期）
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

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成 transactionCategory 值（包含 transfer 和其他常见分类）
     */
    @Provide
    Arbitrary<String> transactionCategories() {
        return Arbitraries.of("transfer", "fee", "adjustment", "other");
    }

    /**
     * 生成交易日期（覆盖季度内外的日期）
     */
    @Provide
    Arbitrary<LocalDateTime> transactionDates() {
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
     * 生成单条方式二 SalesData 记录（使用 transactionCategory 字段）
     */
    @Provide
    Arbitrary<SalesData> method2Record() {
        return Combinators.combine(
                transactionCategories(),
                nullableTransactionDates(),
                nullableAmounts(), // sellingFees
                nullableAmounts(), // fbaFees
                nullableAmounts(), // otherTransactionFees
                nullableAmounts(), // other
                nullableAmounts()  // marketplaceWithheldTax
        ).as((category, txDate, selling, fba, otherTrans, otherAmt, tax) -> {
            SalesData data = new SalesData();
            data.setTransactionCategory(category);
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
        return method2Record().list().ofMinSize(0).ofMaxSize(20);
    }

    // ========== 属性测试 ==========

    /**
     * 属性 5.1：transaction_category 为 transfer 的记录不参与方式二费用计算
     * 对于任意方式二数据集合，筛选后的记录中不包含 transfer 类型
     *
     * // Feature: transfer-category-mapping, Property 5: 方式二费用计算仅排除 transfer 类型记录
     *
     * <p><b>Validates: Requirements 4.1, 4.4</b></p>
     */
    @Property(tries = 200)
    @Label("属性5.1 - transfer 类型记录被排除")
    void transferCategoryShouldBeExcluded(
            @ForAll("method2DataList") List<SalesData> allOtherData) {
        // When - 执行方式二过滤
        List<SalesData> filtered = filterMethod2Records(allOtherData, START_DATE, END_DATE);

        // Then - 筛选后不包含 transfer 类型
        for (SalesData record : filtered) {
            assertNotEquals("transfer", record.getTransactionCategory(),
                    "transaction_category 为 transfer 的记录不应参与方式二费用计算");
        }
    }

    /**
     * 属性 5.2：所有 transaction_category != "transfer" 且日期在范围内的记录都被包含
     * 验证符合条件的记录不会被错误排除
     *
     * // Feature: transfer-category-mapping, Property 5: 方式二费用计算仅排除 transfer 类型记录
     *
     * <p><b>Validates: Requirements 4.5, 6.3</b></p>
     */
    @Property(tries = 200)
    @Label("属性5.2 - 非 transfer 且日期在范围内的记录全部包含")
    void nonTransferInRangeShouldBeIncluded(
            @ForAll("method2DataList") List<SalesData> allOtherData) {
        // When - 执行方式二过滤
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
                "所有非 transfer 且日期在范围内的记录都应出现在筛选结果中");
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：混合数据中正确排除 transfer 类型记录
     *
     * // Feature: transfer-category-mapping, Property 5: 方式二费用计算仅排除 transfer 类型记录
     *
     * <p><b>Validates: Requirements 4.1, 4.4, 4.5, 6.3</b></p>
     */
    @Example
    void mixedDataShouldExcludeTransferCategory() {
        // Given - 构造混合数据

        // 记录1：transfer 类型，日期在范围内 → 应排除
        SalesData transferRecord = new SalesData();
        transferRecord.setTransactionCategory("transfer");
        transferRecord.setTransactionDate(LocalDateTime.of(2025, 8, 15, 10, 0));
        transferRecord.setSellingFees(new BigDecimal("-5.00"));

        // 记录2：fee 类型，日期在范围内 → 应纳入
        SalesData feeRecord = new SalesData();
        feeRecord.setTransactionCategory("fee");
        feeRecord.setTransactionDate(LocalDateTime.of(2025, 8, 20, 10, 0));
        feeRecord.setSellingFees(new BigDecimal("-3.00"));

        // 记录3：fee 类型，日期在范围外（6月） → 应排除（日期原因）
        SalesData outOfRangeRecord = new SalesData();
        outOfRangeRecord.setTransactionCategory("fee");
        outOfRangeRecord.setTransactionDate(LocalDateTime.of(2025, 6, 15, 10, 0));
        outOfRangeRecord.setSellingFees(new BigDecimal("-10.00"));

        // 记录4：other 类型，日期在范围内 → 应纳入
        SalesData otherRecord = new SalesData();
        otherRecord.setTransactionCategory("other");
        otherRecord.setTransactionDate(LocalDateTime.of(2025, 9, 1, 10, 0));
        otherRecord.setSellingFees(new BigDecimal("-2.00"));

        // 记录5：transfer 类型，不同站点的资金划转（如法国 Virement） → 应排除
        SalesData frenchTransfer = new SalesData();
        frenchTransfer.setTransactionCategory("transfer");
        frenchTransfer.setTransactionType("Virement");
        frenchTransfer.setTransactionDate(LocalDateTime.of(2025, 7, 10, 10, 0));
        frenchTransfer.setSellingFees(new BigDecimal("-8.00"));

        List<SalesData> allData = List.of(
                transferRecord, feeRecord, outOfRangeRecord, otherRecord, frenchTransfer);

        // When - 执行方式二过滤
        List<SalesData> filtered = filterMethod2Records(allData, START_DATE, END_DATE);

        // Then - 只有 feeRecord 和 otherRecord 应被纳入
        assertEquals(2, filtered.size(), "应有 2 条记录通过筛选");
        assertTrue(filtered.contains(feeRecord), "fee 类型且日期在范围内的记录应被纳入");
        assertTrue(filtered.contains(otherRecord), "other 类型且日期在范围内的记录应被纳入");
        assertFalse(filtered.contains(transferRecord), "transfer 类型记录应被排除");
        assertFalse(filtered.contains(frenchTransfer), "新站点 transfer 类型记录也应被排除");
    }
}
