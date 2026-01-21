package com.musheng.business.rate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 中国外汇交易中心API响应DTO
 */
@Data
public class ChinaMoneyRateDTO {

    /**
     * 响应状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 数据列表
     */
    private List<RateRecord> records;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 汇率记录
     */
    @Data
    public static class RateRecord {
        /**
         * 日期
         */
        @JsonProperty("date")
        private String date;

        /**
         * 货币对(例如: USD/CNY, EUR/CNY)
         */
        @JsonProperty("vrtEfcBuyPrcCny")
        private String currencyPair;

        /**
         * 汇率值
         */
        @JsonProperty("prcCny")
        private String price;

        /**
         * 涨跌幅
         */
        @JsonProperty("riseDropCny")
        private String riseDropCny;
    }
}
