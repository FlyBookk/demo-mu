package com.musheng.business.common.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Import Progress SSE Service Implementation
 * P0 Requirement: Import progress SSE API
 */
@Slf4j
@Service
public class ImportProgressServiceImpl implements ImportProgressService {

    /**
     * SSE emitter timeout (10 minutes)
     */
    private static final long SSE_TIMEOUT = 600000L;

    /**
     * Store SSE emitters by batch number
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Store current progress by batch number
     */
    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String batchNo) {
        // Remove existing emitter if any
        SseEmitter existingEmitter = emitters.remove(batchNo);
        if (existingEmitter != null) {
            existingEmitter.complete();
        }

        // Create new emitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Set callbacks
        emitter.onCompletion(() -> {
            emitters.remove(batchNo);
            log.info("SSE connection completed: batchNo={}", batchNo);
        });

        emitter.onTimeout(() -> {
            emitters.remove(batchNo);
            log.info("SSE connection timeout: batchNo={}", batchNo);
        });

        emitter.onError(throwable -> {
            emitters.remove(batchNo);
            log.warn("SSE connection error: batchNo={}, error={}", batchNo, throwable.getMessage());
        });

        emitters.put(batchNo, emitter);
        log.info("SSE connection established: batchNo={}", batchNo);

        // Send initial progress if exists
        ImportProgress existingProgress = progressMap.get(batchNo);
        if (existingProgress != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(existingProgress));
            } catch (IOException e) {
                log.warn("Failed to send initial progress: batchNo={}", batchNo);
            }
        }

        return emitter;
    }

    @Override
    public void sendProgress(ImportProgress progress) {
        String batchNo = progress.getBatchNo();

        // Store progress
        progressMap.put(batchNo, progress);

        // Send to subscriber
        SseEmitter emitter = emitters.get(batchNo);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));
                log.debug("Progress sent: batchNo={}, percentage={}%", batchNo, progress.getPercentage());
            } catch (IOException e) {
                log.warn("Failed to send progress: batchNo={}", batchNo);
                emitters.remove(batchNo);
            }
        }
    }

    @Override
    public void complete(String batchNo) {
        SseEmitter emitter = emitters.remove(batchNo);
        if (emitter != null) {
            try {
                // Send complete event
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("Import completed"));
                emitter.complete();
                log.info("SSE connection completed successfully: batchNo={}", batchNo);
            } catch (IOException e) {
                log.warn("Failed to complete SSE: batchNo={}", batchNo);
            }
        }

        // Keep progress for a while for polling clients
        // Progress will be cleaned up by scheduled task
    }

    @Override
    public void error(String batchNo, String errorMessage) {
        // Update progress with error
        ImportProgress errorProgress = ImportProgress.failed(batchNo, errorMessage);
        progressMap.put(batchNo, errorProgress);

        // Send error and complete
        SseEmitter emitter = emitters.remove(batchNo);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorProgress));
                emitter.complete();
                log.info("SSE error sent: batchNo={}, error={}", batchNo, errorMessage);
            } catch (IOException e) {
                log.warn("Failed to send error: batchNo={}", batchNo);
            }
        }
    }

    @Override
    public ImportProgress getProgress(String batchNo) {
        return progressMap.get(batchNo);
    }

    /**
     * Clean up old progress entries (called by scheduled task)
     */
    public void cleanupOldProgress() {
        // In production, implement cleanup based on timestamp
        // For now, keep all progress
    }
}
