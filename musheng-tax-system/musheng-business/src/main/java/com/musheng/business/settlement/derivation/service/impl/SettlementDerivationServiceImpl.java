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

    @Autowired
    private com.musheng.business.common.config.MarketplaceConfigService marketplaceConfigService;

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
        log.info("[Derive] 当前店铺: shopId={}, 季度: {} ~ {}", shopId, request.getStartQuarter(), request.getEndQuarter());

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
        List<String> siteCodes = new ArrayList<>(costMap.keySet());
        if (siteCodes.isEmpty()) {
            throw new IllegalArgumentException("推导请求必须包含至少一个站点的采购成本，shopId=" + shopId);
        }

        // === Step 1：季度汇总 → 计算单价（按站点+店筛选）===
        // 单价基于整季度总量计算，与月份无关
        AggregationResult quarterAgg = salesDataAggregator.aggregateNetSales(
                shopId, periodStart, periodEnd, siteCodes);
        Map<String, Map<String, Integer>> quarterNetSalesMap = quarterAgg.getNetSalesMap();

        // 查询整季度各货币平均汇率
        Map<String, String> siteCurrencyMap = marketplaceConfigService.buildSiteCurrencyMap();
        List<String> currencies = costMap.keySet().stream()
                .map(siteCurrencyMap::get).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<String, BigDecimal> averageRates = calcAverageRates(currencies, periodStart, periodEnd);

        // 按站点计算推导结果（单价，基于季度总量）
        List<SiteDerivationResult> siteResults = new ArrayList<>();
        Map<String, SiteDerivationResult> siteResultMap = new LinkedHashMap<>();
        for (String siteCode : costMap.keySet()) {
            SiteDerivationResult sr = buildSiteResult(siteCode,
                    quarterNetSalesMap.getOrDefault(siteCode, Collections.emptyMap()), costMap, averageRates);
            if (sr != null) {
                siteResults.add(sr);
                siteResultMap.put(siteCode, sr);
            }
        }

        // === Step 2：删除该季度内所有旧数据（含已有月份分拆记录）===
        String batchId = UUID.randomUUID().toString();
        int deletedCount = 0;
        for (String siteCode : siteResultMap.keySet()) {
            deletedCount += settlementImportDataMapper.logicalDeleteByPeriodRangeAndSite(
                    shopId, periodStart, periodEnd, siteCode);
        }

        // === Step 3：按自然月循环，分月汇总数量并存储（按站点+店筛选）===
        // 负数量跨月结转，跨季度清零
        int insertedCount = 0;
        // carryoverMap: 站点 -> (MSKU -> 结转到下月的负数量)
        Map<String, Map<String, Integer>> carryoverMap = new HashMap<>();

        LocalDate monthStart = periodStart.withDayOfMonth(1);
        while (!monthStart.isAfter(periodEnd)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            LocalDate subStart = monthStart.isBefore(periodStart) ? periodStart : monthStart;
            LocalDate subEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;

            // 聚合本月销售数量（按站点+店过滤）
            AggregationResult monthAgg = salesDataAggregator.aggregateNetSales(
                    shopId, subStart, subEnd, siteCodes);

            for (Map.Entry<String, SiteDerivationResult> entry : siteResultMap.entrySet()) {
                String siteCode = entry.getKey();
                SiteDerivationResult sr = entry.getValue();

                // 本月净销售量（可变副本，用于叠加结转）
                Map<String, Integer> monthQty = new HashMap<>(
                        monthAgg.getNetSalesMap().getOrDefault(siteCode, Collections.emptyMap()));

                // 叠加上月结转的负数量
                Map<String, Integer> prevCarryover = carryoverMap.getOrDefault(siteCode, Collections.emptyMap());
                for (Map.Entry<String, Integer> co : prevCarryover.entrySet()) {
                    monthQty.merge(co.getKey(), co.getValue(), Integer::sum);
                }

                // 计算本月结转到下月的负数量（跨季度在循环结束后自动丢弃）
                Map<String, Integer> nextCarryover = new HashMap<>();
                for (Map.Entry<String, Integer> qe : monthQty.entrySet()) {
                    if (qe.getValue() < 0) {
                        nextCarryover.put(qe.getKey(), qe.getValue());
                    }
                }
                carryoverMap.put(siteCode, nextCarryover);

                // 构建 MSKU → 单价 映射（来自季度推导结果）
                Map<String, BigDecimal> unitPriceMap = sr.getItems().stream()
                        .collect(Collectors.toMap(MskuDerivationItem::getMsku, MskuDerivationItem::getUnitPrice));

                // 写入本月有效数量的记录（数量 > 0）
                for (Map.Entry<String, Integer> qe : monthQty.entrySet()) {
                    String msku = qe.getKey();
                    int qty = qe.getValue();
                    if (qty <= 0) continue;

                    BigDecimal unitPrice = unitPriceMap.getOrDefault(msku, BigDecimal.ZERO);
                    BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty))
                            .setScale(2, RoundingMode.HALF_UP);

                    SettlementImportData entity = new SettlementImportData();
                    entity.setShopId(shopId);
                    entity.setPeriodStart(subStart);   // 月份起始，供 SettlementGenerator 按月过滤
                    entity.setPeriodEnd(subEnd);         // 月份结束
                    entity.setSiteCode(siteCode);
                    entity.setCurrency(sr.getCurrency());
                    entity.setMsku(msku);
                    entity.setQuantity(qty);
                    entity.setUnitPrice(unitPrice);
                    entity.setAmount(amount);
                    entity.setProcurementCostCny(sr.getProcurementCostCny());
                    entity.setAverageExchangeRate(sr.getAverageExchangeRate());
                    entity.setImportBatchId(0L);
                    entity.setSettlementBatchId(batchId);
                    entity.setDelFlag(0);
                    settlementImportDataMapper.insert(entity);
                    insertedCount++;
                }
            }

            monthStart = monthStart.plusMonths(1);
        }

        log.info("[Derive] shopId={} 推导完成: 删除{}条旧数据, 按月写入{}条新数据, 批次={}",
                shopId, deletedCount, insertedCount, batchId);

        return DerivationResultVO.builder()
                .startQuarter(request.getStartQuarter()).endQuarter(request.getEndQuarter())
                .periodStart(periodStart).periodEnd(periodEnd)
                .siteResults(siteResults).averageRates(averageRates).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DerivationConfirmResultVO confirm(DerivationConfirmRequest request) {
        Long shopId = ShopContext.requireShopId();
        log.info("[Confirm] 当前店铺: shopId={}, 季度: {} ~ {}, overwrite: {}, 站点数: {}",
                shopId, request.getStartQuarter(), request.getEndQuarter(),
                request.isOverwrite(), request.getSiteDataList().size());

        LocalDate[] dateRange = parseQuarterRange(request.getStartQuarter(), request.getEndQuarter());
        LocalDate periodStart = dateRange[0];
        LocalDate periodEnd = dateRange[1];

        // 收集本次确认涉及的站点，用于按站点过滤销售数据
        List<String> siteCodes = request.getSiteDataList().stream()
                .map(SiteConfirmData::getSiteCode)
                .distinct()
                .collect(Collectors.toList());

        // 无站点数据直接返回
        if (siteCodes.isEmpty()) {
            return DerivationConfirmResultVO.builder()
                    .settlementBatchId(UUID.randomUUID().toString()).recordCount(0).deletedCount(0).build();
        }

        // 查询季度内各月的原始净销售量，作为按比例拆分的依据
        // 结构：月份起始日 -> (站点 -> (MSKU -> 原始净数量))
        List<LocalDate> monthStarts = new ArrayList<>();
        Map<LocalDate, Map<String, Map<String, Integer>>> monthlyRawMap = new LinkedHashMap<>();
        LocalDate ms = periodStart.withDayOfMonth(1);
        while (!ms.isAfter(periodEnd)) {
            LocalDate me = ms.withDayOfMonth(ms.lengthOfMonth());
            LocalDate subStart = ms.isBefore(periodStart) ? periodStart : ms;
            LocalDate subEnd = me.isAfter(periodEnd) ? periodEnd : me;
            AggregationResult monthRaw = salesDataAggregator.aggregateNetSales(shopId, subStart, subEnd, siteCodes);
            monthStarts.add(subStart);
            monthlyRawMap.put(subStart, monthRaw.getNetSalesMap());
            ms = ms.plusMonths(1);
        }

        // 删除旧数据（覆盖模式下清除季度范围内所有月份记录）
        String batchId = UUID.randomUUID().toString();
        int deletedCount = 0;
        int recordCount = 0;
        if (request.isOverwrite()) {
            for (SiteConfirmData siteData : request.getSiteDataList()) {
                deletedCount += settlementImportDataMapper.logicalDeleteByPeriodRangeAndSite(
                        shopId, periodStart, periodEnd, siteData.getSiteCode());
            }
        }

        // 按站点、按 MSKU 将季度确认总量拆分到各月，写入月份记录
        for (SiteConfirmData siteData : request.getSiteDataList()) {
            String siteCode = siteData.getSiteCode();

            for (MskuConfirmItem item : siteData.getItems()) {
                String msku = item.getMsku();
                int quarterlyQty = item.getQuantity();
                if (quarterlyQty <= 0) continue;

                // 取各月该站点该 MSKU 的原始净数量（只保留正值，负值月份不参与比例分配）
                Map<LocalDate, Integer> monthRawQty = new LinkedHashMap<>();
                for (LocalDate subStart : monthStarts) {
                    int raw = monthlyRawMap.get(subStart)
                            .getOrDefault(siteCode, Collections.emptyMap())
                            .getOrDefault(msku, 0);
                    if (raw > 0) {
                        monthRawQty.put(subStart, raw);
                    }
                }
                int rawTotal = monthRawQty.values().stream().mapToInt(Integer::intValue).sum();

                // 按月计算分配量，并处理整除余数（最后一个有效月承接余数）
                Map<LocalDate, Integer> monthAllocated = new LinkedHashMap<>();
                if (rawTotal > 0) {
                    // 按原始销售比例分配
                    int allocated = 0;
                    List<LocalDate> validMonths = new ArrayList<>(monthRawQty.keySet());
                    for (int i = 0; i < validMonths.size() - 1; i++) {
                        LocalDate subStart = validMonths.get(i);
                        int portion = (int) Math.round((double) quarterlyQty * monthRawQty.get(subStart) / rawTotal);
                        monthAllocated.put(subStart, portion);
                        allocated += portion;
                    }
                    // 最后一个月取余数，确保总量精确
                    LocalDate lastMonth = validMonths.get(validMonths.size() - 1);
                    monthAllocated.put(lastMonth, quarterlyQty - allocated);
                } else {
                    // 无原始销售数据时按月均分，最后一个月承接余数
                    int base = quarterlyQty / monthStarts.size();
                    int remainder = quarterlyQty % monthStarts.size();
                    for (int i = 0; i < monthStarts.size(); i++) {
                        int portion = base + (i == monthStarts.size() - 1 ? remainder : 0);
                        if (portion > 0) {
                            monthAllocated.put(monthStarts.get(i), portion);
                        }
                    }
                }

                // 写入各月记录
                for (Map.Entry<LocalDate, Integer> alloc : monthAllocated.entrySet()) {
                    LocalDate subStart = alloc.getKey();
                    int monthQty = alloc.getValue();
                    if (monthQty <= 0) continue;

                    // 月份结束日：取该月最后一天，与季度结束日取小
                    LocalDate monthEnd = subStart.withDayOfMonth(subStart.lengthOfMonth());
                    LocalDate subEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;

                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(monthQty))
                            .setScale(2, RoundingMode.HALF_UP);

                    SettlementImportData entity = new SettlementImportData();
                    entity.setShopId(shopId);
                    entity.setPeriodStart(subStart);   // 月份起始，供 SettlementGenerator 按月过滤
                    entity.setPeriodEnd(subEnd);
                    entity.setSiteCode(siteCode);
                    entity.setCurrency(siteData.getCurrency());
                    entity.setMsku(msku);
                    entity.setQuantity(monthQty);
                    entity.setUnitPrice(unitPrice);
                    entity.setAmount(amount);
                    entity.setProcurementCostCny(siteData.getProcurementCostCny());
                    entity.setAverageExchangeRate(siteData.getAverageExchangeRate());
                    entity.setImportBatchId(0L);
                    entity.setSettlementBatchId(batchId);
                    entity.setDelFlag(0);
                    settlementImportDataMapper.insert(entity);
                    recordCount++;
                }
            }
        }

        log.info("[Confirm] shopId={} 确认完成（按月拆分）: batchId={}, 写入{}条, 删除{}条旧数据",
                shopId, batchId, recordCount, deletedCount);
        return DerivationConfirmResultVO.builder()
                .settlementBatchId(batchId).recordCount(recordCount).deletedCount(deletedCount).build();
    }

    @Override
    public boolean checkExistingData(String startQuarter, String endQuarter) {
        Long shopId = ShopContext.requireShopId();
        log.info("[CheckExisting] 当前店铺: shopId={}, 季度: {} ~ {}", shopId, startQuarter, endQuarter);
        LocalDate[] dateRange = parseQuarterRange(startQuarter, endQuarter);
        // 使用范围查询：匹配季度内所有月份分拆的记录
        LambdaQueryWrapper<SettlementImportData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementImportData::getShopId, shopId)
                .ge(SettlementImportData::getPeriodStart, dateRange[0])
                .le(SettlementImportData::getPeriodStart, dateRange[1])
                .eq(SettlementImportData::getDelFlag, 0);
        return settlementImportDataMapper.selectCount(wrapper) > 0;
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
                log.warn("货币{}在周期{}~{}内无汇率数据", currency, periodStart, periodEnd);
                continue;
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
        // 修复：ImmutableCollections（Map.ofEntries 创建）不支持 null key，需提前判断
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
        String currency = marketplaceConfigService.buildSiteCurrencyMap().get(siteCode);
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
        return SiteDerivationResult.builder().siteCode(siteCode).currency(currency)
                .procurementCostCny(costCny).averageExchangeRate(avgRate).procurementCostOriginal(costOriginal)
                .totalQuantity(totalQuantity).totalAmount(totalAmount).items(items).build();
    }
}
