package com.musheng.business.advertising.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.entity.AdvertisingBill;
import com.musheng.business.advertising.entity.AdvertisingBillItem;
import com.musheng.business.advertising.service.AdvertisingBillService;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.annotation.RequireShop;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 广告发票控制器（主表+明细，无去重）
 */
@Tag(name = "广告数据", description = "广告发票管理接口")
@RestController
@RequestMapping("/v1/advertising")
@RequiredArgsConstructor
@RequireShop
public class AdvertisingBillController {

    private final AdvertisingBillService advertisingBillService;

    @Operation(summary = "下载导入模板", description = "Excel格式，含表头与示例行")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        advertisingBillService.downloadTemplate(response);
    }

    @OperationLog(module = "广告数据", operation = "批量导入广告数据")
    @Operation(summary = "批量导入", description = "按行入库，无去重；同发票多行→1个bill+N个item")
    @PostMapping("/import")
    public Result<AdvertisingDataImportResponse> importData(
            @Valid @RequestBody AdvertisingDataImportBatchRequest request) {
        return Result.success(advertisingBillService.importData(request));
    }

    @Operation(summary = "发票列表", description = "分页查询广告发票主表")
    @GetMapping
    public Result<PageResult<AdvertisingBill>> list(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "年月(YYYY-MM)") @RequestParam(required = false) String yearMonth,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        LocalDate start = null, end = null;
        if (StringUtils.hasText(yearMonth)) {
            try {
                start = LocalDate.parse(yearMonth + "-01");
                end = start.withDayOfMonth(start.lengthOfMonth());
            } catch (Exception ignored) {}
        }
        Page<AdvertisingBill> pageResult = advertisingBillService.list(siteCode, start, end, null, page, size);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    @Operation(summary = "按条件查询", description = "支持账单周期、发票编号")
    @GetMapping("/search")
    public Result<PageResult<AdvertisingBill>> listByConditions(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "账单开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billingStartDate,
            @Parameter(description = "账单结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billingEndDate,
            @Parameter(description = "发票编号") @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Page<AdvertisingBill> pageResult = advertisingBillService.list(
                siteCode, billingStartDate, billingEndDate, invoiceNumber, page, size);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    @Operation(summary = "发票详情", description = "根据ID获取发票及明细")
    @GetMapping("/{id}")
    public Result<AdvertisingBill> getById(@Parameter(description = "发票ID") @PathVariable Long id) {
        return Result.success(advertisingBillService.getById(id));
    }

    @OperationLog(module = "广告数据", operation = "删除广告发票")
    @Operation(summary = "删除", description = "删除发票及明细")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "发票ID") @PathVariable Long id) {
        advertisingBillService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "广告数据", operation = "批量删除广告发票")
    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody java.util.List<Long> ids) {
        advertisingBillService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "明细列表", description = "分页查询广告活动明细（全局视图）")
    @GetMapping("/items")
    public Result<PageResult<AdvertisingBillItem>> listItems(
            @Parameter(description = "发票编号") @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "活动ID") @RequestParam(required = false) String campaignId,
            @Parameter(description = "广告活动名称") @RequestParam(required = false) String campaignName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Page<AdvertisingBillItem> pageResult = advertisingBillService.listItems(
                invoiceNumber, campaignId, campaignName, page, size);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }
}
