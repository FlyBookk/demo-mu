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
    public DashboardData getDashboardData() {
        log.info("Getting dashboard data");

        // 验证店铺已选择（getTaxSummary 内部会使用）
        ShopContext.requireShopId();

        // 计算最近4个季度范围
        LocalDate now = LocalDate.now();
        int currentQ = (now.getMonthValue() - 1) / 3 + 1;
        String currentQuarter = now.getYear() + "-Q" + currentQ;
        String oldestQuarter = currentQuarter;
        for (int i = 0; i < 3; i++) {
            oldestQuarter = getPreviousQuarter(oldestQuarter);
        }

        // 一次性查询所有4个季度的汇总数据
        List<TaxReportSummary> allData = getTaxSummary(null, oldestQuarter, currentQuarter);

        // 按季度分组
        Map<String, List<TaxReportSummary>> dataByQuarter = allData.stream()
                .collect(Collectors.groupingBy(TaxReportSummary::getYearQuarter));

        // 获取当前和上季度数据
        String previousQuarter = getPreviousQuarter(currentQuarter);
        List<TaxReportSummary> currentData = dataByQuarter.getOrDefault(currentQuarter, Collections.emptyList());
        List<TaxReportSummary> previousData = dataByQuarter.getOrDefault(previousQuarter, Collections.emptyList());

        // 汇总当前季度
        BigDecimal totalRevenue = sumField(currentData, TaxReportSummary::getTotalRevenueCny);
        BigDecimal totalRefund = sumField(currentData, TaxReportSummary::getRefundByShipmentCny);
        BigDecimal totalNetIncome = sumField(currentData, TaxReportSummary::getNetIncomeByShipment);
        int totalOrders = currentData.stream()
                .map(TaxReportSummary::getShippingOrderCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        // 汇总上季度（用于环比）
        BigDecimal prevRevenue = sumField(previousData, TaxReportSummary::getTotalRevenueCny);
        BigDecimal prevRefund = sumField(previousData, TaxReportSummary::getRefundByShipmentCny);
        BigDecimal prevNetIncome = sumField(previousData, TaxReportSummary::getNetIncomeByShipment);

        // 构建结果
        DashboardData dashboard = new DashboardData();
        dashboard.setCurrentQuarter(currentQuarter);
        dashboard.setTotalRevenueCny(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        dashboard.setRefundCny(totalRefund.setScale(2, RoundingMode.HALF_UP));
        dashboard.setNetIncomeCny(totalNetIncome.setScale(2, RoundingMode.HALF_UP));
        dashboard.setShippingOrderCount(totalOrders);

        // 计算环比增长率
        dashboard.setRevenueGrowthRate(calculateGrowthRate(totalRevenue, prevRevenue));
        dashboard.setRefundGrowthRate(calculateGrowthRate(totalRefund, prevRefund));
        dashboard.setNetIncomeGrowthRate(calculateGrowthRate(totalNetIncome, prevNetIncome));

        // 各站点数据（当前季度）
        List<DashboardData.SiteRevenue> siteRevenues = currentData.stream()
                .collect(Collectors.groupingBy(TaxReportSummary::getSiteCode))
                .entrySet().stream()
                .map(entry -> {
                    DashboardData.SiteRevenue sr = new DashboardData.SiteRevenue();
                    sr.setSiteCode(entry.getKey());
                    sr.setSiteName(entry.getValue().get(0).getSiteName());
                    sr.setRevenue(sumField(entry.getValue(), TaxReportSummary::getTotalRevenueCny));
                    sr.setRefund(sumField(entry.getValue(), TaxReportSummary::getRefundByShipmentCny));
                    sr.setNetIncome(sumField(entry.getValue(), TaxReportSummary::getNetIncomeByShipment));
                    return sr;
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .collect(Collectors.toList());
        dashboard.setSiteRevenues(siteRevenues);

        // 季度趋势（从dataByQuarter直接计算，无需再次查询）
        List<DashboardData.QuarterTrend> trends = new ArrayList<>();
        String q = oldestQuarter;
        for (int i = 0; i < 4; i++) {
            List<TaxReportSummary> qData = dataByQuarter.getOrDefault(q, Collections.emptyList());
            DashboardData.QuarterTrend trend = new DashboardData.QuarterTrend();
            trend.setQuarter(q);
            trend.setRevenue(sumField(qData, TaxReportSummary::getTotalRevenueCny));
            trend.setRefund(sumField(qData, TaxReportSummary::getRefundByShipmentCny));
            trend.setNetIncome(sumField(qData, TaxReportSummary::getNetIncomeByShipment));
            trends.add(trend);
            // 下一个季度
            q = getNextQuarter(q);
        }
        dashboard.setQuarterTrends(trends);

        return dashboard;
    }

    /**
     * 汇总字段
     */
    private BigDecimal sumField(List<TaxReportSummary> list, java.util.function.Function<TaxReportSummary, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

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

        // ========== 批量加载所有数据（减少数据库查询次数） ==========

        // 1. 加载基础数据
        Map<String, Marketplace> marketplaceMap = loadMarketplaceMap(sites);
        Map<String, Map<String, LocalDate>> refundShipDateMap = loadRefundShipDates(sites, shopId);

        // 2. 批量查询发货数据
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getShopId, shopId)
                .in(ShippingData::getSiteCode, sites)
                .between(ShippingData::getShipDate, minStartDate, maxEndDate);
        List<ShippingData> allShippingData = shippingDataMapper.selectList(shippingWrapper);

        // 3. 批量查询退款数据（按站点，不按日期，因为要按发货归属计算）
        LambdaQueryWrapper<SalesData> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, "refund");
        List<SalesData> allRefundData = salesDataMapper.selectList(refundWrapper);

        // 4. 批量查询费用数据
        LambdaQueryWrapper<SalesData> feeWrapper = new LambdaQueryWrapper<>();
        feeWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getSiteCode, sites)
                .in(SalesData::getTransactionCategory, List.of("fee", "adjustment", "other"))
                .ge(SalesData::getTransactionDate, minStartDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, maxEndDate.plusDays(1).atStartOfDay());
        List<SalesData> allFeeData = salesDataMapper.selectList(feeWrapper);

        // 5. 批量查询广告数据
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

        log.debug("Batch loaded: shipping={}, refunds={}, fees={}, ads={}",
                allShippingData.size(), allRefundData.size(), allFeeData.size(), allAdData.size());

        // ========== 按站点分组 ==========
        Map<String, List<ShippingData>> shippingBySite = allShippingData.stream()
                .collect(Collectors.groupingBy(ShippingData::getSiteCode));
        Map<String, List<SalesData>> refundBySite = allRefundData.stream()
                .collect(Collectors.groupingBy(SalesData::getSiteCode));
        Map<String, List<SalesData>> feeBySite = allFeeData.stream()
                .collect(Collectors.groupingBy(SalesData::getSiteCode));
        Map<String, List<AdvertisingData>> adBySite = allAdData.stream()
                .collect(Collectors.groupingBy(AdvertisingData::getSiteCode));

        // ========== 在内存中计算每个站点+季度的汇总 ==========
        List<TaxReportSummary> results = new ArrayList<>();

        for (String site : sites) {
            List<ShippingData> siteShipping = shippingBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteRefunds = refundBySite.getOrDefault(site, Collections.emptyList());
            List<SalesData> siteFees = feeBySite.getOrDefault(site, Collections.emptyList());
            List<AdvertisingData> siteAds = adBySite.getOrDefault(site, Collections.emptyList());
            Map<String, LocalDate> siteRefundShipDates = refundShipDateMap.getOrDefault(site, Collections.emptyMap());

            for (String quarterStr : quarters) {
                TaxReportSummary summary = calculateTaxSummaryFromMemory(
                        site, quarterStr, marketplaceMap.get(site),
                        siteShipping, siteRefunds, siteFees, siteAds, siteRefundShipDates
                );
                if (summary != null) {
                    results.add(summary);
                }
            }
        }

        return results;
    }

    /**
     * 从内存数据计算单个站点+季度的报税汇总（无数据库查询）
     */
    private TaxReportSummary calculateTaxSummaryFromMemory(
            String siteCode,
            String yearQuarter,
            Marketplace marketplace,
            List<ShippingData> allShipping,
            List<SalesData> allRefunds,
            List<SalesData> allFees,
            List<AdvertisingData> allAds,
            Map<String, LocalDate> refundShipDateMap) {

        // 解析季度日期范围
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        String siteName = marketplace != null ? marketplace.getSiteName() : siteCode;
        String currencyCode = SITE_CURRENCY_MAP.getOrDefault(siteCode, "USD");

        // ========== 1. 收入计算（筛选本季度发货数据） ==========
        List<ShippingData> shippingList = allShipping.stream()
                .filter(s -> s.getShipDate() != null
                        && !s.getShipDate().isBefore(startDate)
                        && !s.getShipDate().isAfter(endDate))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenueCny = BigDecimal.ZERO;
        Set<String> shippingOrderIds = new HashSet<>();

        for (ShippingData shipping : shippingList) {
            BigDecimal revenue = shipping.getRevenueTotal();
            if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
                revenue = calculateShippingRevenue(shipping);
            }
            BigDecimal rate = shipping.getExchangeRate();

            if (revenue != null && revenue.compareTo(BigDecimal.ZERO) != 0) {
                totalRevenue = totalRevenue.add(revenue);
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    totalRevenueCny = totalRevenueCny.add(revenue.multiply(rate));
                } else {
                    totalRevenueCny = totalRevenueCny.add(revenue);
                }
            }
            if (shipping.getOrderId() != null) {
                shippingOrderIds.add(shipping.getOrderId());
            }
        }

        // ========== 2. 退款计算（双维度） ==========
        BigDecimal refundBySettlement = BigDecimal.ZERO;
        BigDecimal refundBySettlementCny = BigDecimal.ZERO;
        int refundCountBySettlement = 0;

        BigDecimal refundByShipment = BigDecimal.ZERO;
        BigDecimal refundByShipmentCny = BigDecimal.ZERO;
        int refundCountByShipment = 0;

        for (SalesData refund : allRefunds) {
            BigDecimal refundTotal = refund.getTotal();
            if (refundTotal == null) continue;

            BigDecimal absAmount = refundTotal.abs();
            BigDecimal rate = refund.getExchangeRate();
            BigDecimal amountCny = (rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                    ? absAmount.multiply(rate) : absAmount;

            // 维度一：按结算时间
            if (refund.getTransactionDate() != null) {
                LocalDate transDate = refund.getTransactionDate().toLocalDate();
                if (!transDate.isBefore(startDate) && !transDate.isAfter(endDate)) {
                    refundBySettlement = refundBySettlement.add(absAmount);
                    refundBySettlementCny = refundBySettlementCny.add(amountCny);
                    refundCountBySettlement++;
                }
            }

            // 维度二：按发货归属
            String orderId = refund.getOrderId();
            LocalDate shipDate = refundShipDateMap.get(orderId);
            if (shipDate != null && !shipDate.isBefore(startDate) && !shipDate.isAfter(endDate)) {
                refundByShipment = refundByShipment.add(absAmount);
                refundByShipmentCny = refundByShipmentCny.add(amountCny);
                refundCountByShipment++;
            }
        }

        // ========== 3. 费用计算（筛选本季度） ==========
        List<SalesData> feeList = allFees.stream()
                .filter(f -> f.getTransactionDate() != null) 
                .filter(f -> {
                    LocalDate d = f.getTransactionDate().toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.toList());

        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal totalServiceFeeCny = BigDecimal.ZERO;

        for (SalesData fee : feeList) {
            BigDecimal feeAmount = sumFeeFields(fee);
            BigDecimal rate = fee.getExchangeRate();
            BigDecimal feeCny = (rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                    ? feeAmount.multiply(rate) : feeAmount;

            totalServiceFee = totalServiceFee.add(feeAmount.abs());
            totalServiceFeeCny = totalServiceFeeCny.add(feeCny.abs());
        }

        // ========== 4. 广告费计算（筛选本季度） ==========
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

        // ========== 5. 构建汇总结果 ==========
        BigDecimal netIncomeBySettlement = totalRevenueCny.subtract(refundBySettlementCny)
                .subtract(totalServiceFeeCny).subtract(advertisingCostCny);
        BigDecimal netIncomeByShipment = totalRevenueCny.subtract(refundByShipmentCny)
                .subtract(totalServiceFeeCny).subtract(advertisingCostCny);

        BigDecimal purchaseAmount = totalRevenueCny.multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = purchaseAmount.add(totalServiceFeeCny).add(advertisingCostCny);

        TaxReportSummary summary = new TaxReportSummary();
        summary.setSiteCode(siteCode);
        summary.setSiteName(siteName);
        summary.setYearQuarter(yearQuarter);
        summary.setCurrencyCode(currencyCode);

        summary.setShippingOrderCount(shippingOrderIds.size());
        summary.setTotalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalRevenueCny(totalRevenueCny.setScale(2, RoundingMode.HALF_UP));

        summary.setRefundBySettlement(refundBySettlement.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundBySettlementCny(refundBySettlementCny.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundCountBySettlement(refundCountBySettlement);

        summary.setRefundByShipment(refundByShipment.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundByShipmentCny(refundByShipmentCny.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundCountByShipment(refundCountByShipment);

        summary.setNetIncomeBySettlement(netIncomeBySettlement.setScale(2, RoundingMode.HALF_UP));
        summary.setNetIncomeByShipment(netIncomeByShipment.setScale(2, RoundingMode.HALF_UP));

        summary.setTotalServiceFee(totalServiceFee.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalServiceFeeCny(totalServiceFeeCny.setScale(2, RoundingMode.HALF_UP));

        summary.setAdvertisingCost(advertisingCost.setScale(2, RoundingMode.HALF_UP));
        summary.setAdvertisingCostCny(advertisingCostCny.setScale(2, RoundingMode.HALF_UP));

        summary.setPurchaseAmount(purchaseAmount);
        summary.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));

        return summary;
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
     * 预加载退款订单的发货日期
     */
    private Map<String, Map<String, LocalDate>> loadRefundShipDates(List<String> sites, Long shopId) {
        // 查询所有站点的退款订单
        LambdaQueryWrapper<SalesData> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(SalesData::getShopId, shopId)  // 店铺数据隔离
                .in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, "refund")
                .isNotNull(SalesData::getOrderId);
        List<SalesData> allRefunds = salesDataMapper.selectList(refundWrapper);

        if (allRefunds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 收集所有退款订单的 orderId（按站点分组）
        Map<String, Set<String>> siteOrderIds = allRefunds.stream()
                .collect(Collectors.groupingBy(
                        SalesData::getSiteCode,
                        Collectors.mapping(SalesData::getOrderId, Collectors.toSet())
                ));

        // 批量查询发货日期
        Map<String, Map<String, LocalDate>> result = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : siteOrderIds.entrySet()) {
            String site = entry.getKey();
            Set<String> orderIds = entry.getValue();

            if (orderIds.isEmpty()) continue;

            List<String> orderIdList = new ArrayList<>(orderIds);
            Map<String, LocalDate> siteShipDates = new HashMap<>();

            int batchSize = 500;
            for (int i = 0; i < orderIdList.size(); i += batchSize) {
                List<String> batch = orderIdList.subList(i, Math.min(i + batchSize, orderIdList.size()));

                LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
                shippingWrapper.eq(ShippingData::getShopId, shopId)  // 店铺数据隔离
                        .eq(ShippingData::getSiteCode, site)
                        .in(ShippingData::getOrderId, batch);
                List<ShippingData> shippingList = shippingDataMapper.selectList(shippingWrapper);

                for (ShippingData shipping : shippingList) {
                    String orderId = shipping.getOrderId();
                    LocalDate shipDate = shipping.getShipDate();
                    if (shipDate != null) {
                        siteShipDates.merge(orderId, shipDate, (existing, newDate) ->
                                newDate.isAfter(existing) ? newDate : existing);
                    }
                }
            }

            result.put(site, siteShipDates);
        }

        return result;
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

                // 表头
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "站点", "季度", "币种",
                        "收入总额(原币)", "收入总额(人民币)", "发货订单数",
                        "退款-结算(原币)", "退款-结算(人民币)", "退款笔数-结算",
                        "退款-发货(原币)", "退款-发货(人民币)", "退款笔数-发货",
                        "净收入-结算", "净收入-发货",
                        "服务费(原币)", "服务费(人民币)",
                        "广告费(原币)", "广告费(人民币)",
                        "采购金额", "总成本"
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
                    row.createCell(0).setCellValue(s.getSiteName());
                    row.createCell(1).setCellValue(s.getYearQuarter());
                    row.createCell(2).setCellValue(s.getCurrencyCode());
                    row.createCell(3).setCellValue(toDouble(s.getTotalRevenue()));
                    row.createCell(4).setCellValue(toDouble(s.getTotalRevenueCny()));
                    row.createCell(5).setCellValue(s.getShippingOrderCount() != null ? s.getShippingOrderCount() : 0);
                    row.createCell(6).setCellValue(toDouble(s.getRefundBySettlement()));
                    row.createCell(7).setCellValue(toDouble(s.getRefundBySettlementCny()));
                    row.createCell(8).setCellValue(s.getRefundCountBySettlement() != null ? s.getRefundCountBySettlement() : 0);
                    row.createCell(9).setCellValue(toDouble(s.getRefundByShipment()));
                    row.createCell(10).setCellValue(toDouble(s.getRefundByShipmentCny()));
                    row.createCell(11).setCellValue(s.getRefundCountByShipment() != null ? s.getRefundCountByShipment() : 0);
                    row.createCell(12).setCellValue(toDouble(s.getNetIncomeBySettlement()));
                    row.createCell(13).setCellValue(toDouble(s.getNetIncomeByShipment()));
                    row.createCell(14).setCellValue(toDouble(s.getTotalServiceFee()));
                    row.createCell(15).setCellValue(toDouble(s.getTotalServiceFeeCny()));
                    row.createCell(16).setCellValue(toDouble(s.getAdvertisingCost()));
                    row.createCell(17).setCellValue(toDouble(s.getAdvertisingCostCny()));
                    row.createCell(18).setCellValue(toDouble(s.getPurchaseAmount()));
                    row.createCell(19).setCellValue(toDouble(s.getTotalCost()));
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
