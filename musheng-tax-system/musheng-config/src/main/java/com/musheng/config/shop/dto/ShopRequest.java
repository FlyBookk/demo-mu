package com.musheng.config.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺请求 DTO
 */
@Data
@Schema(description = "店铺请求")
public class ShopRequest {

    @NotBlank(message = "店铺编码不能为空")
    @Size(max = 50, message = "店铺编码最多50字符")
    @Schema(description = "店铺编码", example = "SHOP001")
    private String shopCode;

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多100字符")
    @Schema(description = "店铺名称", example = "慕声美国店")
    private String shopName;

    @Size(max = 50, message = "卖家ID最多50字符")
    @Schema(description = "亚马逊卖家ID", example = "APNDJLWNA7H88")
    private String sellerId;

    @Size(max = 200, message = "公司名称最多200字符")
    @Schema(description = "公司名称", example = "东莞市慕声商贸有限公司")
    private String companyName;

    @Size(max = 50, message = "统一社会信用代码最多50字符")
    @Schema(description = "统一社会信用代码", example = "91441900MA4WNG4C6H")
    private String taxId;

    @Schema(description = "状态(1启用/0禁用)", example = "1")
    private Integer status = 1;

    @Size(max = 500, message = "备注最多500字符")
    @Schema(description = "备注")
    private String remark;
}
