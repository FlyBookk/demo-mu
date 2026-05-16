package com.musheng.tiktok.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.context.ShopContext;
import com.musheng.common.result.PageResult;
import com.musheng.tiktok.document.dto.TiktokDocumentQueryRequest;
import com.musheng.tiktok.document.entity.*;
import com.musheng.tiktok.document.mapper.*;
import com.musheng.tiktok.document.service.TiktokDocumentQueryService;
import com.musheng.tiktok.document.vo.TiktokDocumentListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TK单据查询服务实现
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Service
@Slf4j
public class TiktokDocumentQueryServiceImpl implements TiktokDocumentQueryService {

    @Autowired
    private TiktokDocumentPoMapper poMapper;
    @Autowired
    private TiktokDocumentPoItemMapper poItemMapper;
    @Autowired
    private TiktokDocumentDnMapper dnMapper;
    @Autowired
    private TiktokDocumentDnItemMapper dnItemMapper;
    @Autowired
    private TiktokDocumentSettlementMapper settlementMapper;
    @Autowired
    private TiktokDocumentSettlementItemMapper settlementItemMapper;
    @Autowired
    private TiktokDocumentInvMapper invMapper;
    @Autowired
    private TiktokDocumentInvItemMapper invItemMapper;

    @Override
    public PageResult<TiktokDocumentListVO> listDocuments(TiktokDocumentQueryRequest request) {
        Long shopId = ShopContext.requireShopId();
        List<TiktokDocumentListVO> allDocs = new ArrayList<>();

        String type = request.getDocumentType();
        boolean queryAll = !StringUtils.hasText(type);

        if (queryAll || "PO".equalsIgnoreCase(type)) {
            allDocs.addAll(queryPoList(shopId, request));
        }
        if (queryAll || "DN".equalsIgnoreCase(type)) {
            allDocs.addAll(queryDnList(shopId, request));
        }
        if (queryAll || "SETTLEMENT".equalsIgnoreCase(type)) {
            allDocs.addAll(querySettlementList(shopId, request));
        }
        if (queryAll || "INV".equalsIgnoreCase(type)) {
            allDocs.addAll(queryInvList(shopId, request));
        }

        // 按创建时间倒序
        allDocs.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

        // 手动分页
        int total = allDocs.size();
        int from = (request.getPageNum() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), total);
        List<TiktokDocumentListVO> pageData = from < total ? allDocs.subList(from, to) : new ArrayList<>();

        return PageResult.of(pageData, (long) total, request.getPageNum(), request.getPageSize());
    }

    @Override
    public TiktokDocumentPo getPoDetail(Long id) {
        Long shopId = ShopContext.requireShopId();
        TiktokDocumentPo po = poMapper.selectById(id);
        if (po == null || !shopId.equals(po.getShopId())) return null;
        return po;
    }

    @Override
    public List<TiktokDocumentPoItem> getPoItems(Long poId) {
        return poItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentPoItem>()
                .eq(TiktokDocumentPoItem::getPoId, poId)
                .orderByAsc(TiktokDocumentPoItem::getSortOrder));
    }

    @Override
    public TiktokDocumentDn getDnDetail(Long id) {
        Long shopId = ShopContext.requireShopId();
        TiktokDocumentDn dn = dnMapper.selectById(id);
        if (dn == null || !shopId.equals(dn.getShopId())) return null;
        return dn;
    }

    @Override
    public List<TiktokDocumentDnItem> getDnItems(Long dnId) {
        return dnItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentDnItem>()
                .eq(TiktokDocumentDnItem::getDnId, dnId)
                .orderByAsc(TiktokDocumentDnItem::getLineNo));
    }

    @Override
    public TiktokDocumentSettlement getSettlementDetail(Long id) {
        Long shopId = ShopContext.requireShopId();
        TiktokDocumentSettlement s = settlementMapper.selectById(id);
        if (s == null || !shopId.equals(s.getShopId())) return null;
        return s;
    }

    @Override
    public List<TiktokDocumentSettlementItem> getSettlementItems(Long settlementId) {
        return settlementItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentSettlementItem>()
                .eq(TiktokDocumentSettlementItem::getSettlementId, settlementId)
                .orderByAsc(TiktokDocumentSettlementItem::getLineNo));
    }

    @Override
    public TiktokDocumentInv getInvDetail(Long id) {
        Long shopId = ShopContext.requireShopId();
        TiktokDocumentInv inv = invMapper.selectById(id);
        if (inv == null || !shopId.equals(inv.getShopId())) return null;
        return inv;
    }

    @Override
    public List<TiktokDocumentInvItem> getInvItems(Long invId) {
        return invItemMapper.selectList(new LambdaQueryWrapper<TiktokDocumentInvItem>()
                .eq(TiktokDocumentInvItem::getInvId, invId)
                .orderByAsc(TiktokDocumentInvItem::getLineNo));
    }

    // ==================== 私有方法 ====================

    private List<TiktokDocumentListVO> queryPoList(Long shopId, TiktokDocumentQueryRequest req) {
        LambdaQueryWrapper<TiktokDocumentPo> wrapper = new LambdaQueryWrapper<TiktokDocumentPo>()
                .eq(TiktokDocumentPo::getShopId, shopId);
        if (StringUtils.hasText(req.getSiteCode())) wrapper.eq(TiktokDocumentPo::getSiteCode, req.getSiteCode());
        if (StringUtils.hasText(req.getDocumentNo())) wrapper.like(TiktokDocumentPo::getDocumentNo, req.getDocumentNo());
        if (req.getStartDate() != null) wrapper.ge(TiktokDocumentPo::getPoDate, req.getStartDate());
        if (req.getEndDate() != null) wrapper.le(TiktokDocumentPo::getPoDate, req.getEndDate());

        return poMapper.selectList(wrapper).stream().map(po -> TiktokDocumentListVO.builder()
                .id(po.getId()).documentType("PO").documentNo(po.getDocumentNo())
                .siteCode(po.getSiteCode()).documentDate(po.getPoDate())
                .buyerName(po.getBuyerName()).sellerName(po.getSellerName())
                .totalQuantity(po.getTotalQuantity()).createTime(po.getCreateTime())
                .build()).toList();
    }

    private List<TiktokDocumentListVO> queryDnList(Long shopId, TiktokDocumentQueryRequest req) {
        LambdaQueryWrapper<TiktokDocumentDn> wrapper = new LambdaQueryWrapper<TiktokDocumentDn>()
                .eq(TiktokDocumentDn::getShopId, shopId);
        if (StringUtils.hasText(req.getSiteCode())) wrapper.eq(TiktokDocumentDn::getSiteCode, req.getSiteCode());
        if (StringUtils.hasText(req.getDocumentNo())) wrapper.like(TiktokDocumentDn::getDocumentNo, req.getDocumentNo());
        if (req.getStartDate() != null) wrapper.ge(TiktokDocumentDn::getDnDate, req.getStartDate());
        if (req.getEndDate() != null) wrapper.le(TiktokDocumentDn::getDnDate, req.getEndDate());

        return dnMapper.selectList(wrapper).stream().map(dn -> TiktokDocumentListVO.builder()
                .id(dn.getId()).documentType("DN").documentNo(dn.getDocumentNo())
                .siteCode(dn.getSiteCode()).documentDate(dn.getDnDate())
                .buyerName(dn.getSupplierName()).sellerName(dn.getCustomerName())
                .totalQuantity(dn.getTotalQuantity()).createTime(dn.getCreateTime())
                .build()).toList();
    }

    private List<TiktokDocumentListVO> querySettlementList(Long shopId, TiktokDocumentQueryRequest req) {
        LambdaQueryWrapper<TiktokDocumentSettlement> wrapper = new LambdaQueryWrapper<TiktokDocumentSettlement>()
                .eq(TiktokDocumentSettlement::getShopId, shopId);
        if (StringUtils.hasText(req.getSiteCode())) wrapper.eq(TiktokDocumentSettlement::getSiteCode, req.getSiteCode());
        if (StringUtils.hasText(req.getDocumentNo())) wrapper.like(TiktokDocumentSettlement::getDocumentNo, req.getDocumentNo());
        if (req.getStartDate() != null) wrapper.ge(TiktokDocumentSettlement::getPeriodStart, req.getStartDate());
        if (req.getEndDate() != null) wrapper.le(TiktokDocumentSettlement::getPeriodEnd, req.getEndDate());

        return settlementMapper.selectList(wrapper).stream().map(s -> TiktokDocumentListVO.builder()
                .id(s.getId()).documentType("SETTLEMENT").documentNo(s.getDocumentNo())
                .siteCode(s.getSiteCode()).documentDate(s.getSettlementDate())
                .buyerName(s.getBuyerName()).sellerName(s.getSellerName())
                .totalQuantity(s.getTotalQuantity()).totalAmount(s.getTotalAmount())
                .createTime(s.getCreateTime())
                .build()).toList();
    }

    private List<TiktokDocumentListVO> queryInvList(Long shopId, TiktokDocumentQueryRequest req) {
        LambdaQueryWrapper<TiktokDocumentInv> wrapper = new LambdaQueryWrapper<TiktokDocumentInv>()
                .eq(TiktokDocumentInv::getShopId, shopId);
        if (StringUtils.hasText(req.getSiteCode())) wrapper.eq(TiktokDocumentInv::getSiteCode, req.getSiteCode());
        if (StringUtils.hasText(req.getDocumentNo())) wrapper.like(TiktokDocumentInv::getDocumentNo, req.getDocumentNo());
        if (req.getStartDate() != null) wrapper.ge(TiktokDocumentInv::getInvDate, req.getStartDate());
        if (req.getEndDate() != null) wrapper.le(TiktokDocumentInv::getInvDate, req.getEndDate());

        return invMapper.selectList(wrapper).stream().map(inv -> TiktokDocumentListVO.builder()
                .id(inv.getId()).documentType("INV").documentNo(inv.getDocumentNo())
                .siteCode(inv.getSiteCode()).documentDate(inv.getInvDate())
                .buyerName(inv.getBuyerName()).sellerName(inv.getSellerName())
                .totalQuantity(inv.getTotalQuantity()).totalAmount(inv.getTotalAmount())
                .createTime(inv.getCreateTime())
                .build()).toList();
    }
}
