package com.musheng.business.report.service;

import com.musheng.business.report.dto.ReportSummary;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 报表服务接口
 */
public interface ReportService {

    /**
     * 查询汇总报表
     *
     * @param siteCode     站点编码(可选)
     * @param yearQuarter  年季度(可选)
     * @param startQuarter 开始年季度(可选)
     * @param endQuarter   结束年季度(可选)
     * @return 汇总报表列表
     */
    List<ReportSummary> getSummary(String siteCode, String yearQuarter, String startQuarter, String endQuarter);

    /**
     * 按站点分组查询汇总报表
     *
     * @param yearQuarter 年季度
     * @return 汇总报表列表
     */
    List<ReportSummary> getSummaryBySite(String yearQuarter);

    /**
     * 按季度分组查询汇总报表
     *
     * @param siteCode     站点编码
     * @param startQuarter 开始年季度(可选)
     * @param endQuarter   结束年季度(可选)
     * @return 汇总报表列表
     */
    List<ReportSummary> getSummaryByQuarter(String siteCode, String startQuarter, String endQuarter);

    /**
     * 导出汇总报表为Excel文件
     *
     * @param siteCode     站点编码(可选)
     * @param yearQuarter  年季度(可选)
     * @param startQuarter 开始年季度(可选)
     * @param endQuarter   结束年季度(可选)
     * @param response     HTTP响应
     */
    void exportSummary(String siteCode, String yearQuarter, String startQuarter, String endQuarter, HttpServletResponse response);

    /**
     * 导出明细报表为Excel文件
     *
     * @param siteCode    站点编码
     * @param yearQuarter 年季度
     * @param reportType  报表类型(sales/shipping/all)
     * @param response    HTTP响应
     */
    void exportDetail(String siteCode, String yearQuarter, String reportType, HttpServletResponse response);
}
