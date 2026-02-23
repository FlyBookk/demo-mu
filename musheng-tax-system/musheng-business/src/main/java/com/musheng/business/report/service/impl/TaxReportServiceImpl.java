package com.musheng.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.advertising.entity.AdvertisingData;
import com.musheng.business.advertising.mapper.AdvertisingDataMapper;
import com.musheng.business.report.dto.DashboardData;
import com.musheng.business.report.dto.FeeBreakdown;
import com.musheng.business.report.dto.TaxReportSummary;
import com.musheng.business.report.service.TaxReportService;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.common.context.ShopContext;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报税汇总服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxReportServiceImpl implements TaxReportService {

    private final SalesDataMapper salesDataMapper;
    private final ShippingDataMapper shippingDataMapper;
    private final AdvertisingDataMapper advertisingDataMapper;
    private final MarketplaceMapper marketplaceMapper;

    /**
     * 站点到货币的映射
     */
    private static final Map<String, String> SITE_CURRENCY_MAP = Map.of(
            "US", "USD",
            "CA", "CAD",
            "UK", "GBP",
            "DE", "EUR"
    );

    @Override
    public DashboardData getDashboardData(String quarterParam) {
        long startTime = System.currentTimeMillis();
        log.info("Getting dashboard data (lightweight), quarter={}", quarterParam);

        Long shopId = ShopContext.requireShopId();
        List<String> sites = List.of("US", "CA", "UK", "DE");

        // 确定当前季度：传入参数优先，否则取系统当前季度
        LocalDate now = LocalDate.now();
        int currentQ = (now.getMonthValue() - 1) / 3 + 1;
        String currentQuarter;
        if (quarterParam != null && !quarterParam.isBlank() && quarterParam.matches("\\d{4}-Q[1-4]")) {
            currentQuarter = quarterParam.trim();
        } else {
            currentQuarter = now.getYear() + "-Q" + currentQ;
        }

        String oldestQuarter = currentQuarter;
        for (int i = 0; i < 3; i++) {
            oldestQuarter = getPreviousQuarter(oldestQuarter);
        }
        String previousQuarter = getPreviousQuarter(currentQuarter);

        // 计算日期范围（基于选中的季度）
        int currentYear = Integer.parseInt(currentQuarter.substring(0, 4));
        int currentQNum = Integer.parseInt(currentQuarter.substring(6, 7));
        int oldYear = Integer.parseInt(oldestQuarter.substring(0, 4));
        int oldQ = Integer.parseInt(oldestQuarter.substring(6, 7));
        LocalDate minStartDate = getQuarterStartDate(oldYear, oldQ);
        LocalDate maxEndDate = getQuarterEndDate(currentYear, currentQNum);

        // ========== 轻量级查询：只查发货数据和退款数据 ==========
        
        // 1. 站点信息
        Map<String, Marketplace> marketplaceMap = loadMarketplaceMap(sites);

        // 2. 发货数据（收入来源）- 使用聚合查询
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getShopId, shopId)
                .in(ShippingData::getSiteCode, sites)
                .between(ShippingData::getShipDate, minStartDate, maxEndDate);
        List<ShippingData> shippingData = shippingDataMapper.selectList(shippingWrapper);

        // 3. 退款数据（只需要total和日期）
        LambdaQueryWrapper<SalesData> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, "refund")
                .ge(SalesData::getTransactionDate, minStartDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, maxEndDate.plusMonths(2).atStartOfDay());
        List<SalesData> refundData = salesDataMapper.selectList(refundWrapper);

        log.debug("Dashboard data loaded in {}ms: shipping={}, refunds={}", 
                System.currentTimeMillis() - startTime, shippingData.size(), refundData.size());

        // ========== 构建订单→发货日期映射 ==========
        Map<String, LocalDate> orderShipDateMap = new HashMap<>();
        for (ShippingData s : shippingData) {
            if (s.getOrderId() != null && s.getShipDate() != null) {
                orderShipDateMap.merge(s.getOrderId(), s.getShipDate(), (a, b) -> b.isAfter(a) ? b : a);
            }
        }

        // ========== 按站点+季度汇总 ==========
        // 数据结构: siteCode -> quarter -> {revenue, refund, orders}
        Map<String, Map<String, BigDecimal>> revenueMap = new HashMap<>();
        Map<String, Map<String, BigDecimal>> refundMap = new HashMap<>();
        Map<String, Map<String, Integer>> orderCountMap = new HashMap<>();

        // 汇总发货收入
        for (ShippingData s : shippingData) {
            if (s.getShipDate() == null) continue;
            String quarter = getQuarterFromDate(s.getShipDate());
            String site = s.getSiteCode();
            
            BigDecimal revenue = s.getRevenueTotal();
            if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
                revenue = calculateShippingRevenue(s);
            }
            BigDecimal rate = s.getExchangeRate();
            BigDecimal revenueCny = (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) 
                    ? revenue.multiply(rate) : revenue;

            revenueMap.computeIfAbsent(site, k -> new HashMap<>())
                    .merge(quarter, revenueCny, BigDecimal::add);
            
            if (s.getOrderId() != null) {
                orderCountMap.computeIfAbsent(site, k -> new HashMap<>())
                        .merge(quarter, 1, Integer::sum);
            }
        }

        // 汇总退款（按发货归属）
        for (SalesData r : refundData) {
            String orderId = r.getOrderId();
            LocalDate shipDate = orderShipDateMap.get(orderId);
            if (shipDate == null) continue;
            
            String quarter = getQuarterFromDate(shipDate);
            String site = r.getSiteCode();
            
            BigDecimal refundAmt = r.getTotal() != null ? r.getTotal().abs() : BigDecimal.ZERO;
            BigDecimal rate = r.getExchangeRate();
            BigDecimal refundCny = (rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                    ? refundAmt.multiply(rate) : refundAmt;

            refundMap.computeIfAbsent(site, k -> new HashMap<>())
                    .merge(quarter, refundCny, BigDecimal::add);
        }

        // ========== 构建结果 ==========
        DashboardData dashboard = new DashboardData();
        dashboard.setCurrentQuarter(currentQuarter);

        // 当前季度汇总
        BigDecimal totalRevenue = sumFromMap(revenueMap, currentQuarter);
        BigDecimal totalRefund = sumFromMap(refundMap, currentQuarter);
        int totalOrders = sumOrdersFromMap(orderCountMap, currentQuarter);

        // 上季度汇总
        BigDecimal prevRevenue = sumFromMap(revenueMap, previousQuarter);
        BigDecimal prevRefund = sumFromMap(refundMap, previousQuarter);

        dashboard.setTotalRevenueCny(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        dashboard.setRefundCny(totalRefund.setScale(2, RoundingMode.HALF_UP));
        dashboard.setNetIncomeCny(totalRevenue.subtract(totalRefund).setScale(2, RoundingMode.HALF_UP));
        dashboard.setShippingOrderCount(totalOrders);

        // 环比增长率
        dashboard.setRevenueGrowthRate(calculateGrowthRate(totalRevenue, prevRevenue));
        dashboard.setRefundGrowthRate(calculateGrowthRate(totalRefund, prevRefund));
        dashboard.setNetIncomeGrowthRate(calculateGrowthRate(
                totalRevenue.subtract(totalRefund), 
                prevRevenue.subtract(prevRefund)));

        // 各站点数据（当前季度）
        List<DashboardData.SiteRevenue> siteRevenues = new ArrayList<>();
        for (String site : sites) {
            BigDecimal siteRev = getFromMap(revenueMap, site, currentQuarter);
            BigDecimal siteRef = getFromMap(refundMap, site, currentQuarter);
            
            DashboardData.SiteRevenue sr = new DashboardData.SiteRevenue();
            sr.setSiteCode(site);
            Marketplace mp = marketplaceMap.get(site);
            sr.setSiteName(mp != null ? mp.getSiteName() : site);
            sr.setRevenue(siteRev.setScale(2, RoundingMode.HALF_UP));
            sr.setRefund(siteRef.setScale(2, RoundingMode.HALF_UP));
            sr.setNetIncome(siteRev.subtract(siteRef).setScale(2, RoundingMode.HALF_UP));
            siteRevenues.add(sr);
        }
        siteRevenues.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));
        dashboard.setSiteRevenues(siteRevenues);

        // 季度趋势
        List<DashboardData.QuarterTrend> trends = new ArrayList<>();
        String q = oldestQuarter;
        for (int i = 0; i < 4; i++) {
            DashboardData.QuarterTrend trend = new DashboardData.QuarterTrend();
            trend.setQuarter(q);
            BigDecimal qRev = sumFromMap(revenueMap, q);
            BigDecimal qRef = sumFromMap(refundMap, q);
            trend.setRevenue(qRev.setScale(2, RoundingMode.HALF_UP));
            trend.setRefund(qRef.setScale(2, RoundingMode.HALF_UP));
            trend.setNetIncome(qRev.subtract(qRef).setScale(2, RoundingMode.HALF_UP));
            trends.add(trend);
            q = getNextQuarter(q);
        }
        dashboard.setQuarterTrends(trends);

        log.info("Dashboard data completed in {}ms", System.currentTimeMillis() - startTime);
        return dashboard;
    }

    /**
     * 从日期获取季度字符串
     */
    private String getQuarterFromDate(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return date.getYear() + "-Q" + quarter;
    }

    /**
     * 从Map汇总所有站点的季度数据
     */
    private BigDecimal sumFromMap(Map<String, Map<String, BigDecimal>> map, String quarter) {
        return map.values().stream()
                .map(m -> m.getOrDefault(quarter, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 从Map获取特定站点季度数据
     */
    private BigDecimal getFromMap(Map<String, Map<String, BigDecimal>> map, String site, String quarter) {
        return map.getOrDefault(site, Collections.emptyMap()).getOrDefault(quarter, BigDecimal.ZERO);
    }

    /**
     * 从Map汇总订单数
     */
    private int sumOrdersFromMap(Map<String, Map<String, Integer>> map, String quarter) {
        return map.values().stream()
                .mapToInt(m -> m.getOrDefault(quarter, 0))
                .sum();
    }

    /**
     * 汇总字段
     */
    /**
     * 获取下一个季度
     */
    private String getNextQuarter(String quarter) {
        int year = Integer.parseInt(quarter.substring(0, 4));
        int q = Integer.parseInt(quarter.substring(6, 7));
        q++;
        if (q > 4) {
            q = 1;
            year++;
        }
        return year + "-Q" + q;
    }

    /**
     * 获取上一个季度
     */
    private String getPreviousQuarter(String quarter) {
        int year = Integer.parseInt(quarter.substring(0, 4));
        int q = Integer.parseInt(quarter.substring(6, 7));
        q--;
        if (q == 0) {
            q = 4;
            year--;
        }
        return year + "-Q" + q;
    }

    /**
     * 计算环比增长率
     */
    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<TaxReportSummary> getTaxSummary(String siteCode, String startQuarter, String endQuarter) {
        log.info("Getting tax summary: siteCode={}, startQuarter={}, endQuarter={}", siteCode, startQuarter, endQuarter);

        Long shopId = ShopContext.requireShopId();
        log.debug("Using shopId={} for data isolation", shopId);

        List<String> sites = StringUtils.hasText(siteCode)
                ? List.of(siteCode)
                : List.of("US", "CA", "UK", "DE");

        List<String> quarters = getQuartersInRange(startQuarter, endQuarter);

        // 计算整体日期范围（用于批量查询）
        LocalDate tempMinStart = null, tempMaxEnd = null;
        for (String q : quarters) {
            int year = Integer.parseInt(q.substring(0, 4));
            int quarter = Integer.parseInt(q.substring(6, 7));
            LocalDate start = getQuarterStartDate(year, quarter);
            LocalDate end = getQuarterEndDate(year, quarter);
            if (tempMinStart == null || start.isBefore(tempMinStart)) tempMinStart = start;
            if (tempMaxEnd == null || end.isAfter(tempMaxEnd)) tempMaxEnd = end;
        }
        final LocalDate minStartDate = tempMinStart;
        final LocalDate maxEndDate = tempMaxEnd;

        if (minStartDate == null || maxEndDate == null) {
            log.warn("No valid date range found for quarters: {}", quarters);
            return Collections.emptyList();
        }

        // ========== 优化后的批量加载（减少数据库查询次数） ==========
        long startTime = System.currentTimeMillis();

        // 1. 加载站点基础数据（小表，快速）
        Map<String, Marketplace> marketplaceMap = loadMarketplaceMap(sites);

        // 2. 批量查询发货数据（按日期范围）
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getShopId, shopId)
                .in(ShippingData::getSiteCode, sites)
                .between(ShippingData::getShipDate, minStartDate, maxEndDate);
        List<ShippingData> allShippingData = shippingDataMapper.selectList(shippingWrapper);

        // 3. 构建订单号→发货日期/汇率映射（从已查询的发货数据中提取，避免重复查询）
        Map<String, Map<String, LocalDate>> refundShipDateMap = new HashMap<>();
        Map<String, Map<String, BigDecimal>> orderRateMapBySite = new HashMap<>();
        for (ShippingData shipping : allShippingData) {
            String site = shipping.getSiteCode();
            String orderId = shipping.getOrderId();
            if (orderId == null) continue;

            refundShipDateMap.computeIfAbsent(site, k -> new HashMap<>())
                    .merge(orderId, shipping.getShipDate(), (a, b) -> b.isAfter(a) ? b : a);
            if (shipping.getExchangeRate() != null) {
                orderRateMapBySite.computeIfAbsent(site, k -> new HashMap<>())
                        .put(orderId, shipping.getExchangeRate());
            }
        }

        // 4. 扩展日期范围查询销售数据（退款可能在发货后2个月内结算）
        LocalDate extendedEndDate = maxEndDate.plusMonths(2);
        
        // 5. 批量查询销售数据（income/refund类型，添加日期过滤）
        LambdaQueryWrapper<SalesData> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .in(SalesData::getTransactionCategory, List.of("income", "refund"))
                .ge(SalesData::getTransactionDate, minStartDate.minusMonths(1).atStartOfDay())
                .lt(SalesData::getTransactionDate, extendedEndDate.plusDays(1).atStartOfDay());
        List<SalesData> allSalesData = salesDataMapper.selectList(salesWrapper);

        // 6. 批量查询其他费用数据（非income/refund类型，按日期过滤）
        LambdaQueryWrapper<SalesData> otherWrapper = new LambdaQueryWrapper<>();
        otherWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .notIn(SalesData::getTransactionCategory, List.of("income", "refund"))
                .ge(SalesData::getTransactionDate, minStartDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, maxEndDate.plusDays(1).atStartOfDay());
        List<SalesData> allOtherData = salesDataMapper.selectList(otherWrapper);

        // 7. 批量查询广告数据
        LambdaQueryWrapper<AdvertisingData> adWrapper = new LambdaQueryWrapper<>();
        adWrapper.eq(AdvertisingData::getShopId, shopId)
                .in(AdvertisingData::getSiteCode, sites)
                .and(w -> w.between(AdvertisingData::getBillingStartDate, minStartDate, maxEndDate)
                        .or()
                        .between(AdvertisingData::getBillingEndDate, minStartDate, maxEndDate)
                        .or()
                        .and(w2 -> w2.le(AdvertisingData::getBillingStartDate, minStartDate)
                                .ge(AdvertisingData::getBillingEndDate, maxEndDate)));
        List<AdvertisingData> allAdData = advertisingDataMapper.selectList(adWrapper);

        log.info("Data loaded in {}ms: shipping={}, sales={}, other={}, ads={}",
                System.currentTimeMillis() - startTime,
                allShippingData.size(), allSalesData.size(), allOtherData.size(), allAdData.size());

        // ========== 按站点分组（内存操作，快速） ==========
        Map<String, List<ShippingData>> shippingBySite = allShippingData.stream()
                .collect(Collectors.groupingBy(ShippingData::getSiteCode));
        Map<String, List<SalesData>> salesBySite = allSalesData.stream()
                .collect(Collectors.groupingBy(SalesData::getSiteCode));
        Map<String, List<SalesData>> otherBySite = allOtherData.stream()
                .collect(Collectors.groupingBy(SalesData::getSiteCode));
        Map<String, List<AdvertisingData>> adBySite = allAdData.stream()
                .collect(Collectors.groupingBy(AdvertisingData::getSiteCode));

        // ========== 在内存中计算每个站点+季度的汇总 ==========
        List<TaxReportSummary> results = new ArrayList<>();

        for (String site : sites) {
            List<ShippingData> siteShipping = shippingBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteSales = salesBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteOther = otherBySite.getOrDefault(site, Collections.emptyList());
            List<AdvertisingData> siteAds = adBySite.getOrDefault(site, Collections.emptyList());
            Map<String, LocalDate> siteRefundShipDates = refundShipDateMap.getOrDefault(site, Collections.emptyMap());
            Map<String, BigDecimal> siteOrderRates = orderRateMapBySite.getOrDefault(site, Collections.emptyMap());

            for (String quarterStr : quarters) {
                TaxReportSummary summary = calculateTaxSummaryFromMemory(
                        site, quarterStr, marketplaceMap.get(site),
                        siteShipping, siteSales, siteOther, siteAds, 
                        siteRefundShipDates, siteOrderRates
                );
                if (summary != null) {
                    results.add(summary);
                }
            }
        }

        log.info("Tax summary calculation completed in {}ms", System.currentTimeMillis() - startTime);
        return results;
    }

    /**
     * 从内存数据计算单个站点+季度的报税汇总（无数据库查询）
     * V2版本：按新的费用计算逻辑
     * - 收入/退款：配送订单号关联销售数据，使用配送汇率
     * - 消费税：平台代扣税（仅income/refund类型）
     * - 佣金/服务费：拆分明细（仅income/refund类型）
     * - 其他费：非income/refund类型的total汇总
     */
    private TaxReportSummary calculateTaxSummaryFromMemory(
            String siteCode,
            String yearQuarter,
            Marketplace marketplace,
            List<ShippingData> allShipping,
            List<SalesData> allSalesData,
            List<SalesData> allOtherData,
            List<AdvertisingData> allAds,
            Map<String, LocalDate> refundShipDateMap,
            Map<String, BigDecimal> orderRateMap) {

        // 解析季度日期范围
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        String siteName = marketplace != null ? marketplace.getSiteName() : siteCode;
        String currencyCode = SITE_CURRENCY_MAP.getOrDefault(siteCode, "USD");

        // ========== 1. 筛选本季度发货数据，收集订单号 ==========
        Set<String> shippingOrderIds = new HashSet<>();
        for (ShippingData shipping : allShipping) {
            if (shipping.getShipDate() != null 
                    && !shipping.getShipDate().isBefore(startDate)
                    && !shipping.getShipDate().isAfter(endDate)
                    && shipping.getOrderId() != null) {
                shippingOrderIds.add(shipping.getOrderId());
            }
        }

        // ========== 2. 筛选本季度发货订单关联的销售数据（income/refund类型） ==========
        // 收入数据：transaction_category = 'income' 且订单号在本季度发货列表中
        List<SalesData> incomeList = allSalesData.stream()
                .filter(s -> "income".equals(s.getTransactionCategory()))
                .filter(s -> s.getOrderId() != null && shippingOrderIds.contains(s.getOrderId()))
                .collect(Collectors.toList());

        // 退款数据：transaction_category = 'refund'
        List<SalesData> refundList = allSalesData.stream()
                .filter(s -> "refund".equals(s.getTransactionCategory()))
                .collect(Collectors.toList());

        // ========== 3. 收入计算（按新公式） ==========
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenueCny = BigDecimal.ZERO;

        // 收入相关费用（仅income类型）
        BigDecimal incomeConsumptionTax = BigDecimal.ZERO;
        BigDecimal incomeConsumptionTaxCny = BigDecimal.ZERO;
        BigDecimal incomeSellingFees = BigDecimal.ZERO;
        BigDecimal incomeSellingFeesCny = BigDecimal.ZERO;
        BigDecimal incomeFbaFees = BigDecimal.ZERO;
        BigDecimal incomeFbaFeesCny = BigDecimal.ZERO;
        BigDecimal incomeOtherTransFees = BigDecimal.ZERO;
        BigDecimal incomeOtherTransFeesCny = BigDecimal.ZERO;
        BigDecimal incomeOther = BigDecimal.ZERO;
        BigDecimal incomeOtherCny = BigDecimal.ZERO;

        for (SalesData income : incomeList) {
            // 使用配送汇率
            BigDecimal rate = orderRateMap.getOrDefault(income.getOrderId(), income.getExchangeRate());
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                rate = BigDecimal.ONE;
            }

            // 收入总额 = 产品销售+产品税+运费+运费税+礼品包装费+礼品包装税+监管费+监管费税+促销折扣+促销折扣税
            BigDecimal revenue = calculateSalesRevenue(income);
            totalRevenue = totalRevenue.add(revenue);
            totalRevenueCny = totalRevenueCny.add(revenue.multiply(rate));

            // 消费税（平台代扣税）
            BigDecimal tax = nullToZero(income.getMarketplaceWithheldTax()).abs();
            incomeConsumptionTax = incomeConsumptionTax.add(tax);
            incomeConsumptionTaxCny = incomeConsumptionTaxCny.add(tax.multiply(rate));

            // 销售费用
            BigDecimal selling = nullToZero(income.getSellingFees()).abs();
            incomeSellingFees = incomeSellingFees.add(selling);
            incomeSellingFeesCny = incomeSellingFeesCny.add(selling.multiply(rate));

            // FBA费用
            BigDecimal fba = nullToZero(income.getFbaFees()).abs();
            incomeFbaFees = incomeFbaFees.add(fba);
            incomeFbaFeesCny = incomeFbaFeesCny.add(fba.multiply(rate));

            // 其他交易费
            BigDecimal otherTrans = nullToZero(income.getOtherTransactionFees()).abs();
            incomeOtherTransFees = incomeOtherTransFees.add(otherTrans);
            incomeOtherTransFeesCny = incomeOtherTransFeesCny.add(otherTrans.multiply(rate));

            // 其他
            BigDecimal other = nullToZero(income.getOther()).abs();
            incomeOther = incomeOther.add(other);
            incomeOtherCny = incomeOtherCny.add(other.multiply(rate));
        }

        // ========== 4. 退款计算（双维度） ==========
        BigDecimal refundBySettlement = BigDecimal.ZERO;
        BigDecimal refundBySettlementCny = BigDecimal.ZERO;
        int refundCountBySettlement = 0;

        BigDecimal refundByShipment = BigDecimal.ZERO;
        BigDecimal refundByShipmentCny = BigDecimal.ZERO;
        int refundCountByShipment = 0;

        // 退款相关费用
        BigDecimal refundConsumptionTax = BigDecimal.ZERO;
        BigDecimal refundConsumptionTaxCny = BigDecimal.ZERO;
        BigDecimal refundSellingFees = BigDecimal.ZERO;
        BigDecimal refundSellingFeesCny = BigDecimal.ZERO;
        BigDecimal refundFbaFees = BigDecimal.ZERO;
        BigDecimal refundFbaFeesCny = BigDecimal.ZERO;
        BigDecimal refundOtherTransFees = BigDecimal.ZERO;
        BigDecimal refundOtherTransFeesCny = BigDecimal.ZERO;
        BigDecimal refundOther = BigDecimal.ZERO;
        BigDecimal refundOtherCny = BigDecimal.ZERO;

        for (SalesData refund : refundList) {
            String orderId = refund.getOrderId();
            LocalDate shipDate = refundShipDateMap.get(orderId);

            // 退款总额 = 产品销售+产品税+运费+运费税+礼品包装费+礼品包装税+监管费+监管费税+促销折扣+促销折扣税
            BigDecimal refundAmount = calculateSalesRevenue(refund).abs();

            // 维度一：按结算时间（使用销售数据自带汇率）
            if (refund.getTransactionDate() != null) {
                LocalDate transDate = refund.getTransactionDate().toLocalDate();
                if (!transDate.isBefore(startDate) && !transDate.isAfter(endDate)) {
                    BigDecimal rate = refund.getExchangeRate();
                    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) rate = BigDecimal.ONE;

                    refundBySettlement = refundBySettlement.add(refundAmount);
                    refundBySettlementCny = refundBySettlementCny.add(refundAmount.multiply(rate));
                    refundCountBySettlement++;
                }
            }

            // 维度二：按发货归属（使用配送汇率）
            if (shipDate != null && !shipDate.isBefore(startDate) && !shipDate.isAfter(endDate)) {
                BigDecimal rate = orderRateMap.getOrDefault(orderId, refund.getExchangeRate());
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) rate = BigDecimal.ONE;

                refundByShipment = refundByShipment.add(refundAmount);
                refundByShipmentCny = refundByShipmentCny.add(refundAmount.multiply(rate));
                refundCountByShipment++;

                // 退款相关费用（只统计按发货归属的）
                BigDecimal tax = nullToZero(refund.getMarketplaceWithheldTax()).abs();
                refundConsumptionTax = refundConsumptionTax.add(tax);
                refundConsumptionTaxCny = refundConsumptionTaxCny.add(tax.multiply(rate));

                BigDecimal selling = nullToZero(refund.getSellingFees()).abs();
                refundSellingFees = refundSellingFees.add(selling);
                refundSellingFeesCny = refundSellingFeesCny.add(selling.multiply(rate));

                BigDecimal fba = nullToZero(refund.getFbaFees()).abs();
                refundFbaFees = refundFbaFees.add(fba);
                refundFbaFeesCny = refundFbaFeesCny.add(fba.multiply(rate));

                BigDecimal otherTrans = nullToZero(refund.getOtherTransactionFees()).abs();
                refundOtherTransFees = refundOtherTransFees.add(otherTrans);
                refundOtherTransFeesCny = refundOtherTransFeesCny.add(otherTrans.multiply(rate));

                BigDecimal other = nullToZero(refund.getOther()).abs();
                refundOther = refundOther.add(other);
                refundOtherCny = refundOtherCny.add(other.multiply(rate));
            }
        }

        // ========== 5. 其他费计算（非income/refund类型），拆分为 ServiceFee 和 其他 ==========
        // ServiceFee: fee 类型且 非 CouponPayment
        // 其他: adjustment + other + (fee 且 CouponPayment)
        BigDecimal miscServiceFee = BigDecimal.ZERO;
        BigDecimal miscServiceFeeCny = BigDecimal.ZERO;
        BigDecimal otherFees = BigDecimal.ZERO;
        BigDecimal otherFeesCny = BigDecimal.ZERO;
        int miscFeesCount = 0;

        for (SalesData other : allOtherData) {
            boolean isServiceFee = "fee".equals(other.getTransactionCategory())
                    && !"CouponPayment".equals(other.getTransactionType());
            boolean isOther = !isServiceFee;

            String orderId = other.getOrderId();
            LocalDate shipDate = orderId != null ? refundShipDateMap.get(orderId) : null;

            BigDecimal amount = nullToZero(other.getTotal()).abs();
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal amountCny;
            // 有订单号且订单在本季度发货：使用配送汇率
            if (shipDate != null && !shipDate.isBefore(startDate) && !shipDate.isAfter(endDate)) {
                BigDecimal rate = orderRateMap.getOrDefault(orderId, other.getExchangeRate());
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) rate = BigDecimal.ONE;
                amountCny = amount.multiply(rate);
            }
            // 无订单号或订单不在本季度：按结算时间判断
            else if (other.getTransactionDate() != null) {
                LocalDate transDate = other.getTransactionDate().toLocalDate();
                if (!transDate.isBefore(startDate) && !transDate.isAfter(endDate)) {
                    BigDecimal rate = other.getExchangeRate();
                    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) rate = BigDecimal.ONE;
                    amountCny = amount.multiply(rate);
                } else {
                    continue;
                }
            } else {
                continue;
            }

            miscFeesCount++;
            if (isServiceFee) {
                miscServiceFee = miscServiceFee.add(amount);
                miscServiceFeeCny = miscServiceFeeCny.add(amountCny);
            } else {
                otherFees = otherFees.add(amount);
                otherFeesCny = otherFeesCny.add(amountCny);
            }
        }

        // ========== 6. 广告费计算 ==========
        List<AdvertisingData> adList = allAds.stream()
                .filter(ad -> isAdInQuarter(ad, startDate, endDate))
                .collect(Collectors.toList());

        BigDecimal advertisingCost = BigDecimal.ZERO;
        BigDecimal advertisingCostCny = BigDecimal.ZERO;

        for (AdvertisingData ad : adList) {
            BigDecimal cost = ad.getCost();
            if (cost != null) {
                advertisingCost = advertisingCost.add(cost);
                BigDecimal amountCny = ad.getAmountCny();
                if (amountCny != null) {
                    advertisingCostCny = advertisingCostCny.add(amountCny);
                } else {
                    advertisingCostCny = advertisingCostCny.add(cost);
                }
            }
        }

        // ========== 7. 汇总费用 ==========
        // 消费税合计 = 收入消费税 + 退款消费税
        BigDecimal consumptionTax = incomeConsumptionTax.add(refundConsumptionTax);
        BigDecimal consumptionTaxCny = incomeConsumptionTaxCny.add(refundConsumptionTaxCny);

        // 佣金/服务费明细
        BigDecimal sellingFees = incomeSellingFees.add(refundSellingFees);
        BigDecimal sellingFeesCny = incomeSellingFeesCny.add(refundSellingFeesCny);
        BigDecimal fbaFees = incomeFbaFees.add(refundFbaFees);
        BigDecimal fbaFeesCny = incomeFbaFeesCny.add(refundFbaFeesCny);
        BigDecimal otherTransactionFees = incomeOtherTransFees.add(refundOtherTransFees);
        BigDecimal otherTransactionFeesCny = incomeOtherTransFeesCny.add(refundOtherTransFeesCny);
        BigDecimal otherAmount = incomeOther.add(refundOther);
        BigDecimal otherAmountCny = incomeOtherCny.add(refundOtherCny);

        // 佣金/服务费合计
        BigDecimal totalServiceFee = sellingFees.add(fbaFees).add(otherTransactionFees).add(otherAmount);
        BigDecimal totalServiceFeeCny = sellingFeesCny.add(fbaFeesCny).add(otherTransactionFeesCny).add(otherAmountCny);

        // 总成本 = 佣金/服务费 + 其他费(ServiceFee+其他) + 广告费
        BigDecimal totalCost = totalServiceFeeCny.add(miscServiceFeeCny).add(otherFeesCny).add(advertisingCostCny);

        // ========== 8. 构建汇总结果 ==========
        TaxReportSummary summary = new TaxReportSummary();
        summary.setSiteCode(siteCode);
        summary.setSiteName(siteName);
        summary.setYearQuarter(yearQuarter);
        summary.setCurrencyCode(currencyCode);

        // 收入
        summary.setTotalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalRevenueCny(totalRevenueCny.setScale(2, RoundingMode.HALF_UP));

        // 退款-按发货归属
        summary.setRefundByShipment(refundByShipment.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundByShipmentCny(refundByShipmentCny.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundCountByShipment(refundCountByShipment);

        // 退款-按结算时间
        summary.setRefundBySettlement(refundBySettlement.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundBySettlementCny(refundBySettlementCny.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundCountBySettlement(refundCountBySettlement);

        // 消费税
        summary.setConsumptionTax(consumptionTax.setScale(2, RoundingMode.HALF_UP));
        summary.setConsumptionTaxCny(consumptionTaxCny.setScale(2, RoundingMode.HALF_UP));

        // 佣金/服务费明细
        summary.setSellingFees(sellingFees.setScale(2, RoundingMode.HALF_UP));
        summary.setSellingFeesCny(sellingFeesCny.setScale(2, RoundingMode.HALF_UP));
        summary.setFbaFees(fbaFees.setScale(2, RoundingMode.HALF_UP));
        summary.setFbaFeesCny(fbaFeesCny.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherTransactionFees(otherTransactionFees.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherTransactionFeesCny(otherTransactionFeesCny.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherAmount(otherAmount.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherAmountCny(otherAmountCny.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalServiceFee(totalServiceFee.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalServiceFeeCny(totalServiceFeeCny.setScale(2, RoundingMode.HALF_UP));

        // 其他费（拆分）
        summary.setMiscServiceFee(miscServiceFee.setScale(2, RoundingMode.HALF_UP));
        summary.setMiscServiceFeeCny(miscServiceFeeCny.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherFees(otherFees.setScale(2, RoundingMode.HALF_UP));
        summary.setOtherFeesCny(otherFeesCny.setScale(2, RoundingMode.HALF_UP));
        summary.setMiscFeesCount(miscFeesCount);

        // 广告费
        summary.setAdvertisingCost(advertisingCost.setScale(2, RoundingMode.HALF_UP));
        summary.setAdvertisingCostCny(advertisingCostCny.setScale(2, RoundingMode.HALF_UP));

        // 总成本
        summary.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));

        return summary;
    }

    /**
     * 计算销售数据的收入/退款总额
     * 公式：产品销售+产品税+运费+运费税+礼品包装费+礼品包装税+监管费+监管费税+促销折扣+促销折扣税
     */
    private BigDecimal calculateSalesRevenue(SalesData sales) {
        return nullToZero(sales.getProductSales())
                .add(nullToZero(sales.getProductSalesTax()))
                .add(nullToZero(sales.getShippingCredits()))
                .add(nullToZero(sales.getShippingCreditsTax()))
                .add(nullToZero(sales.getGiftWrapCredits()))
                .add(nullToZero(sales.getGiftWrapCreditsTax()))
                .add(nullToZero(sales.getRegulatoryFee()))
                .add(nullToZero(sales.getRegulatoryFeeTax()))
                .add(nullToZero(sales.getPromotionalRebates()))
                .add(nullToZero(sales.getPromotionalRebatesTax()));
    }

    /**
     * 空值转零
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 判断广告数据是否在季度范围内
     */
    private boolean isAdInQuarter(AdvertisingData ad, LocalDate startDate, LocalDate endDate) {
        LocalDate adStart = ad.getBillingStartDate();
        LocalDate adEnd = ad.getBillingEndDate();
        if (adStart == null && adEnd == null) return false;

        // 广告周期与季度有交集
        if (adStart != null && adEnd != null) {
            return !(adEnd.isBefore(startDate) || adStart.isAfter(endDate));
        }
        if (adStart != null) {
            return !adStart.isAfter(endDate);
        }
        // adEnd 不为 null（因为前面已排除两者都为 null 的情况）
        return adEnd != null && !adEnd.isBefore(startDate);
    }

    @Override
    public List<FeeBreakdown> getFeeBreakdown(String siteCode, String startQuarter, String endQuarter) {
        log.info("Getting fee breakdown: siteCode={}, startQuarter={}, endQuarter={}", siteCode, startQuarter, endQuarter);

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();

        List<String> sites = StringUtils.hasText(siteCode)
                ? List.of(siteCode)
                : List.of("US", "CA", "UK", "DE");

        List<String> quarters = getQuartersInRange(startQuarter, endQuarter);

        List<FeeBreakdown> results = new ArrayList<>();

        for (String site : sites) {
            for (String quarter : quarters) {
                List<FeeBreakdown> fees = calculateFeeBreakdown(site, quarter, shopId);
                results.addAll(fees);
            }
        }

        return results;
    }

    /**
     * 计算费用分类明细
     */
    private List<FeeBreakdown> calculateFeeBreakdown(String siteCode, String yearQuarter, Long shopId) {
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        // 查询费用类数据
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getShopId, shopId)  // 店铺数据隔离
                .eq(SalesData::getSiteCode, siteCode)
                .in(SalesData::getTransactionCategory, List.of("fee", "adjustment", "other"))
                .ge(SalesData::getTransactionDate, startDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, endDate.plusDays(1).atStartOfDay());
        List<SalesData> feeList = salesDataMapper.selectList(wrapper);

        // 按 transactionType 分组统计
        Map<String, List<SalesData>> groupedByType = feeList.stream()
                .filter(f -> f.getTransactionType() != null)
                .collect(Collectors.groupingBy(SalesData::getTransactionType));

        List<FeeBreakdown> results = new ArrayList<>();

        for (Map.Entry<String, List<SalesData>> entry : groupedByType.entrySet()) {
            String feeType = entry.getKey();
            List<SalesData> fees = entry.getValue();

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalAmountCny = BigDecimal.ZERO;

            for (SalesData fee : fees) {
                BigDecimal amount = sumFeeFields(fee).abs();
                totalAmount = totalAmount.add(amount);

                BigDecimal rate = fee.getExchangeRate();
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    totalAmountCny = totalAmountCny.add(amount.multiply(rate));
                } else {
                    totalAmountCny = totalAmountCny.add(amount);
                }
            }

            FeeBreakdown breakdown = new FeeBreakdown();
            breakdown.setSiteCode(siteCode);
            breakdown.setYearQuarter(yearQuarter);
            breakdown.setFeeType(feeType);
            breakdown.setFeeCategory(fees.get(0).getTransactionCategory());
            breakdown.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
            breakdown.setAmountCny(totalAmountCny.setScale(2, RoundingMode.HALF_UP));
            breakdown.setTransactionCount(fees.size());

            results.add(breakdown);
        }

        // 按金额降序排序
        results.sort((a, b) -> b.getAmountCny().compareTo(a.getAmountCny()));

        return results;
    }

    /**
     * 合计费用字段
     */
    private BigDecimal sumFeeFields(SalesData data) {
        BigDecimal sum = BigDecimal.ZERO;
        if (data.getSellingFees() != null) sum = sum.add(data.getSellingFees());
        if (data.getFbaFees() != null) sum = sum.add(data.getFbaFees());
        if (data.getOtherTransactionFees() != null) sum = sum.add(data.getOtherTransactionFees());
        if (data.getOther() != null) sum = sum.add(data.getOther());
        return sum;
    }

    /**
     * 计算发货单收入
     * 收入 = 商品价格 + 商品税 + 运费 + 运费税 + 礼品包装价格 + 礼品包装税 + 商品促销折扣 + 货件促销折扣
     */
    private BigDecimal calculateShippingRevenue(ShippingData shipping) {
        BigDecimal revenue = BigDecimal.ZERO;
        
        if (shipping.getProductPrice() != null) {
            revenue = revenue.add(shipping.getProductPrice());
        }
        if (shipping.getProductTax() != null) {
            revenue = revenue.add(shipping.getProductTax());
        }
        if (shipping.getShippingPrice() != null) {
            revenue = revenue.add(shipping.getShippingPrice());
        }
        if (shipping.getShippingTax() != null) {
            revenue = revenue.add(shipping.getShippingTax());
        }
        if (shipping.getGiftWrapPrice() != null) {
            revenue = revenue.add(shipping.getGiftWrapPrice());
        }
        if (shipping.getGiftWrapTax() != null) {
            revenue = revenue.add(shipping.getGiftWrapTax());
        }
        if (shipping.getProductPromotionDiscount() != null) {
            revenue = revenue.add(shipping.getProductPromotionDiscount());
        }
        if (shipping.getShipmentPromotionDiscount() != null) {
            revenue = revenue.add(shipping.getShipmentPromotionDiscount());
        }
        
        return revenue;
    }

    /**
     * 预加载 Marketplace 数据
     */
    private Map<String, Marketplace> loadMarketplaceMap(List<String> sites) {
        LambdaQueryWrapper<Marketplace> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Marketplace::getSiteCode, sites);
        List<Marketplace> marketplaces = marketplaceMapper.selectList(wrapper);
        return marketplaces.stream()
                .collect(Collectors.toMap(Marketplace::getSiteCode, m -> m, (a, b) -> a));
    }


    /**
     * 获取季度范围列表
     */
    private List<String> getQuartersInRange(String startQuarter, String endQuarter) {
        List<String> quarters = new ArrayList<>();

        if (!StringUtils.hasText(startQuarter) || !StringUtils.hasText(endQuarter)) {
            // 默认当前季度
            LocalDate now = LocalDate.now();
            int quarter = (now.getMonthValue() - 1) / 3 + 1;
            quarters.add(now.getYear() + "-Q" + quarter);
            return quarters;
        }

        int startYear = Integer.parseInt(startQuarter.substring(0, 4));
        int startQ = Integer.parseInt(startQuarter.substring(6, 7));
        int endYear = Integer.parseInt(endQuarter.substring(0, 4));
        int endQ = Integer.parseInt(endQuarter.substring(6, 7));

        int currentYear = startYear;
        int currentQ = startQ;

        while (currentYear < endYear || (currentYear == endYear && currentQ <= endQ)) {
            quarters.add(currentYear + "-Q" + currentQ);
            currentQ++;
            if (currentQ > 4) {
                currentQ = 1;
                currentYear++;
            }
        }

        return quarters;
    }

    private LocalDate getQuarterStartDate(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    private LocalDate getQuarterEndDate(int year, int quarter) {
        int month = quarter * 3;
        LocalDate firstOfNextMonth = LocalDate.of(year, month, 1).plusMonths(1);
        return firstOfNextMonth.minusDays(1);
    }

    @Override
    public void exportTaxSummary(String siteCode, String startQuarter, String endQuarter, HttpServletResponse response) {
        log.info("Exporting tax summary: siteCode={}, startQuarter={}, endQuarter={}", siteCode, startQuarter, endQuarter);

        try {
            List<TaxReportSummary> summaries = getTaxSummary(siteCode, startQuarter, endQuarter);

            String fileName = "tax_summary_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("报税汇总");

                // 表头 - V2版本（移除发货订单数，其他费拆分为 ServiceFee + 其他）
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "站点", "季度", "币种",
                        "收入总额(原币)", "收入总额(人民币)",
                        "退款-发货(原币)", "退款-发货(人民币)", "退款笔数-发货",
                        "退款-结算(原币)", "退款-结算(人民币)", "退款笔数-结算",
                        "消费税(原币)", "消费税(人民币)",
                        "销售费用(原币)", "销售费用(人民币)",
                        "FBA费用(原币)", "FBA费用(人民币)",
                        "其他交易费(原币)", "其他交易费(人民币)",
                        "其他(原币)", "其他(人民币)",
                        "佣金/服务费合计(原币)", "佣金/服务费合计(人民币)",
                        "其他费-ServiceFee(原币)", "其他费-ServiceFee(人民币)",
                        "其他费-其他(原币)", "其他费-其他(人民币)", "其他费笔数",
                        "广告费(原币)", "广告费(人民币)",
                        "总成本(人民币)"
                };

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 数据行
                int rowNum = 1;
                for (TaxReportSummary s : summaries) {
                    Row row = sheet.createRow(rowNum++);
                    int col = 0;
                    row.createCell(col++).setCellValue(s.getSiteName());
                    row.createCell(col++).setCellValue(s.getYearQuarter());
                    row.createCell(col++).setCellValue(s.getCurrencyCode());
                    row.createCell(col++).setCellValue(toDouble(s.getTotalRevenue()));
                    row.createCell(col++).setCellValue(toDouble(s.getTotalRevenueCny()));
                    // 退款-发货
                    row.createCell(col++).setCellValue(toDouble(s.getRefundByShipment()));
                    row.createCell(col++).setCellValue(toDouble(s.getRefundByShipmentCny()));
                    row.createCell(col++).setCellValue(s.getRefundCountByShipment() != null ? s.getRefundCountByShipment() : 0);
                    // 退款-结算
                    row.createCell(col++).setCellValue(toDouble(s.getRefundBySettlement()));
                    row.createCell(col++).setCellValue(toDouble(s.getRefundBySettlementCny()));
                    row.createCell(col++).setCellValue(s.getRefundCountBySettlement() != null ? s.getRefundCountBySettlement() : 0);
                    // 消费税
                    row.createCell(col++).setCellValue(toDouble(s.getConsumptionTax()));
                    row.createCell(col++).setCellValue(toDouble(s.getConsumptionTaxCny()));
                    // 佣金/服务费明细
                    row.createCell(col++).setCellValue(toDouble(s.getSellingFees()));
                    row.createCell(col++).setCellValue(toDouble(s.getSellingFeesCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getFbaFees()));
                    row.createCell(col++).setCellValue(toDouble(s.getFbaFeesCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherTransactionFees()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherTransactionFeesCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherAmount()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherAmountCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getTotalServiceFee()));
                    row.createCell(col++).setCellValue(toDouble(s.getTotalServiceFeeCny()));
                    // 其他费（拆分）
                    row.createCell(col++).setCellValue(toDouble(s.getMiscServiceFee()));
                    row.createCell(col++).setCellValue(toDouble(s.getMiscServiceFeeCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherFees()));
                    row.createCell(col++).setCellValue(toDouble(s.getOtherFeesCny()));
                    row.createCell(col++).setCellValue(s.getMiscFeesCount() != null ? s.getMiscFeesCount() : 0);
                    // 广告费
                    row.createCell(col++).setCellValue(toDouble(s.getAdvertisingCost()));
                    row.createCell(col++).setCellValue(toDouble(s.getAdvertisingCostCny()));
                    // 总成本
                    row.createCell(col++).setCellValue(toDouble(s.getTotalCost()));
                }

                // 自动列宽
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Failed to export tax summary", e);
            throw new RuntimeException("Failed to export report", e);
        }
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }
}
