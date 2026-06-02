package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.DocumentExportService;
import com.musheng.business.document.service.DocumentPartyConfigService;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.common.service.SysConfigService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Excel导出服务实现类 - 使用Apache POI精确复刻原始样本格式
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class DocumentExportServiceImpl implements DocumentExportService {

    @Autowired
    private DocumentPoMapper documentPoMapper;
    @Autowired
    private DocumentPoItemMapper documentPoItemMapper;
    @Autowired
    private DocumentDnMapper documentDnMapper;
    @Autowired
    private DocumentDnItemMapper documentDnItemMapper;
    @Autowired
    private DocumentSettlementMapper documentSettlementMapper;
    @Autowired
    private DocumentSettlementItemMapper documentSettlementItemMapper;
    @Autowired
    private DocumentInvMapper documentInvMapper;
    @Autowired
    private DocumentInvItemMapper documentInvItemMapper;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private DocumentPartyConfigService documentPartyConfigService;
    @Autowired
    private MarketplaceMapper marketplaceMapper;

    /**
     * 根据货币代码动态查询对应的站点代码（从 t_marketplace 获取，不依赖枚举）
     *
     * @param currencyCode 货币代码（如 USD/GBP/CAD/EUR）
     * @return 站点代码（如 US/UK/CA/EU），未找到时返回原值
     */
    private String resolveSiteCodeByCurrency(String currencyCode) {
        if (!org.springframework.util.StringUtils.hasText(currencyCode)) {
            return currencyCode;
        }
        Marketplace marketplace = marketplaceMapper.selectOne(
                new LambdaQueryWrapper<Marketplace>()
                        .eq(Marketplace::getCurrencyCode, currencyCode)
                        .eq(Marketplace::getStatus, 1)
                        .last("LIMIT 1"));
        return marketplace != null ? marketplace.getSiteCode() : currencyCode;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/M/d");

    /** 图片资源路径 */
    private static final String IMG_COMPANY_LOGO = "templates/document/company_logo.png";
    /** 慕声红章（甲方/买方），909x918px，打印尺寸2.1×2.1cm（小章） */
    private static final String IMG_STAMP_MUSHENG = "templates/document/stamp_musheng.png";
    /** 香港蓝章（乙方/卖方），485x485px，打印尺寸4×4cm（大章） */
    private static final String IMG_STAMP_HK = "templates/document/stamp_hk.png";

    /** Sheet页名称 - 与标准样本完全一致 */
    private static final String SHEET_NAME_PO = "1、PO（店-香）";
    private static final String SHEET_NAME_DN = "2、送货单（香-店）";
    private static final String SHEET_NAME_SETTLEMENT = "3、结算单（店-香）";
    private static final String SHEET_NAME_INV = "4、形式发票（香-店）";


    // ==================== 导出PO ====================

    /**
     * 导出PO为Excel
     *
     * @param poId PO主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public void exportPo(Long poId, HttpServletResponse response) {
        log.info("导出PO Excel, poId={}", poId);
        DocumentPo po = documentPoMapper.selectById(poId);
        if (po == null) {
            throw new RuntimeException("PO不存在, poId=" + poId);
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(po.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }
        // 实时用最新配置覆盖交易方字段
        if (org.springframework.util.StringUtils.hasText(po.getSiteCode())) {
            try {
                DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(po.getSiteCode());
                po.setBuyerName(party.getBuyerName());
                po.setBuyerAddress(party.getBuyerAddress());
                po.setSellerName(party.getSellerName());
            } catch (Exception e) {
                log.warn("PO导出：获取交易方配置失败，使用单据原始值，siteCode={}", po.getSiteCode());
            }
        }
        List<DocumentPoItem> items = documentPoItemMapper.selectList(
                new LambdaQueryWrapper<DocumentPoItem>()
                        .eq(DocumentPoItem::getPoId, poId)
                        .orderByAsc(DocumentPoItem::getSortOrder));
        String fileName = generatePoFileName(po.getDocumentNo(), po.getBuyerName(), po.getSellerName());
        try {
            setExcelResponseHeaders(response, fileName);
            writePoExcel(response.getOutputStream(), po, items);
        } catch (IOException e) {
            log.error("PO导出失败, poId={}", poId, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 写入PO Excel - 严格按照原始样本格式
     * 布局：A1:A3左侧空白合并, B1:D1公司名22pt粗体, B2:D2地址14pt, B3:D3 "Purchase Order" 22pt粗体
     * Row4: Purchasing object / 买方名称(B4:D4合并)
     * Row5: Contract No. / 编号 / Date: / 日期
     * Row6: No# / Description / Q'ty / Address (表头)
     * Row7+: 数据行（货件编号/MSKU/数量/FBA地址）
     * 最后行: 合计Total(A:B合并) / 总数量
     *
     * @param os 输出流
     * @param po PO主表
     * @param items PO明细列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void writePoExcel(OutputStream os, DocumentPo po, List<DocumentPoItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_PO);

            // 列宽（单位：1/256字符宽度）
            sheet.setColumnWidth(0, (int) (21.5 * 256));
            sheet.setColumnWidth(1, (int) (49.75 * 256));
            sheet.setColumnWidth(2, (int) (26.27 * 256));
            sheet.setColumnWidth(3, (int) (46.5 * 256));

            // 字体定义
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

            // 样式：公司名（22pt粗体居中）
            CellStyle csCompany = wb.createCellStyle();
            csCompany.setFont(fontCompany);
            csCompany.setAlignment(HorizontalAlignment.CENTER);
            csCompany.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCompany);

            // 样式：地址（14pt居中）
            CellStyle csAddress = wb.createCellStyle();
            csAddress.setFont(fontAddress);
            csAddress.setAlignment(HorizontalAlignment.CENTER);
            csAddress.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csAddress);

            // 样式：普通居中（12pt）
            CellStyle csCenter = wb.createCellStyle();
            csCenter.setFont(fontNormal);
            csCenter.setAlignment(HorizontalAlignment.CENTER);
            csCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            // 样式：普通居中+边框
            CellStyle csCenterBorder = wb.createCellStyle();
            csCenterBorder.setFont(fontNormal);
            csCenterBorder.setAlignment(HorizontalAlignment.CENTER);
            csCenterBorder.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csCenterBorder);

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

            // 样式：地址列（带自动换行，防止列过宽）
            CellStyle csAddressWrap = wb.createCellStyle();
            csAddressWrap.setFont(fontNormal);
            csAddressWrap.setAlignment(HorizontalAlignment.CENTER);
            csAddressWrap.setVerticalAlignment(VerticalAlignment.CENTER);
            csAddressWrap.setWrapText(true);
            setBorders(csAddressWrap);

            // Row4: Purchasing object / 买方名称（B4:D4合并）
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

            // Row6: 表头 No# / Description / Q'ty / Address
            Row row6 = sheet.createRow(rowIdx++);
            row6.setHeightInPoints(23f);
            createCell(row6, 0, "No#", csCenterBorder);
            createCell(row6, 1, "Description", csAddressWrap);
            createCell(row6, 2, "Q'ty", csCenterBorder);
            createCell(row6, 3, "Address", csAddressWrap);

            // 数据行
            int dataStartRow = rowIdx;
            for (DocumentPoItem item : items) {
                Row dataRow = sheet.createRow(rowIdx);
                dataRow.setHeightInPoints(23f);
                createCell(dataRow, 0, safe(item.getShipmentNo()), csCenterBorder);
                // Description和Address列使用自动换行样式，防止内容被截断
                createCell(dataRow, 1, safe(item.getMsku()), csAddressWrap);
                createCell(dataRow, 2, String.valueOf(item.getQuantity()), csCenterBorder);
                createCell(dataRow, 3, safe(item.getFbaAddress()), csAddressWrap);
                rowIdx++;
            }

            // 如果有多行同一货件地址，合并D列（地址列）
            // 原始样本中D7:D12是合并的，这里按货件编号分组合并
            mergeAddressColumn(sheet, items, dataStartRow);

            // 合计行
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(31f);
            createCell(totalRow, 0, "合计Total", csCenterBorder);
            createCell(totalRow, 1, "", csCenterBorder);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            createCell(totalRow, 2, String.valueOf(po.getTotalQuantity()), csCenterBorder);
            createCell(totalRow, 3, "", csCenterBorder);

            // 嵌入公司Logo图片到A1:A3区域
            if (isStampEnabled()) {
                addImage(wb, sheet, IMG_COMPANY_LOGO, 0, 76200, 0, 19050, 0, 1571625, 2, 457200);
            }

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csCenterBorder);

            // 自适应列宽（最小宽度：No#=18, Description=30, Q'ty=10, Address=30）
            autoFitColumns(sheet, 4, new double[]{16, 28, 10, 30});
            // Address列限制最大宽度40字符，超出部分自动换行
            int addressColWidth = sheet.getColumnWidth(3);
            if (addressColWidth > 40 * 256) {
                sheet.setColumnWidth(3, 40 * 256);
            }
            // 调整包含地址的行高，为换行文本留出空间
            autoSizeContentRows(sheet, items, dataStartRow);

            wb.write(os);
        }
    }

    /**
     * 自动调整含地址列的换行行高 - 同一货件编号的地址只在首行显示，其余行合并
     *
     * @param sheet 工作表
     * @param items PO明细列表
     * @param dataStartRow 数据起始行号
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void mergeAddressColumn(Sheet sheet, List<DocumentPoItem> items, int dataStartRow) {
        if (items.size() <= 1) {
            return;
        }
        int mergeStart = dataStartRow;
        String currentShipment = items.get(0).getShipmentNo();
        for (int i = 1; i < items.size(); i++) {
            String shipment = items.get(i).getShipmentNo();
            if (shipment != null && shipment.equals(currentShipment)) {
                continue;
            }
            // 当货件编号变化时，合并之前的区域
            if (dataStartRow + i - 1 > mergeStart) {
                sheet.addMergedRegion(new CellRangeAddress(mergeStart, dataStartRow + i - 1, 3, 3));
            }
            mergeStart = dataStartRow + i;
            currentShipment = shipment;
        }
        // 最后一组
        if (dataStartRow + items.size() - 1 > mergeStart) {
            sheet.addMergedRegion(new CellRangeAddress(mergeStart, dataStartRow + items.size() - 1, 3, 3));
        }
    }


    /**
     * 自动调整含地址列的换行行高：根据地址文本长度和列宽估算需要几行，
     * 对于内容较长的地址行适当增加行高
     */
    private void autoSizeContentRows(Sheet sheet, List<DocumentPoItem> items, int dataStartRow) {
        double descColChars = sheet.getColumnWidth(1) / 256.0;
        double addrColChars = sheet.getColumnWidth(3) / 256.0;
        if (descColChars < 20) descColChars = 20;
        if (addrColChars < 20) addrColChars = 20;
        for (int i = 0; i < items.size(); i++) {
            Row row = sheet.getRow(dataStartRow + i);
            if (row == null) continue;
            int maxLines = 1;
            // Description列换行估算
            String desc = items.get(i).getMsku();
            if (desc != null && !desc.isEmpty()) {
                int lines = (int) Math.ceil(calculateStringWidth(desc) / (descColChars * 0.7));
                if (lines > maxLines) maxLines = lines;
            }
            // Address列换行估算
            String addr = items.get(i).getFbaAddress();
            if (addr != null && !addr.isEmpty()) {
                int lines = (int) Math.ceil(calculateStringWidth(addr) / (addrColChars * 0.7));
                if (lines > maxLines) maxLines = lines;
            }
            if (maxLines > 1) {
                row.setHeightInPoints(Math.max(row.getHeightInPoints(), maxLines * 23f));
            }
        }
    }

    // ==================== 导出DN ====================

    /**
     * 导出DN为Excel
     *
     * @param dnId DN主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public void exportDn(Long dnId, HttpServletResponse response) {
        log.info("导出DN Excel, dnId={}", dnId);
        DocumentDn dn = documentDnMapper.selectById(dnId);
        if (dn == null) {
            throw new RuntimeException("DN不存在, dnId=" + dnId);
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        log.info("[exportDn] dnId={}, 请求shopId={}, 数据库shopId={}", dnId, shopId, dn.getShopId());
        if (!shopId.equals(dn.getShopId())) {
            log.warn("[exportDn] 权限校验失败: 请求shopId={} != 数据库shopId={}, dnId={}", shopId, dn.getShopId(), dnId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }
        // 实时用最新配置覆盖交易方字段
        if (org.springframework.util.StringUtils.hasText(dn.getSiteCode())) {
            try {
                DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(dn.getSiteCode());
                dn.setSupplierName(party.getSupplierName());
                dn.setCustomerName(party.getCustomerNameTc());
            } catch (Exception e) {
                log.warn("DN导出：获取交易方配置失败，使用单据原始值，siteCode={}", dn.getSiteCode());
            }
        }
        List<DocumentDnItem> items = documentDnItemMapper.selectList(
                new LambdaQueryWrapper<DocumentDnItem>()
                        .eq(DocumentDnItem::getDnId, dnId)
                        .orderByAsc(DocumentDnItem::getLineNo));
        String fileName = generateDnFileName(dn.getDocumentNo(), dn.getSupplierName(), dn.getCustomerName());
        try {
            setExcelResponseHeaders(response, fileName);
            writeDnExcel(response.getOutputStream(), dn, items);
        } catch (IOException e) {
            log.error("DN导出失败, dnId={}", dnId, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 写入DN Excel - 严格按照原始样本格式
     * 布局：A1:F1公司名20pt粗体宋体, A2:F2 "送貨清單" 20pt粗体
     * Row3: 客戶名稱 / 客户名(C3:F3合并)
     * Row4: 送貨日期 / 日期 / 送貨單號 / 编号
     * Row5: No / 產品名稱(B:C合并) / 數量 / 備註(E:F合并) 表头
     * Row6+: 数据行
     * 合计行: 數量合計(A:C合并) / 总数量
     * 特别说明行 / 签收行
     *
     * @param os 输出流
     * @param dn DN主表
     * @param items DN明细列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void writeDnExcel(OutputStream os, DocumentDn dn, List<DocumentDnItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_DN);

            // 列宽
            sheet.setColumnWidth(0, (int) (7.83 * 256));
            sheet.setColumnWidth(1, (int) (18.75 * 256));
            sheet.setColumnWidth(2, (int) (27.93 * 256));
            sheet.setColumnWidth(3, (int) (20.98 * 256));
            sheet.setColumnWidth(4, (int) (21.5 * 256));
            sheet.setColumnWidth(5, (int) (16.63 * 256));

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

            XSSFFont fontNote10 = wb.createFont();
            fontNote10.setFontName("宋体");
            fontNote10.setFontHeightInPoints((short) 10);
            fontNote10.setBold(true);

            // 样式
            CellStyle csTitle = wb.createCellStyle();
            csTitle.setFont(fontTitle);
            csTitle.setAlignment(HorizontalAlignment.CENTER);
            csTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csTitle);

            CellStyle csBold12Left = wb.createCellStyle();
            csBold12Left.setFont(fontBold12);
            csBold12Left.setAlignment(HorizontalAlignment.LEFT);
            csBold12Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12Left);

            CellStyle csBold12Center = wb.createCellStyle();
            csBold12Center.setFont(fontBold12);
            csBold12Center.setAlignment(HorizontalAlignment.CENTER);
            csBold12Center.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12Center);

            CellStyle csBold12Right = wb.createCellStyle();
            csBold12Right.setFont(fontBold12);
            csBold12Right.setAlignment(HorizontalAlignment.RIGHT);
            csBold12Right.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold12Right);

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

            CellStyle csSign = wb.createCellStyle();
            csSign.setFont(fontNote10);
            csSign.setAlignment(HorizontalAlignment.LEFT);
            csSign.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csSign);

            int rowIdx = 0;

            // Row1: 公司名称（A1:F1合并）
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

            // Row5: 表头 No / 產品名稱(B:C合并) / 數量 / 備註(E:F合并)
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
            for (DocumentDnItem item : items) {
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
            createCell(totalRow, 3, String.valueOf(dn.getTotalQuantity()), csNormal12CenterBorder);
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
            createCell(signRow1, 0, "收貨單位及經手人(Receiving unit and handler):", csSign);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
            rowIdx++;

            Row signRow2 = sheet.createRow(rowIdx);
            signRow2.setHeightInPoints(25f);
            createCell(signRow2, 0, "送貨單位及經手人(Shipping company and the person in charge of the delivery):", csSign);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));

            // 嵌入印章图片 - 慕声红章2.1×2.1cm（小章），香港蓝章4×4cm（大章）
            if (isStampEnabled()) {
                int dnSignRow = rowIdx;  // 送貨單位行
                // 慕声红章（收貨單位）：2.1×2.1cm = 756000 EMU
                addImageOriginal(wb, sheet, IMG_STAMP_MUSHENG,
                        4, 742950, dnSignRow - 1, 123825,
                        5, 742950, 3, 123825,
                        756000, 756000);
                // 香港蓝章（送貨單位）：4×4cm = 1440000 EMU
                addImageOriginal(wb, sheet, IMG_STAMP_HK,
                        3, 762000, dnSignRow - 2, 200000,
                        4, 762000, 5, 200000,
                        1440000, 1440000);
            }

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal12CenterBorder);

            // 自适应列宽（最小宽度：No=6, 产品名=18, col2=18, 数量=10, 备注=20, col5=12）
            autoFitColumns(sheet, 6, new double[]{5, 15, 15, 8, 16, 10});

            wb.write(os);
        }
    }


    // ==================== 导出结算单 ====================

    /**
     * 导出结算单为Excel
     *
     * @param settlementId 结算单主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public void exportSettlement(Long settlementId, HttpServletResponse response) {
        log.info("导出结算单 Excel, settlementId={}", settlementId);
        DocumentSettlement settlement = documentSettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在, settlementId=" + settlementId);
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(settlement.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }
        // 实时用最新配置覆盖交易方字段
        // 结算单 siteCode 存的是货币代码（USD/GBP/CAD/EUR），通过 t_marketplace 动态转换为站点代码
        if (org.springframework.util.StringUtils.hasText(settlement.getSiteCode())) {
            try {
                String resolvedSiteCode = resolveSiteCodeByCurrency(settlement.getSiteCode());
                DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(resolvedSiteCode);
                settlement.setBuyerName(party.getBuyerName());
                settlement.setBuyerAddress(party.getBuyerAddress());
                settlement.setSellerName(party.getSellerName());
            } catch (Exception e) {
                log.warn("结算单导出：获取交易方配置失败，使用单据原始值，siteCode={}", settlement.getSiteCode());
            }
        }
        List<DocumentSettlementItem> items = documentSettlementItemMapper.selectList(
                new LambdaQueryWrapper<DocumentSettlementItem>()
                        .eq(DocumentSettlementItem::getSettlementId, settlementId)
                        .orderByAsc(DocumentSettlementItem::getMsku));
        String fileName = generateSettlementFileName(settlement.getDocumentNo(), settlement.getBuyerName(), settlement.getSellerName());
        try {
            setExcelResponseHeaders(response, fileName);
            writeSettlementExcel(response.getOutputStream(), settlement, items);
        } catch (IOException e) {
            log.error("结算单导出失败, settlementId={}", settlementId, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 写入结算单Excel - 严格按照原始样本格式
     * 布局：A1:A3左侧空白合并, B1:F1公司名22pt粗体, B2:F2地址14pt, B3:F3 "Statement of Account" 22pt粗体
     * Row4: Purchasing object / 买方名称(B4:F4合并)
     * Row5: Settlement Period / 周期 / Settlement No. / 编号
     * Row6: Settlement frequency / Monthly / Settlement date / 日期
     * Row7: National site / 站点代码(B7:F7合并)
     * Row8: No# / Description / Currency / Unit price / Q'ty / Amount (表头)
     * Row9+: 数据行
     * 合计行: 合计Total(A:D合并) / 总数量 / 总金额
     * 签章行
     *
     * @param os 输出流
     * @param s 结算单主表
     * @param items 结算单明细列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void writeSettlementExcel(OutputStream os, DocumentSettlement s, List<DocumentSettlementItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_SETTLEMENT);

            // 列宽
            sheet.setColumnWidth(0, (int) (23.0 * 256));
            sheet.setColumnWidth(1, (int) (57.63 * 256));
            sheet.setColumnWidth(2, (int) (17.88 * 256));
            sheet.setColumnWidth(3, (int) (16.25 * 256));
            sheet.setColumnWidth(4, (int) (19.38 * 256));
            sheet.setColumnWidth(5, (int) (15.5 * 256));

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

            XSSFFont fontSign14 = wb.createFont();
            fontSign14.setFontName("宋体");
            fontSign14.setFontHeightInPoints((short) 14);
            fontSign14.setBold(true);

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

            CellStyle csNormal12Left = wb.createCellStyle();
            csNormal12Left.setFont(fontNormal12);
            csNormal12Left.setAlignment(HorizontalAlignment.LEFT);
            csNormal12Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12Left);

            CellStyle csNormal12Right = wb.createCellStyle();
            csNormal12Right.setFont(fontNormal12);
            csNormal12Right.setAlignment(HorizontalAlignment.RIGHT);
            csNormal12Right.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12Right);

            CellStyle csNormal12Center = wb.createCellStyle();
            csNormal12Center.setFont(fontNormal12);
            csNormal12Center.setAlignment(HorizontalAlignment.CENTER);
            csNormal12Center.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csNormal12Center);

            CellStyle csSign = wb.createCellStyle();
            csSign.setFont(fontSign14);
            csSign.setAlignment(HorizontalAlignment.LEFT);
            csSign.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csSign);

            CellStyle csBold14Left = wb.createCellStyle();
            csBold14Left.setFont(fontBold14);
            csBold14Left.setAlignment(HorizontalAlignment.LEFT);
            csBold14Left.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(csBold14Left);

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
            createCell(row5, 1, formatPeriod(s.getPeriodStart(), s.getPeriodEnd()), csNormal12Center);
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

            // Row7: National site / 站点代码（B7:F7合并）
            Row row7 = sheet.createRow(rowIdx++);
            row7.setHeightInPoints(39.5f);
            createCell(row7, 0, "National site:", csNormal12CenterBorder);
            createCell(row7, 1, safe(s.getSiteCode()), csNormal12Center);
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
            for (DocumentSettlementItem item : items) {
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

            // 确保签章跨越的行都存在且有足够行高（标准样本每行41pt）
            for (int r = rowIdx + 1; r <= rowIdx + 3; r++) {
                Row paddingRow = sheet.getRow(r);
                if (paddingRow == null) {
                    paddingRow = sheet.createRow(r);
                }
                paddingRow.setHeightInPoints(41f);
            }

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal12CenterBorder);

            // 列宽自适应（在嵌入印章之前调用，印章使用 oneCellAnchor 定位，尺寸不受列宽影响）
            // 最小宽度：No#=7, Description=26, Currency=10, Unit price=12, Q'ty=8, Amount=12
            autoFitColumns(sheet, 6, new double[]{7, 26, 10, 12, 8, 12});

            // 嵌入图片 - 慕声红章2.1×2.1cm（小章），香港蓝章4×4cm（大章）
            if (isStampEnabled()) {
                // 公司Logo: 标准样本 from(0,219075,0,19050) to(0,1543050,2,447675) ext(1333500,1343025)
                addImageOriginal(wb, sheet, IMG_COMPANY_LOGO,
                        0, 219075, 0, 19050,
                        0, 1543050, 2, 447675,
                        1333500, 1343025);
                // 慕声红章（卖方确认区域）：2.1×2.1cm = 756000 EMU
                addImageOriginal(wb, sheet, IMG_STAMP_MUSHENG,
                        1, 1685925, rowIdx, 28575,
                        2, 1685925, 2, 28575,
                        756000, 756000);
                // 香港蓝章（买方确认区域）：4×4cm = 1440000 EMU
                addImageOriginal(wb, sheet, IMG_STAMP_HK,
                        4, 447675, rowIdx - 1, 171450,
                        5, 447675, 3, 171450,
                        1440000, 1440000);
            }

            wb.write(os);
        }
    }


    // ==================== 导出INV ====================

    /**
     * 导出INV为Excel
     *
     * @param invId INV主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public void exportInv(Long invId, HttpServletResponse response) {
        log.info("导出INV Excel, invId={}", invId);
        DocumentInv inv = documentInvMapper.selectById(invId);
        if (inv == null) {
            throw new RuntimeException("INV不存在, invId=" + invId);
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(inv.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }
        // 实时用最新配置覆盖交易方及银行字段
        // INV 的 siteCode 来自结算单，存的是货币代码（USD/GBP/CAD/EUR），通过 t_marketplace 动态转换为站点代码
        if (org.springframework.util.StringUtils.hasText(inv.getSiteCode())) {
            try {
                String resolvedSiteCode = resolveSiteCodeByCurrency(inv.getSiteCode());
                DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(resolvedSiteCode);
                inv.setSellerName(party.getSellerName());
                inv.setSellerAddress(party.getSellerAddress());
                inv.setSellerPhone(party.getSellerPhone());
                inv.setBuyerName(party.getBuyerNameEn());
                inv.setBuyerAddress(party.getBuyerAddress());
                inv.setBuyerPhone(party.getBuyerPhone());
                inv.setBankAccountName(party.getBankAccountName());
                inv.setBankAccountNumber(party.getBankAccountNumber());
                inv.setBankName(party.getBankName());
                inv.setBankAddress(party.getBankAddress());
                inv.setSwiftCode(party.getSwiftCode());
            } catch (Exception e) {
                log.warn("INV导出：获取交易方配置失败，使用单据原始值，siteCode={}", inv.getSiteCode());
            }
        }
        List<DocumentInvItem> items = documentInvItemMapper.selectList(
                new LambdaQueryWrapper<DocumentInvItem>()
                        .eq(DocumentInvItem::getInvId, invId)
                        .orderByAsc(DocumentInvItem::getLineNo));
        String fileName = generateInvFileName(inv.getDocumentNo(), inv.getSellerName(), inv.getBuyerName());
        try {
            setExcelResponseHeaders(response, fileName);
            writeInvExcel(response.getOutputStream(), inv, items);
        } catch (IOException e) {
            log.error("INV导出失败, invId={}", invId, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 写入INV Excel - 严格按照原始样本格式
     * 布局：A1:H1公司名24pt粗体, A2:H2地址9pt, A3:H4 "COMMERCIAL INVOICE" 20pt粗体
     * Row5: 右侧 INVOICE NO: / 编号(G5:H5)
     * Row6: 右侧 DATE: / 日期(G6:H6)
     * Row7: (FROM)SELLER: / 卖方名 / (TO)BUYER: / 买方名(G7:H7)
     * Row8: ADRESS: / 卖方地址(B8:E8) / ADRESS: / 买方地址(G8:H8)
     * Row9: TEL: / 卖方电话(B9:E9) / TEL: / 买方电话(G9:H9)
     * Row11: No. / Description(B:E合并) / Quantity / Unit price / Total 表头
     * Row12+: 数据行
     * 合计行: CIF USD / TOTALS / 总数量 / 总金额
     * 空行后: BANK INFORMATION
     *
     * @param os 输出流
     * @param inv INV主表
     * @param items INV明细列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void writeInvExcel(OutputStream os, DocumentInv inv, List<DocumentInvItem> items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME_INV);

            // 列宽
            sheet.setColumnWidth(0, (int) (16.75 * 256));
            sheet.setColumnWidth(1, (int) (10.25 * 256));
            sheet.setColumnWidth(2, (int) (12.38 * 256));
            sheet.setColumnWidth(3, (int) (7.75 * 256));
            sheet.setColumnWidth(4, (int) (12.0 * 256));
            sheet.setColumnWidth(5, (int) (11.75 * 256));
            sheet.setColumnWidth(6, (int) (11.38 * 256));
            sheet.setColumnWidth(7, (int) (21.38 * 256));

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
            createCell(headerRow, 7, "Total(" + safe(inv.getSiteCode()) + ")", csBold10CenterBorder);

            // 数据行
            int seq = 1;
            for (DocumentInvItem item : items) {
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
            createCell(totalRow, 0, "CIF " + safe(inv.getSiteCode()), csBold12CenterBorder);
            createCell(totalRow, 1, "TOTALS (" + safe(inv.getSiteCode()) + ")", csBold10CenterBorder);
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

            // 银行信息行
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

            // 确保签章跨越的行都存在且有足够行高（标准样本每行28pt）
            // 签章 from row = rowIdx-5, rowSpan=2, 即跨越 rowIdx-5 到 rowIdx-3
            for (int r = rowIdx; r <= rowIdx + 2; r++) {
                Row paddingRow = sheet.getRow(r);
                if (paddingRow == null) {
                    paddingRow = sheet.createRow(r);
                }
                paddingRow.setHeightInPoints(28f);
            }

            // 填补合并区域中缺失的单元格，避免虚线
            fillMissingMergeCells(sheet, csNormal10CenterBorder);

            // 列宽自适应（INV 有8列 A-H）
            // 最小宽度：No.=5, Desc=12, col2=8, col3=7, col4=8, Qty=10, UnitPrice=10, Total=14
            autoFitColumns(sheet, 8, new double[]{5, 12, 8, 7, 8, 10, 10, 14});

            // 嵌入印章图片 - 慕声红章2.1×2.1cm = 756000 EMU
            if (isStampEnabled()) {
                addImageOriginal(wb, sheet, IMG_STAMP_MUSHENG,
                        5, 552450, rowIdx - 5, 285750,
                        7, 552450, 3, 285750,
                        756000, 756000);
            }

            wb.write(os);
        }
    }


    // ==================== 批量导出 ====================

    /**
     * 批量导出一个结算周期的结算单和INV，打包为ZIP
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public void batchExportByPeriod(LocalDate periodStart, LocalDate periodEnd, HttpServletResponse response) {
        log.info("批量导出结算周期文件, periodStart={}, periodEnd={}", periodStart, periodEnd);

        Long shopId = com.musheng.common.context.ShopContext.requireShopId();

        // 查询该周期的结算单（限当前店铺）
        List<DocumentSettlement> settlements = documentSettlementMapper.selectList(
                new LambdaQueryWrapper<DocumentSettlement>()
                        .eq(DocumentSettlement::getShopId, shopId)
                        .eq(DocumentSettlement::getPeriodStart, periodStart)
                        .eq(DocumentSettlement::getPeriodEnd, periodEnd));
        if (CollectionUtils.isEmpty(settlements)) {
            throw new RuntimeException("该结算周期无结算单数据");
        }

        // 查询关联的INV（限当前店铺）
        List<Long> settlementIds = settlements.stream()
                .map(DocumentSettlement::getId).collect(Collectors.toList());
        List<DocumentInv> invList = documentInvMapper.selectList(
                new LambdaQueryWrapper<DocumentInv>()
                        .eq(DocumentInv::getShopId, shopId)
                        .in(DocumentInv::getSettlementId, settlementIds));

        String zipFileName = formatPeriod(periodStart, periodEnd) + "-结算文件.zip";
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                // 写入结算单
                for (DocumentSettlement s : settlements) {
                    List<DocumentSettlementItem> items = documentSettlementItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentSettlementItem>()
                                    .eq(DocumentSettlementItem::getSettlementId, s.getId())
                                    .orderByAsc(DocumentSettlementItem::getMsku));
                    String fileName = generateSettlementFileName(s.getDocumentNo(), s.getBuyerName(), s.getSellerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writeSettlementExcel(zos, s, items);
                    zos.closeEntry();
                }
                // 写入INV
                for (DocumentInv inv : invList) {
                    List<DocumentInvItem> items = documentInvItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentInvItem>()
                                    .eq(DocumentInvItem::getInvId, inv.getId())
                                    .orderByAsc(DocumentInvItem::getLineNo));
                    String fileName = generateInvFileName(inv.getDocumentNo(), inv.getSellerName(), inv.getBuyerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writeInvExcel(zos, inv, items);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出失败, period={}-{}", periodStart, periodEnd, e);
            throw new RuntimeException("批量导出失败", e);
        }
    }


    // ==================== 批量导出PO/DN ====================

    /**
     * 批量导出PO为ZIP文件
     *
     * @param poIds PO主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年03月28日
     */
    @Override
    public void batchExportPo(List<Long> poIds, HttpServletResponse response) {
        log.info("批量导出PO, 数量={}", poIds.size());
        if (CollectionUtils.isEmpty(poIds)) {
            throw new RuntimeException("PO ID列表不能为空");
        }

        Long shopId = ShopContext.requireShopId();

        List<DocumentPo> poList = documentPoMapper.selectList(
                new LambdaQueryWrapper<DocumentPo>()
                        .eq(DocumentPo::getShopId, shopId)
                        .in(DocumentPo::getId, poIds));
        if (CollectionUtils.isEmpty(poList)) {
            throw new RuntimeException("未找到对应的PO数据");
        }

        // 实时覆盖交易方字段
        for (DocumentPo po : poList) {
            if (org.springframework.util.StringUtils.hasText(po.getSiteCode())) {
                try {
                    DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(po.getSiteCode());
                    po.setBuyerName(party.getBuyerName());
                    po.setBuyerAddress(party.getBuyerAddress());
                    po.setSellerName(party.getSellerName());
                } catch (Exception e) {
                    log.warn("批量导出PO：获取交易方配置失败，使用单据原始值，siteCode={}", po.getSiteCode());
                }
            }
        }

        String zipFileName = "PO采购订单_" + poList.size() + "份.zip";
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20"));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                for (DocumentPo po : poList) {
                    List<DocumentPoItem> items = documentPoItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentPoItem>()
                                    .eq(DocumentPoItem::getPoId, po.getId())
                                    .orderByAsc(DocumentPoItem::getSortOrder));
                    String fileName = generatePoFileName(po.getDocumentNo(), po.getBuyerName(), po.getSellerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writePoExcel(zos, po, items);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出PO失败, 数量={}", poIds.size(), e);
            throw new RuntimeException("批量导出PO失败", e);
        }
    }

    /**
     * 批量导出DN为ZIP文件
     *
     * @param dnIds DN主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年03月28日
     */
    @Override
    public void batchExportDn(List<Long> dnIds, HttpServletResponse response) {
        log.info("批量导出DN, 数量={}", dnIds.size());
        if (CollectionUtils.isEmpty(dnIds)) {
            throw new RuntimeException("DN ID列表不能为空");
        }

        Long shopId = ShopContext.requireShopId();

        List<DocumentDn> dnList = documentDnMapper.selectList(
                new LambdaQueryWrapper<DocumentDn>()
                        .eq(DocumentDn::getShopId, shopId)
                        .in(DocumentDn::getId, dnIds));
        if (CollectionUtils.isEmpty(dnList)) {
            throw new RuntimeException("未找到对应的DN数据");
        }

        // 实时覆盖交易方字段
        for (DocumentDn dn : dnList) {
            if (org.springframework.util.StringUtils.hasText(dn.getSiteCode())) {
                try {
                    DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(dn.getSiteCode());
                    dn.setSupplierName(party.getSupplierName());
                    dn.setCustomerName(party.getCustomerNameTc());
                } catch (Exception e) {
                    log.warn("批量导出DN：获取交易方配置失败，使用单据原始值，siteCode={}", dn.getSiteCode());
                }
            }
        }

        String zipFileName = "DN送货单_" + dnList.size() + "份.zip";
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20"));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                for (DocumentDn dn : dnList) {
                    List<DocumentDnItem> items = documentDnItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentDnItem>()
                                    .eq(DocumentDnItem::getDnId, dn.getId())
                                    .orderByAsc(DocumentDnItem::getLineNo));
                    String fileName = generateDnFileName(dn.getDocumentNo(), dn.getSupplierName(), dn.getCustomerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writeDnExcel(zos, dn, items);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出DN失败, 数量={}", dnIds.size(), e);
            throw new RuntimeException("批量导出DN失败", e);
        }
    }

    // ==================== 文件名生成 ====================

    /**
     * 批量导出结算单为ZIP文件
     *
     * @param settlementIds 结算单主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void batchExportSettlement(List<Long> settlementIds, HttpServletResponse response) {
        log.info("批量导出结算单, 数量={}", settlementIds.size());
        if (CollectionUtils.isEmpty(settlementIds)) {
            throw new RuntimeException("结算单ID列表不能为空");
        }

        Long shopId = com.musheng.common.context.ShopContext.requireShopId();

        // 限当前店铺，防止越权下载
        List<DocumentSettlement> settlementList = documentSettlementMapper.selectList(
                new LambdaQueryWrapper<DocumentSettlement>()
                        .eq(DocumentSettlement::getShopId, shopId)
                        .in(DocumentSettlement::getId, settlementIds));
        if (CollectionUtils.isEmpty(settlementList)) {
            throw new RuntimeException("未找到对应的结算单数据");
        }

        String zipFileName = "结算单_" + settlementList.size() + "份.zip";
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                for (DocumentSettlement s : settlementList) {
                    List<DocumentSettlementItem> items = documentSettlementItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentSettlementItem>()
                                    .eq(DocumentSettlementItem::getSettlementId, s.getId())
                                    .orderByAsc(DocumentSettlementItem::getMsku));
                    String fileName = generateSettlementFileName(s.getDocumentNo(), s.getBuyerName(), s.getSellerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writeSettlementExcel(zos, s, items);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出结算单失败, 数量={}", settlementIds.size(), e);
            throw new RuntimeException("批量导出结算单失败", e);
        }
    }


    /**
     * 批量导出INV为ZIP文件
     *
     * @param invIds INV主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 00:50 2026年03月02日
     */
    @Override
    public void batchExportInv(List<Long> invIds, HttpServletResponse response) {
        log.info("批量导出INV, 数量={}", invIds.size());
        if (CollectionUtils.isEmpty(invIds)) {
            throw new RuntimeException("INV ID列表不能为空");
        }

        Long shopId = com.musheng.common.context.ShopContext.requireShopId();

        // 限当前店铺，防止越权下载
        List<DocumentInv> invList = documentInvMapper.selectList(
                new LambdaQueryWrapper<DocumentInv>()
                        .eq(DocumentInv::getShopId, shopId)
                        .in(DocumentInv::getId, invIds));
        if (CollectionUtils.isEmpty(invList)) {
            throw new RuntimeException("未找到对应的INV数据");
        }

        String zipFileName = "INV发票_" + invList.size() + "份.zip";
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                for (DocumentInv inv : invList) {
                    List<DocumentInvItem> items = documentInvItemMapper.selectList(
                            new LambdaQueryWrapper<DocumentInvItem>()
                                    .eq(DocumentInvItem::getInvId, inv.getId())
                                    .orderByAsc(DocumentInvItem::getLineNo));
                    String fileName = generateInvFileName(inv.getDocumentNo(), inv.getSellerName(), inv.getBuyerName());
                    zos.putNextEntry(new ZipEntry(fileName));
                    writeInvExcel(zos, inv, items);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.error("批量导出INV失败, 数量={}", invIds.size(), e);
            throw new RuntimeException("批量导出INV失败", e);
        }
    }


    // ==================== 文件名生成 ====================

    /**
     * 生成PO导出文件名
     *
     * @param documentNo 单据编号
     * @param buyerName 买方名称
     * @param sellerName 卖方名称
     * @return PO导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public String generatePoFileName(String documentNo, String buyerName, String sellerName) {
        return safe(documentNo) + "-" + safe(buyerName) + "-" + safe(sellerName) + "-PO.xlsx";
    }

    /**
     * 生成DN导出文件名
     *
     * @param documentNo 单据编号
     * @param supplierName 供应商名称
     * @param customerName 客户名称
     * @return DN导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public String generateDnFileName(String documentNo, String supplierName, String customerName) {
        return safe(documentNo) + "-" + safe(supplierName) + "-" + safe(customerName) + "-送貨清單.xlsx";
    }

    /**
     * 生成结算单导出文件名
     *
     * @param documentNo 单据编号
     * @param buyerName 买方名称
     * @param sellerName 卖方名称
     * @return 结算单导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public String generateSettlementFileName(String documentNo, String buyerName, String sellerName) {
        return safe(documentNo) + "-" + safe(buyerName) + "-" + safe(sellerName) + "-结算单.xlsx";
    }

    /**
     * 生成INV导出文件名
     *
     * @param documentNo 单据编号
     * @param sellerName 卖方名称
     * @param buyerName 买方英文名称
     * @return INV导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public String generateInvFileName(String documentNo, String sellerName, String buyerName) {
        return safe(documentNo) + "-" + safe(sellerName) + "-" + safe(buyerName) + "-invoice.xlsx";
    }


    // ==================== 辅助方法 ====================

    /**
     * 判断导出时是否嵌入签章和Logo
     *
     * @return 启用返回 true
     * @author wanhua
     * 21:35 2026年03月21日
     */
    private boolean isStampEnabled() {
        return sysConfigService.getBoolean("export_stamp_enabled", true);
    }

    /**
     * 设置Excel下载响应头
     *
     * @param response HTTP响应对象
     * @param fileName 文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    /**
     * 设置 Excel 文件下载响应头
     * 使用 RFC 5987 标准格式（filename*=UTF-8''xxx），确保中文文件名在浏览器中正确显示
     *
     * @param response HTTP响应对象
     * @param fileName 文件名（含扩展名）
     * @author wanhua
     * 10:30 2026年03月07日
     */
    private void setExcelResponseHeaders(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        // 使用 RFC 5987 标准格式，避免 URLEncoder 将空格编为 + 导致前端解码异常
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename*=UTF-8''" + encodedFileName);
    }

    /**
     * 格式化结算周期为字符串
     *
     * @param start 起始日期
     * @param end 结束日期
     * @return 格式化后的周期字符串
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        return start.format(DATE_FMT) + "~" + end.format(DATE_FMT);
    }

    /**
     * 空值安全转换 - 将null转为空字符串
     *
     * @param value 原始字符串
     * @return 非null字符串
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * 填补合并区域内所有缺失的单元格并应用边框样式，
     * 同时关闭默认网格线，避免未设置边框的单元格显示虚线
     *
     * @param sheet 工作表
     * @param style 应用于缺失单元格的边框样式
     */
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

    /**
     * 设置单元格四边边框为细线
     *
     * @param style 单元格样式
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 自动调整列宽：遍历所有行计算最大内容宽度，跳过合并单元格区域
     *
     * @param sheet      工作表
     * @param numColumns 列数
     * @param minWidths  每列最小宽度（字符数），null则不限制
     * @author wanhua
     * 10:30 2026年05月29日
     */
    private void autoFitColumns(Sheet sheet, int numColumns, double[] minWidths) {
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        for (int col = 0; col < numColumns; col++) {
            double maxWidth = minWidths != null && col < minWidths.length ? minWidths[col] : 8.0;
            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                Cell cell = row.getCell(col);
                if (cell == null) continue;
                if (isInMergedRegionNotFirstCol(mergedRegions, rowIdx, col)) continue;
                String value = getCellStringValue(cell);
                if (value == null || value.isEmpty()) continue;
                double contentWidth = calculateStringWidth(value);
                int mergedColSpan = getMergedColSpan(mergedRegions, rowIdx, col);
                if (mergedColSpan > 1) {
                    contentWidth = contentWidth / mergedColSpan;
                }
                // 中文字体补偿约15%
                contentWidth = contentWidth * 1.15;
                if (contentWidth > maxWidth) maxWidth = contentWidth;
            }
            // 加1字符padding，上限38字符防止列过宽导致打印超幅
            int width = (int) ((maxWidth + 1) * 256);
            if (width > 38 * 256) width = 38 * 256;
            sheet.setColumnWidth(col, width);
        }
    }

    /**
     * 计算字符串显示宽度：中文/全角字符算2，其他算1，多行取最长行
     */
    private double calculateStringWidth(String value) {
        String[] lines = value.split("\n");
        double maxLineWidth = 0;
        for (String line : lines) {
            double lineWidth = 0;
            for (char c : line.toCharArray()) {
                if (c >= '一' && c <= '鿿' || c >= '　' && c <= '〿'
                        || c >= '＀' && c <= '￯') {
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
     * 创建单元格并设置值和样式
     *
     * @param row 行对象
     * @param col 列索引
     * @param value 单元格值
     * @param style 单元格样式
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * 嵌入图片到Excel工作表 - 使用TwoCellAnchor精确定位（适用于logo等不敏感图片）
     *
     * @param wb 工作簿
     * @param sheet 工作表
     * @param resourcePath 图片资源路径（classpath下）
     * @param col1 起始列
     * @param col1Off 起始列偏移（EMU）
     * @param row1 起始行
     * @param row1Off 起始行偏移（EMU）
     * @param col2 结束列
     * @param col2Off 结束列偏移（EMU）
     * @param row2 结束行
     * @param row2Off 结束行偏移（EMU）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void addImage(XSSFWorkbook wb, Sheet sheet, String resourcePath,
                          int col1, int col1Off, int row1, int row1Off,
                          int col2, int col2Off, int row2, int row2Off) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("图片资源不存在: {}", resourcePath);
                return;
            }
            byte[] imageBytes;
            try (InputStream is = resource.getInputStream()) {
                imageBytes = is.readAllBytes();
            }
            int pictureIdx = wb.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);

            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor();
            anchor.setCol1(col1);
            anchor.setDx1(col1Off);
            anchor.setRow1(row1);
            anchor.setDy1(row1Off);
            anchor.setCol2(col2);
            anchor.setDx2(col2Off);
            anchor.setRow2(row2);
            anchor.setDy2(row2Off);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

            XSSFPicture picture = drawing.createPicture(anchor, pictureIdx);

            // 修改底层XML：editAs="oneCell"，图片作为独立图层
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing ctDrawing = drawing.getCTDrawing();
            int anchorIdx = ctDrawing.sizeOfTwoCellAnchorArray() - 1;
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor ctAnchor = ctDrawing.getTwoCellAnchorArray(anchorIdx);
            ctAnchor.setEditAs(org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs.ONE_CELL);

            // 锁定宽高比
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture ctPic = picture.getCTPicture();
            org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualPictureProperties cNvPicPr =
                    ctPic.getNvPicPr().getCNvPicPr();
            if (cNvPicPr.getPicLocks() == null) {
                cNvPicPr.addNewPicLocks();
            }
            cNvPicPr.getPicLocks().setNoChangeAspect(true);
        } catch (IOException e) {
            log.error("嵌入图片失败: {}", resourcePath, e);
        }
    }

    /**
     * 嵌入图片 - 原图直接贴入，完全复刻标准样本的锚点格式
     * 使用twoCellAnchor editAs="oneCell"，图片作为独立图层不受表格伸缩影响
     * from/to坐标和ext尺寸全部从标准样本提取，不依赖POI的resize计算
     *
     * @param wb 工作簿
     * @param sheet 工作表
     * @param resourcePath 图片资源路径
     * @param col1 from列
     * @param dx1 from列偏移（EMU）
     * @param row1 from行
     * @param dy1 from行偏移（EMU）
     * @param col2 to列
     * @param dx2 to列偏移（EMU）
     * @param rowSpan 行跨度（to.row = from.row + rowSpan）
     * @param dy2 to行偏移（EMU）
     * @param cx ext宽度（EMU）
     * @param cy ext高度（EMU）
     * @author wanhua
     * 16:30 2026年03月02日
     */
    private void addImageOriginal(XSSFWorkbook wb, Sheet sheet, String resourcePath,
                                  int col1, int dx1, int row1, int dy1,
                                  int col2, int dx2, int rowSpan, int dy2,
                                  int cx, int cy) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("图片资源不存在: {}", resourcePath);
                return;
            }
            byte[] imageBytes;
            try (InputStream is = resource.getInputStream()) {
                imageBytes = is.readAllBytes();
            }

            int pictureIdx = wb.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);

            // 使用POI标准API创建twoCellAnchor图片（正确处理blip embed关系）
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int toRow = row1 + rowSpan;
            XSSFClientAnchor anchor = new XSSFClientAnchor(dx1, dy1, dx2, dy2, col1, row1, col2, toRow);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
            XSSFPicture picture = drawing.createPicture(anchor, pictureIdx);

            // 获取底层XML，将twoCellAnchor转换为oneCellAnchor
            // twoCellAnchor的尺寸由from/to坐标差决定，无法精确控制
            // oneCellAnchor只有from+ext，ext直接控制图片尺寸
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing ctDrawing = drawing.getCTDrawing();
            int anchorIdx = ctDrawing.sizeOfTwoCellAnchorArray() - 1;
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor ctTwoCell = ctDrawing.getTwoCellAnchorArray(anchorIdx);

            // 提取pic节点的XML，用于迁移到oneCellAnchor
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture ctPic = picture.getCTPicture();

            // 先修改spPr中的ext（在setPic之前，避免深拷贝问题）
            org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties spPr = ctPic.getSpPr();
            if (spPr == null) {
                spPr = ctPic.addNewSpPr();
            }
            if (spPr.isSetXfrm()) {
                spPr.unsetXfrm();
            }
            org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D xfrm = spPr.addNewXfrm();
            org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D picExt = xfrm.addNewExt();
            picExt.setCx(cx);
            picExt.setCy(cy);

            // 锁定宽高比
            org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualPictureProperties cNvPicPr =
                    ctPic.getNvPicPr().getCNvPicPr();
            if (cNvPicPr.getPicLocks() == null) {
                cNvPicPr.addNewPicLocks();
            }
            cNvPicPr.getPicLocks().setNoChangeAspect(true);

            // 创建oneCellAnchor
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTOneCellAnchor oneCell = ctDrawing.addNewOneCellAnchor();

            // 设置from坐标（与原twoCellAnchor相同）
            org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker fromMarker = oneCell.addNewFrom();
            fromMarker.setCol(col1);
            fromMarker.setColOff(dx1);
            fromMarker.setRow(row1);
            fromMarker.setRowOff(dy1);

            // 设置ext尺寸（这是oneCellAnchor的核心：直接控制图片大小）
            org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D anchorExt = oneCell.addNewExt();
            anchorExt.setCx(cx);
            anchorExt.setCy(cy);

            // 迁移pic节点到oneCellAnchor（此时spPr已修改完毕）
            oneCell.setPic(ctPic);

            // 添加clientData
            oneCell.addNewClientData();

            // 删除原来的twoCellAnchor（已迁移到oneCellAnchor）
            ctDrawing.removeTwoCellAnchor(anchorIdx);

        } catch (IOException e) {
            log.error("嵌入图片失败: {}", resourcePath, e);
        }
    }
}
