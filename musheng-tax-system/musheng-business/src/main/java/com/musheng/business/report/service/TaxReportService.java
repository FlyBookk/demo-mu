package com.musheng.business.report.service;

import com.musheng.business.report.dto.DashboardData;
import com.musheng.business.report.dto.FeeBreakdown;
import com.musheng.business.report.dto.TaxReportSummary;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 报税汇总服务接口
 */
public interface TaxReportService {

    /**
     * 获取首页仪表盘数据
     *
     * @param quarter 季度(格式:2024-Q1，不传则默认当前季度)
     * @return 仪表盘数据
     */
    DashboardData getDashboardData(String quarter);

    /**
     * 查询报税汇总数据
     *
     * @param siteCode     站点编码（可选，不传则查询所有站点）
     * @param startQuarter 开始季度（格式：2024-Q1）
     * @param endQuarter   结束季度（格式：2024-Q4）
     * @return 报税汇总列表
     */
    List<TaxReportSummary> getTaxSummary(String siteCode, String startQuarter, String endQuarter);

    /**
     * 查询费用分类明细（用于图表展示）
     *
     * @param siteCode     站点编码（可选）
     * @param startQuarter 开始季度
     * @param endQuarter   结束季度
     * @return 费用分类列表
     */
    List<FeeBreakdown> getFeeBreakdown(String siteCode, String startQuarter, String endQuarter);

    /**
     * 导出报税汇总报表
     *
     * @param siteCode     站点编码（可选）
     * @param startQuarter 开始季度
     * @param endQuarter   结束季度
     * @param response     HTTP响应
     */
    void exportTaxSummary(String siteCode, String startQuarter, String endQuarter, HttpServletResponse response);
}
