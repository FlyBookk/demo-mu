package com.musheng.business.settlement.derivation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.business.settlement.derivation.vo.SupplierRefundDetail;
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
 * SalesDataAggregatorImpl 集成单元测试
 *
 * <p>重点测试 aggregateNetSales 双路径串联逻辑及供应商结差（supplierRefundMap）相关场景：
 * 1. 仅有 income 数据（含供应商 MSKU）
 * 2. 仅有 refund 数据（含供应商 MSKU）
 * 3. income + refund 混合数据（含供应商和非供应商 MSKU）
 * 4. 周期内无数据返回空结果</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class SalesDataAggregatorImplTest {

    @Mock
    private ShippingDataMapper shippingDataMapper;

    @Mock
    private SalesDataMapper salesDataMapper;

    @InjectMocks
    private SalesDataAggregatorImpl aggregator;

    private static final Long SHOP_ID = 1L;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 3, 31);

    /**
     * 场景1：仅有 income 数据，含供应商 MSKU
     *
     * <p>配送数据有 orderId，income 销售数据含 MSUS- 前缀 SKU。
     * refund 路径返回空列表。
     * 验证 netSalesMap 有 income 数据，supplierRefundMap 为空（因为没有 refund）。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testAggregateNetSales_OnlyIncomeWithSupplierMsku_ShouldReturnIncomeDataAndEmptySupplierRefund() {
        // Given - 配送数据：1个订单
        ShippingData shipping1 = buildShippingData("ORD-101", "US", new BigDecimal("7.25"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(shipping1));

        // Income 路径：供应商 MSKU MSUS-SKU-A，数量 3
        SalesData income1 = buildSalesData("ORD-101", "US", "MSUS-SKU-A", 3, "income",
                new BigDecimal("19.99"), new BigDecimal("7.25"));
        // Refund 路径：空列表
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(income1)))
                .thenReturn(new ArrayList<>());

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END);

        // Then
        assertNotNull(result, "汇总结果不应为 null");

        // 验证 netSalesMap：US/MSUS-SKU-A = 3
        Map<String, Map<String, Integer>> netSalesMap = result.getNetSalesMap();
        assertNotNull(netSalesMap, "netSalesMap 不应为 null");
        Map<String, Integer> usSales = netSalesMap.get("US");
        assertNotNull(usSales, "US 站点应有数据");
        assertEquals(3, usSales.get("MSUS-SKU-A"), "MSUS-SKU-A 净销售数量应为 3");

        // 验证 orderRateMap：ORD-101 汇率 7.25
        assertEquals(new BigDecimal("7.25"), result.getOrderRateMap().get("ORD-101"),
                "ORD-101 汇率应为 7.25");

        // 验证 supplierRefundMap：无 refund 数据，应为空
        Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap = result.getSupplierRefundMap();
        assertNotNull(supplierRefundMap, "supplierRefundMap 不应为 null");
        assertTrue(supplierRefundMap.isEmpty(), "无退款数据时 supplierRefundMap 应为空");
    }

    /**
     * 场景2：仅有 refund 数据，含供应商 MSKU
     *
     * <p>配送数据为空（income 路径为空）。
     * refund 路径有 MSUS- 前缀 SKU 的退款记录。
     * 验证 netSalesMap 有负值，supplierRefundMap 包含供应商退款明细。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testAggregateNetSales_OnlyRefundWithSupplierMsku_ShouldReturnNegativeNetSalesAndSupplierRefund() {
        // Given - 无配送数据（income 路径为空）
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // Refund 路径：供应商 MSKU MSUS-SKU-A，退款 2 件，金额 39.98
        SalesData refund1 = buildRefundSalesData("ORD-201", "US", "MSUS-SKU-A", 2,
                new BigDecimal("39.98"), new BigDecimal("7.20"));
        // salesDataMapper 仅被调用一次（income 路径无 orderId 不会查询，只有 refund 路径查询）
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(refund1)));

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END);

        // Then
        assertNotNull(result, "汇总结果不应为 null");

        // 验证 netSalesMap：US/MSUS-SKU-A = -2（退款累减）
        Map<String, Map<String, Integer>> netSalesMap = result.getNetSalesMap();
        assertNotNull(netSalesMap, "netSalesMap 不应为 null");
        Map<String, Integer> usSales = netSalesMap.get("US");
        assertNotNull(usSales, "US 站点应有退款数据");
        assertEquals(-2, usSales.get("MSUS-SKU-A"), "MSUS-SKU-A 净销售数量应为 -2");

        // 验证 supplierRefundMap：包含 MSUS-SKU-A 的退款明细
        Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap = result.getSupplierRefundMap();
        assertNotNull(supplierRefundMap, "supplierRefundMap 不应为 null");
        assertFalse(supplierRefundMap.isEmpty(), "有供应商退款时 supplierRefundMap 不应为空");

        Map<String, SupplierRefundDetail> usRefunds = supplierRefundMap.get("US");
        assertNotNull(usRefunds, "US 站点应有供应商退款明细");
        SupplierRefundDetail detail = usRefunds.get("MSUS-SKU-A");
        assertNotNull(detail, "MSUS-SKU-A 应有退款明细");
        assertEquals(2, detail.getQuantity(), "退款数量应为 2（正数）");
        assertEquals(new BigDecimal("39.98"), detail.getAmount(), "退款金额应为 39.98（正数）");
    }

    /**
     * 场景3：income + refund 混合，含供应商和非供应商 MSKU
     *
     * <p>配送数据有 orderId，income 有 MSUS-SKU-A（供应商）和 ABC-SKU-B（非供应商）。
     * refund 有 MSUS-SKU-A 的退款。
     * 验证 netSalesMap 正确合并，supplierRefundMap 仅包含 MSUS-SKU-A 的退款。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testAggregateNetSales_MixedIncomeRefundWithSupplierAndNonSupplier_ShouldMergeCorrectly() {
        // Given - 配送数据：2个订单
        ShippingData shipping1 = buildShippingData("ORD-301", "US", new BigDecimal("7.25"));
        ShippingData shipping2 = buildShippingData("ORD-302", "US", new BigDecimal("7.30"));
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(shipping1, shipping2));

        // Income 路径：MSUS-SKU-A 5件 + ABC-SKU-B 3件
        SalesData income1 = buildSalesData("ORD-301", "US", "MSUS-SKU-A", 5, "income",
                new BigDecimal("99.95"), new BigDecimal("7.25"));
        SalesData income2 = buildSalesData("ORD-302", "US", "ABC-SKU-B", 3, "income",
                new BigDecimal("59.97"), new BigDecimal("7.30"));

        // Refund 路径：MSUS-SKU-A 退款 2件，金额 39.98
        SalesData refund1 = buildRefundSalesData("ORD-301", "US", "MSUS-SKU-A", 2,
                new BigDecimal("39.98"), new BigDecimal("7.25"));

        // salesDataMapper 第一次调用（income 路径），第二次调用（refund 路径）
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(income1, income2)))
                .thenReturn(new ArrayList<>(List.of(refund1)));

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END);

        // Then
        assertNotNull(result, "汇总结果不应为 null");

        // 验证 netSalesMap 合并结果
        Map<String, Map<String, Integer>> netSalesMap = result.getNetSalesMap();
        Map<String, Integer> usSales = netSalesMap.get("US");
        assertNotNull(usSales, "US 站点应有数据");
        // MSUS-SKU-A: income(5) + refund(-2) = 3
        assertEquals(3, usSales.get("MSUS-SKU-A"), "MSUS-SKU-A 净销售数量应为 3 (5-2)");
        // ABC-SKU-B: income(3)，无退款
        assertEquals(3, usSales.get("ABC-SKU-B"), "ABC-SKU-B 净销售数量应为 3");

        // 验证 orderRateMap
        assertEquals(new BigDecimal("7.25"), result.getOrderRateMap().get("ORD-301"));
        assertEquals(new BigDecimal("7.30"), result.getOrderRateMap().get("ORD-302"));

        // 验证 supplierRefundMap：仅包含 MSUS-SKU-A 的退款，不包含 ABC-SKU-B
        Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap = result.getSupplierRefundMap();
        assertNotNull(supplierRefundMap, "supplierRefundMap 不应为 null");

        Map<String, SupplierRefundDetail> usRefunds = supplierRefundMap.get("US");
        assertNotNull(usRefunds, "US 站点应有供应商退款明细");
        assertNotNull(usRefunds.get("MSUS-SKU-A"), "MSUS-SKU-A 应有退款明细");
        assertNull(usRefunds.get("ABC-SKU-B"), "ABC-SKU-B 不是供应商 MSKU，不应出现在 supplierRefundMap");

        SupplierRefundDetail detail = usRefunds.get("MSUS-SKU-A");
        assertEquals(2, detail.getQuantity(), "供应商退款数量应为 2");
        assertEquals(new BigDecimal("39.98"), detail.getAmount(), "供应商退款金额应为 39.98");
    }

    /**
     * 场景4：周期内无数据返回空结果
     *
     * <p>配送数据为空，refund 为空。
     * 验证所有 map 为空。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testAggregateNetSales_NoDataInPeriod_ShouldReturnEmptyResult() {
        // Given - 无配送数据
        when(shippingDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // 无退款数据
        when(salesDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        // When
        AggregationResult result = aggregator.aggregateNetSales(SHOP_ID, PERIOD_START, PERIOD_END);

        // Then
        assertNotNull(result, "汇总结果不应为 null");
        assertTrue(result.getNetSalesMap().isEmpty(), "netSalesMap 应为空");
        assertTrue(result.getOrderRateMap().isEmpty(), "orderRateMap 应为空");
        assertNotNull(result.getSupplierRefundMap(), "supplierRefundMap 不应为 null");
        assertTrue(result.getSupplierRefundMap().isEmpty(), "supplierRefundMap 应为空");
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
     * @param total 合计金额
     * @param exchangeRate 汇率
     * @return 销售数据对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private SalesData buildSalesData(String orderId, String siteCode, String sku,
                                     int quantity, String transactionCategory,
                                     BigDecimal total, BigDecimal exchangeRate) {
        SalesData data = new SalesData();
        data.setShopId(SHOP_ID);
        data.setOrderId(orderId);
        data.setSiteCode(siteCode);
        data.setSku(sku);
        data.setQuantity(quantity);
        data.setTransactionCategory(transactionCategory);
        data.setTotal(total);
        data.setExchangeRate(exchangeRate);
        return data;
    }

    /**
     * 构建退款销售数据测试对象（transactionCategory 固定为 refund，设置 transactionDate）
     *
     * @param orderId 订单号
     * @param siteCode 站点编码
     * @param sku SKU 编码
     * @param quantity 数量
     * @param total 合计金额
     * @param exchangeRate 汇率
     * @return 退款销售数据对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private SalesData buildRefundSalesData(String orderId, String siteCode, String sku,
                                           int quantity, BigDecimal total, BigDecimal exchangeRate) {
        SalesData data = new SalesData();
        data.setShopId(SHOP_ID);
        data.setOrderId(orderId);
        data.setSiteCode(siteCode);
        data.setSku(sku);
        data.setQuantity(quantity);
        data.setTransactionCategory("refund");
        data.setTransactionDate(PERIOD_START.atStartOfDay());
        data.setTotal(total);
        data.setExchangeRate(exchangeRate);
        return data;
    }
}
