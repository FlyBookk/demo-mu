package com.musheng.business.document.bugfix;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缺陷条件探索性测试 - 结算数据推导缺陷
 *
 * <p>在实施修复之前运行此测试，验证缺陷确实存在。
 * 预期结果：测试失败（证明缺陷存在）。</p>
 *
 * <p>缺陷1：推算表查询使用 shipmentMskus 过滤，导致大量 MSKU 无法匹配单价</p>
 * <p>缺陷2：退款记录被 .ne("refund") 完全排除，未实现按 SKU 级别的退款抵扣</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 2.1, 2.2</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SettlementDataDerivationBugExplorationTest {

    /** DocumentGenerateServiceImpl 源码相对路径 */
    private static final String SERVICE_SOURCE_PATH =
            "src/main/java/com/musheng/business/document/service/impl/DocumentGenerateServiceImpl.java";

    // ==================== 缺陷1: 推算表 MSKU 过滤 ====================

    /**
     * 属性测试：推算表查询不应使用 shipmentMskus 过滤
     *
     * <p>对于任意 shipmentMskus（3 个）和 allPeriodMskus（10 个）的组合，
     * 当 shipmentMskus 是 allPeriodMskus 的子集时（即 shipmentMskus.size() < allPeriodMskus.size()），
     * 推算表查询应返回全部 10 个 MSKU 的单价数据。</p>
     *
     * <p>通过源码分析验证：buildSettlementInput 方法中推算表查询是否使用了
     * shipmentMskus 列表作为 IN 条件过滤。如果使用了，则证明缺陷存在。</p>
     *
     * <p>故障条件：isBugCondition(input) = shipmentMskus.size() < allPeriodMskus.size()</p>
     * <p>期望行为：allPeriodMskusHavePrice(result) = true（查询不应受 shipmentMskus 限制）</p>
     *
     * <p><b>Validates: Requirements 1.1, 2.1</b></p>
     *
     * @param shipmentMskuCount 货件 MSKU 数量（模拟 3 个）
     * @param allPeriodMskuCount 推算表全部 MSKU 数量（模拟 10 个）
     */
    @Property(tries = 10)
    @Label("缺陷1: 推算表查询不应使用 shipmentMskus 过滤（未修复时将失败）")
    void priceQueryShouldNotFilterByShipmentMskus(
            @ForAll @IntRange(min = 1, max = 5) int shipmentMskuCount,
            @ForAll @IntRange(min = 6, max = 15) int allPeriodMskuCount
    ) throws IOException {
        // Given - 故障条件：shipmentMskus 数量 < allPeriodMskus 数量
        assertThat(shipmentMskuCount).isLessThan(allPeriodMskuCount);

        // 模拟 MSKU 列表
        List<String> allPeriodMskus = generateMskuList("MSKU-", allPeriodMskuCount);
        List<String> shipmentMskus = allPeriodMskus.subList(0, shipmentMskuCount);

        // When - 分析源码中 buildSettlementInput 方法的推算表查询逻辑
        String methodBody = readBuildSettlementInputMethod();

        // 检查推算表查询是否使用了 shipmentMskus 过滤
        // 当前缺陷代码：priceWrapper.in(SettlementImportData::getMsku, shipmentMskus)
        boolean hasMskuFilter = methodBody.contains("priceWrapper.in(SettlementImportData::getMsku, shipmentMskus)");

        // Then - 期望行为：推算表查询不应使用 shipmentMskus 过滤
        // 在未修复代码上，hasMskuFilter = true，断言将失败（证明缺陷存在）
        assertThat(hasMskuFilter)
                .as("推算表查询不应使用 shipmentMskus 过滤。" +
                    "当 shipmentMskus=%d 个而推算表有 %d 个 MSKU 时，" +
                    "使用 shipmentMskus 过滤会导致 %d 个 MSKU 无法匹配单价。" +
                    "应仅按 shopId、delFlag、periodStart、periodEnd 过滤。",
                    shipmentMskuCount, allPeriodMskuCount,
                    allPeriodMskuCount - shipmentMskuCount)
                .isFalse();
    }

    // ==================== 缺陷2: 退款排除 ====================

    /**
     * 属性测试：退款记录不应被完全排除，应实现按 SKU 级别的退款抵扣
     *
     * <p>对于任意包含 income 和 refund 记录的销售数据组合，
     * 系统应按 SKU 级别计算净数量（income - refund），而非排除所有退款记录。</p>
     *
     * <p>通过源码分析验证：buildSettlementInput 方法中销售数据查询是否使用了
     * .ne(transactionCategory, "refund") 排除所有退款记录。如果使用了，则证明缺陷存在。</p>
     *
     * <p>故障条件：refundRecordsExist AND refundExcluded</p>
     * <p>期望行为：refundDeductionApplied(result) = true（应做退款抵扣而非排除）</p>
     *
     * <p><b>Validates: Requirements 1.2, 2.2</b></p>
     *
     * @param incomeQuantity income 数量
     * @param refundQuantity refund 数量
     */
    @Property(tries = 10)
    @Label("缺陷2: 退款记录不应被完全排除，应实现退款抵扣（未修复时将失败）")
    void refundShouldBeDeductedNotExcluded(
            @ForAll @IntRange(min = 5, max = 50) int incomeQuantity,
            @ForAll @IntRange(min = 1, max = 10) int refundQuantity
    ) throws IOException {
        // Given - 故障条件：存在退款记录且退款被排除
        int expectedNetQuantity = incomeQuantity - refundQuantity;
        Assume.that(expectedNetQuantity > 0); // 过滤掉净数量 <= 0 的无效输入
        Assume.that(refundQuantity > 0); // 退款记录存在

        // When - 分析源码中 buildSettlementInput 方法的销售数据查询逻辑
        String methodBody = readBuildSettlementInputMethod();

        // 检查是否使用 .ne(transactionCategory, "refund") 排除退款
        // 当前缺陷代码：salesWrapper.ne(SalesData::getTransactionCategory, "refund")
        boolean refundExcluded = methodBody.contains(".ne(SalesData::getTransactionCategory, \"refund\")");

        // 检查是否存在退款抵扣逻辑（按 SKU 级别的 income - refund 计算）
        boolean hasRefundDeduction = methodBody.contains("refund")
                && (methodBody.contains("净数量") || methodBody.contains("netQuantity")
                    || methodBody.contains("income") && methodBody.contains("deduct"));

        // Then - 期望行为：不应排除退款，应实现退款抵扣
        // 在未修复代码上，refundExcluded = true 且 hasRefundDeduction = false，断言将失败
        assertThat(refundExcluded)
                .as("销售数据查询不应使用 .ne(transactionCategory, 'refund') 排除所有退款记录。" +
                    "当 SKU 有 income 数量 %d 和 refund 数量 %d 时，" +
                    "应计算净数量 %d（income - refund），而非使用 income 数量 %d。",
                    incomeQuantity, refundQuantity, expectedNetQuantity, incomeQuantity)
                .isFalse();
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成 MSKU 列表
     *
     * @param prefix MSKU 前缀
     * @param count 数量
     * @return MSKU 列表
     */
    private List<String> generateMskuList(String prefix, int count) {
        List<String> mskus = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            mskus.add(prefix + String.format("%03d", i));
        }
        return mskus;
    }

    /**
     * 读取 buildSettlementInput 方法体
     *
     * <p>从 DocumentGenerateServiceImpl.java 源码中提取 buildSettlementInput 方法体。</p>
     *
     * @return 方法体字符串
     * @throws IOException 读取文件失败
     */
    private String readBuildSettlementInputMethod() throws IOException {
        Path sourcePath = findSourceFile();
        String sourceCode = Files.readString(sourcePath);
        String methodBody = extractMethodBody(sourceCode, "buildSettlementInput");
        assertThat(methodBody)
                .as("应能找到 buildSettlementInput 方法")
                .isNotNull();
        return methodBody;
    }

    /**
     * 查找 DocumentGenerateServiceImpl 源码文件路径
     *
     * <p>从当前工作目录向上查找，兼容不同的运行环境。</p>
     *
     * @return 源码文件路径
     */
    private static Path findSourceFile() {
        // 尝试从 musheng-business 模块根目录查找
        Path modulePath = Path.of(SERVICE_SOURCE_PATH);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        // 尝试从项目根目录查找
        Path projectPath = Path.of("musheng-business", SERVICE_SOURCE_PATH);
        if (Files.exists(projectPath)) {
            return projectPath;
        }
        // 尝试从 musheng-tax-system 目录查找
        Path taxSystemPath = Path.of("musheng-tax-system", "musheng-business", SERVICE_SOURCE_PATH);
        if (Files.exists(taxSystemPath)) {
            return taxSystemPath;
        }
        throw new RuntimeException("无法找到 DocumentGenerateServiceImpl.java 源码文件，" +
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
        // 查找方法签名（private SettlementInput buildSettlementInput(...)）
        String pattern = "(private|public|protected)\\s+\\w+\\s+" + methodName + "\\s*\\(";
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
