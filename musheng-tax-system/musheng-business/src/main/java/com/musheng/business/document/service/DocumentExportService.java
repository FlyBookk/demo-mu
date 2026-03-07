package com.musheng.business.document.service;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Excel导出服务接口
 *
 * <p>提供4种单据（PO/DN/结算单/INV）的Excel导出、批量导出、导出文件名生成功能。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface DocumentExportService {

    /**
     * 导出PO为Excel
     *
     * @param poId PO主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void exportPo(Long poId, HttpServletResponse response);

    /**
     * 导出DN为Excel
     *
     * @param dnId DN主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void exportDn(Long dnId, HttpServletResponse response);

    /**
     * 导出结算单为Excel
     *
     * @param settlementId 结算单主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void exportSettlement(Long settlementId, HttpServletResponse response);

    /**
     * 导出INV为Excel
     *
     * @param invId INV主表ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void exportInv(Long invId, HttpServletResponse response);

    /**
     * 批量导出一个结算周期的8份文件（4结算单+4INV）
     *
     * <p>打包为ZIP文件导出。</p>
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void batchExportByPeriod(LocalDate periodStart, LocalDate periodEnd, HttpServletResponse response);

    /**
     * 批量导出INV为ZIP
     *
     * @param invIds INV主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 00:50 2026年03月02日
     */
    void batchExportInv(List<Long> invIds, HttpServletResponse response);

    /**
     * 生成PO导出文件名
     *
     * <p>格式：{编号}-{买方中文名}-{卖方名}-PO.xlsx</p>
     *
     * @param documentNo 单据编号
     * @param buyerName 买方名称
     * @param sellerName 卖方名称
     * @return PO导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    String generatePoFileName(String documentNo, String buyerName, String sellerName);

    /**
     * 生成DN导出文件名
     *
     * <p>格式：{编号}-{供应商名}-{客户繁体名}-送貨清單.xlsx</p>
     *
     * @param documentNo 单据编号
     * @param supplierName 供应商名称
     * @param customerName 客户名称（繁体）
     * @return DN导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    String generateDnFileName(String documentNo, String supplierName, String customerName);

    /**
     * 生成结算单导出文件名
     *
     * <p>格式：{编号}-{买方中文名}-{卖方名}-结算单.xlsx</p>
     *
     * @param documentNo 单据编号
     * @param buyerName 买方名称
     * @param sellerName 卖方名称
     * @return 结算单导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    String generateSettlementFileName(String documentNo, String buyerName, String sellerName);

    /**
     * 生成INV导出文件名
     *
     * <p>格式：{编号}-{卖方名}-{买方英文名}-invoice.xlsx</p>
     *
     * @param documentNo 单据编号
     * @param sellerName 卖方名称
     * @param buyerName 买方英文名称
     * @return INV导出文件名
     * @author wanhua
     * 10:30 2026年01月29日
     */
    String generateInvFileName(String documentNo, String sellerName, String buyerName);
}
