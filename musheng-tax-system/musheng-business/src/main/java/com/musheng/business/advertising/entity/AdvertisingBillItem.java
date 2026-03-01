package com.musheng.business.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告发票明细表（按广告活动维度，可复核）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_advertising_bill_item")
@Schema(description = "广告发票明细")
public class AdvertisingBillItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "发票主表ID")
    private Long billId;

    @Schema(description = "发票编号（冗余）")
    private String invoiceNumber;

    @Schema(description = "广告活动")
    private String campaignName;

    @Schema(description = "活动ID")
    private String campaignId;

    @Schema(description = "计价方式")
    private String pricingModel;

    @Schema(description = "点击")
    private Integer clicks;

    @Schema(description = "平均点击单价")
    private BigDecimal avgCpc;

    @Schema(description = "费用")
    private BigDecimal cost;

    @Schema(description = "其他费分摊")
    private BigDecimal otherCost;

    @Schema(description = "取值来源")
    private String dataSource;

    @Schema(description = "承担商品")
    private String productList;

    @Schema(description = "广告类型")
    private String adType;

    @Schema(description = "汇率")
    private BigDecimal exchangeRate;

    @Schema(description = "汇率日期")
    private LocalDate exchangeRateDate;

    @Schema(description = "费用人民币")
    private BigDecimal amountCny;

    @Schema(description = "导入批次ID")
    private String importBatchId;
}
