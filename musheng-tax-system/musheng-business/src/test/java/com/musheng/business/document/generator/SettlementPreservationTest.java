package com.musheng.business.document.generator;

import com.musheng.business.common.config.DocumentPartyProperties;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
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
 * 结算单保留属性测试
 *
 * <p>在未修复代码上运行，确认基准行为正常。
 * 测试预期全部通过——通过即确认现有逻辑不受 Bug 影响。</p>
 *
 * <p>Validates: Requirements 3.1, 3.4</p>
 *
 * @author wanhua
 * 10:30 2026年07月14日
 */
@DisplayName("结算单保留属性测试")
class SettlementPreservationTest {

    // ==================== 单月周期保留测试 ====================

    @Nested
    @DisplayName("单月周期保留 - 不受 Bug 影响")
    class SingleMonthPreservationTest {

        /**
         * 单月周期输入时，结算单数据应正确生成
         *
         * <p>构造单月周期的 SettlementInput（periodStart 和 periodEnd 在同一自然月），
         * 验证结算单数据正确生成。单月周期不受跨月数据重复 Bug 影响。</p>
         *
         * <p>Validates: Requirements 3.1, 3.4</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("单月周期输入时，结算单应正确包含所有 MSKU 数据")
        void testGenerate_SingleMonthPeriod_ShouldGenerateCorrectSettlement() {
            // Given - 单月周期：2025年1月，包含两个 US 站点 MSKU
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 1, 1))
                    .periodEnd(LocalDate.of(2025, 1, 31))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)  // 模拟当前 buildSettlementInput 的输出
                                    .siteCode("USD")
                                    .msku("MSUS-A001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("10.00"))
                                    .quantity(100)
                                    .build(),
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("USD")
                                    .msku("MSUS-B001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("20.00"))
                                    .quantity(50)
                                    .build()
                    ))
                    .build();

            // When - 生成结算单（单月，应生成 4 份，每个站点1份）
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 应生成 4 份结算单（4个站点）
            assertEquals(4, results.size(), "单月周期应生成 4 份结算单（4个站点）");

            // US 站点结算单（第1份，序号 001）
            SettlementGenerateResult usResult = results.get(0);
            DocumentSettlement usSettlement = usResult.getSettlement();

            // 验证周期正确
            assertEquals(LocalDate.of(2025, 1, 1), usSettlement.getPeriodStart());
            assertEquals(LocalDate.of(2025, 1, 31), usSettlement.getPeriodEnd());

            // 验证 US 站点包含两个 MSKU
            List<DocumentSettlementItem> usItems = usResult.getItems();
            assertEquals(2, usItems.size(), "US 站点应包含 2 个 MSKU");

            Set<String> usMskus = usItems.stream()
                    .map(DocumentSettlementItem::getMsku)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("MSUS-A001", "MSUS-B001"), usMskus);

            // 验证总数量 = 100 + 50 = 150
            assertEquals(150, usSettlement.getTotalQuantity(),
                    "US 站点总数量应为 150");
        }

        /**
         * 单月周期输入时，MSKU 应按字母升序排列
         *
         * <p>验证 SettlementGenerator 的 aggregateAndSort 逻辑在单月周期下正常工作。</p>
         *
         * <p>Validates: Requirements 3.1</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("单月周期输入时，MSKU 应按字母升序排列")
        void testGenerate_SingleMonthPeriod_ShouldSortMskuAlphabetically() {
            // Given - 单月周期，MSKU 故意乱序
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 3, 1))
                    .periodEnd(LocalDate.of(2025, 3, 31))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("USD")
                                    .msku("MSUS-C001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("30.00"))
                                    .quantity(10)
                                    .build(),
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("USD")
                                    .msku("MSUS-A001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("10.00"))
                                    .quantity(20)
                                    .build()
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - US 站点的 MSKU 应按字母升序排列
            SettlementGenerateResult usResult = results.get(0);
            List<DocumentSettlementItem> usItems = usResult.getItems();
            assertEquals(2, usItems.size());
            assertEquals("MSUS-A001", usItems.get(0).getMsku(), "第1个 MSKU 应为 MSUS-A001");
            assertEquals("MSUS-C001", usItems.get(1).getMsku(), "第2个 MSKU 应为 MSUS-C001");
        }

        /**
         * 单月周期输入时，金额计算应正确（BigDecimal 精确计算）
         *
         * <p>验证 amount = unitPrice × quantity，精度为 4 位小数。</p>
         *
         * <p>Validates: Requirements 3.1</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("单月周期输入时，金额计算应正确")
        void testGenerate_SingleMonthPeriod_ShouldCalculateAmountCorrectly() {
            // Given - 单月周期，单个 MSKU
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 2, 1))
                    .periodEnd(LocalDate.of(2025, 2, 28))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("USD")
                                    .msku("MSUS-A001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("12.5000"))
                                    .quantity(8)
                                    .build()
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 金额 = 12.5000 × 8 = 100.0000
            SettlementGenerateResult usResult = results.get(0);
            DocumentSettlementItem item = usResult.getItems().get(0);
            assertEquals(new BigDecimal("100.0000"), item.getAmount(),
                    "金额应为 12.5000 × 8 = 100.0000");
            assertEquals(new BigDecimal("100.0000"), usResult.getSettlement().getTotalAmount(),
                    "总金额应为 100.0000");
        }
    }

    // ==================== 站点分组保留测试 ====================

    @Nested
    @DisplayName("站点分组保留 - 各站点独立生成结算单")
    class SiteGroupPreservationTest {

        /**
         * 多站点数据时，各站点应独立生成结算单
         *
         * <p>构造多站点数据（不同 siteCode 前缀的 MSKU），
         * 验证各站点独立生成结算单，站点分组逻辑不受影响。</p>
         *
         * <p>Validates: Requirements 3.4</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("多站点数据时，各站点应独立生成结算单")
        void testGenerate_MultiSiteData_ShouldGenerateIndependentSettlements() {
            // Given - 单月周期，包含 US 和 UK 站点数据
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 1, 1))
                    .periodEnd(LocalDate.of(2025, 1, 31))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("USD")
                                    .msku("MSUS-A001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("10.00"))
                                    .quantity(100)
                                    .build(),
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("GBP")
                                    .msku("MSUK-B001")
                                    .currency("GBP")
                                    .unitPrice(new BigDecimal("8.00"))
                                    .quantity(60)
                                    .build()
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 应生成 4 份结算单（4个站点，按 SiteCode 枚举顺序）
            assertEquals(4, results.size());

            // US 站点（序号 001，第1份）
            SettlementGenerateResult usResult = results.get(0);
            assertEquals("USD", usResult.getSettlement().getSiteCode());
            assertEquals("001", usResult.getSettlement().getSiteSequence());
            assertEquals(1, usResult.getItems().size(), "US 站点应只有 1 个 MSKU");
            assertEquals("MSUS-A001", usResult.getItems().get(0).getMsku());
            assertEquals(100, usResult.getSettlement().getTotalQuantity());

            // UK 站点（序号 003，第3份）
            SettlementGenerateResult ukResult = results.get(2);
            assertEquals("GBP", ukResult.getSettlement().getSiteCode());
            assertEquals("003", ukResult.getSettlement().getSiteSequence());
            assertEquals(1, ukResult.getItems().size(), "UK 站点应只有 1 个 MSKU");
            assertEquals("MSUK-B001", ukResult.getItems().get(0).getMsku());
            assertEquals(60, ukResult.getSettlement().getTotalQuantity());

            // CA 站点（序号 002，第2份）- 无数据，应为空
            SettlementGenerateResult caResult = results.get(1);
            assertEquals("CAD", caResult.getSettlement().getSiteCode());
            assertEquals(0, caResult.getItems().size(), "CA 站点应无数据");
            assertEquals(0, caResult.getSettlement().getTotalQuantity());

            // EU 站点（序号 004，第4份）- 无数据，应为空
            SettlementGenerateResult euResult = results.get(3);
            assertEquals("EUR", euResult.getSettlement().getSiteCode());
            assertEquals(0, euResult.getItems().size(), "EU 站点应无数据");
            assertEquals(0, euResult.getSettlement().getTotalQuantity());
        }

        /**
         * 站点分组应支持 siteCode 字段匹配（货币代码）
         *
         * <p>验证 siteCode 字段优先于 MSKU 前缀进行站点匹配。</p>
         *
         * <p>Validates: Requirements 3.4</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("站点分组应支持 siteCode 字段匹配")
        void testGenerate_SiteCodeMatching_ShouldGroupBySiteCode() {
            // Given - 使用 siteCode 字段（货币代码）进行匹配
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 1, 1))
                    .periodEnd(LocalDate.of(2025, 1, 31))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .transactionDate(null)
                                    .siteCode("EUR")
                                    .msku("MSEU-D001")
                                    .currency("EUR")
                                    .unitPrice(new BigDecimal("15.00"))
                                    .quantity(30)
                                    .build()
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - EU 站点（第4份）应包含数据
            SettlementGenerateResult euResult = results.get(3);
            assertEquals("EUR", euResult.getSettlement().getSiteCode());
            assertEquals(1, euResult.getItems().size());
            assertEquals("MSEU-D001", euResult.getItems().get(0).getMsku());
            assertEquals(30, euResult.getSettlement().getTotalQuantity());
        }
    }

    // ==================== INV 文件类型保留测试 ====================

    @Nested
    @DisplayName("INV 文件类型保留 - 生成 INV 格式不变")
    class InvFileTypePreservationTest {

        /**
         * 生成的 INV 文件类型应为 DocumentInv
         *
         * <p>验证 InvGenerator 生成的结果类型为 InvGenerateResult，
         * 包含 DocumentInv 主表和 DocumentInvItem 明细，文件类型不变。</p>
         *
         * <p>Validates: Requirements 3.1</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("InvGenerator 应生成 DocumentInv 类型的 INV 发票")
        void testGenerate_InvFileType_ShouldBeDocumentInv() {
            // Given - 构造结算单结果
            DocumentSettlement settlement = new DocumentSettlement();
            settlement.setDocumentNo("20250205001");
            settlement.setSettlementDate(LocalDate.of(2025, 2, 5));
            settlement.setPeriodStart(LocalDate.of(2025, 1, 1));
            settlement.setPeriodEnd(LocalDate.of(2025, 1, 31));
            settlement.setSiteCode("USD");
            settlement.setSiteSequence("001");
            settlement.setBuyerName("东莞市慕声商贸有限公司");
            settlement.setSellerName("Hong Kong Andeo Group Limited");
            settlement.setTotalQuantity(5);
            settlement.setTotalAmount(new BigDecimal("50.0000"));

            DocumentSettlementItem item = new DocumentSettlementItem();
            item.setLineNo(1);
            item.setMsku("MSUS-A001");
            item.setCurrency("USD");
            item.setUnitPrice(new BigDecimal("10.0000"));
            item.setQuantity(5);
            item.setAmount(new BigDecimal("50.0000"));

            SettlementGenerateResult settlementResult = SettlementGenerateResult.builder()
                    .settlement(settlement)
                    .items(List.of(item))
                    .build();

            DocumentPartyProperties party = createTestParty();

            // When - 生成 INV
            List<InvGenerateResult> invResults = InvGenerator.generate(
                    List.of(settlementResult), 1, party);

            // Then - 验证 INV 文件类型
            assertEquals(1, invResults.size(), "应生成 1 份 INV");
            InvGenerateResult invResult = invResults.get(0);

            // 验证 INV 主表为 DocumentInv 类型
            assertNotNull(invResult.getInv(), "INV 主表不应为 null");
            assertInstanceOf(DocumentInv.class, invResult.getInv(),
                    "INV 主表应为 DocumentInv 类型");

            // 验证 INV 明细正确复制
            assertEquals(1, invResult.getItems().size(), "INV 应有 1 条明细");
            assertEquals("MSUS-A001", invResult.getItems().get(0).getMsku());
            assertEquals(5, invResult.getItems().get(0).getQuantity());

            // 验证站点信息正确传递
            assertEquals("USD", invResult.getInv().getSiteCode());
            assertEquals("001", invResult.getInv().getSiteSequence());

            // 验证合计正确传递
            assertEquals(5, invResult.getInv().getTotalQuantity());
            assertEquals(new BigDecimal("50.0000"), invResult.getInv().getTotalAmount());
        }

        /**
         * INV 应正确复制卖方/买方/银行信息
         *
         * <p>验证 InvGenerator 从 DocumentPartyProperties 正确读取并填充交易方信息。</p>
         *
         * <p>Validates: Requirements 3.1</p>
         *
         * @author wanhua
         * 10:30 2026年07月14日
         */
        @Test
        @DisplayName("INV 应正确复制卖方/买方/银行信息")
        void testGenerate_InvPartyInfo_ShouldCopyFromConfig() {
            // Given
            DocumentSettlement settlement = new DocumentSettlement();
            settlement.setDocumentNo("20250205001");
            settlement.setSettlementDate(LocalDate.of(2025, 2, 5));
            settlement.setPeriodStart(LocalDate.of(2025, 1, 1));
            settlement.setPeriodEnd(LocalDate.of(2025, 1, 31));
            settlement.setSiteCode("USD");
            settlement.setSiteSequence("001");
            settlement.setBuyerName("东莞市慕声商贸有限公司");
            settlement.setSellerName("Hong Kong Andeo Group Limited");
            settlement.setTotalQuantity(0);
            settlement.setTotalAmount(BigDecimal.ZERO);

            SettlementGenerateResult settlementResult = SettlementGenerateResult.builder()
                    .settlement(settlement)
                    .items(List.of())
                    .build();

            DocumentPartyProperties party = createTestParty();

            // When
            List<InvGenerateResult> invResults = InvGenerator.generate(
                    List.of(settlementResult), 1, party);

            // Then - 验证交易方信息
            DocumentInv inv = invResults.get(0).getInv();
            assertEquals(party.getSellerName(), inv.getSellerName(), "卖方名称应一致");
            assertEquals(party.getSellerAddress(), inv.getSellerAddress(), "卖方地址应一致");
            assertEquals(party.getSellerPhone(), inv.getSellerPhone(), "卖方电话应一致");
            assertEquals(party.getBuyerName(), inv.getBuyerName(), "买方名称应一致");
            assertEquals(party.getBuyerAddress(), inv.getBuyerAddress(), "买方地址应一致");
            assertEquals(party.getBuyerPhone(), inv.getBuyerPhone(), "买方电话应一致");
            assertEquals(party.getBankAccountName(), inv.getBankAccountName(), "银行账户名应一致");
            assertEquals(party.getBankAccountNumber(), inv.getBankAccountNumber(), "银行账号应一致");
            assertEquals(party.getBankName(), inv.getBankName(), "银行名称应一致");
            assertEquals(party.getBankAddress(), inv.getBankAddress(), "银行地址应一致");
            assertEquals(party.getSwiftCode(), inv.getSwiftCode(), "SWIFT代码应一致");
        }
    }

    // ==================== 辅助方法 ====================

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
