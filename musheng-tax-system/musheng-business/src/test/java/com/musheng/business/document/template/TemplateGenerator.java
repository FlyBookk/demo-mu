package com.musheng.business.document.template;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Excel模板文件生成器
 * 
 * 用于在 resources/templates/ 目录下生成4个Excel模板占位文件，
 * 供 EasyExcel 模板填充导出使用。
 * 后续可由业务人员替换为实际的Excel模板。
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class TemplateGenerator {

    private static final String TEMPLATE_DIR = "src/main/resources/templates";

    /**
     * 生成4个Excel模板占位文件
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Test
    void generateAllTemplates() throws IOException {
        File dir = new File(TEMPLATE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        generatePoTemplate();
        generateDnTemplate();
        generateSettlementTemplate();
        generateInvTemplate();

        System.out.println("所有模板文件生成完成，目录: " + dir.getAbsolutePath());
    }

    /**
     * 生成PO采购订单模板
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void generatePoTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PO");

            // 表头区域
            createHeaderRow(sheet, 0, new String[]{"Purchase Order"});
            createHeaderRow(sheet, 1, new String[]{"买方/Buyer:", "${header.buyerName}"});
            createHeaderRow(sheet, 2, new String[]{"买方地址/Address:", "${header.buyerAddress}"});
            createHeaderRow(sheet, 3, new String[]{"卖方/Seller:", "${header.sellerName}"});
            createHeaderRow(sheet, 4, new String[]{"PO编号/PO No.:", "${header.documentNo}"});
            createHeaderRow(sheet, 5, new String[]{"日期/Date:", "${header.poDate}"});

            // 明细表头
            createHeaderRow(sheet, 7, new String[]{"序号", "货件编号", "MSKU", "FBA地址", "数量"});

            // 明细数据行（EasyExcel填充区域）
            createHeaderRow(sheet, 8, new String[]{"${items.sortOrder}", "${items.shipmentNo}", "${items.msku}", "${items.fbaAddress}", "${items.quantity}"});

            writeToFile(workbook, "po_template.xlsx");
        }
    }

    /**
     * 生成DN送货单模板
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void generateDnTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DN");

            // 表头区域
            createHeaderRow(sheet, 0, new String[]{"送貨清單 / Delivery Note"});
            createHeaderRow(sheet, 1, new String[]{"供應商/Supplier:", "${header.supplierName}"});
            createHeaderRow(sheet, 2, new String[]{"客戶/Customer:", "${header.customerName}"});
            createHeaderRow(sheet, 3, new String[]{"送貨日期/Date:", "${header.dnDate}"});
            createHeaderRow(sheet, 4, new String[]{"送貨單號/DN No.:", "${header.documentNo}"});

            // 明细表头
            createHeaderRow(sheet, 6, new String[]{"行號", "MSKU", "描述", "數量", "備註(貨件編號)"});

            // 明细数据行
            createHeaderRow(sheet, 7, new String[]{"${items.lineNo}", "${items.msku}", "", "${items.quantity}", "${items.shipmentNo}"});

            writeToFile(workbook, "dn_template.xlsx");
        }
    }

    /**
     * 生成结算单模板
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void generateSettlementTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Settlement");

            // 表头区域
            createHeaderRow(sheet, 0, new String[]{"结算单 / Settlement Statement"});
            createHeaderRow(sheet, 1, new String[]{"买方/Buyer:", "${header.buyerName}"});
            createHeaderRow(sheet, 2, new String[]{"买方地址/Address:", "${header.buyerAddress}"});
            createHeaderRow(sheet, 3, new String[]{"卖方/Seller:", "${header.sellerName}"});
            createHeaderRow(sheet, 4, new String[]{"结算编号/No.:", "${header.documentNo}"});
            createHeaderRow(sheet, 5, new String[]{"结算日期/Date:", "${header.settlementDate}"});
            createHeaderRow(sheet, 6, new String[]{"结算周期/Period:", "${header.periodStart} - ${header.periodEnd}"});
            createHeaderRow(sheet, 7, new String[]{"站点货币/Currency:", "${header.siteCode}"});

            // 明细表头
            createHeaderRow(sheet, 9, new String[]{"No#", "Description", "Currency", "Unit price", "Q'ty", "Amount"});

            // 明细数据行
            createHeaderRow(sheet, 10, new String[]{"${items.lineNo}", "${items.msku}", "${items.currency}", "${items.unitPrice}", "${items.quantity}", "${items.amount}"});

            writeToFile(workbook, "settlement_template.xlsx");
        }
    }

    /**
     * 生成INV发票模板
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void generateInvTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("INV");

            // 表头区域
            createHeaderRow(sheet, 0, new String[]{"COMMERCIAL INVOICE"});
            createHeaderRow(sheet, 1, new String[]{"SELLER:", "${header.sellerName}"});
            createHeaderRow(sheet, 2, new String[]{"Address:", "${header.sellerAddress}"});
            createHeaderRow(sheet, 3, new String[]{"Tel:", "${header.sellerPhone}"});
            createHeaderRow(sheet, 4, new String[]{"BUYER:", "${header.buyerName}"});
            createHeaderRow(sheet, 5, new String[]{"Address:", "${header.buyerAddress}"});
            createHeaderRow(sheet, 6, new String[]{"Tel:", "${header.buyerPhone}"});
            createHeaderRow(sheet, 7, new String[]{"Invoice No.:", "${header.documentNo}"});
            createHeaderRow(sheet, 8, new String[]{"Date:", "${header.invDate}"});

            // 明细表头
            createHeaderRow(sheet, 10, new String[]{"No.", "Description", "Quantity ctns/pcs", "Unit price", "Total"});

            // 明细数据行
            createHeaderRow(sheet, 11, new String[]{"${items.lineNo}", "${items.msku}", "${items.quantity}", "${items.unitPrice}", "${items.amount}"});

            // 银行信息区域
            createHeaderRow(sheet, 13, new String[]{"Bank Information"});
            createHeaderRow(sheet, 14, new String[]{"Account name:", "${header.bankAccountName}"});
            createHeaderRow(sheet, 15, new String[]{"Account number:", "${header.bankAccountNumber}"});
            createHeaderRow(sheet, 16, new String[]{"Bank Name:", "${header.bankName}"});
            createHeaderRow(sheet, 17, new String[]{"Bank address:", "${header.bankAddress}"});
            createHeaderRow(sheet, 18, new String[]{"Swift Code:", "${header.swiftCode}"});

            writeToFile(workbook, "inv_template.xlsx");
        }
    }

    /**
     * 创建表头行
     *
     * @param sheet 工作表
     * @param rowIndex 行索引
     * @param values 单元格值数组
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void createHeaderRow(Sheet sheet, int rowIndex, String[] values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }

    /**
     * 将工作簿写入模板文件
     *
     * @param workbook 工作簿
     * @param fileName 文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private void writeToFile(Workbook workbook, String fileName) throws IOException {
        File file = new File(TEMPLATE_DIR, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        System.out.println("模板文件已生成: " + file.getAbsolutePath());
    }
}
