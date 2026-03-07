package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MSKU数据更新请求
 *
 * @author wanhua
 * 14:00 2026年03月07日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSKU数据更新请求")
public class MskuUpdateRequest {

    @NotNull(message = "记录ID不能为空")
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "销售数量")
    private Integer quantity;

    @Schema(description = "采购成本（人民币）")
    private BigDecimal procurementCostCny;

    @Schema(description = "周期平均汇率")
    private BigDecimal averageExchangeRate;
}
