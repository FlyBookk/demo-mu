package com.musheng.tiktok.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TK单据交易方配置实体
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Data
@TableName("t_tiktok_party_config")
public class TiktokPartyConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String siteCode;
    private String buyerName;
    private String buyerAddress;
    private String buyerPhone;
    private String buyerNameEn;
    private String sellerName;
    private String sellerAddress;
    private String sellerPhone;
    private String supplierName;
    private String customerNameTc;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private String bankAddress;
    private String swiftCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Boolean deleted;
}
