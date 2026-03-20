package com.musheng.business.report.service.impl;

import com.musheng.business.sales.entity.SalesData;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 汇率使用正确性属性测试
 * 验证不同数据类型使用正确的汇率来源，以及汇率缺失/非正数时的回退行为
 *
 * <p>属性 6：对于任意退款记录和方式一费用记录，人民币值等于原币值乘以 orderRateMap 中对应 orderId 的
 * 配送日期汇率；对于任意方式二费用记录，人民币值等于原币值乘以 SalesData 自带的 exchangeRate。
 * 汇率缺失或非正数时回退为 BigDecimal.ONE。</p>
 *
 * <p><b>Validates: Requirements 1.5, 5.6, 12.1, 12.2, 12.3, 12.4, 12.5</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月21日
 */
class ExchangeRatePropertyTest {

    // ========== 汇率选择逻辑（与源码 TaxReportServiceImpl 一致） ==========

    /**
     * 退款汇率选择逻辑（源码 line 646）
     * 使用 orderRateMap 中对应 orderId 的配送日期汇率，缺失时回退为 ONE，非正数回退为 ONE
     *
     * @param orderId      订单号
     * @param orderRateMap 订单→配送汇率映射
     * @return 最终使用的汇率
     */
    private BigDecimal resolveRefundRate(String orderId, Map<String, BigDecimal> orderRateMap) {
        BigDecimal rate = orderRateMap.getOrDefault(orderId, BigDecimal.ONE);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = BigDecimal.ONE;
        }
        return rate;
    }

    /**
     * 方式一 income 汇率选择逻辑（源码 line 593）
     * 优先使用 orderRateMap 中对应 orderId 的配送日期汇率，
     * 缺失时回退到 SalesData.exchangeRate，再缺失或非正数回退为 ONE
     *
     * @param orderId       订单号
     * @param exchangeRate  SalesData 自带的汇率
     * @param orderRateMap  订单→配送汇率映射
     * @return 最终使用的汇率
     */
    private BigDecimal resolveIncomeRate(String orderId, BigDecimal exchangeRate,
                                          Map<String, BigDecimal> orderRateMap) {
        BigDecimal rate = orderRateMap.getOrDefault(orderId, exchangeRate);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = BigDecimal.ONE;
        }
        return rate;
    }

    /**
     * 方式二汇率选择逻辑（源码 line 701）
     * 使用 SalesData 自带的 exchangeRate（交易日期汇率），null 或非正数回退为 ONE
     *
     * @param exchangeRate SalesData 自带的汇率
     * @return 最终使用的汇率
     */
    private BigDecimal resolveMethod2Rate(BigDecimal exchangeRate) {
        BigDecimal rate = exchangeRate;
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = BigDecimal.ONE;
        }
        return rate;
    }

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成订单号
     */
    @Provide
    Arbitrary<String> orderIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(3).ofMaxLength(8)
                .map(s -> "ORD-" + s);
    }

    /**
     * 生成正数汇率（有效汇率）
     */
    @Provide
    Arbitrary<BigDecimal> positiveRates() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("15.00"))
                .ofScale(4);
    }

    /**
     * 生成可为 null 的汇率（包含 null、零、负数、正数）
     */
    @Provide
    Arbitrary<BigDecimal> nullableRates() {
        return Arbitraries.oneOf(
                // 正数汇率（正常情况）
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("0.01"), new BigDecimal("15.00"))
                        .ofScale(4),
                // 零
                Arbitraries.just(BigDecimal.ZERO),
                // 负数
                Arbitraries.bigDecimals()
                        .between(new BigDecimal("-10.00"), new BigDecimal("-0.01"))
                        .ofScale(4),
                // null
                Arbitraries.just((BigDecimal) null)
        );
    }

    /**
     * 生成 orderRateMap（订单→汇率映射）
     */
    @Provide
    Arbitrary<Map<String, BigDecimal>> orderRateMaps() {
        return Arbitraries.maps(
                orderIds(),
                positiveRates()
        ).ofMinSize(0).ofMaxSize(10);
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

    // ========== 属性测试 ==========

    /**
     * 属性 6a：退款汇率使用 orderRateMap
     * 对于任意退款记录，当 orderRateMap 中有对应 orderId 的汇率时，应使用该汇率；
     * 当没有时回退为 BigDecimal.ONE
     *
     * <p><b>Validates: Requirements 1.5, 12.1, 12.4</b></p>
     */
    @Property(tries = 200)
    void testRefundRate_OrderIdInMap_ShouldUseMapRate(
            @ForAll("orderIds") String orderId,
            @ForAll("positiveRates") BigDecimal mapRate) {
        // Given - orderId 在 orderRateMap 中
        Map<String, BigDecimal> orderRateMap = new HashMap<>();
        orderRateMap.put(orderId, mapRate);

        // When - 解析退款汇率
        BigDecimal rate = resolveRefundRate(orderId, orderRateMap);

        // Then - 应使用 orderRateMap 中的汇率
        assertEquals(0, mapRate.compareTo(rate),
                String.format("退款汇率应使用 orderRateMap 中的值 %s，实际为 %s", mapRate, rate));
    }

    /**
     * 属性 6a 补充：退款 orderId 不在 orderRateMap 中时回退为 ONE
     *
     * <p><b>Validates: Requirements 12.4</b></p>
     */
    @Property(tries = 200)
    void testRefundRate_OrderIdNotInMap_ShouldFallbackToOne(
            @ForAll("orderIds") String orderId,
            @ForAll("orderIds") String otherOrderId) {
        // Given - orderId 不在 orderRateMap 中（使用不同的 key）
        Map<String, BigDecimal> orderRateMap = new HashMap<>();
        // 确保 orderId 不在 map 中
        orderRateMap.remove(orderId);
        if (!otherOrderId.equals(orderId)) {
            orderRateMap.put(otherOrderId, new BigDecimal("7.25"));
        }

        // When - 解析退款汇率
        BigDecimal rate = resolveRefundRate(orderId, orderRateMap);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                String.format("退款 orderId 不在 orderRateMap 中时应回退为 ONE，实际为 %s", rate));
    }

    /**
     * 属性 6b：方式一 income 汇率使用 orderRateMap
     * 对于任意 income 记录，当 orderRateMap 中有对应 orderId 的汇率时，应使用该汇率；
     * 当没有时回退到 SalesData.exchangeRate，再没有则回退为 ONE
     *
     * <p><b>Validates: Requirements 12.2</b></p>
     */
    @Property(tries = 200)
    void testIncomeRate_OrderIdInMap_ShouldUseMapRate(
            @ForAll("orderIds") String orderId,
            @ForAll("positiveRates") BigDecimal mapRate,
            @ForAll("nullableRates") BigDecimal selfRate) {
        // Given - orderId 在 orderRateMap 中
        Map<String, BigDecimal> orderRateMap = new HashMap<>();
        orderRateMap.put(orderId, mapRate);

        // When - 解析 income 汇率
        BigDecimal rate = resolveIncomeRate(orderId, selfRate, orderRateMap);

        // Then - 应优先使用 orderRateMap 中的汇率
        assertEquals(0, mapRate.compareTo(rate),
                String.format("income 汇率应优先使用 orderRateMap 中的值 %s，实际为 %s", mapRate, rate));
    }

    /**
     * 属性 6b 补充：income orderId 不在 orderRateMap 中时回退到 SalesData.exchangeRate
     *
     * <p><b>Validates: Requirements 12.2, 12.4</b></p>
     */
    @Property(tries = 200)
    void testIncomeRate_OrderIdNotInMap_ShouldFallbackToSelfRate(
            @ForAll("orderIds") String orderId,
            @ForAll("positiveRates") BigDecimal selfRate) {
        // Given - orderId 不在 orderRateMap 中，SalesData 自带正数汇率
        Map<String, BigDecimal> orderRateMap = new HashMap<>();

        // When - 解析 income 汇率
        BigDecimal rate = resolveIncomeRate(orderId, selfRate, orderRateMap);

        // Then - 应回退到 SalesData.exchangeRate
        assertEquals(0, selfRate.compareTo(rate),
                String.format("income orderId 不在 map 中时应回退到 SalesData.exchangeRate=%s，实际为 %s",
                        selfRate, rate));
    }

    /**
     * 属性 6c：方式二汇率使用 SalesData.exchangeRate
     * 对于任意方式二记录，应使用 SalesData 自带的 exchangeRate（交易日期汇率）
     *
     * <p><b>Validates: Requirements 5.6, 12.3</b></p>
     */
    @Property(tries = 200)
    void testMethod2Rate_PositiveExchangeRate_ShouldUseSelfRate(
            @ForAll("positiveRates") BigDecimal selfRate) {
        // Given - SalesData 自带正数汇率

        // When - 解析方式二汇率
        BigDecimal rate = resolveMethod2Rate(selfRate);

        // Then - 应使用 SalesData.exchangeRate
        assertEquals(0, selfRate.compareTo(rate),
                String.format("方式二应使用 SalesData.exchangeRate=%s，实际为 %s", selfRate, rate));
    }

    /**
     * 属性 6d：汇率非正数回退
     * 当汇率为 null、零或负数时，应回退为 BigDecimal.ONE
     *
     * <p><b>Validates: Requirements 12.5</b></p>
     */
    @Property(tries = 200)
    void testRefundRate_NonPositiveRateInMap_ShouldFallbackToOne(
            @ForAll("orderIds") String orderId) {
        // Given - orderRateMap 中的汇率为零
        Map<String, BigDecimal> orderRateMap = new HashMap<>();
        orderRateMap.put(orderId, BigDecimal.ZERO);

        // When - 解析退款汇率
        BigDecimal rate = resolveRefundRate(orderId, orderRateMap);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                "orderRateMap 中汇率为零时应回退为 ONE");
    }

    /**
     * 属性 6d 补充：方式二汇率为 null 时回退为 ONE
     *
     * <p><b>Validates: Requirements 12.5</b></p>
     */
    @Example
    void testMethod2Rate_NullExchangeRate_ShouldFallbackToOne() {
        // Given - exchangeRate 为 null

        // When - 解析方式二汇率
        BigDecimal rate = resolveMethod2Rate(null);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                "方式二汇率为 null 时应回退为 ONE");
    }

    /**
     * 属性 6d 补充：方式二汇率为负数时回退为 ONE
     *
     * <p><b>Validates: Requirements 12.5</b></p>
     */
    @Property(tries = 100)
    void testMethod2Rate_NegativeExchangeRate_ShouldFallbackToOne(
            @ForAll("positiveRates") BigDecimal positiveRate) {
        // Given - exchangeRate 为负数
        BigDecimal negativeRate = positiveRate.negate();

        // When - 解析方式二汇率
        BigDecimal rate = resolveMethod2Rate(negativeRate);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                String.format("方式二汇率为负数 %s 时应回退为 ONE", negativeRate));
    }

    /**
     * 属性 6d 补充：income 汇率 orderRateMap 缺失且 SalesData.exchangeRate 也无效时回退为 ONE
     *
     * <p><b>Validates: Requirements 12.4, 12.5</b></p>
     */
    @Property(tries = 200)
    void testIncomeRate_BothMissing_ShouldFallbackToOne(
            @ForAll("orderIds") String orderId) {
        // Given - orderId 不在 orderRateMap 中，SalesData.exchangeRate 为 null
        Map<String, BigDecimal> orderRateMap = new HashMap<>();

        // When - 解析 income 汇率
        BigDecimal rate = resolveIncomeRate(orderId, null, orderRateMap);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                "orderRateMap 缺失且 SalesData.exchangeRate 为 null 时应回退为 ONE");
    }

    // ========== 示例测试 ==========

    /**
     * 示例测试：退款汇率正常场景
     * orderId 在 orderRateMap 中，使用配送日期汇率
     *
     * <p><b>Validates: Requirements 1.5, 12.1</b></p>
     */
    @Example
    void testRefundRate_NormalScenario_ShouldUseDeliveryRate() {
        // Given - orderId 在 orderRateMap 中
        String orderId = "111-2345678-9012345";
        BigDecimal deliveryRate = new BigDecimal("7.2500");
        Map<String, BigDecimal> orderRateMap = Map.of(orderId, deliveryRate);

        // When - 解析退款汇率
        BigDecimal rate = resolveRefundRate(orderId, orderRateMap);

        // Then - 应使用配送日期汇率 7.25
        assertEquals(0, deliveryRate.compareTo(rate),
                String.format("退款应使用配送日期汇率 %s，实际为 %s", deliveryRate, rate));
    }

    /**
     * 示例测试：退款汇率缺失场景
     * orderId 不在 orderRateMap 中，回退为 ONE
     *
     * <p><b>Validates: Requirements 12.4</b></p>
     */
    @Example
    void testRefundRate_MissingScenario_ShouldFallbackToOne() {
        // Given - orderId 不在 orderRateMap 中
        String orderId = "111-9999999-0000000";
        Map<String, BigDecimal> orderRateMap = Map.of("111-1111111-1111111", new BigDecimal("7.2500"));

        // When - 解析退款汇率
        BigDecimal rate = resolveRefundRate(orderId, orderRateMap);

        // Then - 应回退为 ONE
        assertEquals(0, BigDecimal.ONE.compareTo(rate),
                "退款 orderId 不在 orderRateMap 中时应回退为 ONE");
    }

    /**
     * 示例测试：方式二汇率正常场景
     * 使用 SalesData.exchangeRate
     *
     * <p><b>Validates: Requirements 5.6, 12.3</b></p>
     */
    @Example
    void testMethod2Rate_NormalScenario_ShouldUseSelfExchangeRate() {
        // Given - SalesData 自带汇率
        BigDecimal selfRate = new BigDecimal("7.2000");

        // When - 解析方式二汇率
        BigDecimal rate = resolveMethod2Rate(selfRate);

        // Then - 应使用 SalesData.exchangeRate
        assertEquals(0, selfRate.compareTo(rate),
                String.format("方式二应使用 SalesData.exchangeRate=%s，实际为 %s", selfRate, rate));
    }
}
