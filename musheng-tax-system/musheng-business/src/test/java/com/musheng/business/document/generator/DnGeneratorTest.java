package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentDnItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DnGenerator 送货单生成器单元测试
 *
 * <p>覆盖DN生成器的核心逻辑：单周期DN、多周期DN、MSKU行号连续、
 * 备注列货件编号、合计行、编号格式、幂等性、繁体中文客户名称。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("DnGenerator 送货单生成器测试")
class DnGeneratorTest {

    /** 默认锚点日期 */
    private static final LocalDate ANCHOR = LocalDate.of(2025, 5, 22);

    // ==================== 单周期DN生成 ====================

    @Nested
    @DisplayName("单周期DN生成")
    class SinglePeriodDnTest {

        @Test
        @DisplayName("所有货件在同一个21天周期内应生成1份DN")
        void testGenerate_SinglePeriod_ShouldGenerateOneDn() {
            // Given - 锚点 2025-05-22，货件在锚点之前创建，都落在第一个DN周期
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 21, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment1, shipment2), 1);

            // Then
            assertEquals(1, results.size());
            DocumentDn dn = results.get(0).getDn();
            assertNotNull(dn);
            assertEquals(80, dn.getTotalQuantity());
        }

        @Test
        @DisplayName("单货件单MSKU应生成正确的DN明细")
        void testGenerate_SingleShipmentSingleMsku_ShouldGenerateCorrectItems() {
            // Given
            ShipmentInput shipment = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 100)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment), 1);

            // Then
            assertEquals(1, results.size());
            List<DocumentDnItem> items = results.get(0).getItems();
            assertEquals(1, items.size());
            assertEquals("MSUS-A001", items.get(0).getMsku());
            assertEquals(100, items.get(0).getQuantity());
            assertEquals("FBA-001", items.get(0).getShipmentNo());
        }
    }

    // ==================== 多周期DN生成 ====================

    @Nested
    @DisplayName("多周期DN生成")
    class MultiplePeriodDnTest {

        @Test
        @DisplayName("货件跨越多个21天周期应生成多份DN")
        void testGenerate_MultiplePeriods_ShouldGenerateMultipleDns() {
            // Given - 锚点 2025-05-22
            // 第一个DN日期: 2025-05-22（周四，工作日）
            // 第二个DN日期: 2025-06-12（+21天，周四，工作日）
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),  // 在第一个DN周期内
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 30, 10, 0),  // 在第二个DN周期内
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment1, shipment2), 1);

            // Then - 应生成2份DN
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("多份DN的编号序号应递增")
        void testGenerate_MultipleDns_DocumentNoShouldIncrement() {
            // Given
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 30, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment1, shipment2), 1);

            // Then
            String no1 = results.get(0).getDn().getDocumentNo();
            String no2 = results.get(1).getDn().getDocumentNo();
            assertNotEquals(no1, no2);
            assertTrue(no1.endsWith("001"));
            assertTrue(no2.endsWith("002"));
        }
    }

    // ==================== MSKU行号连续 ====================

    @Nested
    @DisplayName("MSKU行号连续")
    class LineNumberTest {

        @Test
        @DisplayName("每个MSKU独立一行，行号从1开始连续编号")
        void testGenerate_LineNumbers_ShouldBeSequential() {
            // Given - 两个货件各有2个MSKU
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(
                            new ShipmentInput.MskuItem("MSUS-A001", 50),
                            new ShipmentInput.MskuItem("MSUS-A002", 30)
                    ));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 21, 10, 0),
                    List.of(
                            new ShipmentInput.MskuItem("MSUS-B001", 40),
                            new ShipmentInput.MskuItem("MSUS-B002", 20)
                    ));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment1, shipment2), 1);

            // Then - 行号应为 1, 2, 3, 4
            List<DocumentDnItem> items = results.get(0).getItems();
            assertEquals(4, items.size());
            assertEquals(1, items.get(0).getLineNo());
            assertEquals(2, items.get(1).getLineNo());
            assertEquals(3, items.get(2).getLineNo());
            assertEquals(4, items.get(3).getLineNo());
        }
    }

    // ==================== 备注列货件编号 ====================

    @Nested
    @DisplayName("备注列货件编号")
    class ShipmentNoRemarkTest {

        @Test
        @DisplayName("每个MSKU行的备注列应标注对应的FBA货件编号")
        void testGenerate_ShipmentNoRemark_ShouldMatchShipment() {
            // Given
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(
                            new ShipmentInput.MskuItem("MSUS-A001", 50),
                            new ShipmentInput.MskuItem("MSUS-A002", 30)
                    ));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 21, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-B001", 40)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment1, shipment2), 1);

            // Then
            List<DocumentDnItem> items = results.get(0).getItems();
            assertEquals("FBA-001", items.get(0).getShipmentNo());
            assertEquals("FBA-001", items.get(1).getShipmentNo());
            assertEquals("FBA-002", items.get(2).getShipmentNo());
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
            ShipmentInput shipment = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(
                            new ShipmentInput.MskuItem("MSUS-A001", 50),
                            new ShipmentInput.MskuItem("MSUS-A002", 30),
                            new ShipmentInput.MskuItem("MSUS-A003", 20)
                    ));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment), 1);

            // Then
            int sumOfItems = results.get(0).getItems().stream()
                    .mapToInt(DocumentDnItem::getQuantity).sum();
            assertEquals(sumOfItems, results.get(0).getDn().getTotalQuantity());
            assertEquals(100, results.get(0).getDn().getTotalQuantity());
        }

        @Test
        @DisplayName("编号格式应为 {YYYYMMDD}{3位序号}")
        void testGenerate_DocumentNo_ShouldMatchFormat() {
            // Given
            ShipmentInput shipment = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 100)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment), 1);

            // Then
            String documentNo = results.get(0).getDn().getDocumentNo();
            assertTrue(documentNo.matches("\\d{8}\\d{3}"));
        }
    }

    // ==================== 表头信息 ====================

    @Nested
    @DisplayName("表头信息")
    class HeaderInfoTest {

        @Test
        @DisplayName("供应商名称应为 Hong Kong Andeo Group Limited")
        void testGenerate_SupplierName_ShouldBeCorrect() {
            // Given
            ShipmentInput shipment = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 100)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment), 1);

            // Then
            assertEquals("Hong Kong Andeo Group Limited", results.get(0).getDn().getSupplierName());
        }

        @Test
        @DisplayName("客户名称应为繁体中文 東莞市慕聲商貿有限公司")
        void testGenerate_CustomerName_ShouldBeTraditionalChinese() {
            // Given
            ShipmentInput shipment = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 100)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(shipment), 1);

            // Then
            assertEquals("東莞市慕聲商貿有限公司", results.get(0).getDn().getCustomerName());
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
            ShipmentInput shipment1 = createShipment("FBA-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput shipment2 = createShipment("FBA-002",
                    LocalDateTime.of(2025, 5, 30, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A002", 30)));

            List<ShipmentInput> inputs = List.of(shipment1, shipment2);

            // When - 调用两次
            List<DnGenerateResult> results1 = DnGenerator.generate(ANCHOR, inputs, 1);
            List<DnGenerateResult> results2 = DnGenerator.generate(ANCHOR, inputs, 1);

            // Then - 输出应完全一致
            assertEquals(results1.size(), results2.size());
            for (int i = 0; i < results1.size(); i++) {
                DocumentDn dn1 = results1.get(i).getDn();
                DocumentDn dn2 = results2.get(i).getDn();
                assertEquals(dn1.getDocumentNo(), dn2.getDocumentNo());
                assertEquals(dn1.getDnDate(), dn2.getDnDate());
                assertEquals(dn1.getTotalQuantity(), dn2.getTotalQuantity());
                assertEquals(dn1.getSupplierName(), dn2.getSupplierName());
                assertEquals(dn1.getCustomerName(), dn2.getCustomerName());

                List<DocumentDnItem> items1 = results1.get(i).getItems();
                List<DocumentDnItem> items2 = results2.get(i).getItems();
                assertEquals(items1.size(), items2.size());
                for (int j = 0; j < items1.size(); j++) {
                    assertEquals(items1.get(j).getLineNo(), items2.get(j).getLineNo());
                    assertEquals(items1.get(j).getMsku(), items2.get(j).getMsku());
                    assertEquals(items1.get(j).getQuantity(), items2.get(j).getQuantity());
                    assertEquals(items1.get(j).getShipmentNo(), items2.get(j).getShipmentNo());
                }
            }
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
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR, List.of(), 1);

            // Then
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("null 货件列表应抛出异常")
        void testGenerate_NullShipments_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnGenerator.generate(ANCHOR, null, 1));
        }

        @Test
        @DisplayName("null 锚点日期应抛出异常")
        void testGenerate_NullAnchor_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class,
                    () -> DnGenerator.generate(null, List.of(), 1));
        }
    }

    // ==================== 多站点混合 ====================

    @Nested
    @DisplayName("多站点混合货件")
    class MultiSiteTest {

        @Test
        @DisplayName("一份DN应支持包含多个国家站点的货件")
        void testGenerate_MultiSiteShipments_ShouldSupportMixedSites() {
            // Given
            ShipmentInput usShipment = createShipment("FBA-US-001",
                    LocalDateTime.of(2025, 5, 20, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUS-A001", 50)));
            ShipmentInput ukShipment = createShipment("FBA-UK-001",
                    LocalDateTime.of(2025, 5, 21, 10, 0),
                    List.of(new ShipmentInput.MskuItem("MSUK-B001", 30)));

            // When
            List<DnGenerateResult> results = DnGenerator.generate(ANCHOR,
                    List.of(usShipment, ukShipment), 1);

            // Then - 同一周期内应合并为1份DN
            assertEquals(1, results.size());
            assertEquals(80, results.get(0).getDn().getTotalQuantity());
            assertEquals(2, results.get(0).getItems().size());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建指定MSKU列表的货件
     */
    private ShipmentInput createShipment(String shipmentNo, LocalDateTime createTime,
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
