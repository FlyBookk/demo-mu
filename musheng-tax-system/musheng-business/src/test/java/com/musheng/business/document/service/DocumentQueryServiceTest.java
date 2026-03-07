package com.musheng.business.document.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.document.dto.DocumentQueryRequest;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.impl.DocumentQueryServiceImpl;
import com.musheng.business.document.vo.*;
import com.musheng.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocumentQueryService 单元测试
 *
 * <p>使用 Mockito mock 所有 Mapper，验证查询逻辑正确性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @InjectMocks
    private DocumentQueryServiceImpl documentQueryService;

    @Mock
    private DocumentPoMapper documentPoMapper;
    @Mock
    private DocumentPoItemMapper documentPoItemMapper;
    @Mock
    private DocumentDnMapper documentDnMapper;
    @Mock
    private DocumentDnItemMapper documentDnItemMapper;
    @Mock
    private DocumentSettlementMapper documentSettlementMapper;
    @Mock
    private DocumentSettlementItemMapper documentSettlementItemMapper;
    @Mock
    private DocumentInvMapper documentInvMapper;
    @Mock
    private DocumentInvItemMapper documentInvItemMapper;

    // ==================== getPoDetail 测试 ====================

    /**
     * 测试PO详情查询：正常场景
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetPoDetail_NormalCase_ShouldReturnPoVO() {
        // Given
        Long poId = 1L;
        DocumentPo po = new DocumentPo();
        po.setId(poId);
        po.setDocumentNo("20250902001");
        po.setPoDate(LocalDate.of(2025, 9, 2));
        po.setBuyerName("东莞市慕声商贸有限公司");
        po.setSellerName("Hong Kong Andeo Group Limited");
        po.setTotalQuantity(100);
        po.setShipmentCount(2);

        DocumentPoItem item1 = new DocumentPoItem();
        item1.setId(10L);
        item1.setPoId(poId);
        item1.setShipmentNo("FBA001");
        item1.setMsku("MSUS-001");
        item1.setQuantity(50);
        item1.setFbaAddress("123 Main St, City, CA, 90001, US");
        item1.setSortOrder(1);

        DocumentPoItem item2 = new DocumentPoItem();
        item2.setId(11L);
        item2.setPoId(poId);
        item2.setShipmentNo("FBA001");
        item2.setMsku("MSUS-002");
        item2.setQuantity(50);
        item2.setSortOrder(2);

        when(documentPoMapper.selectById(poId)).thenReturn(po);
        when(documentPoItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(item1, item2));

        // When
        PoVO result = documentQueryService.getPoDetail(poId);

        // Then
        assertNotNull(result);
        assertEquals("20250902001", result.getDocumentNo());
        assertEquals("东莞市慕声商贸有限公司", result.getBuyerName());
        assertEquals(2, result.getItems().size());
        assertEquals("MSUS-001", result.getItems().get(0).getMsku());
        verify(documentPoMapper).selectById(poId);
    }

    /**
     * 测试PO详情查询：PO不存在
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetPoDetail_NotFound_ShouldReturnNull() {
        // Given
        when(documentPoMapper.selectById(999L)).thenReturn(null);

        // When
        PoVO result = documentQueryService.getPoDetail(999L);

        // Then
        assertNull(result);
    }

    // ==================== getDnDetail 测试 ====================

    /**
     * 测试DN详情查询：正常场景
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetDnDetail_NormalCase_ShouldReturnDnVO() {
        // Given
        Long dnId = 2L;
        DocumentDn dn = new DocumentDn();
        dn.setId(dnId);
        dn.setDocumentNo("20250602001");
        dn.setDnDate(LocalDate.of(2025, 6, 2));
        dn.setSupplierName("Hong Kong Andeo Group Limited");
        dn.setCustomerName("東莞市慕聲商貿有限公司");
        dn.setTotalQuantity(80);

        DocumentDnItem dnItem = new DocumentDnItem();
        dnItem.setId(20L);
        dnItem.setDnId(dnId);
        dnItem.setLineNo(1);
        dnItem.setMsku("MSUS-001");
        dnItem.setQuantity(80);
        dnItem.setShipmentNo("FBA001");

        when(documentDnMapper.selectById(dnId)).thenReturn(dn);
        when(documentDnItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(dnItem));

        // When
        DnVO result = documentQueryService.getDnDetail(dnId);

        // Then
        assertNotNull(result);
        assertEquals("20250602001", result.getDocumentNo());
        assertEquals(1, result.getItems().size());
        assertEquals("FBA001", result.getItems().get(0).getShipmentNo());
    }

    // ==================== getSettlementDetail 测试 ====================

    /**
     * 测试结算单详情查询：正常场景
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetSettlementDetail_NormalCase_ShouldReturnSettlementVO() {
        // Given
        Long settlementId = 3L;
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(settlementId);
        settlement.setDocumentNo("20250908001");
        settlement.setSettlementDate(LocalDate.of(2025, 9, 8));
        settlement.setPeriodStart(LocalDate.of(2025, 9, 2));
        settlement.setPeriodEnd(LocalDate.of(2025, 9, 8));
        settlement.setSiteCode("USD");
        settlement.setSiteSequence("001");
        settlement.setBuyerName("东莞市慕声商贸有限公司");
        settlement.setSellerName("Hong Kong Andeo Group Limited");
        settlement.setTotalQuantity(50);
        settlement.setTotalAmount(new BigDecimal("5000.0000"));

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setId(30L);
        sItem.setSettlementId(settlementId);
        sItem.setLineNo(1);
        sItem.setMsku("MSUS-001");
        sItem.setCurrency("USD");
        sItem.setUnitPrice(new BigDecimal("100.0000"));
        sItem.setQuantity(50);
        sItem.setAmount(new BigDecimal("5000.0000"));

        when(documentSettlementMapper.selectById(settlementId)).thenReturn(settlement);
        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));

        // When
        SettlementVO result = documentQueryService.getSettlementDetail(settlementId);

        // Then
        assertNotNull(result);
        assertEquals("20250908001", result.getDocumentNo());
        assertEquals("USD", result.getSiteCode());
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("5000.0000"), result.getItems().get(0).getAmount());
    }

    // ==================== getInvDetail 测试 ====================

    /**
     * 测试INV详情查询：正常场景（含银行信息）
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetInvDetail_NormalCase_ShouldReturnInvVOWithBankInfo() {
        // Given
        Long invId = 4L;
        DocumentInv inv = new DocumentInv();
        inv.setId(invId);
        inv.setDocumentNo("20250909001");
        inv.setInvDate(LocalDate.of(2025, 9, 9));
        inv.setSettlementId(3L);
        inv.setSiteCode("USD");
        inv.setSiteSequence("001");
        inv.setSellerName("Hong Kong Andeo Group Limited");
        inv.setBuyerName("Dongguan Musheng Trade Co., Ltd.");
        inv.setTotalQuantity(50);
        inv.setTotalAmount(new BigDecimal("5000.0000"));
        inv.setBankAccountName("Hong Kong Andeo Group Limited");
        inv.setBankAccountNumber("123456789");
        inv.setBankName("HSBC");
        inv.setBankAddress("Hong Kong");
        inv.setSwiftCode("HSBCHKHH");

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setId(40L);
        invItem.setInvId(invId);
        invItem.setLineNo(1);
        invItem.setMsku("MSUS-001");
        invItem.setQuantity(50);
        invItem.setUnitPrice(new BigDecimal("100.0000"));
        invItem.setAmount(new BigDecimal("5000.0000"));

        when(documentInvMapper.selectById(invId)).thenReturn(inv);
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        InvVO result = documentQueryService.getInvDetail(invId);

        // Then
        assertNotNull(result);
        assertEquals("20250909001", result.getDocumentNo());
        assertEquals("HSBCHKHH", result.getSwiftCode());
        assertEquals("123456789", result.getBankAccountNumber());
        assertEquals(1, result.getItems().size());
    }

    // ==================== listBySettlementPeriod 测试 ====================

    /**
     * 测试按结算周期查询关联单据
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testListBySettlementPeriod_ShouldReturnSettlementsAndInvs() {
        // Given
        LocalDate periodStart = LocalDate.of(2025, 9, 2);
        LocalDate periodEnd = LocalDate.of(2025, 9, 8);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(3L);
        settlement.setDocumentNo("20250908001");
        settlement.setSettlementDate(LocalDate.of(2025, 9, 8));
        settlement.setSiteCode("USD");
        settlement.setBuyerName("东莞市慕声商贸有限公司");
        settlement.setSellerName("Hong Kong Andeo Group Limited");
        settlement.setTotalQuantity(50);
        settlement.setTotalAmount(new BigDecimal("5000.0000"));

        DocumentInv inv = new DocumentInv();
        inv.setId(4L);
        inv.setDocumentNo("20250909001");
        inv.setInvDate(LocalDate.of(2025, 9, 9));
        inv.setSiteCode("USD");
        inv.setSellerName("Hong Kong Andeo Group Limited");
        inv.setBuyerName("Dongguan Musheng Trade Co., Ltd.");
        inv.setTotalQuantity(50);
        inv.setTotalAmount(new BigDecimal("5000.0000"));

        when(documentSettlementMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(settlement));
        when(documentInvMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(inv));

        // When
        List<DocumentListVO> result = documentQueryService.listBySettlementPeriod(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== getShipmentDocumentRelation 测试 ====================

    /**
     * 测试货件关联关系查询
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGetShipmentDocumentRelation_ShouldReturnPoAndDnInfo() {
        // Given
        String shipmentNo = "FBA001";

        DocumentPoItem poItem = new DocumentPoItem();
        poItem.setPoId(1L);
        poItem.setShipmentNo(shipmentNo);
        poItem.setMsku("MSUS-001");
        poItem.setQuantity(50);

        DocumentDnItem dnItem = new DocumentDnItem();
        dnItem.setDnId(2L);
        dnItem.setShipmentNo(shipmentNo);
        dnItem.setMsku("MSUS-001");
        dnItem.setQuantity(50);

        when(documentPoItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(poItem));
        when(documentDnItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(dnItem));

        // When
        Map<String, Object> result = documentQueryService.getShipmentDocumentRelation(shipmentNo);

        // Then
        assertNotNull(result);
        assertEquals(shipmentNo, result.get("shipmentNo"));
        assertNotNull(result.get("poItems"));
        assertNotNull(result.get("dnItems"));
    }

    // ==================== listDocuments 测试 ====================

    /**
     * 测试分页查询PO单据列表
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    @SuppressWarnings("unchecked")
    void testListDocuments_PoType_ShouldReturnPageResult() {
        // Given
        DocumentQueryRequest request = DocumentQueryRequest.builder()
                .documentType("PO")
                .pageNum(1)
                .pageSize(10)
                .build();

        DocumentPo po = new DocumentPo();
        po.setId(1L);
        po.setDocumentNo("20250902001");
        po.setPoDate(LocalDate.of(2025, 9, 2));
        po.setBuyerName("东莞市慕声商贸有限公司");
        po.setSellerName("Hong Kong Andeo Group Limited");
        po.setTotalQuantity(100);

        Page<DocumentPo> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(po));

        when(documentPoMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        // When
        PageResult<DocumentListVO> result = documentQueryService.listDocuments(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("20250902001", result.getRecords().get(0).getDocumentNo());
        assertEquals("PO", result.getRecords().get(0).getDocumentType());
    }
}
