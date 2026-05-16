package com.musheng.tiktok.document.service;

import com.musheng.common.result.PageResult;
import com.musheng.tiktok.document.dto.TiktokDocumentQueryRequest;
import com.musheng.tiktok.document.entity.*;
import com.musheng.tiktok.document.vo.TiktokDocumentListVO;

import java.util.List;

/**
 * TK单据查询服务
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
public interface TiktokDocumentQueryService {

    /**
     * 分页查询单据列表（支持按类型/编号/日期/站点筛选）
     */
    PageResult<TiktokDocumentListVO> listDocuments(TiktokDocumentQueryRequest request);

    /**
     * PO详情
     */
    TiktokDocumentPo getPoDetail(Long id);

    /**
     * PO明细列表
     */
    List<TiktokDocumentPoItem> getPoItems(Long poId);

    /**
     * DN详情
     */
    TiktokDocumentDn getDnDetail(Long id);

    /**
     * DN明细列表
     */
    List<TiktokDocumentDnItem> getDnItems(Long dnId);

    /**
     * 结算单详情
     */
    TiktokDocumentSettlement getSettlementDetail(Long id);

    /**
     * 结算单明细列表
     */
    List<TiktokDocumentSettlementItem> getSettlementItems(Long settlementId);

    /**
     * INV详情
     */
    TiktokDocumentInv getInvDetail(Long id);

    /**
     * INV明细列表
     */
    List<TiktokDocumentInvItem> getInvItems(Long invId);
}
