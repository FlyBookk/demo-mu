package com.musheng.tiktok.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TK结算汇总实体（Statements sheet）
 *
 * @author wanhua
 * 19:42 2026年05月14日
 */
@Data
@TableName("t_tiktok_settlement")
public class TiktokSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;
    private String siteCode;
    private Long importBatchId;
    private String statementId;
    private LocalDate statementDate;
    private String currency;
    private BigDecimal settlementAmount;
    private BigDecimal revenue;
    private BigDecimal fees;
    private BigDecimal adjustmentAmount;
    private String paymentId;
    private LocalDateTime createTime;
}
