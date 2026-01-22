package com.musheng.business.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.*;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.service.SalesDataService;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.enums.SalesSourceType;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 销售数据控制器
 * 处理销售数据增删改查和导入功能
 */
@Tag(name = "销售数据", description = "销售数据管理接口")
@RestController
@RequestMapping("/v1/business/sales")
@RequiredArgsConstructor
public class SalesDataController {

    private final SalesDataService salesDataService;

    @Operation(summary = "销售数据列表", description = "分页查询销售数据")
    @PostMapping("/list")
    public Result<PageResult<SalesData>> list(@RequestBody SalesQueryRequest request) {
        Page<SalesData> pageResult = salesDataService.list(request);
        PageResult<SalesData> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "销售数据详情", description = "根据ID获取销售数据")
    @GetMapping("/{id}")
    public Result<SalesData> getById(
            @Parameter(description = "销售数据ID") @PathVariable Long id) {
        SalesData data = salesDataService.getById(id);
        return Result.success(data);
    }

    @OperationLog(module = "销售数据", operation = "导入销售数据")
    @Operation(summary = "导入销售数据", description = "从文件导入销售数据")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @Parameter(description = "站点编码") @RequestParam String siteCode,
            @Parameter(description = "导入文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> importResult = salesDataService.importData(siteCode, file);
        return Result.success(importResult);
    }

    @OperationLog(module = "销售数据", operation = "删除销售数据")
    @Operation(summary = "删除销售数据", description = "根据ID删除销售数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "销售数据ID") @PathVariable Long id) {
        salesDataService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "销售数据", operation = "批量删除销售数据")
    @Operation(summary = "批量删除销售数据", description = "根据ID列表批量删除销售数据")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(
            @Parameter(description = "销售数据ID列表") @RequestBody List<Long> ids) {
        salesDataService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "销售数据汇总", description = "获取销售数据统计汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "交易分类") @RequestParam(required = false) String transactionCategory,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = salesDataService.getSummary(siteCode, transactionCategory, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "按类型统计", description = "按交易类型分组统计销售数据")
    @GetMapping("/stat-by-type")
    public Result<List<Map<String, Object>>> getStatByType(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> stats = salesDataService.getStatByType(siteCode, startDate, endDate);
        return Result.success(stats);
    }

    @Operation(summary = "导出销售数据", description = "导出销售数据到Excel")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "交易分类") @RequestParam(required = false) String transactionCategory,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) {
        salesDataService.exportData(siteCode, transactionCategory, startDate, endDate, response);
    }
    
    // ========== 双格式导入相关接口 ==========
    
    @Operation(summary = "上传销售数据文件", description = "上传文件并解析表头，返回源字段列表")
    @PostMapping("/upload")
    public Result<SalesUploadResult> uploadFile(
            @Parameter(description = "数据源类型", required = true) @RequestParam SalesSourceType sourceType,
            @Parameter(description = "站点编码（ERP数据需要预选）") @RequestParam(required = false) String siteCode,
            @Parameter(description = "上传文件", required = true) @RequestParam("file") MultipartFile file) {
        SalesUploadResult result = salesDataService.uploadFile(file, sourceType, siteCode);
        return Result.success(result);
    }
    
    @Operation(summary = "预览导入数据", description = "根据模板预览解析后的数据（前10行）")
    @PostMapping("/preview")
    public Result<SalesPreviewResult> previewImport(
            @Valid @RequestBody SalesPreviewRequest request) {
        SalesPreviewResult result = salesDataService.previewImport(request);
        return Result.success(result);
    }
    
    @OperationLog(module = "销售数据", operation = "执行双格式导入")
    @Operation(summary = "执行导入", description = "根据模板配置执行销售数据导入")
    @PostMapping("/import/execute")
    public Result<SalesImportResult> executeImport(
            @Valid @RequestBody SalesImportRequest request) {
        SalesImportResult result = salesDataService.executeImport(request);
        return Result.success(result);
    }
    
    @Operation(summary = "获取导入进度", description = "获取导入任务的实时进度")
    @GetMapping("/import/progress/{batchNo}")
    public Result<SalesImportProgress> getImportProgress(
            @Parameter(description = "导入批次号", required = true) @PathVariable String batchNo) {
        SalesImportProgress progress = salesDataService.getImportProgress(batchNo);
        return Result.success(progress);
    }
}
