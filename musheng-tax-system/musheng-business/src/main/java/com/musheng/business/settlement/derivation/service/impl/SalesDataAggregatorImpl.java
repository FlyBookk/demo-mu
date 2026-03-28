package com.musheng.business.settlement.derivation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.settlement.derivation.service.SalesDataAggregator;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.business.settlement.derivation.vo.SupplierRefundDetail;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * 供应商 MSKU 前缀列表（按前缀长度从长到短排列，避免 MS- 误匹配 MSUS- 等）
     */
    private static final List<String> SUPPLIER_MSKU_PREFIXES = List.of(
            "MSUS-", "MSCA-", "MSUK-", "MSEU-", "MS-"
    );

    @Autowired
    private ShippingDataMapper shippingDataMapper;

    @Autowired
    private SalesDataMapper salesDataMapper;

    @Override
    public AggregationResult aggregateNetSales(Long shopId, LocalDate periodStart, LocalDate periodEnd,
                                               List<String> siteCodes) {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId 不能为空");
        }
        if (CollectionUtils.isEmpty(siteCodes)) {
            throw new IllegalArgumentException("站点列表不能为空，shopId=" + shopId);
        }
        log.info("[Aggregator] 开始汇总净销售数量（双路径），shopId={}, 周期: {} ~ {}, 站点: {}",
                shopId, periodStart, periodEnd, siteCodes);

        // === Income 路径：按配送日期匹配周期 ===
        IncomePathResult incomeResult = processIncomePath(shopId, periodStart, periodEnd, siteCodes);

        // === Refund 路径：按结算日期匹配周期 ===
        List<SalesData> refundList = queryRefundByTransactionDate(shopId, periodStart, periodEnd, siteCodes);

        // 处理退款汇率回退
        Map<String, BigDecimal> refundOrderRateMap = processRefundExchangeRates(refundList, incomeResult.getOrderRateMap());

        // 计算退款净销售数量
        Map<String, Map<String, Integer>> refundNetSalesMap = calculateRefundNetSales(refundList);

        // 构建供应商结差退款汇总
        Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap = buildSupplierRefundMap(refundList, mergeOrderRateMaps(incomeResult.getOrderRateMap(), refundOrderRateMap));

        // === 合并两条路径的结果 ===
        Map<String, Map<String, Integer>> mergedNetSalesMap = mergeNetSalesMaps(incomeResult.getIncomeNetSalesMap(), refundNetSalesMap);
        Map<String, BigDecimal> mergedOrderRateMap = mergeOrderRateMaps(incomeResult.getOrderRateMap(), refundOrderRateMap);

        log.info("双路径汇总完成，站点数: {}, MSKU总数: {}, 供应商结差站点数: {}",
                mergedNetSalesMap.size(),
                mergedNetSalesMap.values().stream().mapToInt(Map::size).sum(),
                supplierRefundMap.size());

        return AggregationResult.builder()
                .netSalesMap(mergedNetSalesMap)
                .orderRateMap(mergedOrderRateMap)
                .supplierRefundMap(supplierRefundMap)
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
    private List<ShippingData> queryShippingData(Long shopId, LocalDate periodStart, LocalDate periodEnd,
                                                  List<String> siteCodes) {
        if (CollectionUtils.isEmpty(siteCodes)) {
            throw new IllegalArgumentException("站点列表不能为空，禁止跨站点查询配送数据");
        }
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingData::getShopId, shopId)
                .ge(ShippingData::getShipDate, periodStart)
                .le(ShippingData::getShipDate, periodEnd)
                .in(ShippingData::getSiteCode, siteCodes);
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
    private List<SalesData> querySalesData(Long shopId, Set<String> orderIds, List<String> siteCodes) {
        List<String> orderIdList = new ArrayList<>(orderIds);
        List<SalesData> result = new ArrayList<>();
        // 分批查询，每批最多 500 个 orderId，避免 SQL IN 子句过长
        int batchSize = 500;
        for (int i = 0; i < orderIdList.size(); i += batchSize) {
            List<String> batch = orderIdList.subList(i, Math.min(i + batchSize, orderIdList.size()));
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SalesData::getShopId, shopId)
                    .in(SalesData::getOrderId, batch)
                    .in(SalesData::getSiteCode, siteCodes); // 双重保险：确保站点一致
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
        return calculateNetSales(salesList, null);
    }

    /**
     * 按站点+MSKU 维度计算净销售数量（支持按 transactionCategory 过滤）
     *
     * <p>income 类型累加，refund 类型累减。
     * 当 categoryFilter 不为空时，仅处理匹配该分类的记录。</p>
     *
     * @param salesList 销售数据列表
     * @param categoryFilter 交易分类过滤条件（null 表示不过滤，处理所有 income/refund）
     * @return 站点 → (MSKU → 净销售数量)
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, Map<String, Integer>> calculateNetSales(List<SalesData> salesList, String categoryFilter) {
        Map<String, Map<String, Integer>> netSalesMap = new HashMap<>();

        for (SalesData sales : salesList) {
            String siteCode = sales.getSiteCode();
            String sku = sales.getSku();
            int quantity = sales.getQuantity() != null ? sales.getQuantity() : 0;
            String category = sales.getTransactionCategory();

            // 如果指定了过滤条件，跳过不匹配的记录
            if (categoryFilter != null && !categoryFilter.equals(category)) {
                continue;
            }

            // income 累加，refund 累减
            int delta;
            if ("income".equals(category)) {
                delta = quantity;
            } else if ("refund".equals(category)) {
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

    /**
     * 处理 Income 路径：查询配送数据 → 收集 orderId → 关联 income 销售记录 → 汇总
     *
     * <p>从 t_shipping_data 按 shipDate 在周期内查询配送记录，收集 orderId 集合，
     * 构建 orderRateMap，再用 orderId 关联 t_sales_data 获取 income 记录，
     * 按 siteCode + sku 维度累加数量。</p>
     *
     * @param shopId 店铺ID
     * @param periodStart 周期起始日（含）
     * @param periodEnd 周期结束日（含）
     * @return Income 路径处理结果（包含 incomeNetSalesMap 和 orderRateMap）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private IncomePathResult processIncomePath(Long shopId, LocalDate periodStart, LocalDate periodEnd,
                                                List<String> siteCodes) {
        // 1. 查询周期内配送记录（按店铺+站点过滤）
        List<ShippingData> shippingList = queryShippingData(shopId, periodStart, periodEnd, siteCodes);

        if (CollectionUtils.isEmpty(shippingList)) {
            log.info("Income 路径：周期 {} ~ {} 内无配送数据", periodStart, periodEnd);
            return IncomePathResult.builder()
                    .incomeNetSalesMap(Collections.emptyMap())
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
            log.info("Income 路径：周期 {} ~ {} 内配送数据均无有效订单号", periodStart, periodEnd);
            return IncomePathResult.builder()
                    .incomeNetSalesMap(Collections.emptyMap())
                    .orderRateMap(Collections.emptyMap())
                    .build();
        }

        log.info("Income 路径：配送数据查询完成，订单数: {}", orderIds.size());

        // 3. 用 orderId 关联查询销售数据（双重保险：同时过滤 siteCode）
        List<SalesData> salesList = querySalesData(shopId, orderIds, siteCodes);

        // 4. 仅处理 income 记录，按站点+MSKU 维度汇总
        Map<String, Map<String, Integer>> incomeNetSalesMap = calculateNetSales(salesList, "income");

        log.info("Income 路径：汇总完成，站点数: {}, MSKU总数: {}",
                incomeNetSalesMap.size(),
                incomeNetSalesMap.values().stream().mapToInt(Map::size).sum());

        return IncomePathResult.builder()
                .incomeNetSalesMap(incomeNetSalesMap)
                .orderRateMap(orderRateMap)
                .build();
    }

    /**
     * 判断 SKU 是否属于供应商（Hong Kong Andeo Group Limited）供货范围
     *
     * <p>按前缀长度从长到短匹配：MSUS-、MSCA-、MSUK-、MSEU-、MS-，
     * 避免 MS- 误匹配 MSUS- 等较长前缀。</p>
     *
     * @param sku SKU 编码
     * @return 属于供应商供货范围返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    static boolean isSupplierMsku(String sku) {
        if (!StringUtils.hasText(sku)) {
            return false;
        }
        for (String prefix : SUPPLIER_MSKU_PREFIXES) {
            if (sku.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按结算日期（transactionDate）查询周期内的退款记录
     *
     * <p>直接从 t_sales_data 按 transactionDate 在周期内查询
     * transactionCategory = 'refund' 且 sku 非空非空白的记录。
     * 注意 transactionDate 是 LocalDateTime 类型，需要转换：
     * periodStart 的 00:00:00 到 periodEnd+1 的 00:00:00（不含）。</p>
     *
     * @param shopId 店铺ID
     * @param periodStart 周期起始日（含）
     * @param periodEnd 周期结束日（含）
     * @return 符合条件的退款销售数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private List<SalesData> queryRefundByTransactionDate(Long shopId, LocalDate periodStart, LocalDate periodEnd,
                                                          List<String> siteCodes) {
        if (CollectionUtils.isEmpty(siteCodes)) {
            throw new IllegalArgumentException("站点列表不能为空，禁止跨站点查询退款数据");
        }
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getShopId, shopId)
                .ge(SalesData::getTransactionDate, periodStart.atStartOfDay())
                .lt(SalesData::getTransactionDate, periodEnd.plusDays(1).atStartOfDay())
                .eq(SalesData::getTransactionCategory, "refund")
                .isNotNull(SalesData::getSku)
                .ne(SalesData::getSku, "")
                .in(SalesData::getSiteCode, siteCodes);

        List<SalesData> refundList = salesDataMapper.selectList(wrapper);

        // 查询后在 Java 中进一步过滤空白 sku（数据库 ne("") 无法过滤纯空格字符串）
        refundList.removeIf(sales -> !StringUtils.hasText(sales.getSku()));

        log.info("Refund 路径：查询周期 {} ~ {} 内退款记录 {} 条", periodStart, periodEnd, refundList.size());
        return refundList;
    }

    /**
     * 处理退款记录的汇率回退策略
     *
     * <p>汇率回退优先级：
     * 1. 先查 incomeOrderRateMap（income 路径构建的 orderRateMap，来自配送数据）
     * 2. 回退到退款记录自身的 exchangeRate
     * 3. 都为空时记录 warn 日志
     * 将确定的汇率写入 refund 路径的 orderRateMap 并返回。</p>
     *
     * @param refundList 退款记录列表
     * @param incomeOrderRateMap income 路径构建的 orderRateMap（orderId → 汇率）
     * @return refund 路径的 orderRateMap（orderId → 汇率）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, BigDecimal> processRefundExchangeRates(List<SalesData> refundList, Map<String, BigDecimal> incomeOrderRateMap) {
        Map<String, BigDecimal> refundOrderRateMap = new HashMap<>();
        Map<String, BigDecimal> safeIncomeRateMap = incomeOrderRateMap != null ? incomeOrderRateMap : Collections.emptyMap();

        for (SalesData refund : refundList) {
            String orderId = refund.getOrderId();
            if (!StringUtils.hasText(orderId)) {
                continue;
            }

            // 已处理过的 orderId 跳过
            if (refundOrderRateMap.containsKey(orderId)) {
                continue;
            }

            // 优先从 income 路径的 orderRateMap 获取汇率
            BigDecimal rate = safeIncomeRateMap.get(orderId);
            if (rate != null) {
                refundOrderRateMap.put(orderId, rate);
                continue;
            }

            // 回退到退款记录自身的 exchangeRate
            rate = refund.getExchangeRate();
            if (rate != null) {
                refundOrderRateMap.put(orderId, rate);
                continue;
            }

            // 都为空时记录 warn 日志
            log.warn("退款记录汇率缺失，orderId: {}, sku: {}, transactionDate: {}",
                    orderId, refund.getSku(), refund.getTransactionDate());
        }

        return refundOrderRateMap;
    }

    /**
     * 计算退款的净销售数量（按 siteCode + sku 维度累减）
     *
     * <p>复用 calculateNetSales 方法，传入 categoryFilter = "refund"，
     * 使退款记录的 quantity 取负值进行累减。</p>
     *
     * @param refundList 退款记录列表
     * @return 站点 → (MSKU → 退款净销售数量，为负值)
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, Map<String, Integer>> calculateRefundNetSales(List<SalesData> refundList) {
        return calculateNetSales(refundList, "refund");
    }

    /**
     * 构建供应商结差退款汇总
     *
     * <p>遍历退款记录，对 isSupplierMsku(sku) = true 的记录按 siteCode + sku 分组，
     * 汇总退款数量（取绝对值，正数）和退款金额（使用 total 字段，取绝对值，正数）。
     * 汇率从 orderRateMap 获取，如果为 null 则跳过金额计算但仍计入数量。</p>
     *
     * @param refundList 退款记录列表
     * @param orderRateMap orderId → 汇率映射
     * @return 站点 → (MSKU → 供应商退款明细)
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, Map<String, SupplierRefundDetail>> buildSupplierRefundMap(
            List<SalesData> refundList, Map<String, BigDecimal> orderRateMap) {
        Map<String, Map<String, SupplierRefundDetail>> supplierRefundMap = new HashMap<>();
        Map<String, BigDecimal> safeOrderRateMap = orderRateMap != null ? orderRateMap : Collections.emptyMap();

        for (SalesData refund : refundList) {
            String sku = refund.getSku();
            // 仅处理供应商 MSKU
            if (!isSupplierMsku(sku)) {
                continue;
            }

            String siteCode = refund.getSiteCode();
            int quantity = refund.getQuantity() != null ? Math.abs(refund.getQuantity()) : 0;
            BigDecimal total = refund.getTotal() != null ? refund.getTotal().abs() : BigDecimal.ZERO;
            String orderId = refund.getOrderId();
            BigDecimal exchangeRate = StringUtils.hasText(orderId) ? safeOrderRateMap.get(orderId) : null;

            // 获取或创建该 siteCode + sku 的汇总记录
            SupplierRefundDetail detail = supplierRefundMap
                    .computeIfAbsent(siteCode, k -> new HashMap<>())
                    .get(sku);

            if (detail == null) {
                // 首条记录，初始化
                detail = SupplierRefundDetail.builder()
                        .quantity(quantity)
                        .amount(exchangeRate != null ? total : BigDecimal.ZERO)
                        .exchangeRate(exchangeRate)
                        .build();
                supplierRefundMap.get(siteCode).put(sku, detail);
            } else {
                // 累加数量
                detail.setQuantity(detail.getQuantity() + quantity);
                // 累加金额（汇率为 null 时跳过金额计算）
                if (exchangeRate != null) {
                    detail.setAmount(detail.getAmount().add(total));
                    // 更新汇率为最新一条有效汇率
                    detail.setExchangeRate(exchangeRate);
                }
            }
        }

        log.info("供应商结差退款汇总完成，站点数: {}, MSKU总数: {}",
                supplierRefundMap.size(),
                supplierRefundMap.values().stream().mapToInt(Map::size).sum());

        return supplierRefundMap;
    }

    /**
     * 合并两条路径的 netSalesMap（income 路径 + refund 路径）
     *
     * <p>先复制 incomeMap 的所有数据，再遍历 refundMap，
     * 对同一 siteCode + sku 的值使用 Integer::sum 相加合并。</p>
     *
     * @param incomeMap income 路径的净销售数量 map（站点 → MSKU → 数量）
     * @param refundMap refund 路径的净销售数量 map（站点 → MSKU → 数量）
     * @return 合并后的 netSalesMap（站点 → MSKU → 净销售数量）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, Map<String, Integer>> mergeNetSalesMaps(
            Map<String, Map<String, Integer>> incomeMap,
            Map<String, Map<String, Integer>> refundMap) {
        // null 输入视为空 map
        Map<String, Map<String, Integer>> safeIncomeMap = incomeMap != null ? incomeMap : Collections.emptyMap();
        Map<String, Map<String, Integer>> safeRefundMap = refundMap != null ? refundMap : Collections.emptyMap();

        // 先复制 incomeMap 的所有数据
        Map<String, Map<String, Integer>> merged = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> siteEntry : safeIncomeMap.entrySet()) {
            merged.put(siteEntry.getKey(), new HashMap<>(siteEntry.getValue()));
        }

        // 再遍历 refundMap，同一 siteCode + sku 的值相加
        for (Map.Entry<String, Map<String, Integer>> siteEntry : safeRefundMap.entrySet()) {
            String siteCode = siteEntry.getKey();
            Map<String, Integer> refundSkuMap = siteEntry.getValue();
            Map<String, Integer> mergedSkuMap = merged.computeIfAbsent(siteCode, k -> new HashMap<>());
            for (Map.Entry<String, Integer> skuEntry : refundSkuMap.entrySet()) {
                mergedSkuMap.merge(skuEntry.getKey(), skuEntry.getValue(), Integer::sum);
            }
        }

        return merged;
    }

    /**
     * 合并两条路径的 orderRateMap（income 路径汇率优先）
     *
     * <p>先放入 refundRateMap 的所有数据，再用 incomeRateMap 覆盖（putAll），
     * 确保同一 orderId 在两条路径中都存在时使用 income 路径的汇率值。</p>
     *
     * @param incomeRateMap income 路径的 orderRateMap（orderId → 汇率）
     * @param refundRateMap refund 路径的 orderRateMap（orderId → 汇率）
     * @return 合并后的 orderRateMap（orderId → 汇率），income 优先
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Map<String, BigDecimal> mergeOrderRateMaps(
            Map<String, BigDecimal> incomeRateMap,
            Map<String, BigDecimal> refundRateMap) {
        // null 输入视为空 map
        Map<String, BigDecimal> safeIncomeRateMap = incomeRateMap != null ? incomeRateMap : Collections.emptyMap();
        Map<String, BigDecimal> safeRefundRateMap = refundRateMap != null ? refundRateMap : Collections.emptyMap();

        // 先放入 refundRateMap 的所有数据
        Map<String, BigDecimal> merged = new HashMap<>(safeRefundRateMap);
        // 再用 incomeRateMap 覆盖，确保 income 优先
        merged.putAll(safeIncomeRateMap);

        return merged;
    }

    /**
     * Income 路径处理结果
     *
     * <p>封装 income 路径的汇总数据，包含按站点+MSKU 维度的 income 数量
     * 和配送数据中的 orderId → exchangeRate 映射。</p>
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class IncomePathResult {
        /** 站点 → (MSKU → income 数量) */
        private Map<String, Map<String, Integer>> incomeNetSalesMap;
        /** orderId → exchangeRate */
        private Map<String, BigDecimal> orderRateMap;
    }
}
