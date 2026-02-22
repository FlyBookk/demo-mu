package com.musheng.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.musheng.common.result.Result;
import com.musheng.system.service.AdminDataDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Admin数据物理删除控制器
 * 
 * 提供 admin 账号物理删除数据的能力,支持销售数据、配送数据、FBA货件数据和广告数据的真实删除。
 * 所有接口仅限 admin 角色访问。
 * 
 * @author wanhua
 * 10:30 2026年01月29日
 */
@RestController
@RequestMapping("/v1/admin/data-deletion")
@Tag(name = "Admin数据物理删除")
@Slf4j
@CrossOrigin
public class AdminDataDeletionController {
    
    @Autowired
    private AdminDataDeletionService adminDataDeletionService;

    /**
     * 批量物理删除销售数据
     * 
     * @param ids 数据ID列表
     * @return 删除结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SaCheckRole("admin")
    @DeleteMapping("/sales/batch")
    @Operation(summary = "批量物理删除销售数据")
    public Result<Void> batchDeleteSalesData(@RequestBody java.util.List<Long> ids) {
        try {
            log.info("Admin 尝试批量物理删除销售数据: ids={}", ids);
            adminDataDeletionService.batchDeleteSalesData(ids);
            log.info("批量物理删除销售数据成功: count={}", ids.size());
            return Result.success();
        } catch (Exception e) {
            log.error("批量物理删除销售数据失败: ids={}", ids, e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量物理删除配送数据
     * 
     * @param ids 数据ID列表
     * @return 删除结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SaCheckRole("admin")
    @DeleteMapping("/shipping/batch")
    @Operation(summary = "批量物理删除配送数据")
    public Result<Void> batchDeleteShippingData(@RequestBody java.util.List<Long> ids) {
        try {
            log.info("Admin 尝试批量物理删除配送数据: ids={}", ids);
            adminDataDeletionService.batchDeleteShippingData(ids);
            log.info("批量物理删除配送数据成功: count={}", ids.size());
            return Result.success();
        } catch (Exception e) {
            log.error("批量物理删除配送数据失败: ids={}", ids, e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量物理删除FBA货件数据
     * 
     * @param ids 数据ID列表
     * @return 删除结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SaCheckRole("admin")
    @DeleteMapping("/fba-shipment/batch")
    @Operation(summary = "批量物理删除FBA货件数据")
    public Result<Void> batchDeleteFbaShipmentData(@RequestBody java.util.List<Long> ids) {
        try {
            log.info("Admin 尝试批量物理删除FBA货件数据: ids={}", ids);
            adminDataDeletionService.batchDeleteFbaShipmentData(ids);
            log.info("批量物理删除FBA货件数据成功: count={}", ids.size());
            return Result.success();
        } catch (Exception e) {
            log.error("批量物理删除FBA货件数据失败: ids={}", ids, e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量物理删除广告数据
     * 
     * @param ids 数据ID列表
     * @return 删除结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @SaCheckRole("admin")
    @DeleteMapping("/advertising/batch")
    @Operation(summary = "批量物理删除广告数据")
    public Result<Void> batchDeleteAdvertisingData(@RequestBody java.util.List<Long> ids) {
        try {
            log.info("Admin 尝试批量物理删除广告数据: ids={}", ids);
            adminDataDeletionService.batchDeleteAdvertisingData(ids);
            log.info("批量物理删除广告数据成功: count={}", ids.size());
            return Result.success();
        } catch (Exception e) {
            log.error("批量物理删除广告数据失败: ids={}", ids, e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }

}
