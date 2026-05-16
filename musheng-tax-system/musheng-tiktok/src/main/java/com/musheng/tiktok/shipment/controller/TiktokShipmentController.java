package com.musheng.tiktok.shipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.tiktok.shipment.entity.TiktokShipment;
import com.musheng.tiktok.shipment.entity.TiktokShipmentItem;
import com.musheng.tiktok.shipment.service.TiktokShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * TK FBT货件接口
 *
 * @author wanhua
 * 19:40 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/shipment")
@Tag(name = "TK FBT货件管理")
@Slf4j
@CrossOrigin
public class TiktokShipmentController {

    @Autowired
    private TiktokShipmentService shipmentService;

    @Operation(summary = "货件列表")
    @GetMapping("/list")
    public Result<PageResult<TiktokShipment>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam String siteCode,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<TiktokShipment> page = shipmentService.list(keyword, siteCode, startDate, endDate, current, size);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }

    @Operation(summary = "货件明细")
    @GetMapping("/{shipmentId}/items")
    public Result<List<TiktokShipmentItem>> getItems(
            @PathVariable String shipmentId,
            @RequestParam String siteCode) {
        return Result.success(shipmentService.getItems(shipmentId, siteCode));
    }

    @Operation(summary = "导入FBT货件（整合版Excel）")
    @PostMapping("/import")
    public Result<Map<String, Integer>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam String siteCode) {
        try {
            Map<String, Integer> result = shipmentService.importExcel(file, siteCode);
            return Result.success(result);
        } catch (Exception e) {
            log.error("TK FBT货件导入失败", e);
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }
}
