package com.musheng.business.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.report.dto.TaxReportDetailExportRow;
import com.musheng.business.report.dto.TaxReportDetailExportRowNoCny;
import com.musheng.business.report.service.TaxReportDetailExportService;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 报税统计明细导出服务实现
 * 使用 EasyExcel 增量输出，避免内存溢出
 * 数据量超过阈值时降级为 CSV+ZIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxReportDetailExportServiceImpl implements TaxReportDetailExportService {

    private final SalesDataMapper salesDataMapper;
    private final ShippingDataMapper shippingDataMapper;

    /** 数据量阈值：超过则用 CSV+ZIP 替代 Excel */
    private static final int EXCEL_ROW_THRESHOLD = 100_000;

    private static final int BATCH_SIZE = 2000;

    private static final List<String> SITES = List.of("US", "CA", "UK", "DE");

    @Override
    public void exportTaxSummaryDetail(String siteCode, String startQuarter, String endQuarter, HttpServletResponse response) {
        Long shopId = ShopContext.requireShopId();
        List<String> sites = StringUtils.hasText(siteCode) ? List.of(siteCode) : SITES;

        LocalDate minStartDate = null;
        LocalDate maxEndDate = null;
        for (String q : getQuartersInRange(startQuarter, endQuarter)) {
            int year = Integer.parseInt(q.substring(0, 4));
            int quarter = Integer.parseInt(q.substring(6, 7));
            LocalDate start = getQuarterStartDate(year, quarter);
            LocalDate end = getQuarterEndDate(year, quarter);
            if (minStartDate == null || start.isBefore(minStartDate)) minStartDate = start;
            if (maxEndDate == null || end.isAfter(maxEndDate)) maxEndDate = end;
        }

        if (minStartDate == null || maxEndDate == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "季度范围无效");
        }

        LocalDateTime extendedStart = minStartDate.minusMonths(1).atStartOfDay();
        LocalDateTime extendedEnd = maxEndDate.plusMonths(2).plusDays(1).atStartOfDay();
        LocalDateTime otherEnd = maxEndDate.plusDays(1).atStartOfDay();
        /** ServiceFee/其它数据与报表一致：按季度范围，不使用扩展的 extendedStart */
        LocalDateTime quarterStart = minStartDate.atStartOfDay();

        // 1. 预统计总行数
        long totalRows = countRows(shopId, sites, extendedStart, extendedEnd, otherEnd);
        log.info("Tax summary detail export: totalRows={}, threshold={}", totalRows, EXCEL_ROW_THRESHOLD);

        try {
            if (totalRows > EXCEL_ROW_THRESHOLD) {
                exportAsCsvZip(shopId, sites, extendedStart, extendedEnd, otherEnd, quarterStart, response);
            } else {
                exportAsExcel(shopId, sites, extendedStart, extendedEnd, otherEnd, quarterStart, response);
            }
        } catch (IOException e) {
            log.error("Export tax summary detail failed", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    private long countRows(Long shopId, List<String> sites,
                          LocalDateTime incomeRefundStart, LocalDateTime incomeRefundEnd, LocalDateTime otherEnd) {
        // income + refund
        LambdaQueryWrapper<SalesData> w1 = new LambdaQueryWrapper<>();
        w1.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .in(SalesData::getTransactionCategory, List.of("income", "refund"))
                .ge(SalesData::getTransactionDate, incomeRefundStart).lt(SalesData::getTransactionDate, incomeRefundEnd);
        long c1 = salesDataMapper.selectCount(w1);

        // fee + adjustment + other
        LambdaQueryWrapper<SalesData> w2 = new LambdaQueryWrapper<>();
        w2.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .notIn(SalesData::getTransactionCategory, List.of("income", "refund"))
                .ge(SalesData::getTransactionDate, incomeRefundStart).lt(SalesData::getTransactionDate, otherEnd);
        long c2 = salesDataMapper.selectCount(w2);

        return c1 + c2;
    }

    private void exportAsExcel(Long shopId, List<String> sites,
                               LocalDateTime extendedStart, LocalDateTime extendedEnd, LocalDateTime otherEnd,
                               LocalDateTime quarterStart, HttpServletResponse response) throws IOException {
        String fileName = "tax_summary_detail_" + System.currentTimeMillis() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), TaxReportDetailExportRowNoCny.class)
                .autoCloseStream(Boolean.FALSE).build()) {

            WriteSheet sheet1 = EasyExcel.writerSheet("收入数据").build();
            WriteSheet sheet2 = EasyExcel.writerSheet("退款数据").build();
            WriteSheet sheet3 = EasyExcel.writerSheet("ServiceFee").head(TaxReportDetailExportRow.class).build();
            WriteSheet sheet4 = EasyExcel.writerSheet("其它数据").head(TaxReportDetailExportRow.class).build();

            writeSheetIncremental(excelWriter, sheet1, shopId, sites, "income", extendedStart, extendedEnd);
            writeSheetIncremental(excelWriter, sheet2, shopId, sites, "refund", extendedStart, extendedEnd);
            writePageByPage(excelWriter, sheet3, buildServiceFeeWrapper(shopId, sites, quarterStart, otherEnd), true, true);
            writePageByPage(excelWriter, sheet4, buildOtherWrapper(shopId, sites, quarterStart, otherEnd), true, true);
        }
        response.getOutputStream().flush();
    }

    private void writeSheetIncremental(ExcelWriter excelWriter, WriteSheet sheet, Long shopId, List<String> sites,
                                       String category, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SalesData> w = new LambdaQueryWrapper<>();
        w.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .eq(SalesData::getTransactionCategory, category)
                .ge(SalesData::getTransactionDate, start).lt(SalesData::getTransactionDate, end)
                .orderByAsc(SalesData::getId);
        writePageByPage(excelWriter, sheet, w, false, false);
    }

    private void writeSheetIncremental(ExcelWriter excelWriter, WriteSheet sheet, Long shopId, List<String> sites,
                                       List<String> categories, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SalesData> w = new LambdaQueryWrapper<>();
        w.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .in(SalesData::getTransactionCategory, categories)
                .ge(SalesData::getTransactionDate, start).lt(SalesData::getTransactionDate, end)
                .orderByAsc(SalesData::getId);
        writePageByPage(excelWriter, sheet, w, false, false);
    }

    private void writePageByPage(ExcelWriter excelWriter, WriteSheet sheet, LambdaQueryWrapper<SalesData> wrapper,
                                 boolean useSalesRateOnly, boolean includeCny) {
        long page = 1;
        List<SalesData> batch;
        do {
            Page<SalesData> p = new Page<>(page, BATCH_SIZE);
            batch = salesDataMapper.selectPage(p, wrapper).getRecords();
            if (!batch.isEmpty()) {
                if (includeCny) {
                    excelWriter.write(toExportRows(batch, useSalesRateOnly), sheet);
                } else {
                    excelWriter.write(toExportRowsNoCny(batch, useSalesRateOnly), sheet);
                }
            }
            page++;
        } while (batch.size() == BATCH_SIZE);
    }

    private void exportAsCsvZip(Long shopId, List<String> sites,
                                LocalDateTime extendedStart, LocalDateTime extendedEnd, LocalDateTime otherEnd,
                                LocalDateTime quarterStart, HttpServletResponse response) throws IOException {
        String zipName = "tax_summary_detail_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipName, StandardCharsets.UTF_8));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            writeCsvToZip(zos, "收入数据.csv", shopId, sites, "income", extendedStart, extendedEnd, false);
            writeCsvToZip(zos, "退款数据.csv", shopId, sites, "refund", extendedStart, extendedEnd, false);
            writeCsvToZip(zos, "ServiceFee.csv", buildServiceFeeWrapper(shopId, sites, quarterStart, otherEnd), true, true);
            writeCsvToZip(zos, "其它数据.csv", buildOtherWrapper(shopId, sites, quarterStart, otherEnd), true, true);
        }
        response.getOutputStream().flush();
    }

    private void writeCsvToZip(ZipOutputStream zos, String entryName, Long shopId, List<String> sites,
                               String category, LocalDateTime start, LocalDateTime end, boolean includeCny) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try {
            LambdaQueryWrapper<SalesData> w = new LambdaQueryWrapper<>();
            w.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                    .eq(SalesData::getTransactionCategory, category)
                    .ge(SalesData::getTransactionDate, start).lt(SalesData::getTransactionDate, end)
                    .orderByAsc(SalesData::getId);
            writeCsvIncremental(zos, w, false, includeCny);
        } finally {
            zos.closeEntry();
        }
    }

    private void writeCsvToZip(ZipOutputStream zos, String entryName, LambdaQueryWrapper<SalesData> wrapper,
                               boolean useSalesRateOnly, boolean includeCny) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try {
            writeCsvIncremental(zos, wrapper, useSalesRateOnly, includeCny);
        } finally {
            zos.closeEntry();
        }
    }

    /** ServiceFee：transaction_type = 'ServiceFee' 的销售数据（与报表一致，仅费用类数据） */
    private LambdaQueryWrapper<SalesData> buildServiceFeeWrapper(Long shopId, List<String> sites,
                                                                 LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SalesData> w = new LambdaQueryWrapper<>();
        w.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .notIn(SalesData::getTransactionCategory, List.of("income", "refund"))
                .eq(SalesData::getTransactionType, "ServiceFee")
                .ge(SalesData::getTransactionDate, start).lt(SalesData::getTransactionDate, end)
                .orderByAsc(SalesData::getId);
        return w;
    }

    /** 其它数据：transaction_type NOT IN (Refund, Shipment, ServiceFee) 的销售数据（与报表一致，仅费用类数据） */
    private LambdaQueryWrapper<SalesData> buildOtherWrapper(Long shopId, List<String> sites,
                                                            LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SalesData> w = new LambdaQueryWrapper<>();
        w.eq(SalesData::getShopId, shopId).in(SalesData::getSiteCode, sites)
                .notIn(SalesData::getTransactionCategory, List.of("income", "refund"))
                .and(w2 -> w2.notIn(SalesData::getTransactionType, List.of("Refund", "Shipment", "ServiceFee"))
                        .or().isNull(SalesData::getTransactionType))
                .ge(SalesData::getTransactionDate, start).lt(SalesData::getTransactionDate, end)
                .orderByAsc(SalesData::getId);
        return w;
    }

    private void writeCsvIncremental(ZipOutputStream zos, LambdaQueryWrapper<SalesData> wrapper,
                                     boolean useSalesRateOnly, boolean includeCny) throws IOException {
        zos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM
        String headerBase = "数据源类型,店铺名称,交易日期,结算ID,ERP结算编号,交易类型,交易分类,订单号,SKU,描述,数量,站点,站点域名,货币,配送方式,"
                + "产品销售额,产品税,运费收入,运费税,礼品包装收入,礼品包装税,监管费,监管费税,促销折扣,促销折扣税,平台代扣税,"
                + "销售费用,FBA费用,其他交易费,其他,合计,汇率,配送日期";
        String header = includeCny ? headerBase + ",人民币金额\n" : headerBase + "\n";
        zos.write(header.getBytes(StandardCharsets.UTF_8));

        Long shopId = ShopContext.requireShopId();
        long page = 1;
        List<SalesData> batch;
        do {
            Page<SalesData> p = new Page<>(page, BATCH_SIZE);
            batch = salesDataMapper.selectPage(p, wrapper).getRecords();
            Map<String, ShippingInfo> shippingInfoMap = useSalesRateOnly ? Map.of() : buildShippingInfoMap(shopId, batch);
            for (SalesData d : batch) {
                zos.write(toCsvLine(d, shippingInfoMap, includeCny).getBytes(StandardCharsets.UTF_8));
            }
            page++;
        } while (batch.size() == BATCH_SIZE);
    }

    private String toCsvLine(SalesData d, Map<String, ShippingInfo> shippingInfoMap, boolean includeCny) {
        BigDecimal rate = resolveExchangeRate(d, shippingInfoMap);
        LocalDate shipDate = resolveShipDate(d, shippingInfoMap);
        BigDecimal totalCny = null;
        if (includeCny) {
            BigDecimal total = d.getTotal();
            totalCny = (total != null && rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                    ? total.multiply(rate).setScale(2, RoundingMode.HALF_UP) : null;
        }

        return escapeCsv(d.getSourceType()) + "," + escapeCsv(d.getStoreName()) + ","
                + (d.getTransactionDate() != null ? d.getTransactionDate().toString() : "") + ","
                + escapeCsv(d.getSettlementId()) + "," + escapeCsv(d.getErpSettlementId()) + ","
                + escapeCsv(d.getTransactionType()) + "," + escapeCsv(d.getTransactionCategory()) + ","
                + escapeCsv(d.getOrderId()) + "," + escapeCsv(d.getSku()) + "," + escapeCsv(d.getDescription()) + ","
                + (d.getQuantity() != null ? d.getQuantity() : "") + ","
                + escapeCsv(d.getSiteCode()) + "," + escapeCsv(d.getMarketplace()) + ","
                + escapeCsv(d.getCurrencyCode()) + "," + escapeCsv(d.getFulfillment()) + ","
                + (d.getProductSales() != null ? d.getProductSales() : "") + ","
                + (d.getProductSalesTax() != null ? d.getProductSalesTax() : "") + ","
                + (d.getShippingCredits() != null ? d.getShippingCredits() : "") + ","
                + (d.getShippingCreditsTax() != null ? d.getShippingCreditsTax() : "") + ","
                + (d.getGiftWrapCredits() != null ? d.getGiftWrapCredits() : "") + ","
                + (d.getGiftWrapCreditsTax() != null ? d.getGiftWrapCreditsTax() : "") + ","
                + (d.getRegulatoryFee() != null ? d.getRegulatoryFee() : "") + ","
                + (d.getRegulatoryFeeTax() != null ? d.getRegulatoryFeeTax() : "") + ","
                + (d.getPromotionalRebates() != null ? d.getPromotionalRebates() : "") + ","
                + (d.getPromotionalRebatesTax() != null ? d.getPromotionalRebatesTax() : "") + ","
                + (d.getMarketplaceWithheldTax() != null ? d.getMarketplaceWithheldTax() : "") + ","
                + (d.getSellingFees() != null ? d.getSellingFees() : "") + ","
                + (d.getFbaFees() != null ? d.getFbaFees() : "") + ","
                + (d.getOtherTransactionFees() != null ? d.getOtherTransactionFees() : "") + ","
                + (d.getOther() != null ? d.getOther() : "") + ","
                + (d.getTotal() != null ? d.getTotal() : "") + ","
                + (rate != null ? rate : "") + ","
                + (shipDate != null ? shipDate.toString() : "")
                + (includeCny ? "," + (totalCny != null ? totalCny : "") : "") + "\n";
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private List<TaxReportDetailExportRow> toExportRows(List<SalesData> list, boolean useSalesRateOnly) {
        Long shopId = ShopContext.requireShopId();
        Map<String, ShippingInfo> shippingInfoMap = useSalesRateOnly ? Map.of() : buildShippingInfoMap(shopId, list);

        List<TaxReportDetailExportRow> rows = new ArrayList<>(list.size());
        for (SalesData d : list) {
            BigDecimal rate = resolveExchangeRate(d, shippingInfoMap);
            LocalDate shipDate = resolveShipDate(d, shippingInfoMap);
            BigDecimal total = d.getTotal();
            BigDecimal totalCny = (total != null && rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                    ? total.multiply(rate).setScale(2, RoundingMode.HALF_UP) : null;

            TaxReportDetailExportRow row = new TaxReportDetailExportRow();
            row.setSourceType(d.getSourceType());
            row.setStoreName(d.getStoreName());
            row.setTransactionDate(d.getTransactionDate());
            row.setSettlementId(d.getSettlementId());
            row.setErpSettlementId(d.getErpSettlementId());
            row.setTransactionType(d.getTransactionType());
            row.setTransactionCategory(d.getTransactionCategory());
            row.setOrderId(d.getOrderId());
            row.setSku(d.getSku());
            row.setDescription(d.getDescription());
            row.setQuantity(d.getQuantity());
            row.setSiteCode(d.getSiteCode());
            row.setMarketplace(d.getMarketplace());
            row.setCurrencyCode(d.getCurrencyCode());
            row.setFulfillment(d.getFulfillment());
            row.setProductSales(d.getProductSales());
            row.setProductSalesTax(d.getProductSalesTax());
            row.setShippingCredits(d.getShippingCredits());
            row.setShippingCreditsTax(d.getShippingCreditsTax());
            row.setGiftWrapCredits(d.getGiftWrapCredits());
            row.setGiftWrapCreditsTax(d.getGiftWrapCreditsTax());
            row.setRegulatoryFee(d.getRegulatoryFee());
            row.setRegulatoryFeeTax(d.getRegulatoryFeeTax());
            row.setPromotionalRebates(d.getPromotionalRebates());
            row.setPromotionalRebatesTax(d.getPromotionalRebatesTax());
            row.setMarketplaceWithheldTax(d.getMarketplaceWithheldTax());
            row.setSellingFees(d.getSellingFees());
            row.setFbaFees(d.getFbaFees());
            row.setOtherTransactionFees(d.getOtherTransactionFees());
            row.setOther(d.getOther());
            row.setTotal(d.getTotal());
            row.setExchangeRate(rate);
            row.setShipDate(shipDate);
            row.setTotalCny(totalCny);
            rows.add(row);
        }
        return rows;
    }

    private List<TaxReportDetailExportRowNoCny> toExportRowsNoCny(List<SalesData> list, boolean useSalesRateOnly) {
        Long shopId = ShopContext.requireShopId();
        Map<String, ShippingInfo> shippingInfoMap = useSalesRateOnly ? Map.of() : buildShippingInfoMap(shopId, list);

        List<TaxReportDetailExportRowNoCny> rows = new ArrayList<>(list.size());
        for (SalesData d : list) {
            BigDecimal rate = resolveExchangeRate(d, shippingInfoMap);
            LocalDate shipDate = resolveShipDate(d, shippingInfoMap);

            TaxReportDetailExportRowNoCny row = new TaxReportDetailExportRowNoCny();
            row.setSourceType(d.getSourceType());
            row.setStoreName(d.getStoreName());
            row.setTransactionDate(d.getTransactionDate());
            row.setSettlementId(d.getSettlementId());
            row.setErpSettlementId(d.getErpSettlementId());
            row.setTransactionType(d.getTransactionType());
            row.setTransactionCategory(d.getTransactionCategory());
            row.setOrderId(d.getOrderId());
            row.setSku(d.getSku());
            row.setDescription(d.getDescription());
            row.setQuantity(d.getQuantity());
            row.setSiteCode(d.getSiteCode());
            row.setMarketplace(d.getMarketplace());
            row.setCurrencyCode(d.getCurrencyCode());
            row.setFulfillment(d.getFulfillment());
            row.setProductSales(d.getProductSales());
            row.setProductSalesTax(d.getProductSalesTax());
            row.setShippingCredits(d.getShippingCredits());
            row.setShippingCreditsTax(d.getShippingCreditsTax());
            row.setGiftWrapCredits(d.getGiftWrapCredits());
            row.setGiftWrapCreditsTax(d.getGiftWrapCreditsTax());
            row.setRegulatoryFee(d.getRegulatoryFee());
            row.setRegulatoryFeeTax(d.getRegulatoryFeeTax());
            row.setPromotionalRebates(d.getPromotionalRebates());
            row.setPromotionalRebatesTax(d.getPromotionalRebatesTax());
            row.setMarketplaceWithheldTax(d.getMarketplaceWithheldTax());
            row.setSellingFees(d.getSellingFees());
            row.setFbaFees(d.getFbaFees());
            row.setOtherTransactionFees(d.getOtherTransactionFees());
            row.setOther(d.getOther());
            row.setTotal(d.getTotal());
            row.setExchangeRate(rate);
            row.setShipDate(shipDate);
            rows.add(row);
        }
        return rows;
    }

    /** 配送信息：汇率 + 配送日期 */
    private record ShippingInfo(BigDecimal exchangeRate, LocalDate shipDate) {}

    /**
     * 从配送数据批量查询，构建 orderId|siteCode -> (汇率, 配送日期) 映射
     */
    private Map<String, ShippingInfo> buildShippingInfoMap(Long shopId, List<SalesData> list) {
        List<String> orderIds = list.stream()
                .map(SalesData::getOrderId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) return Map.of();

        LambdaQueryWrapper<ShippingData> w = new LambdaQueryWrapper<>();
        w.eq(ShippingData::getShopId, shopId).in(ShippingData::getOrderId, orderIds)
                .select(ShippingData::getOrderId, ShippingData::getSiteCode, ShippingData::getExchangeRate, ShippingData::getShipDate);
        List<ShippingData> shippingList = shippingDataMapper.selectList(w);

        return shippingList.stream()
                .filter(s -> s.getExchangeRate() != null && s.getExchangeRate().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        s -> s.getOrderId() + "|" + (s.getSiteCode() != null ? s.getSiteCode() : ""),
                        s -> new ShippingInfo(s.getExchangeRate(), s.getShipDate()),
                        (a, b) -> a));
    }

    private BigDecimal resolveExchangeRate(SalesData d, Map<String, ShippingInfo> shippingInfoMap) {
        if (d.getOrderId() != null && d.getSiteCode() != null) {
            ShippingInfo info = shippingInfoMap.get(d.getOrderId() + "|" + d.getSiteCode());
            if (info != null) return info.exchangeRate();
        }
        return d.getExchangeRate();
    }

    private LocalDate resolveShipDate(SalesData d, Map<String, ShippingInfo> shippingInfoMap) {
        if (d.getOrderId() != null && d.getSiteCode() != null) {
            ShippingInfo info = shippingInfoMap.get(d.getOrderId() + "|" + d.getSiteCode());
            if (info != null) return info.shipDate();
        }
        return null;
    }

    private List<String> getQuartersInRange(String startQuarter, String endQuarter) {
        List<String> quarters = new ArrayList<>();
        if (!StringUtils.hasText(startQuarter) || !StringUtils.hasText(endQuarter)) {
            LocalDate now = LocalDate.now();
            int q = (now.getMonthValue() - 1) / 3 + 1;
            quarters.add(now.getYear() + "-Q" + q);
            return quarters;
        }
        int startYear = Integer.parseInt(startQuarter.substring(0, 4));
        int startQ = Integer.parseInt(startQuarter.substring(6, 7));
        int endYear = Integer.parseInt(endQuarter.substring(0, 4));
        int endQ = Integer.parseInt(endQuarter.substring(6, 7));
        int y = startYear, q = startQ;
        while (y < endYear || (y == endYear && q <= endQ)) {
            quarters.add(y + "-Q" + q);
            q++;
            if (q > 4) { q = 1; y++; }
        }
        return quarters;
    }

    private LocalDate getQuarterStartDate(int year, int quarter) {
        return LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
    }

    private LocalDate getQuarterEndDate(int year, int quarter) {
        return LocalDate.of(year, quarter * 3, 1).plusMonths(1).minusDays(1);
    }
}
