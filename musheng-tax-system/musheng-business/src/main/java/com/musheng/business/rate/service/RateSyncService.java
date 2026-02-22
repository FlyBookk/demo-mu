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
     * @param cookie    可选，从浏览器复制的 Cookie（curl -b 后的内容），不传则用配置文件
     * @return 同步结果
     */
    RateSyncResultDTO syncFromChinaMoney(LocalDate startDate, LocalDate endDate, String cookie);

    /**
     * 同步指定货币的汇率
     *
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @param currencyCodes  指定货币编码列表
     * @param cookie         可选，从浏览器复制的 Cookie
     * @return 同步结果
     */
    RateSyncResultDTO syncSpecificCurrencies(LocalDate startDate, LocalDate endDate, List<String> currencyCodes, String cookie);

    /**
     * 同步最近N天的汇率
     * 自动同步所有启用货币
     *
     * @param days   天数
     * @param cookie 可选，从浏览器复制的 Cookie
     * @return 同步结果
     */
    RateSyncResultDTO syncRecentDays(int days, String cookie);

    /**
     * 通过粘贴的 curl 命令同步汇率
     * 用户从中国货币网页面 F12 → Network → CcprHisNew → 右键 Copy as cURL
     *
     * @param curlCommand 完整的 curl 命令
     * @return 同步结果
     */
    RateSyncResultDTO syncFromCurl(String curlCommand);
}
