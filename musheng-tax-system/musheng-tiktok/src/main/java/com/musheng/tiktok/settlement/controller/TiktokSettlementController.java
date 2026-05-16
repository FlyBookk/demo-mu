package com.musheng.tiktok.settlement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.tiktok.settlement.entity.TiktokSettlement;
import com.musheng.tiktok.settlement.entity.TiktokSettlementOrder;
import com.musheng.tiktok.settlement.service.TiktokSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * TK结算单接口
 *
 * @author wanhua
 * 19:48 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/settlement")
@Tag(name = "TK结算数据管理")
@Slf4j
@CrossOrigin
public class TiktokSettlementController {

    @Autowired
    private TiktokSettlementService settlementService;

    @Operation(summary = "预校验（检查商品映射完整性）")
    @PostMapping("/validate")
    public Result<?> validate(
            @RequestParam("file") MultipartFile file,
            @RequestParam String siteCode) {
        try {
            List<String> unmapped = settlementService.preValidate(file, siteCode);
            if (unmapped.isEmpty()) {
                return Result.success(Map.of("valid", true, "message", "校验通过"));
            } else {
                return Result.success(Map.of("valid", false, "unmappedSkuIds", unmapped,
                        "message", "有" + unmapped.size() + "个SKU未在商品库中找到映射，请先完善商品库"));
            }
        } catch (Exception e) {
            log.error("TK结算单预校验失败", e);
            return Result.error(500, "校验失败: " + e.getMessage());
        }
    }

    @Operation(summary = "导入结算单")
    @PostMapping("/import")
    public Result<Map<String, Integer>> importSettlement(
            @RequestParam("file") MultipartFile file,
            @RequestParam String siteCode) {
        try {
            Map<String, Integer> result = settlementService.importSettlement(file, siteCode);
            return Result.success(result);
        } catch (Exception e) {
            log.error("TK结算单导入失败", e);
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "订单明细列表")
    @GetMapping("/orders")
    public Result<PageResult<TiktokSettlementOrder>> listOrders(
            @RequestParam String siteCode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String msku,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Boolean unmappedOnly,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<TiktokSettlementOrder> page = settlementService.listOrders(siteCode, type, msku, startDate, endDate, unmappedOnly, current, size);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }

    @Operation(summary = "结算汇总列表")
    @GetMapping("/statements")
    public Result<PageResult<TiktokSettlement>> listStatements(
            @RequestParam String siteCode,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<TiktokSettlement> page = settlementService.listStatements(siteCode, current, size);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }
}
