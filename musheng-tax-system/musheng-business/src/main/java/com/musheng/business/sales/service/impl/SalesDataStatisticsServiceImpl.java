package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.utils.DateParseUtils;
import com.musheng.business.common.utils.MoneyConvertUtils;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.service.SalesDataStatisticsService;
import com.musheng.common.context.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售数据统计服务实现类
 * 
 * 职责：
 * 1. 获取销售数据统计汇总
 * 2. 按交易类型分组统计
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataStatisticsServiceImpl implements SalesDataStatisticsService {

    private final SalesDataMapper salesDataMapper;

    @Override
    public Map<String, Object> getSummary(String keyword, String siteCode, String settlementId, 
                                          String transactionCategory, String startDate, String endDate) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(SalesData::getShopId, shopId);

        // 关键字搜索（订单号/SKU）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SalesData::getOrderId, keyword)
                    .or()
                    .like(SalesData::getSku, keyword));
        }
        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(settlementId)) {
            wrapper.eq(SalesData::getSettlementId, settlementId);
        }
        if (StringUtils.hasText(transactionCategory)) {
            wrapper.eq(SalesData::getTransactionCategory, transactionCategory);
        }
        applyDateRangeFilter(wrapper, startDate, endDate);

        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", dataList.size());
        summary.put("totalQuantity", dataList.stream()
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum());

        // 按汇率转换为人民币后汇总（多站点数据统一货币）
        summary.put("totalProductSalesCny", dataList.stream()
                .map(d -> MoneyConvertUtils.convertToCny(d.getProductSales(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalSellingFeesCny", dataList.stream()
                .map(d -> MoneyConvertUtils.convertToCny(d.getSellingFees(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalFbaFeesCny", dataList.stream()
                .map(d -> MoneyConvertUtils.convertToCny(d.getFbaFees(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalOtherFeesCny", dataList.stream()
                .map(d -> MoneyConvertUtils.convertToCny(d.getOtherTransactionFees(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalAmountCny", dataList.stream()
                .map(d -> MoneyConvertUtils.convertToCny(d.getTotal(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 货币统一为人民币
        summary.put("currencyCode", "CNY");

        return summary;
    }

    @Override
    public List<Map<String, Object>> getStatByType(String siteCode, String startDate, String endDate) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(SalesData::getShopId, shopId);

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(SalesData::getSiteCode, siteCode);
        }
        applyDateRangeFilter(wrapper, startDate, endDate);

        List<SalesData> dataList = salesDataMapper.selectList(wrapper);

        // Group by transaction category
        Map<String, List<SalesData>> grouped = dataList.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getTransactionCategory() != null ? d.getTransactionCategory() : "other"
                ));

        List<Map<String, Object>> stats = new ArrayList<>();
        int totalCount = dataList.size();

        for (Map.Entry<String, List<SalesData>> entry : grouped.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("transactionCategory", entry.getKey());
            stat.put("count", entry.getValue().size());
            stat.put("totalAmount", entry.getValue().stream()
                    .map(d -> d.getTotal() != null ? d.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stat.put("percentage", totalCount > 0 ? (entry.getValue().size() * 100.0 / totalCount) : 0);
            stats.add(stat);
        }

        return stats;
    }

    /**
     * 添加日期范围过滤条件到查询包装器
     * 
     * @param wrapper   查询包装器
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    private void applyDateRangeFilter(LambdaQueryWrapper<SalesData> wrapper, String startDate, String endDate) {
        LocalDateTime start = DateParseUtils.parseStartDate(startDate);
        if (start != null) {
            wrapper.ge(SalesData::getTransactionDate, start);
        }
        LocalDateTime end = DateParseUtils.parseEndDate(endDate);
        if (end != null) {
            wrapper.le(SalesData::getTransactionDate, end);
        }
    }
}
