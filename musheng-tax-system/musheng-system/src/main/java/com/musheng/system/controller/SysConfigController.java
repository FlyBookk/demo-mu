package com.musheng.system.controller;

import com.musheng.common.result.Result;
import com.musheng.common.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author wanhua
 * 21:35 2026年03月21日
 */
@RestController
@RequestMapping("/v1/sys-config")
@Tag(name = "系统配置")
@Slf4j
@CrossOrigin
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     * @author wanhua
     * 21:35 2026年03月21日
     */
    @GetMapping("/{key}")
    @Operation(summary = "获取配置值")
    public Result<String> getValue(@PathVariable String key) {
        return Result.success(sysConfigService.getValue(key));
    }

    /**
     * 更新配置值
     *
     * @param key 配置键
     * @param body 请求体，包含 value 字段
     * @return 操作结果
     * @author wanhua
     * 21:35 2026年03月21日
     */
    @PutMapping("/{key}")
    @Operation(summary = "更新配置值")
    public Result<Void> updateValue(@PathVariable String key, @RequestBody Map<String, String> body) {
        sysConfigService.updateValue(key, body.get("value"));
        return Result.success(null);
    }
}
