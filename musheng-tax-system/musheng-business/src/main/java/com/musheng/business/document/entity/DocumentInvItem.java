package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * INV发票明细实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_inv_item")
@Schema(description = "INV发票明细")
public class DocumentInvItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "INV主表ID")
    private Long invId;

    @Schema(description = "序号")
    private Integer lineNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "金额")
    private BigDecimal amount;
}
