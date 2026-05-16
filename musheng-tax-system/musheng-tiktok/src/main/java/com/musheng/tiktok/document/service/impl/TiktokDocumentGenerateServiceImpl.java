package com.musheng.tiktok.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.tiktok.document.utils.TiktokDocumentUtils;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.document.entity.*;
import com.musheng.tiktok.document.mapper.*;
import com.musheng.tiktok.document.service.TiktokDocumentGenerateService;
import com.musheng.tiktok.document.service.TiktokPartyConfigService;
import com.musheng.tiktok.product.entity.TiktokProduct;
import com.musheng.tiktok.product.mapper.TiktokProductMapper;
import com.musheng.tiktok.settlement.entity.TiktokSettlementOrder;
import com.musheng.tiktok.settlement.mapper.TiktokSettlementOrderMapper;
import com.musheng.tiktok.shipment.entity.TiktokShipment;
import com.musheng.tiktok.shipment.entity.TiktokShipmentItem;
import com.musheng.tiktok.shipment.mapper.TiktokShipmentItemMapper;
import com.musheng.tiktok.shipment.mapper.TiktokShipmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TK单据生成服务实现（对齐亚马逊多份生成逻辑）
 *
 * @author wanhua
 * 19:54 2026年05月15日
 */
@Service
@Slf4j
public class TiktokDocumentGenerateServiceImpl implements TiktokDocumentGenerateService {

    private static final int DN_PERIOD_DAYS = 21;
    private static final int SETTLEMENT_DAY_OF_MONTH = 5;
    private static final int AMOUNT_SCALE = 4;

    @Autowired
    private TiktokPartyConfigService partyConfigService;
    @Autowired
    private TiktokDocumentPoMapper poMapper;
    @Autowired
    private TiktokDocumentPoItemMapper poItemMapper;
    @Autowired
    private TiktokDocumentDnMapper dnMapper;
    @Autowired
    private TiktokDocumentDnItemMapper dnItemMapper;
    @Autowired
    private TiktokDocumentSettlementMapper docSettlementMapper;
    @Autowired
    private TiktokDocumentSettlementItemMapper docSettlementItemMapper;
    @Autowired
    private TiktokDocumentInvMapper invMapper;
    @Autowired
    private TiktokDocumentInvItemMapper invItemMapper;
    @Autowired
    private TiktokShipmentMapper shipmentMapper;
    @Autowired
    private TiktokShipmentItemMapper shipmentItemMapper;
    @Autowired
    private TiktokSettlementOrderMapper settlementOrderMapper;
    @Autowired
    private TiktokProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TiktokDocumentPo> generatePo(String siteCode, List<String> shipmentIds) {
        Long shopId = ShopContext.requireShopId();
        TiktokPartyConfig config = partyConfigService.getBySiteCode(siteCode);

        // 1. 查询所有货件并按 creationTime 排序
        List<TiktokShipment> shipments = loadShipments(shopId, siteCode, shipmentIds);
        shipments.sort(Comparator.comparing(s -> s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime()));

        // 2. 推算PO日期并分组
        Map<LocalDate, List<TiktokShipment>> grouped = new LinkedHashMap<>();
        for (TiktokShipment s : shipments) {
            LocalDateTime ct = s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime();
            LocalDate poDate = calculatePoDate(ct);
            grouped.computeIfAbsent(poDate, k -> new ArrayList<>()).add(s);
        }

        // 3. 每组生成一份PO
        List<TiktokDocumentPo> results = new ArrayList<>();
        int sequence = 1;
        for (Map.Entry<LocalDate, List<TiktokShipment>> entry : grouped.entrySet()) {
            LocalDate poDate = entry.getKey();
            List<TiktokShipment> group = entry.getValue();

            TiktokDocumentPo po = new TiktokDocumentPo();
            po.setShopId(shopId);
            po.setSiteCode(siteCode);
            po.setDocumentNo(TiktokDocumentUtils.generateDocNo(poDate, sequence));
            po.setPoDate(poDate);
            po.setBuyerName(config != null ? config.getBuyerName() : "");
            po.setBuyerAddress(config != null ? config.getBuyerAddress() : "");
            po.setSellerName(config != null ? config.getSellerName() : "");
            po.setShipmentCount(group.size());
            poMapper.insert(po);

            int totalQty = 0;
            int sortOrder = 0;
            for (TiktokShipment shipment : group) {
                List<TiktokShipmentItem> items = shipmentItemMapper.selectList(
                        new LambdaQueryWrapper<TiktokShipmentItem>()
                                .eq(TiktokShipmentItem::getShopId, shopId)
                                .eq(TiktokShipmentItem::getSiteCode, siteCode)
                                .eq(TiktokShipmentItem::getShipmentId, shipment.getShipmentId()));
                String address = buildAddress(shipment);
                for (TiktokShipmentItem item : items) {
                    TiktokDocumentPoItem poItem = new TiktokDocumentPoItem();
                    poItem.setShopId(shopId);
                    poItem.setSiteCode(siteCode);
                    poItem.setPoId(po.getId());
                    poItem.setShipmentNo(shipment.getShipmentId());
                    poItem.setMsku(item.getMsku());
                    poItem.setQuantity(item.getQuantityReceived() != null ? item.getQuantityReceived() : 0);
                    poItem.setFbtAddress(sortOrder == 0 ? address : "");
                    poItem.setSortOrder(++sortOrder);
                    poItemMapper.insert(poItem);
                    totalQty += poItem.getQuantity();
                }
            }
            po.setTotalQuantity(totalQty);
            poMapper.updateById(po);
            results.add(po);
            sequence++;
        }
        return results;
    }

    /**
     * 推算PO日期：创建时间所在周的下一个周二（当天是周二取当天），非工作日顺延
     */
    private LocalDate calculatePoDate(LocalDateTime createTime) {
        LocalDate createDate = createTime.toLocalDate();
        DayOfWeek dow = createDate.getDayOfWeek();
        LocalDate poDate;
        if (dow == DayOfWeek.TUESDAY) {
            poDate = createDate;
        } else if (dow.getValue() < DayOfWeek.TUESDAY.getValue()) {
            poDate = createDate.plusDays(DayOfWeek.TUESDAY.getValue() - dow.getValue());
        } else {
            poDate = createDate.plusDays(7 - dow.getValue() + DayOfWeek.TUESDAY.getValue());
        }
        return TiktokDocumentUtils.nearestWorkingDay(poDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TiktokDocumentDn> generateDn(String siteCode, List<String> shipmentIds, LocalDate anchorDate, Long poId) {
        Long shopId = ShopContext.requireShopId();
        TiktokPartyConfig config = partyConfigService.getBySiteCode(siteCode);
        if (anchorDate == null) {
            throw new com.musheng.common.exception.BusinessException("锚点日期不能为空");
        }

        // 当传入 poId 时，从 PO 明细提取对应货件
        if (poId != null) {
            List<TiktokDocumentPoItem> poItems = poItemMapper.selectList(
                    new LambdaQueryWrapper<TiktokDocumentPoItem>()
                            .eq(TiktokDocumentPoItem::getPoId, poId)
                            .eq(TiktokDocumentPoItem::getShopId, shopId));
            shipmentIds = poItems.stream()
                    .map(TiktokDocumentPoItem::getShipmentNo)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new com.musheng.common.exception.BusinessException("货件列表不能为空");
        }

        // 1. 查询货件并按 creationTime 排序
        List<TiktokShipment> shipments = loadShipments(shopId, siteCode, shipmentIds);
        shipments.sort(Comparator.comparing(s -> s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime()));

        // 2. 计算DN日期序列并分组
        LocalDate latestDate = shipments.stream()
                .map(s -> (s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime()).toLocalDate())
                .max(LocalDate::compareTo).orElse(anchorDate);
        LocalDate baseDate = latestDate.isAfter(anchorDate) ? latestDate : anchorDate;
        LocalDate rangeEnd = baseDate.plusDays(DN_PERIOD_DAYS);
        List<LocalDate> dnDates = TiktokDocumentUtils.calculateDnDates(anchorDate, rangeEnd);

        Map<LocalDate, List<TiktokShipment>> grouped = new TreeMap<>();
        for (TiktokShipment s : shipments) {
            LocalDate shipDate = (s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime()).toLocalDate();
            LocalDate targetDn = findTargetDnDate(dnDates, shipDate);
            if (targetDn != null) {
                grouped.computeIfAbsent(targetDn, k -> new ArrayList<>()).add(s);
            }
        }

        // 3. 每组生成一份DN
        List<TiktokDocumentDn> results = new ArrayList<>();
        int sequence = 1;
        List<LocalDate> sortedDnDates = new ArrayList<>(grouped.keySet());

        for (Map.Entry<LocalDate, List<TiktokShipment>> entry : grouped.entrySet()) {
            LocalDate dnDate = entry.getKey();
            List<TiktokShipment> group = entry.getValue();

            TiktokDocumentDn dn = new TiktokDocumentDn();
            dn.setShopId(shopId);
            dn.setSiteCode(siteCode);
            dn.setDocumentNo(TiktokDocumentUtils.generateDocNo(dnDate, sequence));
            dn.setDnDate(dnDate);
            dn.setSupplierName(config != null ? config.getSupplierName() : "");
            dn.setCustomerName(config != null ? config.getCustomerNameTc() : "");

            // 计算周期起止
            int idx = sortedDnDates.indexOf(dnDate);
            if (idx <= 0) {
                dn.setPeriodStart(group.stream()
                        .map(s -> (s.getCreationTime() != null ? s.getCreationTime() : s.getCreateTime()).toLocalDate())
                        .min(LocalDate::compareTo).orElse(anchorDate));
            } else {
                dn.setPeriodStart(sortedDnDates.get(idx - 1).plusDays(1));
            }
            dn.setPeriodEnd(dnDate);
            dnMapper.insert(dn);

            int totalQty = 0;
            int lineNo = 0;
            for (TiktokShipment shipment : group) {
                List<TiktokShipmentItem> items = shipmentItemMapper.selectList(
                        new LambdaQueryWrapper<TiktokShipmentItem>()
                                .eq(TiktokShipmentItem::getShopId, shopId)
                                .eq(TiktokShipmentItem::getSiteCode, siteCode)
                                .eq(TiktokShipmentItem::getShipmentId, shipment.getShipmentId()));
                for (TiktokShipmentItem item : items) {
                    TiktokDocumentDnItem dnItem = new TiktokDocumentDnItem();
                    dnItem.setShopId(shopId);
                    dnItem.setSiteCode(siteCode);
                    dnItem.setDnId(dn.getId());
                    dnItem.setLineNo(++lineNo);
                    dnItem.setMsku(item.getMsku());
                    dnItem.setQuantity(item.getQuantityReceived() != null ? item.getQuantityReceived() : 0);
                    dnItem.setShipmentNo(shipment.getShipmentId());
                    dnItemMapper.insert(dnItem);
                    totalQty += dnItem.getQuantity();
                }
            }
            dn.setTotalQuantity(totalQty);
            dnMapper.updateById(dn);
            results.add(dn);
            sequence++;
        }
        return results;
    }

    private LocalDate findTargetDnDate(List<LocalDate> dnDates, LocalDate shipmentDate) {
        if (dnDates.isEmpty()) return null;
        for (int i = 0; i < dnDates.size(); i++) {
            LocalDate cur = dnDates.get(i);
            if (i == 0) {
                if (!shipmentDate.isAfter(cur)) return cur;
            } else {
                if (shipmentDate.isAfter(dnDates.get(i - 1)) && !shipmentDate.isAfter(cur)) return cur;
            }
        }
        return dnDates.get(dnDates.size() - 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TiktokDocumentSettlement> generateSettlement(String siteCode, String quarter, BigDecimal costAmount) {
        Long shopId = ShopContext.requireShopId();
        if (costAmount == null || costAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.musheng.common.exception.BusinessException("请输入有效的采购成本");
        }
        TiktokPartyConfig config = partyConfigService.getBySiteCode(siteCode);

        // 解析季度
        LocalDate[] range = parseQuarter(quarter);
        LocalDate periodStart = range[0];
        LocalDate periodEnd = range[1];

        // 幂等：删除该季度已有的结算单和INV（覆盖旧数据）
        List<TiktokDocumentSettlement> existingSettlements = docSettlementMapper.selectList(
                new LambdaQueryWrapper<TiktokDocumentSettlement>()
                        .eq(TiktokDocumentSettlement::getShopId, shopId)
                        .eq(TiktokDocumentSettlement::getSiteCode, siteCode)
                        .ge(TiktokDocumentSettlement::getPeriodStart, periodStart)
                        .le(TiktokDocumentSettlement::getPeriodEnd, periodEnd));
        for (TiktokDocumentSettlement es : existingSettlements) {
            // 删除关联的INV及其明细
            List<TiktokDocumentInv> invs = invMapper.selectList(new LambdaQueryWrapper<TiktokDocumentInv>()
                    .eq(TiktokDocumentInv::getSettlementId, es.getId()));
            for (TiktokDocumentInv inv : invs) {
                invItemMapper.delete(new LambdaQueryWrapper<TiktokDocumentInvItem>().eq(TiktokDocumentInvItem::getInvId, inv.getId()));
                invMapper.deleteById(inv.getId());
            }
            // 删除结算单明细和主表
            docSettlementItemMapper.delete(new LambdaQueryWrapper<TiktokDocumentSettlementItem>()
                    .eq(TiktokDocumentSettlementItem::getSettlementId, es.getId()));
            docSettlementMapper.deleteById(es.getId());
        }

        // 查询该季度所有Order明细
        List<TiktokSettlementOrder> orders = settlementOrderMapper.selectList(
                new LambdaQueryWrapper<TiktokSettlementOrder>()
                        .eq(TiktokSettlementOrder::getShopId, shopId)
                        .eq(TiktokSettlementOrder::getSiteCode, siteCode)
                        .eq(TiktokSettlementOrder::getType, "Order")
                        .ge(TiktokSettlementOrder::getStatementDate, periodStart)
                        .le(TiktokSettlementOrder::getStatementDate, periodEnd));

        // 获取零售价（全季度统一查一次）
        Set<String> allMskus = orders.stream().map(TiktokSettlementOrder::getMsku)
                .filter(m -> m != null && !m.isEmpty()).collect(Collectors.toSet());
        Map<String, BigDecimal> priceMap = loadPriceMap(shopId, siteCode, allMskus);

        // 按自然月拆分生成
        List<TiktokDocumentSettlement> results = new ArrayList<>();
        int sequence = 1;
        LocalDate monthStart = periodStart.withDayOfMonth(1);

        while (!monthStart.isAfter(periodEnd)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            LocalDate subStart = monthStart.isBefore(periodStart) ? periodStart : monthStart;
            LocalDate subEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;

            // 结算日 = 下月5日（非工作日顺延）
            LocalDate nextMonth5th = monthStart.plusMonths(1).withDayOfMonth(SETTLEMENT_DAY_OF_MONTH);
            LocalDate settlementDate = TiktokDocumentUtils.nearestWorkingDay(nextMonth5th);

            // 过滤本月数据
            final LocalDate fStart = subStart;
            final LocalDate fEnd = subEnd;
            List<TiktokSettlementOrder> monthOrders = orders.stream()
                    .filter(o -> {
                        LocalDate d = o.getStatementDate();
                        if (d == null) return false;
                        return !d.isBefore(fStart) && !d.isAfter(fEnd);
                    }).collect(Collectors.toList());

            // 按MSKU聚合本月数据
            Map<String, Integer> mskuNetQty = aggregateMskuQty(monthOrders);

            if (mskuNetQty.isEmpty()) {
                monthStart = monthStart.plusMonths(1);
                continue;
            }

            // 校验价格
            List<String> missingPrice = mskuNetQty.keySet().stream()
                    .filter(m -> !priceMap.containsKey(m) || priceMap.get(m).compareTo(BigDecimal.ZERO) == 0)
                    .toList();
            if (!missingPrice.isEmpty()) {
                throw new com.musheng.common.exception.BusinessException("以下MSKU缺少零售价: " + String.join(", ", missingPrice));
            }

            // 计算本月加权零售价总和（用于分摊成本）
            BigDecimal monthWeightedSum = BigDecimal.ZERO;
            for (Map.Entry<String, Integer> e : mskuNetQty.entrySet()) {
                monthWeightedSum = monthWeightedSum.add(priceMap.get(e.getKey()).multiply(BigDecimal.valueOf(e.getValue())));
            }

            // 按月分摊成本：本月成本 = 总成本 × (本月加权/全季度加权)
            BigDecimal totalWeightedSum = calculateTotalWeightedSum(orders, priceMap);
            BigDecimal monthCost = totalWeightedSum.compareTo(BigDecimal.ZERO) != 0
                    ? costAmount.multiply(monthWeightedSum).divide(totalWeightedSum, AMOUNT_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 生成结算单
            TiktokDocumentSettlement settlement = buildSettlement(
                    shopId, siteCode, config, settlementDate, subStart, subEnd,
                    mskuNetQty, priceMap, monthWeightedSum, monthCost, sequence);
            results.add(settlement);

            // 同步生成INV
            generateInvInternal(settlement, config, shopId, siteCode);
            sequence++;
            monthStart = monthStart.plusMonths(1);
        }

        if (results.isEmpty()) {
            throw new com.musheng.common.exception.BusinessException("该季度(" + quarter + ")站点(" + siteCode + ")无有效结算数据");
        }
        return results;
    }

    private TiktokDocumentSettlement buildSettlement(Long shopId, String siteCode, TiktokPartyConfig config,
                                                     LocalDate settlementDate, LocalDate subStart, LocalDate subEnd,
                                                     Map<String, Integer> mskuNetQty, Map<String, BigDecimal> priceMap,
                                                     BigDecimal weightedSum, BigDecimal monthCost, int sequence) {
        TiktokDocumentSettlement settlement = new TiktokDocumentSettlement();
        settlement.setShopId(shopId);
        settlement.setSiteCode(siteCode);
        settlement.setDocumentNo(TiktokDocumentUtils.generateDocNo(settlementDate, sequence));
        settlement.setSettlementDate(settlementDate);
        settlement.setPeriodStart(subStart);
        settlement.setPeriodEnd(subEnd);
        settlement.setCurrency("USD");
        settlement.setExchangeRate(BigDecimal.ZERO);
        settlement.setBuyerName(config != null ? config.getBuyerName() : "");
        settlement.setBuyerAddress(config != null ? config.getBuyerAddress() : "");
        settlement.setSellerName(config != null ? config.getSellerName() : "");
        docSettlementMapper.insert(settlement);

        // MSKU字母升序
        List<String> sortedMskus = mskuNetQty.keySet().stream().sorted().collect(Collectors.toList());
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int lineNo = 0;

        for (String msku : sortedMskus) {
            int netQty = mskuNetQty.get(msku);
            BigDecimal price = priceMap.get(msku);
            BigDecimal unitPrice = weightedSum.compareTo(BigDecimal.ZERO) != 0
                    ? monthCost.multiply(price).divide(weightedSum, AMOUNT_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(netQty)).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

            TiktokDocumentSettlementItem item = new TiktokDocumentSettlementItem();
            item.setShopId(shopId);
            item.setSiteCode(siteCode);
            item.setSettlementId(settlement.getId());
            item.setLineNo(++lineNo);
            item.setMsku(msku);
            item.setCurrency("USD");
            item.setUnitPrice(unitPrice);
            item.setQuantity(netQty);
            item.setAmount(amount);
            docSettlementItemMapper.insert(item);
            totalQty += netQty;
            totalAmount = totalAmount.add(amount);
        }

        settlement.setTotalQuantity(totalQty);
        settlement.setTotalAmount(totalAmount);
        docSettlementMapper.updateById(settlement);
        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TiktokDocumentInv generateInv(Long settlementId) {
        Long shopId = ShopContext.requireShopId();
        TiktokDocumentSettlement settlement = docSettlementMapper.selectById(settlementId);
        if (settlement == null || !shopId.equals(settlement.getShopId())) {
            throw new com.musheng.common.exception.BusinessException("结算单不存在或无权操作");
        }
        TiktokPartyConfig config = partyConfigService.getBySiteCode(settlement.getSiteCode());
        return generateInvInternal(settlement, config, shopId, settlement.getSiteCode());
    }

    // ==================== 私有方法 ====================

    private TiktokDocumentInv generateInvInternal(TiktokDocumentSettlement settlement, TiktokPartyConfig config,
                                                   Long shopId, String siteCode) {
        // INV日期 = 结算日 + 1天
        LocalDate invDate = settlement.getSettlementDate().plusDays(1);
        // 如果是非工作日则顺延
        invDate = TiktokDocumentUtils.nearestWorkingDay(invDate);

        TiktokDocumentInv inv = new TiktokDocumentInv();
        inv.setShopId(shopId);
        inv.setSiteCode(siteCode);
        inv.setDocumentNo(TiktokDocumentUtils.generateDocNo(invDate, 1));
        inv.setInvDate(invDate);
        inv.setSettlementId(settlement.getId());
        inv.setCurrency(settlement.getCurrency());
        inv.setExchangeRate(settlement.getExchangeRate());
        inv.setSellerName(config != null ? config.getSellerName() : "");
        inv.setSellerAddress(config != null ? config.getSellerAddress() : "");
        inv.setSellerPhone(config != null ? config.getSellerPhone() : "");
        inv.setBuyerName(config != null ? config.getBuyerNameEn() : "");
        inv.setBuyerAddress(config != null ? config.getBuyerAddress() : "");
        inv.setBuyerPhone(config != null ? config.getBuyerPhone() : "");
        inv.setTotalQuantity(settlement.getTotalQuantity());
        inv.setTotalAmount(settlement.getTotalAmount());
        inv.setBankAccountName(config != null ? config.getBankAccountName() : "");
        inv.setBankAccountNumber(config != null ? config.getBankAccountNumber() : "");
        inv.setBankName(config != null ? config.getBankName() : "");
        inv.setBankAddress(config != null ? config.getBankAddress() : "");
        inv.setSwiftCode(config != null ? config.getSwiftCode() : "");
        invMapper.insert(inv);

        // 复制结算单明细到INV
        List<TiktokDocumentSettlementItem> items = docSettlementItemMapper.selectList(
                new LambdaQueryWrapper<TiktokDocumentSettlementItem>()
                        .eq(TiktokDocumentSettlementItem::getShopId, shopId)
                        .eq(TiktokDocumentSettlementItem::getSettlementId, settlement.getId()));
        for (TiktokDocumentSettlementItem si : items) {
            TiktokDocumentInvItem invItem = new TiktokDocumentInvItem();
            invItem.setShopId(shopId);
            invItem.setSiteCode(siteCode);
            invItem.setInvId(inv.getId());
            invItem.setLineNo(si.getLineNo());
            invItem.setMsku(si.getMsku());
            invItem.setQuantity(si.getQuantity());
            invItem.setUnitPrice(si.getUnitPrice());
            invItem.setAmount(si.getAmount());
            invItemMapper.insert(invItem);
        }
        return inv;
    }

    private List<TiktokShipment> loadShipments(Long shopId, String siteCode, List<String> shipmentIds) {
        List<TiktokShipment> result = new ArrayList<>();
        for (String sid : shipmentIds) {
            TiktokShipment s = shipmentMapper.selectOne(new LambdaQueryWrapper<TiktokShipment>()
                    .eq(TiktokShipment::getShopId, shopId)
                    .eq(TiktokShipment::getSiteCode, siteCode)
                    .eq(TiktokShipment::getShipmentId, sid));
            if (s == null) {
                throw new com.musheng.common.exception.BusinessException("货件不存在或无权操作: " + sid);
            }
            result.add(s);
        }
        return result;
    }

    private String buildAddress(TiktokShipment s) {
        StringBuilder sb = new StringBuilder();
        if (s.getStreetAddress() != null) sb.append(s.getStreetAddress());
        if (s.getCity() != null) sb.append(", ").append(s.getCity());
        if (s.getState() != null) sb.append(", ").append(s.getState());
        if (s.getPostalCode() != null) sb.append(" ").append(s.getPostalCode());
        return sb.toString();
    }

    private Map<String, BigDecimal> loadPriceMap(Long shopId, String siteCode, Set<String> mskus) {
        if (mskus.isEmpty()) return Map.of();
        List<TiktokProduct> products = productMapper.selectList(new LambdaQueryWrapper<TiktokProduct>()
                .eq(TiktokProduct::getShopId, shopId)
                .eq(TiktokProduct::getSiteCode, siteCode)
                .in(TiktokProduct::getMsku, mskus));
        return products.stream().collect(Collectors.toMap(
                TiktokProduct::getMsku,
                p -> p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO,
                (a, b) -> a));
    }

    private Map<String, Integer> aggregateMskuQty(List<TiktokSettlementOrder> orders) {
        Map<String, int[]> raw = new LinkedHashMap<>();
        for (TiktokSettlementOrder o : orders) {
            String msku = o.getMsku();
            if (msku == null || msku.isEmpty()) continue;
            BigDecimal subtotal = o.getSubtotalAfterDiscount() != null ? o.getSubtotalAfterDiscount() : BigDecimal.ZERO;
            BigDecimal refund = o.getRefundAfterDiscount() != null ? o.getRefundAfterDiscount() : BigDecimal.ZERO;
            // 混合行跳过
            if (subtotal.compareTo(BigDecimal.ZERO) > 0 && refund.compareTo(BigDecimal.ZERO) < 0) continue;
            int q = o.getQuantity() != null ? o.getQuantity() : 0;
            raw.computeIfAbsent(msku, k -> new int[]{0, 0});
            int[] qty = raw.get(msku);
            if (subtotal.compareTo(BigDecimal.ZERO) == 0 && refund.compareTo(BigDecimal.ZERO) < 0) {
                qty[1] += q;
            } else {
                qty[0] += q;
            }
        }
        // 转为净数量，过滤掉<=0的
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : raw.entrySet()) {
            int net = e.getValue()[0] - e.getValue()[1];
            if (net > 0) result.put(e.getKey(), net);
        }
        return result;
    }

    private BigDecimal calculateTotalWeightedSum(List<TiktokSettlementOrder> allOrders, Map<String, BigDecimal> priceMap) {
        Map<String, Integer> totalQty = aggregateMskuQty(allOrders);
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : totalQty.entrySet()) {
            BigDecimal price = priceMap.get(e.getKey());
            if (price != null) {
                sum = sum.add(price.multiply(BigDecimal.valueOf(e.getValue())));
            }
        }
        return sum;
    }

    private LocalDate[] parseQuarter(String quarter) {
        String[] parts = quarter.split("-Q");
        int year = Integer.parseInt(parts[0]);
        int q = Integer.parseInt(parts[1]);
        int startMonth = (q - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{start, end};
    }
}
