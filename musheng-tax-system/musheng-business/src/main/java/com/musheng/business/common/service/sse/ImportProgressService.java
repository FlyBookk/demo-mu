package com.musheng.business.common.service.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Import Progress SSE Service Interface
 * P0 Requirement: Import progress SSE API
 */
public interface ImportProgressService {

    /**
     * Create SSE emitter for client to subscribe
     *
     * @param batchNo Import batch number
     * @return SSE emitter
     */
    SseEmitter subscribe(String batchNo);

    /**
     * Send progress update to subscribers
     *
     * @param progress Import progress
     */
    void sendProgress(ImportProgress progress);

    /**
     * Complete SSE connection
     *
     * @param batchNo Import batch number
     */
    void complete(String batchNo);

    /**
     * Send error and complete SSE connection
     *
     * @param batchNo      Import batch number
     * @param errorMessage Error message
     */
    void error(String batchNo, String errorMessage);

    /**
     * Get current progress for a batch
     *
     * @param batchNo Import batch number
     * @return Current progress or null if not found
     */
    ImportProgress getProgress(String batchNo);
}
