package com.musheng.business.document.bugfix;

import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保持不变属性测试 - 印章尺寸定位与 PO/DN 导出行为不变
 *
 * <p>在实施修复之前运行此测试，验证基线行为需要保持。
 * 预期结果：测试通过（确认基线行为）。修复后这些测试也必须继续通过（回归防护）。</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.8</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("保持不变属性测试 - 印章尺寸定位与PO/DN导出行为")
class SettlementInvExportPreservationTest {

    /** DocumentExportServiceImpl 源码路径 */
    private static final String EXPORT_SERVICE_SOURCE_PATH =
            "src/main/java/com/musheng/business/document/service/impl/DocumentExportServiceImpl.java";

    // ==================== 印章 EMU 尺寸规格常量 ====================

    /** 慕声红章 EMU 尺寸: 2.1×2.1cm */
    private static final long MUSHENG_STAMP_CX = 756000L;
    private static final long MUSHENG_STAMP_CY = 756000L;

    /** 香港蓝章 EMU 尺寸: 4×4cm */
    private static final long HK_STAMP_CX = 1440000L;
    private static final long HK_STAMP_CY = 1440000L;


    // ==================== Property 2a: 印章 EMU 尺寸保持不变 ====================

    @Nested
    @DisplayName("Property 2a: 印章 EMU 尺寸始终为指定值")
    class StampEmuSizePreservationTest {

        /**
         * 验证结算单中慕声红章 EMU 尺寸始终为 cx=756000, cy=756000
         *
         * <p>通过源码分析验证 writeSettlementExcel 方法中 addImageOriginal 调用的
         * 慕声红章参数。在未修复和修复后的代码上都应通过。</p>
         *
         * <p><b>Validates: Requirements 3.3</b></p>
         */
        @Test
        @DisplayName("结算单慕声红章 EMU 尺寸应为 756000×756000")
        void testSettlement_MushengStamp_ShouldHaveCorrectEmuSize() throws IOException {
            // Given - 读取源码并提取 writeSettlementExcel 方法体
            String methodBody = readMethodBody("writeSettlementExcel");
            assertNotNull(methodBody, "应能找到 writeSettlementExcel 方法");

            // When - 提取慕声红章的 addImageOriginal 调用参数
            // 慕声红章使用 IMG_STAMP_MUSHENG 资源
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall mushengCall = findCallByResource(calls, "IMG_STAMP_MUSHENG");

            // Then - 验证 EMU 尺寸
            assertNotNull(mushengCall, "writeSettlementExcel 应包含慕声红章的 addImageOriginal 调用");
            assertEquals(MUSHENG_STAMP_CX, mushengCall.cx,
                    "结算单慕声红章 cx 应为 " + MUSHENG_STAMP_CX + " EMU");
            assertEquals(MUSHENG_STAMP_CY, mushengCall.cy,
                    "结算单慕声红章 cy 应为 " + MUSHENG_STAMP_CY + " EMU");
        }

        /**
         * 验证结算单中香港蓝章 EMU 尺寸始终为 cx=1440000, cy=1440000
         *
         * <p><b>Validates: Requirements 3.3</b></p>
         */
        @Test
        @DisplayName("结算单香港蓝章 EMU 尺寸应为 1440000×1440000")
        void testSettlement_HkStamp_ShouldHaveCorrectEmuSize() throws IOException {
            // Given
            String methodBody = readMethodBody("writeSettlementExcel");
            assertNotNull(methodBody, "应能找到 writeSettlementExcel 方法");

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall hkCall = findCallByResource(calls, "IMG_STAMP_HK");

            // Then
            assertNotNull(hkCall, "writeSettlementExcel 应包含香港蓝章的 addImageOriginal 调用");
            assertEquals(HK_STAMP_CX, hkCall.cx,
                    "结算单香港蓝章 cx 应为 " + HK_STAMP_CX + " EMU");
            assertEquals(HK_STAMP_CY, hkCall.cy,
                    "结算单香港蓝章 cy 应为 " + HK_STAMP_CY + " EMU");
        }

        /**
         * 验证 INV 中慕声红章 EMU 尺寸始终为 cx=756000, cy=756000
         *
         * <p><b>Validates: Requirements 3.3</b></p>
         */
        @Test
        @DisplayName("INV 慕声红章 EMU 尺寸应为 756000×756000")
        void testInv_MushengStamp_ShouldHaveCorrectEmuSize() throws IOException {
            // Given
            String methodBody = readMethodBody("writeInvExcel");
            assertNotNull(methodBody, "应能找到 writeInvExcel 方法");

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall mushengCall = findCallByResource(calls, "IMG_STAMP_MUSHENG");

            // Then
            assertNotNull(mushengCall, "writeInvExcel 应包含慕声红章的 addImageOriginal 调用");
            assertEquals(MUSHENG_STAMP_CX, mushengCall.cx,
                    "INV 慕声红章 cx 应为 " + MUSHENG_STAMP_CX + " EMU");
            assertEquals(MUSHENG_STAMP_CY, mushengCall.cy,
                    "INV 慕声红章 cy 应为 " + MUSHENG_STAMP_CY + " EMU");
        }
    }


    // ==================== Property 2b: 印章 from 坐标和 oneCellAnchor 定位不变 ====================

    @Nested
    @DisplayName("Property 2b: 印章 from 坐标和 oneCellAnchor 定位方式不变")
    class StampAnchorPreservationTest {

        /**
         * 验证结算单慕声红章的 from 坐标不变
         *
         * <p>from 坐标: col1=1, dx1=1685925, row1=rowIdx, dy1=28575</p>
         *
         * <p><b>Validates: Requirements 3.4</b></p>
         */
        @Test
        @DisplayName("结算单慕声红章 from 坐标应保持不变")
        void testSettlement_MushengStamp_FromCoordinatesShouldBePreserved() throws IOException {
            // Given
            String methodBody = readMethodBody("writeSettlementExcel");
            assertNotNull(methodBody, "应能找到 writeSettlementExcel 方法");

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall mushengCall = findCallByResource(calls, "IMG_STAMP_MUSHENG");

            // Then - 验证 from 坐标（col1, dx1, dy1 为固定值；row1 为动态 rowIdx）
            assertNotNull(mushengCall, "应找到慕声红章调用");
            assertEquals(1, mushengCall.col1, "慕声红章 col1 应为 1");
            assertEquals(1685925, mushengCall.dx1, "慕声红章 dx1 应为 1685925");
            assertEquals(28575, mushengCall.dy1, "慕声红章 dy1 应为 28575");
        }

        /**
         * 验证结算单香港蓝章的 from 坐标不变
         *
         * <p>from 坐标: col1=4, dx1=447675, row1=rowIdx-1, dy1=171450</p>
         *
         * <p><b>Validates: Requirements 3.4</b></p>
         */
        @Test
        @DisplayName("结算单香港蓝章 from 坐标应保持不变")
        void testSettlement_HkStamp_FromCoordinatesShouldBePreserved() throws IOException {
            // Given
            String methodBody = readMethodBody("writeSettlementExcel");
            assertNotNull(methodBody, "应能找到 writeSettlementExcel 方法");

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall hkCall = findCallByResource(calls, "IMG_STAMP_HK");

            // Then
            assertNotNull(hkCall, "应找到香港蓝章调用");
            assertEquals(4, hkCall.col1, "香港蓝章 col1 应为 4");
            assertEquals(447675, hkCall.dx1, "香港蓝章 dx1 应为 447675");
            assertEquals(171450, hkCall.dy1, "香港蓝章 dy1 应为 171450");
        }

        /**
         * 验证 INV 慕声红章的 from 坐标不变
         *
         * <p>from 坐标: col1=5, dx1=552450, row1=rowIdx-5, dy1=285750</p>
         *
         * <p><b>Validates: Requirements 3.4</b></p>
         */
        @Test
        @DisplayName("INV 慕声红章 from 坐标应保持不变")
        void testInv_MushengStamp_FromCoordinatesShouldBePreserved() throws IOException {
            // Given
            String methodBody = readMethodBody("writeInvExcel");
            assertNotNull(methodBody, "应能找到 writeInvExcel 方法");

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall mushengCall = findCallByResource(calls, "IMG_STAMP_MUSHENG");

            // Then
            assertNotNull(mushengCall, "应找到慕声红章调用");
            assertEquals(5, mushengCall.col1, "INV 慕声红章 col1 应为 5");
            assertEquals(552450, mushengCall.dx1, "INV 慕声红章 dx1 应为 552450");
            assertEquals(285750, mushengCall.dy1, "INV 慕声红章 dy1 应为 285750");
        }

        /**
         * 验证 addImageOriginal 方法使用 oneCellAnchor 定位方式
         *
         * <p>addImageOriginal 方法内部将 twoCellAnchor 转换为 oneCellAnchor，
         * 确保图片尺寸由 ext 的 cx/cy 直接控制，不受列宽变化影响。</p>
         *
         * <p><b>Validates: Requirements 3.4</b></p>
         */
        @Test
        @DisplayName("addImageOriginal 应使用 oneCellAnchor 定位方式")
        void testAddImageOriginal_ShouldUseOneCellAnchor() throws IOException {
            // Given - 读取 addImageOriginal 方法体
            String methodBody = readMethodBody("addImageOriginal");
            assertNotNull(methodBody, "应能找到 addImageOriginal 方法");

            // Then - 验证关键的 oneCellAnchor 转换逻辑存在
            assertTrue(methodBody.contains("addNewOneCellAnchor"),
                    "addImageOriginal 应创建 oneCellAnchor");
            assertTrue(methodBody.contains("removeTwoCellAnchor"),
                    "addImageOriginal 应删除原始 twoCellAnchor");
            assertTrue(methodBody.contains("addNewFrom"),
                    "oneCellAnchor 应设置 from 坐标");
            assertTrue(methodBody.contains("addNewExt"),
                    "oneCellAnchor 应设置 ext 尺寸");
            assertTrue(methodBody.contains("setCx(cx)"),
                    "ext 应设置 cx 值");
            assertTrue(methodBody.contains("setCy(cy)"),
                    "ext 应设置 cy 值");
        }
    }


    // ==================== Property 2c: PO/DN 导出 autoFitColumns 行为不变 ====================

    @Nested
    @DisplayName("Property 2c: PO/DN 导出 autoFitColumns 行为不变")
    class PoDnAutoFitColumnsPreservationTest {

        /**
         * 验证 PO 导出中 autoFitColumns 调用保持不变
         *
         * <p><b>Validates: Requirements 3.1, 3.8</b></p>
         */
        @Test
        @DisplayName("writePoExcel 应调用 autoFitColumns(sheet, 4)")
        void testWritePoExcel_ShouldCallAutoFitColumns() throws IOException {
            // Given
            String methodBody = readMethodBody("writePoExcel");
            assertNotNull(methodBody, "应能找到 writePoExcel 方法");

            // Then - PO 有4列（A-D），autoFitColumns(sheet, 4) 应存在
            assertTrue(methodBody.contains("autoFitColumns(sheet, 4)"),
                    "writePoExcel 应调用 autoFitColumns(sheet, 4)");
        }

        /**
         * 验证 DN 导出中 autoFitColumns 调用保持不变
         *
         * <p><b>Validates: Requirements 3.2, 3.8</b></p>
         */
        @Test
        @DisplayName("writeDnExcel 应调用 autoFitColumns(sheet, 6)")
        void testWriteDnExcel_ShouldCallAutoFitColumns() throws IOException {
            // Given
            String methodBody = readMethodBody("writeDnExcel");
            assertNotNull(methodBody, "应能找到 writeDnExcel 方法");

            // Then - DN 有6列（A-F），autoFitColumns(sheet, 6) 应存在
            assertTrue(methodBody.contains("autoFitColumns(sheet, 6)"),
                    "writeDnExcel 应调用 autoFitColumns(sheet, 6)");
        }
    }


    // ==================== jqwik 属性测试: 印章 EMU 尺寸对所有导出方法保持不变 ====================

    /**
     * 属性测试: 对于所有结算单/INV 导出方法，印章 EMU 尺寸始终为指定值
     *
     * <p>使用 jqwik 属性测试框架，对所有包含印章嵌入的导出方法进行参数化验证，
     * 确保无论哪个方法，印章的 cx/cy 值都严格等于规格值。</p>
     *
     * <p><b>Validates: Requirements 3.3, 3.4</b></p>
     */
    @Group
    @DisplayName("jqwik 属性测试: 印章 EMU 尺寸与定位")
    class StampEmuPropertyTests {

        /**
         * 属性: 对于所有包含印章的导出方法，慕声红章 EMU 尺寸始终为 756000×756000
         *
         * <p><b>Validates: Requirements 3.3</b></p>
         */
        @Property
        @Label("所有导出方法中慕声红章 EMU 尺寸始终为 756000×756000")
        void mushengStampEmuSize_ShouldAlwaysBe756000(
                @ForAll("stampExportMethods") String methodName) throws IOException {
            // Given - 读取指定方法体
            String methodBody = readMethodBody(methodName);
            assertNotNull(methodBody, "应能找到方法: " + methodName);

            // When - 提取慕声红章调用
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall mushengCall = findCallByResource(calls, "IMG_STAMP_MUSHENG");

            // Then - 如果该方法包含慕声红章，验证尺寸
            if (mushengCall != null) {
                assertEquals(MUSHENG_STAMP_CX, mushengCall.cx,
                        methodName + " 中慕声红章 cx 应为 " + MUSHENG_STAMP_CX);
                assertEquals(MUSHENG_STAMP_CY, mushengCall.cy,
                        methodName + " 中慕声红章 cy 应为 " + MUSHENG_STAMP_CY);
            }
        }

        /**
         * 属性: 对于所有包含印章的导出方法，香港蓝章 EMU 尺寸始终为 1440000×1440000
         *
         * <p><b>Validates: Requirements 3.3</b></p>
         */
        @Property
        @Label("所有导出方法中香港蓝章 EMU 尺寸始终为 1440000×1440000")
        void hkStampEmuSize_ShouldAlwaysBe1440000(
                @ForAll("stampExportMethods") String methodName) throws IOException {
            // Given
            String methodBody = readMethodBody(methodName);
            assertNotNull(methodBody, "应能找到方法: " + methodName);

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);
            AddImageCall hkCall = findCallByResource(calls, "IMG_STAMP_HK");

            // Then - 如果该方法包含香港蓝章，验证尺寸
            if (hkCall != null) {
                assertEquals(HK_STAMP_CX, hkCall.cx,
                        methodName + " 中香港蓝章 cx 应为 " + HK_STAMP_CX);
                assertEquals(HK_STAMP_CY, hkCall.cy,
                        methodName + " 中香港蓝章 cy 应为 " + HK_STAMP_CY);
            }
        }

        /**
         * 属性: 对于所有包含印章的导出方法，addImageOriginal 的 from 坐标中
         * col1 和 dx1/dy1 偏移量为非负整数
         *
         * <p><b>Validates: Requirements 3.4</b></p>
         */
        @Property
        @Label("所有导出方法中印章 from 坐标偏移量为非负整数")
        void stampFromCoordinates_ShouldBeNonNegative(
                @ForAll("stampExportMethods") String methodName) throws IOException {
            // Given
            String methodBody = readMethodBody(methodName);
            assertNotNull(methodBody, "应能找到方法: " + methodName);

            // When
            List<AddImageCall> calls = extractAddImageOriginalCalls(methodBody);

            // Then - 所有印章调用的 from 坐标偏移量应为非负
            for (AddImageCall call : calls) {
                assertTrue(call.col1 >= 0,
                        methodName + " 中 " + call.resourceName + " 的 col1 应 >= 0");
                assertTrue(call.dx1 >= 0,
                        methodName + " 中 " + call.resourceName + " 的 dx1 应 >= 0");
                assertTrue(call.dy1 >= 0,
                        methodName + " 中 " + call.resourceName + " 的 dy1 应 >= 0");
                assertTrue(call.cx > 0,
                        methodName + " 中 " + call.resourceName + " 的 cx 应 > 0");
                assertTrue(call.cy > 0,
                        methodName + " 中 " + call.resourceName + " 的 cy 应 > 0");
            }
        }

        /**
         * 提供所有包含印章嵌入的导出方法名
         *
         * @return 方法名的 Arbitrary
         */
        @Provide
        Arbitrary<String> stampExportMethods() {
            return Arbitraries.of(
                    "writeSettlementExcel",
                    "writeInvExcel",
                    "writeDnExcel"
            );
        }
    }


    // ==================== 辅助方法与数据结构 ====================

    /**
     * addImageOriginal 调用参数的数据结构
     */
    private static class AddImageCall {
        String resourceName;
        int col1;
        long dx1;
        // row1 可能是表达式（如 rowIdx），不解析为固定值
        String row1Expr;
        long dy1;
        long cx;
        long cy;

        @Override
        public String toString() {
            return String.format("AddImageCall{resource=%s, col1=%d, dx1=%d, row1=%s, dy1=%d, cx=%d, cy=%d}",
                    resourceName, col1, dx1, row1Expr, dy1, cx, cy);
        }
    }

    /**
     * 从 addImageOriginal 调用中提取参数
     *
     * <p>解析形如:
     * <pre>
     * addImageOriginal(wb, sheet, IMG_STAMP_MUSHENG,
     *     1, 1685925, rowIdx, 28575,
     *     2, 1685925, 2, 28575,
     *     756000, 756000);
     * </pre>
     * 的调用，提取资源名和关键参数。</p>
     *
     * @param methodBody 方法体源码
     * @return 所有 addImageOriginal 调用的参数列表
     */
    private static List<AddImageCall> extractAddImageOriginalCalls(String methodBody) {
        List<AddImageCall> result = new ArrayList<>();

        // 匹配 addImageOriginal(wb, sheet, RESOURCE_NAME, 后面跟着参数直到 );
        Pattern callPattern = Pattern.compile(
                "addImageOriginal\\s*\\(\\s*wb\\s*,\\s*sheet\\s*,\\s*(\\w+)\\s*,([^;]+?)\\);",
                Pattern.DOTALL);
        Matcher callMatcher = callPattern.matcher(methodBody);

        while (callMatcher.find()) {
            String resourceName = callMatcher.group(1);
            String paramsStr = callMatcher.group(2);

            // 移除换行和多余空格
            paramsStr = paramsStr.replaceAll("\\s+", " ").trim();

            // 按逗号分割参数
            String[] params = paramsStr.split("\\s*,\\s*");

            if (params.length >= 10) {
                AddImageCall call = new AddImageCall();
                call.resourceName = resourceName;

                // 参数顺序: col1, dx1, row1, dy1, col2, dx2, rowSpan, dy2, cx, cy
                call.col1 = parseIntSafe(params[0].trim());
                call.dx1 = parseLongSafe(params[1].trim());
                call.row1Expr = params[2].trim();
                call.dy1 = parseLongSafe(params[3].trim());
                // params[4..7] 是 col2, dx2, rowSpan, dy2（不需要验证）
                call.cx = parseLongSafe(params[8].trim());
                call.cy = parseLongSafe(params[9].trim());

                result.add(call);
            }
        }

        return result;
    }

    /**
     * 根据资源名查找 addImageOriginal 调用
     *
     * @param calls 所有调用列表
     * @param resourceName 资源常量名（如 "IMG_STAMP_MUSHENG"）
     * @return 匹配的调用，未找到返回 null
     */
    private static AddImageCall findCallByResource(List<AddImageCall> calls, String resourceName) {
        return calls.stream()
                .filter(c -> c.resourceName.equals(resourceName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 安全解析整数，表达式返回 -1
     */
    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 安全解析长整数，表达式返回 -1
     */
    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 读取指定方法的方法体
     *
     * @param methodName 方法名
     * @return 方法体字符串，未找到返回 null
     * @throws IOException 读取文件失败
     */
    private static String readMethodBody(String methodName) throws IOException {
        Path sourcePath = findSourceFile();
        String sourceCode = Files.readString(sourcePath);
        return extractMethodBody(sourceCode, methodName);
    }

    /**
     * 查找 DocumentExportServiceImpl 源码文件路径
     *
     * @return 源码文件路径
     */
    private static Path findSourceFile() {
        Path modulePath = Path.of(EXPORT_SERVICE_SOURCE_PATH);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        Path projectPath = Path.of("musheng-business", EXPORT_SERVICE_SOURCE_PATH);
        if (Files.exists(projectPath)) {
            return projectPath;
        }
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
     * @param sourceCode 完整源码
     * @param methodName 方法名
     * @return 方法体字符串，未找到返回 null
     */
    private static String extractMethodBody(String sourceCode, String methodName) {
        // 匹配 private 或 public 方法签名
        String pattern = "(private|public|protected)\\s+[\\w<>\\[\\],\\s]+\\s+" + methodName + "\\s*\\(";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(sourceCode);
        if (!m.find()) {
            return null;
        }

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
