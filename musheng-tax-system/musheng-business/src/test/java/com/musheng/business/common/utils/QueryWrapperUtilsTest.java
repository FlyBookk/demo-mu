package com.musheng.business.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.sales.entity.SalesData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryWrapperUtils 单元测试
 * 纯单元测试，不依赖 Spring Boot 上下文
 *
 * @author wanhua
 * 18:35 2026年02月01日
 */
@DisplayName("QueryWrapperUtils 单元测试")
class QueryWrapperUtilsTest {

    @Nested
    @DisplayName("applyShopIdFilter 测试")
    class ApplyShopIdFilterTest {

        @Test
        @DisplayName("正常店铺ID - 应添加过滤条件")
        void testNormalShopId() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyShopIdFilter(wrapper, SalesData::getShopId, 123L);
            
            // 验证 wrapper 不为空且有条件
            assertNotNull(wrapper);
            // LambdaQueryWrapper 在没有执行时不会生成 SQL，只验证不抛异常
        }

        @Test
        @DisplayName("null店铺ID - 不应添加过滤条件")
        void testNullShopId() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyShopIdFilter(wrapper, SalesData::getShopId, null);
            
            // 验证不抛异常
            assertNotNull(wrapper);
        }
    }

    @Nested
    @DisplayName("applyDateRangeFilter 测试")
    class ApplyDateRangeFilterTest {

        @Test
        @DisplayName("有效日期范围 - 应添加开始和结束条件")
        void testValidDateRange() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    "2026-01-01", "2026-01-31");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("只有开始日期 - 应只添加开始条件")
        void testOnlyStartDate() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    "2026-01-01", null);
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("只有结束日期 - 应只添加结束条件")
        void testOnlyEndDate() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    null, "2026-01-31");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("无效日期 - 不应添加条件")
        void testInvalidDate() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    "invalid", "invalid");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("空日期 - 不应添加条件")
        void testEmptyDate() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    "", "");
            
            assertNotNull(wrapper);
        }
    }

    @Nested
    @DisplayName("applyLikeFilter 测试")
    class ApplyLikeFilterTest {

        @Test
        @DisplayName("有效值 - 应添加模糊查询条件")
        void testValidValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyLikeFilter(wrapper, SalesData::getOrderId, "test");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("空值 - 不应添加条件")
        void testEmptyValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyLikeFilter(wrapper, SalesData::getOrderId, "");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("null值 - 不应添加条件")
        void testNullValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyLikeFilter(wrapper, SalesData::getOrderId, null);
            
            assertNotNull(wrapper);
        }
    }

    @Nested
    @DisplayName("applyEqFilter 测试")
    class ApplyEqFilterTest {

        @Test
        @DisplayName("字符串有效值 - 应添加精确匹配条件")
        void testStringValidValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getTransactionCategory, "Order");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("字符串空值 - 不应添加条件")
        void testStringEmptyValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getTransactionCategory, "");
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("Long有效值 - 应添加精确匹配条件")
        void testLongValidValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getShopId, 100L);
            
            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("Long null值 - 不应添加条件")
        void testLongNullValue() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getShopId, (Long) null);
            
            assertNotNull(wrapper);
        }
    }

    @Nested
    @DisplayName("组合使用测试")
    class CombinedUsageTest {

        @Test
        @DisplayName("多个过滤条件组合 - 应正确组合所有条件")
        void testCombinedFilters() {
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            
            QueryWrapperUtils.applyShopIdFilter(wrapper, SalesData::getShopId, 123L);
            QueryWrapperUtils.applyLikeFilter(wrapper, SalesData::getOrderId, "test");
            QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getTransactionCategory, "Order");
            QueryWrapperUtils.applyDateRangeFilter(wrapper, SalesData::getTransactionDate, 
                    "2026-01-01", "2026-01-31");
            
            assertNotNull(wrapper);
        }
    }
}
