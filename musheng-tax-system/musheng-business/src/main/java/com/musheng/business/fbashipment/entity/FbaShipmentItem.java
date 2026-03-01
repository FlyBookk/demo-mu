package com.musheng.business.fbashipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment_item")
@Schema(description = "FBA货件明细")
public class FbaShipmentItem extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "货件主表ID")
    private Long shipmentId;

    @Schema(description = "货件单号")
    private String shipmentNo;

    @Schema(description = "内部SKU（旧数据兼容）")
    private String sku;

    @Schema(description = "亚马逊MSKU")
    private String msku;

    @Schema(description = "申报量")
    private Integer quantity;

    @Schema(description = "签收量")
    private Integer receivedQuantity;

    @Schema(description = "导入批次ID")
    private Long importBatchId;
}
