package com.musheng.business.common.service.sse;

import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Import Progress SSE Controller
 * P0 Requirement: Import progress SSE API
 */
@Tag(name = "Import Progress", description = "Import progress SSE APIs")
@RestController
@RequestMapping("/v1/import/progress")
@RequiredArgsConstructor
public class ImportProgressController {

    private final ImportProgressService importProgressService;

    @Operation(summary = "Subscribe to Import Progress (SSE)",
            description = "Subscribe to real-time import progress updates via Server-Sent Events")
    @GetMapping(value = "/subscribe/{batchNo}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "Import batch number") @PathVariable String batchNo) {
        return importProgressService.subscribe(batchNo);
    }

    @Operation(summary = "Get Current Progress",
            description = "Get current import progress (for polling fallback)")
    @GetMapping("/{batchNo}")
    public Result<ImportProgress> getProgress(
            @Parameter(description = "Import batch number") @PathVariable String batchNo) {
        ImportProgress progress = importProgressService.getProgress(batchNo);
        return Result.success(progress);
    }
}
