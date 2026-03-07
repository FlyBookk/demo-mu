package com.musheng.business.document.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentNumberCalculator 单元测试
 *
 * <p>覆盖编号生成的正常场景、边界条件和异常场景。
 * 编号格式：{YYYYMMDD}{3位序号}，如 20250902001。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("DocumentNumberCalculator 编号生成器测试")
class DocumentNumberCalculatorTest {

    // ==================== 正常场景测试 ====================

    @Nested
    @DisplayName("generate - 正常场景")
    class NormalCaseTest {

        @Test
        @DisplayName("2025-09-02 序号1 → '20250902001'")
        void testGenerate_Date20250902Seq1_ShouldReturn20250902001() {
            // Given
            LocalDate date = LocalDate.of(2025, 9, 2);
            int sequence = 1;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals("20250902001", result);
        }

        @Test
        @DisplayName("2025-12-31 序号99 → '20251231099'")
        void testGenerate_Date20251231Seq99_ShouldReturn20251231099() {
            // Given
            LocalDate date = LocalDate.of(2025, 12, 31);
            int sequence = 99;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals("20251231099", result);
        }

        @Test
        @DisplayName("2026-01-15 序号5 → '20260115005'")
        void testGenerate_Date20260115Seq5_ShouldReturn20260115005() {
            // Given
            LocalDate date = LocalDate.of(2026, 1, 15);
            int sequence = 5;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals("20260115005", result);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("generate - 边界条件")
    class BoundaryCaseTest {

        @Test
        @DisplayName("序号1（最小值）→ 补零为 '001'")
        void testGenerate_Seq1_ShouldPadTo001() {
            // Given
            LocalDate date = LocalDate.of(2025, 1, 1);
            int sequence = 1;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals("20250101001", result);
        }

        @Test
        @DisplayName("序号999（最大值）→ '999'")
        void testGenerate_Seq999_ShouldReturn999() {
            // Given
            LocalDate date = LocalDate.of(2025, 6, 15);
            int sequence = 999;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals("20250615999", result);
        }

        @Test
        @DisplayName("编号长度固定为11位")
        void testGenerate_ResultLength_ShouldBe11() {
            // Given
            LocalDate date = LocalDate.of(2025, 3, 8);
            int sequence = 42;

            // When
            String result = DocumentNumberCalculator.generate(date, sequence);

            // Then
            assertEquals(11, result.length());
        }
    }

    // ==================== 异常场景测试 ====================

    @Nested
    @DisplayName("generate - 异常场景")
    class ExceptionCaseTest {

        @Test
        @DisplayName("null 日期应抛出 IllegalArgumentException")
        void testGenerate_NullDate_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DocumentNumberCalculator.generate(null, 1));
        }

        @Test
        @DisplayName("序号0应抛出 IllegalArgumentException")
        void testGenerate_Seq0_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DocumentNumberCalculator.generate(LocalDate.of(2025, 1, 1), 0));
        }

        @Test
        @DisplayName("负数序号应抛出 IllegalArgumentException")
        void testGenerate_NegativeSeq_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DocumentNumberCalculator.generate(LocalDate.of(2025, 1, 1), -1));
        }

        @Test
        @DisplayName("序号超过999应抛出 IllegalArgumentException")
        void testGenerate_SeqOver999_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DocumentNumberCalculator.generate(LocalDate.of(2025, 1, 1), 1000));
        }
    }
}
