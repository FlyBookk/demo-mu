package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentPartyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DnGenerator 交易方配置化测试
 *
 * <p>验证 DnGenerator.generate() 新增 party 参数后，
 * 生成的 DN 主表中供应商/客户信息来自 DocumentPartyConfig 而非硬编码。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
class DnGeneratorPartyConfigTest {

    private DocumentPartyConfig party;
    private List<ShipmentInput> shipments;
    private LocalDate anchor;

    @BeforeEach
    void setUp() {
        // 构建测试用交易方配置
        party = new DocumentPartyConfig();
        party.setSiteCode("US");
        party.setSupplierName("Test Supplier Co. Ltd");
        party.setCustomerNameTc("測試客戶公司");

        anchor = LocalDate.of(2025, 9, 2);

        ShipmentInput shipment = ShipmentInput.builder()
                .shipmentNo("FBA123456")
                .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                .streetAddress("123 Test St")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .country("US")
                .items(List.of(
                        ShipmentInput.MskuItem.builder().msku("US-MSKU-001").quantity(50).build()
                ))
                .build();
        shipments = List.of(shipment);
    }

    /**
     * 验证：生成的DN供应商名称来自 party 配置
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseSupplierNameFromConfig() {
        // When
        List<DnGenerateResult> results = DnGenerator.generate(anchor, shipments, 1, party);

        // Then
        assertFalse(results.isEmpty());
        DocumentDn dn = results.get(0).getDn();
        assertEquals("Test Supplier Co. Ltd", dn.getSupplierName());
    }

    /**
     * 验证：生成的DN客户名称来自 party 配置（customerNameTc）
     *
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Test
    void testGenerate_WithPartyConfig_ShouldUseCustomerNameFromConfig() {
        // When
        List<DnGenerateResult> results = DnGenerator.generate(anchor, shipments, 1, party);

        // Then
        DocumentDn dn = results.get(0).getDn();
        assertEquals("測試客戶公司", dn.getCustomerName());
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
        caParty.setSupplierName("Canada Supplier Co. Ltd");
        caParty.setCustomerNameTc("加拿大客戶公司");

        // When
        List<DnGenerateResult> usResults = DnGenerator.generate(anchor, shipments, 1, party);
        List<DnGenerateResult> caResults = DnGenerator.generate(anchor, shipments, 1, caParty);

        // Then
        assertEquals("Test Supplier Co. Ltd", usResults.get(0).getDn().getSupplierName());
        assertEquals("Canada Supplier Co. Ltd", caResults.get(0).getDn().getSupplierName());
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
                () -> DnGenerator.generate(anchor, shipments, 1, null));
    }
}
