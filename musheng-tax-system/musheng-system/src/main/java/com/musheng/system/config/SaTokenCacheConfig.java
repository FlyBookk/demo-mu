package com.musheng.system.config;

import cn.dev33.satoken.dao.SaTokenDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 缓存配置类
 * 
 * 根据配置文件中的 sa-token.cache.type 来决定使用哪种缓存：
 * - caffeine (默认): 使用 Caffeine 本地缓存，适合单机部署
 * - redis: 使用 Redis 分布式缓存，适合集群部署
 * 
 * 配置示例：
 * sa-token:
 *   cache:
 *     type: caffeine  # 或 redis
 * 
 * @author musheng
 */
@Slf4j
@Configuration
public class SaTokenCacheConfig {

    /**
     * 使用 Caffeine 本地缓存
     * 
     * 条件：sa-token.cache.type 为 caffeine 或未配置时生效
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "sa-token.cache",
            name = "type",
            havingValue = "caffeine",
            matchIfMissing = true  // 默认使用 Caffeine
    )
    public SaTokenDao saTokenDaoCaffeine() {
        log.info("========================================");
        log.info("Sa-Token 缓存模式: Caffeine (本地缓存)");
        log.info("适用于: 单机部署、内部系统");
        log.info("注意: 服务重启后登录状态会丢失");
        log.info("========================================");
        return new SaTokenCaffeineDao();
    }
}
