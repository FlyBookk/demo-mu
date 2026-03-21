package com.musheng.common.service;

/**
 * 系统配置服务接口
 *
 * @author wanhua
 * 21:35 2026年03月21日
 */
public interface SysConfigService {

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     * @author wanhua
     * 21:35 2026年03月21日
     */
    String getValue(String key);

    /**
     * 更新配置值
     *
     * @param key 配置键
     * @param value 配置值
     * @author wanhua
     * 21:35 2026年03月21日
     */
    void updateValue(String key, String value);

    /**
     * 获取布尔配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔值
     * @author wanhua
     * 21:35 2026年03月21日
     */
    default boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}
