package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售数据查询请求
 */
@Data
@Schema(description = "销售数据查询请求")
public class SalesQueryRequest {

    @Schema(description = "数据来源(ORIGINAL-原始数据, ERP-ERP结算)")
    private String sourceType;

    @Schema(description = "站点编码")
    private String siteCode;

    @Schema(description = "结算ID")
    private String settlementId;

    @Schema(description = "交易分类(income/refund/fee/adjustment/other)")
    private String transactionCategory;

    @Schema(description = "交易类型")
    private String transactionType;

    @Schema(description = "订单号/SKU/ASIN 关键字")
    private String keyword;

    @Schema(description = "开始日期 (YYYY-MM-DD)")
    private String startDate;

    @Schema(description = "结束日期 (YYYY-MM-DD)")
    private String endDate;

    @Schema(description = "页码(从1开始)", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;
}
