package com.musheng.business.fbashipment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment")
@Schema(description = "FBA货件主表")
public class FbaShipment extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "货件单号")
    private String shipmentId;

    @Schema(description = "货件名称")
    private String shipmentName;

    @Schema(description = "货件状态")
    private String status;

    @Schema(description = "物流中心编码")
    private String warehouseCode;

    @Schema(description = "店铺名称（旧数据兼容）")
    private String shopName;

    @Schema(description = "收件国家")
    private String country;

    @Schema(description = "收件州/省")
    private String state;

    @Schema(description = "收件城市")
    private String city;

    @Schema(description = "收件街道地址")
    private String streetAddress;

    @Schema(description = "收件门牌号")
    private String houseNumber;

    @Schema(description = "货件创建时间")
    private LocalDateTime createdDate;

    @Schema(description = "货件更新时间")
    private LocalDateTime updatedDate;

    @Schema(description = "SKU种类数量")
    private Integer skuCount;

    @Schema(description = "总申报量")
    private Integer totalQuantity;

    @Schema(description = "总签收量")
    private Integer totalReceivedQuantity;

    @Schema(description = "收件人")
    private String recipient;

    @Schema(description = "收件邮编")
    private String postalCode;

    @Schema(description = "导入批次ID")
    private Long importBatchId;

    @TableField(exist = false)
    @Schema(description = "SKU明细列表")
    private List<FbaShipmentItem> items;
}
