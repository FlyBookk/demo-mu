package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * INV发票主表实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_inv")
@Schema(description = "INV发票主表")
public class DocumentInv extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "INV编号")
    private String documentNo;

    @Schema(description = "INV日期（结算日+1工作日）")
    private LocalDate invDate;

    @Schema(description = "关联结算单ID")
    private Long settlementId;

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "站点序号（001-004）")
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
}
