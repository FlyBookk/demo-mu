package com.musheng.business.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.DnGenerateRequest;
import com.musheng.business.document.dto.PoGenerateRequest;
import com.musheng.business.document.dto.SettlementGenerateRequest;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.generator.*;
import com.musheng.business.document.mapper.*;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.business.fbashipment.mapper.FbaShipmentItemMapper;
import com.musheng.business.document.service.impl.DocumentGenerateServiceImpl;
import com.musheng.common.context.ShopContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DocumentGenerateService 单元测试
 *
 * <p>使用 Mockito mock 所有 Mapper 和 Generator，
 * 验证生成器被正确调用、Mapper 的 insert 方法被正确调用、返回值正确。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DocumentGenerateServiceTest {

    @InjectMocks
    private DocumentGenerateServiceImpl documentGenerateService;

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
    @Mock
    private FbaShipmentMapper fbaShipmentMapper;
    @Mock
    private FbaShipmentItemMapper fbaShipmentItemMapper;
    @Mock
    private SettlementImportDataMapper settlementImportDataMapper;

    /**
     * 设置 ShopContext，所有测试方法需要店铺上下文
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @BeforeEach
    void setUp() {
        ShopContext.setShopId(1L);
    }

    /**
     * 清理 ShopContext，避免测试间 ThreadLocal 泄漏
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @AfterEach
    void tearDown() {
        ShopContext.clear();
    }

    // ==================== generatePo 测试 ====================

    /**
     * 测试正常场景：生成PO采购订单
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGeneratePo_NormalCase_ShouldPersistAndReturn() {
        // Given
        PoGenerateRequest request = PoGenerateRequest.builder()
                .shipmentIds(List.of(1L, 2L))
                .build();

        DocumentPo mockPo = new DocumentPo();
        mockPo.setDocumentNo("20250902001");
        mockPo.setPoDate(LocalDate.of(2025, 9, 2));
        mockPo.setTotalQuantity(100);
        mockPo.setShipmentCount(2);

        DocumentPoItem mockItem1 = new DocumentPoItem();
        mockItem1.setMsku("MSUS-001");
        mockItem1.setQuantity(50);

        DocumentPoItem mockItem2 = new DocumentPoItem();
        mockItem2.setMsku("MSUS-002");
        mockItem2.setQuantity(50);

        PoGenerateResult mockResult = PoGenerateResult.builder()
                .po(mockPo)
                .items(List.of(mockItem1, mockItem2))
                .build();

        when(documentPoMapper.insert(any(DocumentPo.class))).thenReturn(1);
        when(documentPoItemMapper.insert(any(DocumentPoItem.class))).thenReturn(1);

        // When - 使用 MockedStatic 模拟 PoGenerator
        try (MockedStatic<PoGenerator> mockedPoGen = mockStatic(PoGenerator.class)) {
            mockedPoGen.when(() -> PoGenerator.generate(anyList(), anyInt()))
                    .thenReturn(List.of(mockResult));

            DocumentPo result = documentGenerateService.generatePo(request);

            // Then
            assertNotNull(result);
            assertEquals("20250902001", result.getDocumentNo());

            // 验证 Mapper insert 被调用
            verify(documentPoMapper, times(1)).insert(any(DocumentPo.class));
            verify(documentPoItemMapper, times(2)).insert(any(DocumentPoItem.class));
        }
    }

    /**
     * 测试空数据场景：货件ID列表为空
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGeneratePo_EmptyShipmentIds_ShouldReturnNull() {
        // Given
        PoGenerateRequest request = PoGenerateRequest.builder()
                .shipmentIds(List.of())
                .build();

        // When - 使用 MockedStatic 模拟 PoGenerator
        try (MockedStatic<PoGenerator> mockedPoGen = mockStatic(PoGenerator.class)) {
            mockedPoGen.when(() -> PoGenerator.generate(anyList(), anyInt()))
                    .thenReturn(List.of());

            DocumentPo result = documentGenerateService.generatePo(request);

            // Then
            assertNull(result);
            verify(documentPoMapper, never()).insert(any(DocumentPo.class));
        }
    }

    // ==================== generateDn 测试 ====================

    /**
     * 测试正常场景：生成DN送货单
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGenerateDn_NormalCase_ShouldPersistAndReturn() {
        // Given
        DnGenerateRequest request = DnGenerateRequest.builder()
                .anchorDate(LocalDate.of(2025, 5, 22))
                .shipmentIds(List.of(1L, 2L))
                .build();

        DocumentDn mockDn = new DocumentDn();
        mockDn.setDocumentNo("20250612001");
        mockDn.setDnDate(LocalDate.of(2025, 6, 12));
        mockDn.setTotalQuantity(80);

        DocumentDnItem mockItem = new DocumentDnItem();
        mockItem.setMsku("MSUS-001");
        mockItem.setQuantity(80);

        DnGenerateResult mockResult = DnGenerateResult.builder()
                .dn(mockDn)
                .items(List.of(mockItem))
                .build();

        when(documentDnMapper.insert(any(DocumentDn.class))).thenReturn(1);
        when(documentDnItemMapper.insert(any(DocumentDnItem.class))).thenReturn(1);

        // When
        try (MockedStatic<DnGenerator> mockedDnGen = mockStatic(DnGenerator.class)) {
            mockedDnGen.when(() -> DnGenerator.generate(any(LocalDate.class), anyList(), anyInt()))
                    .thenReturn(List.of(mockResult));

            DocumentDn result = documentGenerateService.generateDn(request);

            // Then
            assertNotNull(result);
            assertEquals("20250612001", result.getDocumentNo());

            verify(documentDnMapper, times(1)).insert(any(DocumentDn.class));
            verify(documentDnItemMapper, times(1)).insert(any(DocumentDnItem.class));
        }
    }

    // ==================== generateSettlements 测试 ====================

    /**
     * 测试正常场景：生成4份结算单
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGenerateSettlements_NormalCase_ShouldPersist4Settlements() {
        // Given
        SettlementGenerateRequest request = SettlementGenerateRequest.builder()
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .build();

        List<SettlementGenerateResult> mockResults = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DocumentSettlement settlement = new DocumentSettlement();
            settlement.setDocumentNo("2025090900" + (i + 1));
            settlement.setSettlementDate(LocalDate.of(2025, 9, 9));
            settlement.setTotalQuantity(10);
            settlement.setTotalAmount(BigDecimal.valueOf(100));

            DocumentSettlementItem item = new DocumentSettlementItem();
            item.setMsku("MSUS-00" + (i + 1));
            item.setQuantity(10);
            item.setAmount(BigDecimal.valueOf(100));

            mockResults.add(SettlementGenerateResult.builder()
                    .settlement(settlement)
                    .items(List.of(item))
                    .build());
        }

        when(documentSettlementMapper.insert(any(DocumentSettlement.class))).thenReturn(1);
        when(documentSettlementItemMapper.insert(any(DocumentSettlementItem.class))).thenReturn(1);

        // When
        try (MockedStatic<SettlementGenerator> mockedSettGen = mockStatic(SettlementGenerator.class)) {
            mockedSettGen.when(() -> SettlementGenerator.generate(any(SettlementInput.class), anyInt()))
                    .thenReturn(mockResults);

            List<DocumentSettlement> results = documentGenerateService.generateSettlements(request);

            // Then
            assertNotNull(results);
            assertEquals(4, results.size());

            verify(documentSettlementMapper, times(4)).insert(any(DocumentSettlement.class));
            verify(documentSettlementItemMapper, times(4)).insert(any(DocumentSettlementItem.class));
        }
    }

    // ==================== generateInvoices 测试 ====================

    /**
     * 测试正常场景：根据结算单ID生成4份INV
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGenerateInvoices_NormalCase_ShouldPersist4Invoices() {
        // Given
        List<Long> settlementIds = List.of(1L, 2L, 3L, 4L);

        // 模拟查询结算单数据
        List<SettlementGenerateResult> mockSettlementResults = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DocumentSettlement settlement = new DocumentSettlement();
            settlement.setId(settlementIds.get(i));
            settlement.setDocumentNo("2025090900" + (i + 1));
            settlement.setSettlementDate(LocalDate.of(2025, 9, 9));
            settlement.setSiteCode(List.of("USD", "CAD", "GBP", "EUR").get(i));
            settlement.setSiteSequence(String.format("%03d", i + 1));
            settlement.setTotalQuantity(10);
            settlement.setTotalAmount(BigDecimal.valueOf(100));

            DocumentSettlementItem sItem = new DocumentSettlementItem();
            sItem.setLineNo(1);
            sItem.setMsku("MSUS-00" + (i + 1));
            sItem.setQuantity(10);
            sItem.setUnitPrice(BigDecimal.TEN);
            sItem.setAmount(BigDecimal.valueOf(100));

            mockSettlementResults.add(SettlementGenerateResult.builder()
                    .settlement(settlement)
                    .items(List.of(sItem))
                    .build());
        }

        // 模拟 INV 生成结果
        List<InvGenerateResult> mockInvResults = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DocumentInv inv = new DocumentInv();
            inv.setDocumentNo("2025091000" + (i + 1));
            inv.setInvDate(LocalDate.of(2025, 9, 10));
            inv.setTotalQuantity(10);
            inv.setTotalAmount(BigDecimal.valueOf(100));

            DocumentInvItem invItem = new DocumentInvItem();
            invItem.setLineNo(1);
            invItem.setMsku("MSUS-00" + (i + 1));
            invItem.setQuantity(10);
            invItem.setUnitPrice(BigDecimal.TEN);
            invItem.setAmount(BigDecimal.valueOf(100));

            mockInvResults.add(InvGenerateResult.builder()
                    .inv(inv)
                    .items(List.of(invItem))
                    .build());
        }

        // 模拟 Mapper 查询
        for (int i = 0; i < 4; i++) {
            when(documentSettlementMapper.selectById(settlementIds.get(i)))
                    .thenReturn(mockSettlementResults.get(i).getSettlement());
        }

        when(documentSettlementItemMapper.selectList(any()))
                .thenReturn(List.of(mockSettlementResults.get(0).getItems().get(0)));

        when(documentInvMapper.insert(any(DocumentInv.class))).thenReturn(1);
        when(documentInvItemMapper.insert(any(DocumentInvItem.class))).thenReturn(1);

        // When
        try (MockedStatic<InvGenerator> mockedInvGen = mockStatic(InvGenerator.class)) {
            mockedInvGen.when(() -> InvGenerator.generate(anyList(), anyInt()))
                    .thenReturn(mockInvResults);

            List<DocumentInv> results = documentGenerateService.generateInvoices(settlementIds);

            // Then
            assertNotNull(results);
            assertEquals(4, results.size());

            verify(documentInvMapper, times(4)).insert(any(DocumentInv.class));
            verify(documentInvItemMapper, times(4)).insert(any(DocumentInvItem.class));
        }
    }

    /**
     * 测试空数据场景：结算单ID列表为空
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testGenerateInvoices_EmptySettlementIds_ShouldReturnEmptyList() {
        // Given
        List<Long> settlementIds = List.of();

        // When
        List<DocumentInv> results = documentGenerateService.generateInvoices(settlementIds);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(documentInvMapper, never()).insert(any(DocumentInv.class));
    }

    // ==================== buildSettlementInput 逻辑删除兼容测试 ====================

    /**
     * 测试 buildSettlementInput 在有逻辑删除数据时仍能正确工作
     *
     * <p>Given: Mapper 返回仅包含 del_flag=0 的数据（模拟 @TableLogic 自动过滤）
     * When: 调用 generateSettlements
     * Then: 生成器接收到的 SettlementInput 仅包含未删除的数据</p>
     *
     * <p>验证：@TableLogic 注解确保 selectList 自动过滤 del_flag=1 的记录，
     * 现有结算单生成逻辑无需修改即可兼容推导数据的逻辑删除。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testBuildSettlementInput_WithLogicalDeletedData_ShouldOnlyReturnActiveRecords() {
        // Given - 模拟 @TableLogic 自动过滤后，Mapper 仅返回 del_flag=0 的记录
        SettlementImportData activeRecord1 = new SettlementImportData();
        activeRecord1.setSiteCode("US");
        activeRecord1.setMsku("MSUS-001");
        activeRecord1.setCurrency("USD");
        activeRecord1.setUnitPrice(new BigDecimal("10.5000"));
        activeRecord1.setQuantity(100);
        activeRecord1.setDelFlag(0);

        SettlementImportData activeRecord2 = new SettlementImportData();
        activeRecord2.setSiteCode("CA");
        activeRecord2.setMsku("MSCA-001");
        activeRecord2.setCurrency("CAD");
        activeRecord2.setUnitPrice(new BigDecimal("8.0000"));
        activeRecord2.setQuantity(50);
        activeRecord2.setDelFlag(0);

        // @TableLogic 确保 selectList 不会返回 del_flag=1 的记录
        when(settlementImportDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(activeRecord1, activeRecord2));

        SettlementGenerateRequest request = SettlementGenerateRequest.builder()
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .build();

        // 模拟结算单生成结果
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentNo("20250909001");
        settlement.setSettlementDate(LocalDate.of(2025, 9, 9));
        settlement.setTotalQuantity(100);
        settlement.setTotalAmount(BigDecimal.valueOf(1050));

        DocumentSettlementItem item = new DocumentSettlementItem();
        item.setMsku("MSUS-001");
        item.setQuantity(100);
        item.setAmount(BigDecimal.valueOf(1050));

        SettlementGenerateResult mockResult = SettlementGenerateResult.builder()
                .settlement(settlement)
                .items(List.of(item))
                .build();

        when(documentSettlementMapper.insert(any(DocumentSettlement.class))).thenReturn(1);
        when(documentSettlementItemMapper.insert(any(DocumentSettlementItem.class))).thenReturn(1);

        // When
        try (MockedStatic<SettlementGenerator> mockedSettGen = mockStatic(SettlementGenerator.class)) {
            mockedSettGen.when(() -> SettlementGenerator.generate(any(SettlementInput.class), anyInt()))
                    .thenAnswer(invocation -> {
                        // 验证传入的 SettlementInput 仅包含活跃记录
                        SettlementInput input = invocation.getArgument(0);
                        assertEquals(2, input.getItems().size(),
                                "SettlementInput 应仅包含 2 条未删除的记录");
                        assertEquals("MSUS-001", input.getItems().get(0).getMsku());
                        assertEquals("MSCA-001", input.getItems().get(1).getMsku());
                        assertEquals(LocalDate.of(2025, 9, 2), input.getPeriodStart());
                        assertEquals(LocalDate.of(2025, 9, 8), input.getPeriodEnd());
                        return List.of(mockResult);
                    });

            List<DocumentSettlement> results = documentGenerateService.generateSettlements(request);

            // Then
            assertNotNull(results);
            assertEquals(1, results.size());

            // 验证 Mapper.selectList 被调用（buildSettlementInput 查询了数据）
            verify(settlementImportDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }
    }

    /**
     * 测试 buildSettlementInput 在无数据时返回空列表
     *
     * <p>Given: Mapper 返回空列表（所有数据都已逻辑删除或无数据）
     * When: 调用 generateSettlements
     * Then: 返回空结算单列表</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testBuildSettlementInput_AllDataLogicallyDeleted_ShouldReturnEmptyList() {
        // Given - 模拟所有数据都已逻辑删除，@TableLogic 过滤后返回空列表
        when(settlementImportDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        SettlementGenerateRequest request = SettlementGenerateRequest.builder()
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .build();

        // When
        try (MockedStatic<SettlementGenerator> mockedSettGen = mockStatic(SettlementGenerator.class)) {
            mockedSettGen.when(() -> SettlementGenerator.generate(any(SettlementInput.class), anyInt()))
                    .thenReturn(List.of());

            List<DocumentSettlement> results = documentGenerateService.generateSettlements(request);

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty(), "所有数据已逻辑删除时应返回空列表");

            // 验证 Mapper.selectList 被调用
            verify(settlementImportDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }
    }
}
