package com.musheng.business.settlement.derivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 站点确认写入数据
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点确认写入数据")
public class SiteConfirmData {

    /** 站点代码（US/CA/UK/DE） */
    @Schema(description = "站点代码（US/CA/UK/DE）")
    private String siteCode;

    /** 货币代码（USD/CAD/GBP/EUR） */
    @Schema(description = "货币代码（USD/CAD/GBP/EUR）")
    private String currency;

    /** 该站点采购成本（人民币） */
    @Schema(description = "该站点采购成本（人民币）")
    private BigDecimal procurementCostCny;

    /** 周期平均汇率 */
    @Schema(description = "周期平均汇率")
    private BigDecimal averageExchangeRate;

    /** MSKU 明细列表 */
    @Schema(description = "MSKU明细列表")
    private List<MskuConfirmItem> items;
}
