package com.musheng.business.fbashipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * FBA货件明细实体
 * 映射到 t_fba_shipment_detail 表
 * 用于管理亚马逊FBA入库货件信息，追踪货件从创建到入库完成的全过程
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment_detail")
@Schema(description = "FBA货件明细")
public class FbaShipmentDetail extends BaseEntity {

    /**
     * 货件名称(货件批次名称)
     */
    @Schema(description = "货件名称")
    private String shipmentName;

    /**
     * 货件编号(FBA货件ID,唯一)
     */
    @Schema(description = "货件编号")
    private String shipmentId;

    /**
     * 货件创建时间(来自CSV)
     */
    @Schema(description = "货件创建时间")
    private LocalDateTime createdDate;

    /**
     * 货件最后更新时间(来自CSV)
     */
    @Schema(description = "货件最后更新时间")
    private LocalDateTime lastUpdated;

    /**
     * 收货地址(亚马逊仓库代码,如YEG2)
     */
    @Schema(description = "收货地址")
    private String receivingAddress;

    /**
     * SKU种类数量
     */
    @Schema(description = "SKU种类数量")
    private Integer skuCount;

    /**
     * 预计商品数量(计划发货数量)
     */
    @Schema(description = "预计商品数量")
    private Integer expectedQuantity;

    /**
     * 找到的商品数量(实际入库数量)
     */
    @Schema(description = "找到的商品数量")
    private Integer foundQuantity;

    /**
     * 货件状态(已完成/处理中等)
     */
    @Schema(description = "货件状态")
    private String status;

    /**
     * 导入批次ID(关联导入记录表)
     */
    @Schema(description = "导入批次ID")
    private Long importBatchId;
}
