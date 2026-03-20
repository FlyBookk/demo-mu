package com.musheng.business.report.service.impl;

import com.musheng.business.sales.entity.SalesData;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 退款筛选口径属性测试
 * 验证退款筛选逻辑的正确性：refund 类型 + orderId ∈ shippingOrderIds（配送日期口径）
 *
 * <p>属性 1：对于任意退款记录集合和配送订单集合 shippingOrderIds，筛选后的退款列表中
 * 每条记录的 orderId 都存在于 shippingOrderIds 中，且不存在 orderId 为 null 的记录；
 * 交易日期不影响筛选结果。</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月20日
 */
class RefundFilterPropertyTest {

    // ========== 退款筛选逻辑（与源码 TaxReportServiceImpl 一致） ==========

    /**
     * 模拟源码中的退款筛选逻辑
     * 源码位置：TaxReportServiceImpl.calculateTaxSummaryFromMemory() 中退款筛选部分
     *
     * @param allSalesData    所有销售数据（含 income/refund 类型）
     * @param shippingOrderIds 本季度配送订单号集合
     * @return 筛选后的退款列表
     */
    private List<SalesData> filterRefundList(List<SalesData> allSalesData, Set<String> shippingOrderIds) {
        return allSalesData.stream()
                .filter(s -> "refund".equals(s.getTransactionCategory()))
                .filter(s -> s.getOrderId() != null && shippingOrderIds.contains(s.getOrderId()))
                .collect(Collectors.toList());
    }

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成订单号（非 null）
     */
    @Provide
    Arbitrary<String> orderIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(3).ofMaxLength(10)
                .map(s -> "ORD-" + s);
    }

    /**
     * 生成可为 null 的订单号（模拟 orderId 可能为 null 的情况）
     */
    @Provide
    Arbitrary<String> nullableOrderIds() {
        return orderIds().injectNull(0.3); // 30% 概率为 null
    }

    /**
     * 生成交易分类（refund 或 income）
     */
    @Provide
    Arbitrary<String> transactionCategories() {
        return Arbitraries.of("refund", "income");
    }

    /**
     * 生成随机交易日期
     */
    @Provide
    Arbitrary<LocalDateTime> transactionDates() {
        return Arbitraries.integers().between(2024, 2026)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDateTime.of(year, month, day, 10, 0))));
    }

    /**
     * 生成单条 SalesData 退款/收入记录
     */
    @Provide
    Arbitrary<SalesData> salesDataRecord() {
        return Combinators.combine(
                nullableOrderIds(),
                transactionCategories(),
                transactionDates()
        ).as((orderId, category, txDate) -> {
            SalesData data = new SalesData();
            data.setOrderId(orderId);
            data.setTransactionCategory(category);
            data.setTransactionDate(txDate);
            data.setTotal(new BigDecimal("-10.00"));
            return data;
        });
    }

    /**
     * 生成配送订单号集合
     */
    @Provide
    Arbitrary<Set<String>> shippingOrderIdSets() {
        return orderIds().set().ofMinSize(0).ofMaxSize(10);
    }

    /**
     * 生成销售数据列表
     */
    @Provide
    Arbitrary<List<SalesData>> salesDataLists() {
        return salesDataRecord().list().ofMinSize(0).ofMaxSize(20);
    }

    // ========== 属性测试 ==========

    /**
     * 属性 1.1：筛选后每条退款记录的 orderId 都在 shippingOrderIds 中
     * 验证退款通过 orderId 关联到配送数据
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 200)
    void testFilterRefund_AnyRefundSet_ShouldOnlyContainOrderIdsInShippingSet(
            @ForAll("salesDataLists") List<SalesData> allSalesData,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的销售数据和配送订单集合

        // When - 执行退款筛选
        List<SalesData> refundList = filterRefundList(allSalesData, shippingOrderIds);

        // Then - 每条退款记录的 orderId 都在 shippingOrderIds 中
        for (SalesData refund : refundList) {
            assertTrue(shippingOrderIds.contains(refund.getOrderId()),
                    String.format("退款记录的 orderId=%s 不在 shippingOrderIds 中", refund.getOrderId()));
        }
    }

    /**
     * 属性 1.2：筛选后不存在 orderId 为 null 的退款记录
     *
     * <p><b>Validates: Requirements 1.3</b></p>
     */
    @Property(tries = 200)
    void testFilterRefund_AnyRefundSet_ShouldNotContainNullOrderId(
            @ForAll("salesDataLists") List<SalesData> allSalesData,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的销售数据和配送订单集合

        // When - 执行退款筛选
        List<SalesData> refundList = filterRefundList(allSalesData, shippingOrderIds);

        // Then - 不存在 orderId 为 null 的记录
        for (SalesData refund : refundList) {
            assertNotNull(refund.getOrderId(), "筛选后的退款记录不应包含 orderId 为 null 的记录");
        }
    }

    /**
     * 属性 1.3：orderId 不在 shippingOrderIds 中的退款记录被排除
     *
     * <p><b>Validates: Requirements 1.4</b></p>
     */
    @Property(tries = 200)
    void testFilterRefund_OrderIdNotInShippingSet_ShouldBeExcluded(
            @ForAll("salesDataLists") List<SalesData> allSalesData,
            @ForAll("shippingOrderIdSets") Set<String> shippingOrderIds) {
        // Given - 随机生成的销售数据和配送订单集合

        // When - 执行退款筛选
        List<SalesData> refundList = filterRefundList(allSalesData, shippingOrderIds);

        // Then - 所有 refund 类型且 orderId 不在 shippingOrderIds 中的记录都不在结果中
        Set<String> resultOrderIds = refundList.stream()
                .map(SalesData::getOrderId)
                .collect(Collectors.toSet());

        for (SalesData s : allSalesData) {
            if ("refund".equals(s.getTransactionCategory())
                    && s.getOrderId() != null
                    && !shippingOrderIds.contains(s.getOrderId())) {
                assertFalse(resultOrderIds.contains(s.getOrderId()),
                        String.format("orderId=%s 不在 shippingOrderIds 中，不应出现在筛选结果中",
                                s.getOrderId()));
            }
        }
    }

    /**
     * 属性 1.4：交易日期不影响筛选结果
     * 同一 orderId，不同交易日期，只要 orderId 在 shippingOrderIds 中就纳入
     *
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 200)
    void testFilterRefund_DifferentTransactionDates_ShouldNotAffectResult(
            @ForAll("orderIds") String orderId,
            @ForAll("transactionDates") LocalDateTime date1,
            @ForAll("transactionDates") LocalDateTime date2) {
        // Given - 同一 orderId，不同交易日期的两条退款记录
        SalesData refund1 = new SalesData();
        refund1.setOrderId(orderId);
        refund1.setTransactionCategory("refund");
        refund1.setTransactionDate(date1);

        SalesData refund2 = new SalesData();
        refund2.setOrderId(orderId);
        refund2.setTransactionCategory("refund");
        refund2.setTransactionDate(date2);

        Set<String> shippingOrderIds = Set.of(orderId);

        // When - 分别筛选
        List<SalesData> result1 = filterRefundList(List.of(refund1), shippingOrderIds);
        List<SalesData> result2 = filterRefundList(List.of(refund2), shippingOrderIds);

        // Then - 两条记录都应被纳入，交易日期不影响筛选
        assertEquals(1, result1.size(), "交易日期不应影响筛选结果（date1）");
        assertEquals(1, result2.size(), "交易日期不应影响筛选结果（date2）");
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：空集合情况
     * 空的销售数据列表和空的配送订单集合，筛选结果应为空
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Example
    void testFilterRefund_EmptyCollections_ShouldReturnEmpty() {
        // Given - 空集合
        List<SalesData> emptyList = Collections.emptyList();
        Set<String> emptySet = Collections.emptySet();

        // When
        List<SalesData> result = filterRefundList(emptyList, emptySet);

        // Then
        assertTrue(result.isEmpty(), "空集合筛选结果应为空");
    }

    /**
     * 示例测试：混合数据中正确筛选退款
     *
     * <p><b>Validates: Requirements 1.1, 1.3, 1.4</b></p>
     */
    @Example
    void testFilterRefund_MixedData_ShouldFilterCorrectly() {
        // Given - 混合数据
        Set<String> shippingOrderIds = Set.of("ORD-001", "ORD-002");

        SalesData refundInSet = new SalesData();
        refundInSet.setOrderId("ORD-001");
        refundInSet.setTransactionCategory("refund");
        refundInSet.setTransactionDate(LocalDateTime.of(2025, 7, 15, 10, 0));

        SalesData refundNotInSet = new SalesData();
        refundNotInSet.setOrderId("ORD-999");
        refundNotInSet.setTransactionCategory("refund");
        refundNotInSet.setTransactionDate(LocalDateTime.of(2025, 7, 15, 10, 0));

        SalesData refundNullOrderId = new SalesData();
        refundNullOrderId.setOrderId(null);
        refundNullOrderId.setTransactionCategory("refund");
        refundNullOrderId.setTransactionDate(LocalDateTime.of(2025, 7, 15, 10, 0));

        SalesData incomeInSet = new SalesData();
        incomeInSet.setOrderId("ORD-002");
        incomeInSet.setTransactionCategory("income");
        incomeInSet.setTransactionDate(LocalDateTime.of(2025, 7, 15, 10, 0));

        List<SalesData> allData = List.of(refundInSet, refundNotInSet, refundNullOrderId, incomeInSet);

        // When
        List<SalesData> result = filterRefundList(allData, shippingOrderIds);

        // Then - 只有 refundInSet 应被纳入
        assertEquals(1, result.size(), "应只有 1 条退款记录通过筛选");
        assertEquals("ORD-001", result.get(0).getOrderId(), "通过筛选的退款 orderId 应为 ORD-001");
    }
}
