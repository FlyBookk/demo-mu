package com.musheng.business.settlement.derivation.service;

import com.musheng.business.settlement.derivation.vo.AggregationResult;

import java.time.LocalDate;
import java.util.List;

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
     * 汇总净销售数量（按站点+店铺过滤）
     *
     * <p>路径：t_shipping_data → orderId → t_sales_data (income - refund)
     * 返回：按站点+MSKU维度的净销售数量 + orderId→exchangeRate 映射。
     * DB 层根据 shopId 和 siteCodes 过滤，确保数据隔离。</p>
     *
     * @param shopId      店铺ID
     * @param periodStart 周期起始日（含）
     * @param periodEnd   周期结束日（含）
     * @param siteCodes   站点代码列表（null 或空表示不限制站点）
     * @return 汇总结果，包含 netSalesMap 和 orderRateMap
     */
    AggregationResult aggregateNetSales(Long shopId, LocalDate periodStart, LocalDate periodEnd,
                                        List<String> siteCodes);

    /**
     * 禁止无站点过滤的聚合调用（站点是硬性条件）
     *
     * @deprecated 必须使用带 siteCodes 参数的重载方法，确保站点数据隔离
     */
    @Deprecated
    default AggregationResult aggregateNetSales(Long shopId, LocalDate periodStart, LocalDate periodEnd) {
        throw new UnsupportedOperationException(
                "必须提供 siteCodes 参数，禁止跨站点聚合销售数据。请使用 aggregateNetSales(shopId, periodStart, periodEnd, siteCodes)");
    }
}
