package com.musheng.tiktok.settlement.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.product.entity.TiktokProduct;
import com.musheng.tiktok.product.service.TiktokProductService;
import com.musheng.tiktok.settlement.entity.TiktokSettlement;
import com.musheng.tiktok.settlement.entity.TiktokSettlementOrder;
import com.musheng.tiktok.settlement.mapper.TiktokSettlementMapper;
import com.musheng.tiktok.settlement.mapper.TiktokSettlementOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TK结算单服务（预校验 + 导入 + 费用归类 + 查询）
 *
 * @author wanhua
 * 19:44 2026年05月14日
 */
@Service
@Slf4j
public class TiktokSettlementService {

    @Autowired
    private TiktokSettlementMapper settlementMapper;
    @Autowired
    private TiktokSettlementOrderMapper orderMapper;
    @Autowired
    private TiktokProductService productService;
    @Autowired
    private com.musheng.tiktok.product.mapper.TiktokProductMapper productMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 预校验：检查结算单中所有sku_id是否在商品库中有映射
     *
     * @return 未映射的sku_id列表，空表示全部通过
     */
    public List<String> preValidate(MultipartFile file, String siteCode) throws IOException {
        Long shopId = ShopContext.requireShopId();
        Set<String> allSkuIds = new HashSet<>();

        // 解析 Order details sheet 提取所有 sku_id
        EasyExcel.read(file.getInputStream())
                .sheet("Order details")
                .headRowNumber(1)
                .registerReadListener(new PageReadListener<Map<Integer, String>>(rows -> {
                    for (Map<Integer, String> row : rows) {
                        String type = row.get(3);
                        String skuId = row.get(5);
                        if ("Order".equals(type) && StringUtils.hasText(skuId) && !"/".equals(skuId)) {
                            allSkuIds.add(skuId);
                        }
                    }
                }, 500))
                .doRead();

        // 批量查商品库
        Map<String, TiktokProduct> productMap = productService.findBySkuIds(shopId, allSkuIds);

        // 找出未映射的
        return allSkuIds.stream()
                .filter(skuId -> !productMap.containsKey(skuId))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 导入结算单（预校验通过后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importSettlement(MultipartFile file, String siteCode) throws IOException {
        Long shopId = ShopContext.requireShopId();
        long batchId = System.currentTimeMillis();

        // 加载商品映射
        Set<String> allSkuIds = new HashSet<>();
        List<Map<Integer, String>> orderRows = new ArrayList<>();
        List<Map<Integer, String>> stmtRows = new ArrayList<>();

        // 读取 Order details
        EasyExcel.read(file.getInputStream())
                .sheet("Order details")
                .headRowNumber(1)
                .registerReadListener(new PageReadListener<Map<Integer, String>>(rows -> {
                    for (Map<Integer, String> row : rows) {
                        orderRows.add(new HashMap<>(row));
                        String skuId = row.get(5);
                        if (StringUtils.hasText(skuId) && !"/".equals(skuId)) {
                            allSkuIds.add(skuId);
                        }
                    }
                }, 500))
                .doRead();

        // 读取 Statements
        EasyExcel.read(file.getInputStream())
                .sheet("Statements")
                .headRowNumber(1)
                .registerReadListener(new PageReadListener<Map<Integer, String>>(rows -> {
                    stmtRows.addAll(rows.stream().map(HashMap::new).collect(Collectors.toList()));
                }, 100))
                .doRead();

        // 商品映射
        Map<String, TiktokProduct> productMap = productService.findBySkuIds(shopId, allSkuIds);

        // 收集已存在的 statement_id（防止重复导入）
        Set<String> existingStatementIds = new HashSet<>();
        for (Map<Integer, String> row : stmtRows) {
            String stmtId = row.get(1);
            if (!StringUtils.hasText(stmtId)) continue;
            Long exists = settlementMapper.selectCount(new LambdaQueryWrapper<TiktokSettlement>()
                    .eq(TiktokSettlement::getShopId, shopId)
                    .eq(TiktokSettlement::getSiteCode, siteCode)
                    .eq(TiktokSettlement::getStatementId, stmtId));
            if (exists > 0) existingStatementIds.add(stmtId);
        }

        // 持久化 Statements（跳过已存在的）
        int stmtCount = 0;
        for (Map<Integer, String> row : stmtRows) {
            String stmtId = row.get(1);
            if (!StringUtils.hasText(stmtId)) continue;
            if (existingStatementIds.contains(stmtId)) continue;

            TiktokSettlement stmt = new TiktokSettlement();
            stmt.setShopId(shopId);
            stmt.setSiteCode(siteCode);
            stmt.setImportBatchId(batchId);
            stmt.setStatementId(stmtId);
            stmt.setStatementDate(parseDate(row.get(0)));
            stmt.setCurrency("USD");
            stmt.setSettlementAmount(parseBD(row.get(2)));
            stmt.setRevenue(parseBD(row.get(3)));
            stmt.setFees(parseBD(row.get(4)));
            stmt.setAdjustmentAmount(parseBD(row.get(5)));
            stmt.setPaymentId(row.getOrDefault(6, ""));
            settlementMapper.insert(stmt);
            stmtCount++;
        }

        // 持久化 Order details（跳过已存在 statement 下的行）
        int orderCount = 0;
        int skippedCount = 0;
        for (Map<Integer, String> row : orderRows) {
            String type = row.get(3);
            if (!StringUtils.hasText(type)) continue;

            // 该行所属的 statement 已存在，跳过
            String rowStmtId = row.getOrDefault(1, "");
            if (existingStatementIds.contains(rowStmtId)) {
                skippedCount++;
                continue;
            }

            String skuId = row.getOrDefault(5, "");
            if ("/".equals(skuId)) skuId = "";

            // 映射 msku（未映射的自动创建记录，msku为空表示待补充）
            TiktokProduct product = productMap.get(skuId);
            if (product == null && StringUtils.hasText(skuId)) {
                TiktokProduct newProduct = new TiktokProduct();
                newProduct.setShopId(shopId);
                newProduct.setSiteCode(siteCode);
                newProduct.setProductId("");
                newProduct.setSkuId(skuId);
                newProduct.setMsku("");
                newProduct.setProductName(row.getOrDefault(7, ""));
                newProduct.setStatus(1);
                productMapper.insert(newProduct);
                productMap.put(skuId, newProduct);
                product = newProduct;
            }
            String msku = product != null ? product.getMsku() : "";
            String prodId = product != null ? product.getProductId() : "";

            TiktokSettlementOrder order = new TiktokSettlementOrder();
            order.setShopId(shopId);
            order.setSiteCode(siteCode);
            order.setImportBatchId(batchId);
            order.setStatementId(row.getOrDefault(1, ""));
            order.setStatementDate(parseDate(row.get(0)));
            order.setCurrency(row.getOrDefault(2, "USD"));
            order.setType(type);
            order.setOrderId(row.getOrDefault(4, ""));
            order.setSkuId(skuId);
            order.setMsku(msku);
            order.setProductId(prodId);
            order.setQuantity(parseIntSafe(row.get(6)));
            order.setProductName(row.getOrDefault(7, ""));
            order.setSkuName(row.getOrDefault(8, ""));

            // 金额
            order.setTotalSettlementAmount(parseBD(row.get(9)));
            order.setTotalRevenue(parseBD(row.get(10)));
            order.setSubtotalAfterDiscount(parseBD(row.get(11)));
            order.setSubtotalBeforeDiscount(parseBD(row.get(12)));
            order.setSellerDiscount(parseBD(row.get(13)));
            order.setRefundAfterDiscount(parseBD(row.get(14)));
            order.setRefundBeforeDiscount(parseBD(row.get(15)));
            order.setRefundOfSellerDiscount(parseBD(row.get(16)));

            // 报税关键费用字段
            order.setReferralFee(parseBD(row.get(19)));
            order.setRefundAdminFee(parseBD(row.get(20)));
            order.setSellerShippingFee(parseBD(row.get(21)));
            order.setFbtFulfillmentFee(parseBD(row.get(22)));
            order.setActualReturnShippingFee(parseBD(row.get(28)));
            order.setReturnShippingReimb(parseBD(row.get(33)));

            // 费用归类
            classifyFees(order, row);

            // 原始JSON（col 18-61）
            try {
                Map<String, String> rawFees = new LinkedHashMap<>();
                for (int i = 17; i < Math.min(62, row.size()); i++) {
                    String val = row.get(i);
                    if (StringUtils.hasText(val) && !"0".equals(val)) {
                        rawFees.put("col" + (i + 1), val);
                    }
                }
                if (!rawFees.isEmpty()) {
                    order.setRawFeeJson(JSON.writeValueAsString(rawFees));
                }
            } catch (Exception ignored) {}

            orderMapper.insert(order);
            orderCount++;
        }

        if (skippedCount > 0) {
            log.info("跳过已存在结算单下的明细: {} 条", skippedCount);
        }

        log.info("TK结算单导入完成: shopId={}, batch={}, statements={}, orders={}", shopId, batchId, stmtCount, orderCount);
        return Map.of("statements", stmtCount, "orders", orderCount);
    }

    /**
     * 费用归类（6大类）
     */
    private void classifyFees(TiktokSettlementOrder order, Map<Integer, String> row) {
        // 佣金类: Referral(19) + Transaction(18) + RefundAdmin(20) + SalesTaxOnReferral(51)
        BigDecimal commission = abs(parseBD(row.get(19))).add(abs(parseBD(row.get(18)))).add(abs(parseBD(row.get(20)))).add(abs(parseBD(row.get(51))));
        order.setCommissionFee(commission.negate());

        // 物流类: SellerShipping(21) + FBT(22) + TKShipping(23) + Offset(24) + PlatformDisc(25) + CustomerShip(26) + RefundedShip(27) + ReturnShip(28) + subsidy(29) + discount(30) + reimb(31) + returnPaid(32) + returnReimb(33) + appFee(34) + protection(35) + overallSubsidy(36) + keySubsidy(37)
        BigDecimal logistics = BigDecimal.ZERO;
        for (int i = 21; i <= 37; i++) {
            logistics = logistics.add(parseBD(row.get(i)));
        }
        order.setLogisticsFee(logistics);

        // 联盟类: Affiliate(38) + Partner(39) + ShopAds(40) + Deposit(41) + Refund(42) + TKPartner(43) + PartnerShopAds(48)
        BigDecimal affiliate = parseBD(row.get(38)).add(parseBD(row.get(39))).add(parseBD(row.get(40)))
                .add(parseBD(row.get(41))).add(parseBD(row.get(42))).add(parseBD(row.get(43))).add(parseBD(row.get(48)));
        order.setAffiliateFee(affiliate);

        // 促销类: Co-funded(45) + SmartPromo(52) + CreatorBonus(47) + CampaignService(49) + CampaignResource(50) + ManagedPlan(54)(55) + periods(56-61)
        BigDecimal promotion = parseBD(row.get(45)).add(parseBD(row.get(47))).add(parseBD(row.get(49)))
                .add(parseBD(row.get(50))).add(parseBD(row.get(52))).add(parseBD(row.get(54))).add(parseBD(row.get(55)));
        for (int i = 56; i <= 61; i++) {
            promotion = promotion.add(parseBD(row.get(i)));
        }
        order.setPromotionFee(promotion);

        // 税费类: SmartPromoTax(46) + SalesTaxOnReferral(51) 已计入commission，这里放 Sales tax 相关的其他
        BigDecimal tax = parseBD(row.get(46));
        order.setTaxFee(tax);

        // 其他: Customs(44)
        order.setOtherFee(parseBD(row.get(44)));
    }

    /**
     * 结算明细分页查询
     */
    public Page<TiktokSettlementOrder> listOrders(String siteCode, String type, String msku, String startDate, String endDate, Boolean unmappedOnly, Integer current, Integer size) {
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<TiktokSettlementOrder> wrapper = new LambdaQueryWrapper<TiktokSettlementOrder>()
                .eq(TiktokSettlementOrder::getShopId, shopId)
                .eq(TiktokSettlementOrder::getSiteCode, siteCode);
        if (StringUtils.hasText(type)) wrapper.eq(TiktokSettlementOrder::getType, type);
        if (StringUtils.hasText(msku)) wrapper.like(TiktokSettlementOrder::getMsku, msku);
        if (StringUtils.hasText(startDate)) wrapper.ge(TiktokSettlementOrder::getStatementDate, LocalDate.parse(startDate));
        if (StringUtils.hasText(endDate)) wrapper.le(TiktokSettlementOrder::getStatementDate, LocalDate.parse(endDate));
        if (Boolean.TRUE.equals(unmappedOnly)) wrapper.and(w -> w.isNull(TiktokSettlementOrder::getMsku).or().eq(TiktokSettlementOrder::getMsku, ""));
        wrapper.orderByDesc(TiktokSettlementOrder::getStatementDate);
        return orderMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 结算汇总列表
     */
    public Page<TiktokSettlement> listStatements(String siteCode, Integer current, Integer size) {
        Long shopId = ShopContext.requireShopId();
        return settlementMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<TiktokSettlement>()
                        .eq(TiktokSettlement::getShopId, shopId)
                        .eq(TiktokSettlement::getSiteCode, siteCode)
                        .orderByDesc(TiktokSettlement::getStatementDate));
    }

    private LocalDate parseDate(String val) {
        if (!StringUtils.hasText(val)) return null;
        try { return LocalDate.parse(val, DATE_FMT); } catch (Exception e) { return null; }
    }

    private BigDecimal parseBD(String val) {
        if (!StringUtils.hasText(val) || "/".equals(val)) return BigDecimal.ZERO;
        try { return new BigDecimal(val); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal abs(BigDecimal val) {
        return val != null ? val.abs() : BigDecimal.ZERO;
    }

    private int parseIntSafe(String val) {
        if (!StringUtils.hasText(val) || "/".equals(val)) return 0;
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }
}
