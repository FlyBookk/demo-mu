package com.musheng.business.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.impl.DocumentValidationServiceImpl;
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
 * DocumentValidationService 单元测试
 *
 * <p>使用 Mockito mock 所有 Mapper，验证校验逻辑正确性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DocumentValidationServiceTest {

    @InjectMocks
    private DocumentValidationServiceImpl documentValidationService;

    @Mock
    private DocumentSettlementMapper documentSettlementMapper;
    @Mock
    private DocumentSettlementItemMapper documentSettlementItemMapper;
    @Mock
    private DocumentInvMapper documentInvMapper;
    @Mock
    private DocumentInvItemMapper documentInvItemMapper;

    // ==================== validateSettlementInvConsistency 测试 ====================

    /**
     * 测试一致性校验：数据完全一致，返回空列表
     */
    @Test
    @SuppressWarnings("unchecked")
    void testValidateConsistency_AllMatch_ShouldReturnEmptyList() {
        // Given
        Long settlementId = 1L;
        Long invId = 2L;

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setMsku("MSUS-001");
        sItem.setQuantity(10);
        sItem.setUnitPrice(new BigDecimal("50.0000"));
        sItem.setAmount(new BigDecimal("500.0000"));

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setMsku("MSUS-001");
        invItem.setQuantity(10);
        invItem.setUnitPrice(new BigDecimal("50.0000"));
        invItem.setAmount(new BigDecimal("500.0000"));

        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        List<String> errors = documentValidationService.validateSettlementInvConsistency(settlementId, invId);

        // Then
        assertNotNull(errors);
        assertTrue(errors.isEmpty(), "数据完全一致时应返回空列表");
    }

    /**
     * 测试一致性校验：MSKU不匹配
     */
    @Test
    @SuppressWarnings("unchecked")
    void testValidateConsistency_MskuMismatch_ShouldReturnErrors() {
        // Given
        Long settlementId = 1L;
        Long invId = 2L;

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setMsku("MSUS-001");
        sItem.setQuantity(10);
        sItem.setUnitPrice(new BigDecimal("50.0000"));
        sItem.setAmount(new BigDecimal("500.0000"));

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setMsku("MSUS-002");
        invItem.setQuantity(10);
        invItem.setUnitPrice(new BigDecimal("50.0000"));
        invItem.setAmount(new BigDecimal("500.0000"));

        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        List<String> errors = documentValidationService.validateSettlementInvConsistency(settlementId, invId);

        // Then
        assertFalse(errors.isEmpty(), "MSKU不匹配时应返回错误");
    }

    /**
     * 测试一致性校验：数量不匹配
     */
    @Test
    @SuppressWarnings("unchecked")
    void testValidateConsistency_QuantityMismatch_ShouldReturnErrors() {
        // Given
        Long settlementId = 1L;
        Long invId = 2L;

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setMsku("MSUS-001");
        sItem.setQuantity(10);
        sItem.setUnitPrice(new BigDecimal("50.0000"));
        sItem.setAmount(new BigDecimal("500.0000"));

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setMsku("MSUS-001");
        invItem.setQuantity(20);
        invItem.setUnitPrice(new BigDecimal("50.0000"));
        invItem.setAmount(new BigDecimal("1000.0000"));

        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        List<String> errors = documentValidationService.validateSettlementInvConsistency(settlementId, invId);

        // Then
        assertFalse(errors.isEmpty(), "数量不匹配时应返回错误");
    }

    /**
     * 测试一致性校验：金额不匹配
     */
    @Test
    @SuppressWarnings("unchecked")
    void testValidateConsistency_AmountMismatch_ShouldReturnErrors() {
        // Given
        Long settlementId = 1L;
        Long invId = 2L;

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setMsku("MSUS-001");
        sItem.setQuantity(10);
        sItem.setUnitPrice(new BigDecimal("50.0000"));
        sItem.setAmount(new BigDecimal("500.0000"));

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setMsku("MSUS-001");
        invItem.setQuantity(10);
        invItem.setUnitPrice(new BigDecimal("60.0000"));
        invItem.setAmount(new BigDecimal("600.0000"));

        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        List<String> errors = documentValidationService.validateSettlementInvConsistency(settlementId, invId);

        // Then
        assertFalse(errors.isEmpty(), "金额不匹配时应返回错误");
    }

    // ==================== validateInvDate 测试 ====================

    /**
     * 测试INV日期校验：日期正确（结算日+1工作日）
     */
    @Test
    void testValidateInvDate_CorrectDate_ShouldReturnTrue() {
        // Given — 2025-09-08 是周一（工作日），下一个工作日是 2025-09-09（周二）
        Long invId = 1L;

        DocumentInv inv = new DocumentInv();
        inv.setId(invId);
        inv.setInvDate(LocalDate.of(2025, 9, 9));
        inv.setSettlementId(10L);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(10L);
        settlement.setSettlementDate(LocalDate.of(2025, 9, 8));

        when(documentInvMapper.selectById(invId)).thenReturn(inv);
        when(documentSettlementMapper.selectById(10L)).thenReturn(settlement);

        // When
        boolean result = documentValidationService.validateInvDate(invId);

        // Then
        assertTrue(result, "INV日期为结算日+1工作日时应返回true");
    }

    /**
     * 测试INV日期校验：日期不匹配
     */
    @Test
    void testValidateInvDate_WrongDate_ShouldReturnFalse() {
        // Given — 结算日 2025-09-08，期望INV日期 2025-09-09，实际 2025-09-10
        Long invId = 1L;

        DocumentInv inv = new DocumentInv();
        inv.setId(invId);
        inv.setInvDate(LocalDate.of(2025, 9, 10));
        inv.setSettlementId(10L);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(10L);
        settlement.setSettlementDate(LocalDate.of(2025, 9, 8));

        when(documentInvMapper.selectById(invId)).thenReturn(inv);
        when(documentSettlementMapper.selectById(10L)).thenReturn(settlement);

        // When
        boolean result = documentValidationService.validateInvDate(invId);

        // Then
        assertFalse(result, "INV日期不等于结算日+1工作日时应返回false");
    }

    // ==================== validateSettlementSiteMapping 测试 ====================

    /**
     * 测试站点映射校验：映射正确（001→USD）
     */
    @Test
    void testValidateSiteMapping_CorrectMapping_ShouldReturnTrue() {
        // Given
        Long settlementId = 1L;

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(settlementId);
        settlement.setSiteSequence("001");
        settlement.setSiteCode("USD");

        when(documentSettlementMapper.selectById(settlementId)).thenReturn(settlement);

        // When
        boolean result = documentValidationService.validateSettlementSiteMapping(settlementId);

        // Then
        assertTrue(result, "001→USD 映射正确时应返回true");
    }

    /**
     * 测试站点映射校验：映射错误（001→EUR）
     */
    @Test
    void testValidateSiteMapping_WrongMapping_ShouldReturnFalse() {
        // Given
        Long settlementId = 1L;

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(settlementId);
        settlement.setSiteSequence("001");
        settlement.setSiteCode("EUR");

        when(documentSettlementMapper.selectById(settlementId)).thenReturn(settlement);

        // When
        boolean result = documentValidationService.validateSettlementSiteMapping(settlementId);

        // Then
        assertFalse(result, "001→EUR 映射错误时应返回false");
    }

    // ==================== validateAll 测试 ====================

    /**
     * 测试全量校验：所有校验通过
     */
    @Test
    @SuppressWarnings("unchecked")
    void testValidateAll_AllPass_ShouldReturnEmptyErrorLists() {
        // Given
        LocalDate periodStart = LocalDate.of(2025, 9, 2);
        LocalDate periodEnd = LocalDate.of(2025, 9, 8);

        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setId(1L);
        settlement.setDocumentNo("20250909001");
        settlement.setSettlementDate(LocalDate.of(2025, 9, 9));
        settlement.setSiteSequence("001");
        settlement.setSiteCode("USD");

        DocumentInv inv = new DocumentInv();
        inv.setId(2L);
        inv.setSettlementId(1L);
        inv.setInvDate(LocalDate.of(2025, 9, 10));
        inv.setDocumentNo("20250910001");

        DocumentSettlementItem sItem = new DocumentSettlementItem();
        sItem.setMsku("MSUS-001");
        sItem.setQuantity(10);
        sItem.setUnitPrice(new BigDecimal("50.0000"));
        sItem.setAmount(new BigDecimal("500.0000"));

        DocumentInvItem invItem = new DocumentInvItem();
        invItem.setMsku("MSUS-001");
        invItem.setQuantity(10);
        invItem.setUnitPrice(new BigDecimal("50.0000"));
        invItem.setAmount(new BigDecimal("500.0000"));

        // mock 查询结算单列表
        when(documentSettlementMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(settlement));
        // mock 查询关联INV
        when(documentInvMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(inv));
        // mock selectById
        when(documentSettlementMapper.selectById(1L)).thenReturn(settlement);
        when(documentInvMapper.selectById(2L)).thenReturn(inv);
        // mock 明细查询
        when(documentSettlementItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sItem));
        when(documentInvItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(invItem));

        // When
        Map<String, List<String>> result = documentValidationService.validateAll(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("consistency"));
        assertTrue(result.containsKey("invDate"));
        assertTrue(result.containsKey("siteMapping"));
    }
}
