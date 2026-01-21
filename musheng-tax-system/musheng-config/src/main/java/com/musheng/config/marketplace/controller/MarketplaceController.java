package com.musheng.config.marketplace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.marketplace.dto.MarketplaceQueryRequest;
import com.musheng.config.marketplace.dto.MarketplaceRequest;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 站点控制器
 */
@Tag(name = "站点管理", description = "站点管理相关接口")
@RestController
@RequestMapping("/v1/config/marketplaces")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @Operation(summary = "创建站点", description = "创建新站点")
    @PostMapping
    public Result<Marketplace> create(@Valid @RequestBody MarketplaceRequest request) {
        Marketplace data = marketplaceService.create(request);
        return Result.success(data);
    }

    @Operation(summary = "更新站点", description = "更新站点信息")
    @PutMapping("/{id}")
    public Result<Marketplace> update(
            @Parameter(description = "站点ID") @PathVariable Long id,
            @Valid @RequestBody MarketplaceRequest request) {
        Marketplace data = marketplaceService.update(id, request);
        return Result.success(data);
    }

    @Operation(summary = "删除站点", description = "删除站点")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "站点ID") @PathVariable Long id) {
        marketplaceService.delete(id);
        return Result.success();
    }

    @Operation(summary = "站点详情", description = "根据ID获取站点")
    @GetMapping("/{id}")
    public Result<Marketplace> getById(@Parameter(description = "站点ID") @PathVariable Long id) {
        Marketplace data = marketplaceService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "站点列表", description = "分页查询站点")
    @GetMapping
    public Result<PageResult<Marketplace>> list(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "站点名称") @RequestParam(required = false) String siteName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {

        MarketplaceQueryRequest queryRequest = new MarketplaceQueryRequest();
        queryRequest.setSiteCode(siteCode);
        queryRequest.setSiteName(siteName);
        queryRequest.setStatus(status);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        Page<Marketplace> pageResult = marketplaceService.list(queryRequest);
        PageResult<Marketplace> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "启用的站点", description = "获取所有启用的站点(下拉选择用)")
    @GetMapping("/enabled")
    public Result<java.util.List<Marketplace>> getEnabled() {
        java.util.List<Marketplace> list = marketplaceService.getEnabled();
        return Result.success(list);
    }
}
