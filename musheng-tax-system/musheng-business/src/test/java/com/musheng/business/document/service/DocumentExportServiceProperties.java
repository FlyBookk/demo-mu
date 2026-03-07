package com.musheng.business.document.service;

import com.musheng.business.document.service.impl.DocumentExportServiceImpl;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentExportService 属性测试
 *
 * <p>使用 jqwik 框架验证导出文件名格式的通用正确性属性。</p>
 * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class DocumentExportServiceProperties {

    private final DocumentExportServiceImpl exportService = new DocumentExportServiceImpl();

    // ==================== 自定义 Arbitrary 生成器 ====================

    /**
     * 生成随机单据编号（8位日期+3位序号）
     *
     * @return 单据编号 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<String> documentNos() {
        // 生成格式为 YYYYMMDD + 3位序号 的编号
        Arbitrary<Integer> years = Arbitraries.integers().between(2025, 2030);
        Arbitrary<Integer> months = Arbitraries.integers().between(1, 12);
        Arbitrary<Integer> days = Arbitraries.integers().between(1, 28);
        Arbitrary<Integer> seqs = Arbitraries.integers().between(1, 999);

        return Combinators.combine(years, months, days, seqs)
                .as((year, month, day, seq) ->
                        String.format("%04d%02d%02d%03d", year, month, day, seq));
    }

    /**
     * 生成随机非空名称（不含连字符，避免干扰文件名解析）
     *
     * @return 名称 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<String> names() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    // ==================== 属性 15：导出文件名格式 ====================

    /**
     * 属性15-1：PO文件名匹配模式 {编号}-{买方}-{卖方}-PO.xlsx
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.1</b></p>
     *
     * @param documentNo 随机单据编号
     * @param buyerName 随机买方名称
     * @param sellerName 随机卖方名称
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void poFileName_ShouldMatchPattern(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String buyerName,
            @ForAll("names") String sellerName) {

        String fileName = exportService.generatePoFileName(documentNo, buyerName, sellerName);

        // 文件名以.xlsx结尾
        assertTrue(fileName.endsWith(".xlsx"),
                "PO文件名应以.xlsx结尾, 实际: " + fileName);

        // 文件名以-PO.xlsx结尾
        assertTrue(fileName.endsWith("-PO.xlsx"),
                "PO文件名应以-PO.xlsx结尾, 实际: " + fileName);

        // 文件名以编号开头
        assertTrue(fileName.startsWith(documentNo + "-"),
                "PO文件名应以编号开头, 实际: " + fileName);

        // 文件名包含编号
        assertTrue(fileName.contains(documentNo),
                "PO文件名应包含编号, 实际: " + fileName);

        // 文件名包含买方名称
        assertTrue(fileName.contains(buyerName),
                "PO文件名应包含买方名称, 实际: " + fileName);

        // 文件名包含卖方名称
        assertTrue(fileName.contains(sellerName),
                "PO文件名应包含卖方名称, 实际: " + fileName);

        // 验证完整格式：{编号}-{买方}-{卖方}-PO.xlsx
        String expected = documentNo + "-" + buyerName + "-" + sellerName + "-PO.xlsx";
        assertEquals(expected, fileName,
                "PO文件名格式不正确");
    }

    /**
     * 属性15-2：DN文件名匹配模式 {编号}-{供应商}-{客户}-送貨清單.xlsx
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.2</b></p>
     *
     * @param documentNo 随机单据编号
     * @param supplierName 随机供应商名称
     * @param customerName 随机客户名称
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void dnFileName_ShouldMatchPattern(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String supplierName,
            @ForAll("names") String customerName) {

        String fileName = exportService.generateDnFileName(documentNo, supplierName, customerName);

        // 文件名以.xlsx结尾
        assertTrue(fileName.endsWith(".xlsx"),
                "DN文件名应以.xlsx结尾, 实际: " + fileName);

        // 文件名以-送貨清單.xlsx结尾
        assertTrue(fileName.endsWith("-送貨清單.xlsx"),
                "DN文件名应以-送貨清單.xlsx结尾, 实际: " + fileName);

        // 文件名以编号开头
        assertTrue(fileName.startsWith(documentNo + "-"),
                "DN文件名应以编号开头, 实际: " + fileName);

        // 文件名包含编号
        assertTrue(fileName.contains(documentNo),
                "DN文件名应包含编号, 实际: " + fileName);

        // 验证完整格式
        String expected = documentNo + "-" + supplierName + "-" + customerName + "-送貨清單.xlsx";
        assertEquals(expected, fileName,
                "DN文件名格式不正确");
    }

    /**
     * 属性15-3：结算单文件名匹配模式 {编号}-{买方}-{卖方}-结算单.xlsx
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.3</b></p>
     *
     * @param documentNo 随机单据编号
     * @param buyerName 随机买方名称
     * @param sellerName 随机卖方名称
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void settlementFileName_ShouldMatchPattern(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String buyerName,
            @ForAll("names") String sellerName) {

        String fileName = exportService.generateSettlementFileName(documentNo, buyerName, sellerName);

        // 文件名以.xlsx结尾
        assertTrue(fileName.endsWith(".xlsx"),
                "结算单文件名应以.xlsx结尾, 实际: " + fileName);

        // 文件名以-结算单.xlsx结尾
        assertTrue(fileName.endsWith("-结算单.xlsx"),
                "结算单文件名应以-结算单.xlsx结尾, 实际: " + fileName);

        // 文件名以编号开头
        assertTrue(fileName.startsWith(documentNo + "-"),
                "结算单文件名应以编号开头, 实际: " + fileName);

        // 文件名包含编号
        assertTrue(fileName.contains(documentNo),
                "结算单文件名应包含编号, 实际: " + fileName);

        // 验证完整格式
        String expected = documentNo + "-" + buyerName + "-" + sellerName + "-结算单.xlsx";
        assertEquals(expected, fileName,
                "结算单文件名格式不正确");
    }

    /**
     * 属性15-4：INV文件名匹配模式 {编号}-{卖方}-{买方}-invoice.xlsx
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.4</b></p>
     *
     * @param documentNo 随机单据编号
     * @param sellerName 随机卖方名称
     * @param buyerName 随机买方名称
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void invFileName_ShouldMatchPattern(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String sellerName,
            @ForAll("names") String buyerName) {

        String fileName = exportService.generateInvFileName(documentNo, sellerName, buyerName);

        // 文件名以.xlsx结尾
        assertTrue(fileName.endsWith(".xlsx"),
                "INV文件名应以.xlsx结尾, 实际: " + fileName);

        // 文件名以-invoice.xlsx结尾
        assertTrue(fileName.endsWith("-invoice.xlsx"),
                "INV文件名应以-invoice.xlsx结尾, 实际: " + fileName);

        // 文件名以编号开头
        assertTrue(fileName.startsWith(documentNo + "-"),
                "INV文件名应以编号开头, 实际: " + fileName);

        // 文件名包含编号
        assertTrue(fileName.contains(documentNo),
                "INV文件名应包含编号, 实际: " + fileName);

        // 验证完整格式
        String expected = documentNo + "-" + sellerName + "-" + buyerName + "-invoice.xlsx";
        assertEquals(expected, fileName,
                "INV文件名格式不正确");
    }

    /**
     * 属性15-5：所有文件名以.xlsx结尾
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b></p>
     *
     * @param documentNo 随机单据编号
     * @param name1 随机名称1
     * @param name2 随机名称2
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void allFileNames_ShouldEndWithXlsx(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String name1,
            @ForAll("names") String name2) {

        assertTrue(exportService.generatePoFileName(documentNo, name1, name2).endsWith(".xlsx"));
        assertTrue(exportService.generateDnFileName(documentNo, name1, name2).endsWith(".xlsx"));
        assertTrue(exportService.generateSettlementFileName(documentNo, name1, name2).endsWith(".xlsx"));
        assertTrue(exportService.generateInvFileName(documentNo, name1, name2).endsWith(".xlsx"));
    }

    /**
     * 属性15-6：所有文件名包含编号
     *
     * <p>Feature: fba-document-generation, Property 15: 导出文件名格式</p>
     * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b></p>
     *
     * @param documentNo 随机单据编号
     * @param name1 随机名称1
     * @param name2 随机名称2
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void allFileNames_ShouldContainDocumentNo(
            @ForAll("documentNos") String documentNo,
            @ForAll("names") String name1,
            @ForAll("names") String name2) {

        assertTrue(exportService.generatePoFileName(documentNo, name1, name2).contains(documentNo));
        assertTrue(exportService.generateDnFileName(documentNo, name1, name2).contains(documentNo));
        assertTrue(exportService.generateSettlementFileName(documentNo, name1, name2).contains(documentNo));
        assertTrue(exportService.generateInvFileName(documentNo, name1, name2).contains(documentNo));
    }
}
