package com.musheng.business.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateParseUtils 单元测试
 * 
 * 验证 DateParseUtils 工具类的行为与原有 Service 中的日期解析逻辑完全一致。
 * 
 * @author wanhua
 * 18:15 2026年02月01日
 */
@DisplayName("DateParseUtils 单元测试")
class DateParseUtilsTest {
    
    // ==================== parseStartDate 测试 ====================
    
    @Test
    @DisplayName("parseStartDate - 正常日期应返回当天 00:00:00")
    void testParseStartDate_ValidDate_ShouldReturnStartOfDay() {
        String dateStr = "2026-01-15";
        
        LocalDateTime result = DateParseUtils.parseStartDate(dateStr);
        
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseStartDate - null或空字符串应返回null")
    void testParseStartDate_NullOrEmpty_ShouldReturnNull(String dateStr) {
        LocalDateTime result = DateParseUtils.parseStartDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026/01/15", "01-15-2026", "2026年01月15日", "abc123"})
    @DisplayName("parseStartDate - 无效格式应返回null")
    void testParseStartDate_InvalidFormat_ShouldReturnNull(String dateStr) {
        LocalDateTime result = DateParseUtils.parseStartDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @CsvSource({
            "2026-01-01, 2026, 1, 1",
            "2026-12-31, 2026, 12, 31",
            "2025-07-15, 2025, 7, 15",
            "2024-02-29, 2024, 2, 29"
    })
    @DisplayName("parseStartDate - 各种有效日期")
    void testParseStartDate_VariousDates(String dateStr, int year, int month, int day) {
        LocalDateTime result = DateParseUtils.parseStartDate(dateStr);
        
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
        String dateStr = "2026-01-15";
        
        LocalDateTime result = DateParseUtils.parseEndDate(dateStr);
        
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseEndDate - null或空字符串应返回null")
    void testParseEndDate_NullOrEmpty_ShouldReturnNull(String dateStr) {
        LocalDateTime result = DateParseUtils.parseEndDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026/01/15", "01-15-2026", "2026年01月15日"})
    @DisplayName("parseEndDate - 无效格式应返回null")
    void testParseEndDate_InvalidFormat_ShouldReturnNull(String dateStr) {
        LocalDateTime result = DateParseUtils.parseEndDate(dateStr);
        
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
        LocalDateTime result = DateParseUtils.parseEndDate(dateStr);
        
        assertNotNull(result);
        assertEquals(year, result.getYear());
        assertEquals(month, result.getMonthValue());
        assertEquals(day, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
    }
    
    // ==================== parseDate 测试 ====================
    
    @Test
    @DisplayName("parseDate - 正常日期应返回 LocalDate")
    void testParseDate_ValidDate_ShouldReturnLocalDate() {
        String dateStr = "2026-01-15";
        
        LocalDate result = DateParseUtils.parseDate(dateStr);
        
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseDate - null或空字符串应返回null")
    void testParseDate_NullOrEmpty_ShouldReturnNull(String dateStr) {
        LocalDate result = DateParseUtils.parseDate(dateStr);
        
        assertNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026/01/15", "01-15-2026"})
    @DisplayName("parseDate - 无效格式应返回null")
    void testParseDate_InvalidFormat_ShouldReturnNull(String dateStr) {
        LocalDate result = DateParseUtils.parseDate(dateStr);
        
        assertNull(result);
    }
    
    // ==================== isValidDate 测试 ====================
    
    @Test
    @DisplayName("isValidDate - 有效日期应返回true")
    void testIsValidDate_ValidDate_ShouldReturnTrue() {
        assertTrue(DateParseUtils.isValidDate("2026-01-15"));
        assertTrue(DateParseUtils.isValidDate("2024-02-29")); // 闰年
    }
    
    @Test
    @DisplayName("isValidDate - 无效日期应返回false")
    void testIsValidDate_InvalidDate_ShouldReturnFalse() {
        assertFalse(DateParseUtils.isValidDate(null));
        assertFalse(DateParseUtils.isValidDate(""));
        assertFalse(DateParseUtils.isValidDate("invalid"));
        assertFalse(DateParseUtils.isValidDate("2025-02-29")); // 非闰年
    }
    
    // ==================== 边界条件测试 ====================
    
    @Test
    @DisplayName("parseStartDate - 只有空格的字符串应返回null")
    void testParseStartDate_WhitespaceOnly_ShouldReturnNull() {
        LocalDateTime result = DateParseUtils.parseStartDate("   ");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("parseEndDate - 只有空格的字符串应返回null")
    void testParseEndDate_WhitespaceOnly_ShouldReturnNull() {
        LocalDateTime result = DateParseUtils.parseEndDate("   ");
        
        assertNull(result);
    }
}
