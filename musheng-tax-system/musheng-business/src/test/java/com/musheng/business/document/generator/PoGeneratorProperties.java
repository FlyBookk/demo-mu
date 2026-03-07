package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentPoItem;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PoGenerator 属性测试
 *
 * <p>使用 jqwik 框架验证PO生成器的通用正确性属性。
 * 覆盖属性3（数据完整性）、属性5（合计行invariant）、属性6（幂等性）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class PoGeneratorProperties {

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
     * @return ShipmentInput Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<ShipmentInput> shipmentInputs() {
        Arbitrary<String> shipmentNo = Arbitraries.strings()
                .alpha().numeric().ofMinLength(5).ofMaxLength(15)
                .map(s -> "FBA-" + s);
        Arbitrary<LocalDateTime> createTime = Arbitraries.longs()
                .between(
                        LocalDateTime.of(2025, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC),
                        LocalDateTime.of(2026, 12, 31, 23, 59).toEpochSecond(java.time.ZoneOffset.UTC)
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
                    // 确保每个货件编号唯一
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


    // ==================== 属性 3：PO生成数据完整性 ====================

    // Feature: fba-document-generation, Property 3: PO生成数据完整性
    // PO明细按货件分组，MSKU列表和数量与输入一致
    /**
     * 属性3-1：PO明细中每个货件的MSKU列表和数量应与输入完全一致
     *
     * <p>对于任意一组货件数据，生成的PO明细中每个货件的MSKU编码和数量
     * 应与输入数据完全一致，不丢失、不多余、不篡改。</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.3, 2.7</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void poItemsShouldMatchInputShipmentData(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1);

        // Then - 收集所有PO明细，按货件编号分组
        List<DocumentPoItem> allItems = results.stream()
                .flatMap(r -> r.getItems().stream())
                .collect(Collectors.toList());

        // 构建输入数据的 shipmentNo → MSKU/数量 映射
        Map<String, List<ShipmentInput.MskuItem>> inputMap = new LinkedHashMap<>();
        for (ShipmentInput shipment : shipments) {
            inputMap.put(shipment.getShipmentNo(), shipment.getItems());
        }

        // 构建PO明细的 shipmentNo → MSKU/数量 映射
        Map<String, List<DocumentPoItem>> outputMap = allItems.stream()
                .collect(Collectors.groupingBy(DocumentPoItem::getShipmentNo));

        // 验证每个货件的MSKU列表和数量一致
        for (Map.Entry<String, List<ShipmentInput.MskuItem>> entry : inputMap.entrySet()) {
            String shipmentNo = entry.getKey();
            List<ShipmentInput.MskuItem> inputItems = entry.getValue();

            assertTrue(outputMap.containsKey(shipmentNo),
                    "PO明细中缺少货件 " + shipmentNo);

            List<DocumentPoItem> outputItems = outputMap.get(shipmentNo);
            assertEquals(inputItems.size(), outputItems.size(),
                    "货件 " + shipmentNo + " 的MSKU数量不一致");

            for (int i = 0; i < inputItems.size(); i++) {
                assertEquals(inputItems.get(i).getMsku(), outputItems.get(i).getMsku(),
                        "货件 " + shipmentNo + " 第 " + (i + 1) + " 个MSKU编码不一致");
                assertEquals(inputItems.get(i).getQuantity(), outputItems.get(i).getQuantity(),
                        "货件 " + shipmentNo + " MSKU " + inputItems.get(i).getMsku() + " 数量不一致");
            }
        }
    }

    // Feature: fba-document-generation, Property 3: PO生成数据完整性
    // 每个货件首行有FBA地址，后续行地址为空
    /**
     * 属性3-2：每个货件的第一个MSKU行应有FBA地址，后续行地址为null
     *
     * <p>对于任意货件数据，PO明细中每个货件的第一行应包含非null非空的FBA地址，
     * 同一货件的后续行地址应为null。</p>
     *
     * <p><b>Validates: Requirements 2.4</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void firstItemOfEachShipmentShouldHaveAddress(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1);

        // Then - 按货件编号分组检查地址
        List<DocumentPoItem> allItems = results.stream()
                .flatMap(r -> r.getItems().stream())
                .collect(Collectors.toList());

        Map<String, List<DocumentPoItem>> groupedByShipment = new LinkedHashMap<>();
        for (DocumentPoItem item : allItems) {
            groupedByShipment.computeIfAbsent(item.getShipmentNo(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<DocumentPoItem>> entry : groupedByShipment.entrySet()) {
            String shipmentNo = entry.getKey();
            List<DocumentPoItem> items = entry.getValue();

            // 首行应有FBA地址
            assertNotNull(items.get(0).getFbaAddress(),
                    "货件 " + shipmentNo + " 首行FBA地址不应为null");
            assertFalse(items.get(0).getFbaAddress().isEmpty(),
                    "货件 " + shipmentNo + " 首行FBA地址不应为空字符串");

            // 后续行地址应为null
            for (int i = 1; i < items.size(); i++) {
                assertNull(items.get(i).getFbaAddress(),
                        "货件 " + shipmentNo + " 第 " + (i + 1) + " 行FBA地址应为null");
            }
        }
    }

    // Feature: fba-document-generation, Property 3: PO生成数据完整性
    // PO表头包含买方、卖方、编号、日期
    /**
     * 属性3-3：PO表头应包含买方名称、卖方名称、非null编号和日期
     *
     * <p>对于任意货件数据，生成的每份PO表头应包含正确的买方名称
     * "东莞市慕声商贸有限公司"、卖方名称"Hong Kong Andeo Group Limited"、
     * 非null的单据编号和PO日期。</p>
     *
     * <p><b>Validates: Requirements 2.10</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void poHeaderShouldContainRequiredFields(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1);

        // Then
        for (PoGenerateResult result : results) {
            DocumentPo po = result.getPo();

            assertEquals("东莞市慕声商贸有限公司", po.getBuyerName(),
                    "买方名称不正确");
            assertEquals("Hong Kong Andeo Group Limited", po.getSellerName(),
                    "卖方名称不正确");
            assertNotNull(po.getDocumentNo(),
                    "单据编号不应为null");
            assertFalse(po.getDocumentNo().isEmpty(),
                    "单据编号不应为空字符串");
            assertNotNull(po.getPoDate(),
                    "PO日期不应为null");
        }
    }


    // ==================== 属性 6：单据生成幂等性（PO部分） ====================

    // Feature: fba-document-generation, Property 6: 单据生成幂等性（PO部分）
    // 相同输入多次调用，输出完全一致
    /**
     * 属性6：相同输入多次调用generate方法，输出应完全一致
     *
     * <p>对于任意相同的货件输入数据，多次调用PoGenerator.generate方法，
     * 输出的PO日期、编号、明细排序、数量应完全一致。</p>
     *
     * <p><b>Validates: Requirements 2.12</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void sameInputShouldProduceIdenticalOutput(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When - 调用两次
        List<PoGenerateResult> results1 = PoGenerator.generate(shipments, 1);
        List<PoGenerateResult> results2 = PoGenerator.generate(shipments, 1);

        // Then - 输出应完全一致
        assertEquals(results1.size(), results2.size(),
                "两次调用生成的PO数量不一致");

        for (int i = 0; i < results1.size(); i++) {
            DocumentPo po1 = results1.get(i).getPo();
            DocumentPo po2 = results2.get(i).getPo();

            assertEquals(po1.getDocumentNo(), po2.getDocumentNo(),
                    "第 " + (i + 1) + " 份PO编号不一致");
            assertEquals(po1.getPoDate(), po2.getPoDate(),
                    "第 " + (i + 1) + " 份PO日期不一致");
            assertEquals(po1.getTotalQuantity(), po2.getTotalQuantity(),
                    "第 " + (i + 1) + " 份PO总数量不一致");
            assertEquals(po1.getShipmentCount(), po2.getShipmentCount(),
                    "第 " + (i + 1) + " 份PO货件数不一致");
            assertEquals(po1.getBuyerName(), po2.getBuyerName(),
                    "第 " + (i + 1) + " 份PO买方名称不一致");
            assertEquals(po1.getSellerName(), po2.getSellerName(),
                    "第 " + (i + 1) + " 份PO卖方名称不一致");

            // 验证明细一致性
            List<DocumentPoItem> items1 = results1.get(i).getItems();
            List<DocumentPoItem> items2 = results2.get(i).getItems();
            assertEquals(items1.size(), items2.size(),
                    "第 " + (i + 1) + " 份PO明细数量不一致");

            for (int j = 0; j < items1.size(); j++) {
                assertEquals(items1.get(j).getShipmentNo(), items2.get(j).getShipmentNo(),
                        "第 " + (i + 1) + " 份PO第 " + (j + 1) + " 行货件编号不一致");
                assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku(),
                        "第 " + (i + 1) + " 份PO第 " + (j + 1) + " 行MSKU不一致");
                assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity(),
                        "第 " + (i + 1) + " 份PO第 " + (j + 1) + " 行数量不一致");
                assertEquals(items1.get(j).getFbaAddress(), items2.get(j).getFbaAddress(),
                        "第 " + (i + 1) + " 份PO第 " + (j + 1) + " 行地址不一致");
                assertEquals(items1.get(j).getSortOrder(), items2.get(j).getSortOrder(),
                        "第 " + (i + 1) + " 份PO第 " + (j + 1) + " 行排序不一致");
            }
        }
    }

    // ==================== 属性 5：合计行数量 invariant（PO部分） ====================

    // Feature: fba-document-generation, Property 5: 合计行数量 invariant（PO部分）
    // 合计行总数量 = 所有明细行数量之和
    /**
     * 属性5：PO的totalQuantity应等于所有明细行quantity之和
     *
     * <p>对于任意货件数据，生成的每份PO的合计行总数量应精确等于
     * 该PO所有明细行数量之和。</p>
     *
     * <p><b>Validates: Requirements 2.9</b></p>
     *
     * @param shipments 随机生成的货件列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void totalQuantityShouldEqualSumOfItemQuantities(
            @ForAll("shipmentLists") List<ShipmentInput> shipments) {
        // When
        List<PoGenerateResult> results = PoGenerator.generate(shipments, 1);

        // Then
        for (PoGenerateResult result : results) {
            int sumOfItems = result.getItems().stream()
                    .mapToInt(DocumentPoItem::getQuantity)
                    .sum();

            assertEquals(sumOfItems, result.getPo().getTotalQuantity(),
                    "PO " + result.getPo().getDocumentNo()
                            + " 合计行总数量 " + result.getPo().getTotalQuantity()
                            + " 不等于明细行数量之和 " + sumOfItems);
        }
    }
}
