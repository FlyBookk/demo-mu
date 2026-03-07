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
 * INV发票详情视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "INV发票详情视图")
public class InvVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "INV编号")
    private String documentNo;

    @Schema(description = "INV日期")
    private LocalDate invDate;

    @Schema(description = "关联结算单ID")
    private Long settlementId;

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "站点序号")
    private String siteSequence;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "卖方地址")
    private String sellerAddress;

    @Schema(description = "买方英文名称")
    private String buyerName;

    @Schema(description = "买方英文地址")
    private String buyerAddress;

    @Schema(description = "卖方电话")
    private String sellerPhone;

    @Schema(description = "买方电话")
    private String buyerPhone;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "银行账户名")
    private String bankAccountName;

    @Schema(description = "银行账号")
    private String bankAccountNumber;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "银行地址")
    private String bankAddress;

    @Schema(description = "SWIFT代码")
    private String swiftCode;

    @Schema(description = "INV明细列表")
    private List<InvItemVO> items;
}
