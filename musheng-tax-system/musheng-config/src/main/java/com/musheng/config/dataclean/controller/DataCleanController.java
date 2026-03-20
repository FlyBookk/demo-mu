package com.musheng.config.dataclean.controller;

import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.Result;
import com.musheng.config.dataclean.dto.DataCleanModuleVO;
import com.musheng.config.dataclean.service.DataCleanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据清理控制器
 *
 * @author wanhua
 * 12:40 2026年03月08日
 */
@Tag(name = "数据清理", description = "按模块清理业务数据")
@RestController
@RequestMapping("/v1/config/data-clean")
@RequiredArgsConstructor
@Slf4j
public class DataCleanController {

    private final DataCleanService dataCleanService;

    /**
     * 获取可清理的模块列表
     *
     * @return 模块列表
     * @author wanhua
     * 12:40 2026年03月08日
     */
    @Operation(summary = "获取可清理模块", description = "获取所有可清理的业务数据模块及其数据量")
    @GetMapping("/modules")
    public Result<List<DataCleanModuleVO>> getModules() {
        List<DataCleanModuleVO> modules = dataCleanService.getModules();
        return Result.success(modules);
    }

    /**
     * 按模块清理数据
     *
     * @param moduleCode 模块编码
     * @param siteCode   站点编码（可选，为空则清理所有站点）
     * @return 清理结果
     * @author wanhua
     * 12:40 2026年03月08日
     */
    @OperationLog(module = "数据清理", operation = "清理模块数据")
    @Operation(summary = "清理模块数据", description = "按模块编码清理当前店铺的业务数据，可选指定站点")
    @DeleteMapping("/modules/{moduleCode}")
    public Result<Integer> cleanModule(
            @Parameter(description = "模块编码") @PathVariable String moduleCode,
            @Parameter(description = "站点编码，为空则清理所有站点") @RequestParam(required = false) String siteCode) {
        int count = dataCleanService.cleanModule(moduleCode, siteCode);
        return Result.success(count);
    }
}
