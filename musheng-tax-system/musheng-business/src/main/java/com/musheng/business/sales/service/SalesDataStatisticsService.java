package com.musheng.business.sales.service;

import java.util.List;
import java.util.Map;

/**
 * 销售数据统计服务接口
 * 
 * 职责：
 * 1. 获取销售数据统计汇总
 * 2. 按交易类型分组统计
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface SalesDataStatisticsService {

    /**
     * 获取销售数据统计汇总
     *
     * @param keyword             关键字(可选)
     * @param sourceType          数据来源(可选)
     * @param siteCode            站点编码(可选)
     * @param settlementId        结算编号(可选)
     * @param transactionCategory 交易分类(可选)
     * @param isOwnSite           数据归属(可选)
     * @param startDate           开始日期(可选)
     * @param endDate             结束日期(可选)
     * @return 汇总数据
     */
    Map<String, Object> getSummary(String keyword, String sourceType, String siteCode, String settlementId,
                                   String transactionCategory, Integer isOwnSite, String startDate, String endDate);

    /**
     * 按交易类型分组统计
     *
     * @param siteCode  站点编码(可选)
     * @param startDate 开始日期(可选)
     * @param endDate   结束日期(可选)
     * @return 统计列表
     */
    List<Map<String, Object>> getStatByType(String siteCode, String startDate, String endDate);
}
