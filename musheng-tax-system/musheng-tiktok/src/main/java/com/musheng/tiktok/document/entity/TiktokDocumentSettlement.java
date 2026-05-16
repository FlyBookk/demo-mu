package com.musheng.tiktok.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_tiktok_document_settlement")
public class TiktokDocumentSettlement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String siteCode;
    private String documentNo;
    private LocalDate settlementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String currency;
    private BigDecimal exchangeRate;
    private String buyerName;
    private String buyerAddress;
    private String sellerName;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
}
