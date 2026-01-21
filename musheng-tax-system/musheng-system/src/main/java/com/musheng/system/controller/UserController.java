package com.musheng.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.system.dto.UserCreateRequest;
import com.musheng.system.dto.UserUpdateRequest;
import com.musheng.system.entity.User;
import com.musheng.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户增删改查、密码重置和状态管理
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表", description = "分页查询用户")
    @GetMapping
    public Result<PageResult<User>> list(
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "角色编码") @RequestParam(required = false) String roleCode,
            @Parameter(description = "状态(1启用, 0禁用)") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<User> pageResult = userService.list(username, realName, roleCode, status, page, size);
        PageResult<User> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "用户详情", description = "根据ID获取用户")
    @GetMapping("/{id}")
    public Result<User> getById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    @OperationLog(module = "用户管理", operation = "创建用户")
    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<User> create(
            @Valid @RequestBody UserCreateRequest request) {
        User user = userService.create(request);
        return Result.success(user);
    }

    @OperationLog(module = "用户管理", operation = "更新用户")
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/{id}")
    public Result<User> update(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User user = userService.update(id, request);
        return Result.success(user);
    }

    @OperationLog(module = "用户管理", operation = "删除用户")
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "重置密码")
    @Operation(summary = "重置密码", description = "重置用户密码为默认密码")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "切换状态")
    @Operation(summary = "切换状态", description = "启用或禁用用户")
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "启用用户")
    @Operation(summary = "启用用户", description = "启用用户")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.enable(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "禁用用户")
    @Operation(summary = "禁用用户", description = "禁用用户")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.disable(id);
        return Result.success();
    }
}
