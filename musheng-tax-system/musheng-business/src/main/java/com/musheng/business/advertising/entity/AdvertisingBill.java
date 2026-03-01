package com.musheng.business.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 广告发票主表（按发票维度，用于汇总）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_advertising_bill")
@Schema(description = "广告发票主表")
public class AdvertisingBill extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String storeName;

    @Schema(description = "站点编码")
    private String siteCode;

    @Schema(description = "发票编号")
    private String invoiceNumber;

    @Schema(description = "发票状态")
    private String invoiceStatus;

    @Schema(description = "支付类型")
    private String paymentType;

    @Schema(description = "账单开始日期")
    private LocalDate billingStartDate;

    @Schema(description = "账单结束日期")
    private LocalDate billingEndDate;

    @Schema(description = "开具时间")
    private LocalDate issueDate;

    @Schema(description = "付款币种")
    private String currency;

    @Schema(description = "账单金额")
    private BigDecimal invoiceAmount;

    @Schema(description = "费用合计（明细汇总）")
    private BigDecimal totalCost;

    @Schema(description = "费用合计人民币")
    private BigDecimal totalCostCny;

    @Schema(description = "导入批次ID")
    private String importBatchId;

    @TableField(exist = false)
    @Schema(description = "明细列表")
    private List<AdvertisingBillItem> items;
}
