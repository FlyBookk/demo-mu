package com.musheng.business.rate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 中国外汇交易中心配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "china-money")
public class ChinaMoneyConfig {

    /**
     * Cookie 配置（可选）
     * 用于绕过反爬虫机制，需要从浏览器获取有效的 Cookie
     * 格式示例: "lss=xxx; _ulta_id.CM-Prod.e9dc=xxx; AlteonP10=xxx"
     */
    private String cookie;

    /**
     * 是否启用 Cookie（默认不启用）
     */
    private boolean enableCookie = false;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 30000;
}
