package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementGenerator 单元测试
 *
 * <p>覆盖结算单生成器的核心逻辑：单周期4站点生成、MSKU去重汇总、
 * 金额精确计算、MSKU字母升序排列、合计行、编号格式、幂等性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("SettlementGenerator 结算单生成器测试")
class SettlementGeneratorTest {

    // ==================== 单周期4站点生成 ====================

    @Nested
    @DisplayName("单周期4站点生成")
    class SinglePeriodFourSitesTest {

        @Test
        @DisplayName("单周期4站点数据应生成4份结算单，序号001-004")
        void testGenerate_SinglePeriodFourSites_ShouldGenerateFourSettlements() {
            // Given - 一个7天周期，4个站点各有1条数据
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))  // 周二
                    .periodEnd(LocalDate.of(2025, 9, 8))    // 周一
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSCA-B001", "CAD", "12.50", 3),
                            createItem("MSUK-C001", "GBP", "8.00", 7),
                            createItem("MSEU-D001", "EUR", "9.50", 4)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 应生成4份结算单
            assertEquals(4, results.size());

            // 验证序号001-004对应正确站点
            assertEquals("001", results.get(0).getSettlement().getSiteSequence());
            assertEquals("USD", results.get(0).getSettlement().getSiteCode());

            assertEquals("002", results.get(1).getSettlement().getSiteSequence());
            assertEquals("CAD", results.get(1).getSettlement().getSiteCode());

            assertEquals("003", results.get(2).getSettlement().getSiteSequence());
            assertEquals("GBP", results.get(2).getSettlement().getSiteCode());

            assertEquals("004", results.get(3).getSettlement().getSiteSequence());
            assertEquals("EUR", results.get(3).getSettlement().getSiteCode());
        }

        @Test
        @DisplayName("结算单应包含正确的买方和卖方名称")
        void testGenerate_Settlement_ShouldContainCorrectBuyerAndSeller() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(createItem("MSUS-A001", "USD", "10.00", 5)))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then
            DocumentSettlement settlement = results.get(0).getSettlement();
            assertEquals("东莞市慕声商贸有限公司", settlement.getBuyerName());
            assertEquals("Hong Kong Andeo Group Limited", settlement.getSellerName());
        }

        @Test
        @DisplayName("结算单应包含正确的结算周期和结算日")
        void testGenerate_Settlement_ShouldContainCorrectPeriodAndDate() {
            // Given - 周期 2025-09-02(周二) 到 2025-09-08(周一)
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(createItem("MSUS-A001", "USD", "10.00", 5)))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 结算日应为周期结束后的下一个工作日 2025-09-09(周二)
            DocumentSettlement settlement = results.get(0).getSettlement();
            assertEquals(LocalDate.of(2025, 9, 2), settlement.getPeriodStart());
            assertEquals(LocalDate.of(2025, 9, 8), settlement.getPeriodEnd());
            assertEquals(LocalDate.of(2025, 9, 9), settlement.getSettlementDate());
        }
    }

    // ==================== MSKU去重汇总 ====================

    @Nested
    @DisplayName("MSKU去重汇总")
    class MskuDeduplicationTest {

        @Test
        @DisplayName("同一站点同一MSKU多条记录应合并为一条")
        void testGenerate_DuplicateMsku_ShouldMerge() {
            // Given - 同一MSKU出现两次
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSUS-A001", "USD", "10.00", 3)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - US站点应只有1条MSKU记录，数量合并为8
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            assertEquals(1, usResult.getItems().size());
            assertEquals(8, usResult.getItems().get(0).getQuantity());
            // 金额 = 8 × 10.00 = 80.0000
            assertEquals(0, new BigDecimal("80.0000").compareTo(usResult.getItems().get(0).getAmount()));
        }

        @Test
        @DisplayName("不同站点的MSKU不应合并")
        void testGenerate_DifferentSiteMsku_ShouldNotMerge() {
            // Given - 不同站点的MSKU
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSCA-A001", "CAD", "12.00", 3)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - US和CA各有1条记录
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            SettlementGenerateResult caResult = findBySiteSequence(results, "002");
            assertNotNull(usResult);
            assertNotNull(caResult);
            assertEquals(1, usResult.getItems().size());
            assertEquals(1, caResult.getItems().size());
        }
    }

    // ==================== 金额精确计算 ====================

    @Nested
    @DisplayName("金额精确计算")
    class AmountCalculationTest {

        @Test
        @DisplayName("金额应等于数量×单价，使用BigDecimal精确计算")
        void testGenerate_Amount_ShouldEqualQuantityTimesUnitPrice() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "12.3456", 7)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 金额 = 7 × 12.3456 = 86.4192
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            BigDecimal expectedAmount = new BigDecimal("12.3456")
                    .multiply(BigDecimal.valueOf(7))
                    .setScale(4, RoundingMode.HALF_UP);
            assertEquals(0, expectedAmount.compareTo(usResult.getItems().get(0).getAmount()));
        }

        @Test
        @DisplayName("合并后金额应重新计算（合并数量×单价）")
        void testGenerate_MergedAmount_ShouldRecalculate() {
            // Given - 同一MSKU两条记录
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.50", 3),
                            createItem("MSUS-A001", "USD", "10.50", 2)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 合并数量=5，金额=5×10.50=52.5000
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            assertEquals(5, usResult.getItems().get(0).getQuantity());
            assertEquals(0, new BigDecimal("52.5000").compareTo(usResult.getItems().get(0).getAmount()));
        }
    }

    // ==================== MSKU字母升序排列 ====================

    @Nested
    @DisplayName("MSKU字母升序排列")
    class MskuSortingTest {

        @Test
        @DisplayName("同一站点的MSKU应按字母升序排列")
        void testGenerate_MskuSorting_ShouldBeAlphabetical() {
            // Given - 故意乱序输入
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-C001", "USD", "10.00", 1),
                            createItem("MSUS-A001", "USD", "10.00", 2),
                            createItem("MSUS-B001", "USD", "10.00", 3)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 应按字母升序排列
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            List<DocumentSettlementItem> items = usResult.getItems();
            assertEquals(3, items.size());
            assertEquals("MSUS-A001", items.get(0).getMsku());
            assertEquals("MSUS-B001", items.get(1).getMsku());
            assertEquals("MSUS-C001", items.get(2).getMsku());
        }

        @Test
        @DisplayName("明细行号应从1开始连续编号")
        void testGenerate_LineNo_ShouldStartFromOneAndBeSequential() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-B001", "USD", "10.00", 1),
                            createItem("MSUS-A001", "USD", "10.00", 2)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            assertEquals(1, usResult.getItems().get(0).getLineNo());
            assertEquals(2, usResult.getItems().get(1).getLineNo());
        }
    }

    // ==================== 合计行验证 ====================

    @Nested
    @DisplayName("合计行验证")
    class TotalRowTest {

        @Test
        @DisplayName("totalQuantity应等于所有明细行数量之和")
        void testGenerate_TotalQuantity_ShouldEqualSumOfItems() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSUS-B001", "USD", "20.00", 3)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            int sumQuantity = usResult.getItems().stream()
                    .mapToInt(DocumentSettlementItem::getQuantity).sum();
            assertEquals(sumQuantity, usResult.getSettlement().getTotalQuantity());
            assertEquals(8, usResult.getSettlement().getTotalQuantity());
        }

        @Test
        @DisplayName("totalAmount应等于所有明细行金额之和")
        void testGenerate_TotalAmount_ShouldEqualSumOfItemAmounts() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),  // 50.0000
                            createItem("MSUS-B001", "USD", "20.00", 3)   // 60.0000
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then
            SettlementGenerateResult usResult = findBySiteSequence(results, "001");
            assertNotNull(usResult);
            BigDecimal sumAmount = usResult.getItems().stream()
                    .map(DocumentSettlementItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sumAmount.compareTo(usResult.getSettlement().getTotalAmount()));
            assertEquals(0, new BigDecimal("110.0000").compareTo(usResult.getSettlement().getTotalAmount()));
        }
    }

    // ==================== 编号格式验证 ====================

    @Nested
    @DisplayName("编号格式验证")
    class DocumentNoTest {

        @Test
        @DisplayName("编号格式应为 {YYYYMMDD}{3位序号}")
        void testGenerate_DocumentNo_ShouldMatchFormat() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSCA-B001", "CAD", "12.00", 3),
                            createItem("MSUK-C001", "GBP", "8.00", 7),
                            createItem("MSEU-D001", "EUR", "9.00", 4)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 结算日为 2025-09-09，序号001-004
            assertEquals(4, results.size());
            for (SettlementGenerateResult result : results) {
                assertTrue(result.getSettlement().getDocumentNo().matches("\\d{8}\\d{3}"));
            }
            // 序号001-004对应4个站点
            assertTrue(results.get(0).getSettlement().getDocumentNo().endsWith("001"));
            assertTrue(results.get(1).getSettlement().getDocumentNo().endsWith("002"));
            assertTrue(results.get(2).getSettlement().getDocumentNo().endsWith("003"));
            assertTrue(results.get(3).getSettlement().getDocumentNo().endsWith("004"));
        }

        @Test
        @DisplayName("起始序号参数应影响编号生成")
        void testGenerate_StartSequence_ShouldAffectDocumentNo() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(createItem("MSUS-A001", "USD", "10.00", 5)))
                    .build();

            // When - 起始序号为5
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 5);

            // Then - US站点序号应为005
            assertTrue(results.get(0).getSettlement().getDocumentNo().endsWith("005"));
        }
    }

    // ==================== 幂等性验证 ====================

    @Nested
    @DisplayName("幂等性验证")
    class IdempotencyTest {

        @Test
        @DisplayName("相同输入多次调用应输出完全一致")
        void testGenerate_SameInput_ShouldProduceSameOutput() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSUS-B001", "USD", "20.00", 3),
                            createItem("MSCA-C001", "CAD", "15.00", 2)
                    ))
                    .build();

            // When - 调用两次
            List<SettlementGenerateResult> results1 = SettlementGenerator.generate(input, 1);
            List<SettlementGenerateResult> results2 = SettlementGenerator.generate(input, 1);

            // Then - 输出应完全一致
            assertEquals(results1.size(), results2.size());
            for (int i = 0; i < results1.size(); i++) {
                DocumentSettlement s1 = results1.get(i).getSettlement();
                DocumentSettlement s2 = results2.get(i).getSettlement();
                assertEquals(s1.getDocumentNo(), s2.getDocumentNo());
                assertEquals(s1.getSettlementDate(), s2.getSettlementDate());
                assertEquals(s1.getSiteCode(), s2.getSiteCode());
                assertEquals(s1.getSiteSequence(), s2.getSiteSequence());
                assertEquals(s1.getTotalQuantity(), s2.getTotalQuantity());
                assertEquals(0, s1.getTotalAmount().compareTo(s2.getTotalAmount()));

                List<DocumentSettlementItem> items1 = results1.get(i).getItems();
                List<DocumentSettlementItem> items2 = results2.get(i).getItems();
                assertEquals(items1.size(), items2.size());
                for (int j = 0; j < items1.size(); j++) {
                    assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku());
                    assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity());
                    assertEquals(0, items1.get(j).getAmount().compareTo(items2.get(j).getAmount()));
                }
            }
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTest {

        @Test
        @DisplayName("空明细列表应返回空结果")
        void testGenerate_EmptyItems_ShouldReturnEmptyResult() {
            // Given
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of())
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("null输入应抛出异常")
        void testGenerate_NullInput_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> SettlementGenerator.generate(null, 1));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建结算数据明细项
     */
    private SettlementInput.SettlementDataItem createItem(String msku, String currency,
                                                           String unitPrice, int quantity) {
        return SettlementInput.SettlementDataItem.builder()
                .siteCode(null)  // 站点由MSKU前缀推断
                .msku(msku)
                .currency(currency)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .build();
    }

    /**
     * 根据站点序号查找结算单结果
     */
    private SettlementGenerateResult findBySiteSequence(List<SettlementGenerateResult> results,
                                                         String siteSequence) {
        return results.stream()
                .filter(r -> siteSequence.equals(r.getSettlement().getSiteSequence()))
                .findFirst()
                .orElse(null);
    }
}
