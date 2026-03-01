package com.musheng.business.advertising.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 广告数据汇总（用于列表页统计指标）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "广告数据汇总")
public class AdvertisingSummaryDTO {

    @Schema(description = "发票数量")
    private Long invoiceCount;

    @Schema(description = "活动明细条数")
    private Long itemCount;

    @Schema(description = "费用合计（原币）")
    private BigDecimal totalCost;

    @Schema(description = "费用合计（人民币）")
    private BigDecimal totalCostCny;
}
