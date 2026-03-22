package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentInvItem;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.utils.WorkingDayCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InvGenerator 单元测试
 *
 * <p>覆盖INV发票生成器的核心逻辑：4份结算单生成4份INV、INV日期计算、
 * MSKU/数量/单价/金额与结算单一致、编号格式、卖方/买方信息、银行信息、
 * 站点序号和站点代码、空输入、null输入、幂等性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("InvGenerator INV发票生成器测试")
class InvGeneratorTest {

    // ==================== 4份结算单生成4份INV ====================

    @Nested
    @DisplayName("4份结算单生成4份INV")
    class FourSettlementsToFourInvsTest {

        @Test
        @DisplayName("4份结算单应生成4份INV")
        void testGenerate_FourSettlements_ShouldGenerateFourInvs() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            assertEquals(4, results.size());
        }
    }

    // ==================== INV日期计算 ====================

    @Nested
    @DisplayName("INV日期计算")
    class InvDateCalculationTest {

        @Test
        @DisplayName("INV日期应为结算日的下一个工作日")
        void testGenerate_InvDate_ShouldBeNextWorkingDayOfSettlementDate() {
            // Given - 结算日 2025-09-09（周二），下一个工作日应为 2025-09-10（周三）
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            LocalDate expectedInvDate = WorkingDayCalculator.nextWorkingDay(LocalDate.of(2025, 9, 9));
            assertEquals(expectedInvDate, results.get(0).getInv().getInvDate());
        }

        @Test
        @DisplayName("结算日为周五时，INV日期应跳过周末")
        void testGenerate_SettlementDateFriday_ShouldSkipWeekend() {
            // Given - 结算日 2025-09-05（周五），下一个工作日应为 2025-09-08（周一）
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 5));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            LocalDate expectedInvDate = WorkingDayCalculator.nextWorkingDay(LocalDate.of(2025, 9, 5));
            assertEquals(expectedInvDate, results.get(0).getInv().getInvDate());
        }
    }

    // ==================== MSKU/数量/单价/金额与结算单一致 ====================

    @Nested
    @DisplayName("数据与结算单一致性")
    class DataConsistencyTest {

        @Test
        @DisplayName("INV明细的MSKU/数量/单价/金额应与结算单完全一致")
        void testGenerate_InvItems_ShouldMatchSettlementItems() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then - 逐一比对每份INV与对应结算单的明细
            for (int i = 0; i < results.size(); i++) {
                List<DocumentInvItem> invItems = results.get(i).getItems();
                List<DocumentSettlementItem> settlementItems = settlementResults.get(i).getItems();

                assertEquals(settlementItems.size(), invItems.size());
                for (int j = 0; j < invItems.size(); j++) {
                    assertEquals(settlementItems.get(j).getMsku(), invItems.get(j).getMsku());
                    assertEquals(settlementItems.get(j).getQuantity(), invItems.get(j).getQuantity());
                    assertEquals(0, settlementItems.get(j).getUnitPrice()
                            .compareTo(invItems.get(j).getUnitPrice()));
                    assertEquals(0, settlementItems.get(j).getAmount()
                            .compareTo(invItems.get(j).getAmount()));
                }
            }
        }

        @Test
        @DisplayName("INV合计数量和金额应与结算单一致")
        void testGenerate_InvTotals_ShouldMatchSettlementTotals() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (int i = 0; i < results.size(); i++) {
                DocumentInv inv = results.get(i).getInv();
                DocumentSettlement settlement = settlementResults.get(i).getSettlement();
                assertEquals(settlement.getTotalQuantity(), inv.getTotalQuantity());
                assertEquals(0, settlement.getTotalAmount().compareTo(inv.getTotalAmount()));
            }
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
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (InvGenerateResult result : results) {
                assertTrue(result.getInv().getDocumentNo().matches("\\d{8}\\d{3}"));
            }
            // 序号001-004
            assertTrue(results.get(0).getInv().getDocumentNo().endsWith("001"));
            assertTrue(results.get(1).getInv().getDocumentNo().endsWith("002"));
            assertTrue(results.get(2).getInv().getDocumentNo().endsWith("003"));
            assertTrue(results.get(3).getInv().getDocumentNo().endsWith("004"));
        }

        @Test
        @DisplayName("起始序号参数应影响编号生成")
        void testGenerate_StartSequence_ShouldAffectDocumentNo() {
            // Given
            List<SettlementGenerateResult> settlementResults = createSingleSiteSettlement(
                    LocalDate.of(2025, 9, 9), "001", "USD");

            // When - 起始序号为5
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 5, createTestParty());

            // Then
            assertTrue(results.get(0).getInv().getDocumentNo().endsWith("005"));
        }
    }

    // ==================== 卖方/买方信息 ====================

    @Nested
    @DisplayName("卖方/买方信息")
    class SellerBuyerInfoTest {

        @Test
        @DisplayName("卖方名称应为 Hong Kong Andeo Group Limited")
        void testGenerate_SellerName_ShouldBeCorrect() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (InvGenerateResult result : results) {
                assertEquals("Hong Kong Andeo Group Limited", result.getInv().getSellerName());
                assertEquals("Hong Kong", result.getInv().getSellerAddress());
            }
        }

        @Test
        @DisplayName("买方名称应为 Dongguan Musheng Trade Co., Ltd.")
        void testGenerate_BuyerName_ShouldBeCorrect() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (InvGenerateResult result : results) {
                assertEquals("Dongguan Musheng Trade Co., Ltd.", result.getInv().getBuyerName());
                assertEquals("Dongguan, Guangdong, China", result.getInv().getBuyerAddress());
            }
        }
    }

    // ==================== 银行信息填充 ====================

    @Nested
    @DisplayName("银行信息填充")
    class BankInfoTest {

        @Test
        @DisplayName("银行信息应被填充（非空）")
        void testGenerate_BankInfo_ShouldBeFilled() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (InvGenerateResult result : results) {
                DocumentInv inv = result.getInv();
                assertNotNull(inv.getBankAccountName());
                assertFalse(inv.getBankAccountName().isEmpty());
                assertNotNull(inv.getBankAccountNumber());
                assertFalse(inv.getBankAccountNumber().isEmpty());
                assertNotNull(inv.getBankName());
                assertFalse(inv.getBankName().isEmpty());
                assertNotNull(inv.getBankAddress());
                assertFalse(inv.getBankAddress().isEmpty());
                assertNotNull(inv.getSwiftCode());
                assertFalse(inv.getSwiftCode().isEmpty());
            }
        }
    }

    // ==================== 站点序号和站点代码 ====================

    @Nested
    @DisplayName("站点序号和站点代码")
    class SiteInfoTest {

        @Test
        @DisplayName("站点序号和站点代码应与结算单一致")
        void testGenerate_SiteInfo_ShouldMatchSettlement() {
            // Given
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            for (int i = 0; i < results.size(); i++) {
                DocumentInv inv = results.get(i).getInv();
                DocumentSettlement settlement = settlementResults.get(i).getSettlement();
                assertEquals(settlement.getSiteCode(), inv.getSiteCode());
                assertEquals(settlement.getSiteSequence(), inv.getSiteSequence());
            }
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTest {

        @Test
        @DisplayName("空输入应返回空列表")
        void testGenerate_EmptyInput_ShouldReturnEmptyList() {
            // Given
            List<SettlementGenerateResult> emptyList = List.of();

            // When
            List<InvGenerateResult> results = InvGenerator.generate(emptyList, 1, createTestParty());

            // Then
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("null输入应抛出异常")
        void testGenerate_NullInput_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> InvGenerator.generate(null, 1, createTestParty()));
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
            List<SettlementGenerateResult> settlementResults = createFourSiteSettlements(
                    LocalDate.of(2025, 9, 9));

            // When
            List<InvGenerateResult> results1 = InvGenerator.generate(settlementResults, 1, createTestParty());
            List<InvGenerateResult> results2 = InvGenerator.generate(settlementResults, 1, createTestParty());

            // Then
            assertEquals(results1.size(), results2.size());
            for (int i = 0; i < results1.size(); i++) {
                DocumentInv inv1 = results1.get(i).getInv();
                DocumentInv inv2 = results2.get(i).getInv();
                assertEquals(inv1.getDocumentNo(), inv2.getDocumentNo());
                assertEquals(inv1.getInvDate(), inv2.getInvDate());
                assertEquals(inv1.getSiteCode(), inv2.getSiteCode());
                assertEquals(inv1.getSiteSequence(), inv2.getSiteSequence());
                assertEquals(inv1.getSellerName(), inv2.getSellerName());
                assertEquals(inv1.getBuyerName(), inv2.getBuyerName());
                assertEquals(inv1.getTotalQuantity(), inv2.getTotalQuantity());
                assertEquals(0, inv1.getTotalAmount().compareTo(inv2.getTotalAmount()));

                List<DocumentInvItem> items1 = results1.get(i).getItems();
                List<DocumentInvItem> items2 = results2.get(i).getItems();
                assertEquals(items1.size(), items2.size());
                for (int j = 0; j < items1.size(); j++) {
                    assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku());
                    assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity());
                    assertEquals(0, items1.get(j).getUnitPrice().compareTo(items2.get(j).getUnitPrice()));
                    assertEquals(0, items1.get(j).getAmount().compareTo(items2.get(j).getAmount()));
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的交易方配置
     */
    private DocumentPartyConfig createTestParty() {
        DocumentPartyConfig party = new DocumentPartyConfig();
        party.setSellerName("Hong Kong Andeo Group Limited");
        party.setSellerAddress("Hong Kong");
        party.setSellerPhone("00852-54682464");
        party.setBuyerName("Dongguan Musheng Trade Co., Ltd.");
        party.setBuyerAddress("Dongguan, Guangdong, China");
        party.setBuyerPhone("18575382420");
        party.setBankAccountName("Hong Kong Andeo Group Limited");
        party.setBankAccountNumber("012-878-0-001234-5");
        party.setBankName("Bank of China (Hong Kong) Limited");
        party.setBankAddress("Bank of China Tower, 1 Garden Road, Central, Hong Kong");
        party.setSwiftCode("BKCHHKHH");
        return party;
    }

    /**
     * 创建4个站点的结算单结果（模拟 SettlementGenerator 的输出）
     */
    private List<SettlementGenerateResult> createFourSiteSettlements(LocalDate settlementDate) {
        List<SettlementGenerateResult> results = new ArrayList<>();
        results.add(createSettlementResult(settlementDate, "001", "USD", "MSUS-A001", "10.0000", 5));
        results.add(createSettlementResult(settlementDate, "002", "CAD", "MSCA-B001", "12.5000", 3));
        results.add(createSettlementResult(settlementDate, "003", "GBP", "MSUK-C001", "8.0000", 7));
        results.add(createSettlementResult(settlementDate, "004", "EUR", "MSEU-D001", "9.5000", 4));
        return results;
    }

    /**
     * 创建单个站点的结算单结果列表
     */
    private List<SettlementGenerateResult> createSingleSiteSettlement(
            LocalDate settlementDate, String siteSequence, String siteCode) {
        List<SettlementGenerateResult> results = new ArrayList<>();
        results.add(createSettlementResult(settlementDate, siteSequence, siteCode, "MSUS-A001", "10.0000", 5));
        return results;
    }

    /**
     * 创建单份结算单结果
     */
    private SettlementGenerateResult createSettlementResult(
            LocalDate settlementDate, String siteSequence, String siteCode,
            String msku, String unitPrice, int quantity) {

        BigDecimal price = new BigDecimal(unitPrice);
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity))
                .setScale(4, RoundingMode.HALF_UP);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo("20250909" + siteSequence);
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(LocalDate.of(2025, 9, 2));
        settlement.setPeriodEnd(LocalDate.of(2025, 9, 8));
        settlement.setSiteCode(siteCode);
        settlement.setSiteSequence(siteSequence);
        settlement.setBuyerName("东莞市慕声商贸有限公司");
        settlement.setSellerName("Hong Kong Andeo Group Limited");
        settlement.setTotalQuantity(quantity);
        settlement.setTotalAmount(amount);

        DocumentSettlementItem item = new DocumentSettlementItem();
        item.setLineNo(1);
        item.setMsku(msku);
        item.setCurrency(siteCode);
        item.setUnitPrice(price);
        item.setQuantity(quantity);
        item.setAmount(amount);

        return SettlementGenerateResult.builder()
                .settlement(settlement)
                .items(List.of(item))
                .build();
    }
}
