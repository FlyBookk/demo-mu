package com.musheng.business.advertising.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.entity.AdvertisingBill;
import com.musheng.business.advertising.entity.AdvertisingBillItem;
import com.musheng.business.advertising.mapper.AdvertisingBillItemMapper;
import com.musheng.business.advertising.mapper.AdvertisingBillMapper;
import com.musheng.business.advertising.service.AdvertisingBillService;
import com.musheng.business.rate.dto.RateWithDateDTO;
import com.musheng.business.rate.service.RateService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 广告发票服务实现（主表+明细，无去重）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisingBillServiceImpl implements AdvertisingBillService {

    private static final Set<String> VALID_CURRENCIES = Set.of("USD", "CAD", "GBP", "EUR");

    private final AdvertisingBillMapper advertisingBillMapper;
    private final AdvertisingBillItemMapper advertisingBillItemMapper;
    private final RateService rateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingDataImportResponse importData(AdvertisingDataImportBatchRequest request) {
        List<AdvertisingDataImportRequest> dataList = request.getData();
        int totalCount = dataList.size();

        String importBatchId = request.getImportBatchId();
        if (!StringUtils.hasText(importBatchId)) {
            importBatchId = "BATCH-" + System.currentTimeMillis();
        }

        Long shopId = ShopContext.requireShopId();
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        // 按 (shopId, invoiceNumber) 分组，每组→1个bill + N个item（无去重，全部入库）
        Map<String, List<AdvertisingDataImportRequest>> groupByInvoice = dataList.stream()
                .filter(r -> StringUtils.hasText(r.getInvoiceNumber()))
                .collect(Collectors.groupingBy(r -> shopId + "|" + r.getInvoiceNumber().trim()));

        List<AdvertisingDataImportResponse.ImportFailureDetail> failedRecords = new ArrayList<>();
        int importedBillCount = 0;
        int importedItemCount = 0;

        for (Map.Entry<String, List<AdvertisingDataImportRequest>> entry : groupByInvoice.entrySet()) {
            List<AdvertisingDataImportRequest> rows = entry.getValue();
            if (rows.isEmpty()) continue;

            AdvertisingDataImportRequest first = rows.get(0);
            try {
                validateImportData(first);

                // 创建主表
                AdvertisingBill bill = buildBill(first, shopId, importBatchId, currentUserId);
                advertisingBillMapper.insert(bill);

                BigDecimal totalCost = BigDecimal.ZERO;
                BigDecimal totalCostCny = BigDecimal.ZERO;

                for (AdvertisingDataImportRequest row : rows) {
                    AdvertisingBillItem item = buildItem(row, bill.getId(), bill.getInvoiceNumber(),
                            shopId, importBatchId, currentUserId);
                    fillExchangeRate(item, bill.getIssueDate(), bill.getCurrency());
                    advertisingBillItemMapper.insert(item);

                    if (item.getCost() != null) totalCost = totalCost.add(item.getCost());
                    if (item.getAmountCny() != null) totalCostCny = totalCostCny.add(item.getAmountCny());
                    else if (item.getCost() != null && item.getExchangeRate() != null) {
                        totalCostCny = totalCostCny.add(item.getCost().multiply(item.getExchangeRate()));
                    }
                    importedItemCount++;
                }

                bill.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
                bill.setTotalCostCny(totalCostCny.setScale(4, RoundingMode.HALF_UP));
                advertisingBillMapper.updateById(bill);

                importedBillCount++;
            } catch (Exception e) {
                log.error("导入失败: invoiceNumber={}, error={}", first.getInvoiceNumber(), e.getMessage());
                failedRecords.add(AdvertisingDataImportResponse.ImportFailureDetail.builder()
                        .invoiceNumber(first.getInvoiceNumber())
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        int failedCount = failedRecords.size();

        log.info("广告导入完成: total={}, bills={}, items={}, failed={}",
                totalCount, importedBillCount, importedItemCount, failedCount);

        return AdvertisingDataImportResponse.builder()
                .totalCount(totalCount)
                .importedCount(importedItemCount)
                .duplicatedCount(0)
                .failedCount(failedCount)
                .importBatchId(importBatchId)
                .duplicatedInvoices(Collections.emptyList())
                .failedRecords(failedRecords)
                .build();
    }

    @Override
    public Page<AdvertisingBill> list(String siteCode, LocalDate billingStartDate, LocalDate billingEndDate,
                                      String invoiceNumber, int page, int size) {
        LambdaQueryWrapper<AdvertisingBill> wrapper = new LambdaQueryWrapper<>();
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(AdvertisingBill::getShopId, shopId);

        if (StringUtils.hasText(siteCode)) wrapper.eq(AdvertisingBill::getSiteCode, siteCode);
        if (billingStartDate != null) wrapper.ge(AdvertisingBill::getBillingStartDate, billingStartDate);
        if (billingEndDate != null) wrapper.le(AdvertisingBill::getBillingEndDate, billingEndDate);
        if (StringUtils.hasText(invoiceNumber)) wrapper.like(AdvertisingBill::getInvoiceNumber, invoiceNumber);

        wrapper.orderByDesc(AdvertisingBill::getCreateTime);
        return advertisingBillMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public AdvertisingBill getById(Long id) {
        AdvertisingBill bill = advertisingBillMapper.selectById(id);
        if (bill == null) throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "发票不存在");

        LambdaQueryWrapper<AdvertisingBillItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(AdvertisingBillItem::getBillId, id).orderByAsc(AdvertisingBillItem::getCampaignId);
        bill.setItems(advertisingBillItemMapper.selectList(itemWrapper));
        return bill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        advertisingBillItemMapper.deleteByBillId(id);
        advertisingBillMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        advertisingBillItemMapper.deleteByBillIds(ids);
        advertisingBillMapper.deleteBatchIds(ids);
    }

    @Override
    public Page<AdvertisingBillItem> listItems(String invoiceNumber, String campaignId, String campaignName,
                                              int page, int size) {
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<AdvertisingBillItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdvertisingBillItem::getShopId, shopId);

        if (StringUtils.hasText(invoiceNumber)) wrapper.like(AdvertisingBillItem::getInvoiceNumber, invoiceNumber);
        if (StringUtils.hasText(campaignId)) wrapper.like(AdvertisingBillItem::getCampaignId, campaignId);
        if (StringUtils.hasText(campaignName)) wrapper.like(AdvertisingBillItem::getCampaignName, campaignName);

        wrapper.orderByDesc(AdvertisingBillItem::getCreateTime);
        return advertisingBillItemMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private void validateImportData(AdvertisingDataImportRequest data) {
        if (data.getBillingStartDate().isAfter(data.getBillingEndDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账单开始日期不能晚于结束日期");
        }
        if (!VALID_CURRENCIES.contains(data.getCurrency())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的币种: " + data.getCurrency());
        }
    }

    private AdvertisingBill buildBill(AdvertisingDataImportRequest r, Long shopId, String importBatchId, Long userId) {
        AdvertisingBill bill = new AdvertisingBill();
        bill.setShopId(shopId);
        bill.setStoreName(r.getStoreName());
        bill.setSiteCode(inferSiteCode(r.getStoreName(), r.getSiteCode()));
        bill.setInvoiceNumber(r.getInvoiceNumber());
        bill.setInvoiceStatus(r.getInvoiceStatus());
        bill.setPaymentType(r.getPaymentType());
        bill.setBillingStartDate(r.getBillingStartDate());
        bill.setBillingEndDate(r.getBillingEndDate());
        bill.setIssueDate(r.getIssueDate());
        bill.setCurrency(r.getCurrency());
        bill.setInvoiceAmount(r.getInvoiceAmount() != null ? r.getInvoiceAmount() : BigDecimal.ZERO);
        bill.setImportBatchId(importBatchId);
        bill.setCreateBy(userId);
        return bill;
    }

    private AdvertisingBillItem buildItem(AdvertisingDataImportRequest r, Long billId, String invoiceNumber,
                                          Long shopId, String importBatchId, Long userId) {
        AdvertisingBillItem item = new AdvertisingBillItem();
        item.setShopId(shopId);
        item.setBillId(billId);
        item.setInvoiceNumber(invoiceNumber);
        item.setCampaignName(r.getCampaignName());
        item.setCampaignId(r.getCampaignId() != null ? r.getCampaignId() : "");
        item.setPricingModel(r.getPricingModel());
        item.setClicks(r.getClicks());
        item.setAvgCpc(r.getAvgCpc());
        item.setCost(r.getCost() != null ? r.getCost() : BigDecimal.ZERO);
        item.setOtherCost(r.getOtherCost() != null ? r.getOtherCost() : BigDecimal.ZERO);
        item.setDataSource(r.getDataSource());
        item.setProductList(r.getProductList());
        item.setAdType(r.getAdType());
        item.setImportBatchId(importBatchId);
        item.setCreateBy(userId);
        return item;
    }

    private void fillExchangeRate(AdvertisingBillItem item, LocalDate issueDate, String currency) {
        if (issueDate == null || !StringUtils.hasText(currency)) return;
        if ("CNY".equalsIgnoreCase(currency)) {
            item.setExchangeRate(BigDecimal.ONE);
            item.setExchangeRateDate(issueDate);
            if (item.getCost() != null) item.setAmountCny(item.getCost());
            return;
        }
        try {
            RateWithDateDTO dto = rateService.getRateWithDate(currency, issueDate);
            item.setExchangeRate(dto.getRate());
            item.setExchangeRateDate(dto.getActualDate());
            if (item.getCost() != null) {
                item.setAmountCny(item.getCost().multiply(dto.getRate()));
            }
        } catch (Exception e) {
            log.warn("汇率查询失败: currency={}, date={}", currency, issueDate, e);
        }
    }

    private String inferSiteCode(String storeName, String siteCode) {
        if (StringUtils.hasText(siteCode)) return siteCode;
        if (!StringUtils.hasText(storeName)) return null;
        String s = storeName.toUpperCase();
        if (s.contains("UK") || s.contains("英国")) return "UK";
        if (s.contains("US") || s.contains("美国")) return "US";
        if (s.contains("CA") || s.contains("加拿大")) return "CA";
        if (s.contains("DE") || s.contains("德国")) return "DE";
        return null;
    }

    @Override
    public void downloadTemplate(jakarta.servlet.http.HttpServletResponse response) {
        String[] headers = {"店铺", "发票编号", "发票状态", "支付类型", "账单周期", "开具时间", "付款币种", "账单金额",
                "广告活动", "活动ID", "计价方式", "点击", "平均点击单价", "费用", "其他费分摊", "取值来源", "承担商品", "广告类型"};
        try {
            String fileName = "广告数据导入模板.xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (Workbook workbook = new XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {
                Sheet sheet = workbook.createSheet("广告数据");
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                Row exampleRow = sheet.createRow(1);
                exampleRow.createCell(0).setCellValue("慕声欧洲-UK");
                exampleRow.createCell(1).setCellValue("2012576M7PA25");
                exampleRow.createCell(2).setCellValue("PAID_IN_FULL");
                exampleRow.createCell(3).setCellValue("CREDIT_CARD");
                exampleRow.createCell(4).setCellValue("2025-06-27至2025-07-02");
                exampleRow.createCell(5).setCellValue("2025-07-01");
                exampleRow.createCell(6).setCellValue("GBP");
                exampleRow.createCell(7).setCellValue(513.39);
                exampleRow.createCell(8).setCellValue("D28手动广泛");
                exampleRow.createCell(9).setCellValue("205478948551481");
                exampleRow.createCell(10).setCellValue("CPC");
                exampleRow.createCell(11).setCellValue(6);
                exampleRow.createCell(12).setCellValue(0.42);
                exampleRow.createCell(13).setCellValue(2.49);
                exampleRow.createCell(14).setCellValue(0.00);
                exampleRow.createCell(15).setCellValue("业务报告");
                exampleRow.createCell(16).setCellValue("MSUK-D28-209-3,MSUK-D28-268-3");
                exampleRow.createCell(17).setCellValue("SPONSORED PRODUCTS");
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("下载广告模板失败", e);
            throw new RuntimeException("下载模板失败: " + e.getMessage(), e);
        }
    }
}
