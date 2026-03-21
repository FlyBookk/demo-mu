package com.musheng.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpSourceType 枚举 - Transfer 相关功能单元测试
 *
 * 验证 TRANSFER 枚举值的 isTransfer() 判断方法和 fromSourceValue() 解析方法。
 * 需求: 3.1
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("ErpSourceType - Transfer 功能测试")
class ErpSourceTypeTransferTest {

    // ==================== isTransfer() 测试 ====================

    @Test
    @DisplayName("isTransfer - TRANSFER枚举值应返回true")
    void testIsTransfer_TransferEnum_ShouldReturnTrue() {
        // Given
        ErpSourceType sourceType = ErpSourceType.TRANSFER;

        // When
        boolean result = sourceType.isTransfer();

        // Then
        assertTrue(result, "TRANSFER.isTransfer() 应返回 true");
    }

    @Test
    @DisplayName("isTransfer - 非TRANSFER枚举值应返回false")
    void testIsTransfer_NonTransferEnums_ShouldReturnFalse() {
        // Given - 所有非 TRANSFER 的枚举值
        for (ErpSourceType sourceType : ErpSourceType.values()) {
            if (sourceType == ErpSourceType.TRANSFER) {
                continue;
            }

            // When
            boolean result = sourceType.isTransfer();

            // Then
            assertFalse(result,
                    sourceType.name() + ".isTransfer() 应返回 false");
        }
    }

    // ==================== fromSourceValue() 测试 ====================

    @Test
    @DisplayName("fromSourceValue - 'Transfer'应返回TRANSFER枚举")
    void testFromSourceValue_Transfer_ShouldReturnTransfer() {
        // Given
        String sourceValue = "Transfer";

        // When
        ErpSourceType result = ErpSourceType.fromSourceValue(sourceValue);

        // Then
        assertEquals(ErpSourceType.TRANSFER, result,
                "fromSourceValue(\"Transfer\") 应返回 TRANSFER");
    }

    @Test
    @DisplayName("fromSourceValue - 大小写不敏感匹配'transfer'应返回TRANSFER")
    void testFromSourceValue_LowerCase_ShouldReturnTransfer() {
        // Given
        String sourceValue = "transfer";

        // When
        ErpSourceType result = ErpSourceType.fromSourceValue(sourceValue);

        // Then
        assertEquals(ErpSourceType.TRANSFER, result,
                "fromSourceValue(\"transfer\") 应返回 TRANSFER（大小写不敏感）");
    }

    @Test
    @DisplayName("TRANSFER枚举值属性应正确设置")
    void testTransfer_Properties_ShouldBeCorrect() {
        // Given
        ErpSourceType transfer = ErpSourceType.TRANSFER;

        // Then
        assertEquals("Transfer", transfer.getSourceValue(), "sourceValue 应为 'Transfer'");
        assertEquals("TRANSFER", transfer.getSettlementCategory(), "settlementCategory 应为 'TRANSFER'");
    }
}
