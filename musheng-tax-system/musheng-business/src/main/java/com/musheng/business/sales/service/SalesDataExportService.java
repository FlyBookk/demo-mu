package com.musheng.business.sales.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 销售数据导出服务接口
 * 
 * 职责：
 * 1. 导出销售数据到Excel
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface SalesDataExportService {

    /**
     * 导出销售数据到Excel
     *
     * @param siteCode            站点编码(可选)
     * @param transactionCategory 交易分类(可选)
     * @param startDate           开始日期(可选)
     * @param endDate             结束日期(可选)
     * @param response            HTTP响应
     */
    void exportData(String siteCode, String transactionCategory, String startDate, String endDate, 
                    HttpServletResponse response);
}
