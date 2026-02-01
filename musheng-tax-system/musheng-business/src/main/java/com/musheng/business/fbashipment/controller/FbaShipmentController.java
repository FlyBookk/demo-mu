package com.musheng.business.fbashipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.service.FbaShipmentService;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.annotation.RequireShop;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * FBA货件控制器
 * 处理FBA货件的导入、查询、删除等操作
 */
@Tag(name = "FBA货件管理", description = "FBA货件管理接口")
@RestController
@RequestMapping("/v1/business/fba-shipment")
@RequiredArgsConstructor
@RequireShop
public class FbaShipmentController {

    private final FbaShipmentService fbaShipmentService;

    @OperationLog(module = "FBA货件", operation = "导入FBA货件")
    @Operation(summary = "导入FBA货件", description = "从Excel文件导入FBA货件明细")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> importResult = fbaShipmentService.importData(file);
        return Result.success(importResult);
    }

    @OperationLog(module = "FBA货件", operation = "批量导入FBA货件")
    @Operation(summary = "批量导入FBA货件", description = "从多个Excel文件批量导入FBA货件明细，支持幂等性")
    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImportData(
            @Parameter(description = "Excel文件列表") @RequestParam("files") List<MultipartFile> files) {
        Map<String, Object> batchResult = fbaShipmentService.batchImportData(files);
        return Result.success(batchResult);
    }

    @Operation(summary = "货件列表", description = "分页查询FBA货件列表")
    @GetMapping("/list")
    public Result<PageResult<FbaShipment>> list(
            @Parameter(description = "货件单号") @RequestParam(required = false) String shipmentId,
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Page<FbaShipment> pageResult = fbaShipmentService.list(
                shipmentId, shopName, country, startDate, endDate, page, size);
        PageResult<FbaShipment> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "货件详情", description = "根据ID获取货件详情（包含SKU明细）")
    @GetMapping("/{id}")
    public Result<FbaShipment> getById(
            @Parameter(description = "货件ID") @PathVariable Long id) {
        FbaShipment shipment = fbaShipmentService.getById(id);
        return Result.success(shipment);
    }

    @OperationLog(module = "FBA货件", operation = "删除FBA货件")
    @Operation(summary = "删除货件", description = "根据ID删除货件（级联删除明细）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "货件ID") @PathVariable Long id) {
        fbaShipmentService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "FBA货件", operation = "批量删除FBA货件")
    @Operation(summary = "批量删除货件", description = "根据ID列表批量删除货件")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(
            @Parameter(description = "货件ID列表") @RequestBody List<Long> ids) {
        fbaShipmentService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "统计汇总", description = "获取FBA货件统计汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = fbaShipmentService.getSummary(shopName, country, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "导出货件", description = "导出FBA货件到Excel")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) {
        fbaShipmentService.exportData(shopName, country, startDate, endDate, response);
    }

    @Operation(summary = "获取国家列表", description = "获取所有已导入货件的国家列表（去重）")
    @GetMapping("/countries")
    public Result<List<String>> getCountryList() {
        List<String> countries = fbaShipmentService.getCountryList();
        return Result.success(countries);
    }

    @Operation(summary = "获取店铺列表", description = "获取所有已导入货件的店铺名称列表（去重）")
    @GetMapping("/shop-names")
    public Result<List<String>> getShopNameList() {
        List<String> shopNames = fbaShipmentService.getShopNameList();
        return Result.success(shopNames);
    }
}
