package com.musheng.business.document.controller;

import com.musheng.business.document.dto.DnGenerateRequest;
import com.musheng.business.document.dto.DocumentQueryRequest;
import com.musheng.business.document.dto.MskuQueryRequest;
import com.musheng.business.document.dto.MskuUpdateRequest;
import com.musheng.business.document.dto.PoGenerateRequest;
import com.musheng.business.document.dto.SettlementGenerateRequest;
import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.service.DocumentExportService;
import com.musheng.business.document.service.DocumentGenerateService;
import com.musheng.business.document.service.DocumentQueryService;
import com.musheng.business.document.vo.*;
import com.musheng.business.document.vo.MskuListVO;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 单据管理控制器
 *
 * <p>提供FBA单据（PO/DN/结算单/INV）的生成、查询、导出API。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Tag(name = "单据管理", description = "FBA单据生成/查询/导出接口")
@RestController
@RequestMapping("/v1/business/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentGenerateService documentGenerateService;
    private final DocumentQueryService documentQueryService;
    private final DocumentExportService documentExportService;

    // ==================== 生成API ====================

    /**
     * 生成PO采购订单
     *
     * @param request PO生成请求
     * @return 生成的PO实体
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "生成PO采购订单", description = "根据选定的FBA货件生成PO采购订单")
    @PostMapping("/po/generate")
    public Result<DocumentPo> generatePo(
            @Valid @RequestBody PoGenerateRequest request) {
        log.info("生成PO采购订单，站点: {}，货件数量: {}", request.getSiteCode(), request.getShipmentIds().size());
        DocumentPo po = documentGenerateService.generatePo(request);
        return Result.success(po);
    }

    /**
     * 生成DN送货单
     *
     * @param request DN生成请求
     * @return 生成的DN实体
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "生成DN送货单", description = "根据DN周期批量生成送货单")
    @PostMapping("/dn/generate")
    public Result<DocumentDn> generateDn(
            @Valid @RequestBody DnGenerateRequest request) {
        log.info("生成DN送货单，站点: {}，锚点日期: {}，货件数量: {}", request.getSiteCode(), request.getAnchorDate(), request.getShipmentIds().size());
        DocumentDn dn = documentGenerateService.generateDn(request);
        return Result.success(dn);
    }

    /**
     * 生成结算单（4份，按站点拆分）
     *
     * @param request 结算单生成请求
     * @return 生成的结算单列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "生成结算单", description = "根据结算周期生成4份结算单（按站点拆分）")
    @PostMapping("/settlement/generate")
    public Result<List<DocumentSettlement>> generateSettlements(
            @Valid @RequestBody SettlementGenerateRequest request) {
        log.info("生成结算单，周期: {} ~ {}，站点: {}", request.getPeriodStart(), request.getPeriodEnd(), request.getSiteCodes());
        List<DocumentSettlement> settlements = documentGenerateService.generateSettlements(request);
        return Result.success(settlements);
    }

    /**
     * 生成INV发票（4份）
     *
     * @param settlementIds 结算单ID列表
     * @return 生成的INV列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "生成INV发票", description = "根据结算单自动生成对应的4份INV发票")
    @PostMapping("/inv/generate")
    public Result<List<DocumentInv>> generateInvoices(
            @RequestBody List<Long> settlementIds) {
        log.info("生成INV发票，结算单数量: {}", settlementIds.size());
        List<DocumentInv> invoices = documentGenerateService.generateInvoices(settlementIds);
        return Result.success(invoices);
    }

    // ==================== 查询API ====================

    /**
     * 单据列表（分页查询）
     *
     * @param documentType 单据类型
     * @param documentNo 单据编号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "单据列表", description = "分页查询单据列表，支持按类型/编号/日期范围筛选")
    @GetMapping("/list")
    public Result<PageResult<DocumentListVO>> listDocuments(
            @Parameter(description = "单据类型（PO/DN/SETTLEMENT/INV）") @RequestParam(required = false) String documentType,
            @Parameter(description = "单据编号") @RequestParam(required = false) String documentNo,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        DocumentQueryRequest request = DocumentQueryRequest.builder()
                .documentType(documentType)
                .documentNo(documentNo)
                .startDate(startDate)
                .endDate(endDate)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        PageResult<DocumentListVO> result = documentQueryService.listDocuments(request);
        return Result.success(result);
    }

    /**
     * PO详情
     *
     * @param id PO主键
     * @return PO详情视图
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "PO详情", description = "根据ID获取PO采购订单详情（含货件分组明细）")
    @GetMapping("/po/{id}")
    public Result<PoVO> getPoDetail(
            @Parameter(description = "PO主键ID") @PathVariable Long id) {
        PoVO po = documentQueryService.getPoDetail(id);
        return Result.success(po);
    }

    /**
     * DN详情
     *
     * @param id DN主键
     * @return DN详情视图
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "DN详情", description = "根据ID获取送货单详情（含MSKU明细及货件备注）")
    @GetMapping("/dn/{id}")
    public Result<DnVO> getDnDetail(
            @Parameter(description = "DN主键ID") @PathVariable Long id) {
        DnVO dn = documentQueryService.getDnDetail(id);
        return Result.success(dn);
    }

    /**
     * 结算单详情
     *
     * @param id 结算单主键
     * @return 结算单详情视图
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "结算单详情", description = "根据ID获取结算单详情（含周期、站点、MSKU明细）")
    @GetMapping("/settlement/{id}")
    public Result<SettlementVO> getSettlementDetail(
            @Parameter(description = "结算单主键ID") @PathVariable Long id) {
        SettlementVO settlement = documentQueryService.getSettlementDetail(id);
        return Result.success(settlement);
    }

    /**
     * INV详情
     *
     * @param id INV主键
     * @return INV详情视图
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "INV详情", description = "根据ID获取INV发票详情（含银行信息）")
    @GetMapping("/inv/{id}")
    public Result<InvVO> getInvDetail(
            @Parameter(description = "INV主键ID") @PathVariable Long id) {
        InvVO inv = documentQueryService.getInvDetail(id);
        return Result.success(inv);
    }

    /**
     * 按结算周期查看关联单据
     *
     * @param periodStart 周期起始日
     * @param periodEnd 周期结束日
     * @return 关联单据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "按结算周期查看关联单据", description = "查看指定结算周期下的4份结算单+4份INV")
    @GetMapping("/period")
    public Result<List<DocumentListVO>> listBySettlementPeriod(
            @Parameter(description = "周期起始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "周期结束日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        List<DocumentListVO> list = documentQueryService.listBySettlementPeriod(periodStart, periodEnd);
        return Result.success(list);
    }

    /**
     * 货件关联关系查询
     *
     * @param shipmentNo 货件编号
     * @return 包含PO和DN信息的Map
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "货件关联关系查询", description = "查看货件在PO和DN中的关联关系")
    @GetMapping("/shipment-relation")
    public Result<Map<String, Object>> getShipmentDocumentRelation(
            @Parameter(description = "货件编号") @RequestParam String shipmentNo) {
        Map<String, Object> relation = documentQueryService.getShipmentDocumentRelation(shipmentNo);
        return Result.success(relation);
    }

    // ==================== 导出API ====================

    /**
     * MSKU列表查询（分页）
     *
     * <p>查询结算推导后的MSKU汇总数据，支持按站点、MSKU编码、结算周期筛选。</p>
     *
     * @param siteCode 站点代码
     * @param msku MSKU编码（模糊搜索）
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     * @author wanhua
     * 14:00 2026年03月07日
     */
    @Operation(summary = "MSKU列表", description = "查询结算推导后的MSKU汇总数据，支持按站点/MSKU/周期筛选")
    @GetMapping("/msku/list")
    public Result<PageResult<MskuListVO>> listMsku(
            @Parameter(description = "站点代码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "MSKU编码") @RequestParam(required = false) String msku,
            @Parameter(description = "周期起始日") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "周期结束日") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        MskuQueryRequest request = MskuQueryRequest.builder()
                .siteCode(siteCode)
                .msku(msku)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        PageResult<MskuListVO> result = documentQueryService.listMsku(request);
        return Result.success(result);
    }

    /**
     * 更新MSKU数据（手动编辑单价、数量、采购成本、汇率）
     *
     * @param request 更新请求
     * @return 操作结果
     * @author wanhua
     * 14:00 2026年03月07日
     */
    @Operation(summary = "更新MSKU数据", description = "手动编辑MSKU的单价、数量、采购成本、汇率等字段")
    @PutMapping("/msku/update")
    public Result<Void> updateMsku(
            @Valid @RequestBody MskuUpdateRequest request) {
        log.info("更新MSKU数据，ID: {}", request.getId());
        documentQueryService.updateMsku(request);
        return Result.success();
    }

    /**
     * 导出PO Excel
     *
     * @param id PO主键ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "导出PO Excel", description = "导出PO采购订单为Excel文件")
    @GetMapping("/export/po/{id}")
    public void exportPo(
            @Parameter(description = "PO主键ID") @PathVariable Long id,
            HttpServletResponse response) {
        log.info("导出PO Excel，ID: {}", id);
        documentExportService.exportPo(id, response);
    }

    /**
     * 导出DN Excel
     *
     * @param id DN主键ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "导出DN Excel", description = "导出送货单为Excel文件")
    @GetMapping("/export/dn/{id}")
    public void exportDn(
            @Parameter(description = "DN主键ID") @PathVariable Long id,
            HttpServletResponse response) {
        log.info("导出DN Excel，ID: {}", id);
        documentExportService.exportDn(id, response);
    }

    /**
     * 导出结算单 Excel
     *
     * @param id 结算单主键ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "导出结算单 Excel", description = "导出结算单为Excel文件")
    @GetMapping("/export/settlement/{id}")
    public void exportSettlement(
            @Parameter(description = "结算单主键ID") @PathVariable Long id,
            HttpServletResponse response) {
        log.info("导出结算单 Excel，ID: {}", id);
        documentExportService.exportSettlement(id, response);
    }

    /**
     * 导出INV Excel
     *
     * @param id INV主键ID
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "导出INV Excel", description = "导出INV发票为Excel文件")
    @GetMapping("/export/inv/{id}")
    public void exportInv(
            @Parameter(description = "INV主键ID") @PathVariable Long id,
            HttpServletResponse response) {
        log.info("导出INV Excel，ID: {}", id);
        documentExportService.exportInv(id, response);
    }

    /**
     * 批量导出结算周期8份文件
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Operation(summary = "批量导出结算周期文件", description = "批量导出一个结算周期的4份结算单+4份INV（共8个文件）")
    @GetMapping("/export/period")
    public void batchExportByPeriod(
            @Parameter(description = "周期起始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "周期结束日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            HttpServletResponse response) {
        log.info("批量导出结算周期文件，周期: {} ~ {}", periodStart, periodEnd);
        documentExportService.batchExportByPeriod(periodStart, periodEnd, response);
    }

    /**
     * 批量导出结算单为ZIP
     *
     * @param settlementIds 结算单主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Operation(summary = "批量导出结算单为ZIP", description = "将多份结算单打包为ZIP文件下载")
    @PostMapping("/export/settlement/batch")
    public void batchExportSettlement(
            @RequestBody List<Long> settlementIds,
            HttpServletResponse response) {
        log.info("批量导出结算单，数量: {}", settlementIds.size());
        documentExportService.batchExportSettlement(settlementIds, response);
    }

    /**
     * 批量导出INV为ZIP
     *
     * @param invIds INV主键ID列表
     * @param response HTTP响应对象
     * @author wanhua
     * 00:50 2026年03月02日
     */
    @Operation(summary = "批量导出INV为ZIP", description = "将多份INV打包为ZIP文件下载")
    @PostMapping("/export/inv/batch")
    public void batchExportInv(
            @RequestBody List<Long> invIds,
            HttpServletResponse response) {
        log.info("批量导出INV，数量: {}", invIds.size());
        documentExportService.batchExportInv(invIds, response);
    }
}
