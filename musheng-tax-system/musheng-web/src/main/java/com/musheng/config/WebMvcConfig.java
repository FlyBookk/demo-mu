package com.musheng.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * 注意：CORS 和 Sa-Token 认证已移至 SaTokenConfig 中通过 Filter 处理
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // CORS 配置已移至 SaTokenConfig 的 setBeforeAuth 中处理
    // Sa-Token 认证已移至 SaTokenConfig 的 SaServletFilter 中处理
}
