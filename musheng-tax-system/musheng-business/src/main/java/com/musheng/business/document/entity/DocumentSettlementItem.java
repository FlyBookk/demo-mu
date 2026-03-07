package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算单明细实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_settlement_item")
@Schema(description = "结算单明细")
public class DocumentSettlementItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "结算单主表ID")
    private Long settlementId;

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

    @Schema(description = "金额（数量×单价）")
    private BigDecimal amount;
}
