package com.musheng.business.rate.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.mapper.CurrencyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CSV 汇率导入集成测试
 * 
 * 验证重构后的 RateCsvImportStrategy 与原有逻辑完全一致。
 * 
 * 测试场景：
 * 1. 正常导入 - 验证解析和保存逻辑
 * 2. 多种日期格式 - 验证日期解析兼容性
 * 3. 未配置货币跳过 - 验证货币过滤逻辑
 * 4. 重复数据处理 - 验证去重逻辑
 * 5. 错误数据处理 - 验证错误处理逻辑
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CSV 汇率导入集成测试")
class RateCsvImportIntegrationTest {
    
    @Mock
    private CurrencyMapper currencyMapper;
    
    @Mock
    private ExchangeRateMapper exchangeRateMapper;
    
    private RateCsvImportStrategy csvStrategy;
    
    @BeforeEach
    void setUp() {
        csvStrategy = new RateCsvImportStrategy(currencyMapper, exchangeRateMapper);
        
        // 模拟已配置的货币列表（USD, EUR, GBP 已配置，JPY 未配置）
        List<Currency> configuredCurrencies = new ArrayList<>();
        configuredCurrencies.add(createCurrency("USD"));
        configuredCurrencies.add(createCurrency("EUR"));
        configuredCurrencies.add(createCurrency("GBP"));
        
        when(currencyMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(configuredCurrencies);
        
        // 模拟数据库中没有已存在的汇率记录
        when(exchangeRateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>());
        
        // 模拟插入操作
        when(exchangeRateMapper.insert(any(ExchangeRate.class))).thenReturn(1);
    }
    
    private Currency createCurrency(String code) {
        Currency currency = new Currency();
        currency.setCurrencyCode(code);
        currency.setStatus(1);
        return currency;
    }
    
    @Nested
    @DisplayName("正常导入场景")
    class NormalImportTest {
        
        @Test
        @DisplayName("应该正确解析标准 CSV 格式并导入")
        void testImportAndSave_StandardCsv_ShouldParseAndSave() {
            // Given: 标准 CSV 格式文件
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    2026-01-15,EUR,7.8900
                    2026-01-16,USD,7.2600
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When: 执行导入
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then: 验证结果
            assertEquals(3, result.get("totalCount"), "总行数应为 3");
            assertEquals(3, result.get("successCount"), "成功数应为 3");
            assertEquals(0, result.get("failCount"), "失败数应为 0");
            assertEquals(0, result.get("skipCount"), "跳过数应为 0");
            
            // 验证插入了 3 条记录
            verify(exchangeRateMapper, times(3)).insert(any(ExchangeRate.class));
        }
        
        @Test
        @DisplayName("应该正确设置汇率实体的所有字段")
        void testImportAndSave_ShouldSetAllFields() {
            // Given
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            csvStrategy.importAndSave(file, new ImportContext());
            
            // Then: 捕获插入的实体并验证字段
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            
            ExchangeRate savedRate = captor.getValue();
            assertEquals(LocalDate.of(2026, 1, 15), savedRate.getRateDate());
            assertEquals("USD", savedRate.getCurrencyCode());
            assertEquals(new BigDecimal("7.2500"), savedRate.getRate());
            assertEquals("IMPORT", savedRate.getSource());
            // 2026-01-15 是周四，应该是工作日
            assertEquals(1, savedRate.getIsWorkday());
        }
        
        @Test
        @DisplayName("周末日期应该标记为非工作日")
        void testImportAndSave_Weekend_ShouldMarkAsNonWorkday() {
            // Given: 2026-01-17 是周六
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-17,USD,7.2700
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            
            ExchangeRate savedRate = captor.getValue();
            assertEquals(0, savedRate.getIsWorkday(), "周六应该标记为非工作日");
        }
    }
    
    @Nested
    @DisplayName("日期格式兼容性测试")
    class DateFormatTest {
        
        @Test
        @DisplayName("应该支持 yyyy-MM-dd 格式")
        void testParseDate_YyyyMmDd() {
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 15), captor.getValue().getRateDate());
        }
        
        @Test
        @DisplayName("应该支持 yyyy/MM/dd 格式")
        void testParseDate_YyyySlashMmDd() {
            String csvContent = """
                    rate_date,currency_code,rate
                    2026/01/18,USD,7.2800
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 18), captor.getValue().getRateDate());
        }
        
        @Test
        @DisplayName("应该支持 yyyyMMdd 格式")
        void testParseDate_YyyyMmDdNoSeparator() {
            String csvContent = """
                    rate_date,currency_code,rate
                    20260119,USD,7.2900
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 19), captor.getValue().getRateDate());
        }
        
        @Test
        @DisplayName("应该支持 MM/dd/yyyy 格式")
        void testParseDate_MmDdYyyy() {
            String csvContent = """
                    rate_date,currency_code,rate
                    01/20/2026,USD,7.3000
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 20), captor.getValue().getRateDate());
        }
    }
    
    @Nested
    @DisplayName("货币过滤测试")
    class CurrencyFilterTest {
        
        @Test
        @DisplayName("未配置的货币应该被跳过")
        void testImportAndSave_UnconfiguredCurrency_ShouldSkip() {
            // Given: JPY 未配置
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    2026-01-15,JPY,0.0480
                    2026-01-15,EUR,7.8900
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(3, result.get("totalCount"), "总行数应为 3");
            assertEquals(2, result.get("successCount"), "成功数应为 2（USD 和 EUR）");
            assertEquals(1, result.get("skipCount"), "跳过数应为 1（JPY）");
            
            // 验证只插入了 2 条记录
            verify(exchangeRateMapper, times(2)).insert(any(ExchangeRate.class));
        }
        
        @Test
        @DisplayName("应该记录被跳过的货币")
        @SuppressWarnings("unchecked")
        void testImportAndSave_ShouldRecordSkippedCurrencies() {
            // Given
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,JPY,0.0480
                    2026-01-15,CAD,5.2000
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("skipCount"));
            assertTrue(result.containsKey("skippedCurrencies"));
            
            java.util.Set<String> skipped = (java.util.Set<String>) result.get("skippedCurrencies");
            assertTrue(skipped.contains("JPY"));
            assertTrue(skipped.contains("CAD"));
        }
        
        @Test
        @DisplayName("货币代码应该不区分大小写")
        void testImportAndSave_CurrencyCode_CaseInsensitive() {
            // Given: 小写货币代码
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,usd,7.2500
                    2026-01-15,Eur,7.8900
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("successCount"), "应该成功导入 2 条");
            
            // 验证货币代码被转换为大写
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper, times(2)).insert(captor.capture());
            
            List<ExchangeRate> savedRates = captor.getAllValues();
            assertTrue(savedRates.stream().allMatch(r -> r.getCurrencyCode().equals(r.getCurrencyCode().toUpperCase())));
        }
    }
    
    @Nested
    @DisplayName("重复数据处理测试")
    class DuplicateHandlingTest {
        
        @Test
        @DisplayName("文件内重复数据应该被跳过")
        void testImportAndSave_DuplicateInFile_ShouldSkip() {
            // Given: 同一日期同一货币出现两次
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    2026-01-15,USD,7.2600
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("totalCount"));
            assertEquals(1, result.get("successCount"), "只有第一条应该成功");
            assertEquals(1, result.get("failCount"), "第二条应该失败（重复）");
        }
        
        @Test
        @DisplayName("数据库中已存在的数据应该被跳过")
        void testImportAndSave_ExistsInDb_ShouldSkip() {
            // Given: 模拟数据库中已存在 2026-01-15 USD 的汇率
            ExchangeRate existingRate = new ExchangeRate();
            existingRate.setRateDate(LocalDate.of(2026, 1, 15));
            existingRate.setCurrencyCode("USD");
            existingRate.setRate(new BigDecimal("7.2400"));
            
            when(exchangeRateMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(existingRate));
            
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,7.2500
                    2026-01-16,USD,7.2600
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("totalCount"));
            assertEquals(1, result.get("successCount"), "只有 01-16 应该成功");
            
            // existsCount 是 AtomicInteger，需要特殊处理
            Object existsCountObj = result.get("existsCount");
            int existsCount = existsCountObj instanceof java.util.concurrent.atomic.AtomicInteger 
                    ? ((java.util.concurrent.atomic.AtomicInteger) existsCountObj).get() 
                    : (int) existsCountObj;
            assertEquals(1, existsCount, "01-15 应该被标记为已存在");
            
            // 验证只插入了 1 条记录
            verify(exchangeRateMapper, times(1)).insert(any(ExchangeRate.class));
        }
    }
    
    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTest {
        
        @Test
        @DisplayName("无效日期格式应该记录错误")
        @SuppressWarnings("unchecked")
        void testImportAndSave_InvalidDate_ShouldRecordError() {
            // Given
            String csvContent = """
                    rate_date,currency_code,rate
                    invalid-date,USD,7.2500
                    2026-01-15,EUR,7.8900
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("totalCount"));
            assertEquals(1, result.get("successCount"));
            assertEquals(1, result.get("failCount"));
            
            List<String> errors = (List<String>) result.get("errors");
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("Row 1"));
        }
        
        @Test
        @DisplayName("无效汇率格式应该记录错误")
        @SuppressWarnings("unchecked")
        void testImportAndSave_InvalidRate_ShouldRecordError() {
            // Given
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,not-a-number
                    2026-01-15,EUR,7.8900
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("totalCount"));
            assertEquals(1, result.get("successCount"));
            assertEquals(1, result.get("failCount"));
            
            List<String> errors = (List<String>) result.get("errors");
            assertFalse(errors.isEmpty());
        }
        
        @Test
        @DisplayName("带千分位分隔符的汇率应该正确解析")
        void testImportAndSave_RateWithComma_ShouldParse() {
            // Given: 汇率带千分位分隔符
            String csvContent = """
                    rate_date,currency_code,rate
                    2026-01-15,USD,"7,250.00"
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(new BigDecimal("7250.00"), captor.getValue().getRate());
        }
    }
    
    @Nested
    @DisplayName("列名兼容性测试")
    class ColumnNameTest {
        
        @Test
        @DisplayName("应该支持中文列名")
        void testImportAndSave_ChineseColumnNames() {
            // Given
            String csvContent = """
                    日期,货币,汇率
                    2026-01-15,USD,7.2500
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
        
        @Test
        @DisplayName("应该支持 date/currency/exchange_rate 列名")
        void testImportAndSave_AlternativeColumnNames() {
            // Given
            String csvContent = """
                    date,currency,exchange_rate
                    2026-01-15,USD,7.2500
                    """;
            MockMultipartFile file = createCsvFile("rates.csv", csvContent);
            
            // When
            Map<String, Object> result = csvStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
    }
    
    /**
     * 创建 CSV 测试文件
     */
    private MockMultipartFile createCsvFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
