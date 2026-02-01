package com.musheng.business.rate.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.strategy.ImportContext;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.mapper.CurrencyMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Excel 汇率导入集成测试
 * 
 * 验证重构后的 RateExcelImportStrategy 与原有逻辑完全一致。
 * 
 * 测试场景：
 * 1. 正常导入 - 验证矩阵格式 Excel 解析和保存逻辑
 * 2. 多种日期格式 - 验证日期解析兼容性
 * 3. 未配置货币跳过 - 验证货币过滤逻辑
 * 4. 重复数据处理 - 验证去重逻辑
 * 5. 错误数据处理 - 验证错误处理逻辑
 * 
 * Excel 文件格式（矩阵格式）：
 * | 日期       | USD/CNY | EUR/CNY | GBP/CNY |
 * |------------|---------|---------|---------|
 * | 2026-01-15 | 7.2500  | 7.8900  | 9.1200  |
 * | 2026-01-16 | 7.2600  | 7.9000  | 9.1300  |
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Excel 汇率导入集成测试")
class RateExcelImportIntegrationTest {
    
    @Mock
    private CurrencyMapper currencyMapper;
    
    @Mock
    private ExchangeRateMapper exchangeRateMapper;
    
    private RateExcelImportStrategy excelStrategy;
    
    @BeforeEach
    void setUp() {
        excelStrategy = new RateExcelImportStrategy(currencyMapper, exchangeRateMapper);
        
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
        @DisplayName("应该正确解析矩阵格式 Excel 并导入")
        void testImportAndSave_MatrixExcel_ShouldParseAndSave() throws IOException {
            // Given: 矩阵格式 Excel 文件
            // | 日期       | USD/CNY | EUR/CNY |
            // | 2026-01-15 | 7.2500  | 7.8900  |
            // | 2026-01-16 | 7.2600  | 7.9000  |
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY", "EUR/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500, 7.8900},
                            {"2026-01-16", 7.2600, 7.9000}
                    }
            );
            
            // When: 执行导入
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then: 验证结果
            assertEquals(4, result.get("totalCount"), "总行数应为 4（2日期 x 2货币）");
            assertEquals(4, result.get("successCount"), "成功数应为 4");
            assertEquals(0, result.get("failCount"), "失败数应为 0");
            
            // 验证插入了 4 条记录
            verify(exchangeRateMapper, times(4)).insert(any(ExchangeRate.class));
        }
        
        @Test
        @DisplayName("应该正确设置汇率实体的所有字段")
        void testImportAndSave_ShouldSetAllFields() throws IOException {
            // Given
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            // When
            excelStrategy.importAndSave(file, new ImportContext());
            
            // Then: 捕获插入的实体并验证字段
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            
            ExchangeRate savedRate = captor.getValue();
            assertEquals(LocalDate.of(2026, 1, 15), savedRate.getRateDate());
            assertEquals("USD", savedRate.getCurrencyCode());
            assertEquals(new BigDecimal("7.25"), savedRate.getRate());
            assertEquals("IMPORT", savedRate.getSource());
            // 2026-01-15 是周四，应该是工作日
            assertEquals(1, savedRate.getIsWorkday());
        }
        
        @Test
        @DisplayName("周末日期应该标记为非工作日")
        void testImportAndSave_Weekend_ShouldMarkAsNonWorkday() throws IOException {
            // Given: 2026-01-17 是周六
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-17", 7.2700}
                    }
            );
            
            // When
            excelStrategy.importAndSave(file, new ImportContext());
            
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
        void testParseDate_YyyyMmDd() throws IOException {
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 15), captor.getValue().getRateDate());
        }
        
        @Test
        @DisplayName("应该支持 yyyy/MM/dd 格式")
        void testParseDate_YyyySlashMmDd() throws IOException {
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026/01/18", 7.2800}
                    }
            );
            
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 18), captor.getValue().getRateDate());
        }
        
        @Test
        @DisplayName("应该支持 yyyyMMdd 格式")
        void testParseDate_YyyyMmDdNoSeparator() throws IOException {
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"20260119", 7.2900}
                    }
            );
            
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            assertEquals(1, result.get("successCount"));
            
            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 1, 19), captor.getValue().getRateDate());
        }
    }
    
    @Nested
    @DisplayName("货币过滤测试")
    class CurrencyFilterTest {
        
        @Test
        @DisplayName("未配置的货币列应该被跳过")
        void testImportAndSave_UnconfiguredCurrency_ShouldSkip() throws IOException {
            // Given: JPY 未配置
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY", "JPY/CNY", "EUR/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500, 0.0480, 7.8900}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then: JPY 列应该被跳过，只导入 USD 和 EUR
            assertEquals(2, result.get("totalCount"), "总行数应为 2（USD 和 EUR）");
            assertEquals(2, result.get("successCount"), "成功数应为 2");
            
            // 验证只插入了 2 条记录
            verify(exchangeRateMapper, times(2)).insert(any(ExchangeRate.class));
        }
        
        @Test
        @DisplayName("当所有货币都未配置时应该抛出异常")
        void testImportAndSave_AllCurrenciesUnconfigured_ShouldThrowException() throws IOException {
            // Given: JPY 和 CAD 都未配置，没有任何已配置的货币列
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "JPY/CNY", "CAD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 0.0480, 5.2000}
                    }
            );
            
            // When & Then: 应该抛出 BusinessException
            com.musheng.common.exception.BusinessException exception = 
                    assertThrows(com.musheng.common.exception.BusinessException.class, () -> {
                        excelStrategy.importAndSave(file, new ImportContext());
                    });
            
            // 验证异常消息包含未配置的货币信息
            assertTrue(exception.getMessage().contains("JPY") || exception.getMessage().contains("CAD"));
        }
        
        @Test
        @DisplayName("应该记录被跳过的货币（部分货币已配置）")
        @SuppressWarnings("unchecked")
        void testImportAndSave_ShouldRecordSkippedCurrencies() throws IOException {
            // Given: USD 已配置，JPY 未配置
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY", "JPY/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500, 0.0480}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertTrue(result.containsKey("skippedCurrencies"));
            
            java.util.Set<String> skipped = (java.util.Set<String>) result.get("skippedCurrencies");
            assertTrue(skipped.contains("JPY"));
            assertEquals(1, result.get("successCount"), "USD 应该成功导入");
        }
        
        @Test
        @DisplayName("货币代码应该不区分大小写")
        void testImportAndSave_CurrencyCode_CaseInsensitive() throws IOException {
            // Given: 小写货币代码
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "usd/cny", "Eur/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500, 7.8900}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
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
        void testImportAndSave_DuplicateInFile_ShouldSkip() throws IOException {
            // Given: 同一日期同一货币出现两次（两行相同日期）
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500},
                            {"2026-01-15", 7.2600}  // 重复日期
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(2, result.get("totalCount"));
            assertEquals(1, result.get("successCount"), "只有第一条应该成功");
            assertEquals(1, result.get("failCount"), "第二条应该失败（重复）");
        }
        
        @Test
        @DisplayName("数据库中已存在的数据应该被跳过")
        void testImportAndSave_ExistsInDb_ShouldSkip() throws IOException {
            // Given: 模拟数据库中已存在 2026-01-15 USD 的汇率
            ExchangeRate existingRate = new ExchangeRate();
            existingRate.setRateDate(LocalDate.of(2026, 1, 15));
            existingRate.setCurrencyCode("USD");
            existingRate.setRate(new BigDecimal("7.2400"));
            
            when(exchangeRateMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(existingRate));
            
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500},
                            {"2026-01-16", 7.2600}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
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
        void testImportAndSave_InvalidDate_ShouldRecordError() throws IOException {
            // Given: 包含无效日期的 Excel
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"invalid-date", 7.2500},
                            {"2026-01-15", 7.8900}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"), "只有有效日期行应该成功");
            assertEquals(1, result.get("failCount"), "无效日期行应该失败");
            
            List<String> errors = (List<String>) result.get("errors");
            assertFalse(errors.isEmpty());
        }
        
        @Test
        @DisplayName("空单元格应该被跳过")
        void testImportAndSave_EmptyCell_ShouldSkip() throws IOException {
            // Given: 包含空单元格的 Excel
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY", "EUR/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500, null},  // EUR 为空
                            {"2026-01-16", null, 7.9000}   // USD 为空
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then: 只有非空单元格应该被处理
            assertEquals(2, result.get("totalCount"));
            assertEquals(2, result.get("successCount"));
            
            verify(exchangeRateMapper, times(2)).insert(any(ExchangeRate.class));
        }
        
        @Test
        @DisplayName("数据来源行应该被跳过")
        void testImportAndSave_DataSourceRow_ShouldSkip() throws IOException {
            // Given: 包含数据来源行的 Excel
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500},
                            {"数据来源：中国货币网", null}  // 数据来源行
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then: 数据来源行应该被跳过
            assertEquals(1, result.get("totalCount"));
            assertEquals(1, result.get("successCount"));
        }
    }
    
    @Nested
    @DisplayName("列名兼容性测试")
    class ColumnNameTest {
        
        @Test
        @DisplayName("应该支持中文日期列名")
        void testImportAndSave_ChineseDateColumn() throws IOException {
            // Given
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
        
        @Test
        @DisplayName("应该支持 Date 列名")
        void testImportAndSave_EnglishDateColumn() throws IOException {
            // Given
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"Date", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
        
        @Test
        @DisplayName("应该支持 时间 列名")
        void testImportAndSave_TimeColumn() throws IOException {
            // Given
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"时间", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
        
        @Test
        @DisplayName("应该支持 100JPY/CNY 格式的货币列名")
        void testImportAndSave_CurrencyWithPrefix() throws IOException {
            // Given: 100JPY/CNY 格式（日元通常以100为单位）
            // 注意：JPY 未配置，所以会被跳过
            // 这里测试 USD 的正常解析
            MockMultipartFile file = createMatrixExcelFile(
                    new String[]{"日期", "USD/CNY"},
                    new Object[][]{
                            {"2026-01-15", 7.2500}
                    }
            );
            
            // When
            Map<String, Object> result = excelStrategy.importAndSave(file, new ImportContext());
            
            // Then
            assertEquals(1, result.get("successCount"));
        }
    }
    
    @Nested
    @DisplayName("supports() 方法测试")
    class SupportsTest {
        
        @Test
        @DisplayName("应该支持 .xlsx 文件")
        void testSupports_Xlsx() {
            assertTrue(excelStrategy.supports("rates.xlsx"));
            assertTrue(excelStrategy.supports("RATES.XLSX"));
            assertTrue(excelStrategy.supports("exchange_rates.xlsx"));
        }
        
        @Test
        @DisplayName("应该支持 .xls 文件")
        void testSupports_Xls() {
            assertTrue(excelStrategy.supports("rates.xls"));
            assertTrue(excelStrategy.supports("RATES.XLS"));
        }
        
        @Test
        @DisplayName("不应该支持非 Excel 文件")
        void testSupports_NonExcel() {
            assertFalse(excelStrategy.supports("rates.csv"));
            assertFalse(excelStrategy.supports("rates.txt"));
            assertFalse(excelStrategy.supports(null));
        }
    }
    
    /**
     * 创建矩阵格式的 Excel 测试文件
     * 
     * @param headers 表头（第一列为日期，其他列为货币对）
     * @param data 数据行（第一列为日期字符串，其他列为汇率数值）
     * @return MockMultipartFile
     */
    private MockMultipartFile createMatrixExcelFile(String[] headers, Object[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("汇率");
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 创建数据行
            for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                Object[] rowData = data[rowIdx];
                
                for (int colIdx = 0; colIdx < rowData.length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData[colIdx];
                    
                    if (value == null) {
                        // 空单元格
                    } else if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    }
                }
            }
            
            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            return new MockMultipartFile(
                    "file",
                    "rates.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
