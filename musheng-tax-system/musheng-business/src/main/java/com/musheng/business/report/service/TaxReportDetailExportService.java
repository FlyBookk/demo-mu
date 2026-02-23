package com.musheng.business.report.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 报税统计明细导出服务
 * 导出参与统计的原始数据，分 sheet/文件：收入、退款、费用、其它
 */
public interface TaxReportDetailExportService {

    /**
     * 导出报税统计明细（参与统计的销售数据）
     * 数据量小用 Excel 多 sheet；超过阈值用 CSV 多文件打 zip 包
     *
     * @param siteCode     站点编码（可选）
     * @param startQuarter 开始季度
     * @param endQuarter   结束季度
     * @param response     HTTP 响应
     */
    void exportTaxSummaryDetail(String siteCode, String startQuarter, String endQuarter, HttpServletResponse response);
}
