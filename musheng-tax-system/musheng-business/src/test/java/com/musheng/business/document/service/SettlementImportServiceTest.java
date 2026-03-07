package com.musheng.business.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.SettlementImportRequest;
import com.musheng.business.document.dto.SettlementImportRequest.SettlementImportItem;
import com.musheng.business.document.entity.SettlementImportData;
import com.musheng.business.document.mapper.SettlementImportDataMapper;
import com.musheng.business.document.service.impl.SettlementImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SettlementImportService 单元测试
 *
 * <p>使用 Mockito mock SettlementImportDataMapper，
 * 验证导入、查询、删除逻辑的正确性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class SettlementImportServiceTest {

    @InjectMocks
    private SettlementImportServiceImpl settlementImportService;

    @Mock
    private SettlementImportDataMapper settlementImportDataMapper;

    // ==================== importSettlementData 测试 ====================

    /**
     * 测试正常导入场景：多条数据
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testImportSettlementData_NormalCase_ShouldInsertAllItems() {
        // Given
        List<SettlementImportItem> items = List.of(
                SettlementImportItem.builder()
                        .siteCode("USD")
                        .msku("MSUS-001")
                        .currency("USD")
                        .unitPrice(new BigDecimal("10.50"))
                        .quantity(100)
                        .amount(new BigDecimal("1050.00"))
                        .build(),
                SettlementImportItem.builder()
                        .siteCode("CAD")
                        .msku("MSCA-002")
                        .currency("CAD")
                        .unitPrice(new BigDecimal("8.00"))
                        .quantity(50)
                        .amount(new BigDecimal("400.00"))
                        .build()
        );

        SettlementImportRequest request = SettlementImportRequest.builder()
                .shopId(1L)
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .items(items)
                .build();

        when(settlementImportDataMapper.insert(any(SettlementImportData.class))).thenReturn(1);

        // When
        int result = settlementImportService.importSettlementData(request);

        // Then
        assertEquals(2, result);
        verify(settlementImportDataMapper, times(2)).insert(any(SettlementImportData.class));

        // 验证插入的数据字段正确
        ArgumentCaptor<SettlementImportData> captor = ArgumentCaptor.forClass(SettlementImportData.class);
        verify(settlementImportDataMapper, times(2)).insert(captor.capture());
        List<SettlementImportData> capturedData = captor.getAllValues();

        // 验证第一条数据
        SettlementImportData first = capturedData.get(0);
        assertEquals(1L, first.getShopId());
        assertEquals(LocalDate.of(2025, 9, 2), first.getPeriodStart());
        assertEquals(LocalDate.of(2025, 9, 8), first.getPeriodEnd());
        assertEquals("USD", first.getSiteCode());
        assertEquals("MSUS-001", first.getMsku());
        assertEquals("USD", first.getCurrency());
        assertEquals(new BigDecimal("10.50"), first.getUnitPrice());
        assertEquals(100, first.getQuantity());
        assertEquals(new BigDecimal("1050.00"), first.getAmount());
        assertNotNull(first.getImportBatchId());

        // 验证第二条数据
        SettlementImportData second = capturedData.get(1);
        assertEquals("CAD", second.getSiteCode());
        assertEquals("MSCA-002", second.getMsku());

        // 验证两条数据使用相同的批次ID
        assertEquals(first.getImportBatchId(), second.getImportBatchId());
    }

    /**
     * 测试空数据场景：items 列表为空
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testImportSettlementData_EmptyItems_ShouldReturnZero() {
        // Given
        SettlementImportRequest request = SettlementImportRequest.builder()
                .shopId(1L)
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .items(new ArrayList<>())
                .build();

        // When
        int result = settlementImportService.importSettlementData(request);

        // Then
        assertEquals(0, result);
        verify(settlementImportDataMapper, never()).insert(any(SettlementImportData.class));
    }

    /**
     * 测试空数据场景：items 为 null
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testImportSettlementData_NullItems_ShouldReturnZero() {
        // Given
        SettlementImportRequest request = SettlementImportRequest.builder()
                .shopId(1L)
                .periodStart(LocalDate.of(2025, 9, 2))
                .periodEnd(LocalDate.of(2025, 9, 8))
                .items(null)
                .build();

        // When
        int result = settlementImportService.importSettlementData(request);

        // Then
        assertEquals(0, result);
        verify(settlementImportDataMapper, never()).insert(any(SettlementImportData.class));
    }

    // ==================== queryByPeriod 测试 ====================

    /**
     * 测试按周期查询：返回匹配数据
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testQueryByPeriod_NormalCase_ShouldReturnMatchingData() {
        // Given
        LocalDate periodStart = LocalDate.of(2025, 9, 2);
        LocalDate periodEnd = LocalDate.of(2025, 9, 8);

        SettlementImportData data1 = new SettlementImportData();
        data1.setMsku("MSUS-001");
        data1.setQuantity(100);

        SettlementImportData data2 = new SettlementImportData();
        data2.setMsku("MSCA-002");
        data2.setQuantity(50);

        when(settlementImportDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(data1, data2));

        // When
        List<SettlementImportData> result = settlementImportService.queryByPeriod(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(settlementImportDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== deleteByPeriod 测试 ====================

    /**
     * 测试按周期删除：返回删除条数
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void testDeleteByPeriod_NormalCase_ShouldReturnDeletedCount() {
        // Given
        LocalDate periodStart = LocalDate.of(2025, 9, 2);
        LocalDate periodEnd = LocalDate.of(2025, 9, 8);

        when(settlementImportDataMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(5);

        // When
        int result = settlementImportService.deleteByPeriod(periodStart, periodEnd);

        // Then
        assertEquals(5, result);
        verify(settlementImportDataMapper, times(1)).delete(any(LambdaQueryWrapper.class));
    }
}
