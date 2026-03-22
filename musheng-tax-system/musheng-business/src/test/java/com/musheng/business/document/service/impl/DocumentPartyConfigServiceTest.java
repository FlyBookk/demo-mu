package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.DocumentPartyConfigDTO;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.mapper.DocumentPartyConfigMapper;
import com.musheng.business.document.vo.DocumentPartyConfigVO;
import com.musheng.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocumentPartyConfigServiceImpl 单元测试
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@ExtendWith(MockitoExtension.class)
class DocumentPartyConfigServiceTest {

    @InjectMocks
    private DocumentPartyConfigServiceImpl service;

    @Mock
    private DocumentPartyConfigMapper mapper;

    private DocumentPartyConfigDTO buildDTO(String siteCode) {
        return DocumentPartyConfigDTO.builder()
                .siteCode(siteCode)
                .buyerName("东莞市慕声商贸有限公司")
                .buyerAddress("广东省东莞市虎门镇")
                .sellerName("Hong Kong Andeo Group Limited")
                .supplierName("Hong Kong Andeo Group Limited")
                .customerNameTc("東莞市慕聲商貿有限公司")
                .build();
    }

    private DocumentPartyConfig buildEntity(Long id, String siteCode) {
        DocumentPartyConfig config = new DocumentPartyConfig();
        config.setId(id);
        config.setSiteCode(siteCode);
        config.setBuyerName("东莞市慕声商贸有限公司");
        config.setSellerName("Hong Kong Andeo Group Limited");
        config.setSupplierName("Hong Kong Andeo Group Limited");
        config.setCustomerNameTc("東莞市慕聲商貿有限公司");
        config.setDeleted(false);
        return config;
    }

    // ==================== list 测试 ====================

    @Test
    void testList_WhenConfigsExist_ShouldReturnList() {
        // Given
        DocumentPartyConfig config = buildEntity(1L, "US");
        when(mapper.selectList(any())).thenReturn(List.of(config));

        // When
        List<DocumentPartyConfigVO> result = service.list();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSiteCode()).isEqualTo("US");
        assertThat(result.get(0).getBuyerName()).isEqualTo("东莞市慕声商贸有限公司");
    }

    @Test
    void testList_WhenNoConfigs_ShouldReturnEmptyList() {
        // Given
        when(mapper.selectList(any())).thenReturn(List.of());

        // When
        List<DocumentPartyConfigVO> result = service.list();

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== add 测试 ====================

    @Test
    void testAdd_WhenSiteCodeNotExists_ShouldInsert() {
        // Given
        DocumentPartyConfigDTO dto = buildDTO("US");
        when(mapper.selectOne(any())).thenReturn(null); // siteCode 不存在

        // When
        service.add(dto);

        // Then
        ArgumentCaptor<DocumentPartyConfig> captor = ArgumentCaptor.forClass(DocumentPartyConfig.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSiteCode()).isEqualTo("US");
        assertThat(captor.getValue().getBuyerName()).isEqualTo("东莞市慕声商贸有限公司");
    }

    @Test
    void testAdd_WhenSiteCodeAlreadyExists_ShouldThrowBusinessException() {
        // Given
        DocumentPartyConfigDTO dto = buildDTO("US");
        DocumentPartyConfig existing = buildEntity(1L, "US");
        when(mapper.selectOne(any())).thenReturn(existing); // siteCode 已存在

        // When & Then
        assertThatThrownBy(() -> service.add(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该站点配置已存在");

        verify(mapper, never()).insert(any());
    }

    // ==================== update 测试 ====================

    @Test
    void testUpdate_WhenConfigExists_ShouldUpdate() {
        // Given
        DocumentPartyConfigDTO dto = buildDTO("US");
        dto.setId(1L);
        dto.setBuyerName("新买方名称");
        DocumentPartyConfig existing = buildEntity(1L, "US");
        when(mapper.selectById(1L)).thenReturn(existing);

        // When
        service.update(dto);

        // Then
        ArgumentCaptor<DocumentPartyConfig> captor = ArgumentCaptor.forClass(DocumentPartyConfig.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getBuyerName()).isEqualTo("新买方名称");
    }

    @Test
    void testUpdate_WhenConfigNotExists_ShouldThrowBusinessException() {
        // Given
        DocumentPartyConfigDTO dto = buildDTO("US");
        dto.setId(999L);
        when(mapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置不存在");

        verify(mapper, never()).updateById(any());
    }

    // ==================== delete 测试 ====================

    @Test
    void testDelete_WhenConfigExists_ShouldLogicDelete() {
        // Given
        DocumentPartyConfig existing = buildEntity(1L, "US");
        when(mapper.selectById(1L)).thenReturn(existing);

        // When
        service.delete(1L);

        // Then
        verify(mapper).deleteById(1L);
    }

    @Test
    void testDelete_WhenConfigNotExists_ShouldThrowBusinessException() {
        // Given
        when(mapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置不存在");

        verify(mapper, never()).deleteById(any(Long.class));
    }

    // ==================== getBySiteCode 测试 ====================

    @Test
    void testGetBySiteCode_WhenExists_ShouldReturnConfig() {
        // Given
        DocumentPartyConfig config = buildEntity(1L, "US");
        when(mapper.selectOne(any())).thenReturn(config);

        // When
        DocumentPartyConfig result = service.getBySiteCode("US");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSiteCode()).isEqualTo("US");
    }

    @Test
    void testGetBySiteCode_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        when(mapper.selectOne(any())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> service.getBySiteCode("XX"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("XX")
                .hasMessageContaining("交易方配置不存在");
    }
}
