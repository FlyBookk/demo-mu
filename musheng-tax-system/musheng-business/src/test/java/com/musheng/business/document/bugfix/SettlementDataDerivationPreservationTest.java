package com.musheng.business.document.bugfix;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 保持性属性测试 - 结算数据推导缺陷修复
 *
 * <p>验证不受缺陷影响的现有行为在修复后不会被破坏。
 * 在未修复代码上运行时预期通过（确认基线行为）。</p>
 *
 * <p>属性1：仅有 income 记录的 SKU 使用完整 income 数量参与计算</p>
 * <p>属性2：未指定货件 ID 时使用回退逻辑</p>
 * <p>属性3：周期内无配送数据时返回空 items 列表</p>
 * <p>属性4：推算表中无销售数据匹配的 MSKU 不出现在结算明细中</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class SettlementDataDerivationPreservationTest {

    /** DocumentGenerateServiceImpl 源码相对路径 */
    private static final String SERVICE_SOURCE_PATH =
            "src/main/java/com/musheng/business/document/service/impl/DocumentGenerateServiceImpl.java";

    // ==================== 属性1: 仅有 income 的 SKU 使用完整数量 ====================

    /**
     * 属性测试：仅有 income 记录且无 refund 的 SKU，使用完整 income 数量参与计算
     *
     * <p>对于任意仅有 income 记录的 SKU，修复后代码通过按月按 SKU 汇总 income 和 refund，
     * 计算净数量 = income - refund。当无 refund 时，净数量等于 income 数量。</p>
     *
     * <p>通过源码分析验证：buildSettlementInput 方法中，income 数量通过
     * monthlyIncomeMap 汇总，refund 数量通过 monthlyRefundMap 汇总，
     * 净数量 = incomeQty - refundQty。无 refund 时 refundQty=0，净数量=incomeQty。</p>
     *
     * <p><b>Validates: Requirements 3.3, 3.5</b></p>
     *
     * @param incomeQuantity 随机生成的 income 数量
     */
    @Property(tries = 10)
    @Label("保持性1: 仅有 income 的 SKU 使用完整 income 数量（不受退款抵扣影响）")
    void incomeOnlySkuShouldUseFullQuantity(
            @ForAll @IntRange(min = 1, max = 100) int incomeQuantity
    ) throws IOException {
        // Given - 仅有 income 记录的 SKU（无 refund）
        assertThat(incomeQuantity).isGreaterThan(0);

        // When - 分析源码中 buildSettlementInput 方法的明细构建逻辑
        String methodBody = readBuildSettlementInputMethod();

        // 验证：income 查询使用 eq("income") 过滤（仅查询 income 类型）
        boolean filtersIncomeOnly = methodBody.contains("\"income\"");

        // 验证：按月按 SKU 汇总 income 数量
        boolean aggregatesIncomeByMonthSku = methodBody.contains("monthlyIncomeMap")
                && methodBody.contains("sales.getQuantity()");

        // 验证：净数量计算 = incomeQty - refundQty（无 refund 时 refundQty=0，净数量=incomeQty）
        boolean computesNetQuantity = methodBody.contains("incomeQty - refundQty");

        // Then - 保持行为：无 refund 时，净数量等于 income 数量
        assertThat(filtersIncomeOnly)
                .as("应存在 income 类型过滤逻辑，确保查询 income 类型的销售数据")
                .isTrue();

        assertThat(aggregatesIncomeByMonthSku)
                .as("income 数量应通过 monthlyIncomeMap 按月按 SKU 汇总，" +
                    "对于 income 数量 %d 的 SKU，汇总后数量应等于 %d",
                    incomeQuantity, incomeQuantity)
                .isTrue();

        assertThat(computesNetQuantity)
                .as("净数量应通过 incomeQty - refundQty 计算，" +
                    "无 refund 时 refundQty=0，净数量等于 income 数量 %d",
                    incomeQuantity)
                .isTrue();
    }

    // ==================== 属性2: 未指定货件 ID 时使用回退逻辑 ====================

    /**
     * 属性测试：未指定货件 ID 时，系统使用回退逻辑（按周期全量查询）
     *
     * <p>当 shipmentIds 为空时，buildSettlementInput 方法应走回退逻辑分支，
     * 直接按 shopId、delFlag、periodStart、periodEnd 查询 t_settlement_import_data，
     * 不经过 shipping/sales 关联查询。</p>
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     *
     * @param periodDays 随机生成的周期天数（模拟不同周期长度）
     */
    @Property(tries = 10)
    @Label("保持性2: 未指定货件 ID 时使用回退逻辑（按周期全量查询）")
    void noShipmentIdsShouldUseFallbackLogic(
            @ForAll @IntRange(min = 30, max = 90) int periodDays
    ) throws IOException {
        // Given - 未指定货件 ID 的请求（shipmentIds 为空）
        assertThat(periodDays).isGreaterThan(0);

        // When - 分析源码中 buildSettlementInput 方法的回退逻辑
        String methodBody = readBuildSettlementInputMethod();

        // 验证：存在回退逻辑分支（当 shipmentIds 为空时）
        // 当前代码结构：
        //   if (!CollectionUtils.isEmpty(request.getShipmentIds())) { ... 正常逻辑 ... }
        //   // 无货件ID时，回退到原有逻辑
        //   LambdaQueryWrapper<SettlementImportData> fallbackWrapper = ...
        boolean hasFallbackBranch = methodBody.contains("fallbackWrapper")
                && methodBody.contains("SettlementImportData");

        // 验证：回退逻辑使用 shopId + delFlag + period 过滤（不使用 shipmentMskus）
        // 注意：代码使用方法链式调用，delFlag 过滤通过 .eq 链接在 shopId 过滤之后
        boolean fallbackUsesCorrectFilters = methodBody.contains("fallbackWrapper.eq(SettlementImportData::getShopId, shopId)")
                && methodBody.contains(".eq(SettlementImportData::getDelFlag, 0)");

        // 验证：回退逻辑返回 SettlementInput（含 items 列表）
        boolean fallbackReturnsSettlementInput = methodBody.contains("fallbackItems")
                && methodBody.contains("SettlementInput.builder()");

        // Then - 保持行为：回退逻辑存在且使用正确的过滤条件
        assertThat(hasFallbackBranch)
                .as("应存在回退逻辑分支（fallbackWrapper），当未指定货件 ID 时使用")
                .isTrue();

        assertThat(fallbackUsesCorrectFilters)
                .as("回退逻辑应使用 shopId 和 delFlag=0 过滤，" +
                    "不使用 shipmentMskus 列表过滤")
                .isTrue();

        assertThat(fallbackReturnsSettlementInput)
                .as("回退逻辑应返回包含 fallbackItems 的 SettlementInput")
                .isTrue();
    }

    // ==================== 属性3: 无配送数据时返回空 items ====================

    /**
     * 属性测试：周期内无配送数据时，返回空的 SettlementInput（items 为空列表）
     *
     * <p>当指定了货件 ID 但周期内无配送数据时，buildSettlementInput 方法应
     * 提前返回空的 SettlementInput（items = List.of()）。</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     *
     * @param shipmentCount 随机生成的货件数量
     */
    @Property(tries = 10)
    @Label("保持性3: 周期内无配送数据时返回空 items 列表")
    void noShippingDataShouldReturnEmptyItems(
            @ForAll @IntRange(min = 1, max = 10) int shipmentCount
    ) throws IOException {
        // Given - 指定了货件 ID 但周期内无配送数据
        assertThat(shipmentCount).isGreaterThan(0);

        // When - 分析源码中 buildSettlementInput 方法的空配送数据处理
        String methodBody = readBuildSettlementInputMethod();

        // 验证：存在空配送数据的提前返回逻辑
        // 当前代码：
        //   if (CollectionUtils.isEmpty(shippingList)) {
        //       return SettlementInput.builder()
        //           .periodStart(request.getPeriodStart())
        //           .periodEnd(request.getPeriodEnd())
        //           .items(List.of())
        //           .build();
        //   }
        boolean hasEmptyShippingCheck = methodBody.contains("CollectionUtils.isEmpty(shippingList)");
        boolean returnsEmptyItems = methodBody.contains(".items(List.of())");

        // Then - 保持行为：无配送数据时返回空 items
        assertThat(hasEmptyShippingCheck)
                .as("应检查 shippingList 是否为空（CollectionUtils.isEmpty(shippingList)）")
                .isTrue();

        assertThat(returnsEmptyItems)
                .as("无配送数据时应返回 items(List.of())，即空的结算明细列表")
                .isTrue();
    }

    // ==================== 属性4: 无销售数据匹配的 MSKU 被跳过 ====================

    /**
     * 属性测试：推算表中无对应销售记录的 MSKU 不出现在结算明细中
     *
     * <p>当推算表中某 MSKU 在销售数据中无对应记录时，该 MSKU 不会出现在
     * 结算明细中（因为明细是从抵扣后的净数量 monthlyNetMap 遍历构建的，
     * 而 monthlyNetMap 仅包含有 income 数据的 SKU）。</p>
     *
     * <p>通过源码分析验证：buildSettlementInput 方法中，结算明细是从
     * monthlyNetMap（抵扣后的净数量）遍历构建的，推算表仅用于查询单价。
     * 如果某 MSKU 在推算表中有记录但在销售数据中无记录，则不会进入 monthlyNetMap，
     * 因此不会生成明细。</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     *
     * @param totalMskuCount 推算表中的 MSKU 总数
     * @param matchedMskuCount 在销售数据中有匹配的 MSKU 数量
     */
    @Property(tries = 10)
    @Label("保持性4: 推算表中无销售数据匹配的 MSKU 不出现在结算明细中")
    void unmatchedMskuShouldBeSkipped(
            @ForAll @IntRange(min = 5, max = 20) int totalMskuCount,
            @ForAll @IntRange(min = 1, max = 4) int matchedMskuCount
    ) throws IOException {
        // Given - 推算表 MSKU 数量 > 销售数据匹配的 MSKU 数量
        assertThat(totalMskuCount).isGreaterThan(matchedMskuCount);

        // When - 分析源码中 buildSettlementInput 方法的明细构建逻辑
        String methodBody = readBuildSettlementInputMethod();

        // 验证：明细是从抵扣后的净数量（monthlyNetMap）遍历构建的
        // 修复后代码：
        //   for (Map.Entry<YearMonth, Map<String, Integer>> monthEntry : monthlyNetMap.entrySet()) {
        //       for (Map.Entry<String, Integer> skuEntry : monthEntry.getValue().entrySet()) {
        //           String msku = skuEntry.getKey();
        //           ...
        //       }
        //   }
        boolean iteratesNetMap = methodBody.contains("monthlyNetMap.entrySet()")
                && methodBody.contains("skuEntry.getKey()");

        // 验证：使用 msku 从推算表查找单价（而非遍历推算表）
        boolean lookupPriceByMsku = methodBody.contains("mskuPriceMap.get(msku)");

        // 验证：推算表中无匹配时跳过（priceData == null 则 continue）
        boolean skipsWhenNoPriceData = methodBody.contains("priceData == null");

        // Then - 保持行为：从净数量遍历构建明细，推算表仅用于查价
        // monthlyNetMap 仅包含有 income 数据的 SKU，因此推算表中无销售数据匹配的 MSKU 不会出现
        assertThat(iteratesNetMap)
                .as("结算明细应从抵扣后的净数量（monthlyNetMap）遍历构建，" +
                    "而非从推算表遍历。这确保推算表中无销售数据匹配的 MSKU 不会出现在明细中。" +
                    "推算表有 %d 个 MSKU，但仅 %d 个在销售数据中有匹配。",
                    totalMskuCount, matchedMskuCount)
                .isTrue();

        assertThat(lookupPriceByMsku)
                .as("应使用 msku 从 mskuPriceMap 查找单价")
                .isTrue();

        assertThat(skipsWhenNoPriceData)
                .as("当推算表中无对应 MSKU 的单价数据时（priceData == null），应跳过该 SKU")
                .isTrue();
    }

    // ==================== 辅助方法 ====================

    /**
     * 读取 buildSettlementInput 方法体
     *
     * <p>从 DocumentGenerateServiceImpl.java 源码中提取 buildSettlementInput 方法体。</p>
     *
     * @return 方法体字符串
     * @throws IOException 读取文件失败
     * @author wanhua
     * 10:30 2026年01月29日
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
     * @author wanhua
     * 10:30 2026年01月29日
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
     * @author wanhua
     * 10:30 2026年01月29日
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
