package com.musheng.tiktok.document.controller;

import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.tiktok.document.dto.TiktokDocumentQueryRequest;
import com.musheng.tiktok.document.entity.*;
import com.musheng.tiktok.document.service.TiktokDocumentExportService;
import com.musheng.tiktok.document.service.TiktokDocumentGenerateService;
import com.musheng.tiktok.document.service.TiktokDocumentQueryService;
import com.musheng.tiktok.document.vo.TiktokDocumentListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TK单据管理接口（对齐亚马逊 DocumentController）
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/document")
@Tag(name = "TK单据管理")
@Slf4j
@CrossOrigin
public class TiktokDocumentController {

    @Autowired
    private TiktokDocumentQueryService queryService;
    @Autowired
    private TiktokDocumentGenerateService generateService;
    @Autowired
    private TiktokDocumentExportService exportService;

    // ==================== 查询API ====================

    @Operation(summary = "单据列表")
    @GetMapping("/list")
    public Result<PageResult<TiktokDocumentListVO>> listDocuments(
            @Parameter(description = "单据类型（PO/DN/SETTLEMENT/INV）") @RequestParam(required = false) String documentType,
            @Parameter(description = "单据编号") @RequestParam(required = false) String documentNo,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "站点代码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        TiktokDocumentQueryRequest request = TiktokDocumentQueryRequest.builder()
                .documentType(documentType)
                .documentNo(documentNo)
                .startDate(startDate)
                .endDate(endDate)
                .siteCode(siteCode)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return Result.success(queryService.listDocuments(request));
    }

    @Operation(summary = "PO详情")
    @GetMapping("/po/{id}")
    public Result<Map<String, Object>> getPoDetail(@PathVariable Long id) {
        var po = queryService.getPoDetail(id);
        if (po == null) return Result.error(404, "单据不存在或无权访问");
        Map<String, Object> result = new HashMap<>();
        result.put("po", po);
        result.put("items", queryService.getPoItems(id));
        return Result.success(result);
    }

    @Operation(summary = "DN详情")
    @GetMapping("/dn/{id}")
    public Result<Map<String, Object>> getDnDetail(@PathVariable Long id) {
        var dn = queryService.getDnDetail(id);
        if (dn == null) return Result.error(404, "单据不存在或无权访问");
        Map<String, Object> result = new HashMap<>();
        result.put("dn", dn);
        result.put("items", queryService.getDnItems(id));
        return Result.success(result);
    }

    @Operation(summary = "结算单详情")
    @GetMapping("/settlement/{id}")
    public Result<Map<String, Object>> getSettlementDetail(@PathVariable Long id) {
        var settlement = queryService.getSettlementDetail(id);
        if (settlement == null) return Result.error(404, "单据不存在或无权访问");
        Map<String, Object> result = new HashMap<>();
        result.put("settlement", settlement);
        result.put("items", queryService.getSettlementItems(id));
        return Result.success(result);
    }

    @Operation(summary = "INV详情")
    @GetMapping("/inv/{id}")
    public Result<Map<String, Object>> getInvDetail(@PathVariable Long id) {
        var inv = queryService.getInvDetail(id);
        if (inv == null) return Result.error(404, "单据不存在或无权访问");
        Map<String, Object> result = new HashMap<>();
        result.put("inv", inv);
        result.put("items", queryService.getInvItems(id));
        return Result.success(result);
    }

    @Operation(summary = "结算单MSKU季度汇总（多月合并）")
    @GetMapping("/settlement/msku-summary")
    public Result<List<Map<String, Object>>> settlementMskuSummary(
            @RequestParam String siteCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(queryService.getSettlementMskuSummary(siteCode, startDate, endDate));
    }

    // ==================== 生成API ====================

    @Operation(summary = "生成PO（按创建时间分组，可能生成多份）")
    @PostMapping("/po/generate")
    public Result<List<TiktokDocumentPo>> generatePo(@RequestBody GeneratePoRequest req) {
        log.info("生成TK PO，站点: {}，货件数: {}", req.getSiteCode(), req.getShipmentIds().size());
        return Result.success(generateService.generatePo(req.getSiteCode(), req.getShipmentIds()));
    }

    @Operation(summary = "生成DN（按锚点+21天周期分组，可能生成多份）")
    @PostMapping("/dn/generate")
    public Result<List<TiktokDocumentDn>> generateDn(@RequestBody GenerateDnRequest req) {
        log.info("生成TK DN，站点: {}，锚点: {}，poId: {}，货件数: {}", req.getSiteCode(), req.getAnchorDate(), req.getPoId(), req.getShipmentIds() != null ? req.getShipmentIds().size() : 0);
        return Result.success(generateService.generateDn(req.getSiteCode(), req.getShipmentIds(), req.getAnchorDate(), req.getPoId()));
    }

    @Operation(summary = "生成Settlement+INV（按月拆分，可能生成多份）")
    @PostMapping("/settlement/generate")
    public Result<List<TiktokDocumentSettlement>> generateSettlement(@RequestBody GenerateSettlementRequest req) {
        log.info("生成TK结算单，站点: {}，季度: {}", req.getSiteCode(), req.getQuarter());
        return Result.success(generateService.generateSettlement(req.getSiteCode(), req.getQuarter(), req.getCostAmount()));
    }

    @Operation(summary = "根据结算单生成INV")
    @PostMapping("/inv/generate/{settlementId}")
    public Result<TiktokDocumentInv> generateInv(@PathVariable Long settlementId) {
        return Result.success(generateService.generateInv(settlementId));
    }

    @Operation(summary = "根据结算单ID列表查询关联的INV")
    @PostMapping("/inv/by-settlements")
    public Result<List<TiktokDocumentInv>> getInvBySettlements(@RequestBody List<Long> settlementIds) {
        Long shopId = com.musheng.common.context.ShopContext.requireShopId();
        List<TiktokDocumentInv> invs = new java.util.ArrayList<>();
        for (Long sid : settlementIds) {
            var list = queryService.getInvBySettlementId(sid);
            if (list != null) invs.addAll(list);
        }
        return Result.success(invs);
    }

    // ==================== 导出API ====================

    @Operation(summary = "导出PO Excel")
    @GetMapping("/export/po/{id}")
    public void exportPo(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportPo(id, response);
    }

    @Operation(summary = "导出DN Excel")
    @GetMapping("/export/dn/{id}")
    public void exportDn(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportDn(id, response);
    }

    @Operation(summary = "导出结算单 Excel")
    @GetMapping("/export/settlement/{id}")
    public void exportSettlement(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportSettlement(id, response);
    }

    @Operation(summary = "导出INV Excel")
    @GetMapping("/export/inv/{id}")
    public void exportInv(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportInv(id, response);
    }

    @Operation(summary = "批量导出PO为ZIP")
    @PostMapping("/export/po/batch")
    public void batchExportPo(@RequestBody List<Long> poIds, HttpServletResponse response) {
        exportService.batchExportPo(poIds, response);
    }

    @Operation(summary = "批量导出DN为ZIP")
    @PostMapping("/export/dn/batch")
    public void batchExportDn(@RequestBody List<Long> dnIds, HttpServletResponse response) {
        exportService.batchExportDn(dnIds, response);
    }

    @Operation(summary = "批量导出结算单为ZIP")
    @PostMapping("/export/settlement/batch")
    public void batchExportSettlement(@RequestBody List<Long> ids, HttpServletResponse response) {
        exportService.batchExportSettlement(ids, response);
    }

    @Operation(summary = "批量导出INV为ZIP")
    @PostMapping("/export/inv/batch")
    public void batchExportInv(@RequestBody List<Long> ids, HttpServletResponse response) {
        exportService.batchExportInv(ids, response);
    }

    // ==================== 请求DTO ====================

    @Data
    public static class GeneratePoRequest {
        private String siteCode;
        private List<String> shipmentIds;
    }

    @Data
    public static class GenerateDnRequest {
        private String siteCode;
        private List<String> shipmentIds;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate anchorDate;
        /** 关联PO主键（传入时自动从PO明细提取货件） */
        private Long poId;
    }

    @Data
    public static class GenerateSettlementRequest {
        private String siteCode;
        private String quarter; // 格式: 2025-Q3
        private BigDecimal costAmount; // 采购成本（原币）
    }
}
