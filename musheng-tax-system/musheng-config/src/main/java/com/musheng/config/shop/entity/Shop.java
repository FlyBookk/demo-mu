package com.musheng.config.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shop")
@Schema(description = "店铺实体")
public class Shop extends BaseEntity {

    /**
     * 店铺编码
     */
    @Schema(description = "店铺编码")
    private String shopCode;

    /**
     * 店铺名称
     */
    @Schema(description = "店铺名称")
    private String shopName;

    /**
     * 亚马逊卖家ID
     */
    @Schema(description = "亚马逊卖家ID")
    private String sellerId;

    /**
     * 公司名称
     */
    @Schema(description = "公司名称")
    private String companyName;

    /**
     * 统一社会信用代码
     */
    @Schema(description = "统一社会信用代码")
    private String taxId;

    /**
     * 状态(1启用/0禁用)
     */
    @Schema(description = "状态(1启用/0禁用)")
    private Integer status;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
