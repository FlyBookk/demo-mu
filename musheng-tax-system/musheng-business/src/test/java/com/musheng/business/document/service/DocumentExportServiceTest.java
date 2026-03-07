package com.musheng.business.document.service;

import com.musheng.business.document.service.impl.DocumentExportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentExportService 单元测试
 *
 * <p>主要测试文件名生成方法（纯函数，不需要 mock HttpServletResponse）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@ExtendWith(MockitoExtension.class)
class DocumentExportServiceTest {

    @InjectMocks
    private DocumentExportServiceImpl documentExportService;

    // ==================== generatePoFileName 测试 ====================

    /**
     * 测试PO文件名格式：标准输入
     */
    @Test
    void testGeneratePoFileName_StandardInput_ShouldMatchFormat() {
        // Given
        String documentNo = "20250902001";
        String buyerName = "东莞市慕声商贸有限公司";
        String sellerName = "Hong Kong Andeo Group Limited";

        // When
        String fileName = documentExportService.generatePoFileName(documentNo, buyerName, sellerName);

        // Then
        assertEquals("20250902001-东莞市慕声商贸有限公司-Hong Kong Andeo Group Limited-PO.xlsx", fileName);
        assertTrue(fileName.endsWith(".xlsx"), "文件名应以.xlsx结尾");
        assertTrue(fileName.contains(documentNo), "文件名应包含编号");
        assertTrue(fileName.endsWith("-PO.xlsx"), "PO文件名应以-PO.xlsx结尾");
    }

    /**
     * 测试PO文件名格式：不同编号
     */
    @Test
    void testGeneratePoFileName_DifferentDocNo_ShouldContainDocNo() {
        // Given
        String documentNo = "20251225003";
        String buyerName = "买方A";
        String sellerName = "卖方B";

        // When
        String fileName = documentExportService.generatePoFileName(documentNo, buyerName, sellerName);

        // Then
        assertEquals("20251225003-买方A-卖方B-PO.xlsx", fileName);
        assertTrue(fileName.startsWith(documentNo), "文件名应以编号开头");
    }

    // ==================== generateDnFileName 测试 ====================

    /**
     * 测试DN文件名格式：标准输入
     */
    @Test
    void testGenerateDnFileName_StandardInput_ShouldMatchFormat() {
        // Given
        String documentNo = "20250902001";
        String supplierName = "Hong Kong Andeo Group Limited";
        String customerName = "東莞市慕聲商貿有限公司";

        // When
        String fileName = documentExportService.generateDnFileName(documentNo, supplierName, customerName);

        // Then
        assertEquals("20250902001-Hong Kong Andeo Group Limited-東莞市慕聲商貿有限公司-送貨清單.xlsx", fileName);
        assertTrue(fileName.endsWith(".xlsx"), "文件名应以.xlsx结尾");
        assertTrue(fileName.contains(documentNo), "文件名应包含编号");
        assertTrue(fileName.endsWith("-送貨清單.xlsx"), "DN文件名应以-送貨清單.xlsx结尾");
    }

    /**
     * 测试DN文件名格式：不同编号
     */
    @Test
    void testGenerateDnFileName_DifferentDocNo_ShouldContainDocNo() {
        // Given
        String documentNo = "20251001002";
        String supplierName = "供应商X";
        String customerName = "客户Y";

        // When
        String fileName = documentExportService.generateDnFileName(documentNo, supplierName, customerName);

        // Then
        assertEquals("20251001002-供应商X-客户Y-送貨清單.xlsx", fileName);
    }

    // ==================== generateSettlementFileName 测试 ====================

    /**
     * 测试结算单文件名格式：标准输入
     */
    @Test
    void testGenerateSettlementFileName_StandardInput_ShouldMatchFormat() {
        // Given
        String documentNo = "20250909001";
        String buyerName = "东莞市慕声商贸有限公司";
        String sellerName = "Hong Kong Andeo Group Limited";

        // When
        String fileName = documentExportService.generateSettlementFileName(documentNo, buyerName, sellerName);

        // Then
        assertEquals("20250909001-东莞市慕声商贸有限公司-Hong Kong Andeo Group Limited-结算单.xlsx", fileName);
        assertTrue(fileName.endsWith(".xlsx"), "文件名应以.xlsx结尾");
        assertTrue(fileName.contains(documentNo), "文件名应包含编号");
        assertTrue(fileName.endsWith("-结算单.xlsx"), "结算单文件名应以-结算单.xlsx结尾");
    }

    /**
     * 测试结算单文件名格式：不同站点编号
     */
    @Test
    void testGenerateSettlementFileName_DifferentSequence_ShouldContainDocNo() {
        // Given
        String documentNo = "20250909004";
        String buyerName = "买方C";
        String sellerName = "卖方D";

        // When
        String fileName = documentExportService.generateSettlementFileName(documentNo, buyerName, sellerName);

        // Then
        assertEquals("20250909004-买方C-卖方D-结算单.xlsx", fileName);
    }

    // ==================== generateInvFileName 测试 ====================

    /**
     * 测试INV文件名格式：标准输入
     */
    @Test
    void testGenerateInvFileName_StandardInput_ShouldMatchFormat() {
        // Given
        String documentNo = "20250910001";
        String sellerName = "Hong Kong Andeo Group Limited";
        String buyerName = "Dongguan Musheng Trade Co., Ltd.";

        // When
        String fileName = documentExportService.generateInvFileName(documentNo, sellerName, buyerName);

        // Then
        assertEquals("20250910001-Hong Kong Andeo Group Limited-Dongguan Musheng Trade Co., Ltd.-invoice.xlsx", fileName);
        assertTrue(fileName.endsWith(".xlsx"), "文件名应以.xlsx结尾");
        assertTrue(fileName.contains(documentNo), "文件名应包含编号");
        assertTrue(fileName.endsWith("-invoice.xlsx"), "INV文件名应以-invoice.xlsx结尾");
    }

    /**
     * 测试INV文件名格式：不同编号
     */
    @Test
    void testGenerateInvFileName_DifferentDocNo_ShouldContainDocNo() {
        // Given
        String documentNo = "20250910004";
        String sellerName = "卖方E";
        String buyerName = "Buyer F";

        // When
        String fileName = documentExportService.generateInvFileName(documentNo, sellerName, buyerName);

        // Then
        assertEquals("20250910004-卖方E-Buyer F-invoice.xlsx", fileName);
    }

    // ==================== 文件名通用属性测试 ====================

    /**
     * 测试所有文件名都以.xlsx结尾
     */
    @Test
    void testAllFileNames_ShouldEndWithXlsx() {
        // Given
        String docNo = "20250902001";

        // When
        String poFile = documentExportService.generatePoFileName(docNo, "买方", "卖方");
        String dnFile = documentExportService.generateDnFileName(docNo, "供应商", "客户");
        String settlementFile = documentExportService.generateSettlementFileName(docNo, "买方", "卖方");
        String invFile = documentExportService.generateInvFileName(docNo, "卖方", "买方");

        // Then
        assertTrue(poFile.endsWith(".xlsx"), "PO文件名应以.xlsx结尾");
        assertTrue(dnFile.endsWith(".xlsx"), "DN文件名应以.xlsx结尾");
        assertTrue(settlementFile.endsWith(".xlsx"), "结算单文件名应以.xlsx结尾");
        assertTrue(invFile.endsWith(".xlsx"), "INV文件名应以.xlsx结尾");
    }

    /**
     * 测试所有文件名都包含编号
     */
    @Test
    void testAllFileNames_ShouldContainDocumentNo() {
        // Given
        String docNo = "20250902001";

        // When
        String poFile = documentExportService.generatePoFileName(docNo, "买方", "卖方");
        String dnFile = documentExportService.generateDnFileName(docNo, "供应商", "客户");
        String settlementFile = documentExportService.generateSettlementFileName(docNo, "买方", "卖方");
        String invFile = documentExportService.generateInvFileName(docNo, "卖方", "买方");

        // Then
        assertTrue(poFile.contains(docNo), "PO文件名应包含编号");
        assertTrue(dnFile.contains(docNo), "DN文件名应包含编号");
        assertTrue(settlementFile.contains(docNo), "结算单文件名应包含编号");
        assertTrue(invFile.contains(docNo), "INV文件名应包含编号");
    }

    /**
     * 测试文件名中各部分用连字符分隔
     */
    @Test
    void testAllFileNames_ShouldUseDashSeparator() {
        // Given
        String docNo = "20250902001";
        String buyer = "买方";
        String seller = "卖方";

        // When
        String poFile = documentExportService.generatePoFileName(docNo, buyer, seller);

        // Then
        // 格式：{编号}-{买方}-{卖方}-PO.xlsx，至少3个连字符
        String[] parts = poFile.replace(".xlsx", "").split("-");
        assertTrue(parts.length >= 3, "文件名应包含至少3个连字符分隔的部分");
    }
}
