package com.musheng.business.document.service;

import com.musheng.business.document.dto.DocumentQueryRequest;
import com.musheng.business.document.dto.MskuQueryRequest;
import com.musheng.business.document.dto.MskuUpdateRequest;
import com.musheng.business.document.vo.*;
import com.musheng.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 单据查询服务接口
 *
 * <p>提供分页查询、4种单据详情查询、按结算周期查看关联单据、货件关联关系查询。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface DocumentQueryService {

    /**
     * 分页查询单据列表（支持按类型、编号、日期范围筛选）
     *
     * @param request 查询请求参数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    PageResult<DocumentListVO> listDocuments(DocumentQueryRequest request);

    /**
     * PO详情（含货件分组明细）
     *
     * @param id PO主键
     * @return PO详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    PoVO getPoDetail(Long id);

    /**
     * DN详情（含MSKU明细及货件备注）
     *
     * @param id DN主键
     * @return DN详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    DnVO getDnDetail(Long id);

    /**
     * 结算单详情（含周期、站点、MSKU明细）
     *
     * @param id 结算单主键
     * @return 结算单详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    SettlementVO getSettlementDetail(Long id);

    /**
     * INV详情（含银行信息）
     *
     * @param id INV主键
     * @return INV详情视图，不存在返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    InvVO getInvDetail(Long id);

    /**
     * 按结算周期查看关联的4份结算单+4份INV
     *
     * @param periodStart 周期起始日
     * @param periodEnd   周期结束日
     * @return 单据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    List<DocumentListVO> listBySettlementPeriod(LocalDate periodStart, LocalDate periodEnd);

    /**
     * 查看货件在PO和DN中的关联关系
     *
     * @param shipmentNo 货件编号
     * @return 包含 PO 和 DN 信息的 Map
     * @author wanhua
     * 10:30 2026年01月29日
     */
    Map<String, Object> getShipmentDocumentRelation(String shipmentNo);

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
    PageResult<MskuListVO> listMsku(MskuQueryRequest request);

    /**
     * 更新MSKU数据（手动编辑单价、数量、采购成本、汇率）
     *
     * @param request 更新请求
     * @author wanhua
     * 14:00 2026年03月07日
     */
    void updateMsku(MskuUpdateRequest request);
}
