package com.musheng.business.rate.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 汇率查询缓存配置
 *
 * 使用 Caffeine 本地缓存，解决批量导入时的 N+1 查询问题。
 * 缓存 key: currencyCode_date（如 USD_2025-01-01）
 * 页面汇率数据变更（新增/修改/删除/导入/同步）时需主动清除缓存。
 *
 * @author wanhua
 */
@Configuration
public class ExchangeRateCacheConfig {

    /**
     * 汇率查询缓存
     * - 最大 10000 条
     * - 写入后 24 小时过期
     * - 记录统计信息便于监控
     */
    @Bean("exchangeRateCache")
    public Cache<String, Object> exchangeRateCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
