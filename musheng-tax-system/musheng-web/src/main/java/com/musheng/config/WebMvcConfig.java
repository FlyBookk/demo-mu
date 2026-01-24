package com.musheng.config;

import com.musheng.interceptor.ShopContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * 注意：CORS 和 Sa-Token 认证已移至 SaTokenConfig 中通过 Filter 处理
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ShopContextInterceptor shopContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册店铺上下文拦截器
        registry.addInterceptor(shopContextInterceptor)
                .addPathPatterns("/v1/**")  // 拦截所有业务接口
                .excludePathPatterns(
                        "/v1/auth/**"       // 排除认证接口
                );
    }
}
