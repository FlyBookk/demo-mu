package com.musheng.business.rate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.rate.dto.CurlSyncRequest;
import com.musheng.business.rate.dto.RateConvertRequest;
import com.musheng.business.rate.dto.RateConvertResultDTO;
import com.musheng.business.rate.dto.RateRequest;
import com.musheng.business.rate.dto.RateSyncResultDTO;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.service.RateService;
import com.musheng.business.rate.service.RateSyncService;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 汇率控制器
 * 处理汇率查询、手动录入和自动同步功能
 */
@Tag(name = "汇率管理", description = "汇率管理接口")
@RestController
@RequestMapping("/v1/business/rates")
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;
    private final RateSyncService rateSyncService;

    @Operation(summary = "汇率列表", description = "分页查询汇率")
    @GetMapping
    public Result<PageResult<ExchangeRate>> list(
            @Parameter(description = "货币编码") @RequestParam(required = false) String currencyCode,
            @Parameter(description = "开始日期(YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期(YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "数据来源(PBOC/MANUAL/IMPORT)") @RequestParam(required = false) String source,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<ExchangeRate> pageResult = rateService.list(currencyCode, startDate, endDate, source, page, size);
        PageResult<ExchangeRate> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "汇率详情", description = "根据ID获取汇率详情")
    @GetMapping("/{id}")
    public Result<ExchangeRate> getById(@Parameter(description = "汇率ID") @PathVariable Long id) {
        ExchangeRate rate = rateService.getById(id);
        return Result.success(rate);
    }

    @OperationLog(module = "汇率管理", operation = "新增汇率")
    @Operation(summary = "新增汇率", description = "手动录入汇率数据")
    @PostMapping
    public Result<ExchangeRate> create(@Valid @RequestBody RateRequest request) {
        ExchangeRate rate = rateService.create(request);
        return Result.success(rate);
    }

    @OperationLog(module = "汇率管理", operation = "修改汇率")
    @Operation(summary = "修改汇率", description = "修改已有汇率数据")
    @PutMapping("/{id}")
    public Result<ExchangeRate> update(
            @Parameter(description = "汇率ID") @PathVariable Long id,
            @Valid @RequestBody RateRequest request) {
        ExchangeRate rate = rateService.update(id, request);
        return Result.success(rate);
    }

    @OperationLog(module = "汇率管理", operation = "删除汇率")
    @Operation(summary = "删除汇率", description = "删除指定汇率")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "汇率ID") @PathVariable Long id) {
        rateService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "汇率管理", operation = "批量删除汇率")
    @Operation(summary = "批量删除汇率", description = "批量删除多个汇率")
    @DeleteMapping
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        rateService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "查询汇率", description = "根据货币编码和日期获取汇率")
    @GetMapping("/query")
    public Result<BigDecimal> getRate(
            @Parameter(description = "货币编码", required = true) @RequestParam String currencyCode,
            @Parameter(description = "日期(YYYY-MM-DD)", required = true) @RequestParam String date) {
        BigDecimal rate = rateService.getRate(currencyCode, date);
        return Result.success(rate);
    }

    @Operation(summary = "货币转换", description = "将指定货币金额转换为人民币")
    @PostMapping("/convert")
    public Result<RateConvertResultDTO> convertCurrency(@Valid @RequestBody RateConvertRequest request) {
        RateConvertResultDTO result = rateService.convertCurrency(request);
        return Result.success(result);
    }

    @OperationLog(module = "汇率管理", operation = "导入汇率")
    @Operation(summary = "导入汇率", description = "从文件导入汇率数据")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @Parameter(description = "导入文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> importResult = rateService.importData(file);
        return Result.success(importResult);
    }

    @OperationLog(module = "汇率管理", operation = "curl同步汇率")
    @Operation(summary = "通过 curl 同步汇率", description = "粘贴从中国货币网复制的完整 curl 命令，后端执行请求并解析数据")
    @PostMapping("/sync/curl")
    public Result<RateSyncResultDTO> syncFromCurl(@Valid @RequestBody CurlSyncRequest request) {
        RateSyncResultDTO result = rateSyncService.syncFromCurl(request.getCurl());
        return Result.success(result);
    }

    @Operation(summary = "导出汇率", description = "导出汇率数据为Excel文件")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "货币编码") @RequestParam(required = false) String currencyCode,
            @Parameter(description = "开始日期(YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期(YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "数据来源(PBOC/MANUAL/IMPORT)") @RequestParam(required = false) String source,
            HttpServletResponse response) {
        rateService.exportData(currencyCode, startDate, endDate, source, response);
    }
}
