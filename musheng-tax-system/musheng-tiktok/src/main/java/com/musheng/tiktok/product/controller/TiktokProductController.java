package com.musheng.tiktok.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.tiktok.product.entity.TiktokProduct;
import com.musheng.tiktok.product.service.TiktokProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * TK商品库接口
 *
 * @author wanhua
 * 19:20 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/product")
@Tag(name = "TK商品管理")
@Slf4j
@CrossOrigin
public class TiktokProductController {

    @Autowired
    private TiktokProductService productService;

    @Operation(summary = "商品列表")
    @GetMapping("/list")
    public Result<PageResult<TiktokProduct>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam String siteCode,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<TiktokProduct> page = productService.list(keyword, siteCode, current, size);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }

    @Operation(summary = "导入SKU对照表")
    @PostMapping("/import")
    public Result<Map<String, Integer>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam String siteCode) {
        try {
            Map<String, Integer> result = productService.importExcel(file, siteCode);
            return Result.success(result);
        } catch (Exception e) {
            log.error("TK商品库导入失败", e);
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新商品MSKU")
    @PutMapping("/{id}/msku")
    public Result<Void> updateMsku(@PathVariable Long id, @RequestParam String msku) {
        productService.updateMsku(id, msku);
        return Result.success();
    }
}
