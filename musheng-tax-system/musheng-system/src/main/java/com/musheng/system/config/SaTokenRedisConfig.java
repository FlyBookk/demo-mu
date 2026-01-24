package com.musheng.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Sa-Token Redis 缓存配置
 * 
 * 仅当满足以下条件时才会加载 Redis 配置：
 * 1. sa-token.cache.type = redis
 * 2. Redis 相关类存在于 classpath 中
 * 
 * 这样设计确保了：
 * - 不配置 Redis 时系统正常启动
 * - 配置为 caffeine 时不会尝试连接 Redis
 * - 只有明确配置 redis 时才会启用 Redis 缓存
 * 
 * @author musheng
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "sa-token.cache",
        name = "type",
        havingValue = "redis"
)
@ConditionalOnClass(name = {
        "org.springframework.data.redis.core.RedisTemplate",
        "cn.dev33.satoken.dao.SaTokenDaoRedisJackson"
})
public class SaTokenRedisConfig {

    /**
     * 构造函数 - 打印 Redis 模式启用日志
     */
    public SaTokenRedisConfig() {
        log.info("========================================");
        log.info("Sa-Token 缓存模式: Redis (分布式缓存)");
        log.info("适用于: 集群部署、需要会话共享的场景");
        log.info("请确保 Redis 服务已启动并正确配置");
        log.info("========================================");
    }

    // Redis 模式下，sa-token-redis-jackson 会自动注册 SaTokenDaoRedisJackson
    // 无需在此手动配置
}
