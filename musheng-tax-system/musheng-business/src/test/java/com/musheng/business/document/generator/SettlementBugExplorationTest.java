package com.musheng.business.document.generator;

import com.musheng.business.common.config.DocumentPartyProperties;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.utils.WorkingDayCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结算单 Bug 故障条件探索性测试
 *
 * <p>修复后验证：跨月数据按 transactionDate 正确隔离，INV 日期为结算日 + 3 个工作日。</p>
 *
 * <p>Validates: Requirements 1.1, 1.2</p>
 *
 * @author wanhua
 * 10:30 2026年07月14日
 */
@DisplayName("结算单 Bug 故障条件探索性测试")
class SettlementBugExplorationTest {

    // ==================== 跨月数据隔离测试 ====================

    @Nested
    @DisplayName("跨月数据隔离 - 故障条件")
    class CrossMonthDataIsolationTest {

        /**
         * 跨月结算单数据隔离测试（验证修复后行为）
         *
         * <p>构造跨两个自然月的输入：1月含 MSKU-A 数量 100（transactionDate=1月15日），
         * 2月含 MSKU-B 数量 50（transactionDate=2月15日）。
         * 断言：1月结算单只含 MSKU-A(100)，2月结算单只含 MSKU-B(50)。
         * 修复后 buildSettlementInput 正确传入 transactionDate，SettlementGenerator 可按月过滤。</p>
         *
         * <p>Validates: Requirements 1.1, 1.2</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("跨两月输入（含正确transactionDate）时，各月结算单应只含该月数据")
        void testGenerate_CrossMonthWithTransactionDate_ShouldIsolateDataByMonth() {
            // Given - 1月含 MSUS-A001 数量 100，2月含 MSUS-B001 数量 50
            // 修复后 transactionDate 正确传入，验证按月隔离行为
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 1, 1))
                    .periodEnd(LocalDate.of(2025, 2, 28))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(LocalDate.of(2025, 1, 15))  // 1月中旬
                                    .siteCode("USD")
                                    .msku("MSUS-A001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("10.00"))
                                    .quantity(100)
                                    .build(),
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(LocalDate.of(2025, 2, 15))  // 2月中旬
                                    .siteCode("USD")
                                    .msku("MSUS-B001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("20.00"))
                                    .quantity(50)
                                    .build()
                    ))
                    .build();

            // When - 生成结算单（跨2个自然月，应生成 2×4=8 份，每月4个站点）
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 找到1月和2月的 US 站点结算单
            // 1月的 US 站点结算单（siteSequence="001"，前4份中的第1份）
            SettlementGenerateResult janUs = results.get(0);
            // 2月的 US 站点结算单（siteSequence="001"，后4份中的第1份）
            SettlementGenerateResult febUs = results.get(4);

            // 验证确实是不同月份
            DocumentSettlement janSettlement = janUs.getSettlement();
            DocumentSettlement febSettlement = febUs.getSettlement();
            assertEquals(LocalDate.of(2025, 1, 1), janSettlement.getPeriodStart());
            assertEquals(LocalDate.of(2025, 2, 1), febSettlement.getPeriodStart());

            // 核心断言：1月结算单应只含 MSUS-A001(100)
            List<DocumentSettlementItem> janItems = janUs.getItems();
            Set<String> janMskus = janItems.stream()
                    .map(DocumentSettlementItem::getMsku)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("MSUS-A001"), janMskus,
                    "1月结算单应只含 MSUS-A001，但实际包含: " + janMskus);
            assertEquals(100, janSettlement.getTotalQuantity(),
                    "1月结算单总数量应为 100");

            // 核心断言：2月结算单应只含 MSUS-B001(50)
            List<DocumentSettlementItem> febItems = febUs.getItems();
            Set<String> febMskus = febItems.stream()
                    .map(DocumentSettlementItem::getMsku)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("MSUS-B001"), febMskus,
                    "2月结算单应只含 MSUS-B001，但实际包含: " + febMskus);
            assertEquals(50, febSettlement.getTotalQuantity(),
                    "2月结算单总数量应为 50");
        }
    }

    // ==================== INV 日期测试 ====================

    @Nested
    @DisplayName("INV 日期计算 - 故障条件")
    class InvDateCalculationTest {

        /**
         * INV 日期应为结算日 + 3 个工作日
         *
         * <p>给定结算日，断言 INV 日期 = addWorkingDays(settlementDate, 3)。
         * 预期结果：测试失败（当前是 nextWorkingDay，只加1个工作日）。</p>
         *
         * <p>Validates: Requirements 3.1</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("INV 日期应为结算日 + 3 个工作日，而非下一个工作日")
        void testBuildInv_InvDate_ShouldBeSettlementDatePlusThreeWorkingDays() {
            // Given - 构造一份结算单，结算日为 2025-02-05（周三）
            LocalDate settlementDate = LocalDate.of(2025, 2, 5);
            List<SettlementGenerateResult> settlementResults = List.of(
                    createSettlementResult(settlementDate, "001", "USD")
            );

            DocumentPartyProperties party = createTestParty();

            // When - 生成 INV
            List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, party);

            // Then - INV 日期应为 addWorkingDays(2025-02-05, 3) = 2025-02-10（周一）
            LocalDate expectedInvDate = WorkingDayCalculator.addWorkingDays(settlementDate, 3);
            LocalDate actualInvDate = invResults.get(0).getInv().getInvDate();

            assertEquals(expectedInvDate, actualInvDate,
                    "INV 日期应为结算日 + 3 个工作日 (" + expectedInvDate + ")，" +
                    "但实际为 " + actualInvDate + "（可能是 nextWorkingDay 的结果）");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的结算单结果
     *
     * @param settlementDate 结算日
     * @param siteSequence 站点序号
     * @param siteCode 站点代码
     * @return 结算单生成结果
     * @author wanhua
     * 10:30 2026年07月14日
     */
    private SettlementGenerateResult createSettlementResult(
            LocalDate settlementDate, String siteSequence, String siteCode) {
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo("20250205" + siteSequence);
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(LocalDate.of(2025, 1, 1));
        settlement.setPeriodEnd(LocalDate.of(2025, 1, 31));
        settlement.setSiteCode(siteCode);
        settlement.setSiteSequence(siteSequence);
        settlement.setBuyerName("东莞市慕声商贸有限公司");
        settlement.setSellerName("Hong Kong Andeo Group Limited");
        settlement.setTotalQuantity(5);
        settlement.setTotalAmount(new BigDecimal("50.0000"));

        DocumentSettlementItem item = new DocumentSettlementItem();
        item.setLineNo(1);
        item.setMsku("MSUS-A001");
        item.setCurrency(siteCode);
        item.setUnitPrice(new BigDecimal("10.0000"));
        item.setQuantity(5);
        item.setAmount(new BigDecimal("50.0000"));

        return SettlementGenerateResult.builder()
                .settlement(settlement)
                .items(List.of(item))
                .build();
    }

    /**
     * 创建测试用的交易方配置
     *
     * @return 交易方信息配置
     * @author wanhua
     * 10:30 2026年07月14日
     */
    private DocumentPartyProperties createTestParty() {
        DocumentPartyProperties party = new DocumentPartyProperties();
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
}
