package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 结算单明细视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算单明细视图")
public class SettlementItemVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "序号")
    private Integer lineNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "货币代码")
    private String currency;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "销售数量")
    private Integer quantity;

    @Schema(description = "金额")
    private BigDecimal amount;
}
