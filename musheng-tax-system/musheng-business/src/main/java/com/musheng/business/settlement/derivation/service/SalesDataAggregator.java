package com.musheng.business.settlement.derivation.service;

import com.musheng.business.settlement.derivation.vo.AggregationResult;

import java.time.LocalDate;

/**
 * 销售数据汇总组件接口
 *
 * <p>负责从配送数据出发，通过 orderId 关联销售数据，
 * 汇总净销售数量（income - refund），按站点+MSKU 维度聚合。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface SalesDataAggregator {

    /**
     * 汇总净销售数量
     *
     * <p>路径：t_shipping_data → orderId → t_sales_data (income - refund)
     * 返回：按站点+MSKU维度的净销售数量 + orderId→exchangeRate 映射</p>
     *
     * @param shopId 店铺ID
     * @param periodStart 周期起始日（含）
     * @param periodEnd 周期结束日（含）
     * @return 汇总结果，包含 netSalesMap 和 orderRateMap
     * @author wanhua
     * 10:30 2026年01月29日
     */
    AggregationResult aggregateNetSales(Long shopId, LocalDate periodStart, LocalDate periodEnd);
}
