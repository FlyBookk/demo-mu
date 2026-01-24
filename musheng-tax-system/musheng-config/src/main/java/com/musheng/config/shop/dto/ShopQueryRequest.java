package com.musheng.config.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 店铺查询请求 DTO
 */
@Data
@Schema(description = "店铺查询请求")
public class ShopQueryRequest {

    @Schema(description = "店铺编码（模糊匹配）", example = "SHOP")
    private String shopCode;

    @Schema(description = "店铺名称（模糊匹配）", example = "慕声")
    private String shopName;

    @Schema(description = "状态(1启用/0禁用)", example = "1")
    private Integer status;

    @Schema(description = "页码（从1开始）", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
