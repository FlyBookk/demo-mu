package com.musheng.config.currency.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.currency.dto.CurrencyQueryRequest;
import com.musheng.config.currency.dto.CurrencyRequest;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 货币控制器
 */
@Tag(name = "货币管理", description = "货币管理相关接口")
@RestController
@RequestMapping("/v1/config/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(summary = "创建货币", description = "创建新货币")
    @PostMapping
    public Result<Currency> create(@Valid @RequestBody CurrencyRequest request) {
        Currency data = currencyService.create(request);
        return Result.success(data);
    }

    @Operation(summary = "更新货币", description = "更新货币信息")
    @PutMapping("/{id}")
    public Result<Currency> update(
            @Parameter(description = "货币ID") @PathVariable("id") Long id,
            @Valid @RequestBody CurrencyRequest request) {
        Currency data = currencyService.update(id, request);
        return Result.success(data);
    }

    @Operation(summary = "删除货币", description = "删除货币")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "货币ID") @PathVariable("id") Long id) {
        currencyService.delete(id);
        return Result.success();
    }

    @Operation(summary = "货币详情", description = "根据ID获取货币")
    @GetMapping("/{id}")
    public Result<Currency> getById(@Parameter(description = "货币ID") @PathVariable("id") Long id) {
        Currency data = currencyService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "货币列表", description = "分页查询货币")
    @GetMapping
    public Result<PageResult<Currency>> list(
            @Parameter(description = "货币编码") @RequestParam(name = "currencyCode", required = false) String currencyCode,
            @Parameter(description = "货币名称") @RequestParam(name = "currencyName", required = false) String currencyName,
            @Parameter(description = "状态") @RequestParam(name = "status", required = false) Integer status,
            @Parameter(description = "页码(从1开始)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(name = "size", defaultValue = "10") int size) {

        CurrencyQueryRequest queryRequest = new CurrencyQueryRequest();
        queryRequest.setCurrencyCode(currencyCode);
        queryRequest.setCurrencyName(currencyName);
        queryRequest.setStatus(status);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        Page<Currency> pageResult = currencyService.list(queryRequest);
        PageResult<Currency> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "启用的货币", description = "获取所有启用的货币(下拉选择用)")
    @GetMapping("/enabled")
    public Result<java.util.List<Currency>> getEnabled() {
        java.util.List<Currency> list = currencyService.getEnabled();
        return Result.success(list);
    }
}
