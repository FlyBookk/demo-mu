package com.musheng.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.advertising.entity.AdvertisingBill;
import com.musheng.business.advertising.entity.AdvertisingBillItem;
import com.musheng.business.advertising.mapper.AdvertisingBillItemMapper;
import com.musheng.business.advertising.mapper.AdvertisingBillMapper;
import com.musheng.business.rate.dto.RateWithDateDTO;
import com.musheng.business.rate.service.RateService;
import com.musheng.business.report.dto.DashboardData;
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
import java.util.Objects;
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
    private final AdvertisingBillMapper advertisingBillMapper;
    private final AdvertisingBillItemMapper advertisingBillItemMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final RateService rateService;

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

        // 2. 发货数据（收入来源）- 使用聚合查询，排除站点为空的数据
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getShopId, shopId)
                .in(ShippingData::getSiteCode, sites)
                .isNotNull(ShippingData::getSiteCode)
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
            
            BigDecimal revenue = s.getTotalAmount();
            if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
                revenue = calculateShippingTotalAmount(s);
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
            
            BigDecimal refundAmt = r.getTotal() != null ? r.getTotal() : BigDecimal.ZERO;
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
        // 增长率分母用 abs：当 previous 为负时需正分母，否则百分比无意义（非金额汇总）
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<TaxReportSummary> getTaxSummary(String siteCode, String startQuarter, String endQuarter, String refundDateMode) {
        // 退款时间维度：ship=配送日期（默认），settlement=结算日期
        final String mode = StringUtils.hasText(refundDateMode) ? refundDateMode : "ship";
        log.info("Getting tax summary: siteCode={}, startQuarter={}, endQuarter={}, refundDateMode={}", siteCode, startQuarter, endQuarter, mode);

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

        // 2. 批量查询发货数据（按日期范围，向前扩展6个月以覆盖Q-1发货→Q退款的订单汇率）
        // 排除 Non-Amazon 渠道订单（S01前缀为非亚马逊渠道，不参与报税计算），排除站点为空的数据
        LocalDate expandedMinStartDate = minStartDate.minusMonths(6);
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getShopId, shopId)
                .in(ShippingData::getSiteCode, sites)
                .isNotNull(ShippingData::getSiteCode)
                .between(ShippingData::getShipDate, expandedMinStartDate, maxEndDate)
                .and(w -> w.isNull(ShippingData::getOrderId)
                        .or().notLike(ShippingData::getOrderId, "S01%"));
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
        // 排除 Non-Amazon 渠道订单（如 sim1.stores.amazon.com），这类订单不参与亚马逊报税计算
        LambdaQueryWrapper<SalesData> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .in(SalesData::getTransactionCategory, List.of("income", "refund"))
                .and(w -> w.isNull(SalesData::getMarketplace)
                        .or().notLike(SalesData::getMarketplace, "sim1.stores.amazon.com"))
                .ge(SalesData::getTransactionDate, minStartDate.minusMonths(1).atStartOfDay())
                .lt(SalesData::getTransactionDate, extendedEndDate.plusDays(1).atStartOfDay());
        List<SalesData> allSalesData = salesDataMapper.selectList(salesWrapper);

        // 6. 批量查询其他费用数据（非income/refund类型，按日期过滤）
        // 不排除 sim1.stores.amazon.com，该渠道的非收入类型数据也属于平台费用
        LambdaQueryWrapper<SalesData> otherWrapper = new LambdaQueryWrapper<>();
        otherWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .notIn(SalesData::getTransactionCategory, List.of("income", "refund"))
                .ge(SalesData::getTransactionDate, minStartDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, maxEndDate.plusDays(1).atStartOfDay());
        List<SalesData> allOtherData = salesDataMapper.selectList(otherWrapper);

        // 6.1 单独查询 sim1.stores.amazon.com 渠道的 income 类型数据
        // 这类数据（S01-前缀订单）的 total 字段是三方平台配送费扣款，需计入其他费用
        LambdaQueryWrapper<SalesData> sim1Wrapper = new LambdaQueryWrapper<>();
        sim1Wrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, "income")
                .like(SalesData::getMarketplace, "sim1.stores.amazon.com")
                .ge(SalesData::getTransactionDate, minStartDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, maxEndDate.plusDays(1).atStartOfDay());
        List<SalesData> sim1Data = salesDataMapper.selectList(sim1Wrapper);

        // 7. 批量查询广告数据（主表+明细）
        // 按「账单周期与查询范围有交集」过滤：billingEndDate >= minStartDate 且 billingStartDate <= maxEndDate
        // 兼容：site_code 可能为空（从 store_name 推断），故用 OR site_code IS NULL 纳入
        LambdaQueryWrapper<AdvertisingBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(AdvertisingBill::getShopId, shopId)
                .and(w -> w.in(AdvertisingBill::getSiteCode, sites).or().isNull(AdvertisingBill::getSiteCode))
                .ge(AdvertisingBill::getBillingEndDate, minStartDate)
                .le(AdvertisingBill::getBillingStartDate, maxEndDate);
        List<AdvertisingBill> allBills = advertisingBillMapper.selectList(billWrapper);
        List<Long> billIds = allBills.stream().map(AdvertisingBill::getId).collect(Collectors.toList());
        Map<Long, AdvertisingBill> billMap = allBills.stream().collect(Collectors.toMap(AdvertisingBill::getId, b -> b));

        List<AdDataForTax> allAdData = new ArrayList<>();
        if (!billIds.isEmpty()) {
            LambdaQueryWrapper<AdvertisingBillItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(AdvertisingBillItem::getBillId, billIds);
            List<AdvertisingBillItem> items = advertisingBillItemMapper.selectList(itemWrapper);
            for (AdvertisingBillItem item : items) {
                AdvertisingBill bill = billMap.get(item.getBillId());
                if (bill != null) {
                    String site = StringUtils.hasText(bill.getSiteCode())
                            ? bill.getSiteCode()
                            : inferSiteCodeFromStoreName(bill.getStoreName());
                    allAdData.add(new AdDataForTax(site, item.getCost(), item.getAmountCny(),
                            bill.getBillingStartDate(), bill.getBillingEndDate(),
                            bill.getCurrency(), bill.getIssueDate()));
                }
            }
        }

        log.info("Data loaded in {}ms: shipping={}, sales={}, other={}, sim1={}, ads={}",
                System.currentTimeMillis() - startTime,
                allShippingData.size(), allSalesData.size(), allOtherData.size(), sim1Data.size(), allAdData.size());

        // ========== 按站点分组（内存操作，快速） ==========
        Map<String, List<ShippingData>> shippingBySite = allShippingData.stream()
                .collect(Collectors.groupingBy(ShippingData::getSiteCode));
        Map<String, List<SalesData>> salesBySite = allSalesData.stream()
                .collect(Collectors.groupingBy(SalesData::getSiteCode));
        // sim1 数据合并到 otherBySite，其 total 字段将计入其他费用
        Map<String, List<SalesData>> otherBySite = new HashMap<>();
        allOtherData.forEach(d -> otherBySite.computeIfAbsent(d.getSiteCode(), k -> new ArrayList<>()).add(d));
        sim1Data.forEach(d -> otherBySite.computeIfAbsent(d.getSiteCode(), k -> new ArrayList<>()).add(d));
        Map<String, List<AdDataForTax>> adBySite = allAdData.stream()
                .filter(ad -> ad.siteCode != null)
                .collect(Collectors.groupingBy(ad -> ad.siteCode));

        // ========== 在内存中计算每个站点+季度的汇总 ==========
        List<TaxReportSummary> results = new ArrayList<>();

        for (String site : sites) {
            List<ShippingData> siteShipping = shippingBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteSales = salesBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteOther = otherBySite.getOrDefault(site, Collections.emptyList());
            List<AdDataForTax> siteAds = adBySite.getOrDefault(site, Collections.emptyList());
            Map<String, LocalDate> siteRefundShipDates = refundShipDateMap.getOrDefault(site, Collections.emptyMap());
            Map<String, BigDecimal> siteOrderRates = orderRateMapBySite.getOrDefault(site, Collections.emptyMap());

            for (String quarterStr : quarters) {
                TaxReportSummary summary = calculateTaxSummaryFromMemory(
                        site, quarterStr, marketplaceMap.get(site),
                        siteShipping, siteSales, siteOther, siteAds, 
                        siteRefundShipDates, siteOrderRates, mode
                );
                if (summary != null) {
                    results.add(summary);
                }
            }
        }

        // 过滤掉无数据的站点+季度（收入、退款、佣金等均为0的不展示）
        results = results.stream()
                .filter(TaxReportServiceImpl::hasAnyTaxData)
                .collect(Collectors.toList());

        log.info("Tax summary calculation completed in {}ms, filtered to {} rows with data",
                System.currentTimeMillis() - startTime, results.size());
        return results;
    }

    /** 判断站点+季度是否有有效数据（任一核心字段非零则展示） */
    private static boolean hasAnyTaxData(TaxReportSummary s) {
        return isNonZero(s.getTotalRevenueCny()) || isNonZero(s.getTotalRevenue())
                || (s.getRefundCount() != null && s.getRefundCount() > 0)
                || isNonZero(s.getRefundAmountCny())
                || isNonZero(s.getTotalCommissionFeeCny()) || isNonZero(s.getConsumptionTaxCny())
                || isNonZero(s.getAdvertisingCostCny()) || isNonZero(s.getPlatformExpensesCny())
                || isNonZero(s.getProfit4PercentCny());
    }

    private static boolean isNonZero(BigDecimal b) {
        return b != null && b.compareTo(BigDecimal.ZERO) != 0;
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
            List<AdDataForTax> allAds,
            Map<String, LocalDate> refundShipDateMap,
            Map<String, BigDecimal> orderRateMap,
            String refundDateMode) {

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

        // 退款筛选：根据 refundDateMode 决定过滤维度
        // ship（默认）：退款订单号在本季度配送订单集合中（配送日期口径）
        // settlement：退款记录的结算日期（transactionDate）在本季度范围内
        List<SalesData> refundList;
        if ("settlement".equals(refundDateMode)) {
            // 结算日期口径：按退款记录自身的 transactionDate 落在季度范围内
            refundList = allSalesData.stream()
                    .filter(s -> "refund".equals(s.getTransactionCategory()))
                    .filter(s -> {
                        if (s.getTransactionDate() == null) return false;
                        LocalDate txDate = s.getTransactionDate().toLocalDate();
                        return !txDate.isBefore(startDate) && !txDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());
        } else {
            // 配送日期口径（默认）：退款订单号在本季度配送订单集合中
            refundList = allSalesData.stream()
                    .filter(s -> "refund".equals(s.getTransactionCategory()))
                    .filter(s -> s.getOrderId() != null && shippingOrderIds.contains(s.getOrderId()))
                    .collect(Collectors.toList());
        }

        // ========== 3. 收入计算：收入总额取配送数据总计费用（8项之和） ==========
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenueCny = BigDecimal.ZERO;
        for (ShippingData shipping : allShipping) {
            if (shipping.getShipDate() == null || shipping.getShipDate().isBefore(startDate) || shipping.getShipDate().isAfter(endDate)) {
                continue;
            }
            BigDecimal amount = shipping.getTotalAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                amount = calculateShippingTotalAmount(shipping);
            }
            totalRevenue = totalRevenue.add(amount);
            BigDecimal rate = shipping.getExchangeRate();
            totalRevenueCny = totalRevenueCny.add((rate != null && rate.compareTo(BigDecimal.ZERO) > 0) ? amount.multiply(rate) : amount);
        }

        // 收入相关费用（仅income类型，来自销售数据）
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

            // 消费税（平台代扣税）- 保留原正负
            BigDecimal tax = nullToZero(income.getMarketplaceWithheldTax());
            incomeConsumptionTax = incomeConsumptionTax.add(tax);
            incomeConsumptionTaxCny = incomeConsumptionTaxCny.add(tax.multiply(rate));

            // 销售费用 - 保留原正负
            BigDecimal selling = nullToZero(income.getSellingFees());
            incomeSellingFees = incomeSellingFees.add(selling);
            incomeSellingFeesCny = incomeSellingFeesCny.add(selling.multiply(rate));

            // FBA费用 - 保留原正负
            BigDecimal fba = nullToZero(income.getFbaFees());
            incomeFbaFees = incomeFbaFees.add(fba);
            incomeFbaFeesCny = incomeFbaFeesCny.add(fba.multiply(rate));

            // 其他交易费 - 保留原正负
            BigDecimal otherTrans = nullToZero(income.getOtherTransactionFees());
            incomeOtherTransFees = incomeOtherTransFees.add(otherTrans);
            incomeOtherTransFeesCny = incomeOtherTransFeesCny.add(otherTrans.multiply(rate));

            // 其他 - 保留原正负
            BigDecimal other = nullToZero(income.getOther());
            incomeOther = incomeOther.add(other);
            incomeOtherCny = incomeOtherCny.add(other.multiply(rate));
        }

        // ========== 4. 退款计算（单一配送日期口径） ==========
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal refundAmountCny = BigDecimal.ZERO;
        int refundCount = 0;

        // 退款关联的费用（方式一 refund 部分）
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
            // 退款金额 = 9 项求和（含监管费、监管费税，不含促销折扣税）
            BigDecimal amt = calculateRefundAmount(refund);

            // 汇率选取：
            // - 配送日期口径：优先使用配送汇率（orderRateMap），缺失时回退到退款记录自身汇率
            // - 结算日期口径：使用退款记录自身的结算汇率（exchangeRate）
            BigDecimal rate;
            if ("settlement".equals(refundDateMode)) {
                rate = refund.getExchangeRate();
            } else {
                rate = orderRateMap.getOrDefault(refund.getOrderId(), refund.getExchangeRate());
            }
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                rate = BigDecimal.ONE;
            }

            // 累加退款金额和笔数
            refundAmount = refundAmount.add(amt);
            refundAmountCny = refundAmountCny.add(amt.multiply(rate));
            refundCount++;

            // 退款关联的费用 - 保留原正负，汇率使用配送日期汇率
            BigDecimal tax = nullToZero(refund.getMarketplaceWithheldTax());
            refundConsumptionTax = refundConsumptionTax.add(tax);
            refundConsumptionTaxCny = refundConsumptionTaxCny.add(tax.multiply(rate));

            BigDecimal selling = nullToZero(refund.getSellingFees());
            refundSellingFees = refundSellingFees.add(selling);
            refundSellingFeesCny = refundSellingFeesCny.add(selling.multiply(rate));

            BigDecimal fba = nullToZero(refund.getFbaFees());
            refundFbaFees = refundFbaFees.add(fba);
            refundFbaFeesCny = refundFbaFeesCny.add(fba.multiply(rate));

            BigDecimal otherTrans = nullToZero(refund.getOtherTransactionFees());
            refundOtherTransFees = refundOtherTransFees.add(otherTrans);
            refundOtherTransFeesCny = refundOtherTransFeesCny.add(otherTrans.multiply(rate));

            BigDecimal other = nullToZero(refund.getOther());
            refundOther = refundOther.add(other);
            refundOtherCny = refundOtherCny.add(other.multiply(rate));
        }

        // ========== 5. 方式二费用计算（非income/refund类型），取具体费用项按类别归入 ==========
        // 佣金3项：sellingFees + fbaFees + otherTransactionFees
        // 其他费：other
        // 税：marketplaceWithheldTax（新增统计）
        BigDecimal miscCommissionFee = BigDecimal.ZERO;
        BigDecimal miscCommissionFeeCny = BigDecimal.ZERO;
        BigDecimal miscOtherFee = BigDecimal.ZERO;
        BigDecimal miscOtherFeeCny = BigDecimal.ZERO;
        BigDecimal miscConsumptionTax = BigDecimal.ZERO;
        BigDecimal miscConsumptionTaxCny = BigDecimal.ZERO;

        for (SalesData other : allOtherData) {
            // 排除 transfer 类型（资金划转，不属于费用），使用转化后的 transactionCategory 字段
            if ("transfer".equals(other.getTransactionCategory())) {
                continue;
            }

            // 排除结算日期不在季度范围内的记录
            if (other.getTransactionDate() == null) continue;
            LocalDate transDate = other.getTransactionDate().toLocalDate();
            if (transDate.isBefore(startDate) || transDate.isAfter(endDate)) continue;

            // 汇率使用结算日期汇率
            BigDecimal rate = other.getExchangeRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                rate = BigDecimal.ONE;
            }

            // 佣金3项：sellingFees + fbaFees + otherTransactionFees
            BigDecimal selling = nullToZero(other.getSellingFees());
            BigDecimal fba = nullToZero(other.getFbaFees());
            BigDecimal otherTrans = nullToZero(other.getOtherTransactionFees());
            BigDecimal commission = selling.add(fba).add(otherTrans);
            miscCommissionFee = miscCommissionFee.add(commission);
            miscCommissionFeeCny = miscCommissionFeeCny.add(commission.multiply(rate));

            // 其他费：other
            BigDecimal otherAmt = nullToZero(other.getOther());
            miscOtherFee = miscOtherFee.add(otherAmt);
            miscOtherFeeCny = miscOtherFeeCny.add(otherAmt.multiply(rate));

            // 税：marketplaceWithheldTax（新增统计）
            BigDecimal tax = nullToZero(other.getMarketplaceWithheldTax());
            miscConsumptionTax = miscConsumptionTax.add(tax);
            miscConsumptionTaxCny = miscConsumptionTaxCny.add(tax.multiply(rate));
        }

        // ========== 6. 广告费计算 ==========
        List<AdDataForTax> adList = allAds.stream()
                .filter(ad -> isAdInQuarter(ad, startDate, endDate))
                .collect(Collectors.toList());

        BigDecimal advertisingCost = BigDecimal.ZERO;
        BigDecimal advertisingCostCny = BigDecimal.ZERO;

        for (AdDataForTax ad : adList) {
            BigDecimal cost = ad.cost;
            if (cost != null) {
                advertisingCost = advertisingCost.add(cost);
                BigDecimal amountCny = ad.amountCny;
                if (amountCny != null) {
                    advertisingCostCny = advertisingCostCny.add(amountCny);
                } else {
                    // 兼容：amountCny 未持久化时，按发票开具日期查汇率换算（与配送/销售一致）
                    try {
                        if (StringUtils.hasText(ad.currency()) && ad.issueDate() != null) {
                            if ("CNY".equalsIgnoreCase(ad.currency())) {
                                advertisingCostCny = advertisingCostCny.add(cost);
                            } else {
                                RateWithDateDTO dto = rateService.getRateWithDate(ad.currency(), ad.issueDate());
                                advertisingCostCny = advertisingCostCny.add(cost.multiply(dto.getRate()));
                            }
                        }
                    } catch (Exception e) {
                        log.warn("广告费汇率换算失败: currency={}, date={}", ad.currency(), ad.issueDate(), e);
                    }
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

        // ========== 7.1 费用汇总（方式一+方式二叠加） ==========
        // 佣金服务费合计 = 方式一(sellingFees+fbaFees+otherTransactionFees) + 方式二(miscCommissionFee)
        BigDecimal totalCommissionFee = sellingFees.add(fbaFees).add(otherTransactionFees).add(miscCommissionFee);
        BigDecimal totalCommissionFeeCny = sellingFeesCny.add(fbaFeesCny).add(otherTransactionFeesCny).add(miscCommissionFeeCny);

        // 其他费合计 = 方式一(otherAmount) + 方式二(miscOtherFee)
        BigDecimal totalOtherFee = otherAmount.add(miscOtherFee);
        BigDecimal totalOtherFeeCny = otherAmountCny.add(miscOtherFeeCny);

        // 税合计 = 方式一(incomeConsumptionTax+refundConsumptionTax) + 方式二(miscConsumptionTax)
        // 覆盖之前的 consumptionTax 变量（之前只有方式一）
        consumptionTax = consumptionTax.add(miscConsumptionTax);
        consumptionTaxCny = consumptionTaxCny.add(miscConsumptionTaxCny);


        // ========== 8. 平台支出与采购成本计算（按图片公式） ==========
        // ③收入净额 = 收入总额 - |退款金额|
        BigDecimal netRevenueCny = totalRevenueCny.subtract(refundAmountCny.abs());

        // ⑨平台支出合计 = |消费税| + |佣金服务费| + |广告费| + |其他费|
        BigDecimal platformExpensesCny = consumptionTaxCny.abs()
                .add(totalCommissionFeeCny.abs())
                .add(advertisingCostCny.abs())
                .add(totalOtherFeeCny.abs());

        // ⑩4%利润 = ③ × 4%
        BigDecimal profit4PercentCny = netRevenueCny.multiply(new BigDecimal("0.04"));

        // ⑪采购成本 = ③ − ⑨ − ⑩
        BigDecimal procurementCostCny = netRevenueCny.subtract(platformExpensesCny).subtract(profit4PercentCny);

        // ========== 9. 构建汇总结果 ==========
        TaxReportSummary summary = new TaxReportSummary();
        summary.setSiteCode(siteCode);
        summary.setSiteName(siteName);
        summary.setYearQuarter(yearQuarter);
        summary.setCurrencyCode(currencyCode);

        // 收入
        summary.setTotalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalRevenueCny(totalRevenueCny.setScale(2, RoundingMode.HALF_UP));

        // 配送匹配订单笔数：本季度配送的订单号，且在销售数据（income类型）中能匹配到的去重订单数
        long matchedOrderCount = incomeList.stream()
                .map(SalesData::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        summary.setShippingMatchCount((int) matchedOrderCount);

        // 退款（单一配送日期口径）
        summary.setRefundAmount(refundAmount.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundAmountCny(refundAmountCny.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundCount(refundCount);

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

        // 佣金服务费合计（方式一+方式二）
        summary.setTotalCommissionFee(totalCommissionFee.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalCommissionFeeCny(totalCommissionFeeCny.setScale(2, RoundingMode.HALF_UP));

        // 其他费合计（方式一+方式二）
        summary.setTotalOtherFee(totalOtherFee.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalOtherFeeCny(totalOtherFeeCny.setScale(2, RoundingMode.HALF_UP));

        // 广告费（支出取反，与佣金服务费等保持一致，负数表示支出）
        summary.setAdvertisingCost(advertisingCost.negate().setScale(2, RoundingMode.HALF_UP));
        summary.setAdvertisingCostCny(advertisingCostCny.negate().setScale(2, RoundingMode.HALF_UP));

        // 平台支出与采购成本（按图片公式）
        summary.setPlatformExpenses(BigDecimal.ZERO);
        summary.setPlatformExpensesCny(platformExpensesCny.setScale(2, RoundingMode.HALF_UP));
        summary.setProfit4Percent(BigDecimal.ZERO);
        summary.setProfit4PercentCny(profit4PercentCny.setScale(2, RoundingMode.HALF_UP));
        summary.setProcurementCost(BigDecimal.ZERO);
        summary.setProcurementCostCny(procurementCostCny.setScale(2, RoundingMode.HALF_UP));

        return summary;
    }

    /**
     * 计算退款金额（9 项求和）
     * 产品销售额、产品税、运费收入、运费税、礼品包装收入、礼品包装税、
     * 监管费、监管费税、促销折扣
     *
     * @param refund 退款记录
     * @return 退款小计（原币）
     * @author wanhua
     * 10:30 2026年03月20日
     */
    private BigDecimal calculateRefundAmount(SalesData refund) {
        return nullToZero(refund.getProductSales())
                .add(nullToZero(refund.getProductSalesTax()))
                .add(nullToZero(refund.getShippingCredits()))
                .add(nullToZero(refund.getShippingCreditsTax()))
                .add(nullToZero(refund.getGiftWrapCredits()))
                .add(nullToZero(refund.getGiftWrapCreditsTax()))
                .add(nullToZero(refund.getRegulatoryFee()))
                .add(nullToZero(refund.getRegulatoryFeeTax()))
                .add(nullToZero(refund.getPromotionalRebates()));
    }

    /**
     * 空值转零
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** 广告费数据（用于报税汇总计算） */
    private record AdDataForTax(String siteCode, BigDecimal cost, BigDecimal amountCny,
                                LocalDate billingStartDate, LocalDate billingEndDate,
                                String currency, LocalDate issueDate) {}

    /** 从店铺名称推断站点编码（与 AdvertisingBillServiceImpl 一致） */
    private static String inferSiteCodeFromStoreName(String storeName) {
        if (!StringUtils.hasText(storeName)) return null;
        String s = storeName.toUpperCase();
        if (s.contains("UK") || s.contains("英国")) return "UK";
        if (s.contains("US") || s.contains("美国")) return "US";
        if (s.contains("CA") || s.contains("加拿大")) return "CA";
        if (s.contains("DE") || s.contains("德国")) return "DE";
        return null;
    }

    /**
     * 判断广告数据是否在季度范围内
     */
    private boolean isAdInQuarter(AdDataForTax ad, LocalDate startDate, LocalDate endDate) {
        LocalDate adStart = ad.billingStartDate;
        LocalDate adEnd = ad.billingEndDate;
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

    /**
     * 计算发货单总计费用（当 totalAmount 为空时回退）
     * 总计 = 商品价格 + 商品税 + 运费 + 运费税 + 礼品包装价格 + 礼品包装税 + 商品促销折扣 + 货件促销折扣
     */
    private BigDecimal calculateShippingTotalAmount(ShippingData shipping) {
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
    public void exportTaxSummary(String siteCode, String startQuarter, String endQuarter, String refundDateMode, HttpServletResponse response) {
        log.info("导出报税汇总列表: siteCode={}, startQuarter={}, endQuarter={}, refundDateMode={}", siteCode, startQuarter, endQuarter, refundDateMode);

        try {
            List<TaxReportSummary> summaries = getTaxSummary(siteCode, startQuarter, endQuarter, refundDateMode);

            String fileName = "报税汇总_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("报税汇总");

                // 表头与前端列表一致
                String[] headers = {
                        "站点", "季度",
                        "收入总额①(人民币)",
                        "退款金额②(人民币)",
                        "收入净额③=①-②(人民币)",
                        "平台代扣税④(人民币)",
                        "佣金服务费⑤(人民币)",
                        "其他费用(人民币)",
                        "广告费⑥(人民币)",
                        "平台支出合计⑨=④+⑤+⑥(人民币)",
                        "4%利润⑩=③×4%(人民币)",
                        "采购成本⑪=③-⑨-⑩(人民币)"
                };

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                Row headerRow = sheet.createRow(0);
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
                    row.createCell(col++).setCellValue(toDouble(s.getTotalRevenueCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getRefundAmountCny()));
                    // 收入净额 = 收入总额 - |退款金额|
                    double netRevenue = toDouble(s.getTotalRevenueCny()) - Math.abs(toDouble(s.getRefundAmountCny()));
                    row.createCell(col++).setCellValue(netRevenue);
                    row.createCell(col++).setCellValue(toDouble(s.getConsumptionTaxCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getTotalCommissionFeeCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getTotalOtherFeeCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getAdvertisingCostCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getPlatformExpensesCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getProfit4PercentCny()));
                    row.createCell(col++).setCellValue(toDouble(s.getProcurementCostCny()));
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("导出报税汇总失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }
}
