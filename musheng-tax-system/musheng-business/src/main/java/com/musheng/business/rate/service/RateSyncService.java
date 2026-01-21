package com.musheng.business.rate.service;

import com.musheng.business.rate.dto.RateSyncResultDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率同步服务接口
 */
public interface RateSyncService {

    /**
     * 从中国外汇交易中心同步汇率数据
     * 只同步启用状态的货币
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 同步结果
     */
    RateSyncResultDTO syncFromChinaMoney(LocalDate startDate, LocalDate endDate);

    /**
     * 同步指定货币的汇率
     *
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @param currencyCodes  指定货币编码列表
     * @return 同步结果
     */
    RateSyncResultDTO syncSpecificCurrencies(LocalDate startDate, LocalDate endDate, List<String> currencyCodes);

    /**
     * 同步最近N天的汇率
     * 自动同步所有启用货币
     *
     * @param days 天数
     * @return 同步结果
     */
    RateSyncResultDTO syncRecentDays(int days);
}
