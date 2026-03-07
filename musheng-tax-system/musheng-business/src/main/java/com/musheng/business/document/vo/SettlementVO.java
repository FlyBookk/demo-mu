package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 结算单详情视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算单详情视图")
public class SettlementVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "结算日")
    private LocalDate settlementDate;

    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "站点序号")
    private String siteSequence;

    @Schema(description = "买方名称")
    private String buyerName;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "结算单明细列表")
    private List<SettlementItemVO> items;
}
