package com.musheng.business.advertising.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告数据导入请求DTO（单条记录）
 * 用于导入详细的广告发票数据
 *
 * @author 后端研发团队
 * @since 2026-01-20
 */
@Data
@Schema(description = "广告数据导入请求")
public class AdvertisingDataImportRequest {

    @NotBlank(message = "店铺名称不能为空")
    @Schema(description = "店铺名称", example = "慕声北美-CA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeName;

    @Schema(description = "站点编码（可从店铺名称自动推断）", example = "CA")
    private String siteCode;

    @NotBlank(message = "发票编号不能为空")
    @Schema(description = "发票编号（去重关键字段）", example = "1871317CQPA25", requiredMode = Schema.RequiredMode.REQUIRED)
    private String invoiceNumber;

    @NotBlank(message = "发票状态不能为空")
    @Schema(description = "发票状态", example = "PAID_IN_FULL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String invoiceStatus;

    @Schema(description = "支付类型", example = "CREDIT_CARD")
    private String paymentType;

    @NotNull(message = "账单开始日期不能为空")
    @Schema(description = "账单开始日期", example = "2025-06-30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billingStartDate;

    @NotNull(message = "账单结束日期不能为空")
    @Schema(description = "账单结束日期", example = "2025-07-03", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billingEndDate;

    @NotNull(message = "开具时间不能为空")
    @Schema(description = "发票开具日期", example = "2025-07-02", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate issueDate;

    @NotBlank(message = "付款币种不能为空")
    @Schema(description = "付款币种", example = "CAD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotNull(message = "账单金额不能为空")
    @Schema(description = "账单金额（发票总金额，允许负数用于抵冲）", example = "73.23", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal invoiceAmount;

    @NotNull(message = "费用不能为空")
    @Schema(description = "费用（实际花费，允许负数用于正负抵冲）", example = "45.4", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal cost;

    @Schema(description = "其他费分摊", example = "0")
    private BigDecimal otherCost;

    @Schema(description = "广告活动名称", example = "D77自动")
    private String campaignName;

    @Schema(description = "活动ID", example = "302861713011712")
    private String campaignId;

    @Schema(description = "计价方式", example = "CPC")
    private String pricingModel;

    @Schema(description = "点击次数", example = "15")
    private Integer clicks;

    @Schema(description = "平均点击单价", example = "3.04")
    private BigDecimal avgCpc;

    @Schema(description = "取值来源", example = "业务报告")
    private String dataSource;

    @Schema(description = "承担商品（逗号分隔）", example = "MSCA-D77-7-6,MSCA-D77-5-2")
    private String productList;

    @Schema(description = "广告类型", example = "SPONSORED PRODUCTS")
    private String adType;

    @Schema(description = "发票附件路径")
    private String attachmentPath;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "汇率")
    private BigDecimal exchangeRate;
}
