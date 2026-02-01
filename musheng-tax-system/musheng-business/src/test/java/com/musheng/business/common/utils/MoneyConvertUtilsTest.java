package com.musheng.business.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MoneyConvertUtils 单元测试
 *
 * @author wanhua
 * 18:55 2026年02月01日
 */
@DisplayName("MoneyConvertUtils 单元测试")
class MoneyConvertUtilsTest {

    @Nested
    @DisplayName("convertToCny 测试")
    class ConvertToCnyTest {

        @Test
        @DisplayName("正常转换 - 应返回正确的人民币金额")
        void testNormalConversion() {
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal exchangeRate = new BigDecimal("7.25");
            
            BigDecimal result = MoneyConvertUtils.convertToCny(amount, exchangeRate);
            
            assertEquals(new BigDecimal("725.0000"), result);
        }

        @Test
        @DisplayName("金额为null - 应返回0")
        void testNullAmount() {
            BigDecimal exchangeRate = new BigDecimal("7.25");
            
            BigDecimal result = MoneyConvertUtils.convertToCny(null, exchangeRate);
            
            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("汇率为null - 应返回0")
        void testNullExchangeRate() {
            BigDecimal amount = new BigDecimal("100.00");
            
            BigDecimal result = MoneyConvertUtils.convertToCny(amount, null);
            
            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("汇率为0 - 应返回0")
        void testZeroExchangeRate() {
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal exchangeRate = BigDecimal.ZERO;
            
            BigDecimal result = MoneyConvertUtils.convertToCny(amount, exchangeRate);
            
            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("金额为0 - 应返回0")
        void testZeroAmount() {
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal exchangeRate = new BigDecimal("7.25");
            
            BigDecimal result = MoneyConvertUtils.convertToCny(amount, exchangeRate);
            
            assertEquals(0, result.compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("负数金额 - 应返回负数结果")
        void testNegativeAmount() {
            BigDecimal amount = new BigDecimal("-100.00");
            BigDecimal exchangeRate = new BigDecimal("7.25");
            
            BigDecimal result = MoneyConvertUtils.convertToCny(amount, exchangeRate);
            
            assertTrue(result.compareTo(BigDecimal.ZERO) < 0);
        }
    }

    @Nested
    @DisplayName("convertToCnyWithScale 测试")
    class ConvertToCnyWithScaleTest {

        @Test
        @DisplayName("正常转换带精度 - 应返回2位小数")
        void testNormalConversionWithScale() {
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal exchangeRate = new BigDecimal("7.2567");
            
            BigDecimal result = MoneyConvertUtils.convertToCnyWithScale(amount, exchangeRate);
            
            assertEquals(2, result.scale());
            assertEquals(new BigDecimal("725.67"), result);
        }

        @Test
        @DisplayName("四舍五入 - 应正确舍入")
        void testRounding() {
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal exchangeRate = new BigDecimal("7.2564");
            
            BigDecimal result = MoneyConvertUtils.convertToCnyWithScale(amount, exchangeRate);
            
            assertEquals(new BigDecimal("725.64"), result);
        }
    }

    @Nested
    @DisplayName("safeAdd 测试")
    class SafeAddTest {

        @Test
        @DisplayName("正常加法 - 应返回正确结果")
        void testNormalAdd() {
            BigDecimal a = new BigDecimal("100.00");
            BigDecimal b = new BigDecimal("50.00");
            
            BigDecimal result = MoneyConvertUtils.safeAdd(a, b);
            
            assertEquals(new BigDecimal("150.00"), result);
        }

        @Test
        @DisplayName("第一个参数为null - 应视为0")
        void testFirstNull() {
            BigDecimal b = new BigDecimal("50.00");
            
            BigDecimal result = MoneyConvertUtils.safeAdd(null, b);
            
            assertEquals(new BigDecimal("50.00"), result);
        }

        @Test
        @DisplayName("第二个参数为null - 应视为0")
        void testSecondNull() {
            BigDecimal a = new BigDecimal("100.00");
            
            BigDecimal result = MoneyConvertUtils.safeAdd(a, null);
            
            assertEquals(new BigDecimal("100.00"), result);
        }

        @Test
        @DisplayName("两个参数都为null - 应返回0")
        void testBothNull() {
            BigDecimal result = MoneyConvertUtils.safeAdd(null, null);
            
            assertEquals(BigDecimal.ZERO, result);
        }
    }

    @Nested
    @DisplayName("safeSubtract 测试")
    class SafeSubtractTest {

        @Test
        @DisplayName("正常减法 - 应返回正确结果")
        void testNormalSubtract() {
            BigDecimal a = new BigDecimal("100.00");
            BigDecimal b = new BigDecimal("30.00");
            
            BigDecimal result = MoneyConvertUtils.safeSubtract(a, b);
            
            assertEquals(new BigDecimal("70.00"), result);
        }

        @Test
        @DisplayName("被减数为null - 应视为0")
        void testFirstNull() {
            BigDecimal b = new BigDecimal("30.00");
            
            BigDecimal result = MoneyConvertUtils.safeSubtract(null, b);
            
            assertEquals(new BigDecimal("-30.00"), result);
        }
    }

    @Nested
    @DisplayName("isPositive 测试")
    class IsPositiveTest {

        @Test
        @DisplayName("正数 - 应返回true")
        void testPositive() {
            assertTrue(MoneyConvertUtils.isPositive(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("负数 - 应返回false")
        void testNegative() {
            assertFalse(MoneyConvertUtils.isPositive(new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("零 - 应返回false")
        void testZero() {
            assertFalse(MoneyConvertUtils.isPositive(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("null - 应返回false")
        void testNull() {
            assertFalse(MoneyConvertUtils.isPositive(null));
        }
    }

    @Nested
    @DisplayName("isNegative 测试")
    class IsNegativeTest {

        @Test
        @DisplayName("负数 - 应返回true")
        void testNegative() {
            assertTrue(MoneyConvertUtils.isNegative(new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("正数 - 应返回false")
        void testPositive() {
            assertFalse(MoneyConvertUtils.isNegative(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("零 - 应返回false")
        void testZero() {
            assertFalse(MoneyConvertUtils.isNegative(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("null - 应返回false")
        void testNull() {
            assertFalse(MoneyConvertUtils.isNegative(null));
        }
    }

    @Nested
    @DisplayName("isZeroOrNull 测试")
    class IsZeroOrNullTest {

        @Test
        @DisplayName("零 - 应返回true")
        void testZero() {
            assertTrue(MoneyConvertUtils.isZeroOrNull(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("null - 应返回true")
        void testNull() {
            assertTrue(MoneyConvertUtils.isZeroOrNull(null));
        }

        @Test
        @DisplayName("正数 - 应返回false")
        void testPositive() {
            assertFalse(MoneyConvertUtils.isZeroOrNull(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("负数 - 应返回false")
        void testNegative() {
            assertFalse(MoneyConvertUtils.isZeroOrNull(new BigDecimal("-100.00")));
        }
    }
}
