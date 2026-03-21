package com.musheng.business.settlement.derivation.service.impl;

import net.jqwik.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * isSupplierMsku 方法属性测试 - MSKU 前缀供应商判断
 *
 * 验证 isSupplierMsku 方法对任意 SKU 字符串的判断正确性：
 * 返回 true 当且仅当 SKU 以 MSUS-、MSCA-、MSUK-、MSEU- 或 MS- 开头。
 *
 * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
 *
 * <p><b>Validates: Requirements 5.1</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SalesDataAggregatorIsSupplierMskuPropertyTest {

    /** 供应商 MSKU 前缀列表（按长度从长到短） */
    private static final List<String> SUPPLIER_PREFIXES = List.of(
            "MSUS-", "MSCA-", "MSUK-", "MSEU-", "MS-"
    );

    // ==================== jqwik 自定义生成器 ====================

    /**
     * 生成供应商前缀
     */
    @Provide
    Arbitrary<String> supplierPrefixes() {
        return Arbitraries.of(SUPPLIER_PREFIXES);
    }

    /**
     * 生成非空的 SKU 后缀部分（如 D06-348-5）
     */
    @Provide
    Arbitrary<String> skuSuffixes() {
        return Arbitraries.strings()
                .alpha().numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /**
     * 生成带供应商前缀的完整 SKU（应返回 true）
     */
    @Provide
    Arbitrary<String> supplierSkus() {
        return Combinators.combine(supplierPrefixes(), skuSuffixes())
                .as((prefix, suffix) -> prefix + suffix);
    }

    /**
     * 生成不以任何供应商前缀开头的 SKU（应返回 false）
     * 使用不可能匹配供应商前缀的字符开头
     */
    @Provide
    Arbitrary<String> nonSupplierSkus() {
        // 以非 M 开头的字母 + 随机后缀，确保不会匹配任何供应商前缀
        return Arbitraries.of("ABC-", "XYZ-", "FBA-", "SKU-", "TEST-", "D06-", "UK-", "CA-", "EU-", "US-")
                .flatMap(prefix -> skuSuffixes().map(suffix -> prefix + suffix));
    }

    /**
     * 生成以 M 开头但不匹配任何供应商前缀的 SKU
     * 例如：MA-xxx、MB-xxx、MX-xxx、MSA-xxx（不是 MSUS/MSCA/MSUK/MSEU/MS-）
     */
    @Provide
    Arbitrary<String> mStartButNotSupplierSkus() {
        return Arbitraries.of("MA-", "MB-", "MX-", "MT-", "MZ-", "MSA-", "MSB-", "MSX-", "MSUX-", "MSCB-")
                .flatMap(prefix -> skuSuffixes().map(suffix -> prefix + suffix));
    }

    // ==================== 属性测试 ====================

    /**
     * 属性 6.1：带供应商前缀的 SKU 必须返回 true
     *
     * 对于任意供应商前缀 + 任意后缀组成的 SKU，isSupplierMsku 应返回 true
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 带前缀返回 true")
    void skuWithSupplierPrefixShouldReturnTrue(
            @ForAll("supplierSkus") String sku) {
        assertTrue(SalesDataAggregatorImpl.isSupplierMsku(sku),
                "带供应商前缀的 SKU '" + sku + "' 应返回 true");
    }

    /**
     * 属性 6.2：不带供应商前缀的 SKU 必须返回 false
     *
     * 对于任意不以供应商前缀开头的 SKU，isSupplierMsku 应返回 false
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 无前缀返回 false")
    void skuWithoutSupplierPrefixShouldReturnFalse(
            @ForAll("nonSupplierSkus") String sku) {
        assertFalse(SalesDataAggregatorImpl.isSupplierMsku(sku),
                "不带供应商前缀的 SKU '" + sku + "' 应返回 false");
    }

    /**
     * 属性 6.3：以 M 开头但不匹配供应商前缀的 SKU 返回 false
     *
     * 验证不会因为以 M 开头就误判为供应商 SKU
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - M开头非供应商返回 false")
    void skuStartingWithMButNotSupplierShouldReturnFalse(
            @ForAll("mStartButNotSupplierSkus") String sku) {
        assertFalse(SalesDataAggregatorImpl.isSupplierMsku(sku),
                "以 M 开头但不匹配供应商前缀的 SKU '" + sku + "' 应返回 false");
    }

    /**
     * 属性 6.4：isSupplierMsku 的判断结果与手动前缀匹配一致
     *
     * 对于任意非空字符串，isSupplierMsku 返回 true 当且仅当该字符串以某个供应商前缀开头
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Property(tries = 200)
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 与手动匹配一致")
    void isSupplierMskuShouldMatchManualPrefixCheck(
            @ForAll("randomStrings") String sku) {
        // 手动判断：检查是否以任一供应商前缀开头
        boolean expectedResult = SUPPLIER_PREFIXES.stream().anyMatch(sku::startsWith);

        assertEquals(expectedResult, SalesDataAggregatorImpl.isSupplierMsku(sku),
                "SKU '" + sku + "' 的 isSupplierMsku 结果应与手动前缀匹配一致");
    }

    /**
     * 生成随机字符串（用于属性 6.4 的全面验证）
     */
    @Provide
    Arbitrary<String> randomStrings() {
        return Arbitraries.strings()
                .alpha().numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    // ==================== 边界测试 ====================

    /**
     * 边界：null 输入应返回 false
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Example
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - null 返回 false")
    void nullSkuShouldReturnFalse() {
        assertFalse(SalesDataAggregatorImpl.isSupplierMsku(null),
                "null SKU 应返回 false");
    }

    /**
     * 边界：空字符串应返回 false
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Example
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 空字符串返回 false")
    void emptySkuShouldReturnFalse() {
        assertFalse(SalesDataAggregatorImpl.isSupplierMsku(""),
                "空字符串 SKU 应返回 false");
    }

    /**
     * 边界：仅前缀无后缀也应返回 true（前缀本身是合法 SKU）
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Example
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 仅前缀返回 true")
    void prefixOnlyShouldReturnTrue() {
        for (String prefix : SUPPLIER_PREFIXES) {
            assertTrue(SalesDataAggregatorImpl.isSupplierMsku(prefix),
                    "仅前缀 '" + prefix + "' 应返回 true");
        }
    }

    /**
     * 边界：空白字符串应返回 false
     *
     * // Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断
     *
     * <p><b>Validates: Requirements 5.1</b></p>
     */
    @Example
    @Label("Feature: settlement-derivation-date-adjust, Property 6: MSKU 前缀供应商判断 - 空白字符串返回 false")
    void blankSkuShouldReturnFalse() {
        assertFalse(SalesDataAggregatorImpl.isSupplierMsku("   "),
                "空白字符串 SKU 应返回 false");
    }
}
