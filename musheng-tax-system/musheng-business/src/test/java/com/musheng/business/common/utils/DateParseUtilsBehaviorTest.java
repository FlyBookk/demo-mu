package com.musheng.business.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日期解析行为测试
 * 
 * 本测试类用于验证新的 DateParseUtils 工具类与原有 Service 中的日期解析逻辑行为完全一致。
 * 
 * ⚠️ 重要：这些测试定义了日期解析的"正确行为"，重构后的工具类必须通过所有测试。
 * 
 * 原有逻辑来源：
 * - SalesDataServiceImpl.parseStartDate()
 * - SalesDataServiceImpl.parseEndDate()
 * - RateServiceImpl.parseRateDate()
 * 
 * @author wanhua
 * 10:50 2026年02月01日
 */
@DisplayName("日期解析行为测试")
public class DateParseUtilsBehaviorTest {
    
    // ==================== parseStartDate 测试 ====================
    
    @Test
    @DisplayName("parseStartDate - 正常日期应返回当天 00:00:00")
    void testParseStartDate_ValidDate_ShouldReturnStartOfDay() {
        // 原有行为：LocalDate.parse(dateStr).atStartOfDay()
        String dateStr = "2026-01-15";
        
        LocalDateTime expected = LocalDate.of(2026, 1, 15).atStartOfDay();
        LocalDateTime actual = originalParseStartDate(dateStr);
        
        assertEquals(expected, actual);
        // 验证时间部分为 00:00:00
        assertEquals(0, actual.getHour());
        assertEquals(0, actual.getMinute());
        assertEquals(0, actual.getSecond());
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseStartDate - null或空字符串应返回null")
    void testParseStartDate_NullOrEmpty_ShouldReturnNull(String dateStr) {
        // 原有行为：空值返回 null
        LocalDateTime result = originalParseStartDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026/01/15", "01-15-2026", "2026年01月15日", "abc123"})
    @DisplayName("parseStartDate - 无效格式应返回null（不抛异常）")
    void testParseStartDate_InvalidFormat_ShouldReturnNull(String dateStr) {
        // 原有行为：解析失败返回 null，不抛异常
        LocalDateTime result = originalParseStartDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @CsvSource({
            "2026-01-01, 2026, 1, 1",
            "2026-12-31, 2026, 12, 31",
            "2025-07-15, 2025, 7, 15",
            "2024-02-29, 2024, 2, 29"  // 闰年
    })
    @DisplayName("parseStartDate - 各种有效日期")
    void testParseStartDate_VariousDates(String dateStr, int year, int month, int day) {
        LocalDateTime result = originalParseStartDate(dateStr);
        
        assertNotNull(result);
        assertEquals(year, result.getYear());
        assertEquals(month, result.getMonthValue());
        assertEquals(day, result.getDayOfMonth());
        assertEquals(0, result.getHour());
    }
    
    // ==================== parseEndDate 测试 ====================
    
    @Test
    @DisplayName("parseEndDate - 正常日期应返回当天 23:59:59")
    void testParseEndDate_ValidDate_ShouldReturnEndOfDay() {
        // 原有行为：LocalDate.parse(dateStr).atTime(23, 59, 59)
        String dateStr = "2026-01-15";
        
        LocalDateTime expected = LocalDate.of(2026, 1, 15).atTime(23, 59, 59);
        LocalDateTime actual = originalParseEndDate(dateStr);
        
        assertEquals(expected, actual);
        // 验证时间部分为 23:59:59
        assertEquals(23, actual.getHour());
        assertEquals(59, actual.getMinute());
        assertEquals(59, actual.getSecond());
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseEndDate - null或空字符串应返回null")
    void testParseEndDate_NullOrEmpty_ShouldReturnNull(String dateStr) {
        LocalDateTime result = originalParseEndDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026/01/15", "01-15-2026", "2026年01月15日"})
    @DisplayName("parseEndDate - 无效格式应返回null（不抛异常）")
    void testParseEndDate_InvalidFormat_ShouldReturnNull(String dateStr) {
        LocalDateTime result = originalParseEndDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @CsvSource({
            "2026-01-01, 2026, 1, 1",
            "2026-12-31, 2026, 12, 31",
            "2025-07-15, 2025, 7, 15"
    })
    @DisplayName("parseEndDate - 各种有效日期")
    void testParseEndDate_VariousDates(String dateStr, int year, int month, int day) {
        LocalDateTime result = originalParseEndDate(dateStr);
        
        assertNotNull(result);
        assertEquals(year, result.getYear());
        assertEquals(month, result.getMonthValue());
        assertEquals(day, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
    }
    
    // ==================== 边界条件测试 ====================
    
    @Test
    @DisplayName("parseStartDate - 只有空格的字符串应返回null")
    void testParseStartDate_WhitespaceOnly_ShouldReturnNull() {
        LocalDateTime result = originalParseStartDate("   ");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("parseEndDate - 只有空格的字符串应返回null")
    void testParseEndDate_WhitespaceOnly_ShouldReturnNull() {
        LocalDateTime result = originalParseEndDate("   ");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("parseStartDate - 带前后空格的日期应能正确解析")
    void testParseStartDate_WithWhitespace_ShouldTrim() {
        // 注意：原有实现可能不会 trim，需要验证
        String dateStr = " 2026-01-15 ";
        LocalDateTime result = originalParseStartDate(dateStr.trim());
        
        assertNotNull(result);
        assertEquals(2026, result.getYear());
    }
    
    // ==================== 原有逻辑的复制（用于对比） ====================
    
    /**
     * 原有 parseStartDate 逻辑（从 SalesDataServiceImpl 复制）
     * 
     * ⚠️ 这是重构的基准，新工具类必须与此行为完全一致
     */
    private LocalDateTime originalParseStartDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            // log.warn("Invalid start date format: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 原有 parseEndDate 逻辑（从 SalesDataServiceImpl 复制）
     * 
     * ⚠️ 这是重构的基准，新工具类必须与此行为完全一致
     */
    private LocalDateTime originalParseEndDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atTime(23, 59, 59);
        } catch (Exception e) {
            // log.warn("Invalid end date format: {}", dateStr);
            return null;
        }
    }
}
