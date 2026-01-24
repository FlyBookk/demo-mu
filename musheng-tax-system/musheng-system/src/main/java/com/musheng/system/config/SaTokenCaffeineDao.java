package com.musheng.system.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.util.SaFoxUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 基于 Caffeine 的本地缓存实现
 * 
 * 用于替代 Redis，适用于单机部署的内部系统
 * 
 * @author musheng
 */
@Slf4j
public class SaTokenCaffeineDao implements SaTokenDao {

    /**
     * 数据缓存 - 支持动态过期时间
     */
    private final Cache<String, Object> dataCache;

    /**
     * Session 缓存 - 支持动态过期时间
     */
    private final Cache<String, Object> sessionCache;

    /**
     * 过期时间记录（毫秒时间戳）
     */
    private final Map<String, Long> expireMap = new ConcurrentHashMap<>();

    public SaTokenCaffeineDao() {
        // 初始化数据缓存
        this.dataCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        Long expireAt = expireMap.get(key);
                        if (expireAt == null || expireAt == -1) {
                            return Long.MAX_VALUE; // 永不过期
                        }
                        long remaining = expireAt - System.currentTimeMillis();
                        return remaining > 0 ? TimeUnit.MILLISECONDS.toNanos(remaining) : 0;
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                        return expireAfterCreate(key, value, currentTime);
                    }

                    @Override
                    public long expireAfterRead(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();

        // 初始化 Session 缓存
        this.sessionCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        Long expireAt = expireMap.get(key);
                        if (expireAt == null || expireAt == -1) {
                            return Long.MAX_VALUE;
                        }
                        long remaining = expireAt - System.currentTimeMillis();
                        return remaining > 0 ? TimeUnit.MILLISECONDS.toNanos(remaining) : 0;
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                        return expireAfterCreate(key, value, currentTime);
                    }

                    @Override
                    public long expireAfterRead(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();

        log.info("Sa-Token Caffeine 本地缓存初始化完成");
    }

    // ==================== String 读写 ====================

    @Override
    public String get(String key) {
        Object value = dataCache.getIfPresent(key);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || isEmpty(value)) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            expireMap.put(key, -1L);
        } else {
            expireMap.put(key, System.currentTimeMillis() + timeout * 1000);
        }
        dataCache.put(key, value);
    }

    @Override
    public void update(String key, String value) {
        if (getTimeout(key) == NOT_VALUE_EXPIRE) {
            return;
        }
        dataCache.put(key, value);
    }

    @Override
    public void delete(String key) {
        dataCache.invalidate(key);
        expireMap.remove(key);
    }

    @Override
    public long getTimeout(String key) {
        Long expireAt = expireMap.get(key);
        if (expireAt == null) {
            return NOT_VALUE_EXPIRE;
        }
        if (expireAt == -1) {
            return NEVER_EXPIRE;
        }
        long remaining = (expireAt - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? remaining : NOT_VALUE_EXPIRE;
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        Long expireAt = expireMap.get(key);
        if (expireAt == null) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            expireMap.put(key, -1L);
        } else {
            expireMap.put(key, System.currentTimeMillis() + timeout * 1000);
        }
        // 触发缓存刷新
        Object value = dataCache.getIfPresent(key);
        if (value != null) {
            dataCache.put(key, value);
        }
    }

    // ==================== Object 读写 ====================

    @Override
    public Object getObject(String key) {
        return sessionCache.getIfPresent(key);
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        if (timeout == 0 || object == null) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            expireMap.put(key, -1L);
        } else {
            expireMap.put(key, System.currentTimeMillis() + timeout * 1000);
        }
        sessionCache.put(key, object);
    }

    @Override
    public void updateObject(String key, Object object) {
        if (getObjectTimeout(key) == NOT_VALUE_EXPIRE) {
            return;
        }
        sessionCache.put(key, object);
    }

    @Override
    public void deleteObject(String key) {
        sessionCache.invalidate(key);
        expireMap.remove(key);
    }

    @Override
    public long getObjectTimeout(String key) {
        return getTimeout(key);
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        Long expireAt = expireMap.get(key);
        if (expireAt == null) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            expireMap.put(key, -1L);
        } else {
            expireMap.put(key, System.currentTimeMillis() + timeout * 1000);
        }
        // 触发缓存刷新
        Object value = sessionCache.getIfPresent(key);
        if (value != null) {
            sessionCache.put(key, value);
        }
    }

    // ==================== 搜索相关 ====================

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        List<String> keys = new ArrayList<>();
        for (String key : dataCache.asMap().keySet()) {
            if (key.startsWith(prefix) && (isEmpty(keyword) || key.contains(keyword))) {
                keys.add(key);
            }
        }
        return SaFoxUtil.searchList(keys, start, size, sortType);
    }

    /**
     * 判断字符串是否为空
     */
    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
