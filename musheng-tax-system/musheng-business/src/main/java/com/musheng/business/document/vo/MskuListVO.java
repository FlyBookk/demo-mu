package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * MSKU列表视图对象
 *
 * <p>展示结算推导后的MSKU汇总数据，包含站点、MSKU编码、数量、单价、总价等。</p>
 *
 * @author wanhua
 * 14:00 2026年03月07日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSKU列表视图")
public class MskuListVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "货币代码")
    private String currency;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "销售数量")
    private Integer quantity;

    @Schema(description = "金额（总价）")
    private BigDecimal amount;

    @Schema(description = "结算周期起始日")
    private String periodStart;

    @Schema(description = "结算周期结束日")
    private String periodEnd;

    @Schema(description = "采购成本（人民币）")
    private BigDecimal procurementCostCny;

    @Schema(description = "周期平均汇率")
    private BigDecimal averageExchangeRate;
}
