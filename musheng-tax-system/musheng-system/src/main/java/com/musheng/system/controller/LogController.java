package com.musheng.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.system.entity.OperationLog;
import com.musheng.system.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Operation Log Controller
 * Handles operation log queries
 */
@Tag(name = "Operation Log", description = "Operation log management APIs")
@RestController
@RequestMapping("/v1/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "List Operation Logs", description = "Query operation logs with pagination")
    @GetMapping
    public Result<PageResult<OperationLog>> list(
            @Parameter(description = "Username") @RequestParam(required = false) String username,
            @Parameter(description = "Operation") @RequestParam(required = false) String operation,
            @Parameter(description = "Module") @RequestParam(required = false) String module,
            @Parameter(description = "Status (1-success, 0-fail)") @RequestParam(required = false) Integer status,
            @Parameter(description = "Start time") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Page<OperationLog> pageResult = logService.list(username, operation, module, status, startTime, endTime, page, size);
        PageResult<OperationLog> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "Get Log Detail", description = "Get operation log by ID")
    @GetMapping("/{id}")
    public Result<OperationLog> getById(
            @Parameter(description = "Log ID") @PathVariable Long id) {
        OperationLog log = logService.getById(id);
        return Result.success(log);
    }

    @Operation(summary = "Get User Operation Logs", description = "Query operation logs by user ID")
    @GetMapping("/user/{userId}")
    public Result<PageResult<OperationLog>> getByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Page<OperationLog> pageResult = logService.getByUserId(userId, page, size);
        PageResult<OperationLog> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }
}
