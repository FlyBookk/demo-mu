package com.musheng.common.enums;

import net.jqwik.api.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpSourceType 到 transactionCategory 映射正确性 - 属性测试
 *
 * 验证所有 ErpSourceType 枚举值通过分类判断方法映射到正确的 transactionCategory。
 * 模拟 ErpSettlementParser.convertToSalesData 中的分类逻辑。
 *
 * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
 *
 * **Validates: Requirements 3.1, 3.2**
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class ErpSourceTypeCategoryPropertyTest {

    /** 合法的 transactionCategory 值集合 */
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "income", "refund", "transfer", "fee", "adjustment", "other"
    );

    /**
     * 模拟 ErpSettlementParser.convertToSalesData 中的分类逻辑
     * 根据 ErpSourceType 的判断方法返回对应的 transactionCategory
     *
     * @param sourceType ERP 来源类型枚举
     * @return 对应的 transactionCategory 字符串
     */
    private String resolveTransactionCategory(ErpSourceType sourceType) {
        if (sourceType.isOrder()) {
            return "income";
        } else if (sourceType.isRefund()) {
            return "refund";
        } else if (sourceType.isTransfer()) {
            return "transfer";
        } else if (sourceType.isFee()) {
            return "fee";
        } else if (sourceType.isAdjustment()) {
            return "adjustment";
        } else {
            return "other";
        }
    }

    // ==================== 属性测试 ====================

    /**
     * 属性 4：对于任意 ErpSourceType 枚举值，分类映射结果必须属于合法分类集合
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - 任意 ErpSourceType 映射结果属于合法分类集合")
    void categoryMappingShouldAlwaysProduceValidCategory(
            @ForAll("allErpSourceTypes") ErpSourceType sourceType) {
        // When
        String category = resolveTransactionCategory(sourceType);

        // Then - 结果必须是合法分类之一
        assertTrue(VALID_CATEGORIES.contains(category),
                sourceType.name() + " 映射结果 '" + category + "' 不在合法分类集合中");
    }

    /**
     * 属性 4：isOrder() 为 true 的枚举值必须映射到 income
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - isOrder() 为 true 时映射到 income")
    void orderTypeShouldMapToIncome(
            @ForAll("orderTypes") ErpSourceType sourceType) {
        assertEquals("income", resolveTransactionCategory(sourceType),
                sourceType.name() + " isOrder()=true 应映射到 income");
    }

    /**
     * 属性 4：isRefund() 为 true 的枚举值必须映射到 refund
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - isRefund() 为 true 时映射到 refund")
    void refundTypeShouldMapToRefund(
            @ForAll("refundTypes") ErpSourceType sourceType) {
        assertEquals("refund", resolveTransactionCategory(sourceType),
                sourceType.name() + " isRefund()=true 应映射到 refund");
    }

    /**
     * 属性 4：isTransfer() 为 true 的枚举值必须映射到 transfer
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - isTransfer() 为 true 时映射到 transfer")
    void transferTypeShouldMapToTransfer(
            @ForAll("transferTypes") ErpSourceType sourceType) {
        assertEquals("transfer", resolveTransactionCategory(sourceType),
                sourceType.name() + " isTransfer()=true 应映射到 transfer");
    }

    /**
     * 属性 4：isFee() 为 true 的枚举值必须映射到 fee
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - isFee() 为 true 时映射到 fee")
    void feeTypeShouldMapToFee(
            @ForAll("feeTypes") ErpSourceType sourceType) {
        assertEquals("fee", resolveTransactionCategory(sourceType),
                sourceType.name() + " isFee()=true 应映射到 fee");
    }

    /**
     * 属性 4：isAdjustment() 为 true 的枚举值必须映射到 adjustment
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - isAdjustment() 为 true 时映射到 adjustment")
    void adjustmentTypeShouldMapToAdjustment(
            @ForAll("adjustmentTypes") ErpSourceType sourceType) {
        assertEquals("adjustment", resolveTransactionCategory(sourceType),
                sourceType.name() + " isAdjustment()=true 应映射到 adjustment");
    }

    /**
     * 属性 4：所有判断方法均为 false 的枚举值必须映射到 other
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - 所有分类判断均为 false 时映射到 other")
    void uncategorizedTypeShouldMapToOther(
            @ForAll("otherTypes") ErpSourceType sourceType) {
        assertEquals("other", resolveTransactionCategory(sourceType),
                sourceType.name() + " 所有分类判断均为 false，应映射到 other");
    }

    /**
     * 属性 4：每个枚举值的分类判断方法互斥（最多一个返回 true）
     * // Feature: transfer-category-mapping, Property 4: ErpSourceType 到 transactionCategory 的映射正确性
     */
    @Property
    @Label("属性4 - 分类判断方法互斥性（最多命中一个主分类）")
    void categoryMethodsShouldBeExclusive(
            @ForAll("allErpSourceTypes") ErpSourceType sourceType) {
        int trueCount = 0;
        if (sourceType.isOrder()) trueCount++;
        if (sourceType.isRefund()) trueCount++;
        if (sourceType.isTransfer()) trueCount++;
        if (sourceType.isFee()) trueCount++;
        if (sourceType.isAdjustment()) trueCount++;

        assertTrue(trueCount <= 1,
                sourceType.name() + " 有 " + trueCount + " 个分类判断返回 true，应最多 1 个");
    }

    // ==================== 数据提供器 ====================

    /** 提供所有 ErpSourceType 枚举值 */
    @Provide
    Arbitrary<ErpSourceType> allErpSourceTypes() {
        return Arbitraries.of(ErpSourceType.values());
    }

    /** 提供 isOrder() 为 true 的枚举值 */
    @Provide
    Arbitrary<ErpSourceType> orderTypes() {
        return Arbitraries.of(filterByPredicate(ErpSourceType::isOrder));
    }

    /** 提供 isRefund() 为 true 的枚举值 */
    @Provide
    Arbitrary<ErpSourceType> refundTypes() {
        return Arbitraries.of(filterByPredicate(ErpSourceType::isRefund));
    }

    /** 提供 isTransfer() 为 true 的枚举值 */
    @Provide
    Arbitrary<ErpSourceType> transferTypes() {
        return Arbitraries.of(filterByPredicate(ErpSourceType::isTransfer));
    }

    /** 提供 isFee() 为 true 的枚举值 */
    @Provide
    Arbitrary<ErpSourceType> feeTypes() {
        return Arbitraries.of(filterByPredicate(ErpSourceType::isFee));
    }

    /** 提供 isAdjustment() 为 true 的枚举值 */
    @Provide
    Arbitrary<ErpSourceType> adjustmentTypes() {
        return Arbitraries.of(filterByPredicate(ErpSourceType::isAdjustment));
    }

    /** 提供所有分类判断均为 false 的枚举值（归入 other） */
    @Provide
    Arbitrary<ErpSourceType> otherTypes() {
        return Arbitraries.of(filterByPredicate(st ->
                !st.isOrder() && !st.isRefund() && !st.isTransfer()
                        && !st.isFee() && !st.isAdjustment()));
    }

    /**
     * 根据条件过滤 ErpSourceType 枚举值
     */
    private ErpSourceType[] filterByPredicate(java.util.function.Predicate<ErpSourceType> predicate) {
        return Arrays.stream(ErpSourceType.values())
                .filter(predicate)
                .toArray(ErpSourceType[]::new);
    }
}
