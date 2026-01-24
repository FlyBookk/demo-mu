package com.musheng.config.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.Result;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;
import com.musheng.config.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺管理控制器
 */
@Tag(name = "店铺管理", description = "店铺 CRUD 接口")
@RestController
@RequestMapping("/v1/config/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @Operation(summary = "分页查询店铺")
    @GetMapping("/page")
    public Result<IPage<Shop>> page(ShopQueryRequest request) {
        return Result.success(shopService.queryPage(request));
    }

    @Operation(summary = "获取启用的店铺列表（下拉选项）")
    @GetMapping("/options")
    public Result<List<Shop>> options() {
        return Result.success(shopService.listEnabled());
    }

    @Operation(summary = "获取店铺详情")
    @GetMapping("/{id}")
    public Result<Shop> getById(
            @Parameter(description = "店铺ID") @PathVariable Long id) {
        return Result.success(shopService.getById(id));
    }

    @Operation(summary = "新增店铺")
    @PostMapping
    @OperationLog(module = "店铺管理", operation = "新增店铺")
    public Result<Long> create(@Valid @RequestBody ShopRequest request) {
        return Result.success(shopService.createShop(request));
    }

    @Operation(summary = "更新店铺")
    @PutMapping("/{id}")
    @OperationLog(module = "店铺管理", operation = "更新店铺")
    public Result<Void> update(
            @Parameter(description = "店铺ID") @PathVariable Long id,
            @Valid @RequestBody ShopRequest request) {
        shopService.updateShop(id, request);
        return Result.success();
    }

    @Operation(summary = "删除店铺")
    @DeleteMapping("/{id}")
    @OperationLog(module = "店铺管理", operation = "删除店铺")
    public Result<Void> delete(
            @Parameter(description = "店铺ID") @PathVariable Long id) {
        shopService.deleteShop(id);
        return Result.success();
    }
}
