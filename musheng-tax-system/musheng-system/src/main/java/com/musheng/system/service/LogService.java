package com.musheng.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.system.entity.OperationLog;

import java.time.LocalDateTime;

/**
 * Operation Log Service Interface
 */
public interface LogService {

    /**
     * Query operation logs with pagination
     *
     * @param username  Username (optional)
     * @param operation Operation type (optional)
     * @param module    Module (optional)
     * @param status    Status (optional)
     * @param startTime Start time (optional)
     * @param endTime   End time (optional)
     * @param page      Page number
     * @param size      Page size
     * @return Paginated result
     */
    Page<OperationLog> list(String username, String operation, String module, Integer status,
                            LocalDateTime startTime, LocalDateTime endTime, int page, int size);

    /**
     * Get operation log by ID
     *
     * @param id Log ID
     * @return OperationLog entity
     */
    OperationLog getById(Long id);

    /**
     * Query operation logs by user ID
     *
     * @param userId User ID
     * @param page   Page number
     * @param size   Page size
     * @return Paginated result
     */
    Page<OperationLog> getByUserId(Long userId, int page, int size);

    /**
     * Save operation log
     *
     * @param log OperationLog entity
     */
    void save(OperationLog log);
}
