package com.musheng.tiktok.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.document.entity.*;
import com.musheng.tiktok.document.mapper.*;
import com.musheng.tiktok.document.service.TiktokDocumentExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * TK单据导出服务实现 - 对齐亚马逊Excel样式
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Service
@Slf4j
public class TiktokDocumentExportServiceImpl implements TiktokDocumentExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/M/d");

    private static final String SHEET_NAME_PO = "1、PO（采购订单）";
    private static final String SHEET_NAME_DN = "2、送货单";
    private static final String SHEET_NAME_SETTLEMENT = "3、结算单";
    private static final String SHEET_NAME_INV = "4、形式发票";

    @Autowired
    private TiktokDocumentPoMapper poMapper;
    @Autowired
    private TiktokDocumentPoItemMapper poItemMapper;
    @Autowired
    private TiktokDocumentDnMapper dnMapper;
    @Autowired
    private TiktokDocumentDnItemMapper dnItemMapper;
    @Autowired
    private TiktokDocumentSettlementMapper settlementMapper;
    @Autowired
    private TiktokDocumentSettlementItemMapper settlementItemMapper;
    @Autowired
    private TiktokDocumentInvMapper invMapper;
    @Autowired
    private TiktokDocumentInvItemMapper invItemMapper;

    // ==================== 导出PO ====================

    @Override
    public void exportPo(Long poId, HttpServletResponse response) {
        TiktokDocumentPo po = poMapper.selectById(poId);
        requireOwnership(po != null ? po.getShopId() : null);
        List<TiktokDocumentPoItem> items = poItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentPoItem>()
                .eq(TiktokDocumentPoItem::getPoId, poId).orderByAsc(TiktokDocumentPoItem::getSortOrder));
        try {
            writeResponse(response, buildPoWorkbook(po, items), po.getDocumentNo() + "-PO.xlsx");
        } catch (IOException e) {
            log.error("导出PO失败", e);
        }
    }

    private void writePoExcel(OutputStream os, TiktokDocumentPo po, List<TiktokDocumentPoItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_PO);

            // 字体
            XSSFFont fontCompany = wb.createFont();
            fontCompany.setFontName("微软雅黑");
            fontCompany.setFontHeightInPoints((short) 22);
            fontCompany.setBold(true);

            XSSFFont fontAddress = wb.createFont();
            fontAddress.setFontName("微软雅黑");
            fontAddress.setFontHeightInPoints((short) 14);

            XSSFFont fontNormal = wb.createFont();
            fontNormal.setFontName("微软雅黑");
            fontNormal.setFontHeightInPoints((short) 12);

            // 样式
            CellStyle csCompany = wb.createCellStyle();
            csCompany.setFont(fontCompany);
            csCompany.setAlignment(HorizontalAlignment.CENTER);
            csCompany.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCompany);

            CellStyle csAddress = wb.createCellStyle();
            csAddress.setFont(fontAddress);
            csAddress.setAlignment(HorizontalAlignment.CENTER);
            csAddress.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csAddress);

            CellStyle csCenterBorder = wb.createCellStyle();
            csCenterBorder.setFont(fontNormal);
            csCenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csCenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCenterBorder);

            CellStyle csContentWrap = wb.createCellStyle();
            csContentWrap.setFont(fontNormal);
            csContentWrap.setAlignment(HorizontalAlignment.CENTER);
            csContentWrap.setVerticalAlignment(VerticalAlignment.CENTER);
            csContentWrap.setWrapText(true);
            setBorders(csContentWrap);

            int rowIdx = 0;

            // Row1: 公司名称（B1:D1合并）
            Row row1 = sheet.createRow(rowIdx++);
            createCell(row1, 1, safe(po.getBuyerName()), csCompany);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 3));

            // Row2: 地址（B2:D2合并）
            Row row2 = sheet.createRow(rowIdx++);
            row2.setHeightInPoints(27f);
            createCell(row2, 1, safe(po.getBuyerAddress()), csAddress);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));

            // Row3: Purchase Order（B3:D3合并）
            Row row3 = sheet.createRow(rowIdx++);
            row3.setHeightInPoints(37f);
            createCell(row3, 1, "Purchase Order", csCompany);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 3));

            // A1:A3合并（先创建A1单元格带样式，避免合并区域出现虚线）
            createCell(row1, 0, "", csCompany);
            sheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 0));

            // Row4: Purchasing object / 卖方名称（B4:D4合并）
            Row row4 = sheet.createRow(rowIdx++);
            row4.setHeightInPoints(39.5f);
            createCell(row4, 0, "Purchasing object", csCenterBorder);
            createCell(row4, 1, safe(po.getSellerName()), csCenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 3));

            // Row5: Contract No. / 编号 / Date: / 日期
            Row row5 = sheet.createRow(rowIdx++);
            row5.setHeightInPoints(39.5f);
            createCell(row5, 0, "Contract No.", csCenterBorder);
            createCell(row5, 1, safe(po.getDocumentNo()), csCenterBorder);
            createCell(row5, 2, "Date:", csCenterBorder);
            createCell(row5, 3, po.getPoDate() != null ? po.getPoDate().format(DATE_FMT) : "", csCenterBorder);

            // Row6: 表头
            Row row6 = sheet.createRow(rowIdx++);
            row6.setHeightInPoints(23f);
            createCell(row6, 0, "No#", csCenterBorder);
            createCell(row6, 1, "Description", csContentWrap);
            createCell(row6, 2, "Q'ty", csCenterBorder);
            createCell(row6, 3, "FBT Address", csContentWrap);

            // 数据行
            int dataStartRow = rowIdx;
            for (TiktokDocumentPoItem item : items) {
                Row dataRow = sheet.createRow(rowIdx);
                dataRow.setHeightInPoints(23f);
                createCell(dataRow, 0, safe(item.getShipmentNo()), csCenterBorder);
                createCell(dataRow, 1, safe(item.getMsku()), csContentWrap);
                createCell(dataRow, 2, String.valueOf(item.getQuantity()), csCenterBorder);
                createCell(dataRow, 3, safe(item.getFbtAddress()), csContentWrap);
                rowIdx++;
            }

            // 合并FBT地址列（同一货件编号）
            mergeAddressColumn(sheet, items, dataStartRow);

            // 合计行
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(31f);
            createCell(totalRow, 0, "合计Total", csCenterBorder);
            createCell(totalRow, 1, "", csCenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            int totalQty = items.stream().mapToInt(TiktokDocumentPoItem::getQuantity).sum();
            createCell(totalRow, 2, String.valueOf(totalQty), csCenterBorder);
            createCell(totalRow, 3, "", csCenterBorder);

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csCenterBorder);

            // 自适应列宽（最小宽度：No#=18, Description=30, Q'ty=10, FBT Address=30）
            autoFitColumns(sheet, 4, new double[]{16, 28, 10, 30});

            wb.write(os);
        }
    }

    /**
     * 合并PO FBT地址列 - 同一货件编号的地址只在首行显示
     */
    private void mergeAddressColumn(Sheet sheet, List<TiktokDocumentPoItem> items, int dataStartRow) {
        if (items.size() <= 1) return;
        int mergeStart = dataStartRow;
        String currentShipment = items.get(0).getShipmentNo();
        for (int i = 1; i < items.size(); i++) {
            String shipment = items.get(i).getShipmentNo();
            if (shipment != null && shipment.equals(currentShipment)) continue;
            if (dataStartRow + i - 1 > mergeStart) {
                sheet.addMergedRegion(new CellRangeAddress(mergeStart, dataStartRow + i - 1, 3, 3));
            }
            mergeStart = dataStartRow + i;
            currentShipment = shipment;
        }
        if (dataStartRow + items.size() - 1 > mergeStart) {
            sheet.addMergedRegion(new CellRangeAddress(mergeStart, dataStartRow + items.size() - 1, 3, 3));
        }
    }

    // ==================== 导出DN ====================

    @Override
    public void exportDn(Long dnId, HttpServletResponse response) {
        TiktokDocumentDn dn = dnMapper.selectById(dnId);
        requireOwnership(dn != null ? dn.getShopId() : null);
        List<TiktokDocumentDnItem> items = dnItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentDnItem>()
                .eq(TiktokDocumentDnItem::getDnId, dnId).orderByAsc(TiktokDocumentDnItem::getLineNo));
        try {
            writeResponse(response, buildDnWorkbook(dn, items), dn.getDocumentNo() + "-DN.xlsx");
        } catch (IOException e) {
            log.error("导出DN失败", e);
        }
    }

    private void writeDnExcel(OutputStream os, TiktokDocumentDn dn, List<TiktokDocumentDnItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_DN);

            // 字体
            XSSFFont fontTitle = wb.createFont();
            fontTitle.setFontName("宋体");
            fontTitle.setFontHeightInPoints((short) 20);
            fontTitle.setBold(true);

            XSSFFont fontBold12 = wb.createFont();
            fontBold12.setFontName("宋体");
            fontBold12.setFontHeightInPoints((short) 12);
            fontBold12.setBold(true);

            XSSFFont fontNormal12 = wb.createFont();
            fontNormal12.setFontName("宋体");
            fontNormal12.setFontHeightInPoints((short) 12);

            XSSFFont fontYahei12 = wb.createFont();
            fontYahei12.setFontName("微软雅黑");
            fontYahei12.setFontHeightInPoints((short) 12);

            // 样式
            CellStyle csTitle = wb.createCellStyle();
            csTitle.setFont(fontTitle);
            csTitle.setAlignment(HorizontalAlignment.CENTER);
            csTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csTitle);

            CellStyle csBold12Center = wb.createCellStyle();
            csBold12Center.setFont(fontBold12);
            csBold12Center.setAlignment(HorizontalAlignment.CENTER);
            csBold12Center.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12Center);

            CellStyle csBold12CenterBorder = wb.createCellStyle();
            csBold12CenterBorder.setFont(fontBold12);
            csBold12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csBold12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12CenterBorder);

            CellStyle csNormal12CenterBorder = wb.createCellStyle();
            csNormal12CenterBorder.setFont(fontNormal12);
            csNormal12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csNormal12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12CenterBorder);

            CellStyle csYahei12CenterBorder = wb.createCellStyle();
            csYahei12CenterBorder.setFont(fontYahei12);
            csYahei12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csYahei12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csYahei12CenterBorder);

            CellStyle csNote = wb.createCellStyle();
            csNote.setFont(fontNormal12);
            csNote.setAlignment(HorizontalAlignment.LEFT);
            csNote.setVerticalAlignment(VerticalAlignment.CENTER);
            csNote.setWrapText(true);
            setBorders(csNote);

            int rowIdx = 0;

            // Row1: 供应商名称（A1:F1合并）
            Row row1 = sheet.createRow(rowIdx++);
            row1.setHeightInPoints(48f);
            createCell(row1, 0, safe(dn.getSupplierName()), csTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Row2: 送貨清單（A2:F2合并）
            Row row2 = sheet.createRow(rowIdx++);
            row2.setHeightInPoints(24f);
            createCell(row2, 0, "送貨清單", csTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            // Row3: 客戶名稱 / 客户名
            Row row3 = sheet.createRow(rowIdx++);
            row3.setHeightInPoints(25f);
            createCell(row3, 0, "客戶名稱（Customer Name）：", csBold12Center);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 1));
            createCell(row3, 2, safe(dn.getCustomerName()), csBold12Center);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 5));

            // Row4: 送貨日期 / 日期 / 送貨單號 / 编号
            Row row4 = sheet.createRow(rowIdx++);
            row4.setHeightInPoints(25f);
            createCell(row4, 0, "送貨日期（Delivery date）：", csBold12Center);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 1));
            createCell(row4, 2, dn.getDnDate() != null ? dn.getDnDate().format(DATE_FMT) : "", csBold12Center);
            createCell(row4, 3, "送貨單號(Delivery Number)：", csBold12Center);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 4));
            createCell(row4, 5, safe(dn.getDocumentNo()), csBold12Center);

            // Row5: 表头
            Row row5 = sheet.createRow(rowIdx++);
            row5.setHeightInPoints(24.75f);
            createCell(row5, 0, "No", csBold12CenterBorder);
            createCell(row5, 1, "產品名稱(Product Name)", csBold12CenterBorder);
            createCell(row5, 2, "", csBold12CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
            createCell(row5, 3, "數量(quantity)", csBold12CenterBorder);
            createCell(row5, 4, "備註（對應貨件編號）", csBold12CenterBorder);
            createCell(row5, 5, "", csBold12CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 4, 5));

            // 数据行
            for (TiktokDocumentDnItem item : items) {
                Row dataRow = sheet.createRow(rowIdx);
                dataRow.setHeightInPoints(19.5f);
                createCell(dataRow, 0, String.valueOf(item.getLineNo()), csNormal12CenterBorder);
                createCell(dataRow, 1, safe(item.getMsku()), csNormal12CenterBorder);
                createCell(dataRow, 2, "", csNormal12CenterBorder);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, 2));
                createCell(dataRow, 3, String.valueOf(item.getQuantity()), csNormal12CenterBorder);
                createCell(dataRow, 4, safe(item.getShipmentNo()), csYahei12CenterBorder);
                createCell(dataRow, 5, "", csYahei12CenterBorder);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 5));
                rowIdx++;
            }

            // 合计行
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(19.5f);
            createCell(totalRow, 0, "數量合計(Total quantity)：", csBold12CenterBorder);
            createCell(totalRow, 1, "", csBold12CenterBorder);
            createCell(totalRow, 2, "", csBold12CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));
            int totalQty = items.stream().mapToInt(TiktokDocumentDnItem::getQuantity).sum();
            createCell(totalRow, 3, String.valueOf(totalQty), csNormal12CenterBorder);
            createCell(totalRow, 4, "", csNormal12CenterBorder);
            createCell(totalRow, 5, "", csNormal12CenterBorder);
            rowIdx++;

            // 特别说明行
            Row noteRow = sheet.createRow(rowIdx);
            noteRow.setHeightInPoints(130f);
            String noteText = "特別說明:\n1、貨物一經簽收，即視為貨物數量無誤;\n"
                    + "2、收貨方如發現品質問題請在收到貨後七天內向送貨方提出，否則送貨方一律不承擔任何責任。\n"
                    + "Special Notes:\n1. Once the goods are signed for, it is assumed that the quantity of the goods is correct.\n"
                    + "2. If the recipient discovers any quality issues, they must inform the supplier within seven days of receiving the goods; "
                    + "otherwise, the supplier shall not assume any responsibility.";
            createCell(noteRow, 0, noteText, csNote);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 5));
            rowIdx++;

            // 签收行
            Row signRow1 = sheet.createRow(rowIdx);
            signRow1.setHeightInPoints(25f);
            createCell(signRow1, 0, "收貨單位及經手人(Receiving unit and handler):", csNote);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
            rowIdx++;

            Row signRow2 = sheet.createRow(rowIdx);
            signRow2.setHeightInPoints(25f);
            createCell(signRow2, 0, "送貨單位及經手人(Shipping company and the person in charge of the delivery):", csNote);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal12CenterBorder);

            // 自适应列宽（最小宽度：No=6, 产品名=18, col2=18, 数量=10, 货件编号=20, col5=12）
            autoFitColumns(sheet, 6, new double[]{5, 15, 15, 8, 16, 10});

            wb.write(os);
        }
    }

    // ==================== 导出结算单 ====================

    @Override
    public void exportSettlement(Long settlementId, HttpServletResponse response) {
        TiktokDocumentSettlement s = settlementMapper.selectById(settlementId);
        requireOwnership(s != null ? s.getShopId() : null);
        List<TiktokDocumentSettlementItem> items = settlementItemMapper.selectList(
                new LambdaQueryWrapper<TiktokDocumentSettlementItem>()
                        .eq(TiktokDocumentSettlementItem::getSettlementId, settlementId)
                        .orderByAsc(TiktokDocumentSettlementItem::getLineNo));
        try {
            writeResponse(response, buildSettlementWorkbook(s, items), s.getDocumentNo() + "-结算单.xlsx");
        } catch (IOException e) {
            log.error("导出结算单失败", e);
        }
    }

    private void writeSettlementExcel(OutputStream os, TiktokDocumentSettlement s, List<TiktokDocumentSettlementItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_SETTLEMENT);

            // 字体
            XSSFFont fontCompany = wb.createFont();
            fontCompany.setFontName("微软雅黑");
            fontCompany.setFontHeightInPoints((short) 22);
            fontCompany.setBold(true);

            XSSFFont fontAddr = wb.createFont();
            fontAddr.setFontName("微软雅黑");
            fontAddr.setFontHeightInPoints((short) 14);

            XSSFFont fontBold14 = wb.createFont();
            fontBold14.setFontName("微软雅黑");
            fontBold14.setFontHeightInPoints((short) 14);
            fontBold14.setBold(true);

            XSSFFont fontBold12 = wb.createFont();
            fontBold12.setFontName("微软雅黑");
            fontBold12.setFontHeightInPoints((short) 12);
            fontBold12.setBold(true);

            XSSFFont fontNormal12 = wb.createFont();
            fontNormal12.setFontName("微软雅黑");
            fontNormal12.setFontHeightInPoints((short) 12);

            // 样式
            CellStyle csCompany = wb.createCellStyle();
            csCompany.setFont(fontCompany);
            csCompany.setAlignment(HorizontalAlignment.CENTER);
            csCompany.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCompany);

            CellStyle csAddr = wb.createCellStyle();
            csAddr.setFont(fontAddr);
            csAddr.setAlignment(HorizontalAlignment.CENTER);
            csAddr.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csAddr);

            CellStyle csBold12LeftBorder = wb.createCellStyle();
            csBold12LeftBorder.setFont(fontBold12);
            csBold12LeftBorder.setAlignment(HorizontalAlignment.LEFT);
            csBold12LeftBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12LeftBorder);

            CellStyle csBold14CenterBorder = wb.createCellStyle();
            csBold14CenterBorder.setFont(fontBold14);
            csBold14CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csBold14CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold14CenterBorder);

            CellStyle csNormal12CenterBorder = wb.createCellStyle();
            csNormal12CenterBorder.setFont(fontNormal12);
            csNormal12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csNormal12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12CenterBorder);

            CellStyle csNormal12Center = wb.createCellStyle();
            csNormal12Center.setFont(fontNormal12);
            csNormal12Center.setAlignment(HorizontalAlignment.CENTER);
            csNormal12Center.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12Center);

            CellStyle csBold14Left = wb.createCellStyle();
            csBold14Left.setFont(fontBold14);
            csBold14Left.setAlignment(HorizontalAlignment.LEFT);
            csBold14Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold14Left);

            CellStyle csSign = wb.createCellStyle();
            XSSFFont fontSign14 = wb.createFont();
            fontSign14.setFontName("宋体");
            fontSign14.setFontHeightInPoints((short) 14);
            fontSign14.setBold(true);
            csSign.setFont(fontSign14);
            csSign.setAlignment(HorizontalAlignment.LEFT);
            csSign.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csSign);

            int rowIdx = 0;

            // Row1: 公司名称（B1:F1合并）
            Row row1 = sheet.createRow(rowIdx++);
            row1.setHeightInPoints(45f);
            createCell(row1, 1, safe(s.getBuyerName()), csCompany);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 5));

            // Row2: 地址（B2:F2合并）
            Row row2 = sheet.createRow(rowIdx++);
            row2.setHeightInPoints(27f);
            createCell(row2, 1, safe(s.getBuyerAddress()), csAddr);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

            // Row3: Statement of Account（B3:F3合并）
            Row row3 = sheet.createRow(rowIdx++);
            row3.setHeightInPoints(36f);
            createCell(row3, 1, "Statement of Account", csCompany);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 5));

            // A1:A3合并（先创建A1单元格带样式，避免合并区域出现虚线）
            createCell(row1, 0, "", csCompany);
            sheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 0));

            // Row4: Purchasing object / 卖方名称（B4:F4合并）
            Row row4 = sheet.createRow(rowIdx++);
            row4.setHeightInPoints(41f);
            createCell(row4, 0, " Purchasing object：", csBold12LeftBorder);
            createCell(row4, 1, safe(s.getSellerName()), csBold14CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 5));

            // Row5: Settlement Period / 周期 / Settlement No. / 编号
            Row row5 = sheet.createRow(rowIdx++);
            row5.setHeightInPoints(39.5f);
            createCell(row5, 0, "Settlement Period：", csNormal12CenterBorder);
            String period = (s.getPeriodStart() != null && s.getPeriodEnd() != null)
                    ? s.getPeriodStart().format(DATE_FMT) + "~" + s.getPeriodEnd().format(DATE_FMT) : "";
            createCell(row5, 1, period, csNormal12Center);
            createCell(row5, 2, "Settlement No.：", csNormal12Center);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 2, 4));
            createCell(row5, 5, safe(s.getDocumentNo()), csNormal12Center);

            // Row6: Settlement frequency / Monthly / Settlement date / 日期
            Row row6 = sheet.createRow(rowIdx++);
            row6.setHeightInPoints(39.5f);
            createCell(row6, 0, " Settlement frequency:", csNormal12CenterBorder);
            createCell(row6, 1, "Monthly", csNormal12Center);
            createCell(row6, 2, "Settlement date：", csNormal12Center);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 2, 4));
            createCell(row6, 5, s.getSettlementDate() != null ? s.getSettlementDate().format(DATE_FMT) : "", csNormal12Center);

            // Row7: Currency / 币种（B7:F7合并）
            Row row7 = sheet.createRow(rowIdx++);
            row7.setHeightInPoints(39.5f);
            createCell(row7, 0, "Currency:", csNormal12CenterBorder);
            createCell(row7, 1, safe(s.getCurrency()), csNormal12Center);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 1, 5));

            // Row8: 表头
            Row row8 = sheet.createRow(rowIdx++);
            row8.setHeightInPoints(41f);
            createCell(row8, 0, "No#", csNormal12CenterBorder);
            createCell(row8, 1, "Description", csNormal12CenterBorder);
            createCell(row8, 2, "Currency", csNormal12CenterBorder);
            createCell(row8, 3, "Unit price", csNormal12CenterBorder);
            createCell(row8, 4, "Q'ty", csNormal12CenterBorder);
            createCell(row8, 5, "Amount", csNormal12CenterBorder);

            // 数据行
            int seq = 1;
            for (TiktokDocumentSettlementItem item : items) {
                Row dataRow = sheet.createRow(rowIdx);
                dataRow.setHeightInPoints(41f);
                createCell(dataRow, 0, String.valueOf(seq++), csNormal12CenterBorder);
                createCell(dataRow, 1, safe(item.getMsku()), csNormal12CenterBorder);
                createCell(dataRow, 2, safe(item.getCurrency()), csNormal12CenterBorder);
                createCell(dataRow, 3, item.getUnitPrice() != null ? item.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal12CenterBorder);
                createCell(dataRow, 4, String.valueOf(item.getQuantity()), csNormal12CenterBorder);
                createCell(dataRow, 5, item.getAmount() != null ? item.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal12CenterBorder);
                rowIdx++;
            }

            // 合计行
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(41f);
            createCell(totalRow, 0, "合计Total", csNormal12CenterBorder);
            createCell(totalRow, 1, "", csNormal12CenterBorder);
            createCell(totalRow, 2, "", csNormal12CenterBorder);
            createCell(totalRow, 3, "", csNormal12CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
            createCell(totalRow, 4, String.valueOf(s.getTotalQuantity()), csNormal12CenterBorder);
            createCell(totalRow, 5, s.getTotalAmount() != null ? s.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal12CenterBorder);
            rowIdx++;

            // 空行
            rowIdx++;

            // 签章说明
            Row noteRow = sheet.createRow(rowIdx);
            noteRow.setHeightInPoints(41f);
            createCell(noteRow, 0, "注：经签章即表明对以上数据核对无异议", csBold14Left);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 5));
            rowIdx += 2;

            // 卖方确认 / 买方确认
            Row confirmRow = sheet.createRow(rowIdx);
            confirmRow.setHeightInPoints(41f);
            createCell(confirmRow, 0, "卖方确认（SELLER confirms）：", csSign);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            createCell(confirmRow, 2, "买方确认（Buyer confirms）:", csSign);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 5));

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal12CenterBorder);

            // 自适应列宽（最小宽度：标签=24, Description=30, Currency=12, Unit price=12, Q'ty=10, Amount=14）
            autoFitColumns(sheet, 6, new double[]{7, 26, 10, 12, 8, 12});

            wb.write(os);
        }
    }

    // ==================== 导出INV ====================

    @Override
    public void exportInv(Long invId, HttpServletResponse response) {
        TiktokDocumentInv inv = invMapper.selectById(invId);
        requireOwnership(inv != null ? inv.getShopId() : null);
        List<TiktokDocumentInvItem> items = invItemMapper.selectList(
                new LambdaQueryWrapper<TiktokDocumentInvItem>()
                        .eq(TiktokDocumentInvItem::getInvId, invId)
                        .orderByAsc(TiktokDocumentInvItem::getLineNo));
        try {
            writeResponse(response, buildInvWorkbook(inv, items), inv.getDocumentNo() + "-INV.xlsx");
        } catch (IOException e) {
            log.error("导出INV失败", e);
        }
    }

    private void writeInvExcel(OutputStream os, TiktokDocumentInv inv, List<TiktokDocumentInvItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_INV);

            // 字体
            XSSFFont fontCompany24 = wb.createFont();
            fontCompany24.setFontName("微软雅黑");
            fontCompany24.setFontHeightInPoints((short) 24);
            fontCompany24.setBold(true);

            XSSFFont fontAddr9 = wb.createFont();
            fontAddr9.setFontName("微软雅黑");
            fontAddr9.setFontHeightInPoints((short) 9);

            XSSFFont fontTitle20 = wb.createFont();
            fontTitle20.setFontName("微软雅黑");
            fontTitle20.setFontHeightInPoints((short) 20);
            fontTitle20.setBold(true);

            XSSFFont fontBold10 = wb.createFont();
            fontBold10.setFontName("微软雅黑");
            fontBold10.setFontHeightInPoints((short) 10);
            fontBold10.setBold(true);

            XSSFFont fontNormal10 = wb.createFont();
            fontNormal10.setFontName("微软雅黑");
            fontNormal10.setFontHeightInPoints((short) 10);

            XSSFFont fontNormal9 = wb.createFont();
            fontNormal9.setFontName("微软雅黑");
            fontNormal9.setFontHeightInPoints((short) 9);

            XSSFFont fontNormal12 = wb.createFont();
            fontNormal12.setFontName("微软雅黑");
            fontNormal12.setFontHeightInPoints((short) 12);

            XSSFFont fontBold12 = wb.createFont();
            fontBold12.setFontName("微软雅黑");
            fontBold12.setFontHeightInPoints((short) 12);
            fontBold12.setBold(true);

            // 样式
            CellStyle csCompany = wb.createCellStyle();
            csCompany.setFont(fontCompany24);
            csCompany.setAlignment(HorizontalAlignment.CENTER);
            csCompany.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCompany);

            CellStyle csAddrCenter = wb.createCellStyle();
            csAddrCenter.setFont(fontAddr9);
            csAddrCenter.setAlignment(HorizontalAlignment.CENTER);
            csAddrCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            csAddrCenter.setWrapText(true);
            setBorders(csAddrCenter);

            CellStyle csTitle = wb.createCellStyle();
            csTitle.setFont(fontTitle20);
            csTitle.setAlignment(HorizontalAlignment.CENTER);
            csTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csTitle);

            CellStyle csBold10 = wb.createCellStyle();
            csBold10.setFont(fontBold10);
            csBold10.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold10);

            CellStyle csBold10Left = wb.createCellStyle();
            csBold10Left.setFont(fontBold10);
            csBold10Left.setAlignment(HorizontalAlignment.LEFT);
            csBold10Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold10Left);

            CellStyle csNormal10Left = wb.createCellStyle();
            csNormal10Left.setFont(fontNormal10);
            csNormal10Left.setAlignment(HorizontalAlignment.LEFT);
            csNormal10Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal10Left);

            CellStyle csNormal9Left = wb.createCellStyle();
            csNormal9Left.setFont(fontNormal9);
            csNormal9Left.setAlignment(HorizontalAlignment.LEFT);
            csNormal9Left.setVerticalAlignment(VerticalAlignment.CENTER);
            csNormal9Left.setWrapText(true);
            setBorders(csNormal9Left);

            CellStyle csBold10CenterBorder = wb.createCellStyle();
            csBold10CenterBorder.setFont(fontBold10);
            csBold10CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csBold10CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold10CenterBorder);

            CellStyle csNormal12CenterBorder = wb.createCellStyle();
            csNormal12CenterBorder.setFont(fontNormal12);
            csNormal12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csNormal12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12CenterBorder);

            CellStyle csNormal10CenterBorder = wb.createCellStyle();
            csNormal10CenterBorder.setFont(fontNormal10);
            csNormal10CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csNormal10CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal10CenterBorder);

            CellStyle csBold12CenterBorder = wb.createCellStyle();
            csBold12CenterBorder.setFont(fontBold12);
            csBold12CenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csBold12CenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12CenterBorder);

            int rowIdx = 0;

            // Row1: 公司名称（A1:H1合并）
            Row row1 = sheet.createRow(rowIdx++);
            row1.setHeightInPoints(33f);
            createCell(row1, 0, safe(inv.getSellerName()), csCompany);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Row2: 地址（A2:H2合并）
            Row row2 = sheet.createRow(rowIdx++);
            row2.setHeightInPoints(28f);
            createCell(row2, 0, safe(inv.getSellerAddress()), csAddrCenter);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            // Row3-4: COMMERCIAL INVOICE（A3:H4合并）
            Row row3 = sheet.createRow(rowIdx++);
            row3.setHeightInPoints(28f);
            createCell(row3, 0, "COMMERCIAL INVOICE", csTitle);
            Row row4 = sheet.createRow(rowIdx++);
            row4.setHeightInPoints(28f);
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 7));

            // Row5: INVOICE NO: / 编号
            Row row5 = sheet.createRow(rowIdx++);
            row5.setHeightInPoints(28f);
            createCell(row5, 6, "INVOICE NO:", csBold10);
            createCell(row5, 7, safe(inv.getDocumentNo()), csNormal10Left);

            // Row6: DATE: / 日期
            Row row6 = sheet.createRow(rowIdx++);
            row6.setHeightInPoints(28f);
            createCell(row6, 6, "DATE:", csBold10Left);
            createCell(row6, 7, inv.getInvDate() != null ? inv.getInvDate().format(DATE_FMT) : "", csNormal10Left);

            // Row7: (FROM)SELLER / 卖方名 / (TO)BUYER / 买方名
            Row row7 = sheet.createRow(rowIdx++);
            row7.setHeightInPoints(28f);
            createCell(row7, 0, "(FROM)SELLER:", csBold10);
            createCell(row7, 1, safe(inv.getSellerName()), csNormal10Left);
            createCell(row7, 5, "(TO)BUYER:", csBold10);
            createCell(row7, 6, safe(inv.getBuyerName()), csNormal9Left);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 6, 7));

            // Row8: ADRESS / 卖方地址 / ADRESS / 买方地址
            Row row8 = sheet.createRow(rowIdx++);
            row8.setHeightInPoints(28f);
            createCell(row8, 0, "ADRESS:", csBold10);
            createCell(row8, 1, safe(inv.getSellerAddress()), csNormal9Left);
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 1, 4));
            createCell(row8, 5, "ADRESS:", csBold10);
            createCell(row8, 6, safe(inv.getBuyerAddress()), csNormal9Left);
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 6, 7));

            // Row9: TEL / 卖方电话 / TEL / 买方电话
            Row row9 = sheet.createRow(rowIdx++);
            row9.setHeightInPoints(28f);
            createCell(row9, 0, "TEL:", csBold10);
            createCell(row9, 1, safe(inv.getSellerPhone()), csNormal9Left);
            sheet.addMergedRegion(new CellRangeAddress(8, 8, 1, 4));
            createCell(row9, 5, "TEL:", csBold10);
            createCell(row9, 6, safe(inv.getBuyerPhone()), csNormal9Left);
            sheet.addMergedRegion(new CellRangeAddress(8, 8, 6, 7));

            // Row10: 空行
            sheet.createRow(rowIdx++);

            // Row11: 表头
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(30f);
            createCell(headerRow, 0, "No.", csBold10CenterBorder);
            createCell(headerRow, 1, "Description", csBold10CenterBorder);
            createCell(headerRow, 2, "", csBold10CenterBorder);
            createCell(headerRow, 3, "", csBold10CenterBorder);
            createCell(headerRow, 4, "", csBold10CenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));
            createCell(headerRow, 5, "Quantity\n（ctns/pcs）", csBold10CenterBorder);
            createCell(headerRow, 6, "Unit price", csBold10CenterBorder);
            createCell(headerRow, 7, "Total(" + safe(inv.getCurrency()) + ")", csBold10CenterBorder);

            // 数据行
            int seq = 1;
            for (TiktokDocumentInvItem item : items) {
                Row dataRow = sheet.createRow(rowIdx);
                dataRow.setHeightInPoints(28f);
                createCell(dataRow, 0, String.valueOf(seq++), csBold10CenterBorder);
                createCell(dataRow, 1, safe(item.getMsku()), csNormal12CenterBorder);
                createCell(dataRow, 2, "", csNormal12CenterBorder);
                createCell(dataRow, 3, "", csNormal12CenterBorder);
                createCell(dataRow, 4, "", csNormal12CenterBorder);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, 4));
                createCell(dataRow, 5, String.valueOf(item.getQuantity()), csNormal12CenterBorder);
                createCell(dataRow, 6, item.getUnitPrice() != null ? item.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal12CenterBorder);
                createCell(dataRow, 7, item.getAmount() != null ? item.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal10CenterBorder);
                rowIdx++;
            }

            // 合计行
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(28f);
            createCell(totalRow, 0, "CIF " + safe(inv.getCurrency()), csBold12CenterBorder);
            createCell(totalRow, 1, "TOTALS (" + safe(inv.getCurrency()) + ")", csBold10CenterBorder);
            createCell(totalRow, 2, "", csBold10CenterBorder);
            createCell(totalRow, 3, "", csBold10CenterBorder);
            createCell(totalRow, 4, "", csBold10CenterBorder);
            createCell(totalRow, 5, String.valueOf(inv.getTotalQuantity()), csNormal10CenterBorder);
            createCell(totalRow, 6, "", csNormal10CenterBorder);
            createCell(totalRow, 7, inv.getTotalAmount() != null ? inv.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00", csNormal10CenterBorder);
            rowIdx++;

            // 空行
            rowIdx++;

            // BANK INFORMATION
            Row bankTitleRow = sheet.createRow(rowIdx++);
            createCell(bankTitleRow, 0, "BANK INFORMATION:", csBold12CenterBorder);

            String[][] bankInfo = {
                    {"Account name:", safe(inv.getBankAccountName())},
                    {"account number: ", safe(inv.getBankAccountNumber())},
                    {"Bank Name:", safe(inv.getBankName())},
                    {"Bank address:", safe(inv.getBankAddress())},
                    {"Swift Code: ", safe(inv.getSwiftCode())}
            };
            for (String[] info : bankInfo) {
                Row bankRow = sheet.createRow(rowIdx);
                bankRow.setHeightInPoints(28f);
                createCell(bankRow, 0, info[0], csNormal9Left);
                createCell(bankRow, 1, info[1], csNormal9Left);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, 4));
                rowIdx++;
            }

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal10CenterBorder);

            // 自适应列宽（最小宽度：No=14, desc=10, col2=10, col3=8, col4=10, Qty=10, UnitPrice=10, Total=18）
            autoFitColumns(sheet, 8, new double[]{5, 12, 8, 7, 8, 10, 10, 14});

            wb.write(os);
        }
    }

    // ==================== 批量导出 ====================

    @Override
    public void batchExportPo(List<Long> poIds, HttpServletResponse response) {
        Long shopId = ShopContext.requireShopId();
        batchExport(response, "TK-PO批量导出.zip", poIds, "PO", id -> {
            TiktokDocumentPo po = poMapper.selectById(id);
            if (po == null || !shopId.equals(po.getShopId())) return null;
            List<TiktokDocumentPoItem> items = poItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentPoItem>()
                    .eq(TiktokDocumentPoItem::getPoId, id).orderByAsc(TiktokDocumentPoItem::getSortOrder));
            return buildPoWorkbook(po, items);
        });
    }

    @Override
    public void batchExportDn(List<Long> dnIds, HttpServletResponse response) {
        Long shopId = ShopContext.requireShopId();
        batchExport(response, "TK-DN批量导出.zip", dnIds, "DN", id -> {
            TiktokDocumentDn dn = dnMapper.selectById(id);
            if (dn == null || !shopId.equals(dn.getShopId())) return null;
            List<TiktokDocumentDnItem> items = dnItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentDnItem>()
                    .eq(TiktokDocumentDnItem::getDnId, id).orderByAsc(TiktokDocumentDnItem::getLineNo));
            return buildDnWorkbook(dn, items);
        });
    }

    @Override
    public void batchExportSettlement(List<Long> ids, HttpServletResponse response) {
        Long shopId = ShopContext.requireShopId();
        batchExport(response, "TK-结算单批量导出.zip", ids, "Settlement", id -> {
            TiktokDocumentSettlement s = settlementMapper.selectById(id);
            if (s == null || !shopId.equals(s.getShopId())) return null;
            List<TiktokDocumentSettlementItem> items = settlementItemMapper.selectList(
                    new LambdaQueryWrapper<TiktokDocumentSettlementItem>()
                            .eq(TiktokDocumentSettlementItem::getSettlementId, id)
                            .orderByAsc(TiktokDocumentSettlementItem::getLineNo));
            return buildSettlementWorkbook(s, items);
        });
    }

    @Override
    public void batchExportInv(List<Long> ids, HttpServletResponse response) {
        Long shopId = ShopContext.requireShopId();
        batchExport(response, "TK-INV批量导出.zip", ids, "INV", id -> {
            TiktokDocumentInv inv = invMapper.selectById(id);
            if (inv == null || !shopId.equals(inv.getShopId())) return null;
            List<TiktokDocumentInvItem> items = invItemMapper.selectList(
                    new LambdaQueryWrapper<TiktokDocumentInvItem>()
                            .eq(TiktokDocumentInvItem::getInvId, id)
                            .orderByAsc(TiktokDocumentInvItem::getLineNo));
            return buildInvWorkbook(inv, items);
        });
    }

    // ==================== buildXxxWorkbook 方法（供批量导出使用） ====================

    private WorkbookResult buildPoWorkbook(TiktokDocumentPo po, List<TiktokDocumentPoItem> items) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writePoExcel(bos, po, items);
            return new WorkbookResult(bos.toByteArray(), po.getDocumentNo() + "-PO.xlsx");
        }
    }

    private WorkbookResult buildDnWorkbook(TiktokDocumentDn dn, List<TiktokDocumentDnItem> items) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeDnExcel(bos, dn, items);
            return new WorkbookResult(bos.toByteArray(), dn.getDocumentNo() + "-DN.xlsx");
        }
    }

    private WorkbookResult buildSettlementWorkbook(TiktokDocumentSettlement s, List<TiktokDocumentSettlementItem> items) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeSettlementExcel(bos, s, items);
            return new WorkbookResult(bos.toByteArray(), s.getDocumentNo() + "-结算单.xlsx");
        }
    }

    private WorkbookResult buildInvWorkbook(TiktokDocumentInv inv, List<TiktokDocumentInvItem> items) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeInvExcel(bos, inv, items);
            return new WorkbookResult(bos.toByteArray(), inv.getDocumentNo() + "-INV.xlsx");
        }
    }

    // ==================== 私有辅助方法 ====================

    private void requireOwnership(Long dataShopId) {
        Long currentShopId = ShopContext.requireShopId();
        if (dataShopId == null || !currentShopId.equals(dataShopId)) {
            throw new IllegalStateException("单据不存在或无权操作");
        }
    }

    private void writeResponse(HttpServletResponse response, WorkbookResult result, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        try (OutputStream os = response.getOutputStream()) {
            os.write(result.data());
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        WorkbookResult build(Long id) throws IOException;
    }

    private record WorkbookResult(byte[] data, String fileName) {}

    private void batchExport(HttpServletResponse response, String zipName, List<Long> ids, String type, WorkbookBuilder builder) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(zipName, StandardCharsets.UTF_8));
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Long id : ids) {
                WorkbookResult result = builder.build(id);
                if (result != null) {
                    zos.putNextEntry(new ZipEntry(result.fileName()));
                    zos.write(result.data());
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出{}失败", type, e);
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void fillMissingMergeCells(Sheet sheet, CellStyle style) {
        sheet.setDisplayGridlines(false);
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    row = sheet.createRow(r);
                }
                for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                    if (row.getCell(c) == null) {
                        Cell cell = row.createCell(c);
                        cell.setCellStyle(style);
                    }
                }
            }
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * 安全获取单元格字符串值，处理各种单元格类型
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    /**
     * 自适应列宽：遍历所有行计算最大内容宽度，跳过合并单元格区域
     *
     * @param sheet      工作表
     * @param numColumns 列数
     * @param minWidths  每列最小宽度（字符数），null则不限制
     */
    private void autoFitColumns(Sheet sheet, int numColumns, double[] minWidths) {
        // 收集所有合并区域，用于判断某个单元格是否在合并区域内（非首列）
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

        for (int col = 0; col < numColumns; col++) {
            double maxWidth = minWidths != null && col < minWidths.length ? minWidths[col] : 8.0;

            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                Cell cell = row.getCell(col);
                if (cell == null) continue;

                // 跳过合并区域中非首列的单元格
                if (isInMergedRegionNotFirstCol(mergedRegions, rowIdx, col)) continue;

                String value = getCellStringValue(cell);
                if (value == null || value.isEmpty()) continue;

                // 计算内容宽度：中文字符算2个宽度单位，英文算1个
                double contentWidth = calculateStringWidth(value);

                // 如果单元格在合并区域中，按合并的列数分摊宽度
                int mergedColSpan = getMergedColSpan(mergedRegions, rowIdx, col);
                if (mergedColSpan > 1) {
                    contentWidth = contentWidth / mergedColSpan;
                }

                // 字体大小补偿约15%
                contentWidth = contentWidth * 1.15;

                if (contentWidth > maxWidth) {
                    maxWidth = contentWidth;
                }
            }

            // 加1字符padding，上限38字符防止列过宽导致打印超幅
            int width = (int) ((maxWidth + 1) * 256);
            if (width > 38 * 256) width = 38 * 256;
            sheet.setColumnWidth(col, width);
        }
    }

    /**
     * 计算字符串显示宽度：中文/全角字符算2，其他算1
     */
    private double calculateStringWidth(String value) {
        // 处理多行内容，取最长行
        String[] lines = value.split("\n");
        double maxLineWidth = 0;
        for (String line : lines) {
            double lineWidth = 0;
            for (char c : line.toCharArray()) {
                if (c >= '\u4e00' && c <= '\u9fff' || c >= '\u3000' && c <= '\u303f'
                        || c >= '\uff00' && c <= '\uffef') {
                    lineWidth += 2;
                } else {
                    lineWidth += 1;
                }
            }
            if (lineWidth > maxLineWidth) maxLineWidth = lineWidth;
        }
        return maxLineWidth;
    }

    /**
     * 判断单元格是否在合并区域中且不是该区域的首列
     */
    private boolean isInMergedRegionNotFirstCol(List<CellRangeAddress> regions, int row, int col) {
        for (CellRangeAddress region : regions) {
            if (row >= region.getFirstRow() && row <= region.getLastRow()
                    && col > region.getFirstColumn() && col <= region.getLastColumn()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取单元格所在合并区域的列跨度，不在合并区域返回1
     */
    private int getMergedColSpan(List<CellRangeAddress> regions, int row, int col) {
        for (CellRangeAddress region : regions) {
            if (row >= region.getFirstRow() && row <= region.getLastRow()
                    && col >= region.getFirstColumn() && col <= region.getLastColumn()) {
                return region.getLastColumn() - region.getFirstColumn() + 1;
            }
        }
        return 1;
    }
}
