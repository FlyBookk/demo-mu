package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentPoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PoGenerator 单元测试
 *
 * <p>覆盖PO生成器的核心逻辑：单货件PO、多货件PO、多站点混合货件、
 * 地址填充规则、合计行、编号格式、幂等性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("PoGenerator PO生成器测试")
class PoGeneratorTest {

    // ==================== 单货件PO生成 ====================

    @Nested
    @DisplayName("单货件PO生成")
    class SingleShipmentPoTest {

        @Test
        @DisplayName("单货件单MSKU应生成正确的PO")
        void testGenerate_SingleShipmentSingleMsku_ShouldGenerateCorrectPo() {
            // Given - 2025-09-01 周一创建的货件，PO日期应为下一个周二 2025-09-02
            ShipmentInput shipment = ShipmentInput.builder()
                    .shipmentNo("FBA-001")
                    .shipmentName("测试货件1")
                    .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                    .items(List.of(
                            ShipmentInput.MskuItem.builder().msku("MSUS-ABC001").quantity(100).build()
                    ))
                    .streetAddress("123 Main St")
                    .city("Phoenix")
                    .stateProvince("AZ")
                    .postalCode("85001")
                    .country("US")
                    .build();

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then
            assertEquals(1, results.size());
            PoGenerateResult result = results.get(0);
            DocumentPo po = result.getPo();

            assertNotNull(po);
            assertEquals("东莞市慕声商贸有限公司", po.getBuyerName());
            assertEquals("Hong Kong Andeo Group Limited", po.getSellerName());
            assertNotNull(po.getDocumentNo());
            assertNotNull(po.getPoDate());
            assertEquals(100, po.getTotalQuantity());
            assertEquals(1, po.getShipmentCount());

            // 明细验证
            List<DocumentPoItem> items = result.getItems();
            assertEquals(1, items.size());
            assertEquals("FBA-001", items.get(0).getShipmentNo());
            assertEquals("MSUS-ABC001", items.get(0).getMsku());
            assertEquals(100, items.get(0).getQuantity());
        }

        @Test
        @DisplayName("单货件多MSKU应生成正确的PO明细")
        void testGenerate_SingleShipmentMultipleMsku_ShouldGenerateCorrectItems() {
            // Given
            ShipmentInput shipment = ShipmentInput.builder()
                    .shipmentNo("FBA-002")
                    .shipmentName("测试货件2")
                    .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                    .items(List.of(
                            ShipmentInput.MskuItem.builder().msku("MSUS-ABC001").quantity(50).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUS-ABC002").quantity(30).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUS-ABC003").quantity(20).build()
                    ))
                    .streetAddress("456 Oak Ave")
                    .city("Los Angeles")
                    .stateProvince("CA")
                    .postalCode("90001")
                    .country("US")
                    .build();

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then
            assertEquals(1, results.size());
            PoGenerateResult result = results.get(0);
            assertEquals(100, result.getPo().getTotalQuantity());
            assertEquals(3, result.getItems().size());
        }
    }

    // ==================== PO日期推算 ====================

    @Nested
    @DisplayName("PO日期推算")
    class PoDateCalculationTest {

        @Test
        @DisplayName("周一创建的货件，PO日期应为同周周二")
        void testGenerate_MondayShipment_PoDateShouldBeNextTuesday() {
            // Given - 2025-09-01 是周一
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - PO日期应为 2025-09-02（周二）
            assertEquals(java.time.LocalDate.of(2025, 9, 2), results.get(0).getPo().getPoDate());
        }

        @Test
        @DisplayName("周二创建的货件，PO日期应为当天")
        void testGenerate_TuesdayShipment_PoDateShouldBeSameDay() {
            // Given - 2025-09-02 是周二
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 2, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - PO日期应为 2025-09-02（当天周二）
            assertEquals(java.time.LocalDate.of(2025, 9, 2), results.get(0).getPo().getPoDate());
        }

        @Test
        @DisplayName("周三创建的货件，PO日期应为下周二")
        void testGenerate_WednesdayShipment_PoDateShouldBeNextWeekTuesday() {
            // Given - 2025-09-03 是周三
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 3, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - PO日期应为 2025-09-09（下周二）
            assertEquals(java.time.LocalDate.of(2025, 9, 9), results.get(0).getPo().getPoDate());
        }

        @Test
        @DisplayName("周五创建的货件，PO日期应为下周二")
        void testGenerate_FridayShipment_PoDateShouldBeNextWeekTuesday() {
            // Given - 2025-09-05 是周五
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 5, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - PO日期应为 2025-09-09（下周二）
            assertEquals(java.time.LocalDate.of(2025, 9, 9), results.get(0).getPo().getPoDate());
        }

        @Test
        @DisplayName("PO日期遇节假日应顺延到下一个工作日")
        void testGenerate_PoDateOnHoliday_ShouldPostponeToNextWorkingDay() {
            // Given - 2025-09-29 是周一，下一个周二是 2025-09-30
            // 2025-09-30 是工作日，所以PO日期就是 2025-09-30
            // 但如果我们选一个会落在国庆的日期：
            // 2025-09-30 周二 → PO日期 = 2025-09-30（工作日，正常）
            // 换一个：2025-12-22 周一 → 下一个周二 2025-12-23（工作日）
            // 要测试节假日顺延，需要PO日期恰好落在节假日
            // 2025-12-25 是周四（圣诞节），不是周二，不会直接命中
            // 构造一个场景：如果推算出的周二恰好是节假日
            // 2026-02-16 是周一（春节），2026-02-17 周二也是春节假期
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2026, 2, 16, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - 2026-02-17 周二是春节假期，应顺延到 2026-02-23（周一）
            // 2026-02-18~22 都是春节假期或周末
            // 2026春节：2026-02-16~22，2026-02-23 周一是工作日
            assertEquals(java.time.LocalDate.of(2026, 2, 23), results.get(0).getPo().getPoDate());
        }
    }

    // ==================== 多货件PO生成 ====================

    @Nested
    @DisplayName("多货件PO生成")
    class MultipleShipmentPoTest {

        @Test
        @DisplayName("同一PO日期的多个货件应合并到同一份PO")
        void testGenerate_SamePoDate_ShouldMergeIntoOnePo() {
            // Given - 两个货件都在同一周创建（周一），PO日期都是同周周二
            ShipmentInput shipment1 = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipmentWithItems("FBA-002",
                    LocalDateTime.of(2025, 9, 1, 14, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment1, shipment2), 1);

            // Then - 应合并为1份PO
            assertEquals(1, results.size());
            assertEquals(2, results.get(0).getPo().getShipmentCount());
            assertEquals(80, results.get(0).getPo().getTotalQuantity());
            assertEquals(2, results.get(0).getItems().size());
        }

        @Test
        @DisplayName("不同PO日期的货件应生成多份PO")
        void testGenerate_DifferentPoDate_ShouldGenerateMultiplePos() {
            // Given - 两个货件在不同周创建
            ShipmentInput shipment1 = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),  // 周一 → PO日期 9/2
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipmentWithItems("FBA-002",
                    LocalDateTime.of(2025, 9, 10, 10, 0), // 周三 → PO日期 9/16
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment1, shipment2), 1);

            // Then - 应生成2份PO
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("多份PO的编号序号应递增")
        void testGenerate_MultiplePos_DocumentNoShouldIncrement() {
            // Given
            ShipmentInput shipment1 = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipmentWithItems("FBA-002",
                    LocalDateTime.of(2025, 9, 10, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment1, shipment2), 1);

            // Then - 编号序号应递增
            String no1 = results.get(0).getPo().getDocumentNo();
            String no2 = results.get(1).getPo().getDocumentNo();
            assertNotEquals(no1, no2);
            // 第一份序号001，第二份序号002
            assertTrue(no1.endsWith("001"));
            assertTrue(no2.endsWith("002"));
        }
    }

    // ==================== 多站点混合货件 ====================

    @Nested
    @DisplayName("多站点混合货件")
    class MultiSiteShipmentTest {

        @Test
        @DisplayName("一份PO应支持包含多个国家站点的货件")
        void testGenerate_MultiSiteShipments_ShouldSupportMixedSites() {
            // Given - US和UK站点的货件在同一周创建
            ShipmentInput usShipment = createShipmentWithItems("FBA-US-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput ukShipment = createShipmentWithItems("FBA-UK-001",
                    LocalDateTime.of(2025, 9, 1, 12, 0),
                    List.of(new ShipmentInput.MskuItem("MSUK-B001", 30)));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(
                    List.of(usShipment, ukShipment), 1);

            // Then - 应合并为1份PO，包含两个站点的货件
            assertEquals(1, results.size());
            assertEquals(2, results.get(0).getPo().getShipmentCount());
            assertEquals(80, results.get(0).getPo().getTotalQuantity());
        }
    }

    // ==================== 地址填充规则 ====================

    @Nested
    @DisplayName("地址填充规则")
    class AddressFillingTest {

        @Test
        @DisplayName("每个货件首行MSKU应填写完整FBA地址，后续行留空")
        void testGenerate_AddressFilling_FirstRowHasAddressRestEmpty() {
            // Given - 一个货件包含3个MSKU
            ShipmentInput shipment = ShipmentInput.builder()
                    .shipmentNo("FBA-001")
                    .shipmentName("测试货件")
                    .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                    .items(List.of(
                            ShipmentInput.MskuItem.builder().msku("MSUS-A001").quantity(50).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUS-A002").quantity(30).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUS-A003").quantity(20).build()
                    ))
                    .streetAddress("123 Main St")
                    .city("Phoenix")
                    .stateProvince("AZ")
                    .postalCode("85001")
                    .country("US")
                    .build();

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then
            List<DocumentPoItem> items = results.get(0).getItems();
            assertEquals(3, items.size());

            // 首行有完整地址
            String expectedAddress = "123 Main St, Phoenix, AZ, 85001, US";
            assertEquals(expectedAddress, items.get(0).getFbaAddress());

            // 后续行地址为空
            assertNull(items.get(1).getFbaAddress());
            assertNull(items.get(2).getFbaAddress());
        }

        @Test
        @DisplayName("多个货件各自首行有地址，后续行留空")
        void testGenerate_MultipleShipments_EachFirstRowHasAddress() {
            // Given
            ShipmentInput shipment1 = ShipmentInput.builder()
                    .shipmentNo("FBA-001")
                    .shipmentName("货件1")
                    .createTime(LocalDateTime.of(2025, 9, 1, 10, 0))
                    .items(List.of(
                            ShipmentInput.MskuItem.builder().msku("MSUS-A001").quantity(50).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUS-A002").quantity(30).build()
                    ))
                    .streetAddress("123 Main St")
                    .city("Phoenix")
                    .stateProvince("AZ")
                    .postalCode("85001")
                    .country("US")
                    .build();

            ShipmentInput shipment2 = ShipmentInput.builder()
                    .shipmentNo("FBA-002")
                    .shipmentName("货件2")
                    .createTime(LocalDateTime.of(2025, 9, 1, 14, 0))
                    .items(List.of(
                            ShipmentInput.MskuItem.builder().msku("MSUK-B001").quantity(40).build(),
                            ShipmentInput.MskuItem.builder().msku("MSUK-B002").quantity(20).build()
                    ))
                    .streetAddress("456 Oak Ave")
                    .city("London")
                    .stateProvince("England")
                    .postalCode("EC1A 1BB")
                    .country("UK")
                    .build();

            // When
            List<PoGenerateResult> results = PoGenerator.generate(
                    List.of(shipment1, shipment2), 1);

            // Then
            List<DocumentPoItem> items = results.get(0).getItems();
            assertEquals(4, items.size());

            // 货件1首行有地址
            assertEquals("123 Main St, Phoenix, AZ, 85001, US", items.get(0).getFbaAddress());
            assertNull(items.get(1).getFbaAddress());

            // 货件2首行有地址
            assertEquals("456 Oak Ave, London, England, EC1A 1BB, UK", items.get(2).getFbaAddress());
            assertNull(items.get(3).getFbaAddress());
        }
    }

    // ==================== 合计行与编号 ====================

    @Nested
    @DisplayName("合计行与编号格式")
    class TotalAndNumberTest {

        @Test
        @DisplayName("合计行总数量应等于所有明细行数量之和")
        void testGenerate_TotalQuantity_ShouldEqualSumOfItems() {
            // Given
            ShipmentInput shipment = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),
                    List.of(
                            new ShipmentInput.MskuItem("MSUS-A001", 50),
                            new ShipmentInput.MskuItem("MSUS-A002", 30),
                            new ShipmentInput.MskuItem("MSUS-A003", 20)
                    ));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then
            int sumOfItems = results.get(0).getItems().stream()
                    .mapToInt(DocumentPoItem::getQuantity).sum();
            assertEquals(sumOfItems, results.get(0).getPo().getTotalQuantity());
            assertEquals(100, results.get(0).getPo().getTotalQuantity());
        }

        @Test
        @DisplayName("编号格式应为 {YYYYMMDD}{3位序号}")
        void testGenerate_DocumentNo_ShouldMatchFormat() {
            // Given
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 1);

            // Then - PO日期为 2025-09-02，序号001
            String documentNo = results.get(0).getPo().getDocumentNo();
            assertTrue(documentNo.matches("\\d{8}\\d{3}"));
            assertEquals("20250902001", documentNo);
        }

        @Test
        @DisplayName("起始序号参数应影响编号生成")
        void testGenerate_StartSequence_ShouldAffectDocumentNo() {
            // Given
            ShipmentInput shipment = createSimpleShipment("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0));

            // When - 起始序号为5
            List<PoGenerateResult> results = PoGenerator.generate(List.of(shipment), 5);

            // Then
            assertEquals("20250902005", results.get(0).getPo().getDocumentNo());
        }
    }

    // ==================== 幂等性 ====================

    @Nested
    @DisplayName("幂等性")
    class IdempotencyTest {

        @Test
        @DisplayName("相同输入多次调用应输出完全一致")
        void testGenerate_SameInput_ShouldProduceSameOutput() {
            // Given
            ShipmentInput shipment1 = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipmentWithItems("FBA-002",
                    LocalDateTime.of(2025, 9, 10, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            List<ShipmentInput> inputs = List.of(shipment1, shipment2);

            // When - 调用两次
            List<PoGenerateResult> results1 = PoGenerator.generate(inputs, 1);
            List<PoGenerateResult> results2 = PoGenerator.generate(inputs, 1);

            // Then - 输出应完全一致
            assertEquals(results1.size(), results2.size());
            for (int i = 0; i < results1.size(); i++) {
                DocumentPo po1 = results1.get(i).getPo();
                DocumentPo po2 = results2.get(i).getPo();
                assertEquals(po1.getDocumentNo(), po2.getDocumentNo());
                assertEquals(po1.getPoDate(), po2.getPoDate());
                assertEquals(po1.getTotalQuantity(), po2.getTotalQuantity());
                assertEquals(po1.getShipmentCount(), po2.getShipmentCount());
                assertEquals(po1.getBuyerName(), po2.getBuyerName());
                assertEquals(po1.getSellerName(), po2.getSellerName());

                List<DocumentPoItem> items1 = results1.get(i).getItems();
                List<DocumentPoItem> items2 = results2.get(i).getItems();
                assertEquals(items1.size(), items2.size());
                for (int j = 0; j < items1.size(); j++) {
                    assertEquals(items1.get(j).getShipmentNo(), items2.get(j).getShipmentNo());
                    assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku());
                    assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity());
                    assertEquals(items1.get(j).getFbaAddress(), items2.get(j).getFbaAddress());
                    assertEquals(items1.get(j).getSortOrder(), items2.get(j).getSortOrder());
                }
            }
        }
    }

    // ==================== 排序 ====================

    @Nested
    @DisplayName("货件排序")
    class SortingTest {

        @Test
        @DisplayName("货件应按创建时间升序排序")
        void testGenerate_Shipments_ShouldBeSortedByCreateTime() {
            // Given - 故意乱序输入
            ShipmentInput shipment1 = createShipmentWithItems("FBA-003",
                    LocalDateTime.of(2025, 9, 1, 16, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-C001", 20)));
            ShipmentInput shipment2 = createShipmentWithItems("FBA-001",
                    LocalDateTime.of(2025, 9, 1, 8, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment3 = createShipmentWithItems("FBA-002",
                    LocalDateTime.of(2025, 9, 1, 12, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-B001", 30)));

            // When
            List<PoGenerateResult> results = PoGenerator.generate(
                    List.of(shipment1, shipment2, shipment3), 1);

            // Then - 明细应按创建时间升序排列：FBA-001, FBA-002, FBA-003
            List<DocumentPoItem> items = results.get(0).getItems();
            assertEquals("FBA-001", items.get(0).getShipmentNo());
            assertEquals("FBA-002", items.get(1).getShipmentNo());
            assertEquals("FBA-003", items.get(2).getShipmentNo());
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTest {

        @Test
        @DisplayName("空货件列表应返回空结果")
        void testGenerate_EmptyList_ShouldReturnEmptyResult() {
            // When
            List<PoGenerateResult> results = PoGenerator.generate(List.of(), 1);

            // Then
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("null 参数应抛出异常")
        void testGenerate_NullInput_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PoGenerator.generate(null, 1));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建简单的单MSKU货件
     */
    private ShipmentInput createSimpleShipment(String shipmentNo, LocalDateTime createTime) {
        return ShipmentInput.builder()
                .shipmentNo(shipmentNo)
                .shipmentName("测试货件")
                .createTime(createTime)
                .items(List.of(
                        ShipmentInput.MskuItem.builder().msku("MSUS-TEST001").quantity(100).build()
                ))
                .streetAddress("123 Test St")
                .city("TestCity")
                .stateProvince("TS")
                .postalCode("12345")
                .country("US")
                .build();
    }

    /**
     * 创建指定MSKU列表的货件
     */
    private ShipmentInput createShipmentWithItems(String shipmentNo, LocalDateTime createTime,
                                                   List<ShipmentInput.MskuItem> items) {
        return ShipmentInput.builder()
                .shipmentNo(shipmentNo)
                .shipmentName("测试货件")
                .createTime(createTime)
                .items(items)
                .streetAddress("123 Test St")
                .city("TestCity")
                .stateProvince("TS")
                .postalCode("12345")
                .country("US")
                .build();
    }
}
