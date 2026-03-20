package com.musheng.business.report.service;

import com.musheng.business.report.dto.DashboardData;
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
     * @param siteCode       站点编码（可选，不传则查询所有站点）
     * @param startQuarter   开始季度（格式：2024-Q1）
     * @param endQuarter     结束季度（格式：2024-Q4）
     * @param refundDateMode 退款时间维度：ship=配送日期（默认），settlement=结算日期
     * @return 报税汇总列表
     */
    List<TaxReportSummary> getTaxSummary(String siteCode, String startQuarter, String endQuarter, String refundDateMode);

    /**
     * 导出报税汇总列表（表头与列表一致）
     *
     * @param siteCode       站点编码（可选）
     * @param startQuarter   开始季度
     * @param endQuarter     结束季度
     * @param refundDateMode 退款时间维度：ship=配送日期（默认），settlement=结算日期
     * @param response       HTTP响应
     */
    void exportTaxSummary(String siteCode, String startQuarter, String endQuarter, String refundDateMode, HttpServletResponse response);
}
