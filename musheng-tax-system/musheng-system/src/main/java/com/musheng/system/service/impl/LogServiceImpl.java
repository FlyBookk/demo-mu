package com.musheng.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.system.entity.OperationLog;
import com.musheng.system.mapper.OperationLogMapper;
import com.musheng.system.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Operation Log Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public Page<OperationLog> list(String username, String operation, String module, Integer status,
                                   LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(OperationLog::getCreateTime);

        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public OperationLog getById(Long id) {
        OperationLog operationLog = operationLogMapper.selectById(id);
        if (operationLog == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Operation log not found");
        }
        return operationLog;
    }

    @Override
    public Page<OperationLog> getByUserId(Long userId, int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getUserId, userId);
        wrapper.orderByDesc(OperationLog::getCreateTime);

        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Async
    public void save(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }
    }
}
