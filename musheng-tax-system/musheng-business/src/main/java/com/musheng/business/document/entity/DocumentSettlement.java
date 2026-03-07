package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算单主表实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_settlement")
@Schema(description = "结算单主表")
public class DocumentSettlement extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "单据编号（如20250902001）")
    private String documentNo;

    @Schema(description = "结算日")
    private LocalDate settlementDate;

    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @Schema(description = "站点代码（USD/CAD/GBP/EUR）")
    private String siteCode;

    @Schema(description = "站点序号（001-004）")
    private String siteSequence;

    @Schema(description = "买方名称")
    private String buyerName;

    @Schema(description = "买方地址")
    private String buyerAddress;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;
}
