package com.musheng.business.settlement.derivation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.settlement.derivation.service.SalesDataAggregator;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售数据汇总组件实现类
 *
 * <p>从配送数据出发，通过 orderId 关联销售数据，
 * 按站点+MSKU 维度汇总净销售数量（income - refund）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class SalesDataAggregatorImpl implements SalesDataAggregator {

    @Autowired
    private ShippingDataMapper shippingDataMapper;

    @Autowired
    private SalesDataMapper salesDataMapper;

    @Override
    public AggregationResult aggregateNetSales(Long shopId, LocalDate periodStart, LocalDate periodEnd) {
        log.info("开始汇总净销售数量，店铺: {}, 周期: {} ~ {}", shopId, periodStart, periodEnd);

        // 1. 查询周期内配送记录
        List<ShippingData> shippingList = queryShippingData(shopId, periodStart, periodEnd);

        if (CollectionUtils.isEmpty(shippingList)) {
            log.info("周期 {} ~ {} 内无配送数据，返回空结果", periodStart, periodEnd);
            return AggregationResult.builder()
                    .netSalesMap(Collections.emptyMap())
                    .orderRateMap(Collections.emptyMap())
                    .build();
        }

        // 2. 收集 orderId 集合，构建 orderRateMap（过滤 orderId 为空的记录）
        Set<String> orderIds = new HashSet<>();
        Map<String, BigDecimal> orderRateMap = new HashMap<>();
        for (ShippingData shipping : shippingList) {
            if (shipping.getOrderId() == null || shipping.getOrderId().isBlank()) {
                continue;
            }
            orderIds.add(shipping.getOrderId());
            orderRateMap.put(shipping.getOrderId(), shipping.getExchangeRate());
        }

        if (CollectionUtils.isEmpty(orderIds)) {
            log.info("周期 {} ~ {} 内配送数据均无有效订单号，返回空结果", periodStart, periodEnd);
            return AggregationResult.builder()
                    .netSalesMap(Collections.emptyMap())
                    .orderRateMap(Collections.emptyMap())
                    .build();
        }

        log.info("配送数据查询完成，订单数: {}", orderIds.size());

        // 3. 用 orderId 关联查询销售数据
        List<SalesData> salesList = querySalesData(shopId, orderIds);

        // 4. 按站点+MSKU 维度汇总净销售数量
        Map<String, Map<String, Integer>> netSalesMap = calculateNetSales(salesList);

        log.info("净销售数量汇总完成，站点数: {}, MSKU总数: {}",
                netSalesMap.size(),
                netSalesMap.values().stream().mapToInt(Map::size).sum());

        return AggregationResult.builder()
                .netSalesMap(netSalesMap)
                .orderRateMap(orderRateMap)
                .build();
    }

    /**
     * 查询周期内配送数据
     *
     * @param shopId 店铺ID
     * @param periodStart 周期起始日（含）
     * @param periodEnd 周期结束日（含）
     * @return 配送数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private List<ShippingData> queryShippingData(Long shopId, LocalDate periodStart, LocalDate periodEnd) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingData::getShopId, shopId)
                .ge(ShippingData::getShipDate, periodStart)
                .le(ShippingData::getShipDate, periodEnd);
        return shippingDataMapper.selectList(wrapper);
    }

    /**
     * 用 orderId 集合关联查询销售数据（分批查询，避免 IN 子句过长）
     *
     * @param shopId 店铺ID
     * @param orderIds 订单ID集合
     * @return 销售数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private List<SalesData> querySalesData(Long shopId, Set<String> orderIds) {
        List<String> orderIdList = new ArrayList<>(orderIds);
        List<SalesData> result = new ArrayList<>();
        // 分批查询，每批最多 500 个 orderId，避免 SQL IN 子句过长
        int batchSize = 500;
        for (int i = 0; i < orderIdList.size(); i += batchSize) {
            List<String> batch = orderIdList.subList(i, Math.min(i + batchSize, orderIdList.size()));
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SalesData::getShopId, shopId)
                    .in(SalesData::getOrderId, batch);
            result.addAll(salesDataMapper.selectList(wrapper));
        }
        return result;
    }

    /**
     * 按站点+MSKU 维度计算净销售数量
     *
     * <p>income 类型累加，refund 类型累减。</p>
     *
     * @param salesList 销售数据列表
     * @return 站点 → (MSKU → 净销售数量)
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, Map<String, Integer>> calculateNetSales(List<SalesData> salesList) {
        Map<String, Map<String, Integer>> netSalesMap = new HashMap<>();

        for (SalesData sales : salesList) {
            String siteCode = sales.getSiteCode();
            String sku = sales.getSku();
            int quantity = sales.getQuantity() != null ? sales.getQuantity() : 0;

            // income 累加，refund 累减
            int delta;
            if ("income".equals(sales.getTransactionCategory())) {
                delta = quantity;
            } else if ("refund".equals(sales.getTransactionCategory())) {
                delta = -quantity;
            } else {
                // 其他类型跳过
                continue;
            }

            netSalesMap
                    .computeIfAbsent(siteCode, k -> new HashMap<>())
                    .merge(sku, delta, Integer::sum);
        }

        return netSalesMap;
    }
}
