package com.musheng.business.advertising.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.dto.AdvertisingDataRequest;
import com.musheng.business.advertising.entity.AdvertisingData;
import com.musheng.business.advertising.service.AdvertisingDataService;
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

import java.time.LocalDate;

/**
 * 广告数据控制器
 * P0需求: 广告数据增删改查接口
 */
@Tag(name = "广告数据", description = "广告数据管理接口")
@RestController
@RequestMapping("/v1/advertising")
@RequiredArgsConstructor
public class AdvertisingDataController {

    private final AdvertisingDataService advertisingDataService;

    @OperationLog(module = "广告数据", operation = "创建广告数据")
    @Operation(summary = "创建广告数据", description = "创建新的广告数据记录")
    @PostMapping
    public Result<AdvertisingData> create(@Valid @RequestBody AdvertisingDataRequest request) {
        AdvertisingData data = advertisingDataService.create(request);
        return Result.success(data);
    }

    @OperationLog(module = "广告数据", operation = "批量导入广告数据")
    @Operation(summary = "批量导入广告数据", description = "批量导入广告发票数据，支持发票编号去重、时间字段校验")
    @PostMapping("/import")
    public Result<AdvertisingDataImportResponse> importData(
            @Valid @RequestBody AdvertisingDataImportBatchRequest request) {
        AdvertisingDataImportResponse result = advertisingDataService.importData(request);
        return Result.success(result);
    }

    @OperationLog(module = "广告数据", operation = "更新广告数据")
    @Operation(summary = "更新广告数据", description = "更新广告数据记录")
    @PutMapping("/{id}")
    public Result<AdvertisingData> update(
            @Parameter(description = "广告数据ID") @PathVariable Long id,
            @Valid @RequestBody AdvertisingDataRequest request) {
        AdvertisingData data = advertisingDataService.update(id, request);
        return Result.success(data);
    }

    @OperationLog(module = "广告数据", operation = "删除广告数据")
    @Operation(summary = "删除广告数据", description = "删除广告数据记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "广告数据ID") @PathVariable Long id) {
        advertisingDataService.delete(id);
        return Result.success();
    }

    @Operation(summary = "广告数据详情", description = "根据ID获取广告数据")
    @GetMapping("/{id}")
    public Result<AdvertisingData> getById(@Parameter(description = "广告数据ID") @PathVariable Long id) {
        AdvertisingData data = advertisingDataService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "广告数据列表", description = "分页查询广告数据")
    @GetMapping
    public Result<PageResult<AdvertisingData>> list(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "年月(YYYY-MM)") @RequestParam(required = false) String yearMonth,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<AdvertisingData> pageResult = advertisingDataService.list(siteCode, yearMonth, page, size);
        PageResult<AdvertisingData> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "按条件查询广告数据", description = "支持按账单周期、发票编号等条件查询")
    @GetMapping("/search")
    public Result<PageResult<AdvertisingData>> listByConditions(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "账单开始日期(YYYY-MM-DD)") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billingStartDate,
            @Parameter(description = "账单结束日期(YYYY-MM-DD)") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billingEndDate,
            @Parameter(description = "发票编号") @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<AdvertisingData> pageResult = advertisingDataService.listByConditions(
                siteCode, billingStartDate, billingEndDate, invoiceNumber, page, size);
        PageResult<AdvertisingData> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }
}
