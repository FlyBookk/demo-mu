package com.musheng.system.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 根据缓存类型配置动态排除 Redis 自动配置
 * 
 * 当 sa-token.cache.type 不为 redis 时，排除以下自动配置：
 * - RedisAutoConfiguration
 * - RedisReactiveAutoConfiguration  
 * - RedisRepositoriesAutoConfiguration
 * 
 * 这确保了在使用 Caffeine 本地缓存时，不会因为 Redis 未配置而报错
 * 
 * @author musheng
 */
public class CacheTypeAutoConfigurationExcludeFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    /**
     * 需要排除的 Redis 自动配置类
     */
    private static final Set<String> REDIS_AUTO_CONFIGURATIONS = new HashSet<>(Arrays.asList(
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    ));

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        
        // 获取缓存类型配置，默认为 caffeine
        String cacheType = environment.getProperty("sa-token.cache.type", "caffeine");
        boolean useRedis = "redis".equalsIgnoreCase(cacheType);

        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String configClass = autoConfigurationClasses[i];
            
            if (REDIS_AUTO_CONFIGURATIONS.contains(configClass)) {
                // 如果使用 Redis，则加载 Redis 自动配置
                // 如果使用 Caffeine，则排除 Redis 自动配置
                matches[i] = useRedis;
            } else {
                // 其他自动配置类正常加载
                matches[i] = true;
            }
        }

        return matches;
    }
}
