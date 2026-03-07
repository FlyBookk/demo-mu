package com.musheng.business.fbashipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.service.FbaShipmentService;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.annotation.RequireShop;
import com.musheng.common.result.PageResult;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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

    /**
     * 批量导入FBA货件
     * 从多个Excel文件批量导入FBA货件明细，支持幂等性，需指定站点代码
     *
     * @param files Excel文件列表
     * @param siteCode 站点代码（如 US/CA/UK/DE），必填
     * @return 批量导入结果
     * @author wanhua
     * 10:30 2026年03月07日
     */
    @OperationLog(module = "FBA货件", operation = "批量导入FBA货件")
    @Operation(summary = "批量导入FBA货件", description = "从多个Excel文件批量导入FBA货件明细，支持幂等性，需指定站点代码")
    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImportData(
            @Parameter(description = "Excel文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "站点代码（必填）") @RequestParam String siteCode) {
        // 校验站点代码非空
        if (!StringUtils.hasText(siteCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "站点代码不能为空");
        }
        Map<String, Object> batchResult = fbaShipmentService.batchImportData(files, siteCode);
        return Result.success(batchResult);
    }

    /**
     * 分页查询FBA货件列表
     *
     * @param shipmentId 货件单号（模糊查询）
     * @param status 货件状态
     * @param shopName 店铺名称
     * @param country 国家
     * @param siteCode 站点代码（如 US/CA/UK/DE），可选
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页查询结果
     * @author wanhua
     * 10:30 2026年03月07日
     */
    @Operation(summary = "货件列表", description = "分页查询FBA货件列表")
    @GetMapping("/list")
    public Result<PageResult<FbaShipment>> list(
            @Parameter(description = "货件单号") @RequestParam(required = false) String shipmentId,
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "站点代码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Page<FbaShipment> pageResult = fbaShipmentService.list(
                shipmentId, status, shopName, country, siteCode, startDate, endDate, page, size);
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
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = fbaShipmentService.getSummary(status, shopName, country, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "导出货件", description = "导出FBA货件明细为CSV（与导入格式一致）")
    @GetMapping("/export")
    public void exportData(
            @Parameter(description = "货件状态") @RequestParam(required = false) String status,
            @Parameter(description = "店铺名称") @RequestParam(required = false) String shopName,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) {
        fbaShipmentService.exportData(status, shopName, country, startDate, endDate, response);
    }

    @Operation(summary = "获取国家列表", description = "获取所有已导入货件的国家列表（去重）")
    @GetMapping("/countries")
    public Result<List<String>> getCountryList() {
        List<String> countries = fbaShipmentService.getCountryList();
        return Result.success(countries);
    }
}
