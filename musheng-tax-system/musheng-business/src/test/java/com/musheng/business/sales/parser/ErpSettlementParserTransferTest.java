package com.musheng.business.sales.parser;

import com.musheng.business.sales.entity.SalesData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpSettlementParser - Transfer 分类映射单元测试
 *
 * 通过 parse(String, ParseContext, int) 公共方法间接测试 convertToSalesData 中的
 * transactionCategory 分类逻辑，验证 Transfer 来源类型被正确映射为 "transfer"，
 * 以及非 Transfer 来源类型保持原有分类逻辑不变。
 *
 * 需求: 3.1, 3.2
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@DisplayName("ErpSettlementParser - Transfer 分类映射测试")
class ErpSettlementParserTransferTest {

    private ErpSettlementParser parser;

    /**
     * ERP CSV 表头（与 ErpSettlementParser 中 ERP_HEADER_MAPPING 对应的中文表头）
     */
    private static final String CSV_HEADER = "结算编号,订单号,店铺,国家,报告类型,配送方式,来源,MSKU,交易类型,结算时间,币种,金额,数量,结算状态,转账状态,Settlement ID,SKU,品名,FNSKU";

    @BeforeEach
    void setUp() {
        parser = new ErpSettlementParser();
    }

    // ==================== Transfer 来源类型测试 ====================

    @Test
    @DisplayName("convertToSalesData - Transfer来源应映射为transfer分类")
    void testConvertToSalesData_TransferSource_ShouldMapToTransferCategory() {
        // Given - 构造来源为 Transfer 的 ERP CSV 数据
        String csvContent = CSV_HEADER + "\n"
                + "S001,111-1234567-1234567,TestStore,US,,FBA,Transfer,SKU001,Principal,2026-01-15 10:00:00,USD,100.00,1,已结算,已转账,AMZ001,SKU001,测试商品,FNSKU001";

        ParseContext context = ParseContext.builder()
                .importBatchId(1L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess(), "解析应成功");
        assertEquals(1, result.getDataList().size(), "应解析出1条记录");

        SalesData data = result.getDataList().get(0);
        assertEquals("transfer", data.getTransactionCategory(),
                "Transfer 来源类型应映射为 transaction_category = 'transfer'");
    }

    // ==================== 非 Transfer 来源类型测试 ====================

    @Test
    @DisplayName("convertToSalesData - Shipment来源应映射为income分类")
    void testConvertToSalesData_ShipmentSource_ShouldMapToIncomeCategory() {
        // Given - 构造来源为 Shipment 的 ERP CSV 数据
        String csvContent = CSV_HEADER + "\n"
                + "S002,222-1234567-1234567,TestStore,US,,FBA,Shipment,SKU002,Principal,2026-01-15 10:00:00,USD,200.00,2,已结算,已转账,AMZ002,SKU002,测试商品2,FNSKU002";

        ParseContext context = ParseContext.builder()
                .importBatchId(2L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess(), "解析应成功");
        assertEquals(1, result.getDataList().size());

        SalesData data = result.getDataList().get(0);
        assertEquals("income", data.getTransactionCategory(),
                "Shipment 来源类型应映射为 transaction_category = 'income'");
    }

    @Test
    @DisplayName("convertToSalesData - Refund来源应映射为refund分类")
    void testConvertToSalesData_RefundSource_ShouldMapToRefundCategory() {
        // Given
        String csvContent = CSV_HEADER + "\n"
                + "S003,333-1234567-1234567,TestStore,US,,FBA,Refund,SKU003,Principal,2026-01-15 10:00:00,USD,-50.00,1,已结算,已转账,AMZ003,SKU003,测试商品3,FNSKU003";

        ParseContext context = ParseContext.builder()
                .importBatchId(3L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess());
        SalesData data = result.getDataList().get(0);
        assertEquals("refund", data.getTransactionCategory(),
                "Refund 来源类型应映射为 transaction_category = 'refund'");
    }

    @Test
    @DisplayName("convertToSalesData - ServiceFee来源应映射为fee分类")
    void testConvertToSalesData_ServiceFeeSource_ShouldMapToFeeCategory() {
        // Given
        String csvContent = CSV_HEADER + "\n"
                + "S004,444-1234567-1234567,TestStore,US,,FBA,ServiceFee,SKU004,Commission,2026-01-15 10:00:00,USD,-10.00,1,已结算,已转账,AMZ004,SKU004,测试商品4,FNSKU004";

        ParseContext context = ParseContext.builder()
                .importBatchId(4L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess());
        SalesData data = result.getDataList().get(0);
        assertEquals("fee", data.getTransactionCategory(),
                "ServiceFee 来源类型应映射为 transaction_category = 'fee'");
    }

    @Test
    @DisplayName("convertToSalesData - Adjustment来源应映射为adjustment分类")
    void testConvertToSalesData_AdjustmentSource_ShouldMapToAdjustmentCategory() {
        // Given
        String csvContent = CSV_HEADER + "\n"
                + "S005,555-1234567-1234567,TestStore,US,,FBA,Adjustment,SKU005,Revenue,2026-01-15 10:00:00,USD,5.00,1,已结算,已转账,AMZ005,SKU005,测试商品5,FNSKU005";

        ParseContext context = ParseContext.builder()
                .importBatchId(5L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess());
        SalesData data = result.getDataList().get(0);
        assertEquals("adjustment", data.getTransactionCategory(),
                "Adjustment 来源类型应映射为 transaction_category = 'adjustment'");
    }

    // ==================== 混合数据测试 ====================

    @Test
    @DisplayName("convertToSalesData - 混合来源类型应各自映射为正确分类")
    void testConvertToSalesData_MixedSources_ShouldMapToCorrectCategories() {
        // Given - 包含 Transfer、Shipment、Refund 三种来源的混合数据
        String csvContent = CSV_HEADER + "\n"
                + "S010,AAA-1234567-1234567,TestStore,US,,FBA,Transfer,SKU010,Principal,2026-01-15 10:00:00,USD,500.00,1,已结算,已转账,AMZ010,SKU010,商品A,FNSKU010\n"
                + "S011,BBB-1234567-1234567,TestStore,US,,FBA,Shipment,SKU011,Principal,2026-01-15 10:00:00,USD,300.00,2,已结算,已转账,AMZ011,SKU011,商品B,FNSKU011\n"
                + "S012,CCC-1234567-1234567,TestStore,US,,FBA,Refund,SKU012,Principal,2026-01-15 10:00:00,USD,-100.00,1,已结算,已转账,AMZ012,SKU012,商品C,FNSKU012";

        ParseContext context = ParseContext.builder()
                .importBatchId(10L)
                .build();

        // When
        ParseResult result = parser.parse(csvContent, context, 100);

        // Then
        assertTrue(result.getSuccess());
        List<SalesData> dataList = result.getDataList();
        assertEquals(3, dataList.size(), "应解析出3条记录");

        // 验证各记录的分类（按 CSV 行顺序，LinkedHashMap 保持插入顺序）
        assertEquals("transfer", dataList.get(0).getTransactionCategory(),
                "第1条 Transfer 记录应为 transfer 分类");
        assertEquals("income", dataList.get(1).getTransactionCategory(),
                "第2条 Shipment 记录应为 income 分类");
        assertEquals("refund", dataList.get(2).getTransactionCategory(),
                "第3条 Refund 记录应为 refund 分类");
    }
}
