package com.musheng.business.settlement.derivation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 站点推导结果
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点推导结果")
public class SiteDerivationResult {

    /** 站点代码（US/CA/UK/DE） */
    @Schema(description = "站点代码（US/CA/UK/DE）")
    private String siteCode;

    /** 货币代码（USD/CAD/GBP/EUR） */
    @Schema(description = "货币代码（USD/CAD/GBP/EUR）")
    private String currency;

    /** 采购成本（人民币） */
    @Schema(description = "采购成本（人民币）")
    private BigDecimal procurementCostCny;

    /** 周期平均汇率 */
    @Schema(description = "周期平均汇率")
    private BigDecimal averageExchangeRate;

    /** 采购成本（原币）= 采购成本（人民币）÷ 周期平均汇率 */
    @Schema(description = "采购成本（原币）")
    private BigDecimal procurementCostOriginal;

    /** 站点总净销售数量 */
    @Schema(description = "站点总净销售数量")
    private Integer totalQuantity;

    /** 站点合计金额 */
    @Schema(description = "站点合计金额")
    private BigDecimal totalAmount;

    /** MSKU 推导明细列表 */
    @Schema(description = "MSKU推导明细列表")
    private List<MskuDerivationItem> items;
}
