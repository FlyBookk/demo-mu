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
}
