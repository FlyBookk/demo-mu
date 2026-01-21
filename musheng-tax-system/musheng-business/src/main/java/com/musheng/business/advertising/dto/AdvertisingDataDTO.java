package com.musheng.business.advertising.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告发票数据DTO
 *
 * @author 后端研发团队
 * @since 2026-01-20
 */
@Data
public class AdvertisingDataDTO {

    private Long id;
    private String storeName;
    private String siteCode;
    private String invoiceNumber;
    private String invoiceStatus;
    private String paymentType;
    private LocalDate billingStartDate;
    private LocalDate billingEndDate;
    private LocalDate issueDate;
    private String currency;
    private BigDecimal invoiceAmount;
    private BigDecimal cost;
    private BigDecimal otherCost;
    private String campaignName;
    private String campaignId;
    private String pricingModel;
    private Integer clicks;
    private BigDecimal avgCpc;
    private String dataSource;
    private String productList;
    private String adType;
    private String attachmentPath;
    private String remark;
    private BigDecimal exchangeRate;
    private BigDecimal amountCny;
    private String importBatchId;
}
