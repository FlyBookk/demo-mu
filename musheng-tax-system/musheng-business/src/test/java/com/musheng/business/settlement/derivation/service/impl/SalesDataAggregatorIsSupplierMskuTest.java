package com.musheng.business.settlement.derivation.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * isSupplierMsku 方法单元测试 - 具体示例验证
 *
 * <p>覆盖供应商 MSKU 前缀判断的各种具体场景：
 * 各站点前缀（MSUS-、MSCA-、MSUK-、MSEU-）、通用前缀（MS-）、
 * 非供应商 SKU、null、空字符串、空白字符串。</p>
 *
 * <p><b>Validates: Requirements 5.1</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("isSupplierMsku 单元测试")
class SalesDataAggregatorIsSupplierMskuTest {

    // ==================== 供应商 MSKU 前缀匹配（返回 true） ====================

    /**
     * 美国站前缀 MSUS- 应返回 true
     */
    @Test
    @DisplayName("MSUS-D06-348-5 → true（美国站前缀）")
    void testIsSupplierMsku_MsusPrefix_ShouldReturnTrue() {
        // Given - 美国站供应商 MSKU
        String sku = "MSUS-D06-348-5";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertTrue(result, "美国站前缀 MSUS- 应判定为供应商 MSKU");
    }

    /**
     * 加拿大站前缀 MSCA- 应返回 true
     */
    @Test
    @DisplayName("MSCA-D72-1-4 → true（加拿大站前缀）")
    void testIsSupplierMsku_MscaPrefix_ShouldReturnTrue() {
        // Given - 加拿大站供应商 MSKU
        String sku = "MSCA-D72-1-4";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertTrue(result, "加拿大站前缀 MSCA- 应判定为供应商 MSKU");
    }

    /**
     * 英国站前缀 MSUK- 应返回 true
     */
    @Test
    @DisplayName("MSUK-D72-1-4 → true（英国站前缀）")
    void testIsSupplierMsku_MsukPrefix_ShouldReturnTrue() {
        // Given - 英国站供应商 MSKU
        String sku = "MSUK-D72-1-4";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertTrue(result, "英国站前缀 MSUK- 应判定为供应商 MSKU");
    }

    /**
     * 欧洲站前缀 MSEU- 应返回 true
     */
    @Test
    @DisplayName("MSEU-D72-1-4 → true（欧洲站前缀）")
    void testIsSupplierMsku_MseuPrefix_ShouldReturnTrue() {
        // Given - 欧洲站供应商 MSKU
        String sku = "MSEU-D72-1-4";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertTrue(result, "欧洲站前缀 MSEU- 应判定为供应商 MSKU");
    }

    /**
     * 通用前缀 MS- 应返回 true
     */
    @Test
    @DisplayName("MS-D72-1-4 → true（通用前缀）")
    void testIsSupplierMsku_MsGenericPrefix_ShouldReturnTrue() {
        // Given - 通用供应商 MSKU
        String sku = "MS-D72-1-4";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertTrue(result, "通用前缀 MS- 应判定为供应商 MSKU");
    }

    // ==================== 非供应商 SKU（返回 false） ====================

    /**
     * 非供应商前缀 ABC- 应返回 false
     */
    @Test
    @DisplayName("ABC-D06-348-5 → false（非供应商前缀）")
    void testIsSupplierMsku_NonSupplierPrefix_ShouldReturnFalse() {
        // Given - 非供应商 SKU
        String sku = "ABC-D06-348-5";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertFalse(result, "非供应商前缀 ABC- 不应判定为供应商 MSKU");
    }

    // ==================== 边界情况（返回 false） ====================

    /**
     * null 输入应返回 false
     */
    @Test
    @DisplayName("null → false")
    void testIsSupplierMsku_NullInput_ShouldReturnFalse() {
        // Given - null SKU
        String sku = null;

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertFalse(result, "null 输入应返回 false");
    }

    /**
     * 空字符串应返回 false
     */
    @Test
    @DisplayName("空字符串 → false")
    void testIsSupplierMsku_EmptyString_ShouldReturnFalse() {
        // Given - 空字符串
        String sku = "";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertFalse(result, "空字符串应返回 false");
    }

    /**
     * 空白字符串应返回 false
     */
    @Test
    @DisplayName("空白字符串 → false")
    void testIsSupplierMsku_BlankString_ShouldReturnFalse() {
        // Given - 空白字符串
        String sku = "   ";

        // When
        boolean result = SalesDataAggregatorImpl.isSupplierMsku(sku);

        // Then
        assertFalse(result, "空白字符串应返回 false");
    }
}
