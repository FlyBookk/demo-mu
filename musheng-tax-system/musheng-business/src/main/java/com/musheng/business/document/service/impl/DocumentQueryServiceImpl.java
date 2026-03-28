package com.musheng.business.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.document.dto.DocumentQueryRequest;
import com.musheng.business.document.dto.MskuQueryRequest;
import com.musheng.business.document.dto.MskuUpdateRequest;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.enums.DocumentType;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.DocumentQueryService;
import com.musheng.business.document.vo.*;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.common.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 单据查询服务实现类
 *
 * <p>提供分页查询、4种单据详情查询、按结算周期查看关联单据、货件关联关系查询。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class DocumentQueryServiceImpl implements DocumentQueryService {

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
    private SettlementImportDataMapper settlementImportDataMapper;

    /**
     * 分页查询单据列表（支持按类型、编号、日期范围筛选）
     *
     * @param request 查询请求参数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public PageResult<DocumentListVO> listDocuments(DocumentQueryRequest request) {
        log.info("分页查询单据列表, 类型={}, 编号={}", request.getDocumentType(), request.getDocumentNo());

        DocumentType type = StringUtils.hasText(request.getDocumentType())
                ? DocumentType.fromCode(request.getDocumentType())
                : null;

        if (type == null && StringUtils.hasText(request.getDocumentType())) {
            log.warn("未知的单据类型: {}", request.getDocumentType());
            return PageResult.of(Collections.emptyList(), 0, request.getPageNum(), request.getPageSize());
        }

        if (type == null) {
            // 未指定类型时，查询全部类型合并返回
            return queryAllTypesList(request);
        }

        return switch (type) {
            case PO -> queryPoList(request);
            case DN -> queryDnList(request);
            case SETTLEMENT -> querySettlementList(request);
            case INV -> queryInvList(request);
        };
    }

    /**
     * PO详情（含货件分组明细）
     *
     * @param id PO主键
     * @return PO详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public PoVO getPoDetail(Long id) {
        log.info("查询PO详情, id={}", id);
        DocumentPo po = documentPoMapper.selectById(id);
        if (po == null) {
            log.warn("PO不存在, id={}", id);
            return null;
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(po.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        PoVO vo = BeanUtil.toBean(po, PoVO.class);

        // 查询明细，按 sortOrder 排序
        LambdaQueryWrapper<DocumentPoItem> itemQuery = new LambdaQueryWrapper<DocumentPoItem>()
                .eq(DocumentPoItem::getPoId, id)
                .orderByAsc(DocumentPoItem::getSortOrder);
        List<DocumentPoItem> items = documentPoItemMapper.selectList(itemQuery);

        List<PoItemVO> itemVOs = items.stream()
                .map(item -> BeanUtil.toBean(item, PoItemVO.class))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * DN详情（含MSKU明细及货件备注）
     *
     * @param id DN主键
     * @return DN详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public DnVO getDnDetail(Long id) {
        log.info("查询DN详情, id={}", id);
        DocumentDn dn = documentDnMapper.selectById(id);
        if (dn == null) {
            log.warn("DN不存在, id={}", id);
            return null;
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(dn.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        DnVO vo = BeanUtil.toBean(dn, DnVO.class);

        LambdaQueryWrapper<DocumentDnItem> itemQuery = new LambdaQueryWrapper<DocumentDnItem>()
                .eq(DocumentDnItem::getDnId, id)
                .orderByAsc(DocumentDnItem::getLineNo);
        List<DocumentDnItem> items = documentDnItemMapper.selectList(itemQuery);

        List<DnItemVO> itemVOs = items.stream()
                .map(item -> BeanUtil.toBean(item, DnItemVO.class))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * 结算单详情（含周期、站点、MSKU明细）
     *
     * @param id 结算单主键
     * @return 结算单详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public SettlementVO getSettlementDetail(Long id) {
        log.info("查询结算单详情, id={}", id);
        DocumentSettlement settlement = documentSettlementMapper.selectById(id);
        if (settlement == null) {
            log.warn("结算单不存在, id={}", id);
            return null;
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(settlement.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        SettlementVO vo = BeanUtil.toBean(settlement, SettlementVO.class);

        LambdaQueryWrapper<DocumentSettlementItem> itemQuery = new LambdaQueryWrapper<DocumentSettlementItem>()
                .eq(DocumentSettlementItem::getSettlementId, id)
                .orderByAsc(DocumentSettlementItem::getLineNo);
        List<DocumentSettlementItem> items = documentSettlementItemMapper.selectList(itemQuery);

        List<SettlementItemVO> itemVOs = items.stream()
                .map(item -> BeanUtil.toBean(item, SettlementItemVO.class))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * INV详情（含银行信息）
     *
     * @param id INV主键
     * @return INV详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public InvVO getInvDetail(Long id) {
        log.info("查询INV详情, id={}", id);
        DocumentInv inv = documentInvMapper.selectById(id);
        if (inv == null) {
            log.warn("INV不存在, id={}", id);
            return null;
        }
        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(inv.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        InvVO vo = BeanUtil.toBean(inv, InvVO.class);

        LambdaQueryWrapper<DocumentInvItem> itemQuery = new LambdaQueryWrapper<DocumentInvItem>()
                .eq(DocumentInvItem::getInvId, id)
                .orderByAsc(DocumentInvItem::getLineNo);
        List<DocumentInvItem> items = documentInvItemMapper.selectList(itemQuery);

        List<InvItemVO> itemVOs = items.stream()
                .map(item -> BeanUtil.toBean(item, InvItemVO.class))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * 按结算周期查看关联的4份结算单+4份INV
     *
     * @param periodStart 周期起始日
     * @param periodEnd   周期结束日
     * @return 单据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public List<DocumentListVO> listBySettlementPeriod(LocalDate periodStart, LocalDate periodEnd) {
        log.info("按结算周期查询关联单据, periodStart={}, periodEnd={}", periodStart, periodEnd);
        List<DocumentListVO> result = new ArrayList<>();
        Long shopId = ShopContext.requireShopId();

        // 查询结算单（强制 shopId 隔离）
        LambdaQueryWrapper<DocumentSettlement> settlementQuery = new LambdaQueryWrapper<DocumentSettlement>()
                .eq(DocumentSettlement::getShopId, shopId)
                .eq(DocumentSettlement::getPeriodStart, periodStart)
                .eq(DocumentSettlement::getPeriodEnd, periodEnd);
        List<DocumentSettlement> settlements = documentSettlementMapper.selectList(settlementQuery);

        for (DocumentSettlement s : settlements) {
            result.add(DocumentListVO.builder()
                    .id(s.getId())
                    .documentType(DocumentType.SETTLEMENT.getCode())
                    .documentNo(s.getDocumentNo())
                    .documentDate(s.getSettlementDate())
                    .buyerName(s.getBuyerName())
                    .sellerName(s.getSellerName())
                    .totalQuantity(s.getTotalQuantity())
                    .totalAmount(s.getTotalAmount())
                    .build());
        }

        // 查询INV（通过结算单ID关联）
        if (!CollectionUtils.isEmpty(settlements)) {
            List<Long> settlementIds = settlements.stream()
                    .map(DocumentSettlement::getId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<DocumentInv> invQuery = new LambdaQueryWrapper<DocumentInv>()
                    .in(DocumentInv::getSettlementId, settlementIds);
            List<DocumentInv> invs = documentInvMapper.selectList(invQuery);

            for (DocumentInv inv : invs) {
                result.add(DocumentListVO.builder()
                        .id(inv.getId())
                        .documentType(DocumentType.INV.getCode())
                        .documentNo(inv.getDocumentNo())
                        .documentDate(inv.getInvDate())
                        .buyerName(inv.getBuyerName())
                        .sellerName(inv.getSellerName())
                        .totalQuantity(inv.getTotalQuantity())
                        .totalAmount(inv.getTotalAmount())
                        .build());
            }
        }

        return result;
    }

    /**
     * 查看货件在PO和DN中的关联关系
     *
     * @param shipmentNo 货件编号
     * @return 包含 PO 和 DN 信息的 Map
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public Map<String, Object> getShipmentDocumentRelation(String shipmentNo) {
        log.info("查询货件关联关系, shipmentNo={}", shipmentNo);
        Map<String, Object> result = new HashMap<>();
        result.put("shipmentNo", shipmentNo);
        Long shopId = ShopContext.requireShopId();

        // 查询PO明细（强制 shopId 隔离）
        LambdaQueryWrapper<DocumentPoItem> poItemQuery = new LambdaQueryWrapper<DocumentPoItem>()
                .eq(DocumentPoItem::getShopId, shopId)
                .eq(DocumentPoItem::getShipmentNo, shipmentNo);
        List<DocumentPoItem> poItems = documentPoItemMapper.selectList(poItemQuery);
        result.put("poItems", poItems.stream()
                .map(item -> BeanUtil.toBean(item, PoItemVO.class))
                .collect(Collectors.toList()));

        // 查询DN明细（强制 shopId 隔离）
        LambdaQueryWrapper<DocumentDnItem> dnItemQuery = new LambdaQueryWrapper<DocumentDnItem>()
                .eq(DocumentDnItem::getShopId, shopId)
                .eq(DocumentDnItem::getShipmentNo, shipmentNo);
        List<DocumentDnItem> dnItems = documentDnItemMapper.selectList(dnItemQuery);
        result.put("dnItems", dnItems.stream()
                .map(item -> BeanUtil.toBean(item, DnItemVO.class))
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * MSKU列表分页查询
     *
     * <p>查询结算推导后的MSKU汇总数据，支持按站点、MSKU编码、结算周期筛选。</p>
     *
     * @param request 查询请求参数
     * @return 分页结果
     * @author wanhua
     * 14:00 2026年03月07日
     */
    @Override
    public PageResult<MskuListVO> listMsku(MskuQueryRequest request) {
        log.info("查询MSKU列表, 站点={}, MSKU={}, 周期={}~{}",
                request.getSiteCode(), request.getMsku(), request.getPeriodStart(), request.getPeriodEnd());

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Page<SettlementImportData> page = new Page<>(pageNum, pageSize);
        Long shopId = ShopContext.requireShopId();

        LambdaQueryWrapper<SettlementImportData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SettlementImportData::getShopId, shopId)
                .eq(SettlementImportData::getDelFlag, 0)
                .eq(StringUtils.hasText(request.getSiteCode()),
                        SettlementImportData::getSiteCode, request.getSiteCode())
                .like(StringUtils.hasText(request.getMsku()),
                        SettlementImportData::getMsku, request.getMsku())
                .ge(request.getPeriodStart() != null,
                        SettlementImportData::getPeriodStart, request.getPeriodStart())
                .le(request.getPeriodEnd() != null,
                        SettlementImportData::getPeriodEnd, request.getPeriodEnd())
                .orderByDesc(SettlementImportData::getPeriodStart)
                .orderByAsc(SettlementImportData::getSiteCode)
                .orderByAsc(SettlementImportData::getMsku);

        Page<SettlementImportData> pageResult = settlementImportDataMapper.selectPage(page, queryWrapper);

        List<MskuListVO> records = pageResult.getRecords().stream()
                .map(data -> MskuListVO.builder()
                        .id(data.getId())
                        .siteCode(data.getSiteCode())
                        .msku(data.getMsku())
                        .currency(data.getCurrency())
                        .unitPrice(data.getUnitPrice())
                        .quantity(data.getQuantity())
                        .amount(data.getAmount())
                        .periodStart(data.getPeriodStart() != null ? data.getPeriodStart().toString() : null)
                        .periodEnd(data.getPeriodEnd() != null ? data.getPeriodEnd().toString() : null)
                        .procurementCostCny(data.getProcurementCostCny())
                        .averageExchangeRate(data.getAverageExchangeRate())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 更新MSKU数据（手动编辑单价、数量、采购成本、汇率）
     *
     * <p>根据记录ID更新 t_settlement_import_data 中的可编辑字段，
     * 同时自动重算金额（amount = unitPrice * quantity）。</p>
     *
     * @param request 更新请求
     * @author wanhua
     * 14:00 2026年03月07日
     */
    @Override
    public void updateMsku(MskuUpdateRequest request) {
        SettlementImportData data = settlementImportDataMapper.selectById(request.getId());
        if (data == null) {
            throw new RuntimeException("记录不存在，ID: " + request.getId());
        }

        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(data.getShopId())) {
            throw new RuntimeException("无权操作该记录");
        }

        // 更新可编辑字段
        if (request.getUnitPrice() != null) {
            data.setUnitPrice(request.getUnitPrice());
        }
        if (request.getQuantity() != null) {
            data.setQuantity(request.getQuantity());
        }
        if (request.getProcurementCostCny() != null) {
            data.setProcurementCostCny(request.getProcurementCostCny());
        }
        if (request.getAverageExchangeRate() != null) {
            data.setAverageExchangeRate(request.getAverageExchangeRate());
        }

        // 重算金额
        if (data.getUnitPrice() != null && data.getQuantity() != null) {
            data.setAmount(data.getUnitPrice().multiply(java.math.BigDecimal.valueOf(data.getQuantity())));
        }

        settlementImportDataMapper.updateById(data);
        log.info("MSKU数据更新成功，ID: {}, MSKU: {}", data.getId(), data.getMsku());
    }

    // ==================== 私有方法：分页查询各类型单据 ====================

    /**
     * 查询全部类型单据（合并PO/DN/结算单/INV）
     *
     * <p>分别查询4种类型，合并后按日期倒序排列，手动分页返回。</p>
     *
     * @param request 查询请求
     * @return 分页结果
     * @author wanhua
     * 15:00 2026年03月07日
     */
    private PageResult<DocumentListVO> queryAllTypesList(DocumentQueryRequest request) {
        List<DocumentListVO> allRecords = new ArrayList<>();
        Long shopId = ShopContext.requireShopId();

        // 查询PO
        LambdaQueryWrapper<DocumentPo> poQuery = new LambdaQueryWrapper<DocumentPo>()
                .eq(DocumentPo::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentPo::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentPo::getPoDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentPo::getPoDate, request.getEndDate())
                .orderByDesc(DocumentPo::getPoDate);
        List<DocumentPo> poList = documentPoMapper.selectList(poQuery);
        for (DocumentPo po : poList) {
            allRecords.add(DocumentListVO.builder()
                    .id(po.getId())
                    .documentType(DocumentType.PO.getCode())
                    .documentNo(po.getDocumentNo())
                    .documentDate(po.getPoDate())
                    .buyerName(po.getBuyerName())
                    .sellerName(po.getSellerName())
                    .totalQuantity(po.getTotalQuantity())
                    .totalAmount(null)
                    .build());
        }

        // 查询DN
        LambdaQueryWrapper<DocumentDn> dnQuery = new LambdaQueryWrapper<DocumentDn>()
                .eq(DocumentDn::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentDn::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentDn::getDnDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentDn::getDnDate, request.getEndDate())
                .orderByDesc(DocumentDn::getDnDate);
        List<DocumentDn> dnList = documentDnMapper.selectList(dnQuery);
        for (DocumentDn dn : dnList) {
            allRecords.add(DocumentListVO.builder()
                    .id(dn.getId())
                    .documentType(DocumentType.DN.getCode())
                    .documentNo(dn.getDocumentNo())
                    .documentDate(dn.getDnDate())
                    .buyerName(dn.getCustomerName())
                    .sellerName(dn.getSupplierName())
                    .totalQuantity(dn.getTotalQuantity())
                    .totalAmount(null)
                    .build());
        }

        // 查询结算单
        LambdaQueryWrapper<DocumentSettlement> settlementQuery = new LambdaQueryWrapper<DocumentSettlement>()
                .eq(DocumentSettlement::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentSettlement::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentSettlement::getSettlementDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentSettlement::getSettlementDate, request.getEndDate())
                .orderByDesc(DocumentSettlement::getSettlementDate);
        List<DocumentSettlement> settlementList = documentSettlementMapper.selectList(settlementQuery);
        for (DocumentSettlement s : settlementList) {
            allRecords.add(DocumentListVO.builder()
                    .id(s.getId())
                    .documentType(DocumentType.SETTLEMENT.getCode())
                    .documentNo(s.getDocumentNo())
                    .documentDate(s.getSettlementDate())
                    .buyerName(s.getBuyerName())
                    .sellerName(s.getSellerName())
                    .totalQuantity(s.getTotalQuantity())
                    .totalAmount(s.getTotalAmount())
                    .build());
        }

        // 查询INV
        LambdaQueryWrapper<DocumentInv> invQuery = new LambdaQueryWrapper<DocumentInv>()
                .eq(DocumentInv::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentInv::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentInv::getInvDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentInv::getInvDate, request.getEndDate())
                .orderByDesc(DocumentInv::getInvDate);
        List<DocumentInv> invList = documentInvMapper.selectList(invQuery);
        for (DocumentInv inv : invList) {
            allRecords.add(DocumentListVO.builder()
                    .id(inv.getId())
                    .documentType(DocumentType.INV.getCode())
                    .documentNo(inv.getDocumentNo())
                    .documentDate(inv.getInvDate())
                    .buyerName(inv.getBuyerName())
                    .sellerName(inv.getSellerName())
                    .totalQuantity(inv.getTotalQuantity())
                    .totalAmount(inv.getTotalAmount())
                    .build());
        }

        // 按日期倒序排列
        allRecords.sort((a, b) -> {
            if (a.getDocumentDate() == null && b.getDocumentDate() == null) return 0;
            if (a.getDocumentDate() == null) return 1;
            if (b.getDocumentDate() == null) return -1;
            return b.getDocumentDate().compareTo(a.getDocumentDate());
        });

        // 手动分页
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int total = allRecords.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<DocumentListVO> pageRecords = fromIndex < total
                ? allRecords.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return PageResult.of(pageRecords, total, pageNum, pageSize);
    }

    /**
     * 分页查询PO列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    private PageResult<DocumentListVO> queryPoList(DocumentQueryRequest request) {
        Long shopId = ShopContext.requireShopId();
        Page<DocumentPo> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<DocumentPo> query = new LambdaQueryWrapper<DocumentPo>()
                .eq(DocumentPo::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentPo::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentPo::getPoDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentPo::getPoDate, request.getEndDate())
                .orderByDesc(DocumentPo::getPoDate);

        Page<DocumentPo> pageResult = documentPoMapper.selectPage(page, query);

        List<DocumentListVO> records = pageResult.getRecords().stream()
                .map(po -> DocumentListVO.builder()
                        .id(po.getId())
                        .documentType(DocumentType.PO.getCode())
                        .documentNo(po.getDocumentNo())
                        .documentDate(po.getPoDate())
                        .buyerName(po.getBuyerName())
                        .sellerName(po.getSellerName())
                        .totalQuantity(po.getTotalQuantity())
                        .totalAmount(null)
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 分页查询DN列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    private PageResult<DocumentListVO> queryDnList(DocumentQueryRequest request) {
        Long shopId = ShopContext.requireShopId();
        Page<DocumentDn> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<DocumentDn> query = new LambdaQueryWrapper<DocumentDn>()
                .eq(DocumentDn::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentDn::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentDn::getDnDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentDn::getDnDate, request.getEndDate())
                .orderByDesc(DocumentDn::getDnDate);

        Page<DocumentDn> pageResult = documentDnMapper.selectPage(page, query);

        List<DocumentListVO> records = pageResult.getRecords().stream()
                .map(dn -> DocumentListVO.builder()
                        .id(dn.getId())
                        .documentType(DocumentType.DN.getCode())
                        .documentNo(dn.getDocumentNo())
                        .documentDate(dn.getDnDate())
                        .buyerName(dn.getCustomerName())
                        .sellerName(dn.getSupplierName())
                        .totalQuantity(dn.getTotalQuantity())
                        .totalAmount(null)
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 分页查询结算单列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    private PageResult<DocumentListVO> querySettlementList(DocumentQueryRequest request) {
        Long shopId = ShopContext.requireShopId();
        Page<DocumentSettlement> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<DocumentSettlement> query = new LambdaQueryWrapper<DocumentSettlement>()
                .eq(DocumentSettlement::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentSettlement::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentSettlement::getSettlementDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentSettlement::getSettlementDate, request.getEndDate())
                .orderByDesc(DocumentSettlement::getSettlementDate);

        Page<DocumentSettlement> pageResult = documentSettlementMapper.selectPage(page, query);

        List<DocumentListVO> records = pageResult.getRecords().stream()
                .map(s -> DocumentListVO.builder()
                        .id(s.getId())
                        .documentType(DocumentType.SETTLEMENT.getCode())
                        .documentNo(s.getDocumentNo())
                        .documentDate(s.getSettlementDate())
                        .buyerName(s.getBuyerName())
                        .sellerName(s.getSellerName())
                        .totalQuantity(s.getTotalQuantity())
                        .totalAmount(s.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 分页查询INV列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    private PageResult<DocumentListVO> queryInvList(DocumentQueryRequest request) {
        Long shopId = ShopContext.requireShopId();
        Page<DocumentInv> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<DocumentInv> query = new LambdaQueryWrapper<DocumentInv>()
                .eq(DocumentInv::getShopId, shopId)
                .like(StringUtils.hasText(request.getDocumentNo()),
                        DocumentInv::getDocumentNo, request.getDocumentNo())
                .ge(request.getStartDate() != null,
                        DocumentInv::getInvDate, request.getStartDate())
                .le(request.getEndDate() != null,
                        DocumentInv::getInvDate, request.getEndDate())
                .orderByDesc(DocumentInv::getInvDate);

        Page<DocumentInv> pageResult = documentInvMapper.selectPage(page, query);

        List<DocumentListVO> records = pageResult.getRecords().stream()
                .map(inv -> DocumentListVO.builder()
                        .id(inv.getId())
                        .documentType(DocumentType.INV.getCode())
                        .documentNo(inv.getDocumentNo())
                        .documentDate(inv.getInvDate())
                        .buyerName(inv.getBuyerName())
                        .sellerName(inv.getSellerName())
                        .totalQuantity(inv.getTotalQuantity())
                        .totalAmount(inv.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }
}
