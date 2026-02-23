package com.musheng.business.sales.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.utils.DateParseUtils;
import com.musheng.business.sales.dto.SalesDataExportRow;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.service.SalesDataExportService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售数据导出服务实现类
 * 导出原始数据全字段，非列表精简视图
 *
 * @author wanhua
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataExportServiceImpl implements SalesDataExportService {

    private final SalesDataMapper salesDataMapper;

    @Override
    public void exportData(String siteCode, String transactionCategory, String startDate, String endDate,
                           HttpServletResponse response) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        Long shopId = ShopContext.requireShopId();
        wrapper.eq(SalesData::getShopId, shopId);

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(transactionCategory)) {
            wrapper.eq(SalesData::getTransactionCategory, transactionCategory);
        }
        applyDateRangeFilter(wrapper, startDate, endDate);

        wrapper.orderByDesc(SalesData::getTransactionDate);
        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        try {
            String fileName = "销售数据_原始_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            List<SalesDataExportRow> rows = dataList.stream().map(this::toExportRow).collect(Collectors.toList());
            EasyExcel.write(response.getOutputStream(), SalesDataExportRow.class)
                    .sheet("销售数据")
                    .doWrite(rows);
        } catch (Exception e) {
            log.error("Failed to export sales data", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    private SalesDataExportRow toExportRow(SalesData d) {
        SalesDataExportRow row = new SalesDataExportRow();
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
        row.setExchangeRate(d.getExchangeRate());
        row.setExchangeRateDate(d.getExchangeRateDate());
        return row;
    }

    private void applyDateRangeFilter(LambdaQueryWrapper<SalesData> wrapper, String startDate, String endDate) {
        LocalDateTime start = DateParseUtils.parseStartDate(startDate);
        if (start != null) {
            wrapper.ge(SalesData::getTransactionDate, start);
        }
        LocalDateTime end = DateParseUtils.parseEndDate(endDate);
        if (end != null) {
            wrapper.le(SalesData::getTransactionDate, end);
        }
    }
}
