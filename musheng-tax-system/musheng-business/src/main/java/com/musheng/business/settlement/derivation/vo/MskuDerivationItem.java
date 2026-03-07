package com.musheng.business.settlement.derivation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MSKU 推导明细项
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSKU推导明细项")
public class MskuDerivationItem {

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

    /** 是否已调整（初始为 false，财务修改单价后变为 true） */
    @Schema(description = "是否已调整（初始为false，财务修改单价后变为true）")
    private boolean adjusted;
}
