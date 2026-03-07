package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.common.config.DocumentPartyProperties;
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
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.common.context.ShopContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    private SalesDataMapper salesDataMapper;

    @Autowired
    private ShippingDataMapper shippingDataMapper;

    @Autowired
    private DocumentPartyProperties documentPartyProperties;

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
    public DocumentPo generatePo(PoGenerateRequest request) {
        log.info("开始生成PO采购订单，货件ID数量: {}", request.getShipmentIds().size());

        // 构建货件输入数据（后续可对接 FbaShipmentService 查询真实数据）
        List<ShipmentInput> shipmentInputs = buildShipmentInputs(request.getShipmentIds());

        // 调用生成器
        List<PoGenerateResult> results = PoGenerator.generate(shipmentInputs, 1);
        if (CollectionUtils.isEmpty(results)) {
            log.info("PO生成结果为空，无数据可持久化");
            return null;
        }

        // 获取当前店铺ID（数据隔离）
        Long shopId = ShopContext.requireShopId();

        // 取第一份PO结果进行持久化（一次请求通常生成一份PO）
        PoGenerateResult result = results.get(0);
        DocumentPo po = result.getPo();
        po.setShopId(shopId);

        // 持久化PO主表（幂等：重复单据号时返回已有记录）
        try {
            documentPoMapper.insert(po);
            log.info("PO主表持久化成功，编号: {}, ID: {}", po.getDocumentNo(), po.getId());

            // 持久化PO明细
            for (DocumentPoItem item : result.getItems()) {
                item.setPoId(po.getId());
                item.setShopId(shopId);
                documentPoItemMapper.insert(item);
            }
            log.info("PO明细持久化成功，明细数量: {}", result.getItems().size());
        } catch (DuplicateKeyException e) {
            log.info("PO单据号已存在，返回已有记录，编号: {}", po.getDocumentNo());
            LambdaQueryWrapper<DocumentPo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DocumentPo::getDocumentNo, po.getDocumentNo());
            po = documentPoMapper.selectOne(wrapper);
        }

        return po;
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
    public DocumentDn generateDn(DnGenerateRequest request) {
        log.info("开始生成DN送货单，锚点日期: {}, 货件ID数量: {}",
                request.getAnchorDate(), request.getShipmentIds().size());

        // 构建货件输入数据
        List<ShipmentInput> shipmentInputs = buildShipmentInputs(request.getShipmentIds());

        // 调用生成器
        List<DnGenerateResult> results = DnGenerator.generate(
                request.getAnchorDate(), shipmentInputs, 1);
        if (CollectionUtils.isEmpty(results)) {
            log.info("DN生成结果为空，无数据可持久化");
            return null;
        }

        // 取第一份DN结果进行持久化
        DnGenerateResult result = results.get(0);
        DocumentDn dn = result.getDn();

        // 获取当前店铺ID（数据隔离）
        Long shopId = ShopContext.requireShopId();
        dn.setShopId(shopId);

        // 持久化DN主表（幂等：重复单据号时返回已有记录）
        try {
            documentDnMapper.insert(dn);
            log.info("DN主表持久化成功，编号: {}, ID: {}", dn.getDocumentNo(), dn.getId());

            // 持久化DN明细
            for (DocumentDnItem item : result.getItems()) {
                item.setDnId(dn.getId());
                item.setShopId(shopId);
                documentDnItemMapper.insert(item);
            }
            log.info("DN明细持久化成功，明细数量: {}", result.getItems().size());
        } catch (DuplicateKeyException e) {
            log.info("DN单据号已存在，返回已有记录，编号: {}", dn.getDocumentNo());
            LambdaQueryWrapper<DocumentDn> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DocumentDn::getDocumentNo, dn.getDocumentNo());
            dn = documentDnMapper.selectOne(wrapper);
        }

        return dn;
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

        // 构建结算数据输入（后续可对接 SettlementImportDataMapper 查询真实数据）
        SettlementInput input = buildSettlementInput(request);

        // 调用生成器
        List<SettlementGenerateResult> results = SettlementGenerator.generate(input, 1);
        if (CollectionUtils.isEmpty(results)) {
            log.info("结算单生成结果为空，无数据可持久化");
            return List.of();
        }

        // 持久化所有结算单
        Long shopId = ShopContext.requireShopId();
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
                log.info("结算单单据号已存在，使用已有记录，编号: {}", settlement.getDocumentNo());
                LambdaQueryWrapper<DocumentSettlement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DocumentSettlement::getDocumentNo, settlement.getDocumentNo());
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

        // 调用INV生成器
        List<InvGenerateResult> invResults = InvGenerator.generate(settlementResults, 1, documentPartyProperties);

        // 持久化所有INV
        Long shopId = ShopContext.requireShopId();
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
                wrapper.eq(DocumentInv::getDocumentNo, inv.getDocumentNo());
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

        // 批量查询货件主表
        List<FbaShipment> shipments = fbaShipmentMapper.selectBatchIds(shipmentIds);
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
     * 根据结算单生成请求，通过配送数据日期范围关联销售数据，构建 SettlementInput
     *
     * <p>新逻辑：
     * 1. 根据货件ID查询配送数据（t_shipping_data），获取 ship_date 范围
     * 2. 在该日期范围内查询销售数据（t_sales_data），通过 orderId 关联
     * 3. 排除退款订单（transactionCategory = 'refund'），统计净销量
     * 4. 从 t_settlement_import_data 查询对应 MSKU 的单价
     * 5. 组合为 SettlementInput 传给生成器</p>
     *
     * @param request 结算单生成请求（包含周期和货件ID列表）
     * @return SettlementInput 结算数据输入
     * @author wanhua
     * 10:30 2026年03月07日
     */
    private SettlementInput buildSettlementInput(SettlementGenerateRequest request) {
        Long shopId = ShopContext.requireShopId();

        // 1. 根据货件ID查询配送数据，获取 orderId 集合和日期范围
        List<ShippingData> shippingList = List.of();
        if (!CollectionUtils.isEmpty(request.getShipmentIds())) {
            // 查询货件对应的 FbaShipmentItem，获取 MSKU 列表
            LambdaQueryWrapper<FbaShipmentItem> mskuWrapper = new LambdaQueryWrapper<>();
            mskuWrapper.in(FbaShipmentItem::getShipmentId, request.getShipmentIds());
            List<FbaShipmentItem> shipmentItems = fbaShipmentItemMapper.selectList(mskuWrapper);
            List<String> shipmentMskus = shipmentItems.stream()
                    .map(FbaShipmentItem::getMsku)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("选中货件涉及 {} 个MSKU", shipmentMskus.size());

            // 查询配送数据（按周期范围）
            LambdaQueryWrapper<ShippingData> shippingWrapper = new LambdaQueryWrapper<>();
            shippingWrapper.eq(ShippingData::getShopId, shopId)
                    .ge(ShippingData::getShipDate, request.getPeriodStart())
                    .le(ShippingData::getShipDate, request.getPeriodEnd());
            shippingList = shippingDataMapper.selectList(shippingWrapper);
            log.info("查询到配送数据 {} 条，周期: {} ~ {}",
                    shippingList.size(), request.getPeriodStart(), request.getPeriodEnd());

            if (CollectionUtils.isEmpty(shippingList)) {
                log.warn("周期内无配送数据，无法关联销售数据");
                return SettlementInput.builder()
                        .periodStart(request.getPeriodStart())
                        .periodEnd(request.getPeriodEnd())
                        .items(List.of())
                        .build();
            }

            // 2. 收集 orderId，关联查询销售数据（排除退款）
            Set<String> orderIds = shippingList.stream()
                    .map(ShippingData::getOrderId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toSet());

            LambdaQueryWrapper<SalesData> salesWrapper = new LambdaQueryWrapper<>();
            salesWrapper.eq(SalesData::getShopId, shopId)
                    .in(SalesData::getOrderId, orderIds)
                    .ne(SalesData::getTransactionCategory, "refund");
            List<SalesData> salesList = salesDataMapper.selectList(salesWrapper);
            log.info("关联查询到销售数据 {} 条（已排除退款）", salesList.size());

            // 3. 按站点+MSKU 统计净销量（income 累加，其他跳过）
            // key: siteCode, value: (msku -> 净销量)
            Map<String, Map<String, Integer>> netSalesMap = new HashMap<>();
            for (SalesData sales : salesList) {
                if (!"income".equals(sales.getTransactionCategory())) {
                    continue;
                }
                String siteCode = sales.getSiteCode();
                String sku = sales.getSku();
                int qty = sales.getQuantity() != null ? sales.getQuantity() : 0;
                netSalesMap.computeIfAbsent(siteCode, k -> new HashMap<>())
                        .merge(sku, qty, Integer::sum);
            }

            // 4. 从 t_settlement_import_data 查询 MSKU 单价（按货件MSKU过滤）
            LambdaQueryWrapper<SettlementImportData> priceWrapper = new LambdaQueryWrapper<>();
            priceWrapper.eq(SettlementImportData::getShopId, shopId)
                    .eq(SettlementImportData::getDelFlag, 0)
                    .le(SettlementImportData::getPeriodStart, request.getPeriodEnd())
                    .ge(SettlementImportData::getPeriodEnd, request.getPeriodStart());
            if (!CollectionUtils.isEmpty(shipmentMskus)) {
                priceWrapper.in(SettlementImportData::getMsku, shipmentMskus);
            }
            List<SettlementImportData> priceList = settlementImportDataMapper.selectList(priceWrapper);

            // 构建 MSKU → 单价映射（同一MSKU取第一条）
            Map<String, SettlementImportData> mskuPriceMap = new LinkedHashMap<>();
            for (SettlementImportData data : priceList) {
                mskuPriceMap.putIfAbsent(data.getMsku(), data);
            }
            log.info("查询到结算导入数据 {} 条（用于单价）", priceList.size());

            // 5. 组合：用销售净销量 + 推导单价 构建 SettlementDataItem
            List<SettlementInput.SettlementDataItem> items = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> siteEntry : netSalesMap.entrySet()) {
                String siteCode = siteEntry.getKey();
                for (Map.Entry<String, Integer> mskuEntry : siteEntry.getValue().entrySet()) {
                    String msku = mskuEntry.getKey();
                    int quantity = mskuEntry.getValue();
                    SettlementImportData priceData = mskuPriceMap.get(msku);
                    if (priceData == null) {
                        log.warn("MSKU {} 在结算导入数据中未找到单价，跳过", msku);
                        continue;
                    }
                    items.add(SettlementInput.SettlementDataItem.builder()
                            .siteCode(siteCode)
                            .msku(msku)
                            .currency(priceData.getCurrency())
                            .unitPrice(priceData.getUnitPrice())
                            .quantity(quantity)
                            .build());
                }
            }

            log.info("构建 SettlementInput 完成，共 {} 条明细", items.size());
            return SettlementInput.builder()
                    .periodStart(request.getPeriodStart())
                    .periodEnd(request.getPeriodEnd())
                    .items(items)
                    .build();
        }

        // 无货件ID时，回退到原有逻辑（按周期全量查询）
        log.warn("未指定货件ID，回退到全量结算数据查询");
        LambdaQueryWrapper<SettlementImportData> fallbackWrapper = new LambdaQueryWrapper<>();
        fallbackWrapper.eq(SettlementImportData::getShopId, shopId)
                .eq(SettlementImportData::getDelFlag, 0)
                .le(SettlementImportData::getPeriodStart, request.getPeriodEnd())
                .ge(SettlementImportData::getPeriodEnd, request.getPeriodStart());
        List<SettlementImportData> dataList = settlementImportDataMapper.selectList(fallbackWrapper);

        if (CollectionUtils.isEmpty(dataList)) {
            log.warn("未查询到结算导入数据，周期: {} ~ {}", request.getPeriodStart(), request.getPeriodEnd());
            return SettlementInput.builder()
                    .periodStart(request.getPeriodStart())
                    .periodEnd(request.getPeriodEnd())
                    .items(List.of())
                    .build();
        }

        List<SettlementInput.SettlementDataItem> fallbackItems = dataList.stream()
                .map(data -> SettlementInput.SettlementDataItem.builder()
                        .siteCode(data.getSiteCode())
                        .msku(data.getMsku())
                        .currency(data.getCurrency())
                        .unitPrice(data.getUnitPrice())
                        .quantity(data.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return SettlementInput.builder()
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .items(fallbackItems)
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
