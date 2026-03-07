package com.musheng.business.document.controller;

import com.musheng.business.document.dto.SettlementImportRequest;
import com.musheng.business.document.service.DocumentValidationService;
import com.musheng.business.document.service.SettlementImportService;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 结算数据导入控制器
 *
 * <p>提供结算数据导入和校验API。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Tag(name = "结算数据导入", description = "结算数据导入和校验接口")
@RestController
@RequestMapping("/v1/business/document")
@RequiredArgsConstructor
@Slf4j
public class SettlementImportController {

    private final SettlementImportService settlementImportService;
    private final DocumentValidationService documentValidationService;

    /**
     * 导入结算数据
     *
     * @param request 导入请求（包含周期、站点、MSKU、数量、单价等）
     * @return 导入的数据条数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "导入结算数据", description = "导入结算数据到系统")
    @PostMapping("/settlement-data/import")
    public Result<Integer> importSettlementData(
            @Valid @RequestBody SettlementImportRequest request) {
        log.info("导入结算数据，周期: {} ~ {}，数据条数: {}",
                request.getPeriodStart(), request.getPeriodEnd(), request.getItems().size());
        int count = settlementImportService.importSettlementData(request);
        return Result.success(count);
    }

    /**
     * 校验结算周期数据一致性
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @return 校验结果（Map&lt;校验类型, 错误列表&gt;）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "校验结算周期数据一致性", description = "校验指定结算周期内结算单与INV的数据一致性")
    @GetMapping("/validate/period")
    public Result<Map<String, List<String>>> validatePeriod(
            @Parameter(description = "周期起始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "周期结束日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        log.info("校验结算周期数据一致性，周期: {} ~ {}", periodStart, periodEnd);
        Map<String, List<String>> validationResult = documentValidationService.validateAll(periodStart, periodEnd);
        return Result.success(validationResult);
    }
}
