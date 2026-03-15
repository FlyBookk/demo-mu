package com.musheng.business.settlement.derivation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.document.entity.SettlementImportData;
import com.musheng.business.document.mapper.SettlementImportDataMapper;
import com.musheng.business.settlement.derivation.dto.DerivationConfirmRequest;
import com.musheng.business.settlement.derivation.dto.DerivationRequest;
import com.musheng.business.settlement.derivation.dto.MskuConfirmItem;
import com.musheng.business.settlement.derivation.dto.SiteConfirmData;
import com.musheng.business.settlement.derivation.dto.SiteCostInput;
import com.musheng.business.settlement.derivation.service.SalesDataAggregator;
import com.musheng.business.settlement.derivation.service.SettlementDerivationService;
import com.musheng.business.settlement.derivation.vo.*;
import com.musheng.common.context.ShopContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 结算数据推导服务实现（按季度）
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class SettlementDerivationServiceImpl implements SettlementDerivationService {

    private static final Map<String, String> SITE_CURRENCY_MAP = Map.of(
            "US", "USD", "CA", "CAD", "UK", "GBP", "DE", "EUR"
    );

    @Autowired
    private SalesDataAggregator salesDataAggregator;
    @Autowired
    private ExchangeRateMapper exchangeRateMapper;
    @Autowired
    private SettlementImportDataMapper settlementImportDataMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DerivationResultVO derive(DerivationRequest request) {
        Long shopId = ShopContext.requireShopId();
        log.info("开始推导计算，店铺ID: {}, 季度: {} ~ {}", shopId, request.getStartQuarter(), request.getEndQuarter());

        LocalDate[] dateRange = parseQuarterRange(request.getStartQuarter(), request.getEndQuarter());
        LocalDate periodStart = dateRange[0];
        LocalDate periodEnd = dateRange[1];

        // 构建站点采购成本映射
        Map<String, BigDecimal> costMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(request.getSiteCosts())) {
            for (SiteCostInput input : request.getSiteCosts()) {
                costMap.put(input.getSiteCode(), input.getCostCny());
            }
        }

        // 汇总销售数据
        AggregationResult aggResult = salesDataAggregator.aggregateNetSales(shopId, periodStart, periodEnd);
        Map<String, Map<String, Integer>> netSalesMap = aggResult.getNetSalesMap();

        // 查询周期内各货币平均汇率
        List<String> currencies = costMap.keySet().stream()
                .map(SITE_CURRENCY_MAP::get).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<String, BigDecimal> averageRates = calcAverageRates(currencies, periodStart, periodEnd);

        // 按站点计算推导结果
        List<SiteDerivationResult> siteResults = new ArrayList<>();
        for (String siteCode : costMap.keySet()) {
            SiteDerivationResult sr = buildSiteResult(siteCode,
                    netSalesMap.getOrDefault(siteCode, Collections.emptyMap()), costMap, averageRates);
            if (sr != null) {
                siteResults.add(sr);
            }
        }

        // 删旧数据 + 写入新数据
        String batchId = UUID.randomUUID().toString();
        int deletedCount = 0;
        int insertedCount = 0;
        for (SiteDerivationResult sr : siteResults) {
            int deleted = settlementImportDataMapper.logicalDeleteByPeriodAndSite(periodStart, periodEnd, sr.getSiteCode());
            deletedCount += deleted;
            for (MskuDerivationItem item : sr.getItems()) {
                SettlementImportData entity = new SettlementImportData();
                entity.setShopId(shopId);
                entity.setPeriodStart(periodStart);
                entity.setPeriodEnd(periodEnd);
                entity.setSiteCode(sr.getSiteCode());
                entity.setCurrency(sr.getCurrency());
                entity.setMsku(item.getMsku());
                entity.setQuantity(item.getQuantity());
                entity.setUnitPrice(item.getUnitPrice());
                entity.setAmount(item.getAmount());
                entity.setProcurementCostCny(sr.getProcurementCostCny());
                entity.setAverageExchangeRate(sr.getAverageExchangeRate());
                entity.setImportBatchId(0L);
                entity.setSettlementBatchId(batchId);
                entity.setDelFlag(0);
                settlementImportDataMapper.insert(entity);
                insertedCount++;
            }
        }
        log.info("推导完成: 删除{}条旧数据, 写入{}条新数据, 批次={}", deletedCount, insertedCount, batchId);

        return DerivationResultVO.builder()
                .startQuarter(request.getStartQuarter()).endQuarter(request.getEndQuarter())
                .periodStart(periodStart).periodEnd(periodEnd)
                .siteResults(siteResults).averageRates(averageRates).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DerivationConfirmResultVO confirm(DerivationConfirmRequest request) {
        Long shopId = ShopContext.requireShopId();
        LocalDate[] dateRange = parseQuarterRange(request.getStartQuarter(), request.getEndQuarter());
        String batchId = UUID.randomUUID().toString();
        int deletedCount = 0;
        int recordCount = 0;
        for (SiteConfirmData siteData : request.getSiteDataList()) {
            if (request.isOverwrite()) {
                deletedCount += settlementImportDataMapper.logicalDeleteByPeriodAndSite(
                        dateRange[0], dateRange[1], siteData.getSiteCode());
            }
            for (MskuConfirmItem item : siteData.getItems()) {
                SettlementImportData entity = new SettlementImportData();
                entity.setShopId(shopId);
                entity.setPeriodStart(dateRange[0]);
                entity.setPeriodEnd(dateRange[1]);
                entity.setSiteCode(siteData.getSiteCode());
                entity.setCurrency(siteData.getCurrency());
                entity.setMsku(item.getMsku());
                entity.setQuantity(item.getQuantity());
                entity.setUnitPrice(item.getUnitPrice());
                entity.setAmount(item.getAmount());
                entity.setProcurementCostCny(siteData.getProcurementCostCny());
                entity.setAverageExchangeRate(siteData.getAverageExchangeRate());
                entity.setImportBatchId(0L);
                entity.setSettlementBatchId(batchId);
                entity.setDelFlag(0);
                settlementImportDataMapper.insert(entity);
                recordCount++;
            }
        }
        return DerivationConfirmResultVO.builder()
                .settlementBatchId(batchId).recordCount(recordCount).deletedCount(deletedCount).build();
    }

    @Override
    public boolean checkExistingData(String startQuarter, String endQuarter) {
        ShopContext.requireShopId();
        LocalDate[] dateRange = parseQuarterRange(startQuarter, endQuarter);
        for (String siteCode : SITE_CURRENCY_MAP.keySet()) {
            List<SettlementImportData> existing =
                    settlementImportDataMapper.selectByPeriodAndSite(dateRange[0], dateRange[1], siteCode);
            if (!CollectionUtils.isEmpty(existing)) {
                return true;
            }
        }
        return false;
    }

    private LocalDate[] parseQuarterRange(String startQuarter, String endQuarter) {
        LocalDate start = parseQuarterStart(startQuarter);
        LocalDate end = parseQuarterEnd(endQuarter);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始季度不能晚于结束季度: " + startQuarter + " > " + endQuarter);
        }
        return new LocalDate[]{start, end};
    }

    private LocalDate parseQuarterStart(String quarter) {
        int[] p = parseQuarterParts(quarter);
        return LocalDate.of(p[0], (p[1] - 1) * 3 + 1, 1);
    }

    private LocalDate parseQuarterEnd(String quarter) {
        int[] p = parseQuarterParts(quarter);
        return LocalDate.of(p[0], p[1] * 3, 1).plusMonths(1).minusDays(1);
    }

    private int[] parseQuarterParts(String quarter) {
        if (quarter == null || !quarter.matches("\\d{4}-Q[1-4]")) {
            throw new IllegalArgumentException("季度格式错误，应为 yyyy-Qn: " + quarter);
        }
        return new int[]{Integer.parseInt(quarter.substring(0, 4)), Integer.parseInt(quarter.substring(6))};
    }

    private Map<String, BigDecimal> calcAverageRates(List<String> currencies, LocalDate periodStart, LocalDate periodEnd) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String currency : currencies) {
            List<ExchangeRate> rates = exchangeRateMapper.selectList(
                    new LambdaQueryWrapper<ExchangeRate>()
                            .eq(ExchangeRate::getCurrencyCode, currency)
                            .ge(ExchangeRate::getRateDate, periodStart)
                            .le(ExchangeRate::getRateDate, periodEnd));
            if (CollectionUtils.isEmpty(rates)) {
                throw new IllegalArgumentException(
                        String.format("货币%s在周期%s~%s内无汇率数据", currency, periodStart, periodEnd));
            }
            BigDecimal sum = rates.stream().map(ExchangeRate::getRate).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(currency, sum.divide(BigDecimal.valueOf(rates.size()), 6, RoundingMode.HALF_UP));
        }
        return result;
    }

    /**
     * 产品系列成本权重映射表
     * <p>不同产品系列因材质、工艺、品质等差异，采购成本不同。
     * 权重值表示相对于基准成本的倍率，1.0 为基准。</p>
     */
    private static final Map<String, BigDecimal> SERIES_WEIGHT_MAP = Map.ofEntries(
            Map.entry("D70", new BigDecimal("0.75")),   // 走量基础款，成本最低
            Map.entry("D71", new BigDecimal("0.80")),   // 基础款升级
            Map.entry("D22", new BigDecimal("1.10")),   // 中端常规款
            Map.entry("D23", new BigDecimal("1.05")),   // 中端常规款
            Map.entry("D06", new BigDecimal("1.15")),   // 中高端款
            Map.entry("D25", new BigDecimal("1.20")),   // 中高端款
            Map.entry("D28", new BigDecimal("1.25")),   // 高端款
            Map.entry("D30", new BigDecimal("0.90")),   // 经济款
            Map.entry("D33", new BigDecimal("0.95")),   // 经济款
            Map.entry("D42", new BigDecimal("1.30")),   // 高端精品款
            Map.entry("D47", new BigDecimal("1.35")),   // 高端精品款
            Map.entry("D60", new BigDecimal("0.85")),   // 轻量款
            Map.entry("D63", new BigDecimal("1.40")),   // 旗舰款
            Map.entry("D72", new BigDecimal("1.00")),   // 标准款（基准）
            Map.entry("D73", new BigDecimal("1.10")),   // 标准升级款
            Map.entry("D74", new BigDecimal("1.45")),   // 旗舰高端款
            Map.entry("D75", new BigDecimal("1.50"))    // 顶级旗舰款
    );

    /**
     * 尺码成本系数映射表
     * <p>尺码越大，用料越多，成本越高。</p>
     */
    private static final Map<String, BigDecimal> SIZE_FACTOR_MAP = Map.of(
            "1", new BigDecimal("0.85"),
            "2", new BigDecimal("0.90"),
            "3", new BigDecimal("0.95"),
            "4", new BigDecimal("1.00"),
            "5", new BigDecimal("1.08"),
            "6", new BigDecimal("1.15"),
            "7", new BigDecimal("1.22")
    );

    /**
     * 从 SKU 编码中提取产品系列代码
     *
     * @param sku SKU编码，如 MS-D06-348-5 或 MSUK-D74-6-3
     * @return 产品系列代码，如 D06、D74
     * @author wanhua
     * 16:30 2026年03月07日
     */
    private String extractSeries(String sku) {
        if (sku == null) { return null; }
        // MS-D06-xxx 或 MSUK-D74-xxx，第二段是系列
        String[] parts = sku.split("-");
        if (parts.length >= 2) {
            String candidate = parts[1];
            if (candidate.matches("D\\d+")) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 从 SKU 编码中提取尺码标识
     *
     * @param sku SKU编码，如 MS-D06-348-5
     * @return 尺码标识，如 "5"
     * @author wanhua
     * 16:30 2026年03月07日
     */
    private String extractSize(String sku) {
        if (sku == null || sku.isEmpty()) { return "4"; }
        String lastChar = sku.substring(sku.length() - 1);
        if (lastChar.matches("\\d")) {
            return lastChar;
        }
        return "4"; // 默认中间尺码
    }

    /**
     * 计算单个 SKU 的成本权重（产品系列权重 × 尺码系数）
     *
     * @param sku SKU编码
     * @return 综合成本权重
     * @author wanhua
     * 16:30 2026年03月07日
     */
    private BigDecimal calcSkuWeight(String sku) {
        String series = extractSeries(sku);
        String size = extractSize(sku);
        BigDecimal seriesWeight = (series != null) ? SERIES_WEIGHT_MAP.getOrDefault(series, BigDecimal.ONE) : BigDecimal.ONE;
        BigDecimal sizeFactor = (size != null) ? SIZE_FACTOR_MAP.getOrDefault(size, BigDecimal.ONE) : BigDecimal.ONE;
        return seriesWeight.multiply(sizeFactor);
    }

    /**
     * 按站点构建推导结果（差异化定价）
     *
     * <p>定价逻辑：根据 SKU 的产品系列和尺码计算加权成本，
     * 保证所有 SKU 的加权总成本 = 用户输入的总采购成本（原币）。</p>
     *
     * @param siteCode 站点代码
     * @param mskuSalesMap SKU → 净销售数量
     * @param costMap 站点 → 采购成本(CNY)
     * @param averageRates 货币 → 平均汇率
     * @return 站点推导结果
     * @author wanhua
     * 16:30 2026年03月07日
     */
    private SiteDerivationResult buildSiteResult(String siteCode, Map<String, Integer> mskuSalesMap,
                                                  Map<String, BigDecimal> costMap, Map<String, BigDecimal> averageRates) {
        String currency = SITE_CURRENCY_MAP.get(siteCode);
        if (currency == null) { return null; }
        BigDecimal costCny = costMap.getOrDefault(siteCode, BigDecimal.ZERO);
        BigDecimal avgRate = averageRates.get(currency);
        if (avgRate == null || avgRate.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("站点{}对应货币{}无有效汇率，跳过", siteCode, currency);
            return null;
        }
        BigDecimal costOriginal = costCny.divide(avgRate, 2, RoundingMode.HALF_UP);
        int totalQuantity = mskuSalesMap.values().stream().mapToInt(Integer::intValue).sum();

        // 第一步：计算加权总量 = Σ(数量 × SKU权重)
        BigDecimal weightedTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> skuWeights = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : mskuSalesMap.entrySet()) {
            int qty = entry.getValue();
            if (qty <= 0) { continue; }
            BigDecimal weight = calcSkuWeight(entry.getKey());
            skuWeights.put(entry.getKey(), weight);
            weightedTotal = weightedTotal.add(weight.multiply(BigDecimal.valueOf(qty)));
        }

        // 第二步：基准单价 = 总成本(原币) / 加权总量
        BigDecimal baseUnitPrice = weightedTotal.compareTo(BigDecimal.ZERO) > 0
                ? costOriginal.divide(weightedTotal, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 第三步：每个 SKU 的单价 = 基准单价 × SKU权重
        List<MskuDerivationItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : skuWeights.entrySet()) {
            String sku = entry.getKey();
            BigDecimal weight = entry.getValue();
            int qty = mskuSalesMap.get(sku);
            BigDecimal unitPrice = baseUnitPrice.multiply(weight).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(amount);
            items.add(MskuDerivationItem.builder().msku(sku).quantity(qty)
                    .unitPrice(unitPrice).amount(amount).adjusted(false).build());
        }
        // 按 MSKU 字母升序排列
        items.sort(Comparator.comparing(MskuDerivationItem::getMsku));
        return SiteDerivationResult.builder().siteCode(siteCode).currency(currency)
                .procurementCostCny(costCny).averageExchangeRate(avgRate).procurementCostOriginal(costOriginal)
                .totalQuantity(totalQuantity).totalAmount(totalAmount).items(items).build();
    }
}
