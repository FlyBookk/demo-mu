package com.musheng.tiktok.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_tiktok_document_inv")
public class TiktokDocumentInv {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String siteCode;
    private String documentNo;
    private LocalDate invDate;
    private Long settlementId;
    private String currency;
    private BigDecimal exchangeRate;
    private String sellerName;
    private String sellerAddress;
    private String buyerName;
    private String buyerAddress;
    private String sellerPhone;
    private String buyerPhone;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private String bankAddress;
    private String swiftCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
}
