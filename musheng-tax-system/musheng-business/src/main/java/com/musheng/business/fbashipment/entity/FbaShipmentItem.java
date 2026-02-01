package com.musheng.business.fbashipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * FBA货件明细实体
 * 映射到 t_fba_shipment_item 表
 * 用于管理FBA货件中的SKU级别明细信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment_item")
@Schema(description = "FBA货件明细")
public class FbaShipmentItem extends BaseEntity {

    /**
     * 店铺ID（用于数据隔离）
     */
    @Schema(description = "店铺ID")
    private Long shopId;

    /**
     * 货件主表ID（外键）
     */
    @Schema(description = "货件主表ID")
    private Long shipmentId;

    /**
     * 货件单号（冗余字段，便于查询）
     */
    @Schema(description = "货件单号")
    private String shipmentNo;

    /**
     * 内部SKU编码
     */
    @Schema(description = "内部SKU")
    private String sku;

    /**
     * 亚马逊MSKU
     */
    @Schema(description = "亚马逊MSKU")
    private String msku;

    /**
     * 发货量
     */
    @Schema(description = "发货量")
    private Integer quantity;

    /**
     * 导入批次ID（关联导入记录表）
     */
    @Schema(description = "导入批次ID")
    private Long importBatchId;
}
