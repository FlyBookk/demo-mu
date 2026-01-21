package com.musheng.business.report.controller;

import com.musheng.business.report.dto.ReportSummary;
import com.musheng.business.report.service.ReportService;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表控制器
 * 处理报表生成、汇总查询和导出功能
 */
@Tag(name = "汇总报表", description = "汇总报表管理接口")
@RestController
@RequestMapping("/v1/business/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "汇总报表", description = "按站点和季度查询汇总报表")
    @GetMapping("/summary")
    public Result<List<ReportSummary>> getSummary(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "年季度(YYYY-Q1)") @RequestParam(required = false) String yearQuarter,
            @Parameter(description = "开始年季度") @RequestParam(required = false) String startQuarter,
            @Parameter(description = "结束年季度") @RequestParam(required = false) String endQuarter) {
        List<ReportSummary> summaries = reportService.getSummary(siteCode, yearQuarter, startQuarter, endQuarter);
        return Result.success(summaries);
    }

    @Operation(summary = "按站点汇总", description = "按站点分组查询汇总报表")
    @GetMapping("/summary/by-site")
    public Result<List<ReportSummary>> getSummaryBySite(
            @Parameter(description = "年季度(YYYY-Q1)", required = true) @RequestParam String yearQuarter) {
        List<ReportSummary> summaries = reportService.getSummaryBySite(yearQuarter);
        return Result.success(summaries);
    }

    @Operation(summary = "按季度汇总", description = "按季度分组查询汇总报表")
    @GetMapping("/summary/by-quarter")
    public Result<List<ReportSummary>> getSummaryByQuarter(
            @Parameter(description = "站点编码", required = true) @RequestParam String siteCode,
            @Parameter(description = "开始年季度") @RequestParam(required = false) String startQuarter,
            @Parameter(description = "结束年季度") @RequestParam(required = false) String endQuarter) {
        List<ReportSummary> summaries = reportService.getSummaryByQuarter(siteCode, startQuarter, endQuarter);
        return Result.success(summaries);
    }

    @Operation(summary = "导出汇总报表", description = "导出汇总报表为Excel文件")
    @GetMapping("/summary/export")
    public void exportSummary(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "年季度(YYYY-Q1)") @RequestParam(required = false) String yearQuarter,
            @Parameter(description = "开始年季度") @RequestParam(required = false) String startQuarter,
            @Parameter(description = "结束年季度") @RequestParam(required = false) String endQuarter,
            HttpServletResponse response) {
        reportService.exportSummary(siteCode, yearQuarter, startQuarter, endQuarter, response);
    }

    @Operation(summary = "导出明细报表", description = "导出明细报表为Excel文件")
    @GetMapping("/detail/export")
    public void exportDetail(
            @Parameter(description = "站点编码", required = true) @RequestParam String siteCode,
            @Parameter(description = "年季度(YYYY-Q1)", required = true) @RequestParam String yearQuarter,
            @Parameter(description = "报表类型(sales/shipping/all)") @RequestParam(defaultValue = "all") String reportType,
            HttpServletResponse response) {
        reportService.exportDetail(siteCode, yearQuarter, reportType, response);
    }
}
