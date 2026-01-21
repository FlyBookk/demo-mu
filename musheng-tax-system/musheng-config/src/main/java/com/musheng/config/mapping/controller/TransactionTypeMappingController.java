package com.musheng.config.mapping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.mapping.dto.TransactionTypeMappingQueryRequest;
import com.musheng.config.mapping.dto.TransactionTypeMappingRequest;
import com.musheng.config.mapping.entity.TransactionTypeMapping;
import com.musheng.config.mapping.service.TransactionTypeMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 交易类型映射控制器
 */
@Tag(name = "交易类型映射", description = "交易类型映射管理接口")
@RestController
@RequestMapping("/v1/config/transaction-type-mappings")
@RequiredArgsConstructor
public class TransactionTypeMappingController {

    private final TransactionTypeMappingService transactionTypeMappingService;

    @Operation(summary = "创建映射", description = "创建交易类型映射")
    @PostMapping
    public Result<TransactionTypeMapping> create(@Valid @RequestBody TransactionTypeMappingRequest request) {
        TransactionTypeMapping data = transactionTypeMappingService.create(request);
        return Result.success(data);
    }

    @Operation(summary = "更新映射", description = "更新交易类型映射")
    @PutMapping("/{id}")
    public Result<TransactionTypeMapping> update(
            @Parameter(description = "映射ID") @PathVariable Long id,
            @Valid @RequestBody TransactionTypeMappingRequest request) {
        TransactionTypeMapping data = transactionTypeMappingService.update(id, request);
        return Result.success(data);
    }

    @Operation(summary = "删除映射", description = "删除交易类型映射")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "映射ID") @PathVariable Long id) {
        transactionTypeMappingService.delete(id);
        return Result.success();
    }

    @Operation(summary = "映射详情", description = "根据ID获取交易类型映射")
    @GetMapping("/{id}")
    public Result<TransactionTypeMapping> getById(@Parameter(description = "映射ID") @PathVariable Long id) {
        TransactionTypeMapping data = transactionTypeMappingService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "映射列表", description = "分页查询交易类型映射")
    @GetMapping
    public Result<PageResult<TransactionTypeMapping>> list(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "原始类型") @RequestParam(required = false) String originalType,
            @Parameter(description = "映射类型") @RequestParam(required = false) String mappedType,
            @Parameter(description = "标准分类") @RequestParam(required = false) String standardCategory,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {

        TransactionTypeMappingQueryRequest queryRequest = new TransactionTypeMappingQueryRequest();
        queryRequest.setSiteCode(siteCode);
        queryRequest.setOriginalType(originalType);
        queryRequest.setMappedType(mappedType);
        queryRequest.setStandardCategory(standardCategory);
        queryRequest.setStatus(status);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        Page<TransactionTypeMapping> pageResult = transactionTypeMappingService.list(queryRequest);
        PageResult<TransactionTypeMapping> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }
}
