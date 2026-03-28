package com.musheng.business.settlement.derivation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.settlement.derivation.service.impl.SalesDataAggregatorImpl;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SalesDataAggregator 单元测试
 *
 * <p>测试双路径（Income + Refund）汇总逻辑：
 * Income 路径通过配送数据关联销售数据，Refund 路径按结算日期直接查询。</p>
 *
 * <p>注意：新架构下 salesDataMapper.selectList 会被调用多次：
 * 1. Income 路径：通过 orderId 关联查询 income 记录
 * 2. Refund 路径：通过 transactionDate 查询 refund 记录
 * 因此 mock 需要返回可变列表（ArrayList），因为 queryRefundByTransactionDate 会调用 removeIf。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class SalesDataAggregatorTest {

    @Mock
    private ShippingDataMapper shippingDataMapper;

    @Mock
    private SalesDataMapper salesDataMapper;

    @InjectMocks
    private SalesDataAggregatorImpl aggregator;

    private static final Long SHOP_ID = 1L;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 2, 24);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 3, 2);

    /**
     * 测试正常汇总：income 和 refund 都有数据，计算净销售数量
     *
     * <p>双路径架构下：
     * - Income 路径：配送数据关联 income 销售记录
     * - Refund 路径：按 transactionDate 直接查询 refund 记录
     * salesDataMapper 会被调用两次，使用 thenReturn 链式返回不同结果。</p>
     */
    @Test
    void testAggregateNetSales_NormalAggregation_ShouldReturnCorrectResult() {
        // Given - 配送数据：2个订单
        ShippingData shipping1 = buildShippingData("ORD-001", "US", new BigDecimal("7.25"));
        ShippingData shipping2 = buildShippingData("ORD-002", "US", new BigDecimal("7.30"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(shipping1, shipping2));

        // Income 路径销售数据：income 5件
        SalesData income1 = buildSalesData("ORD-001", "US", "SKU-A", 2, "income");
        SalesData income2 = buildSalesData("ORD-002", "US", "SKU-A", 3, "income");
        // Refund 路径销售数据：refund 1件
        SalesData refund1 = buildRefundSalesData("ORD-001", "US", "SKU-A", 1);

        // salesDataMapper 第一次调用（income 路径 querySalesData），第二次调用（refund 路径 queryRefundByTransactionDate）
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(income1, income2)))
                .thenReturn(new ArrayList<>(List.of(refund1)));

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END, List.of("US", "CA", "UK", "EU"));

        // Then
        assertNotNull(result, "汇总结果不应为 null");
        assertNotNull(result.getNetSalesMap(), "netSalesMap 不应为 null");
        assertNotNull(result.getOrderRateMap(), "orderRateMap 不应为 null");

        // 净销售数量 = income(2+3) + refund(-1) = 4
        Map<String, Integer> usSales = result.getNetSalesMap().get("US");
        assertNotNull(usSales, "US 站点应有数据");
        assertEquals(4, usSales.get("SKU-A"), "SKU-A 净销售数量应为 4");

        // orderRateMap 应包含两个订单的汇率（来自 income 路径的配送数据）
        assertEquals(new BigDecimal("7.25"), result.getOrderRateMap().get("ORD-001"));
        assertEquals(new BigDecimal("7.30"), result.getOrderRateMap().get("ORD-002"));
    }

    /**
     * 测试空配送数据且无退款：应返回空结果
     */
    @Test
    void testAggregateNetSales_EmptyShippingData_ShouldReturnEmptyResult() {
        // Given - 无配送数据（income 路径为空）
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // 无退款数据（refund 路径为空）
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END, List.of("US", "CA", "UK", "EU"));

        // Then
        assertNotNull(result, "汇总结果不应为 null");
        assertTrue(result.getNetSalesMap().isEmpty(), "netSalesMap 应为空");
        assertTrue(result.getOrderRateMap().isEmpty(), "orderRateMap 应为空");
    }

    /**
     * 测试仅 income 无 refund：净销售数量应为正数
     */
    @Test
    void testAggregateNetSales_OnlyIncome_ShouldReturnPositiveQuantity() {
        // Given
        ShippingData shipping1 = buildShippingData("ORD-001", "CA", new BigDecimal("5.50"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(shipping1));

        SalesData income1 = buildSalesData("ORD-001", "CA", "SKU-B", 5, "income");
        // 第一次调用返回 income 数据，第二次调用返回空 refund 列表
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(income1)))
                .thenReturn(new ArrayList<>());

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END, List.of("US", "CA", "UK", "EU"));

        // Then
        Map<String, Integer> caSales = result.getNetSalesMap().get("CA");
        assertNotNull(caSales, "CA 站点应有数据");
        assertEquals(5, caSales.get("SKU-B"), "SKU-B 净销售数量应为 5");
    }

    /**
     * 测试净销售为负：refund 大于 income
     */
    @Test
    void testAggregateNetSales_NegativeNetSales_ShouldReturnNegativeQuantity() {
        // Given
        ShippingData shipping1 = buildShippingData("ORD-001", "UK", new BigDecimal("0.85"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(shipping1));

        SalesData income1 = buildSalesData("ORD-001", "UK", "SKU-C", 1, "income");
        SalesData refund1 = buildRefundSalesData("ORD-001", "UK", "SKU-C", 3);
        // 第一次调用返回 income 数据，第二次调用返回 refund 数据
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(income1)))
                .thenReturn(new ArrayList<>(List.of(refund1)));

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END, List.of("US", "CA", "UK", "EU"));

        // Then
        Map<String, Integer> ukSales = result.getNetSalesMap().get("UK");
        assertNotNull(ukSales, "UK 站点应有数据");
        assertEquals(-2, ukSales.get("SKU-C"), "SKU-C 净销售数量应为 -2");
    }

    /**
     * 测试多站点多 MSKU：各维度独立汇总
     */
    @Test
    void testAggregateNetSales_MultipleSitesAndMskus_ShouldAggregateCorrectly() {
        // Given - 3个订单，跨2个站点，2个MSKU
        ShippingData s1 = buildShippingData("ORD-001", "US", new BigDecimal("7.25"));
        ShippingData s2 = buildShippingData("ORD-002", "US", new BigDecimal("7.30"));
        ShippingData s3 = buildShippingData("ORD-003", "DE", new BigDecimal("0.92"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(s1, s2, s3));

        // Income 路径销售数据
        SalesData inc1 = buildSalesData("ORD-001", "US", "SKU-A", 2, "income");
        SalesData inc2 = buildSalesData("ORD-002", "US", "SKU-B", 4, "income");
        SalesData inc3 = buildSalesData("ORD-003", "DE", "SKU-A", 6, "income");
        // Refund 路径销售数据
        SalesData ref1 = buildRefundSalesData("ORD-002", "US", "SKU-B", 1);

        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(inc1, inc2, inc3)))
                .thenReturn(new ArrayList<>(List.of(ref1)));

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END, List.of("US", "CA", "UK", "EU"));

        // Then
        // US 站点
        Map<String, Integer> usSales = result.getNetSalesMap().get("US");
        assertNotNull(usSales, "US 站点应有数据");
        assertEquals(2, usSales.get("SKU-A"), "US/SKU-A 净销售数量应为 2");
        assertEquals(3, usSales.get("SKU-B"), "US/SKU-B 净销售数量应为 3 (4-1)");

        // DE 站点
        Map<String, Integer> deSales = result.getNetSalesMap().get("DE");
        assertNotNull(deSales, "DE 站点应有数据");
        assertEquals(6, deSales.get("SKU-A"), "DE/SKU-A 净销售数量应为 6");

        // orderRateMap 应包含3个订单（来自 income 路径配送数据）
        assertEquals(3, result.getOrderRateMap().size(), "应有3个订单的汇率映射");
    }

    // ========== 辅助方法 ==========

    /**
     * 构建配送数据测试对象
     *
     * @param orderId 订单号
     * @param siteCode 站点编码
     * @param exchangeRate 汇率
     * @return 配送数据对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private ShippingData buildShippingData(String orderId, String siteCode, BigDecimal exchangeRate) {
        ShippingData data = new ShippingData();
        data.setShopId(SHOP_ID);
        data.setOrderId(orderId);
        data.setSiteCode(siteCode);
        data.setExchangeRate(exchangeRate);
        data.setShipDate(PERIOD_START);
        return data;
    }

    /**
     * 构建销售数据测试对象
     *
     * @param orderId 订单号
     * @param siteCode 站点编码
     * @param sku SKU 编码
     * @param quantity 数量
     * @param transactionCategory 交易分类
     * @return 销售数据对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private SalesData buildSalesData(String orderId, String siteCode, String sku,
                                     int quantity, String transactionCategory) {
        SalesData data = new SalesData();
        data.setShopId(SHOP_ID);
        data.setOrderId(orderId);
        data.setSiteCode(siteCode);
        data.setSku(sku);
        data.setQuantity(quantity);
        data.setTransactionCategory(transactionCategory);
        return data;
    }

    /**
     * 构建退款销售数据测试对象（transactionCategory 固定为 refund，设置 transactionDate）
     *
     * @param orderId 订单号
     * @param siteCode 站点编码
     * @param sku SKU 编码
     * @param quantity 数量
     * @return 退款销售数据对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private SalesData buildRefundSalesData(String orderId, String siteCode, String sku, int quantity) {
        SalesData data = new SalesData();
        data.setShopId(SHOP_ID);
        data.setOrderId(orderId);
        data.setSiteCode(siteCode);
        data.setSku(sku);
        data.setQuantity(quantity);
        data.setTransactionCategory("refund");
        data.setTransactionDate(PERIOD_START.atStartOfDay());
        return data;
    }
}
