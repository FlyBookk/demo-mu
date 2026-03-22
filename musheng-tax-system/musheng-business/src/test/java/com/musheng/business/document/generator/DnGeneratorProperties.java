package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentDnItem;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.entity.DocumentPoItem;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DnGenerator 属性测试
 *
 * <p>使用 jqwik 框架验证DN生成器的通用正确性属性。
 * 覆盖属性7（PO与DN数据一致性）、属性6（幂等性）、属性5（合计行invariant）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class DnGeneratorProperties {

    /** 固定锚点日期 */
    private static final LocalDate ANCHOR = LocalDate.of(2025, 5, 22);

    // ==================== 测试辅助方法 ====================

    /**
     * 构建测试用交易方配置
     *
     * @return 测试用 DocumentPartyConfig
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private DocumentPartyConfig buildTestPartyConfig() {
        DocumentPartyConfig party = new DocumentPartyConfig();
        party.setBuyerName("东莞市慕声商贸有限公司");
        party.setBuyerAddress("广东省东莞市");
        party.setSellerName("Hong Kong Andeo Group Limited");
        return party;
    }

    // ==================== 自定义 Arbitrary 生成器 ====================

    /**
     * 生成随机MSKU编码
     *
     * @return MSKU编码 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<String> mskuCodes() {
        Arbitrary<String> prefix = Arbitraries.of("MSUS-", "MSCA-", "MSUK-", "MSEU-");
        Arbitrary<String> suffix = Arbitraries.strings()
                .alpha().numeric().ofMinLength(3).ofMaxLength(8);
        return Combinators.combine(prefix, suffix).as((p, s) -> p + s);
    }

    /**
     * 生成随机MSKU明细项
     *
     * @return MskuItem Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<ShipmentInput.MskuItem> mskuItems() {
        return Combinators.combine(
                mskuCodes(),
                Arbitraries.integers().between(1, 9999)
        ).as((msku, qty) -> ShipmentInput.MskuItem.builder()
                .msku(msku)
                .quantity(qty)
                .build());
    }

    /**
     * 生成随机货件输入数据
     *
     * <p>创建时间限制在锚点日期前后合理范围内（2025-04-01 ~ 2025-12-31），
     * 确保DN周期计算能正确覆盖所有货件。</p>
     *
     * @return ShipmentInput Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<ShipmentInput> shipmentInputs() {
        Arbitrary<String> shipmentNo = Arbitraries.strings()
                .alpha().numeric().ofMinLength(5).ofMaxLength(15)
                .map(s -> "FBA-" + s);
        // 限制创建时间在锚点日期之前合理范围内（锚点前42天到锚点后180天），
        // 确保DN周期计算的rangeEnd能覆盖锚点日期
        Arbitrary<LocalDateTime> createTime = Arbitraries.longs()
                .between(
                        LocalDateTime.of(2025, 5, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC),
                        LocalDateTime.of(2026, 6, 30, 23, 59).toEpochSecond(java.time.ZoneOffset.UTC)
                ).map(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, java.time.ZoneOffset.UTC));
        Arbitrary<List<ShipmentInput.MskuItem>> items = mskuItems().list().ofMinSize(1).ofMaxSize(5);
        Arbitrary<String> street = Arbitraries.of("123 Main St", "456 Oak Ave", "789 Pine Rd", "101 Elm Blvd");
        Arbitrary<String> city = Arbitraries.of("Phoenix", "London", "Toronto", "Berlin");
        Arbitrary<String> state = Arbitraries.of("AZ", "England", "ON", "BE");
        Arbitrary<String> postal = Arbitraries.of("85001", "EC1A 1BB", "M5V 2T6", "10115");
        Arbitrary<String> country = Arbitraries.of("US", "UK", "CA", "DE");

        return Combinators.combine(shipmentNo, createTime, items, street, city, state, postal, country)
                .as((no, time, itemList, st, ci, sta, pos, cou) -> ShipmentInput.builder()
                        .shipmentNo(no)
                        .shipmentName("测试货件")
                        .createTime(time)
                        .items(itemList)
                        .streetAddress(st)
                        .city(ci)
                        .stateProvince(sta)
                        .postalCode(pos)
                        .country(cou)
                        .build());
    }

    /**
     * 生成随机货件列表（1~5个货件，货件编号唯一）
     *
     * <p>使用索引后缀确保每个货件编号唯一，避免分组逻辑冲突。</p>
     *
     * @return 货件列表 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<List<ShipmentInput>> shipmentLists() {
        return Arbitraries.integers().between(1, 5).flatMap(size ->
                Combinators.combine(
                        shipmentInputs().list().ofSize(size),
                        Arbitraries.just(size)
                ).as((list, s) -> {
                    List<ShipmentInput> result = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        ShipmentInput original = list.get(i);
                        result.add(ShipmentInput.builder()
                                .shipmentNo(original.getShipmentNo() + "-" + (i + 1))
                                .shipmentName(original.getShipmentName())
                                .createTime(original.getCreateTime())
                                .items(original.getItems())
                                .streetAddress(original.getStreetAddress())
                                .city(original.getCity())
                                .stateProvince(original.getStateProvince())
                                .postalCode(original.getPostalCode())
                                .country(original.getCountry())
                                .build());
                    }
                    return result;
                })
        );
    }


    // ==================== 属性 7：PO与DN数据一致性 ====================

    // Feature: fba-document-generation, Property 7: PO与DN数据一致性
    // 同一组货件生成PO和DN后，货件ID集合相同
    /**
     * 属性7-1：PO和DN包含的货件编号集合应完全相同
     *
     * <p>对于任意一组货件数据，分别生成PO和DN后，
     * 两者包含的货件编号集合应完全相同。</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void poAndDnShouldContainSameShipmentIds(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When - 分别生成PO和DN
        List<PoGenerateResult> poResults = PoGenerator.generate(shipments, 1, buildTestPartyConfig());
        List<DnGenerateResult> dnResults = DnGenerator.generate(ANCHOR, shipments, 1);

        // Then - 收集PO中所有货件编号
        java.util.Set<String> poShipmentNos = poResults.stream()
                .flatMap(r -> r.getItems().stream())
                .map(DocumentPoItem::getShipmentNo)
                .collect(Collectors.toSet());

        // 收集DN中所有货件编号
        java.util.Set<String> dnShipmentNos = dnResults.stream()
                .flatMap(r -> r.getItems().stream())
                .map(DocumentDnItem::getShipmentNo)
                .collect(Collectors.toSet());

        // 验证货件编号集合相同
        assertEquals(poShipmentNos, dnShipmentNos,
                "PO和DN包含的货件编号集合不一致");
    }

    // Feature: fba-document-generation, Property 7: PO与DN数据一致性
    // 每个货件的MSKU列表和数量在PO和DN中一致
    /**
     * 属性7-2：每个货件的MSKU列表和数量在PO和DN中应完全一致
     *
     * <p>对于任意一组货件数据，分别生成PO和DN后，
     * 对于每个货件，PO和DN中的MSKU列表和数量应完全一致。</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void poAndDnShouldHaveConsistentMskuDataPerShipment(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When - 分别生成PO和DN
        List<PoGenerateResult> poResults = PoGenerator.generate(shipments, 1, buildTestPartyConfig());
        List<DnGenerateResult> dnResults = DnGenerator.generate(ANCHOR, shipments, 1);

        // Then - 按货件编号分组PO明细：shipmentNo → (msku → quantity)
        Map<String, Map<String, Integer>> poDataByShipment = new HashMap<>();
        for (PoGenerateResult poResult : poResults) {
            for (DocumentPoItem item : poResult.getItems()) {
                poDataByShipment
                        .computeIfAbsent(item.getShipmentNo(), k -> new HashMap<>())
                        .merge(item.getMsku(), item.getQuantity(), Integer::sum);
            }
        }

        // 按货件编号分组DN明细：shipmentNo → (msku → quantity)
        Map<String, Map<String, Integer>> dnDataByShipment = new HashMap<>();
        for (DnGenerateResult dnResult : dnResults) {
            for (DocumentDnItem item : dnResult.getItems()) {
                dnDataByShipment
                        .computeIfAbsent(item.getShipmentNo(), k -> new HashMap<>())
                        .merge(item.getMsku(), item.getQuantity(), Integer::sum);
            }
        }

        // 验证每个货件的MSKU和数量一致
        assertEquals(poDataByShipment.keySet(), dnDataByShipment.keySet(),
                "PO和DN的货件编号集合不一致");

        for (String shipmentNo : poDataByShipment.keySet()) {
            Map<String, Integer> poMskuMap = poDataByShipment.get(shipmentNo);
            Map<String, Integer> dnMskuMap = dnDataByShipment.get(shipmentNo);

            assertEquals(poMskuMap, dnMskuMap,
                    "货件 " + shipmentNo + " 的MSKU/数量在PO和DN中不一致");
        }
    }


    // ==================== 属性 6：单据生成幂等性（DN部分） ====================

    // Feature: fba-document-generation, Property 6: 单据生成幂等性（DN部分）
    // 相同输入多次调用，输出完全一致
    /**
     * 属性6：相同输入多次调用generate方法，输出应完全一致
     *
     * <p>对于任意相同的锚点日期和货件输入数据，多次调用DnGenerator.generate方法，
     * 输出的DN日期、编号、明细排序、数量应完全一致。</p>
     *
     * <p><b>Validates: Requirements 3.11</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void sameInputShouldProduceIdenticalDnOutput(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When - 调用两次
        List<DnGenerateResult> results1 = DnGenerator.generate(ANCHOR, shipments, 1);
        List<DnGenerateResult> results2 = DnGenerator.generate(ANCHOR, shipments, 1);

        // Then - 输出应完全一致
        assertEquals(results1.size(), results2.size(),
                "两次调用生成的DN数量不一致");

        for (int i = 0; i < results1.size(); i++) {
            DocumentDn dn1 = results1.get(i).getDn();
            DocumentDn dn2 = results2.get(i).getDn();

            assertEquals(dn1.getDocumentNo(), dn2.getDocumentNo(),
                    "第 " + (i + 1) + " 份DN编号不一致");
            assertEquals(dn1.getDnDate(), dn2.getDnDate(),
                    "第 " + (i + 1) + " 份DN日期不一致");
            assertEquals(dn1.getTotalQuantity(), dn2.getTotalQuantity(),
                    "第 " + (i + 1) + " 份DN总数量不一致");
            assertEquals(dn1.getSupplierName(), dn2.getSupplierName(),
                    "第 " + (i + 1) + " 份DN供应商名称不一致");
            assertEquals(dn1.getCustomerName(), dn2.getCustomerName(),
                    "第 " + (i + 1) + " 份DN客户名称不一致");

            // 验证明细一致性
            List<DocumentDnItem> items1 = results1.get(i).getItems();
            List<DocumentDnItem> items2 = results2.get(i).getItems();
            assertEquals(items1.size(), items2.size(),
                    "第 " + (i + 1) + " 份DN明细数量不一致");

            for (int j = 0; j < items1.size(); j++) {
                assertEquals(items1.get(j).getLineNo(), items2.get(j).getLineNo(),
                        "第 " + (i + 1) + " 份DN第 " + (j + 1) + " 行行号不一致");
                assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku(),
                        "第 " + (i + 1) + " 份DN第 " + (j + 1) + " 行MSKU不一致");
                assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity(),
                        "第 " + (i + 1) + " 份DN第 " + (j + 1) + " 行数量不一致");
                assertEquals(items1.get(j).getShipmentNo(), items2.get(j).getShipmentNo(),
                        "第 " + (i + 1) + " 份DN第 " + (j + 1) + " 行货件编号不一致");
            }
        }
    }

    // ==================== 属性 5：合计行数量 invariant（DN部分） ====================

    // Feature: fba-document-generation, Property 5: 合计行数量 invariant（DN部分）
    // 合计行总数量 = 所有明细行数量之和
    /**
     * 属性5：DN的totalQuantity应等于所有明细行quantity之和
     *
     * <p>对于任意货件数据，生成的每份DN的合计行总数量应精确等于
     * 该DN所有明细行数量之和。</p>
     *
     * <p><b>Validates: Requirements 3.7</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void dnTotalQuantityShouldEqualSumOfItemQuantities(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When
        List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, shipments, 1);

        // Then
        for (DnGenerateResult result : results) {
            int sumOfItems = result.getItems().stream()
                    .mapToInt(DocumentDnItem::getQuantity)
                    .sum();

            assertEquals(sumOfItems, result.getDn().getTotalQuantity(),
                    "DN " + result.getDn().getDocumentNo()
                            + " 合计行总数量 " + result.getDn().getTotalQuantity()
                            + " 不等于明细行数量之和 " + sumOfItems);
        }
    }
}
