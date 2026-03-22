package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentSettlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementGenerator 交易方配置化测试
 *
 * <p>验证 SettlementGenerator.generate() 新增 party 参数后，
 * 生成的结算单主表中买方/卖方信息来自 DocumentPartyConfig 而非硬编码。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
class SettlementGeneratorPartyConfigTest {

    private DocumentPartyConfig party;
    private SettlementInput input;

    @BeforeEach
    void setUp() {
        // 构建测试用交易方配置
        party = new DocumentPartyConfig();
        party.setSiteCode("US");
        party.setBuyerName("测试买方公司");
        party.setBuyerAddress("测试买方地址");
        party.setSellerName("Test Seller Co. Ltd");

        // 构建测试结算数据
        input = SettlementInput.builder()
                .periodStart(LocalDate.of(2025, 9, 1))
                .periodEnd(LocalDate.of(2025, 9, 30))
                .items(List.of(
                        SettlementInput.SettlementDataItem.builder()
                                .siteCode(null)
                                .msku("MSUS-A001")
                                .currency("USD")
                                .unitPrice(new BigDecimal("10.00"))
                                .quantity(5)
                                .build()
                ))
                .build();
    }

    /**
     * 验证：生成的结算单买方名称来自 party 配置，而非硬编码
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBuyerNameFromConfig() {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1, party);

        // Then
        assertFalse(results.isEmpty());
        DocumentSettlement settlement = results.get(0).getSettlement();
        assertEquals("测试买方公司", settlement.getBuyerName());
    }

    /**
     * 验证：生成的结算单买方地址来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBuyerAddressFromConfig() {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1, party);

        // Then
        DocumentSettlement settlement = results.get(0).getSettlement();
        assertEquals("测试买方地址", settlement.getBuyerAddress());
    }

    /**
     * 验证：生成的结算单卖方名称来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseSellerNameFromConfig() {
        // When
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1, party);

        // Then
        DocumentSettlement settlement = results.get(0).getSettlement();
        assertEquals("Test Seller Co. Ltd", settlement.getSellerName());
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
        caParty.setBuyerName("加拿大买方公司");
        caParty.setBuyerAddress("加拿大买方地址");
        caParty.setSellerName("Canada Seller Co. Ltd");

        // When
        List<SettlementGenerateResult> usResults = SettlementGenerator.generate(input, 1, party);
        List<SettlementGenerateResult> caResults = SettlementGenerator.generate(input, 1, caParty);

        // Then
        assertEquals("测试买方公司", usResults.get(0).getSettlement().getBuyerName());
        assertEquals("加拿大买方公司", caResults.get(0).getSettlement().getBuyerName());
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
                () -> SettlementGenerator.generate(input, 1, null));
    }
}
