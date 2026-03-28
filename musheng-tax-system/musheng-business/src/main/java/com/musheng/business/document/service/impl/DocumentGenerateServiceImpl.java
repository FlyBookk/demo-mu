package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.service.DocumentPartyConfigService;
import com.musheng.business.document.dto.DnGenerateRequest;
import com.musheng.business.document.dto.PoGenerateRequest;
import com.musheng.business.document.dto.SettlementGenerateRequest;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.generator.*;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.DocumentGenerateService;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.mapper.FbaShipmentItemMapper;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;

import com.musheng.business.settlement.derivation.service.SalesDataAggregator;
import com.musheng.business.settlement.derivation.vo.AggregationResult;
import com.musheng.common.context.ShopContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

import static com.musheng.common.result.ErrorCode.FORBIDDEN;

/**
 * 单据生成服务实现类
 *
 * <p>负责调用各生成器（PoGenerator、DnGenerator、SettlementGenerator、InvGenerator）
 * 并将生成结果持久化到数据库。所有方法使用事务保证一致性。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class DocumentGenerateServiceImpl implements DocumentGenerateService {

    @Autowired
    private DocumentPoMapper documentPoMapper;

    @Autowired
    private DocumentPoItemMapper documentPoItemMapper;

    @Autowired
    private DocumentDnMapper documentDnMapper;

    @Autowired
    private DocumentDnItemMapper documentDnItemMapper;

    @Autowired
    private DocumentSettlementMapper documentSettlementMapper;

    @Autowired
    private DocumentSettlementItemMapper documentSettlementItemMapper;

    @Autowired
    private DocumentInvMapper documentInvMapper;

    @Autowired
    private DocumentInvItemMapper documentInvItemMapper;

    @Autowired
    private FbaShipmentMapper fbaShipmentMapper;

    @Autowired
    private FbaShipmentItemMapper fbaShipmentItemMapper;

    @Autowired
    private SettlementImportDataMapper settlementImportDataMapper;

    @Autowired
    private SalesDataAggregator salesDataAggregator;



    @Autowired
    private DocumentPartyConfigService documentPartyConfigService;

    @Autowired
    private MarketplaceMapper marketplaceMapper;
    /**
     * 根据选定的FBA货件生成PO采购订单
     *
     * <p>流程：构建 ShipmentInput → 调用 PoGenerator → 持久化主表和明细。
     * 当前阶段使用简单的 ShipmentInput 构建，后续可对接 FbaShipmentService。</p>
     *
     * @param request PO生成请求（包含货件ID列表）
     * @return 持久化后的PO实体，无数据时返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DuplicateKeyException.class)
    public List<DocumentPo> generatePo(PoGenerateRequest request) {
        log.info("开始生成PO采购订单，货件ID数量: {}", request.getShipmentIds().size());

        List<ShipmentInput> shipmentInputs = buildShipmentInputs(request.getShipmentIds());

        String siteCode = request.getSiteCode();
        DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(siteCode);

        List<PoGenerateResult> results = PoGenerator.generate(shipmentInputs, 1, party);
        if (CollectionUtils.isEmpty(results)) {
            log.info("PO生成结果为空，无数据可持久化");
            return List.of();
        }

        Long shopId = ShopContext.requireShopId();
        log.info("[GeneratePO] 当前店铺: shopId={}, 货件数量: {}, 站点: {}, 生成PO数量: {}",
                shopId, request.getShipmentIds().size(), siteCode, results.size());

        List<DocumentPo> savedPos = new ArrayList<>();
        int sequence = 1;
        for (PoGenerateResult result : results) {
            DocumentPo po = result.getPo();
            po.setShopId(shopId);
            po.setSiteCode(siteCode);
            // 重新生成编号，确保序号递增
            po.setDocumentNo(com.musheng.business.document.utils.DocumentNumberCalculator.generate(po.getPoDate(), sequence));
            try {
                documentPoMapper.insert(po);
                log.info("PO主表持久化成功，编号: {}, ID: {}", po.getDocumentNo(), po.getId());
                for (DocumentPoItem item : result.getItems()) {
                    item.setPoId(po.getId());
                    item.setShopId(shopId);
                    documentPoItemMapper.insert(item);
                }
                log.info("PO明细持久化成功，明细数量: {}", result.getItems().size());
            } catch (DuplicateKeyException e) {
                log.info("PO单据号已存在，返回已有记录，编号: {}", po.getDocumentNo());
                LambdaQueryWrapper<DocumentPo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DocumentPo::getDocumentNo, po.getDocumentNo())
                       .eq(DocumentPo::getShopId, shopId);
                po = documentPoMapper.selectOne(wrapper);
            }
            savedPos.add(po);
            sequence++;
        }

        return savedPos;
    }

    /**
     * 根据DN周期批量生成送货单
     *
     * <p>流程：构建 ShipmentInput → 调用 DnGenerator → 持久化主表和明细。</p>
     *
     * @param request DN生成请求（包含锚点日期和货件ID列表）
     * @return 持久化后的DN实体，无数据时返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DuplicateKeyException.class)
    public List<DocumentDn> generateDn(DnGenerateRequest request) {
        // 当传入 poId 时，从 PO 明细提取对应货件 ID
        List<Long> shipmentIds = request.getShipmentIds();
        if (request.getPoId() != null) {
            List<DocumentPoItem> poItems = documentPoItemMapper.selectList(
                    new LambdaQueryWrapper<DocumentPoItem>()
                            .eq(DocumentPoItem::getPoId, request.getPoId()));
            List<String> shipmentNos = poItems.stream()
                    .map(DocumentPoItem::getShipmentNo)
                    .distinct()
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(shipmentNos)) {
                List<FbaShipment> shipments = fbaShipmentMapper.selectList(
                        new LambdaQueryWrapper<FbaShipment>()
                                .in(FbaShipment::getShipmentId, shipmentNos));
                shipmentIds = shipments.stream().map(FbaShipment::getId).collect(Collectors.toList());
            }
        }
        if (CollectionUtils.isEmpty(shipmentIds)) {
            throw new IllegalArgumentException("货件ID列表不能为空");
        }
        log.info("开始生成DN送货单，锚点日期: {}, 货件ID数量: {}", request.getAnchorDate(), shipmentIds.size());

        List<ShipmentInput> shipmentInputs = buildShipmentInputs(shipmentIds);

        String siteCode = request.getSiteCode();
        DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(siteCode);

        // 校验锚点日期不能早于所有货件的最晚PO日期
        if (!CollectionUtils.isEmpty(shipmentInputs)) {
            LocalDate latestPoDate = shipmentInputs.stream()
                    .map(s -> PoGenerator.calculatePoDate(s.getCreateTime()))
                    .max(LocalDate::compareTo)
                    .orElse(null);
            if (latestPoDate != null && request.getAnchorDate().isBefore(latestPoDate)) {
                throw new IllegalArgumentException(
                        "DN锚点日期（" + request.getAnchorDate() + "）不能早于货件对应的最晚PO日期（" + latestPoDate + "）");
            }
        }

        List<DnGenerateResult> results = DnGenerator.generate(
                request.getAnchorDate(), shipmentInputs, 1, party);
        if (CollectionUtils.isEmpty(results)) {
            log.info("DN生成结果为空，无数据可持久化");
            return List.of();
        }

        Long shopId = ShopContext.requireShopId();
        log.info("[GenerateDN] 当前店铺: shopId={}, 锚点日期: {}, 货件数量: {}, 站点: {}, 生成DN数量: {}",
                shopId, request.getAnchorDate(), request.getShipmentIds().size(), siteCode, results.size());

        List<DocumentDn> savedDns = new ArrayList<>();
        for (DnGenerateResult result : results) {
            DocumentDn dn = result.getDn();
            dn.setSiteCode(siteCode);
            dn.setShopId(shopId);
            try {
                documentDnMapper.insert(dn);
                log.info("DN主表持久化成功，编号: {}, ID: {}", dn.getDocumentNo(), dn.getId());
                for (DocumentDnItem item : result.getItems()) {
                    item.setDnId(dn.getId());
                    item.setShopId(shopId);
                    documentDnItemMapper.insert(item);
                }
                log.info("DN明细持久化成功，明细数量: {}", result.getItems().size());
            } catch (DuplicateKeyException e) {
                log.info("DN单据号已存在，返回已有记录，编号: {}", dn.getDocumentNo());
                LambdaQueryWrapper<DocumentDn> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DocumentDn::getDocumentNo, dn.getDocumentNo())
                       .eq(DocumentDn::getShopId, shopId);
                dn = documentDnMapper.selectOne(wrapper);
            }
            savedDns.add(dn);
        }

        return savedDns;
    }

    /**
     * 根据结算周期生成4份结算单（按站点拆分）
     *
     * <p>流程：构建 SettlementInput → 调用 SettlementGenerator → 持久化4份结算单。</p>
     *
     * @param request 结算单生成请求（包含周期起止日期）
     * @return 持久化后的结算单列表（通常4份）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DuplicateKeyException.class)
    public List<DocumentSettlement> generateSettlements(SettlementGenerateRequest request) {
        log.info("开始生成结算单，周期: {} ~ {}", request.getPeriodStart(), request.getPeriodEnd());

        // 构建结算数据输入
        SettlementInput input = buildSettlementInput(request);

        // 按站点分别查询交易方配置，确保每个站点使用正确的买卖方信息
        Map<String, DocumentPartyConfig> partyMap = new LinkedHashMap<>();
        for (String siteCode : request.getSiteCodes()) {
            try {
                DocumentPartyConfig party = documentPartyConfigService.getBySiteCode(siteCode);
                partyMap.put(siteCode, party);
            } catch (Exception e) {
                log.warn("站点 {} 未配置交易方信息，使用默认配置", siteCode);
                // 使用默认配置兜底
                DocumentPartyConfig defaultParty = new DocumentPartyConfig();
                defaultParty.setBuyerName("东莞市慕声商贸有限公司");
                defaultParty.setBuyerAddress("广东省东莞市");
                defaultParty.setSellerName("Hong Kong Andeo Group Limited");
                partyMap.put(siteCode, defaultParty);
            }
        }

        // 调用生成器（按站点使用各自的交易方配置，sequence 全局递增确保单据号唯一）
        List<SettlementGenerateResult> allResults = SettlementGenerator.generate(input, 1, partyMap);
        if (CollectionUtils.isEmpty(allResults)) {
            log.info("结算单生成结果为空，无数据可持久化");
            return List.of();
        }

        // 过滤掉空结算单（total_quantity=0），避免无意义数据入库
        List<SettlementGenerateResult> results = allResults.stream()
                .filter(r -> r.getSettlement().getTotalQuantity() > 0)
                .collect(Collectors.toList());
        log.info("结算单生成 {} 份，过滤空结算单后剩余 {} 份（有数据）",
                allResults.size(), results.size());
        if (CollectionUtils.isEmpty(results)) {
            log.warn("所有站点结算单均为空，请检查是否已导入对应站点的结算数据");
            return List.of();
        }

        // 持久化所有结算单
        Long shopId = ShopContext.requireShopId();
        log.info("[GenerateSettlement] 当前店铺: shopId={}, 周期: {} ~ {}, 有效结算单: {}份",
                shopId, request.getPeriodStart(), request.getPeriodEnd(), results.size());
        List<DocumentSettlement> settlements = new ArrayList<>();
        for (SettlementGenerateResult result : results) {
            DocumentSettlement settlement = result.getSettlement();
            settlement.setShopId(shopId);

            // 持久化结算单主表（幂等：重复单据号时使用已有记录）
            try {
                documentSettlementMapper.insert(settlement);
                log.info("结算单主表持久化成功，编号: {}, 站点: {}",
                        settlement.getDocumentNo(), settlement.getSiteCode());

                // 持久化结算单明细
                for (DocumentSettlementItem item : result.getItems()) {
                    item.setSettlementId(settlement.getId());
                    item.setShopId(shopId);
                    documentSettlementItemMapper.insert(item);
                }
            } catch (DuplicateKeyException e) {
                log.error("结算单单据号已存在，使用已有记录，编号: {}", settlement.getDocumentNo(), e);
                LambdaQueryWrapper<DocumentSettlement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DocumentSettlement::getDocumentNo, settlement.getDocumentNo())
                       .eq(DocumentSettlement::getShopId, shopId);
                settlement = documentSettlementMapper.selectOne(wrapper);
            }

            settlements.add(settlement);
        }
        log.info("结算单生成完成，共 {} 份", settlements.size());

        return settlements;
    }

    /**
     * 根据结算单自动生成对应的4份INV
     *
     * <p>流程：查询结算单数据 → 构建 SettlementGenerateResult →
     * 调用 InvGenerator → 持久化4份INV。</p>
     *
     * @param settlementIds 结算单ID列表
     * @return 持久化后的INV列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DuplicateKeyException.class)
    public List<DocumentInv> generateInvoices(List<Long> settlementIds) {
        if (CollectionUtils.isEmpty(settlementIds)) {
            log.info("结算单ID列表为空，无法生成INV");
            return List.of();
        }
        log.info("开始生成INV发票，结算单ID数量: {}", settlementIds.size());

        // 查询结算单数据并构建生成器输入
        List<SettlementGenerateResult> settlementResults = new ArrayList<>();
        for (Long settlementId : settlementIds) {
            DocumentSettlement settlement = documentSettlementMapper.selectById(settlementId);
            if (settlement == null) {
                log.warn("结算单不存在，ID: {}", settlementId);
                continue;
            }
            // 校验结算单归属当前店铺，防止越权访问
            Long currentShopId = ShopContext.requireShopId();
            log.info("[generateInvoices] settlementId={}, 请求shopId={}, 数据库shopId={}", settlementId, currentShopId, settlement.getShopId());
            if (!currentShopId.equals(settlement.getShopId())) {
                log.warn("[generateInvoices] 权限校验失败: 请求shopId={} != 数据库shopId={}, settlementId={}", currentShopId, settlement.getShopId(), settlementId);
                throw new com.musheng.common.exception.BusinessException(
                        FORBIDDEN, "无权访问该数据");
            }

            // 查询结算单明细
            LambdaQueryWrapper<DocumentSettlementItem> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DocumentSettlementItem::getSettlementId, settlementId);
            List<DocumentSettlementItem> items = documentSettlementItemMapper.selectList(queryWrapper);

            settlementResults.add(SettlementGenerateResult.builder()
                    .settlement(settlement)
                    .items(items)
                    .build());
        }

        if (CollectionUtils.isEmpty(settlementResults)) {
            log.info("未查询到有效结算单数据，无法生成INV");
            return List.of();
        }

        // 按结算单的站点分别查询交易方配置，确保每份 INV 使用正确的买卖方信息
        Long shopId = ShopContext.requireShopId();
        Map<String, DocumentPartyConfig> partyMap = new LinkedHashMap<>();
        for (SettlementGenerateResult sr : settlementResults) {
            String siteCode = sr.getSettlement().getSiteCode();
            if (!partyMap.containsKey(siteCode)) {
                try {
                    partyMap.put(siteCode, documentPartyConfigService.getBySiteCode(siteCode));
                } catch (Exception e) {
                    log.warn("站点 {} 未配置交易方信息，使用默认配置", siteCode);
                    DocumentPartyConfig defaultParty = new DocumentPartyConfig();
                    defaultParty.setSellerName("Hong Kong Andeo Group Limited");
                    defaultParty.setBuyerName("东莞市慕声商贸有限公司");
                    partyMap.put(siteCode, defaultParty);
                }
            }
        }
        List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, partyMap);

        // 持久化所有INV
        log.info("[GenerateINV] 当前店铺: shopId={}, 结算单数量: {}, 生成INV数量: {}",
                shopId, settlementIds.size(), invResults.size());
        List<DocumentInv> invoices = new ArrayList<>();
        for (InvGenerateResult invResult : invResults) {
            DocumentInv inv = invResult.getInv();
            inv.setSettlementId(findSettlementId(settlementResults, invResult));
            inv.setShopId(shopId);

            // 持久化INV主表（幂等：重复单据号时使用已有记录）
            try {
                documentInvMapper.insert(inv);
                log.info("INV主表持久化成功，编号: {}, 站点: {}",
                        inv.getDocumentNo(), inv.getSiteCode());

                // 持久化INV明细
                for (DocumentInvItem item : invResult.getItems()) {
                    item.setInvId(inv.getId());
                    item.setShopId(shopId);
                    documentInvItemMapper.insert(item);
                }
            } catch (DuplicateKeyException e) {
                log.info("INV单据号已存在，使用已有记录，编号: {}", inv.getDocumentNo());
                LambdaQueryWrapper<DocumentInv> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DocumentInv::getDocumentNo, inv.getDocumentNo())
                       .eq(DocumentInv::getShopId, shopId);
                inv = documentInvMapper.selectOne(wrapper);
            }

            invoices.add(inv);
        }
        log.info("INV生成完成，共 {} 份", invoices.size());

        return invoices;
    }

    /**
     * 根据货件ID列表查询FBA货件数据并构建 ShipmentInput
     *
     * <p>从 t_fba_shipment 和 t_fba_shipment_item 查询真实数据，
     * 映射为生成器所需的 ShipmentInput 结构。</p>
     *
     * @param shipmentIds 货件ID列表
     * @return ShipmentInput 列表
     * @author wanhua
     * 10:30 2026年03月01日
     */
    private List<ShipmentInput> buildShipmentInputs(List<Long> shipmentIds) {
        if (CollectionUtils.isEmpty(shipmentIds)) {
            log.warn("货件ID列表为空，无法构建ShipmentInput");
            return List.of();
        }

        // 批量查询货件主表（强制 shopId 隔离，防止跨店铺访问）
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<FbaShipment> shipmentWrapper = new LambdaQueryWrapper<>();
        shipmentWrapper.in(FbaShipment::getId, shipmentIds)
                .eq(FbaShipment::getShopId, shopId);
        List<FbaShipment> shipments = fbaShipmentMapper.selectList(shipmentWrapper);
        if (CollectionUtils.isEmpty(shipments)) {
            log.warn("未查询到货件数据，ID列表: {}", shipmentIds);
            return List.of();
        }

        return shipments.stream().map(shipment -> {
            // 查询该货件的MSKU明细
            LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(FbaShipmentItem::getShipmentId, shipment.getId());
            List<FbaShipmentItem> items = fbaShipmentItemMapper.selectList(itemWrapper);

            // 映射明细为 MskuItem
            List<ShipmentInput.MskuItem> mskuItems = items.stream()
                    .map(item -> ShipmentInput.MskuItem.builder()
                            .msku(item.getMsku())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            return ShipmentInput.builder()
                    .shipmentNo(shipment.getShipmentId())
                    .shipmentName(shipment.getShipmentName())
                    .createTime(shipment.getCreatedDate())
                    .streetAddress(shipment.getStreetAddress())
                    .city(shipment.getCity())
                    .stateProvince(shipment.getState())
                    .postalCode(shipment.getPostalCode())
                    .country(shipment.getCountry())
                    .items(mskuItems)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 根据结算周期从推导数据构建 SettlementInput
     *
     * <p>从 t_settlement_import_data 查询对应周期的推导数据，
     * 映射为 SettlementInput 传给生成器。</p>
     *
     * @param request 结算单生成请求（包含周期起止日期）
     * @return SettlementInput 结算数据输入
     * @author wanhua
     * 16:58 2026年03月21日
     */
    private SettlementInput buildSettlementInput(SettlementGenerateRequest request) {
        Long shopId = ShopContext.requireShopId();
        log.info("[BuildSettlementInput] 当前店铺: shopId={}, 周期: {} ~ {}, 站点: {}",
                shopId, request.getPeriodStart(), request.getPeriodEnd(), request.getSiteCodes());

        // Step 1：从推导结果读取季度单价（MSKU → unitPrice）
        LambdaQueryWrapper<SettlementImportData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementImportData::getShopId, shopId)
                .eq(SettlementImportData::getDelFlag, 0)
                .le(SettlementImportData::getPeriodStart, request.getPeriodEnd())
                .ge(SettlementImportData::getPeriodEnd, request.getPeriodStart())
                .in(SettlementImportData::getSiteCode, request.getSiteCodes());
        List<SettlementImportData> derivedList = settlementImportDataMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(derivedList)) {
            log.warn("未查询到推导数据，周期: {} ~ {}", request.getPeriodStart(), request.getPeriodEnd());
            return SettlementInput.builder()
                    .periodStart(request.getPeriodStart())
                    .periodEnd(request.getPeriodEnd())
                    .items(List.of())
                    .build();
        }

        // 构建 siteCode → (msku → unitPrice) 映射
        Map<String, Map<String, BigDecimal>> siteUnitPriceMap = new LinkedHashMap<>();
        Map<String, String> siteCurrencyFromDerived = new LinkedHashMap<>();
        for (SettlementImportData d : derivedList) {
            siteUnitPriceMap
                    .computeIfAbsent(d.getSiteCode(), k -> new LinkedHashMap<>())
                    .put(d.getMsku(), d.getUnitPrice());
            if (d.getCurrency() != null) {
                siteCurrencyFromDerived.put(d.getSiteCode(), d.getCurrency());
            }
        }

        // Step 2：按月重新聚合销售数量，用季度单价计算月度金额
        List<SettlementInput.SettlementDataItem> items = new ArrayList<>();
        LocalDate periodStart = request.getPeriodStart();
        LocalDate periodEnd = request.getPeriodEnd();

        LocalDate monthStart = periodStart.withDayOfMonth(1);
        while (!monthStart.isAfter(periodEnd)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            LocalDate subStart = monthStart.isBefore(periodStart) ? periodStart : monthStart;
            LocalDate subEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;

            // 按月聚合净销售数量（shopId + siteCode + 时间，三者缺一不可）
            AggregationResult monthAgg = salesDataAggregator.aggregateNetSales(
                    shopId, subStart, subEnd, request.getSiteCodes());

            for (String siteCode : request.getSiteCodes()) {
                Map<String, BigDecimal> unitPriceMap = siteUnitPriceMap.getOrDefault(siteCode, Collections.emptyMap());
                Map<String, Integer> monthQty = monthAgg.getNetSalesMap().getOrDefault(siteCode, Collections.emptyMap());
                String currency = siteCurrencyFromDerived.get(siteCode);

                for (Map.Entry<String, Integer> entry : monthQty.entrySet()) {
                    String msku = entry.getKey();
                    int qty = entry.getValue();
                    if (qty <= 0) continue;

                    BigDecimal unitPrice = unitPriceMap.get(msku);
                    if (unitPrice == null) {
                        // 推导结果中没有该 MSKU 的单价，跳过（可能是新增 MSKU）
                        log.warn("MSKU {} 在推导结果中无单价，站点: {}, 月份: {}", msku, siteCode, subStart);
                        continue;
                    }

                    items.add(SettlementInput.SettlementDataItem.builder()
                            .transactionDate(subStart)   // 月份起始日，供 SettlementGenerator 按月过滤
                            .siteCode(siteCode)
                            .msku(msku)
                            .currency(currency)
                            .unitPrice(unitPrice)
                            .quantity(qty)
                            .build());
                }
            }

            monthStart = monthStart.plusMonths(1);
        }

        // Step 3：构建站点货币映射
        LambdaQueryWrapper<Marketplace> mWrapper = new LambdaQueryWrapper<>();
        mWrapper.eq(Marketplace::getStatus, 1);
        List<Marketplace> marketplaces = marketplaceMapper.selectList(mWrapper);
        Map<String, String> siteCurrencyMap = new LinkedHashMap<>();
        for (Marketplace m : marketplaces) {
            if (m.getSiteCode() != null && m.getCurrencyCode() != null) {
                siteCurrencyMap.put(m.getSiteCode(), m.getCurrencyCode());
            }
        }

        log.info("构建 SettlementInput 完成，共 {} 条月度明细，站点货币映射: {}", items.size(), siteCurrencyMap);
        return SettlementInput.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .selectedSiteCodes(request.getSiteCodes())
                .siteCurrencyMap(siteCurrencyMap)
                .items(items)
                .build();
    }

    /**
     * 查找INV对应的结算单ID
     *
     * @param settlementResults 结算单结果列表
     * @param invResult INV生成结果
     * @return 对应的结算单ID
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private Long findSettlementId(List<SettlementGenerateResult> settlementResults,
                                   InvGenerateResult invResult) {
        String invSiteSequence = invResult.getInv().getSiteSequence();
        for (SettlementGenerateResult sr : settlementResults) {
            if (sr.getSettlement().getSiteSequence() != null
                    && sr.getSettlement().getSiteSequence().equals(invSiteSequence)) {
                return sr.getSettlement().getId();
            }
        }
        // 按顺序匹配
        int index = Math.min(settlementResults.size() - 1,
                Math.max(0, settlementResults.indexOf(invResult)));
        return settlementResults.isEmpty() ? null : settlementResults.get(0).getSettlement().getId();
    }


}
