package com.musheng.business.settlement.derivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 站点采购成本输入
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点采购成本输入")
public class SiteCostInput {

    /** 站点代码（US/CA/UK/DE） */
    @Schema(description = "站点代码（US/CA/UK/DE）")
    private String siteCode;

    /** 采购成本（人民币） */
    @Schema(description = "采购成本（人民币）")
    private BigDecimal costCny;
}
