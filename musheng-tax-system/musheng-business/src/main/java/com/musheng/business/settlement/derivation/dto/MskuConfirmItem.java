package com.musheng.business.settlement.derivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MSKU 确认写入明细项
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSKU确认写入明细项")
public class MskuConfirmItem {

    /** MSKU 编码 */
    @Schema(description = "MSKU编码")
    private String msku;

    /** 净销售数量 */
    @Schema(description = "净销售数量")
    private Integer quantity;

    /** 单价（4位小数） */
    @Schema(description = "单价（4位小数）")
    private BigDecimal unitPrice;

    /** 金额 = 数量 × 单价（4位小数） */
    @Schema(description = "金额 = 数量 × 单价（4位小数）")
    private BigDecimal amount;
}
