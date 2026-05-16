package com.musheng.tiktok.settlement.controller;

import com.musheng.common.result.Result;
import com.musheng.tiktok.settlement.service.TiktokExchangeRateService;
import com.musheng.tiktok.settlement.service.TiktokTaxSummaryService;
import com.musheng.tiktok.settlement.service.TiktokTaxSummaryService.QuarterOperationSummary;
import com.musheng.tiktok.settlement.service.TiktokTaxSummaryService.QuarterTaxSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * TK报税汇总接口（按季度）
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/report")
@Tag(name = "TK报税汇总")
@Slf4j
@CrossOrigin
public class TiktokReportController {

    @Autowired
    private TiktokTaxSummaryService taxSummaryService;
    @Autowired
    private TiktokExchangeRateService exchangeRateService;

    @Operation(summary = "报税口径汇总（按季度，含月度明细）")
    @GetMapping("/tax-summary")
    public Result<QuarterTaxSummary> taxSummary(
            @Parameter(description = "季度，格式: 2025-Q3") @RequestParam String quarter,
            @Parameter(description = "站点代码") @RequestParam String siteCode,
            @Parameter(description = "汇率（可选，不传则自动查找）") @RequestParam(required = false) BigDecimal exchangeRate) {
        BigDecimal rate = resolveRate(quarter, exchangeRate);
        if (rate == null) {
            return Result.error(400, "未找到" + quarter + "对应的汇率，请手动输入");
        }
        return Result.success(taxSummaryService.getQuarterTaxSummary(quarter, siteCode, rate));
    }

    @Operation(summary = "运营口径汇总（按季度）")
    @GetMapping("/operation-summary")
    public Result<QuarterOperationSummary> operationSummary(
            @Parameter(description = "季度，格式: 2025-Q3") @RequestParam String quarter,
            @Parameter(description = "站点代码") @RequestParam String siteCode,
            @Parameter(description = "汇率（可选）") @RequestParam(required = false) BigDecimal exchangeRate) {
        BigDecimal rate = resolveRate(quarter, exchangeRate);
        if (rate == null) {
            return Result.error(400, "未找到" + quarter + "对应的汇率，请手动输入");
        }
        return Result.success(taxSummaryService.getQuarterOperationSummary(quarter, siteCode, rate));
    }

    private BigDecimal resolveRate(String quarter, BigDecimal exchangeRate) {
        if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            return exchangeRate;
        }
        // 从季度推导月份查汇率（取季度最后一个月）
        String[] parts = quarter.split("-Q");
        int year = Integer.parseInt(parts[0]);
        int q = Integer.parseInt(parts[1]);
        int lastMonth = q * 3;
        String month = year + "-" + String.format("%02d", lastMonth);
        return exchangeRateService.getReportExchangeRate(month);
    }
}
