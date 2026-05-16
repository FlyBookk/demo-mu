package com.musheng.tiktok.document.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TK单据列表视图
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Data
@Builder
public class TiktokDocumentListVO {
    private Long id;
    private String documentType;
    private String documentNo;
    private String siteCode;
    private LocalDate documentDate;
    private String buyerName;
    private String sellerName;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}
