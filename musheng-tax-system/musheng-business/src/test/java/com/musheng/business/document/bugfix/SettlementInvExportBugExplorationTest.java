package com.musheng.business.document.bugfix;

import com.musheng.business.document.generator.SettlementGenerateResult;
import com.musheng.business.document.generator.SettlementGenerator;
import com.musheng.business.document.generator.SettlementInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缺陷条件探索性测试 - 结算单与 INV 导出缺陷
 *
 * <p>在实施修复之前运行此测试，验证缺陷确实存在。
 * 预期结果：测试失败（证明缺陷存在）。</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.3, 1.4</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("缺陷条件探索性测试 - 结算单与INV导出")
class SettlementInvExportBugExplorationTest {

    /** 期望的买方地址 */
    private static final String EXPECTED_BUYER_ADDRESS =
            "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房";

    /** DocumentExportServiceImpl 源码路径 */
    private static final String EXPORT_SERVICE_SOURCE_PATH =
            "src/main/java/com/musheng/business/document/service/impl/DocumentExportServiceImpl.java";


    // ==================== 测试1: buyerAddress 缺失 ====================

    @Nested
    @DisplayName("测试1: 结算单 buyerAddress 缺失")
    class BuyerAddressMissingTest {

        /**
         * 验证 SettlementGenerator.buildSettlement() 生成的结算单包含正确的 buyerAddress
         *
         * <p>在未修复代码上，buildSettlement() 未设置 buyerAddress，
         * 因此 settlement.getBuyerAddress() 返回 null，测试将失败。</p>
         *
         * <p><b>Validates: Requirements 1.1</b></p>
         */
        @Test
        @DisplayName("结算单 buyerAddress 应等于期望地址（未修复时将失败，返回 null）")
        void testBuildSettlement_BuyerAddress_ShouldEqualExpectedAddress() {
            // Given - 构造一个标准的结算数据输入
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            SettlementInput.SettlementDataItem.builder()
                                    .siteCode("USD")
                                    .msku("MSUS-TEST-001")
                                    .currency("USD")
                                    .unitPrice(new BigDecimal("10.00"))
                                    .quantity(5)
                                    .build()
                    ))
                    .build();

            // When - 调用生成器生成结算单
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 验证第一份结算单的 buyerAddress
            assertFalse(results.isEmpty(), "应至少生成一份结算单");
            SettlementGenerateResult usdResult = results.get(0);
            assertNotNull(usdResult.getSettlement(), "结算单主表不应为 null");

            // 核心断言：buyerAddress 应等于期望地址
            // 在未修复代码上，此断言将失败（getBuyerAddress() 返回 null）
            assertEquals(
                    EXPECTED_BUYER_ADDRESS,
                    usdResult.getSettlement().getBuyerAddress(),
                    "结算单 buyerAddress 应为 '广东省东莞市虎门镇连升路82号虎门万达广场2栋606房'"
            );
        }

        /**
         * 验证所有站点的结算单都包含正确的 buyerAddress
         *
         * <p><b>Validates: Requirements 1.1</b></p>
         */
        @Test
        @DisplayName("所有站点结算单的 buyerAddress 都应正确设置（未修复时将失败）")
        void testBuildSettlement_AllSites_ShouldHaveBuyerAddress() {
            // Given - 4个站点各有数据
            SettlementInput input = SettlementInput.builder()
                    .periodStart(LocalDate.of(2025, 9, 2))
                    .periodEnd(LocalDate.of(2025, 9, 8))
                    .items(List.of(
                            createItem("MSUS-A001", "USD", "10.00", 5),
                            createItem("MSCA-B001", "CAD", "12.50", 3),
                            createItem("MSUK-C001", "GBP", "8.00", 7),
                            createItem("MSEU-D001", "EUR", "9.50", 4)
                    ))
                    .build();

            // When
            List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);

            // Then - 所有4份结算单的 buyerAddress 都应正确
            assertEquals(4, results.size(), "应生成4份结算单");
            for (SettlementGenerateResult result : results) {
                assertEquals(
                        EXPECTED_BUYER_ADDRESS,
                        result.getSettlement().getBuyerAddress(),
                        "站点 " + result.getSettlement().getSiteCode() + " 的 buyerAddress 应正确设置"
                );
            }
        }
    }


    // ==================== 测试2: writeSettlementExcel 列宽固定不自适应 ====================

    @Nested
    @DisplayName("测试2: 结算单 Excel 列宽固定不自适应")
    class SettlementAutoFitColumnsTest {

        /**
         * 验证 writeSettlementExcel 方法在 addImageOriginal 之前调用了 autoFitColumns(sheet, 6)
         *
         * <p>通过源码分析验证方法调用顺序。在未修复代码上，writeSettlementExcel 末尾有注释
         * "不调用autoFitColumns避免破坏印章定位"，autoFitColumns 未被调用，测试将失败。</p>
         *
         * <p><b>Validates: Requirements 1.3</b></p>
         */
        @Test
        @DisplayName("writeSettlementExcel 应在 addImageOriginal 之前调用 autoFitColumns（未修复时将失败）")
        void testWriteSettlementExcel_ShouldCallAutoFitColumnsBeforeAddImage() throws IOException {
            // Given - 读取 DocumentExportServiceImpl 源码
            Path sourcePath = findSourceFile();
            String sourceCode = Files.readString(sourcePath);

            // 提取 writeSettlementExcel 方法体
            String methodBody = extractMethodBody(sourceCode, "writeSettlementExcel");
            assertNotNull(methodBody, "应能找到 writeSettlementExcel 方法");

            // When - 检查 autoFitColumns 是否在 addImageOriginal 之前被调用
            int autoFitPos = methodBody.indexOf("autoFitColumns(sheet, 6)");
            int addImagePos = methodBody.indexOf("addImageOriginal(");

            // Then - autoFitColumns 应在 addImageOriginal 之前被调用
            // 在未修复代码上，autoFitColumns 未被调用（autoFitPos == -1），测试将失败
            assertTrue(autoFitPos >= 0,
                    "writeSettlementExcel 应调用 autoFitColumns(sheet, 6)，" +
                    "但当前代码中未找到该调用（列宽固定不自适应）");
            assertTrue(addImagePos >= 0,
                    "writeSettlementExcel 应调用 addImageOriginal");
            assertTrue(autoFitPos < addImagePos,
                    "autoFitColumns 应在 addImageOriginal 之前调用，" +
                    "当前 autoFitColumns 位置=" + autoFitPos + ", addImageOriginal 位置=" + addImagePos);
        }
    }

    // ==================== 测试3: writeInvExcel 列宽固定不自适应 ====================

    @Nested
    @DisplayName("测试3: INV Excel 列宽固定不自适应")
    class InvAutoFitColumnsTest {

        /**
         * 验证 writeInvExcel 方法在 addImageOriginal 之前调用了 autoFitColumns(sheet, 8)
         *
         * <p>通过源码分析验证方法调用顺序。在未修复代码上，writeInvExcel 末尾有注释
         * "不调用autoFitColumns避免破坏印章定位"，autoFitColumns 未被调用，测试将失败。</p>
         *
         * <p><b>Validates: Requirements 1.4</b></p>
         */
        @Test
        @DisplayName("writeInvExcel 应在 addImageOriginal 之前调用 autoFitColumns（未修复时将失败）")
        void testWriteInvExcel_ShouldCallAutoFitColumnsBeforeAddImage() throws IOException {
            // Given - 读取 DocumentExportServiceImpl 源码
            Path sourcePath = findSourceFile();
            String sourceCode = Files.readString(sourcePath);

            // 提取 writeInvExcel 方法体
            String methodBody = extractMethodBody(sourceCode, "writeInvExcel");
            assertNotNull(methodBody, "应能找到 writeInvExcel 方法");

            // When - 检查 autoFitColumns 是否在 addImageOriginal 之前被调用
            int autoFitPos = methodBody.indexOf("autoFitColumns(sheet, 8)");
            int addImagePos = methodBody.indexOf("addImageOriginal(");

            // Then - autoFitColumns 应在 addImageOriginal 之前被调用
            // 在未修复代码上，autoFitColumns 未被调用（autoFitPos == -1），测试将失败
            assertTrue(autoFitPos >= 0,
                    "writeInvExcel 应调用 autoFitColumns(sheet, 8)，" +
                    "但当前代码中未找到该调用（列宽固定不自适应）");
            assertTrue(addImagePos >= 0,
                    "writeInvExcel 应调用 addImageOriginal");
            assertTrue(autoFitPos < addImagePos,
                    "autoFitColumns 应在 addImageOriginal 之前调用，" +
                    "当前 autoFitColumns 位置=" + autoFitPos + ", addImageOriginal 位置=" + addImagePos);
        }
    }


    // ==================== 辅助方法 ====================

    /**
     * 创建结算数据明细项
     *
     * @param msku MSKU编码
     * @param currency 货币代码
     * @param unitPrice 单价
     * @param quantity 数量
     * @return 结算数据明细项
     */
    private SettlementInput.SettlementDataItem createItem(String msku, String currency,
                                                           String unitPrice, int quantity) {
        return SettlementInput.SettlementDataItem.builder()
                .siteCode(null)
                .msku(msku)
                .currency(currency)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .build();
    }

    /**
     * 查找 DocumentExportServiceImpl 源码文件路径
     *
     * <p>从当前工作目录向上查找，兼容不同的运行环境。</p>
     *
     * @return 源码文件路径
     */
    private static Path findSourceFile() {
        // 尝试从 musheng-business 模块根目录查找
        Path modulePath = Path.of(EXPORT_SERVICE_SOURCE_PATH);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        // 尝试从项目根目录查找
        Path projectPath = Path.of("musheng-business", EXPORT_SERVICE_SOURCE_PATH);
        if (Files.exists(projectPath)) {
            return projectPath;
        }
        // 尝试从 musheng-tax-system 目录查找
        Path taxSystemPath = Path.of("musheng-tax-system", "musheng-business", EXPORT_SERVICE_SOURCE_PATH);
        if (Files.exists(taxSystemPath)) {
            return taxSystemPath;
        }
        throw new RuntimeException("无法找到 DocumentExportServiceImpl.java 源码文件，" +
                "已尝试路径: " + modulePath + ", " + projectPath + ", " + taxSystemPath);
    }

    /**
     * 从源码中提取指定方法的方法体
     *
     * <p>使用大括号匹配提取方法体内容。</p>
     *
     * @param sourceCode 完整源码
     * @param methodName 方法名
     * @return 方法体字符串，未找到返回 null
     */
    private static String extractMethodBody(String sourceCode, String methodName) {
        // 查找方法签名（private void methodName(...)）
        String pattern = "private\\s+void\\s+" + methodName + "\\s*\\(";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(sourceCode);
        if (!m.find()) {
            return null;
        }

        // 从方法签名开始，找到第一个 { 然后匹配到对应的 }
        int startIdx = m.start();
        int braceStart = sourceCode.indexOf('{', startIdx);
        if (braceStart < 0) {
            return null;
        }

        int depth = 0;
        int endIdx = -1;
        for (int i = braceStart; i < sourceCode.length(); i++) {
            char c = sourceCode.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    endIdx = i;
                    break;
                }
            }
        }

        if (endIdx < 0) {
            return null;
        }

        return sourceCode.substring(braceStart, endIdx + 1);
    }
}
