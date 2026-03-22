package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InvGenerator 交易方配置化测试
 *
 * <p>验证 InvGenerator.generate() 参数类型从 DocumentPartyProperties 改为
 * DocumentPartyConfig 后，生成的 INV 中卖方/买方/银行信息来自 DocumentPartyConfig。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
class InvGeneratorPartyConfigTest {

    private DocumentPartyConfig party;
    private List<SettlementGenerateResult> settlementResults;

    @BeforeEach
    void setUp() {
        // 构建测试用交易方配置
        party = new DocumentPartyConfig();
        party.setSiteCode("US");
        party.setSellerName("Test Seller Co. Ltd");
        party.setSellerAddress("Test Seller Address");
        party.setSellerPhone("00852-12345678");
        party.setBuyerName("测试买方公司");
        party.setBuyerAddress("测试买方地址");
        party.setBuyerPhone("13800138000");
        party.setBuyerNameEn("Test Buyer Co., Ltd.");
        party.setBankAccountName("Test Seller Co. Ltd");
        party.setBankAccountNumber("012-878-0-999999-9");
        party.setBankName("Test Bank Limited");
        party.setBankAddress("Test Bank Address");
        party.setSwiftCode("TESTSWFT");

        // 构建测试结算单数据
        settlementResults = List.of(buildSettlementResult(
                LocalDate.of(2025, 9, 9), "001", "USD", "MSUS-A001", "10.0000", 5));
    }

    /**
     * 验证：生成的INV卖方名称来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseSellerNameFromConfig() {
        // When
        List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, party);

        // Then
        assertFalse(results.isEmpty());
        DocumentInv inv = results.get(0).getInv();
        assertEquals("Test Seller Co. Ltd", inv.getSellerName());
    }

    /**
     * 验证：生成的INV买方名称来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBuyerNameFromConfig() {
        // When
        List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, party);

        // Then
        DocumentInv inv = results.get(0).getInv();
        assertEquals("测试买方公司", inv.getBuyerName());
    }

    /**
     * 验证：生成的INV银行信息来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBankInfoFromConfig() {
        // When
        List<InvGenerateResult> results = InvGenerator.generate(settlementResults, 1, party);

        // Then
        DocumentInv inv = results.get(0).getInv();
        assertEquals("Test Seller Co. Ltd", inv.getBankAccountName());
        assertEquals("012-878-0-999999-9", inv.getBankAccountNumber());
        assertEquals("Test Bank Limited", inv.getBankName());
        assertEquals("Test Bank Address", inv.getBankAddress());
        assertEquals("TESTSWFT", inv.getSwiftCode());
    }

    /**
     * 验证：不同站点配置生成不同的交易方信息
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithDifferentPartyConfig_ShouldUseDifferentPartyInfo() {
        // Given - 加拿大站点配置
        DocumentPartyConfig caParty = new DocumentPartyConfig();
        caParty.setSiteCode("CA");
        caParty.setSellerName("Canada Seller Co. Ltd");
        caParty.setBuyerName("加拿大买方公司");
        caParty.setBankAccountName("Canada Seller Co. Ltd");
        caParty.setBankAccountNumber("CA-999999");
        caParty.setBankName("Canada Bank");
        caParty.setBankAddress("Canada Bank Address");
        caParty.setSwiftCode("CASWFT");

        // When
        List<InvGenerateResult> usResults = InvGenerator.generate(settlementResults, 1, party);
        List<InvGenerateResult> caResults = InvGenerator.generate(settlementResults, 1, caParty);

        // Then
        assertEquals("Test Seller Co. Ltd", usResults.get(0).getInv().getSellerName());
        assertEquals("Canada Seller Co. Ltd", caResults.get(0).getInv().getSellerName());
    }

    /**
     * 验证：party 为 null 时抛出 IllegalArgumentException
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithNullParty_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> InvGenerator.generate(settlementResults, 1, null));
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建单份结算单结果
     */
    private SettlementGenerateResult buildSettlementResult(
            LocalDate settlementDate, String siteSequence, String siteCode,
            String msku, String unitPrice, int quantity) {

        BigDecimal price = new BigDecimal(unitPrice);
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity))
                .setScale(4, RoundingMode.HALF_UP);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo("20250909" + siteSequence);
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(LocalDate.of(2025, 9, 1));
        settlement.setPeriodEnd(LocalDate.of(2025, 9, 30));
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
