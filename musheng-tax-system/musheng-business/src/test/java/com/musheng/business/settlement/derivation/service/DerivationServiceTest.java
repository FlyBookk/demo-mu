package com.musheng.business.settlement.derivation.service;

import com.musheng.business.document.mapper.SettlementImportDataMapper;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.settlement.derivation.dto.DerivationRequest;
import com.musheng.business.settlement.derivation.dto.SiteCostInput;
import com.musheng.business.settlement.derivation.service.impl.SettlementDerivationServiceImpl;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.business.settlement.derivation.vo.DerivationResultVO;
import com.musheng.business.settlement.derivation.vo.MskuDerivationItem;
import com.musheng.business.settlement.derivation.vo.SiteDerivationResult;
import com.musheng.common.context.ShopContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SettlementDerivationService.derive 方法单元测试（按季度）
 *
 * <p>使用 Mockito mock SalesDataAggregator 和 ExchangeRateMapper，
 * 验证完整推导流程、异常场景、精度和排序。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DerivationServiceTest {

    @Mock
    private SalesDataAggregator salesDataAggregator;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @Mock
    private SettlementImportDataMapper settlementImportDataMapper;

    @InjectMocks
    private SettlementDerivationServiceImpl service;

    /** 2025-Q3 对应日期范围：2025-07-01 ~ 2025-09-30 */
    private static final LocalDate Q3_START = LocalDate.of(2025, 7, 1);
    private static final LocalDate Q3_END = LocalDate.of(2025, 9, 30);
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
     * 测试完整推导流程：正常场景（按季度）
     *
     * <p>US 站点有 2 个 MSKU，采购成本 10000 CNY，汇率 7.2，
     * 预期采购成本（原币）= 10000 / 7.2，单价 = 采购成本（原币）/ 总数量</p>
     */
    @Test
    void testDerive_CompleteFlow_ShouldReturnCorrectResult() {
        // Given
        Map<String, Map<String, Integer>> netSalesMap = new LinkedHashMap<>();
        Map<String, Integer> usMskuMap = new LinkedHashMap<>();
        usMskuMap.put("MSKU-A", 100);
        usMskuMap.put("MSKU-B", 200);
        netSalesMap.put("US", usMskuMap);

        AggregationResult aggResult = AggregationResult.builder()
                .netSalesMap(netSalesMap)
                .orderRateMap(new HashMap<>())
                .build();

        when(salesDataAggregator.aggregateNetSales(eq(SHOP_ID), eq(Q3_START), eq(Q3_END)))
                .thenReturn(aggResult);

        // mock 汇率查询
        List<ExchangeRate> usdRates = new ArrayList<>();
        ExchangeRate rate1 = new ExchangeRate();
        rate1.setCurrencyCode("USD");
        rate1.setRate(new BigDecimal("7.1"));
        rate1.setRateDate(Q3_START);
        ExchangeRate rate2 = new ExchangeRate();
        rate2.setCurrencyCode("USD");
        rate2.setRate(new BigDecimal("7.3"));
        rate2.setRateDate(Q3_START.plusDays(1));
        usdRates.add(rate1);
        usdRates.add(rate2);
        when(exchangeRateMapper.selectList(any())).thenReturn(usdRates);

        // mock 删除旧数据
        when(settlementImportDataMapper.logicalDeleteByPeriodAndSite(any(), any(), any())).thenReturn(0);
        when(settlementImportDataMapper.insert(any())).thenReturn(1);

        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-Q3")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When
        DerivationResultVO result = service.derive(request);

        // Then
        assertNotNull(result, "推导结果不应为 null");
        assertEquals("2025-Q3", result.getStartQuarter());
        assertEquals("2025-Q3", result.getEndQuarter());
        assertEquals(Q3_START, result.getPeriodStart());
        assertEquals(Q3_END, result.getPeriodEnd());
        assertNotNull(result.getAverageRates());

        // 平均汇率 = (7.1 + 7.3) / 2 = 7.200000
        BigDecimal expectedAvgRate = new BigDecimal("7.200000");
        assertEquals(expectedAvgRate, result.getAverageRates().get("USD"));

        // 验证站点结果
        assertEquals(1, result.getSiteResults().size());
        SiteDerivationResult usResult = result.getSiteResults().get(0);
        assertEquals("US", usResult.getSiteCode());
        assertEquals("USD", usResult.getCurrency());
        assertEquals(new BigDecimal("10000.00"), usResult.getProcurementCostCny());
        assertEquals(expectedAvgRate, usResult.getAverageExchangeRate());
        assertEquals(300, usResult.getTotalQuantity());

        // 验证 MSKU 明细
        assertEquals(2, usResult.getItems().size());
        assertEquals("MSKU-A", usResult.getItems().get(0).getMsku());
        assertEquals("MSKU-B", usResult.getItems().get(1).getMsku());
    }

    /**
     * 测试缺少汇率数据时应抛出异常
     */
    @Test
    void testDerive_MissingExchangeRate_ShouldThrowException() {
        // Given
        Map<String, Map<String, Integer>> netSalesMap = new LinkedHashMap<>();
        netSalesMap.put("US", Map.of("MSKU-A", 100));

        AggregationResult aggResult = AggregationResult.builder()
                .netSalesMap(netSalesMap)
                .orderRateMap(new HashMap<>())
                .build();

        when(salesDataAggregator.aggregateNetSales(eq(SHOP_ID), eq(Q3_START), eq(Q3_END)))
                .thenReturn(aggResult);

        // 返回空汇率列表
        when(exchangeRateMapper.selectList(any())).thenReturn(Collections.emptyList());

        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-Q3")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When & Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.derive(request));
        assertTrue(ex.getMessage().contains("USD") && ex.getMessage().contains("汇率"),
                "异常消息应包含缺少的货币代码和汇率提示");
    }

    /**
     * 测试单价精度：保留 4 位小数（HALF_UP）
     */
    @Test
    void testDerive_UnitPricePrecision_ShouldBeFourDecimalPlaces() {
        // Given - 构造一个会产生无限小数的场景
        Map<String, Map<String, Integer>> netSalesMap = new LinkedHashMap<>();
        netSalesMap.put("US", Map.of("MSKU-A", 3));

        AggregationResult aggResult = AggregationResult.builder()
                .netSalesMap(netSalesMap)
                .orderRateMap(new HashMap<>())
                .build();

        when(salesDataAggregator.aggregateNetSales(eq(SHOP_ID), eq(Q3_START), eq(Q3_END)))
                .thenReturn(aggResult);

        List<ExchangeRate> rates = new ArrayList<>();
        ExchangeRate usdRate = new ExchangeRate();
        usdRate.setCurrencyCode("USD");
        usdRate.setRate(new BigDecimal("7.0"));
        usdRate.setRateDate(Q3_START);
        rates.add(usdRate);
        when(exchangeRateMapper.selectList(any())).thenReturn(rates);

        when(settlementImportDataMapper.logicalDeleteByPeriodAndSite(any(), any(), any())).thenReturn(0);
        when(settlementImportDataMapper.insert(any())).thenReturn(1);

        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-Q3")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When
        DerivationResultVO result = service.derive(request);

        // Then
        SiteDerivationResult usResult = result.getSiteResults().get(0);
        MskuDerivationItem item = usResult.getItems().get(0);

        assertEquals(2, item.getUnitPrice().scale(), "单价应保留 2 位小数");
        assertEquals(2, item.getAmount().scale(), "金额应保留 2 位小数");
        BigDecimal expectedAmount = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedAmount, item.getAmount(), "金额应等于数量×单价");
    }

    /**
     * 测试 MSKU 按字母升序排列
     */
    @Test
    void testDerive_MskuSortedAlphabetically_ShouldBeTrue() {
        // Given - 故意乱序放入 MSKU
        Map<String, Map<String, Integer>> netSalesMap = new LinkedHashMap<>();
        Map<String, Integer> usMskuMap = new LinkedHashMap<>();
        usMskuMap.put("MSKU-C", 50);
        usMskuMap.put("MSKU-A", 100);
        usMskuMap.put("MSKU-B", 200);
        netSalesMap.put("US", usMskuMap);

        AggregationResult aggResult = AggregationResult.builder()
                .netSalesMap(netSalesMap)
                .orderRateMap(new HashMap<>())
                .build();

        when(salesDataAggregator.aggregateNetSales(eq(SHOP_ID), eq(Q3_START), eq(Q3_END)))
                .thenReturn(aggResult);

        List<ExchangeRate> rates = new ArrayList<>();
        ExchangeRate usdRate = new ExchangeRate();
        usdRate.setCurrencyCode("USD");
        usdRate.setRate(new BigDecimal("7.2"));
        usdRate.setRateDate(Q3_START);
        rates.add(usdRate);
        when(exchangeRateMapper.selectList(any())).thenReturn(rates);

        when(settlementImportDataMapper.logicalDeleteByPeriodAndSite(any(), any(), any())).thenReturn(0);
        when(settlementImportDataMapper.insert(any())).thenReturn(1);

        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-Q3")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When
        DerivationResultVO result = service.derive(request);

        // Then - MSKU 应按字母升序排列
        List<MskuDerivationItem> items = result.getSiteResults().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("MSKU-A", items.get(0).getMsku());
        assertEquals("MSKU-B", items.get(1).getMsku());
        assertEquals("MSKU-C", items.get(2).getMsku());
    }

    /**
     * 测试无效季度格式应抛出异常
     */
    @Test
    void testDerive_InvalidQuarterFormat_ShouldThrowException() {
        // Given
        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-03")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.derive(request));
    }

    /**
     * 测试开始季度晚于结束季度应抛出异常
     */
    @Test
    void testDerive_StartAfterEnd_ShouldThrowException() {
        // Given
        DerivationRequest request = DerivationRequest.builder()
                .startQuarter("2025-Q4")
                .endQuarter("2025-Q3")
                .siteCosts(List.of(
                        SiteCostInput.builder().siteCode("US").costCny(new BigDecimal("10000.00")).build()
                ))
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.derive(request));
    }
}
