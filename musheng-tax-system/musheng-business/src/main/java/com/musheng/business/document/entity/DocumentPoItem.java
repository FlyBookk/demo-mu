package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PO采购订单明细实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_po_item")
@Schema(description = "PO采购订单明细")
public class DocumentPoItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "PO主表ID")
    private Long poId;

    @Schema(description = "FBA货件编号")
    private String shipmentNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "FBA仓库地址（仅货件首行填写）")
    private String fbaAddress;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
