package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PoGenerator 交易方配置化测试
 *
 * <p>验证 PoGenerator.generate() 新增 party 参数后，
 * 生成的 PO 主表中买方/卖方信息来自 DocumentPartyConfig 而非硬编码。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
class PoGeneratorPartyConfigTest {

    private DocumentPartyConfig party;
    private List<ShipmentInput> shipments;

    @BeforeEach
    void setUp() {
        // 构建测试用交易方配置
        party = new DocumentPartyConfig();
        party.setSiteCode("US");
        party.setBuyerName("测试买方公司");
        party.setBuyerAddress("测试买方地址");
        party.setSellerName("Test Seller Co. Ltd");

        // 构建测试货件
        ShipmentInput shipment = ShipmentInput.builder()
                .shipmentNo("FBA123456")
                .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                .streetAddress("123 Test St")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .country("US")
                .items(List.of(
                        ShipmentInput.MskuItem.builder().msku("US-MSKU-001").quantity(100).build()
                ))
                .build();
        shipments = List.of(shipment);
    }

    /**
     * 验证：生成的PO买方名称来自 party 配置，而非硬编码
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBuyerNameFromConfig() {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1, party);

        // Then
        assertFalse(results.isEmpty());
        DocumentPo po = results.get(0).getPo();
        assertEquals("测试买方公司", po.getBuyerName());
    }

    /**
     * 验证：生成的PO买方地址来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseBuyerAddressFromConfig() {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1, party);

        // Then
        DocumentPo po = results.get(0).getPo();
        assertEquals("测试买方地址", po.getBuyerAddress());
    }

    /**
     * 验证：生成的PO卖方名称来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseSellerNameFromConfig() {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1, party);

        // Then
        DocumentPo po = results.get(0).getPo();
        assertEquals("Test Seller Co. Ltd", po.getSellerName());
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
        List<PoGenerateResult> usResults = PoGenerator.generate(shipments, 1, party);
        List<PoGenerateResult> caResults = PoGenerator.generate(shipments, 1, caParty);

        // Then
        assertEquals("测试买方公司", usResults.get(0).getPo().getBuyerName());
        assertEquals("加拿大买方公司", caResults.get(0).getPo().getBuyerName());
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
                () -> PoGenerator.generate(shipments, 1, null));
    }
}
