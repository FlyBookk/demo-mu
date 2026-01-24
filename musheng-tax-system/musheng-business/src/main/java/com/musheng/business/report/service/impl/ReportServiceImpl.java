package com.musheng.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.advertising.entity.AdvertisingData;
import com.musheng.business.advertising.mapper.AdvertisingDataMapper;
import com.musheng.business.rate.service.impl.RateServiceImpl;
import com.musheng.business.report.dto.ReportSummary;
import com.musheng.business.report.service.ReportService;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Report Service Implementation
 * Implements summary calculation for sales, shipping, and advertising data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SalesDataMapper salesDataMapper;
    private final ShippingDataMapper shippingDataMapper;
    private final AdvertisingDataMapper advertisingDataMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final RateServiceImpl rateService;

    /**
     * Site code to currency mapping
     */
    private static final Map<String, String> SITE_CURRENCY_MAP = Map.of(
            "US", "USD",
            "CA", "CAD",
            "UK", "GBP",
            "DE", "EUR"
    );

    @Override
    public List<ReportSummary> getSummary(String siteCode, String yearQuarter, String startQuarter, String endQuarter) {
        log.info("Getting summary report: siteCode={}, yearQuarter={}, startQuarter={}, endQuarter={}",
                siteCode, yearQuarter, startQuarter, endQuarter);

        // Determine quarters to process
        List<String> quarters = getQuartersInRange(startQuarter, endQuarter, yearQuarter);

        // Determine sites to process
        List<String> sites = StringUtils.hasText(siteCode) ?
                List.of(siteCode) : List.of("US", "CA", "UK", "DE");

        return calculateSummaryBatch(sites, quarters);
    }

    @Override
    public List<ReportSummary> getSummaryBySite(String yearQuarter) {
        log.info("Getting summary report by site: yearQuarter={}", yearQuarter);

        List<String> sites = List.of("US", "CA", "UK", "DE");
        List<String> quarters = List.of(yearQuarter);

        return calculateSummaryBatch(sites, quarters);
    }

    @Override
    public List<ReportSummary> getSummaryByQuarter(String siteCode, String startQuarter, String endQuarter) {
        log.info("Getting summary report by quarter: siteCode={}, startQuarter={}, endQuarter={}",
                siteCode, startQuarter, endQuarter);

        List<String> sites = List.of(siteCode);
        List<String> quarters = getQuartersInRange(startQuarter, endQuarter, null);

        return calculateSummaryBatch(sites, quarters);
    }

    /**
     * 批量计算汇总报表 - 预加载基础数据，避免 N+1 查询
     */
    private List<ReportSummary> calculateSummaryBatch(List<String> sites, List<String> quarters) {
        List<ReportSummary> results = new ArrayList<>();

        // ========== 1. 预加载 Marketplace 数据 ==========
        Map<String, Marketplace> marketplaceMap = loadMarketplaceMap(sites);

        // ========== 2. 计算所有季度的日期范围 ==========
        LocalDate minStartDate = null;
        LocalDate maxEndDate = null;
        Map<String, LocalDate[]> quarterDateRanges = new HashMap<>();

        for (String quarter : quarters) {
            int year = Integer.parseInt(quarter.substring(0, 4));
            int q = Integer.parseInt(quarter.substring(6, 7));
            LocalDate startDate = getQuarterStartDate(year, q);
            LocalDate endDate = getQuarterEndDate(year, q);
            quarterDateRanges.put(quarter, new LocalDate[]{startDate, endDate});

            if (minStartDate == null || startDate.isBefore(minStartDate)) {
                minStartDate = startDate;
            }
            if (maxEndDate == null || endDate.isAfter(maxEndDate)) {
                maxEndDate = endDate;
            }
        }

        // ========== 3. 预加载所有退款订单的发货日期 ==========
        Map<String, Map<String, LocalDate>> refundShipDateMap = loadRefundShipDates(sites);

        // ========== 4. 预加载汇率数据 ==========
        Set<String> currencies = sites.stream()
                .map(site -> SITE_CURRENCY_MAP.getOrDefault(site, "USD"))
                .collect(Collectors.toSet());
        Map<String, BigDecimal> rateCache = loadExchangeRates(currencies, maxEndDate);

        // ========== 5. 循环计算每个站点和季度的汇总 ==========
        for (String site : sites) {
            for (String quarter : quarters) {
                LocalDate[] dateRange = quarterDateRanges.get(quarter);
                ReportSummary summary = calculateSummaryWithPreloadedData(
                        site, quarter, dateRange[0], dateRange[1],
                        marketplaceMap, refundShipDateMap.getOrDefault(site, Collections.emptyMap()), rateCache);
                if (summary != null) {
                    results.add(summary);
                }
            }
        }

        return results;
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
     * 预加载所有退款订单的发货日期 - 避免 N+1 查询
     * 返回 Map<siteCode, Map<orderId, shipDate>>
     */
    private Map<String, Map<String, LocalDate>> loadRefundShipDates(List<String> sites) {
        // 1. 查询所有站点的退款订单
        LambdaQueryWrapper<SalesData> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, "refund")
                .isNotNull(SalesData::getOrderId);
        List<SalesData> allRefunds = salesDataMapper.selectList(refundWrapper);

        if (allRefunds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 收集所有退款订单的 orderId（按站点分组）
        Map<String, Set<String>> siteOrderIds = allRefunds.stream()
                .collect(Collectors.groupingBy(
                        SalesData::getSiteCode,
                        Collectors.mapping(SalesData::getOrderId, Collectors.toSet())
                ));

        // 3. 批量查询这些订单的发货日期
        Map<String, Map<String, LocalDate>> result = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : siteOrderIds.entrySet()) {
            String siteCode = entry.getKey();
            Set<String> orderIds = entry.getValue();

            if (orderIds.isEmpty()) {
                continue;
            }

            // 分批查询，避免 IN 子句过长
            List<String> orderIdList = new ArrayList<>(orderIds);
            Map<String, LocalDate> siteShipDates = new HashMap<>();

            int batchSize = 500;
            for (int i = 0; i < orderIdList.size(); i += batchSize) {
                List<String> batch = orderIdList.subList(i, Math.min(i + batchSize, orderIdList.size()));

                LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
                shippingWrapper.eq(ShippingData::getSiteCode, siteCode)
                        .in(ShippingData::getOrderId, batch);
                List<ShippingData> shippingList = shippingDataMapper.selectList(shippingWrapper);

                // 每个订单取最新的发货日期
                for (ShippingData shipping : shippingList) {
                    String orderId = shipping.getOrderId();
                    LocalDate shipDate = shipping.getShipDate();
                    if (shipDate != null) {
                        siteShipDates.merge(orderId, shipDate, (existing, newDate) ->
                                newDate.isAfter(existing) ? newDate : existing);
                    }
                }
            }

            result.put(siteCode, siteShipDates);
        }

        return result;
    }

    /**
     * 预加载汇率数据
     */
    private Map<String, BigDecimal> loadExchangeRates(Set<String> currencies, LocalDate date) {
        Map<String, BigDecimal> rateCache = new HashMap<>();
        for (String currency : currencies) {
            if ("CNY".equals(currency)) {
                rateCache.put(currency, BigDecimal.ONE);
            } else {
                try {
                    BigDecimal rate = rateService.getRate(currency, date.toString());
                    rateCache.put(currency, rate);
                } catch (Exception e) {
                    log.warn("Exchange rate not found for {} on {}, using 1:1", currency, date);
                    rateCache.put(currency, BigDecimal.ONE);
                }
            }
        }
        return rateCache;
    }

    /**
     * 使用预加载数据计算汇总（优化版本，避免 N+1 查询）
     */
    private ReportSummary calculateSummaryWithPreloadedData(
            String siteCode,
            String yearQuarter,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Marketplace> marketplaceMap,
            Map<String, LocalDate> refundShipDateMap,
            Map<String, BigDecimal> rateCache) {

        log.debug("Calculating summary for site={}, quarter={}", siteCode, yearQuarter);

        // 从预加载数据获取站点信息
        Marketplace marketplace = marketplaceMap.get(siteCode);
        String siteName = marketplace != null ? marketplace.getSiteName() : siteCode;
        String currencyCode = SITE_CURRENCY_MAP.getOrDefault(siteCode, "USD");

        // Query sales data for this quarter by transaction_date
        LambdaQueryWrapper<SalesData> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(SalesData::getSiteCode, siteCode)
                .ge(SalesData::getTransactionDate, startDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, endDate.plusDays(1).atStartOfDay());
        List<SalesData> salesList = salesDataMapper.selectList(salesWrapper);

        // Query shipping data for this quarter (for shipping cost)
        LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
        shippingWrapper.eq(ShippingData::getSiteCode, siteCode)
                .between(ShippingData::getShipDate, startDate, endDate);
        List<ShippingData> shippingList = shippingDataMapper.selectList(shippingWrapper);

        // Query refunds for this site
        LambdaQueryWrapper<SalesData> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(SalesData::getSiteCode, siteCode)
                .eq(SalesData::getTransactionCategory, "refund");
        List<SalesData> allRefunds = salesDataMapper.selectList(refundWrapper);

        // 使用预加载的发货日期数据过滤退款（无 N+1 查询）
        List<SalesData> refundsForThisQuarter = allRefunds.stream()
                .filter(refund -> {
                    String orderId = refund.getOrderId();
                    // 从预加载的 map 中获取发货日期
                    LocalDate shipDate = refundShipDateMap.get(orderId);
                    if (shipDate != null) {
                        return !shipDate.isBefore(startDate) && !shipDate.isAfter(endDate);
                    }
                    // 如果没有发货日期，回退到交易日期
                    if (refund.getTransactionDate() != null) {
                        LocalDate transactionDate = refund.getTransactionDate().toLocalDate();
                        return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                    }
                    return false;
                })
                .collect(Collectors.toList());

        // Query advertising data (by billing date range)
        LambdaQueryWrapper<AdvertisingData> adWrapper = new LambdaQueryWrapper<>();
        adWrapper.eq(AdvertisingData::getSiteCode, siteCode)
                .and(wrapper -> wrapper
                        .between(AdvertisingData::getBillingStartDate, startDate, endDate)
                        .or()
                        .between(AdvertisingData::getBillingEndDate, startDate, endDate)
                        .or()
                        .and(w -> w.le(AdvertisingData::getBillingStartDate, startDate)
                                .ge(AdvertisingData::getBillingEndDate, endDate)));
        List<AdvertisingData> adList = advertisingDataMapper.selectList(adWrapper);

        // Check if there's any data
        if (salesList.isEmpty() && shippingList.isEmpty() && adList.isEmpty() && refundsForThisQuarter.isEmpty()) {
            log.debug("No data found for site={}, quarter={}", siteCode, yearQuarter);
            return null;
        }

        // Calculate totals
        ReportSummary summary = new ReportSummary();
        summary.setSiteCode(siteCode);
        summary.setSiteName(siteName);
        summary.setYearQuarter(yearQuarter);
        summary.setCurrencyCode(currencyCode);

        // Calculate income (sales) totals
        BigDecimal totalSalesAmount = salesList.stream()
                .filter(s -> "income".equals(s.getTransactionCategory()))
                .map(SalesData::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate refund amount (attributed by shipping date)
        BigDecimal refundAmount = refundsForThisQuarter.stream()
                .map(SalesData::getTotal)
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net sales = income - refunds
        BigDecimal netSalesAmount = totalSalesAmount.subtract(refundAmount);
        summary.setTotalSalesAmount(netSalesAmount);

        // Calculate shipping costs
        BigDecimal totalShippingCost = shippingList.stream()
                .map(ShippingData::getShippingCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalShippingCost(totalShippingCost);

        // Calculate advertising costs
        BigDecimal totalAdCost = adList.stream()
                .map(AdvertisingData::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalAdvertisingCost(totalAdCost);

        // 使用预加载的汇率数据
        BigDecimal rate = rateCache.getOrDefault(currencyCode, BigDecimal.ONE);

        BigDecimal salesCny = netSalesAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingCny = totalShippingCost.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal adCny = totalAdCost.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        summary.setTotalSalesAmountEur(salesCny);
        summary.setTotalShippingCostEur(shippingCny);
        summary.setTotalAdvertisingCostEur(adCny);

        // Calculate net amount (sales - shipping - advertising)
        BigDecimal netAmount = salesCny.subtract(shippingCny).subtract(adCny);
        summary.setNetAmountEur(netAmount);

        // Calculate VAT
        BigDecimal vatRate = getVatRate(siteCode);
        BigDecimal vatAmount = netAmount.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        summary.setVatAmountEur(vatAmount);

        // Count transactions
        summary.setTransactionCount(salesList.size() + refundsForThisQuarter.size());

        log.debug("Summary calculated: site={}, quarter={}, sales={}, refunds={}, shipping={}, ads={}",
                siteCode, yearQuarter, totalSalesAmount, refundAmount, totalShippingCost, totalAdCost);

        return summary;
    }

    /**
     * Get VAT rate by site
     */
    private BigDecimal getVatRate(String siteCode) {
        return switch (siteCode) {
            case "DE" -> new BigDecimal("19");
            case "UK" -> new BigDecimal("20");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Get quarter start date
     */
    private LocalDate getQuarterStartDate(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    /**
     * Get quarter end date
     */
    private LocalDate getQuarterEndDate(int year, int quarter) {
        int month = quarter * 3;
        LocalDate firstOfNextMonth = LocalDate.of(year, month, 1).plusMonths(1);
        return firstOfNextMonth.minusDays(1);
    }

    /**
     * Get months in quarter
     */
    private List<String> getMonthsInQuarter(int year, int quarter) {
        List<String> months = new ArrayList<>();
        int startMonth = (quarter - 1) * 3 + 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 0; i < 3; i++) {
            months.add(LocalDate.of(year, startMonth + i, 1).format(formatter));
        }
        return months;
    }

    /**
     * Get list of quarters in range
     */
    private List<String> getQuartersInRange(String startQuarter, String endQuarter, String singleQuarter) {
        if (StringUtils.hasText(singleQuarter)) {
            return List.of(singleQuarter);
        }

        List<String> quarters = new ArrayList<>();
        if (!StringUtils.hasText(startQuarter) || !StringUtils.hasText(endQuarter)) {
            // Default to current quarter
            LocalDate now = LocalDate.now();
            int quarter = (now.getMonthValue() - 1) / 3 + 1;
            quarters.add(now.getYear() + "-Q" + quarter);
            return quarters;
        }

        // Parse start and end quarters
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

    @Override
    public void exportSummary(String siteCode, String yearQuarter, String startQuarter, String endQuarter, HttpServletResponse response) {
        log.info("Exporting summary report: siteCode={}, yearQuarter={}", siteCode, yearQuarter);

        try {
            // Get summary data
            List<ReportSummary> summaries = getSummary(siteCode, yearQuarter, startQuarter, endQuarter);

            // Set response headers for Excel download
            String fileName = "summary_report_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            // Create Excel workbook
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("Summary Report");

                // Create header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Site Code", "Site Name", "Year-Quarter", "Currency",
                        "Sales Amount", "Sales (EUR)", "Shipping Cost", "Shipping (EUR)",
                        "Advertising Cost", "Advertising (EUR)", "Net Amount (EUR)", "VAT (EUR)", "Transaction Count"};

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Fill data rows
                int rowNum = 1;
                for (ReportSummary summary : summaries) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(summary.getSiteCode());
                    row.createCell(1).setCellValue(summary.getSiteName());
                    row.createCell(2).setCellValue(summary.getYearQuarter());
                    row.createCell(3).setCellValue(summary.getCurrencyCode());
                    row.createCell(4).setCellValue(summary.getTotalSalesAmount() != null ? summary.getTotalSalesAmount().doubleValue() : 0);
                    row.createCell(5).setCellValue(summary.getTotalSalesAmountEur() != null ? summary.getTotalSalesAmountEur().doubleValue() : 0);
                    row.createCell(6).setCellValue(summary.getTotalShippingCost() != null ? summary.getTotalShippingCost().doubleValue() : 0);
                    row.createCell(7).setCellValue(summary.getTotalShippingCostEur() != null ? summary.getTotalShippingCostEur().doubleValue() : 0);
                    row.createCell(8).setCellValue(summary.getTotalAdvertisingCost() != null ? summary.getTotalAdvertisingCost().doubleValue() : 0);
                    row.createCell(9).setCellValue(summary.getTotalAdvertisingCostEur() != null ? summary.getTotalAdvertisingCostEur().doubleValue() : 0);
                    row.createCell(10).setCellValue(summary.getNetAmountEur() != null ? summary.getNetAmountEur().doubleValue() : 0);
                    row.createCell(11).setCellValue(summary.getVatAmountEur() != null ? summary.getVatAmountEur().doubleValue() : 0);
                    row.createCell(12).setCellValue(summary.getTransactionCount() != null ? summary.getTransactionCount() : 0);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Failed to export summary report", e);
            throw new RuntimeException("Failed to export report", e);
        }
    }

    @Override
    public void exportDetail(String siteCode, String yearQuarter, String reportType, HttpServletResponse response) {
        log.info("Exporting detailed report: siteCode={}, yearQuarter={}, reportType={}", siteCode, yearQuarter, reportType);

        try {
            // Set response headers for Excel download
            String fileName = "detail_report_" + siteCode + "_" + yearQuarter + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {

                // Export based on report type
                switch (reportType != null ? reportType : "sales") {
                    case "sales" -> exportSalesDetail(workbook, siteCode, yearQuarter);
                    case "shipping" -> exportShippingDetail(workbook, siteCode, yearQuarter);
                    case "advertising" -> exportAdvertisingDetail(workbook, siteCode, yearQuarter);
                    default -> exportSalesDetail(workbook, siteCode, yearQuarter);
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Failed to export detailed report", e);
            throw new RuntimeException("Failed to export report", e);
        }
    }

    private void exportSalesDetail(Workbook workbook, String siteCode, String yearQuarter) {
        Sheet sheet = workbook.createSheet("Sales Detail");

        // Parse quarter to get date range
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        // Query data by transaction_date range
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getSiteCode, siteCode)
                .ge(SalesData::getTransactionDate, startDate.atStartOfDay())
                .lt(SalesData::getTransactionDate, endDate.plusDays(1).atStartOfDay())
                .orderByDesc(SalesData::getTransactionDate);
        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        // Create header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Order ID", "Transaction Date", "Transaction Type", "Category", "SKU", "Description",
                "Quantity", "Product Sales", "Selling Fees", "FBA Fees", "Total", "Currency"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Fill data
        int rowNum = 1;
        for (SalesData data : dataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getOrderId());
            row.createCell(1).setCellValue(data.getTransactionDate() != null ? data.getTransactionDate().toString() : "");
            row.createCell(2).setCellValue(data.getTransactionType());
            row.createCell(3).setCellValue(data.getTransactionCategory());
            row.createCell(4).setCellValue(data.getSku());
            row.createCell(5).setCellValue(data.getDescription());
            row.createCell(6).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
            row.createCell(7).setCellValue(data.getProductSales() != null ? data.getProductSales().doubleValue() : 0);
            row.createCell(8).setCellValue(data.getSellingFees() != null ? data.getSellingFees().doubleValue() : 0);
            row.createCell(9).setCellValue(data.getFbaFees() != null ? data.getFbaFees().doubleValue() : 0);
            row.createCell(10).setCellValue(data.getTotal() != null ? data.getTotal().doubleValue() : 0);
            row.createCell(11).setCellValue(data.getCurrencyCode());
        }
    }

    private void exportShippingDetail(Workbook workbook, String siteCode, String yearQuarter) {
        Sheet sheet = workbook.createSheet("Shipping Detail");

        // Parse quarter
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        // Query data
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingData::getSiteCode, siteCode)
                .between(ShippingData::getShipDate, startDate, endDate)
                .orderByDesc(ShippingData::getShipDate);
        List<ShippingData> dataList = shippingDataMapper.selectList(wrapper);

        // Create header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Order ID", "Ship Date", "Tracking Number", "Carrier", "SKU", "Quantity",
                "Product Price", "Shipping Price", "Shipping Cost", "Revenue Total", "Currency"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Fill data
        int rowNum = 1;
        for (ShippingData data : dataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getOrderId());
            row.createCell(1).setCellValue(data.getShipDate() != null ? data.getShipDate().toString() : "");
            row.createCell(2).setCellValue(data.getTrackingNumber());
            row.createCell(3).setCellValue(data.getCarrier());
            row.createCell(4).setCellValue(data.getSku());
            row.createCell(5).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
            row.createCell(6).setCellValue(data.getProductPrice() != null ? data.getProductPrice().doubleValue() : 0);
            row.createCell(7).setCellValue(data.getShippingPrice() != null ? data.getShippingPrice().doubleValue() : 0);
            row.createCell(8).setCellValue(data.getShippingCost() != null ? data.getShippingCost().doubleValue() : 0);
            row.createCell(9).setCellValue(data.getRevenueTotal() != null ? data.getRevenueTotal().doubleValue() : 0);
            row.createCell(10).setCellValue(data.getCurrencyCode());
        }
    }

    private void exportAdvertisingDetail(Workbook workbook, String siteCode, String yearQuarter) {
        Sheet sheet = workbook.createSheet("Advertising Detail");

        // Parse quarter to get date range
        int year = Integer.parseInt(yearQuarter.substring(0, 4));
        int quarter = Integer.parseInt(yearQuarter.substring(6, 7));
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);

        // Query data by billing date range
        LambdaQueryWrapper<AdvertisingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdvertisingData::getSiteCode, siteCode)
                .and(w -> w
                        .between(AdvertisingData::getBillingStartDate, startDate, endDate)
                        .or()
                        .between(AdvertisingData::getBillingEndDate, startDate, endDate)
                        .or()
                        .and(inner -> inner.le(AdvertisingData::getBillingStartDate, startDate)
                                .ge(AdvertisingData::getBillingEndDate, endDate)))
                .orderByDesc(AdvertisingData::getBillingStartDate);
        List<AdvertisingData> dataList = advertisingDataMapper.selectList(wrapper);

        // Create header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Store Name", "Invoice Number", "Billing Period", "Cost", "Currency", "Cost (CNY)", "Exchange Rate", "Campaign", "Clicks", "Avg CPC", "Remark"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Fill data
        int rowNum = 1;
        for (AdvertisingData data : dataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getStoreName());
            row.createCell(1).setCellValue(data.getInvoiceNumber());
            row.createCell(2).setCellValue((data.getBillingStartDate() != null ? data.getBillingStartDate().toString() : "") +
                                          " ~ " +
                                          (data.getBillingEndDate() != null ? data.getBillingEndDate().toString() : ""));
            row.createCell(3).setCellValue(data.getCost() != null ? data.getCost().doubleValue() : 0);
            row.createCell(4).setCellValue(data.getCurrency());
            row.createCell(5).setCellValue(data.getAmountCny() != null ? data.getAmountCny().doubleValue() : 0);
            row.createCell(6).setCellValue(data.getExchangeRate() != null ? data.getExchangeRate().doubleValue() : 0);
            row.createCell(7).setCellValue(data.getCampaignName());
            row.createCell(8).setCellValue(data.getClicks() != null ? data.getClicks() : 0);
            row.createCell(9).setCellValue(data.getAvgCpc() != null ? data.getAvgCpc().doubleValue() : 0);
            row.createCell(10).setCellValue(data.getRemark());
        }
    }
}
