package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.utils.DateParseUtils;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.service.SalesDataExportService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售数据导出服务实现类
 * 
 * 职责：
 * 1. 导出销售数据到Excel
 * 
 * @author wanhua
 * 10:30 2026年02月01日
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

        // 店铺数据隔离
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
            String fileName = "sales_data_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("Sales Data");

                // Create header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"订单号", "站点", "交易日期", "交易类型", "交易分类", "SKU",
                        "数量", "产品销售", "销售费用", "FBA费用", "合计", "货币"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Fill data rows
                int rowNum = 1;
                for (SalesData data : dataList) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data.getOrderId());
                    row.createCell(1).setCellValue(data.getSiteCode());
                    row.createCell(2).setCellValue(data.getTransactionDate() != null ? data.getTransactionDate().toString() : "");
                    row.createCell(3).setCellValue(data.getTransactionType());
                    row.createCell(4).setCellValue(data.getTransactionCategory());
                    row.createCell(5).setCellValue(data.getSku());
                    row.createCell(6).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
                    row.createCell(7).setCellValue(data.getProductSales() != null ? data.getProductSales().doubleValue() : 0);
                    row.createCell(8).setCellValue(data.getSellingFees() != null ? data.getSellingFees().doubleValue() : 0);
                    row.createCell(9).setCellValue(data.getFbaFees() != null ? data.getFbaFees().doubleValue() : 0);
                    row.createCell(10).setCellValue(data.getTotal() != null ? data.getTotal().doubleValue() : 0);
                    row.createCell(11).setCellValue(data.getCurrencyCode());
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Failed to export sales data", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 添加日期范围过滤条件到查询包装器
     * 
     * @param wrapper   查询包装器
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
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
