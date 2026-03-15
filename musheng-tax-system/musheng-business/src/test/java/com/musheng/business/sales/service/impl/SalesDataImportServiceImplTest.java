package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.parser.SalesDataParserFactory;
import com.musheng.business.sales.repository.SalesDataRepository;
import com.musheng.business.rate.service.RateService;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import com.musheng.config.mapping.mapper.FieldMappingTemplateMapper;
import com.musheng.config.mapping.mapper.TransactionTypeMappingMapper;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SalesDataImportServiceImpl 单元测试
 * 
 * 测试导入服务的核心方法：
 * 1. generateBatchNo() - 批次号生成
 * 2. isDuplicate() - 重复检查
 * 3. parseDecimalField() - 金额解析
 * 
 * @author wanhua
 * 14:30 2026年02月01日
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalesDataImportServiceImpl 单元测试")
class SalesDataImportServiceImplTest {

    @Mock
    private SalesDataMapper salesDataMapper;

    @Mock
    private SalesDataRepository salesDataRepository;

    @Mock
    private FieldMappingTemplateMapper fieldMappingTemplateMapper;

    @Mock
    private TransactionTypeMappingMapper transactionTypeMappingMapper;

    @Mock
    private MarketplaceMapper marketplaceMapper;

    @Mock
    private ImportRecordMapper importRecordMapper;

    @Mock
    private CsvParseServiceImpl csvParseService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SalesDataParserFactory parserFactory;

    @Mock
    private RateService rateService;

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @InjectMocks
    private SalesDataImportServiceImpl salesDataImportService;

    /**
     * generateBatchNo 方法测试
     */
    @Nested
    @DisplayName("generateBatchNo 方法测试")
    class GenerateBatchNoTests {

        @Test
        @DisplayName("生成批次号 - 应返回有效格式 SALES-{timestamp}-{uuid}")
        void testGenerateBatchNo_ShouldReturnValidFormat() throws Exception {
            // Given: 通过反射获取私有方法
            Method generateBatchNoMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("generateBatchNo");
            generateBatchNoMethod.setAccessible(true);

            // When: 调用方法
            String batchNo = (String) generateBatchNoMethod.invoke(salesDataImportService);

            // Then: 验证格式
            assertNotNull(batchNo, "批次号不应为空");
            assertTrue(batchNo.startsWith("SALES-"), "批次号应以 SALES- 开头");
            
            // 验证格式: SALES-{timestamp}-{uuid}
            String[] parts = batchNo.split("-");
            assertEquals(3, parts.length, "批次号应包含3个部分");
            assertEquals("SALES", parts[0], "第一部分应为 SALES");
            
            // 验证时间戳部分是数字
            assertTrue(parts[1].matches("\\d+"), "第二部分应为时间戳数字");
            
            // 验证 UUID 部分长度为 8
            assertEquals(8, parts[2].length(), "UUID 部分应为 8 个字符");
        }

        @Test
        @DisplayName("生成批次号 - 多次调用应返回不同值")
        void testGenerateBatchNo_MultipleCalls_ShouldReturnDifferentValues() throws Exception {
            // Given: 通过反射获取私有方法
            Method generateBatchNoMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("generateBatchNo");
            generateBatchNoMethod.setAccessible(true);

            // When: 多次调用
            String batchNo1 = (String) generateBatchNoMethod.invoke(salesDataImportService);
            String batchNo2 = (String) generateBatchNoMethod.invoke(salesDataImportService);

            // Then: 验证不同
            assertNotEquals(batchNo1, batchNo2, "多次生成的批次号应不同");
        }
    }

    /**
     * isDuplicate 方法测试
     * 
     * ⚠️ 重构后使用 SalesDataRepository.existsByOrderIdAndCategory() 方法
     */
    @Nested
    @DisplayName("isDuplicate 方法测试")
    class IsDuplicateTests {

        @Test
        @DisplayName("检查重复 - 数据存在时应返回 true")
        void testIsDuplicate_WhenExists_ShouldReturnTrue() throws Exception {
            // Given: 准备测试数据
            SalesData salesData = new SalesData();
            salesData.setOrderId("ORDER-001");
            salesData.setTransactionCategory("income");

            // Mock: Repository 返回存在记录
            when(salesDataRepository.existsByOrderIdAndCategory("ORDER-001", "income")).thenReturn(true);

            // When: 通过反射调用私有方法
            Method isDuplicateMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("isDuplicate", SalesData.class);
            isDuplicateMethod.setAccessible(true);
            boolean result = (boolean) isDuplicateMethod.invoke(salesDataImportService, salesData);

            // Then: 验证结果
            assertTrue(result, "数据存在时应返回 true");
            verify(salesDataRepository, times(1)).existsByOrderIdAndCategory("ORDER-001", "income");
        }

        @Test
        @DisplayName("检查重复 - 数据不存在时应返回 false")
        void testIsDuplicate_WhenNotExists_ShouldReturnFalse() throws Exception {
            // Given: 准备测试数据
            SalesData salesData = new SalesData();
            salesData.setOrderId("ORDER-NEW");
            salesData.setTransactionCategory("income");

            // Mock: Repository 返回不存在记录
            when(salesDataRepository.existsByOrderIdAndCategory("ORDER-NEW", "income")).thenReturn(false);

            // When: 通过反射调用私有方法
            Method isDuplicateMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("isDuplicate", SalesData.class);
            isDuplicateMethod.setAccessible(true);
            boolean result = (boolean) isDuplicateMethod.invoke(salesDataImportService, salesData);

            // Then: 验证结果
            assertFalse(result, "数据不存在时应返回 false");
            verify(salesDataRepository, times(1)).existsByOrderIdAndCategory("ORDER-NEW", "income");
        }

        @Test
        @DisplayName("检查重复 - 多条记录存在时应返回 true")
        void testIsDuplicate_WhenMultipleExists_ShouldReturnTrue() throws Exception {
            // Given: 准备测试数据
            SalesData salesData = new SalesData();
            salesData.setOrderId("ORDER-MULTI");
            salesData.setTransactionCategory("refund");

            // Mock: Repository 返回存在记录（多条也是存在）
            when(salesDataRepository.existsByOrderIdAndCategory("ORDER-MULTI", "refund")).thenReturn(true);

            // When: 通过反射调用私有方法
            Method isDuplicateMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("isDuplicate", SalesData.class);
            isDuplicateMethod.setAccessible(true);
            boolean result = (boolean) isDuplicateMethod.invoke(salesDataImportService, salesData);

            // Then: 验证结果
            assertTrue(result, "存在多条记录时应返回 true");
            verify(salesDataRepository, times(1)).existsByOrderIdAndCategory("ORDER-MULTI", "refund");
        }
    }

    /**
     * parseDecimalField 方法测试
     */
    @Nested
    @DisplayName("parseDecimalField 方法测试")
    class ParseDecimalFieldTests {

        @Test
        @DisplayName("解析金额 - 正常数值应正确解析")
        void testParseDecimalField_NormalValue_ShouldParseCorrectly() throws Exception {
            // Given: 准备测试数据
            Map<String, String> rowData = new HashMap<>();
            rowData.put("product_sales", "123.45");
            
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("product_sales", "product_sales");
            
            String siteCode = "US";

            // Mock: csvParseService 返回解析结果
            when(csvParseService.parseDecimal("123.45", "US"))
                    .thenReturn(new BigDecimal("123.45"));

            // When: 通过反射调用私有方法
            Method parseDecimalFieldMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("parseDecimalField", Map.class, Map.class, String.class, String.class);
            parseDecimalFieldMethod.setAccessible(true);
            BigDecimal result = (BigDecimal) parseDecimalFieldMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "product_sales", siteCode);

            // Then: 验证结果
            assertEquals(new BigDecimal("123.45"), result, "应正确解析金额");
        }

        @Test
        @DisplayName("解析金额 - 空值应返回 ZERO")
        void testParseDecimalField_EmptyValue_ShouldReturnZero() throws Exception {
            // Given: 准备测试数据（空值）
            Map<String, String> rowData = new HashMap<>();
            rowData.put("product_sales", "");
            
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("product_sales", "product_sales");
            
            String siteCode = "US";

            // When: 通过反射调用私有方法
            Method parseDecimalFieldMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("parseDecimalField", Map.class, Map.class, String.class, String.class);
            parseDecimalFieldMethod.setAccessible(true);
            BigDecimal result = (BigDecimal) parseDecimalFieldMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "product_sales", siteCode);

            // Then: 验证结果
            assertEquals(BigDecimal.ZERO, result, "空值应返回 ZERO");
        }

        @Test
        @DisplayName("解析金额 - 字段不存在应返回 ZERO")
        void testParseDecimalField_FieldNotExists_ShouldReturnZero() throws Exception {
            // Given: 准备测试数据（字段不存在）
            Map<String, String> rowData = new HashMap<>();
            
            Map<String, String> fieldMapping = new HashMap<>();
            
            String siteCode = "US";

            // When: 通过反射调用私有方法
            Method parseDecimalFieldMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("parseDecimalField", Map.class, Map.class, String.class, String.class);
            parseDecimalFieldMethod.setAccessible(true);
            BigDecimal result = (BigDecimal) parseDecimalFieldMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "non_existent_field", siteCode);

            // Then: 验证结果
            assertEquals(BigDecimal.ZERO, result, "字段不存在应返回 ZERO");
        }

        @Test
        @DisplayName("解析金额 - 负数应正确解析")
        void testParseDecimalField_NegativeValue_ShouldParseCorrectly() throws Exception {
            // Given: 准备测试数据（负数）
            Map<String, String> rowData = new HashMap<>();
            rowData.put("selling_fees", "-15.99");
            
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("selling_fees", "selling_fees");
            
            String siteCode = "DE";

            // Mock: csvParseService 返回解析结果
            when(csvParseService.parseDecimal("-15.99", "DE"))
                    .thenReturn(new BigDecimal("-15.99"));

            // When: 通过反射调用私有方法
            Method parseDecimalFieldMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("parseDecimalField", Map.class, Map.class, String.class, String.class);
            parseDecimalFieldMethod.setAccessible(true);
            BigDecimal result = (BigDecimal) parseDecimalFieldMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "selling_fees", siteCode);

            // Then: 验证结果
            assertEquals(new BigDecimal("-15.99"), result, "负数应正确解析");
        }
    }

    /**
     * getMappedValue 方法测试
     */
    @Nested
    @DisplayName("getMappedValue 方法测试")
    class GetMappedValueTests {

        @Test
        @DisplayName("获取映射值 - 映射存在时应返回正确值")
        void testGetMappedValue_WhenMappingExists_ShouldReturnValue() throws Exception {
            // Given: 准备测试数据
            Map<String, String> rowData = new HashMap<>();
            rowData.put("order id", "ORD-12345");
            
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("order_id", "order id");

            // When: 通过反射调用私有方法
            Method getMappedValueMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("getMappedValue", Map.class, Map.class, String.class);
            getMappedValueMethod.setAccessible(true);
            String result = (String) getMappedValueMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "order_id");

            // Then: 验证结果
            assertEquals("ORD-12345", result, "应返回映射的值");
        }

        @Test
        @DisplayName("获取映射值 - 映射不存在时应尝试通用字段名")
        void testGetMappedValue_WhenMappingNotExists_ShouldTryCommonNames() throws Exception {
            // Given: 准备测试数据（使用通用字段名）
            Map<String, String> rowData = new HashMap<>();
            rowData.put("order id", "ORD-67890");
            
            Map<String, String> fieldMapping = new HashMap<>();
            // 不设置映射，让方法尝试通用字段名

            // When: 通过反射调用私有方法
            Method getMappedValueMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("getMappedValue", Map.class, Map.class, String.class);
            getMappedValueMethod.setAccessible(true);
            String result = (String) getMappedValueMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "order_id");

            // Then: 验证结果
            assertEquals("ORD-67890", result, "应通过通用字段名找到值");
        }

        @Test
        @DisplayName("获取映射值 - 字段不存在时应返回空字符串")
        void testGetMappedValue_WhenFieldNotExists_ShouldReturnEmpty() throws Exception {
            // Given: 准备测试数据（字段不存在）
            Map<String, String> rowData = new HashMap<>();
            
            Map<String, String> fieldMapping = new HashMap<>();

            // When: 通过反射调用私有方法
            Method getMappedValueMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("getMappedValue", Map.class, Map.class, String.class);
            getMappedValueMethod.setAccessible(true);
            String result = (String) getMappedValueMethod.invoke(
                    salesDataImportService, rowData, fieldMapping, "non_existent");

            // Then: 验证结果
            assertEquals("", result, "字段不存在时应返回空字符串");
        }
    }

    /**
     * buildOrderKey 方法测试
     */
    @Nested
    @DisplayName("buildUnifiedUniqueKey 方法测试")
    class BuildOrderKeyTests {

        @Test
        @DisplayName("构建唯一键 - 标准订单所有字段都有值")
        void testBuildOrderKey_AllFieldsPresent_ShouldBuildCorrectKey() throws Exception {
            // Given: 准备测试数据（标准订单号格式）
            SalesData salesData = new SalesData();
            salesData.setOrderId("111-1234567-1234567");
            salesData.setSiteCode("US");
            salesData.setTransactionCategory("Order");
            salesData.setSku("SKU-001");

            // When: 通过反射调用私有方法
            Method buildKeyMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("buildUnifiedUniqueKey", SalesData.class);
            buildKeyMethod.setAccessible(true);
            String result = (String) buildKeyMethod.invoke(salesDataImportService, salesData);

            // Then: 验证结果（标准订单：orderId|siteCode|transactionCategory|sku）
            assertEquals("111-1234567-1234567|US|Order|SKU-001", result, "应正确构建唯一键");
        }

        @Test
        @DisplayName("构建唯一键 - 部分字段为空")
        void testBuildOrderKey_SomeFieldsNull_ShouldHandleNulls() throws Exception {
            // Given: 准备测试数据（部分字段为空，标准订单号格式）
            SalesData salesData = new SalesData();
            salesData.setOrderId("111-1234567-1234567");
            salesData.setSiteCode(null);
            salesData.setTransactionCategory(null);
            salesData.setSku("SKU-002");

            // When: 通过反射调用私有方法
            Method buildKeyMethod = SalesDataImportServiceImpl.class
                    .getDeclaredMethod("buildUnifiedUniqueKey", SalesData.class);
            buildKeyMethod.setAccessible(true);
            String result = (String) buildKeyMethod.invoke(salesDataImportService, salesData);

            // Then: 验证结果（空值应替换为空字符串）
            assertEquals("111-1234567-1234567|||SKU-002", result, "空值应替换为空字符串");
        }
    }
}
