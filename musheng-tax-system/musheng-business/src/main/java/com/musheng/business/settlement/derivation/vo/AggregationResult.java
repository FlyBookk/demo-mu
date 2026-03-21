package com.musheng.business.settlement.derivation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 销售数据汇总结果
 *
 * <p>包含按站点+MSKU维度的净销售数量，以及订单汇率映射。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationResult {

    /**
     * 站点 → (MSKU → 净销售数量)
     */
    private Map<String, Map<String, Integer>> netSalesMap;

    /**
     * orderId → exchangeRate
     */
    private Map<String, BigDecimal> orderRateMap;

    /**
     * 供应商结差退款汇总：站点编码(siteCode) → (MSKU(sku) → 退款明细)
     *
     * <p>按供货协议第 3.1 条，记录结算周期内属于供应商供货范围的退款数量和金额，
     * 用于供应商结差扣除计算。该字段为可选字段，不影响现有消费方。</p>
     */
    private Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap;
}
