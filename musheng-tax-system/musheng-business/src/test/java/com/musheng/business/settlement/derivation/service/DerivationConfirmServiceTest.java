package com.musheng.business.settlement.derivation.service;

import com.musheng.business.document.entity.SettlementImportData;
import com.musheng.business.document.mapper.SettlementImportDataMapper;
import com.musheng.business.settlement.derivation.dto.DerivationConfirmRequest;
import com.musheng.business.settlement.derivation.dto.MskuConfirmItem;
import com.musheng.business.settlement.derivation.dto.SiteConfirmData;
import com.musheng.business.settlement.derivation.service.impl.SettlementDerivationServiceImpl;
import com.musheng.business.settlement.derivation.vo.DerivationConfirmResultVO;
import com.musheng.common.context.ShopContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * confirm 方法单元测试（按季度）
 *
 * <p>使用 Mockito mock SettlementImportDataMapper，
 * 验证写入、字段完整性、空数据场景。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DerivationConfirmServiceTest {

    @Mock
    private SettlementImportDataMapper settlementImportDataMapper;

    @Mock
    private SalesDataAggregator salesDataAggregator;

    @InjectMocks
    private SettlementDerivationServiceImpl service;

    private static final Long SHOP_ID = 1L;

    @BeforeEach
    void setUp() {
        ShopContext.setShopId(SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        ShopContext.clear();
    }

    /**
     * 测试写入：验证返回正确的 batchId 和 recordCount
     *
     * <p>Given: 1 个站点（US），2 个 MSKU 明细
     * When: 调用 confirm
     * Then: 返回 batchId 非空，recordCount=2</p>
     */
    @Test
    void testConfirm_Write_ShouldReturnCorrectResult() {
        // Given
        when(settlementImportDataMapper.insert(any(SettlementImportData.class))).thenReturn(1);
        when(settlementImportDataMapper.logicalDeleteByPeriodAndSite(any(), any(), any())).thenReturn(0);

        DerivationConfirmRequest request = buildRequest(List.of(
                buildSiteData("US", "USD", new BigDecimal("10000.00"), new BigDecimal("7.200000"),
                        List.of(
                                buildMskuItem("MSKU-A", 100, new BigDecimal("4.6296"), new BigDecimal("462.9600")),
                                buildMskuItem("MSKU-B", 200, new BigDecimal("4.6296"), new BigDecimal("925.9200"))
                        ))
        ));

        // When
        DerivationConfirmResultVO result = service.confirm(request);

        // Then
        assertNotNull(result, "确认结果不应为 null");
        assertNotNull(result.getSettlementBatchId(), "batchId 不应为 null");
        assertFalse(result.getSettlementBatchId().isEmpty(), "batchId 不应为空字符串");
        assertEquals(2, result.getRecordCount(), "应写入 2 条记录");

        verify(settlementImportDataMapper, times(2)).insert(any(SettlementImportData.class));
    }

    /**
     * 测试字段完整性：验证写入的 SettlementImportData 所有字段正确填充
     *
     * <p>Given: 1 个站点（CA），1 个 MSKU 明细
     * When: 调用 confirm
     * Then: 捕获写入的实体，验证所有字段映射正确</p>
     */
    @Test
    void testConfirm_FieldCompleteness_ShouldFillAllFields() {
        // Given
        when(settlementImportDataMapper.insert(any(SettlementImportData.class))).thenReturn(1);
        when(settlementImportDataMapper.logicalDeleteByPeriodAndSite(any(), any(), any())).thenReturn(0);

        BigDecimal costCny = new BigDecimal("8000.00");
        BigDecimal avgRate = new BigDecimal("5.300000");
        BigDecimal unitPrice = new BigDecimal("3.0189");
        BigDecimal amount = new BigDecimal("150.9450");

        DerivationConfirmRequest request = buildRequest(List.of(
                buildSiteData("CA", "CAD", costCny, avgRate,
                        List.of(buildMskuItem("MSKU-X", 50, unitPrice, amount)))
        ));

        // When
        service.confirm(request);

        // Then - 捕获写入的实体
        ArgumentCaptor<SettlementImportData> captor = ArgumentCaptor.forClass(SettlementImportData.class);
        verify(settlementImportDataMapper).insert(captor.capture());

        SettlementImportData saved = captor.getValue();
        assertEquals(SHOP_ID, saved.getShopId(), "shopId 应为当前店铺 ID");
        // 2025-Q3 对应 2025-07-01 ~ 2025-09-30
        assertEquals(LocalDate.of(2025, 7, 1), saved.getPeriodStart(), "periodStart 应正确");
        assertEquals(LocalDate.of(2025, 9, 30), saved.getPeriodEnd(), "periodEnd 应正确");
        assertEquals("CA", saved.getSiteCode(), "siteCode 应为 CA");
        assertEquals("MSKU-X", saved.getMsku(), "msku 应为 MSKU-X");
        assertEquals("CAD", saved.getCurrency(), "currency 应为 CAD");
        assertEquals(unitPrice, saved.getUnitPrice(), "unitPrice 应正确");
        assertEquals(50, saved.getQuantity(), "quantity 应为 50");
        assertEquals(amount, saved.getAmount(), "amount 应正确");
        assertNotNull(saved.getSettlementBatchId(), "settlementBatchId 不应为 null");
        assertEquals(costCny, saved.getProcurementCostCny(), "procurementCostCny 应正确");
        assertEquals(avgRate, saved.getAverageExchangeRate(), "averageExchangeRate 应正确");
    }

    /**
     * 测试空站点数据：应返回 0 条记录
     *
     * <p>Given: siteDataList 为空列表
     * When: 调用 confirm
     * Then: recordCount=0，不调用 mapper.insert</p>
     */
    @Test
    void testConfirm_EmptySiteData_ShouldReturnZeroRecords() {
        // Given
        DerivationConfirmRequest request = buildRequest(Collections.emptyList());

        // When
        DerivationConfirmResultVO result = service.confirm(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getRecordCount(), "空站点数据应返回 0 条记录");
        assertNotNull(result.getSettlementBatchId());

        verify(settlementImportDataMapper, never()).insert(any());
    }

    // ========== checkExistingData 测试 ==========

    /**
     * 测试 checkExistingData：某站点有数据时应返回 true（按季度）
     */
    @Test
    void testCheckExistingData_HasData_ShouldReturnTrue() {
        // Given - US 站点有数据（2025-Q3 对应 2025-07-01 ~ 2025-09-30）
        SettlementImportData existingRecord = new SettlementImportData();
        existingRecord.setId(1L);
        when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "US"))
                .thenReturn(List.of(existingRecord));
        // 其他站点无数据
        lenient().when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "CA"))
                .thenReturn(Collections.emptyList());
        lenient().when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "UK"))
                .thenReturn(Collections.emptyList());
        lenient().when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "DE"))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = service.checkExistingData("2025-Q3", "2025-Q3");

        // Then
        assertTrue(result, "某站点有数据时应返回 true");
    }

    /**
     * 测试 checkExistingData：所有站点无数据时应返回 false（按季度）
     */
    @Test
    void testCheckExistingData_NoData_ShouldReturnFalse() {
        // Given - 所有站点无数据
        when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "US"))
                .thenReturn(Collections.emptyList());
        when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "CA"))
                .thenReturn(Collections.emptyList());
        when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "UK"))
                .thenReturn(Collections.emptyList());
        when(settlementImportDataMapper.selectByPeriodAndSite(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30), "DE"))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = service.checkExistingData("2025-Q3", "2025-Q3");

        // Then
        assertFalse(result, "所有站点无数据时应返回 false");
    }

    // ========== 辅助方法 ==========

    private DerivationConfirmRequest buildRequest(List<SiteConfirmData> siteDataList) {
        return DerivationConfirmRequest.builder()
                .startQuarter("2025-Q3")
                .endQuarter("2025-Q3")
                .overwrite(false)
                .siteDataList(siteDataList)
                .build();
    }

    private SiteConfirmData buildSiteData(String siteCode, String currency,
                                          BigDecimal costCny, BigDecimal avgRate,
                                          List<MskuConfirmItem> items) {
        return SiteConfirmData.builder()
                .siteCode(siteCode)
                .currency(currency)
                .procurementCostCny(costCny)
                .averageExchangeRate(avgRate)
                .items(items)
                .build();
    }

    private MskuConfirmItem buildMskuItem(String msku, int quantity,
                                          BigDecimal unitPrice, BigDecimal amount) {
        return MskuConfirmItem.builder()
                .msku(msku)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .build();
    }
}
