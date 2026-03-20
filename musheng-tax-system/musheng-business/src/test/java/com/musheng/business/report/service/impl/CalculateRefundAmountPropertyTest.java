package com.musheng.business.report.service.impl;

import com.musheng.business.sales.entity.SalesData;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * calculateRefundAmount 属性测试
 * 验证退款费用项 9 项求和正确性
 *
 * <p>属性 2：对于任意 SalesData 退款记录，calculateRefundAmount 的返回值恰好等于
 * productSales + productSalesTax + shippingCredits + shippingCreditsTax +
 * giftWrapCredits + giftWrapCreditsTax + regulatoryFee + regulatoryFeeTax +
 * promotionalRebates 九项之和（null 视为 0），不包含 promotionalRebatesTax 和费用类字段。</p>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.4</b></p>
 *
 * @author wanhua
 * 10:30 2026年03月20日
 */
class CalculateRefundAmountPropertyTest {

    // ========== 反射工具方法 ==========

    /**
     * 通过反射调用 private calculateRefundAmount 方法
     *
     * @param refund 退款记录
     * @return 退款金额小计
     */
    private BigDecimal invokeCalculateRefundAmount(SalesData refund) throws Exception {
        // TaxReportServiceImpl 使用 @RequiredArgsConstructor，构造器需要所有 final 字段
        // calculateRefundAmount 是纯计算方法，不依赖任何注入的依赖，可安全传 null
        Object service = createServiceInstance();
        Method method = TaxReportServiceImpl.class.getDeclaredMethod("calculateRefundAmount", SalesData.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, refund);
    }

    /**
     * 通过反射创建 TaxReportServiceImpl 实例（所有依赖传 null）
     * calculateRefundAmount 是纯计算方法，不访问任何注入的依赖
     */
    private Object createServiceInstance() throws Exception {
        var constructors = TaxReportServiceImpl.class.getDeclaredConstructors();
        for (var constructor : constructors) {
            constructor.setAccessible(true);
            // 找到参数最多的构造器（Lombok 生成的 @RequiredArgsConstructor）
            Object[] args = new Object[constructor.getParameterCount()];
            return constructor.newInstance(args);
        }
        throw new RuntimeException("无法创建 TaxReportServiceImpl 实例");
    }

    /**
     * 空值转零（与被测方法一致的辅助逻辑）
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ========== jqwik 自定义生成器 ==========

    /**
     * 生成可为 null 的 BigDecimal（模拟数据库字段可能为 null 的情况）
     */
    @Provide
    Arbitrary<BigDecimal> nullableBigDecimal() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-9999.99"), new BigDecimal("9999.99"))
                .ofScale(2)
                .injectNull(0.2); // 20% 概率为 null
    }

    /**
     * 生成完整的退款 SalesData 对象
     * 使用 Combinators.combine(List) 方式绕过 8 参数限制
     */
    @Provide
    Arbitrary<SalesData> refundSalesData() {
        Arbitrary<BigDecimal> amounts = nullableBigDecimal();
        // 15 个金额字段：9 项参与计算 + 1 promotionalRebatesTax + 5 费用类字段
        List<Arbitrary<BigDecimal>> arbitraries = List.of(
                amounts, amounts, amounts, amounts, amounts,
                amounts, amounts, amounts, amounts,
                amounts, amounts, amounts, amounts, amounts, amounts
        );
        return Combinators.combine(arbitraries).as(values -> {
            SalesData data = new SalesData();
            data.setTransactionCategory("refund");
            data.setOrderId("TEST-ORDER-001");
            // 9 项参与计算的字段
            data.setProductSales(values.get(0));
            data.setProductSalesTax(values.get(1));
            data.setShippingCredits(values.get(2));
            data.setShippingCreditsTax(values.get(3));
            data.setGiftWrapCredits(values.get(4));
            data.setGiftWrapCreditsTax(values.get(5));
            data.setRegulatoryFee(values.get(6));
            data.setRegulatoryFeeTax(values.get(7));
            data.setPromotionalRebates(values.get(8));
            // 不参与计算的字段（设置随机值以验证不影响结果）
            data.setPromotionalRebatesTax(values.get(9));
            data.setSellingFees(values.get(10));
            data.setFbaFees(values.get(11));
            data.setOtherTransactionFees(values.get(12));
            data.setOther(values.get(13));
            data.setMarketplaceWithheldTax(values.get(14));
            return data;
        });
    }

    // ========== 属性测试 ==========

    /**
     * 属性 2：退款费用项 9 项求和正确性
     * 对于任意 SalesData 退款记录，calculateRefundAmount 返回值恰好等于 9 项之和
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.4</b></p>
     */
    @Property(tries = 200)
    void testCalculateRefundAmount_AnyRefundData_ShouldEqualNineFieldsSum(
            @ForAll("refundSalesData") SalesData refund) throws Exception {
        // Given - 随机生成的退款记录（由 refundSalesData 提供）

        // When - 调用被测方法
        BigDecimal actual = invokeCalculateRefundAmount(refund);

        // Then - 验证返回值等于 9 项之和（null 视为 0）
        BigDecimal expected = nullToZero(refund.getProductSales())
                .add(nullToZero(refund.getProductSalesTax()))
                .add(nullToZero(refund.getShippingCredits()))
                .add(nullToZero(refund.getShippingCreditsTax()))
                .add(nullToZero(refund.getGiftWrapCredits()))
                .add(nullToZero(refund.getGiftWrapCreditsTax()))
                .add(nullToZero(refund.getRegulatoryFee()))
                .add(nullToZero(refund.getRegulatoryFeeTax()))
                .add(nullToZero(refund.getPromotionalRebates()));

        assertEquals(0, expected.compareTo(actual),
                String.format("退款 9 项求和不一致: 期望=%s, 实际=%s", expected, actual));
    }

    /**
     * 属性 2 补充：promotionalRebatesTax 不参与计算
     * 即使 promotionalRebatesTax 设置了不同值，也不影响 calculateRefundAmount 的结果
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Property(tries = 100)
    void testCalculateRefundAmount_PromotionalRebatesTaxChanged_ShouldNotAffectResult(
            @ForAll("refundSalesData") SalesData refund,
            @ForAll @BigRange(min = "-9999.99", max = "9999.99") @Scale(2) BigDecimal differentTaxValue
    ) throws Exception {
        // Given - 计算原始结果
        BigDecimal originalResult = invokeCalculateRefundAmount(refund);

        // When - 修改 promotionalRebatesTax 为不同值
        refund.setPromotionalRebatesTax(differentTaxValue);
        BigDecimal modifiedResult = invokeCalculateRefundAmount(refund);

        // Then - 结果不受影响
        assertEquals(0, originalResult.compareTo(modifiedResult),
                "修改 promotionalRebatesTax 不应影响 calculateRefundAmount 的结果");
    }

    /**
     * 属性 2 补充：费用类字段不参与计算
     * sellingFees/fbaFees/otherTransactionFees/other/marketplaceWithheldTax 不影响结果
     *
     * <p><b>Validates: Requirements 2.4</b></p>
     */
    @Property(tries = 100)
    void testCalculateRefundAmount_FeeFieldsChanged_ShouldNotAffectResult(
            @ForAll("refundSalesData") SalesData refund,
            @ForAll @BigRange(min = "-9999.99", max = "9999.99") @Scale(2) BigDecimal newFee
    ) throws Exception {
        // Given - 计算原始结果
        BigDecimal originalResult = invokeCalculateRefundAmount(refund);

        // When - 修改所有费用类字段为同一个随机值
        refund.setSellingFees(newFee);
        refund.setFbaFees(newFee);
        refund.setOtherTransactionFees(newFee);
        refund.setOther(newFee);
        refund.setMarketplaceWithheldTax(newFee);
        BigDecimal modifiedResult = invokeCalculateRefundAmount(refund);

        // Then - 结果不受影响
        assertEquals(0, originalResult.compareTo(modifiedResult),
                "修改费用类字段（sellingFees/fbaFees/otherTransactionFees/other/marketplaceWithheldTax）不应影响结果");
    }

    /**
     * 属性 2 补充：全部字段为 null 时应返回 ZERO
     *
     * <p><b>Validates: Requirements 2.1, 2.2</b></p>
     */
    @Example
    void testCalculateRefundAmount_AllFieldsNull_ShouldReturnZero() throws Exception {
        // Given - 所有金额字段为 null 的退款记录
        SalesData refund = new SalesData();
        refund.setTransactionCategory("refund");

        // When
        BigDecimal result = invokeCalculateRefundAmount(refund);

        // Then
        assertEquals(0, BigDecimal.ZERO.compareTo(result),
                "所有字段为 null 时应返回 ZERO");
    }

    /**
     * 属性 2 补充：正常 9 项全部有值的具体示例
     * 与设计文档中的示例一致
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void testCalculateRefundAmount_AllNineFieldsSet_ShouldReturnCorrectSum() throws Exception {
        // Given - 设计文档中的示例数据
        SalesData refund = new SalesData();
        refund.setTransactionCategory("refund");
        refund.setProductSales(new BigDecimal("-29.99"));
        refund.setProductSalesTax(new BigDecimal("-2.40"));
        refund.setShippingCredits(new BigDecimal("-3.99"));
        refund.setShippingCreditsTax(new BigDecimal("-0.32"));
        refund.setGiftWrapCredits(BigDecimal.ZERO);
        refund.setGiftWrapCreditsTax(BigDecimal.ZERO);
        refund.setRegulatoryFee(new BigDecimal("-0.25"));
        refund.setRegulatoryFeeTax(new BigDecimal("-0.02"));
        refund.setPromotionalRebates(new BigDecimal("5.00"));
        // 不参与计算的字段（设置非零值验证不影响结果）
        refund.setPromotionalRebatesTax(new BigDecimal("-1.50"));
        refund.setSellingFees(new BigDecimal("-10.00"));
        refund.setFbaFees(new BigDecimal("-5.00"));

        // When
        BigDecimal result = invokeCalculateRefundAmount(refund);

        // Then
        // -29.99 + (-2.40) + (-3.99) + (-0.32) + 0 + 0 + (-0.25) + (-0.02) + 5.00 = -31.97
        BigDecimal expected = new BigDecimal("-31.97");
        assertEquals(0, expected.compareTo(result),
                String.format("9 项求和应为 -31.97，实际为 %s", result));
    }
}
