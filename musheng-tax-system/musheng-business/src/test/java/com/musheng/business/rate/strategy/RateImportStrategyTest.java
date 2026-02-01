package com.musheng.business.rate.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 汇率导入策略单元测试
 * 
 * 测试策略类的公共方法和文件类型判断逻辑。
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
class RateImportStrategyTest {
    
    @Nested
    @DisplayName("RateCsvImportStrategy 测试")
    class CsvStrategyTest {
        
        @Test
        @DisplayName("supports() - 应该支持 .csv 文件")
        void testSupports_CsvFile_ShouldReturnTrue() {
            RateCsvImportStrategy strategy = new RateCsvImportStrategy(null, null);
            
            assertTrue(strategy.supports("rates.csv"));
            assertTrue(strategy.supports("RATES.CSV"));
            assertTrue(strategy.supports("exchange_rates.csv"));
        }
        
        @Test
        @DisplayName("supports() - 不应该支持非 CSV 文件")
        void testSupports_NonCsvFile_ShouldReturnFalse() {
            RateCsvImportStrategy strategy = new RateCsvImportStrategy(null, null);
            
            assertFalse(strategy.supports("rates.xlsx"));
            assertFalse(strategy.supports("rates.xls"));
            assertFalse(strategy.supports("rates.txt"));
            assertFalse(strategy.supports(null));
        }
    }
    
    @Nested
    @DisplayName("RateExcelImportStrategy 测试")
    class ExcelStrategyTest {
        
        @Test
        @DisplayName("supports() - 应该支持 .xlsx 文件")
        void testSupports_XlsxFile_ShouldReturnTrue() {
            RateExcelImportStrategy strategy = new RateExcelImportStrategy(null, null);
            
            assertTrue(strategy.supports("rates.xlsx"));
            assertTrue(strategy.supports("RATES.XLSX"));
            assertTrue(strategy.supports("exchange_rates.xlsx"));
        }
        
        @Test
        @DisplayName("supports() - 应该支持 .xls 文件")
        void testSupports_XlsFile_ShouldReturnTrue() {
            RateExcelImportStrategy strategy = new RateExcelImportStrategy(null, null);
            
            assertTrue(strategy.supports("rates.xls"));
            assertTrue(strategy.supports("RATES.XLS"));
        }
        
        @Test
        @DisplayName("supports() - 不应该支持非 Excel 文件")
        void testSupports_NonExcelFile_ShouldReturnFalse() {
            RateExcelImportStrategy strategy = new RateExcelImportStrategy(null, null);
            
            assertFalse(strategy.supports("rates.csv"));
            assertFalse(strategy.supports("rates.txt"));
            assertFalse(strategy.supports(null));
        }
    }
    
    @Nested
    @DisplayName("AbstractRateImportStrategy 公共方法测试")
    class AbstractStrategyTest {
        
        // 使用具体实现类来测试抽象类的方法
        private final TestableRateImportStrategy strategy = new TestableRateImportStrategy();
        
        @Test
        @DisplayName("parseRateDate() - 应该解析 yyyy-MM-dd 格式")
        void testParseRateDate_YyyyMmDd_ShouldParse() {
            LocalDate result = strategy.testParseRateDate("2026-01-15");
            assertEquals(LocalDate.of(2026, 1, 15), result);
        }
        
        @Test
        @DisplayName("parseRateDate() - 应该解析 yyyy/MM/dd 格式")
        void testParseRateDate_YyyySlashMmDd_ShouldParse() {
            LocalDate result = strategy.testParseRateDate("2026/01/15");
            assertEquals(LocalDate.of(2026, 1, 15), result);
        }
        
        @Test
        @DisplayName("parseRateDate() - 应该解析 yyyyMMdd 格式")
        void testParseRateDate_YyyyMmDdNoSeparator_ShouldParse() {
            LocalDate result = strategy.testParseRateDate("20260115");
            assertEquals(LocalDate.of(2026, 1, 15), result);
        }
        
        @Test
        @DisplayName("parseRateDate() - 应该解析 MM/dd/yyyy 格式")
        void testParseRateDate_MmDdYyyy_ShouldParse() {
            LocalDate result = strategy.testParseRateDate("01/15/2026");
            assertEquals(LocalDate.of(2026, 1, 15), result);
        }
        
        @Test
        @DisplayName("parseRateDate() - 无效日期应该抛出异常")
        void testParseRateDate_InvalidDate_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class, () -> {
                strategy.testParseRateDate("invalid-date");
            });
        }
        
        @Test
        @DisplayName("isWeekend() - 周六应该返回 true")
        void testIsWeekend_Saturday_ShouldReturnTrue() {
            // 2026-01-17 是周六
            LocalDate saturday = LocalDate.of(2026, 1, 17);
            assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
            assertTrue(strategy.testIsWeekend(saturday));
        }
        
        @Test
        @DisplayName("isWeekend() - 周日应该返回 true")
        void testIsWeekend_Sunday_ShouldReturnTrue() {
            // 2026-01-18 是周日
            LocalDate sunday = LocalDate.of(2026, 1, 18);
            assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
            assertTrue(strategy.testIsWeekend(sunday));
        }
        
        @Test
        @DisplayName("isWeekend() - 工作日应该返回 false")
        void testIsWeekend_Weekday_ShouldReturnFalse() {
            // 2026-01-15 是周四
            LocalDate thursday = LocalDate.of(2026, 1, 15);
            assertEquals(DayOfWeek.THURSDAY, thursday.getDayOfWeek());
            assertFalse(strategy.testIsWeekend(thursday));
        }
        
        @Test
        @DisplayName("findColumnIndex() - 应该找到匹配的列")
        void testFindColumnIndex_MatchingColumn_ShouldReturnIndex() {
            List<String> headers = List.of("日期", "货币", "汇率");
            
            assertEquals(0, strategy.testFindColumnIndex(headers, "日期", "date"));
            assertEquals(1, strategy.testFindColumnIndex(headers, "currency", "货币"));
            assertEquals(2, strategy.testFindColumnIndex(headers, "rate", "汇率"));
        }
        
        @Test
        @DisplayName("findColumnIndex() - 未找到列应该返回 -1")
        void testFindColumnIndex_NoMatch_ShouldReturnMinusOne() {
            List<String> headers = List.of("日期", "货币", "汇率");
            
            assertEquals(-1, strategy.testFindColumnIndex(headers, "unknown"));
        }
        
        @Test
        @DisplayName("findColumnIndex() - 应该忽略大小写")
        void testFindColumnIndex_CaseInsensitive_ShouldMatch() {
            List<String> headers = List.of("DATE", "CURRENCY", "RATE");
            
            assertEquals(0, strategy.testFindColumnIndex(headers, "date"));
            assertEquals(1, strategy.testFindColumnIndex(headers, "currency"));
            assertEquals(2, strategy.testFindColumnIndex(headers, "rate"));
        }
    }
    
    /**
     * 可测试的策略实现，用于测试抽象类的 protected 方法
     */
    private static class TestableRateImportStrategy extends AbstractRateImportStrategy {
        
        TestableRateImportStrategy() {
            super(null, null);
        }
        
        @Override
        public boolean supports(String fileName) {
            return false;
        }
        
        @Override
        public List<com.musheng.business.rate.entity.ExchangeRate> parse(
                org.springframework.web.multipart.MultipartFile file,
                com.musheng.business.common.strategy.ImportContext context) {
            return List.of();
        }
        
        @Override
        public java.util.Map<String, Object> importAndSave(
                org.springframework.web.multipart.MultipartFile file,
                com.musheng.business.common.strategy.ImportContext context) {
            return java.util.Map.of();
        }
        
        // 暴露 protected 方法用于测试
        LocalDate testParseRateDate(String dateStr) {
            return parseRateDate(dateStr);
        }
        
        boolean testIsWeekend(LocalDate date) {
            return isWeekend(date);
        }
        
        int testFindColumnIndex(List<String> headers, String... possibleNames) {
            return findColumnIndex(headers, possibleNames);
        }
    }
}
