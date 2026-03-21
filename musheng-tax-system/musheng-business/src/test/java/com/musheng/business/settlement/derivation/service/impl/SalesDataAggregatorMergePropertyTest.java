package com.musheng.business.settlement.derivation.service.impl;

import net.jqwik.api.*;
import net.jqwik.api.Combinators;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * mergeNetSalesMaps 和 mergeOrderRateMaps 合并方法属性测试
 *
 * 验证双路径合并逻辑的正确性：
 * - 属性 4：netSalesMap 合并后每个 (siteCode, sku) 的值等于 incomeMap + refundMap 对应值之和
 * - 属性 5：orderRateMap 合并时 income 路径汇率优先
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SalesDataAggregatorMergePropertyTest {

    /** 被测实例（mergeNetSalesMaps 和 mergeOrderRateMaps 不依赖注入的 bean） */
    private final SalesDataAggregatorImpl aggregator = new SalesDataAggregatorImpl();

    // ==================== 反射工具方法 ====================

    /**
     * 通过反射调用 private mergeNetSalesMaps 方法
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Integer>> invokeMergeNetSalesMaps(
            Map<String, Map<String, Integer>> incomeMap,
            Map<String, Map<String, Integer>> refundMap) throws Exception {
        Method method = SalesDataAggregatorImpl.class.getDeclaredMethod(
                "mergeNetSalesMaps", Map.class, Map.class);
        method.setAccessible(true);
        return (Map<String, Map<String, Integer>>) method.invoke(aggregator, incomeMap, refundMap);
    }

    /**
     * 通过反射调用 private mergeOrderRateMaps 方法
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> invokeMergeOrderRateMaps(
            Map<String, BigDecimal> incomeRateMap,
            Map<String, BigDecimal> refundRateMap) throws Exception {
        Method method = SalesDataAggregatorImpl.class.getDeclaredMethod(
                "mergeOrderRateMaps", Map.class, Map.class);
        method.setAccessible(true);
        return (Map<String, BigDecimal>) method.invoke(aggregator, incomeRateMap, refundRateMap);
    }

    // ==================== jqwik 自定义生成器 ====================

    /** 站点编码列表 */
    private static final List<String> SITE_CODES = List.of("US", "CA", "UK", "DE", "FR", "JP");

    /**
     * 生成随机站点编码
     */
    @Provide
    Arbitrary<String> siteCodes() {
        return Arbitraries.of(SITE_CODES);
    }

    /**
     * 生成随机 SKU 字符串
     */
    @Provide
    Arbitrary<String> skus() {
        return Arbitraries.of("MSUS-", "MSCA-", "MSUK-", "MS-", "ABC-", "FBA-", "SKU-")
                .flatMap(prefix -> Arbitraries.strings().alpha().numeric()
                        .withChars('-').ofMinLength(3).ofMaxLength(10)
                        .map(suffix -> prefix + suffix));
    }

    /**
     * 生成随机数量值（可正可负）
     */
    @Provide
    Arbitrary<Integer> quantities() {
        return Arbitraries.integers().between(-1000, 1000);
    }

    /**
     * 生成随机 netSalesMap：站点 → (SKU → 数量)
     * 每个 map 包含 0~3 个站点，每个站点 0~4 个 SKU
     */
    @Provide
    Arbitrary<Map<String, Map<String, Integer>>> netSalesMaps() {
        Arbitrary<Map<String, Integer>> skuMap = Arbitraries.maps(skus(), quantities())
                .ofMinSize(0).ofMaxSize(4);
        return Arbitraries.maps(siteCodes(), skuMap)
                .ofMinSize(0).ofMaxSize(3);
    }

    /**
     * 生成随机 orderId
     */
    @Provide
    Arbitrary<String> orderIds() {
        return Arbitraries.strings().numeric().ofMinLength(5).ofMaxLength(15)
                .map(s -> "ORD-" + s);
    }

    /**
     * 生成随机汇率值
     */
    @Provide
    Arbitrary<BigDecimal> exchangeRates() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(10.0))
                .ofScale(4);
    }

    /**
     * 生成随机 orderRateMap：orderId → 汇率
     */
    @Provide
    Arbitrary<Map<String, BigDecimal>> orderRateMaps() {
        return Arbitraries.maps(orderIds(), exchangeRates())
                .ofMinSize(0).ofMaxSize(5);
    }

    // ==================== 属性 4：双路径 netSalesMap 合并正确性 ====================

    /**
     * 属性 4：双路径 netSalesMap 合并正确性
     *
     * 对于任意 incomeMap 和 refundMap，合并后的 netSalesMap 中
     * 每个 (siteCode, sku) 的值应等于 incomeMap 中对应值（默认 0）
     * 加上 refundMap 中对应值（默认 0）。
     *
     * // Feature: settlement-derivation-date-adjust, Property 4: 双路径 netSalesMap 合并正确性
     *
     * <p><b>Validates: Requirements 4.1, 4.2</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 4: 双路径 netSalesMap 合并正确性")
    void mergedNetSalesMapShouldEqualSumOfBothMaps(
            @ForAll("netSalesMaps") Map<String, Map<String, Integer>> incomeMap,
            @ForAll("netSalesMaps") Map<String, Map<String, Integer>> refundMap) throws Exception {

        // 调用被测方法
        Map<String, Map<String, Integer>> merged = invokeMergeNetSalesMaps(incomeMap, refundMap);

        // 收集所有出现过的 (siteCode, sku) 组合
        Set<String> allSites = new HashSet<>();
        allSites.addAll(incomeMap.keySet());
        allSites.addAll(refundMap.keySet());

        for (String siteCode : allSites) {
            Map<String, Integer> incomeSkuMap = incomeMap.getOrDefault(siteCode, Collections.emptyMap());
            Map<String, Integer> refundSkuMap = refundMap.getOrDefault(siteCode, Collections.emptyMap());

            Set<String> allSkus = new HashSet<>();
            allSkus.addAll(incomeSkuMap.keySet());
            allSkus.addAll(refundSkuMap.keySet());

            // 合并结果中该站点必须存在
            assertTrue(merged.containsKey(siteCode),
                    "合并结果应包含站点: " + siteCode);

            Map<String, Integer> mergedSkuMap = merged.get(siteCode);

            for (String sku : allSkus) {
                int expectedValue = incomeSkuMap.getOrDefault(sku, 0)
                        + refundSkuMap.getOrDefault(sku, 0);

                assertEquals(expectedValue, mergedSkuMap.getOrDefault(sku, 0),
                        "站点 " + siteCode + " SKU " + sku + " 合并值应为 income + refund");
            }
        }

        // 验证合并结果不包含额外的站点
        for (String siteCode : merged.keySet()) {
            assertTrue(allSites.contains(siteCode),
                    "合并结果不应包含额外站点: " + siteCode);
        }
    }

    // ==================== 属性 5：orderRateMap 合并 income 优先 ====================

    /**
     * 属性 5：orderRateMap 合并 income 优先
     *
     * 对于任意 incomeRateMap 和 refundRateMap，合并后的 orderRateMap 中，
     * 如果同一 orderId 在两条路径中都存在，则应使用 income 路径的汇率值。
     *
     * // Feature: settlement-derivation-date-adjust, Property 5: orderRateMap 合并 income 优先
     *
     * <p><b>Validates: Requirements 4.3</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 5: orderRateMap 合并 income 优先")
    void mergedOrderRateMapShouldPreferIncomeRate(
            @ForAll("orderRateMaps") Map<String, BigDecimal> incomeRateMap,
            @ForAll("orderRateMaps") Map<String, BigDecimal> refundRateMap) throws Exception {

        // 调用被测方法
        Map<String, BigDecimal> merged = invokeMergeOrderRateMaps(incomeRateMap, refundRateMap);

        // 收集所有 orderId
        Set<String> allOrderIds = new HashSet<>();
        allOrderIds.addAll(incomeRateMap.keySet());
        allOrderIds.addAll(refundRateMap.keySet());

        // 验证合并结果包含所有 orderId
        assertEquals(allOrderIds, merged.keySet(),
                "合并结果应包含两条路径的所有 orderId");

        for (String orderId : allOrderIds) {
            BigDecimal mergedRate = merged.get(orderId);

            if (incomeRateMap.containsKey(orderId)) {
                // income 路径存在时，应使用 income 路径的汇率
                assertEquals(0, incomeRateMap.get(orderId).compareTo(mergedRate),
                        "orderId " + orderId + " 应使用 income 路径汇率");
            } else {
                // 仅 refund 路径存在时，应使用 refund 路径的汇率
                assertEquals(0, refundRateMap.get(orderId).compareTo(mergedRate),
                        "orderId " + orderId + " 仅在 refund 路径中，应使用 refund 路径汇率");
            }
        }
    }
}
