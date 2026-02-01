package com.musheng.business.fbashipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.service.FbaShipmentItemService;
import com.musheng.common.annotation.RequireShop;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * FBA货件明细控制器
 * 提供SKU级别的全局查询功能
 */
@Tag(name = "FBA货件明细管理", description = "FBA货件SKU明细全局查询接口")
@RestController
@RequestMapping("/v1/business/fba-shipment-item")
@RequiredArgsConstructor
@RequireShop
public class FbaShipmentItemController {

    private final FbaShipmentItemService fbaShipmentItemService;

    @Operation(summary = "SKU明细列表", description = "分页查询FBA货件SKU明细列表（全局视图）")
    @GetMapping("/list")
    public Result<PageResult<FbaShipmentItem>> list(
            @Parameter(description = "货件单号") @RequestParam(required = false) String shipmentNo,
            @Parameter(description = "内部SKU") @RequestParam(required = false) String sku,
            @Parameter(description = "亚马逊MSKU") @RequestParam(required = false) String msku,
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Page<FbaShipmentItem> pageResult = fbaShipmentItemService.listItems(
                shipmentNo, sku, msku, shopName, country, startDate, endDate, page, size);
        PageResult<FbaShipmentItem> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }
}
