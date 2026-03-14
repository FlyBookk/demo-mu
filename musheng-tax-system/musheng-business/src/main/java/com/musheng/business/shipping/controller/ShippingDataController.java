package com.musheng.business.shipping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.service.ShippingDataService;
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
 * 配送数据控制器
 * 处理配送数据增删改查和导入功能
 */
@Tag(name = "配送数据", description = "配送数据管理接口")
@RestController
@RequestMapping("/v1/business/shipping")
@RequiredArgsConstructor
@RequireShop  // 整个控制器都需要店铺上下文
public class ShippingDataController {

    private final ShippingDataService shippingDataService;

    @Operation(summary = "配送数据列表", description = "分页查询配送数据")
    @GetMapping
    public Result<PageResult<ShippingData>> list(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "物流单号") @RequestParam(required = false) String trackingNumber,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderId,
            @Parameter(description = "开始日期(发货日期)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期(发货日期)") @RequestParam(required = false) String endDate,
            @Parameter(description = "是否本站数据(0-否/1-是)") @RequestParam(required = false) Integer isOwnSite,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<ShippingData> pageResult = shippingDataService.list(siteCode, trackingNumber, orderId, startDate, endDate, isOwnSite, page, size);
        PageResult<ShippingData> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "配送数据详情", description = "根据ID获取配送数据")
    @GetMapping("/{id}")
    public Result<ShippingData> getById(
            @Parameter(description = "配送数据ID") @PathVariable Long id) {
        ShippingData data = shippingDataService.getById(id);
        return Result.success(data);
    }

    @OperationLog(module = "配送数据", operation = "导入配送数据")
    @Operation(summary = "导入配送数据", description = "从文件导入配送数据，自动识别销售渠道并分配站点")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @Parameter(description = "导入文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> importResult = shippingDataService.importData(file);
        return Result.success(importResult);
    }

    @OperationLog(module = "配送数据", operation = "批量导入配送数据")
    @Operation(summary = "批量导入配送数据", description = "从多个文件批量导入配送数据，支持多选")
    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImportData(
            @Parameter(description = "导入文件列表") @RequestParam("files") List<MultipartFile> files) {
        Map<String, Object> batchResult = shippingDataService.batchImportData(files);
        return Result.success(batchResult);
    }

    @OperationLog(module = "配送数据", operation = "删除配送数据")
    @Operation(summary = "删除配送数据", description = "根据ID删除配送数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "配送数据ID") @PathVariable Long id) {
        shippingDataService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "配送数据", operation = "批量删除配送数据")
    @Operation(summary = "批量删除配送数据", description = "根据ID列表批量删除配送数据")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(
            @Parameter(description = "配送数据ID列表") @RequestBody List<Long> ids) {
        shippingDataService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "配送数据汇总", description = "获取配送数据统计汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = shippingDataService.getSummary(siteCode, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "导出配送数据", description = "导出配送数据到Excel")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) {
        shippingDataService.exportData(siteCode, startDate, endDate, response);
    }
}
