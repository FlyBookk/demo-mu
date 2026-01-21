package com.musheng.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.system.dto.RoleCreateRequest;
import com.musheng.system.dto.RoleUpdateRequest;
import com.musheng.system.dto.RolePermissionRequest;
import com.musheng.system.entity.Role;
import com.musheng.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 * 处理角色增删改查和权限分配
 */
@Tag(name = "角色管理", description = "角色管理相关接口")
@RestController
@RequestMapping("/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表", description = "分页查询角色")
    @GetMapping
    public Result<PageResult<Role>> list(
            @Parameter(description = "角色编码") @RequestParam(required = false) String roleCode,
            @Parameter(description = "角色名称") @RequestParam(required = false) String roleName,
            @Parameter(description = "状态(1启用, 0禁用)") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<Role> pageResult = roleService.list(roleCode, roleName, status, page, size);
        PageResult<Role> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "所有角色", description = "获取所有启用的角色(下拉选择用)")
    @GetMapping("/all")
    public Result<List<Role>> getAll() {
        List<Role> roles = roleService.getAllEnabled();
        return Result.success(roles);
    }

    @Operation(summary = "角色详情", description = "根据ID获取角色")
    @GetMapping("/{id}")
    public Result<Role> getById(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        Role role = roleService.getById(id);
        return Result.success(role);
    }

    @OperationLog(module = "角色管理", operation = "创建角色")
    @Operation(summary = "创建角色", description = "创建新角色")
    @PostMapping
    public Result<Role> create(
            @Valid @RequestBody RoleCreateRequest request) {
        Role role = roleService.create(request);
        return Result.success(role);
    }

    @OperationLog(module = "角色管理", operation = "更新角色")
    @Operation(summary = "更新角色", description = "更新角色信息")
    @PutMapping("/{id}")
    public Result<Role> update(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        Role role = roleService.update(id, request);
        return Result.success(role);
    }

    @OperationLog(module = "角色管理", operation = "删除角色")
    @Operation(summary = "删除角色", description = "根据ID删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "角色管理", operation = "分配权限")
    @Operation(summary = "分配权限", description = "为角色分配权限")
    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Valid @RequestBody RolePermissionRequest request) {
        roleService.assignPermissions(id, request.getPermissions());
        return Result.success();
    }

    @Operation(summary = "获取权限", description = "获取角色的权限列表")
    @GetMapping("/{id}/permissions")
    public Result<List<String>> getPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        List<String> permissions = roleService.getPermissions(id);
        return Result.success(permissions);
    }
}
