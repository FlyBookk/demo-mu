package com.musheng.business.fbashipment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FBA货件主表实体
 * 映射到 t_fba_shipment 表
 * 用于管理亚马逊FBA货件的汇总信息，一个货件对应多个SKU明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment")
@Schema(description = "FBA货件主表")
public class FbaShipment extends BaseEntity {

    /**
     * 店铺ID（用于数据隔离）
     */
    @Schema(description = "店铺ID")
    private Long shopId;

    /**
     * 货件单号（FBA货件编号，如：FBA15KYVTSMJ）
     */
    @Schema(description = "货件单号")
    private String shipmentId;

    /**
     * 物流中心编码（亚马逊仓库地址）
     */
    @Schema(description = "物流中心编码")
    private String warehouseCode;

    /**
     * 店铺名称（如：慕声欧洲-UK）
     */
    @Schema(description = "店铺名称")
    private String shopName;

    /**
     * 国家（如：英国）
     */
    @Schema(description = "国家")
    private String country;

    /**
     * 货件创建时间
     */
    @Schema(description = "货件创建时间")
    private LocalDateTime createdDate;

    /**
     * SKU种类数量（自动计算）
     */
    @Schema(description = "SKU种类数量")
    private Integer skuCount;

    /**
     * 总发货量（自动汇总）
     */
    @Schema(description = "总发货量")
    private Integer totalQuantity;

    /**
     * 导入批次ID（关联导入记录表）
     */
    @Schema(description = "导入批次ID")
    private Long importBatchId;

    /**
     * 关联的SKU明细列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "SKU明细列表")
    private List<FbaShipmentItem> items;
}
