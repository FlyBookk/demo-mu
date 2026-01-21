package com.musheng.business.fbashipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipmentDetail;
import com.musheng.business.fbashipment.service.FbaShipmentDetailService;
import com.musheng.common.annotation.OperationLog;
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
 * FBA货件明细控制器
 * 处理FBA货件明细增删改查和导入功能
 */
@Tag(name = "FBA货件明细", description = "FBA货件明细管理接口")
@RestController
@RequestMapping("/v1/business/fba-shipment")
@RequiredArgsConstructor
public class FbaShipmentDetailController {

    private final FbaShipmentDetailService fbaShipmentDetailService;

    @Operation(summary = "FBA货件明细列表", description = "分页查询FBA货件明细")
    @GetMapping
    public Result<PageResult<FbaShipmentDetail>> list(
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "货件编号") @RequestParam(required = false) String shipmentId,
            @Parameter(description = "收货地址") @RequestParam(required = false) String receivingAddress,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<FbaShipmentDetail> pageResult = fbaShipmentDetailService.list(status, shipmentId, receivingAddress, page, size);
        PageResult<FbaShipmentDetail> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "FBA货件明细详情", description = "根据ID获取FBA货件明细")
    @GetMapping("/{id}")
    public Result<FbaShipmentDetail> getById(
            @Parameter(description = "货件明细ID") @PathVariable Long id) {
        FbaShipmentDetail data = fbaShipmentDetailService.getById(id);
        return Result.success(data);
    }

    @OperationLog(module = "FBA货件明细", operation = "导入FBA货件明细")
    @Operation(summary = "导入FBA货件明细", description = "从文件导入FBA货件明细，自动识别GBK编码")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @Parameter(description = "导入文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> importResult = fbaShipmentDetailService.importData(file);
        return Result.success(importResult);
    }

    @OperationLog(module = "FBA货件明细", operation = "新增FBA货件明细")
    @Operation(summary = "新增FBA货件明细", description = "新增FBA货件明细记录")
    @PostMapping
    public Result<Long> add(
            @Parameter(description = "FBA货件明细") @RequestBody FbaShipmentDetail fbaShipmentDetail) {
        Long id = fbaShipmentDetailService.add(fbaShipmentDetail);
        return Result.success(id);
    }

    @OperationLog(module = "FBA货件明细", operation = "更新FBA货件明细")
    @Operation(summary = "更新FBA货件明细", description = "更新FBA货件明细记录")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "货件明细ID") @PathVariable Long id,
            @Parameter(description = "FBA货件明细") @RequestBody FbaShipmentDetail fbaShipmentDetail) {
        fbaShipmentDetail.setId(id);
        fbaShipmentDetailService.update(fbaShipmentDetail);
        return Result.success();
    }

    @OperationLog(module = "FBA货件明细", operation = "删除FBA货件明细")
    @Operation(summary = "删除FBA货件明细", description = "根据ID删除FBA货件明细")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "货件明细ID") @PathVariable Long id) {
        fbaShipmentDetailService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "FBA货件明细", operation = "批量删除FBA货件明细")
    @Operation(summary = "批量删除FBA货件明细", description = "根据ID列表批量删除FBA货件明细")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(
            @Parameter(description = "货件明细ID列表") @RequestBody List<Long> ids) {
        fbaShipmentDetailService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "FBA货件明细汇总", description = "获取FBA货件明细统计汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "收货地址") @RequestParam(required = false) String receivingAddress,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = fbaShipmentDetailService.getSummary(status, receivingAddress, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "导出FBA货件明细", description = "导出FBA货件明细到Excel")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "收货地址") @RequestParam(required = false) String receivingAddress,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) {
        fbaShipmentDetailService.exportData(status, receivingAddress, startDate, endDate, response);
    }
}
