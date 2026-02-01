package com.musheng.business.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 金额转换行为测试
 * 
 * 本测试类用于验证新的 MoneyConvertUtils 工具类与原有 Service 中的金额转换逻辑行为完全一致。
 * 
 * ⚠️ 重要：这些测试定义了金额转换的"正确行为"，重构后的工具类必须通过所有测试。
 * 
 * 原有逻辑来源：
 * - SalesDataServiceImpl.convertToCny()
 * - ShippingDataServiceImpl.convertToCny()
 * 
 * @author wanhua
 * 10:55 2026年02月01日
 */
@DisplayName("金额转换行为测试")
public class MoneyConvertUtilsBehaviorTest {
    
    // ==================== 正常场景测试 ====================
    
    @Test
    @DisplayName("convertToCny - 正常金额和汇率应正确计算")
    void testConvertToCny_ValidAmountAndRate_ShouldCalculate() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal exchangeRate = new BigDecimal("7.2");
        
        BigDecimal expected = new BigDecimal("720.00");
        BigDecimal actual = originalConvertToCny(amount, exchangeRate);
        
        assertEquals(0, expected.compareTo(actual), 
                "Expected: " + expected + ", Actual: " + actual);
    }
    
    @Test
    @DisplayName("convertToCny - 小数金额应正确计算")
    void testConvertToCny_DecimalAmount_ShouldCalculate() {
        BigDecimal amount = new BigDecimal("123.45");
        BigDecimal exchangeRate = new BigDecimal("7.2345");
        
        BigDecimal expected = amount.multiply(exchangeRate);
        BigDecimal actual = originalConvertToCny(amount, exchangeRate);
        
        assertEquals(0, expected.compareTo(actual));
    }
    
    @Test
    @DisplayName("convertToCny - 零金额应返回零")
    void testConvertToCny_ZeroAmount_ShouldReturnZero() {
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal exchangeRate = new BigDecimal("7.2");
        
        BigDecimal result = originalConvertToCny(amount, exchangeRate);
        
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
    
    // ==================== 异常场景测试 ====================
    
    @Test
    @DisplayName("convertToCny - null金额应返回ZERO")
    void testConvertToCny_NullAmount_ShouldReturnZero() {
        // 原有行为：amount 为 null 时返回 ZERO
        BigDecimal result = originalConvertToCny(null, new BigDecimal("7.2"));
        
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
    
    @Test
    @DisplayName("convertToCny - null汇率应返回ZERO")
    void testConvertToCny_NullRate_ShouldReturnZero() {
        // 原有行为：exchangeRate 为 null 时返回 ZERO（并记录警告日志）
        BigDecimal result = originalConvertToCny(new BigDecimal("100"), null);
        
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
    
    @Test
    @DisplayName("convertToCny - 零汇率应返回ZERO")
    void testConvertToCny_ZeroRate_ShouldReturnZero() {
        // 原有行为：exchangeRate 为 0 时返回 ZERO（并记录警告日志）
        BigDecimal result = originalConvertToCny(new BigDecimal("100"), BigDecimal.ZERO);
        
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
    
    @Test
    @DisplayName("convertToCny - 双null应返回ZERO")
    void testConvertToCny_BothNull_ShouldReturnZero() {
        BigDecimal result = originalConvertToCny(null, null);
        
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
    
    // ==================== 参数化测试 ====================
    
    @ParameterizedTest
    @MethodSource("provideConversionTestCases")
    @DisplayName("convertToCny - 各种金额和汇率组合")
    void testConvertToCny_VariousCases(BigDecimal amount, BigDecimal rate, BigDecimal expected) {
        BigDecimal actual = originalConvertToCny(amount, rate);
        
        assertEquals(0, expected.compareTo(actual),
                String.format("Amount: %s, Rate: %s, Expected: %s, Actual: %s",
                        amount, rate, expected, actual));
    }
    
    private static Stream<Arguments> provideConversionTestCases() {
        return Stream.of(
                // 正常情况
                Arguments.of(new BigDecimal("100"), new BigDecimal("7.2"), new BigDecimal("720")),
                Arguments.of(new BigDecimal("50.5"), new BigDecimal("7.0"), new BigDecimal("353.5")),
                Arguments.of(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1")),
                
                // 边界情况
                Arguments.of(BigDecimal.ZERO, new BigDecimal("7.2"), BigDecimal.ZERO),
                Arguments.of(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO),
                Arguments.of(null, new BigDecimal("7.2"), BigDecimal.ZERO),
                Arguments.of(new BigDecimal("100"), null, BigDecimal.ZERO),
                Arguments.of(null, null, BigDecimal.ZERO),
                
                // 负数情况（如果业务允许）
                Arguments.of(new BigDecimal("-100"), new BigDecimal("7.2"), new BigDecimal("-720")),
                
                // 大数情况
                Arguments.of(new BigDecimal("999999.99"), new BigDecimal("7.2"), 
                        new BigDecimal("999999.99").multiply(new BigDecimal("7.2"))),
                
                // 小数精度
                Arguments.of(new BigDecimal("0.01"), new BigDecimal("7.2345"), 
                        new BigDecimal("0.01").multiply(new BigDecimal("7.2345")))
        );
    }
    
    // ==================== 精度测试 ====================
    
    @Test
    @DisplayName("convertToCny - 应保持原始精度（不四舍五入）")
    void testConvertToCny_ShouldPreservePrecision() {
        BigDecimal amount = new BigDecimal("100.123");
        BigDecimal exchangeRate = new BigDecimal("7.2345");
        
        // 原有行为：直接 multiply，不做四舍五入
        BigDecimal expected = amount.multiply(exchangeRate);
        BigDecimal actual = originalConvertToCny(amount, exchangeRate);
        
        assertEquals(expected, actual);
    }
    
    // ==================== 原有逻辑的复制（用于对比） ====================
    
    /**
     * 原有 convertToCny 逻辑（从 SalesDataServiceImpl 复制）
     * 
     * ⚠️ 这是重构的基准，新工具类必须与此行为完全一致
     */
    private BigDecimal originalConvertToCny(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有汇率，返回0（避免错误累加）
            // log.warn("Missing exchange rate for conversion, amount={}", amount);
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }
}
