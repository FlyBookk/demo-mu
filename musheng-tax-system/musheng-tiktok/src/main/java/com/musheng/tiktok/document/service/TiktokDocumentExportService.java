package com.musheng.tiktok.document.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * TK单据导出服务
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
public interface TiktokDocumentExportService {

    void exportPo(Long poId, HttpServletResponse response);

    void exportDn(Long dnId, HttpServletResponse response);

    void exportSettlement(Long settlementId, HttpServletResponse response);

    void exportInv(Long invId, HttpServletResponse response);

    void batchExportPo(List<Long> poIds, HttpServletResponse response);

    void batchExportDn(List<Long> dnIds, HttpServletResponse response);

    void batchExportSettlement(List<Long> settlementIds, HttpServletResponse response);

    void batchExportInv(List<Long> invIds, HttpServletResponse response);
}
