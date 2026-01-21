package com.musheng.business.rate.scheduler;

import com.musheng.business.rate.dto.RateSyncResultDTO;
import com.musheng.business.rate.service.RateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 汇率自动同步定时任务
 *
 * 配置说明：
 * - 在 application.yml 中配置 rate.sync.enabled=true 启用自动同步
 * - 默认每天早上 8:00 同步最近 3 天的汇率
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rate.sync", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RateSyncScheduler {

    private final RateSyncService rateSyncService;

    /**
     * 每天早上 8:00 自动同步最近 3 天的汇率
     * Cron 表达式: 0 0 8 * * ?
     * - 秒: 0
     * - 分: 0
     * - 时: 8
     * - 日: *（每天）
     * - 月: *（每月）
     * - 周: ?（不指定）
     */
    @Scheduled(cron = "${rate.sync.cron:0 0 8 * * ?}")
    public void syncRatesDaily() {
        log.info("Starting scheduled rate sync task");

        try {
            // 同步最近 3 天的汇率（覆盖周末和节假日）
            int days = 3;
            RateSyncResultDTO result = rateSyncService.syncRecentDays(days);

            if (result.getSuccess()) {
                log.info("Scheduled rate sync completed successfully: {}",  result.getMessage());
                log.info("Sync details: total={}, inserted={}, updated={}, failed={}, duration={}ms",
                        result.getTotalCount(),
                        result.getInsertCount(),
                        result.getUpdateCount(),
                        result.getFailCount(),
                        result.getDurationMs());
            } else {
                log.error("Scheduled rate sync failed: {}", result.getMessage());
            }

        } catch (Exception e) {
            log.error("Scheduled rate sync encountered an error", e);
        }
    }

    /**
     * 每小时检查一次汇率数据完整性（可选）
     * 检查今天的汇率是否已同步
     */
    @Scheduled(cron = "${rate.sync.check.cron:0 0 * * * ?}")
    @ConditionalOnProperty(prefix = "rate.sync.check", name = "enabled", havingValue = "true", matchIfMissing = false)
    public void checkTodayRates() {
        log.info("Checking today's rate availability");

        try {
            // 同步今天的汇率（如果缺失）
            RateSyncResultDTO result = rateSyncService.syncRecentDays(1);

            if (result.getInsertCount() > 0) {
                log.info("Found missing rates for today, synced {} new records", result.getInsertCount());
            } else {
                log.debug("Today's rates are up to date");
            }

        } catch (Exception e) {
            log.warn("Failed to check today's rates", e);
        }
    }
}
