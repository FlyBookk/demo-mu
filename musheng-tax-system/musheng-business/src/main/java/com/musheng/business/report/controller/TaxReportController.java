package com.musheng.business.report.controller;

import com.musheng.business.report.dto.DashboardData;
import com.musheng.business.report.dto.FeeBreakdown;
import com.musheng.business.report.dto.TaxReportSummary;
import com.musheng.business.report.service.TaxReportDetailExportService;
import com.musheng.business.report.service.TaxReportService;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报税汇总控制器
 */
@Tag(name = "报税汇总", description = "报税汇总报表接口")
@RestController
@RequestMapping("/v1/business/reports")
@RequiredArgsConstructor
public class TaxReportController {

    private final TaxReportService taxReportService;
    private final TaxReportDetailExportService taxReportDetailExportService;

    @Operation(summary = "首页仪表盘数据", description = "获取首页核心指标和图表数据，支持按季度筛选")
    @GetMapping("/dashboard")
    public Result<DashboardData> getDashboardData(
            @Parameter(description = "季度(格式:2024-Q1，不传则默认当前季度)") @RequestParam(required = false) String quarter) {
        DashboardData data = taxReportService.getDashboardData(quarter);
        return Result.success(data);
    }

    @Operation(summary = "报税汇总", description = "按站点和季度查询报税汇总数据（收入按发货、退款双维度）")
    @GetMapping("/tax-summary")
    public Result<List<TaxReportSummary>> getTaxSummary(
            @Parameter(description = "站点编码（不传则查询所有站点）") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始季度(格式:2024-Q1)", required = true) @RequestParam String startQuarter,
            @Parameter(description = "结束季度(格式:2024-Q4)", required = true) @RequestParam String endQuarter) {
        List<TaxReportSummary> summaries = taxReportService.getTaxSummary(siteCode, startQuarter, endQuarter);
        return Result.success(summaries);
    }

    @Operation(summary = "费用分类明细", description = "按费用类型分类统计（用于图表展示）")
    @GetMapping("/fee-breakdown")
    public Result<List<FeeBreakdown>> getFeeBreakdown(
            @Parameter(description = "站点编码（不传则查询所有站点）") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始季度", required = true) @RequestParam String startQuarter,
            @Parameter(description = "结束季度", required = true) @RequestParam String endQuarter) {
        List<FeeBreakdown> fees = taxReportService.getFeeBreakdown(siteCode, startQuarter, endQuarter);
        return Result.success(fees);
    }

    @Operation(summary = "导出报税汇总", description = "导出报税汇总报表为Excel文件")
    @GetMapping("/tax-summary/export")
    public void exportTaxSummary(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始季度") @RequestParam String startQuarter,
            @Parameter(description = "结束季度") @RequestParam String endQuarter,
            HttpServletResponse response) {
        taxReportService.exportTaxSummary(siteCode, startQuarter, endQuarter, response);
    }

    @Operation(summary = "导出报税统计明细", description = "导出参与统计的原始数据，分sheet：收入/退款/费用/其它。数据量>10万时自动降级为CSV+ZIP")
    @GetMapping("/tax-summary/export-detail")
    public void exportTaxSummaryDetail(
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "开始季度") @RequestParam String startQuarter,
            @Parameter(description = "结束季度") @RequestParam String endQuarter,
            HttpServletResponse response) {
        taxReportDetailExportService.exportTaxSummaryDetail(siteCode, startQuarter, endQuarter, response);
    }
}
