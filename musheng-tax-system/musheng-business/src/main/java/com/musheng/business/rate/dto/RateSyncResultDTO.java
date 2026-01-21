package com.musheng.business.rate.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率同步结果DTO
 */
@Data
@Builder
public class RateSyncResultDTO {

    /**
     * 同步是否成功
     */
    private Boolean success;

    /**
     * 同步开始日期
     */
    private LocalDate startDate;

    /**
     * 同步结束日期
     */
    private LocalDate endDate;

    /**
     * 同步的货币列表
     */
    private List<String> currencyCodes;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 新增记录数
     */
    private Integer insertCount;

    /**
     * 更新记录数
     */
    private Integer updateCount;

    /**
     * 失败记录数
     */
    private Integer failCount;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 同步耗时(毫秒)
     */
    private Long durationMs;

    /**
     * 同步消息
     */
    private String message;
}
