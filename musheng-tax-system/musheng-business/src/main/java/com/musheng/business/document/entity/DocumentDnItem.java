package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 送货单明细实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_dn_item")
@Schema(description = "送货单明细")
public class DocumentDnItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "DN主表ID")
    private Long dnId;

    @Schema(description = "行号（1,2,3...）")
    private Integer lineNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "对应货件编号（备注列）")
    private String shipmentNo;
}
