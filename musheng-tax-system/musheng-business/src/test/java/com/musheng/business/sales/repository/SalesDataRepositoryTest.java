package com.musheng.business.sales.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.SalesQueryRequest;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.repository.impl.SalesDataRepositoryImpl;
import com.musheng.common.context.ShopContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SalesDataRepository 单元测试
 * 
 * 测试 Repository 层的数据访问方法：
 * 1. findByQuery() - 分页查询
 * 2. findById() - 根据ID查询
 * 3. existsByOrderIdAndCategory() - 重复检查
 * 4. save() / saveBatch() - 保存
 * 5. deleteById() / deleteByIds() - 删除
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalesDataRepository 单元测试")
class SalesDataRepositoryTest {

    @Mock
    private SalesDataMapper salesDataMapper;

    @InjectMocks
    private SalesDataRepositoryImpl salesDataRepository;

    private MockedStatic<ShopContext> shopContextMock;

    private static final Long TEST_SHOP_ID = 1001L;

    @BeforeEach
    void setUp() {
        // Mock ShopContext
        shopContextMock = mockStatic(ShopContext.class);
        shopContextMock.when(ShopContext::requireShopId).thenReturn(TEST_SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        if (shopContextMock != null) {
            shopContextMock.close();
        }
    }

    /**
     * findByQuery 方法测试
     */
    @Nested
    @DisplayName("findByQuery 方法测试")
    class FindByQueryTests {

        @Test
        @DisplayName("分页查询 - 无过滤条件应返回所有数据")
        void testFindByQuery_NoFilter_ShouldReturnAllData() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            Page<SalesData> expectedPage = new Page<>(1, 20);
            expectedPage.setRecords(Collections.singletonList(createTestSalesData()));
            expectedPage.setTotal(1);

            when(salesDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            // When: 执行查询
            Page<SalesData> result = salesDataRepository.findByQuery(query, 1, 20);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(1, result.getTotal(), "总数应为1");
            assertEquals(1, result.getRecords().size(), "记录数应为1");
            verify(salesDataMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 带站点过滤条件")
        void testFindByQuery_WithSiteCode_ShouldFilterBySiteCode() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            query.setSiteCode("US");
            
            Page<SalesData> expectedPage = new Page<>(1, 20);
            expectedPage.setRecords(Collections.singletonList(createTestSalesData()));
            expectedPage.setTotal(1);

            when(salesDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            // When: 执行查询
            Page<SalesData> result = salesDataRepository.findByQuery(query, 1, 20);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            verify(salesDataMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 带日期范围过滤条件")
        void testFindByQuery_WithDateRange_ShouldFilterByDateRange() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            query.setStartDate("2026-01-01");
            query.setEndDate("2026-01-31");
            
            Page<SalesData> expectedPage = new Page<>(1, 20);
            expectedPage.setRecords(Collections.emptyList());
            expectedPage.setTotal(0);

            when(salesDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            // When: 执行查询
            Page<SalesData> result = salesDataRepository.findByQuery(query, 1, 20);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(0, result.getTotal(), "总数应为0");
            verify(salesDataMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 带关键字搜索")
        void testFindByQuery_WithKeyword_ShouldSearchByKeyword() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            query.setKeyword("ORDER-001");
            
            Page<SalesData> expectedPage = new Page<>(1, 20);
            expectedPage.setRecords(Collections.singletonList(createTestSalesData()));
            expectedPage.setTotal(1);

            when(salesDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            // When: 执行查询
            Page<SalesData> result = salesDataRepository.findByQuery(query, 1, 20);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(1, result.getTotal(), "总数应为1");
            verify(salesDataMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 第二页数据")
        void testFindByQuery_SecondPage_ShouldReturnSecondPageData() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            
            Page<SalesData> expectedPage = new Page<>(2, 20);
            expectedPage.setRecords(Collections.singletonList(createTestSalesData()));
            expectedPage.setTotal(25);

            when(salesDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            // When: 执行查询
            Page<SalesData> result = salesDataRepository.findByQuery(query, 2, 20);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(25, result.getTotal(), "总数应为25");
            verify(salesDataMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }
    }

    /**
     * findById 方法测试
     */
    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("根据ID查询 - 数据存在应返回数据")
        void testFindById_WhenExists_ShouldReturnData() {
            // Given: 准备测试数据
            Long id = 1L;
            SalesData expectedData = createTestSalesData();
            expectedData.setId(id);

            when(salesDataMapper.selectById(id)).thenReturn(expectedData);

            // When: 执行查询
            SalesData result = salesDataRepository.findById(id);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(id, result.getId(), "ID应匹配");
            verify(salesDataMapper, times(1)).selectById(id);
        }

        @Test
        @DisplayName("根据ID查询 - 数据不存在应返回null")
        void testFindById_WhenNotExists_ShouldReturnNull() {
            // Given: 准备测试数据
            Long id = 999L;

            when(salesDataMapper.selectById(id)).thenReturn(null);

            // When: 执行查询
            SalesData result = salesDataRepository.findById(id);

            // Then: 验证结果
            assertNull(result, "结果应为空");
            verify(salesDataMapper, times(1)).selectById(id);
        }
    }

    /**
     * existsByOrderIdAndCategory 方法测试
     */
    @Nested
    @DisplayName("existsByOrderIdAndCategory 方法测试")
    class ExistsByOrderIdAndCategoryTests {

        @Test
        @DisplayName("检查重复 - 数据存在应返回true")
        void testExistsByOrderIdAndCategory_WhenExists_ShouldReturnTrue() {
            // Given: 准备测试数据
            String orderId = "ORDER-001";
            String category = "income";

            when(salesDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When: 执行检查
            boolean result = salesDataRepository.existsByOrderIdAndCategory(orderId, category);

            // Then: 验证结果
            assertTrue(result, "数据存在时应返回true");
            verify(salesDataMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("检查重复 - 数据不存在应返回false")
        void testExistsByOrderIdAndCategory_WhenNotExists_ShouldReturnFalse() {
            // Given: 准备测试数据
            String orderId = "ORDER-NEW";
            String category = "income";

            when(salesDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When: 执行检查
            boolean result = salesDataRepository.existsByOrderIdAndCategory(orderId, category);

            // Then: 验证结果
            assertFalse(result, "数据不存在时应返回false");
            verify(salesDataMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("检查重复 - 多条记录存在应返回true")
        void testExistsByOrderIdAndCategory_WhenMultipleExists_ShouldReturnTrue() {
            // Given: 准备测试数据
            String orderId = "ORDER-MULTI";
            String category = "refund";

            when(salesDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            // When: 执行检查
            boolean result = salesDataRepository.existsByOrderIdAndCategory(orderId, category);

            // Then: 验证结果
            assertTrue(result, "多条记录存在时应返回true");
        }
    }

    /**
     * save 方法测试
     */
    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存数据 - 应调用mapper的insert方法")
        void testSave_ShouldCallMapperInsert() {
            // Given: 准备测试数据
            SalesData salesData = createTestSalesData();

            when(salesDataMapper.insert(salesData)).thenReturn(1);

            // When: 执行保存
            salesDataRepository.save(salesData);

            // Then: 验证调用
            verify(salesDataMapper, times(1)).insert(salesData);
        }
    }

    /**
     * saveBatch 方法测试
     */
    @Nested
    @DisplayName("saveBatch 方法测试")
    class SaveBatchTests {

        @Test
        @DisplayName("批量保存 - 应逐条调用mapper的insert方法")
        void testSaveBatch_ShouldCallMapperInsertForEach() {
            // Given: 准备测试数据
            List<SalesData> dataList = Arrays.asList(
                    createTestSalesData(),
                    createTestSalesData(),
                    createTestSalesData()
            );

            when(salesDataMapper.insert(any(SalesData.class))).thenReturn(1);

            // When: 执行批量保存
            salesDataRepository.saveBatch(dataList);

            // Then: 验证调用次数
            verify(salesDataMapper, times(3)).insert(any(SalesData.class));
        }

        @Test
        @DisplayName("批量保存 - 空列表不应调用mapper")
        void testSaveBatch_EmptyList_ShouldNotCallMapper() {
            // Given: 空列表
            List<SalesData> dataList = Collections.emptyList();

            // When: 执行批量保存
            salesDataRepository.saveBatch(dataList);

            // Then: 验证未调用
            verify(salesDataMapper, never()).insert(any(SalesData.class));
        }

        @Test
        @DisplayName("批量保存 - null列表不应调用mapper")
        void testSaveBatch_NullList_ShouldNotCallMapper() {
            // When: 执行批量保存
            salesDataRepository.saveBatch(null);

            // Then: 验证未调用
            verify(salesDataMapper, never()).insert(any(SalesData.class));
        }
    }

    /**
     * deleteById 方法测试
     */
    @Nested
    @DisplayName("deleteById 方法测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("删除数据 - 应调用mapper的deleteById方法")
        void testDeleteById_ShouldCallMapperDeleteById() {
            // Given: 准备测试数据
            Long id = 1L;

            when(salesDataMapper.deleteById(id)).thenReturn(1);

            // When: 执行删除
            salesDataRepository.deleteById(id);

            // Then: 验证调用
            verify(salesDataMapper, times(1)).deleteById(id);
        }
    }

    /**
     * deleteByIds 方法测试
     */
    @Nested
    @DisplayName("deleteByIds 方法测试")
    class DeleteByIdsTests {

        @Test
        @DisplayName("批量删除 - 应调用mapper的deleteBatchIds方法")
        void testDeleteByIds_ShouldCallMapperDeleteBatchIds() {
            // Given: 准备测试数据
            List<Long> ids = Arrays.asList(1L, 2L, 3L);

            when(salesDataMapper.deleteBatchIds(ids)).thenReturn(3);

            // When: 执行批量删除
            salesDataRepository.deleteByIds(ids);

            // Then: 验证调用
            verify(salesDataMapper, times(1)).deleteBatchIds(ids);
        }

        @Test
        @DisplayName("批量删除 - 空列表不应调用mapper")
        void testDeleteByIds_EmptyList_ShouldNotCallMapper() {
            // Given: 空列表
            List<Long> ids = Collections.emptyList();

            // When: 执行批量删除
            salesDataRepository.deleteByIds(ids);

            // Then: 验证未调用
            verify(salesDataMapper, never()).deleteBatchIds(any());
        }

        @Test
        @DisplayName("批量删除 - null列表不应调用mapper")
        void testDeleteByIds_NullList_ShouldNotCallMapper() {
            // When: 执行批量删除
            salesDataRepository.deleteByIds(null);

            // Then: 验证未调用
            verify(salesDataMapper, never()).deleteBatchIds(any());
        }
    }

    /**
     * findListByQuery 方法测试
     */
    @Nested
    @DisplayName("findListByQuery 方法测试")
    class FindListByQueryTests {

        @Test
        @DisplayName("查询列表 - 应返回所有匹配数据")
        void testFindListByQuery_ShouldReturnAllMatchingData() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            query.setSiteCode("US");
            
            List<SalesData> expectedList = Arrays.asList(
                    createTestSalesData(),
                    createTestSalesData()
            );

            when(salesDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

            // When: 执行查询
            List<SalesData> result = salesDataRepository.findListByQuery(query);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertEquals(2, result.size(), "应返回2条数据");
            verify(salesDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("查询列表 - 无匹配数据应返回空列表")
        void testFindListByQuery_NoMatch_ShouldReturnEmptyList() {
            // Given: 准备测试数据
            SalesQueryRequest query = new SalesQueryRequest();
            query.setSiteCode("XX");

            when(salesDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // When: 执行查询
            List<SalesData> result = salesDataRepository.findListByQuery(query);

            // Then: 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.isEmpty(), "应返回空列表");
            verify(salesDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试用的 SalesData 对象
     */
    private SalesData createTestSalesData() {
        SalesData salesData = new SalesData();
        salesData.setShopId(TEST_SHOP_ID);
        salesData.setOrderId("ORDER-001");
        salesData.setSiteCode("US");
        salesData.setTransactionCategory("income");
        salesData.setTransactionType("Order");
        return salesData;
    }
}
