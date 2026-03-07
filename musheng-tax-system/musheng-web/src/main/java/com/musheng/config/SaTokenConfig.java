package com.musheng.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;

/**
 * Sa-Token过滤器配置类
 * 
 * 使用 Filter 而不是 Interceptor，确保 SaHolder.getRequest() 能正确获取请求信息
 */
@Configuration
public class SaTokenConfig {

    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                // 拦截所有路由
                .addInclude("/**")
                // 放行的路由
                .addExclude("/favicon.ico", "/error")
                // 认证函数
                .setAuth(obj -> {
                    // 登录校验：排除白名单路由
                    SaRouter.match("/**")
                            .notMatch(
                                    "/v1/auth/login",
                                    "/v1/auth/logout",
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/doc.html",
                                    "/webjars/**"
                            )
                            .check(r -> StpUtil.checkLogin());

                    // 管理员路由权限校验
                    SaRouter.match("/v1/system/**")
                            .check(r -> StpUtil.checkRole("admin"));

                    // admin 数据删除功能权限校验
                    SaRouter.match("/v1/admin/data-deletion/**")
                            .check(r -> StpUtil.checkRole("admin"));
                })
                // 异常处理函数
                .setError(e -> {
                    // 设置响应头
                    SaHolder.getResponse()
                            .setHeader("Content-Type", "application/json;charset=UTF-8");
                    // 返回错误信息
                    return SaResult.error(e.getMessage()).setCode(401);
                })
                // 前置函数：处理跨域预检请求
                .setBeforeAuth(obj -> {
                    SaHolder.getResponse()
                            // 允许指定域访问跨域资源
                            .setHeader("Access-Control-Allow-Origin", "*")
                            // 允许所有请求方式
                            .setHeader("Access-Control-Allow-Methods", "*")
                            // 允许的请求头
                            .setHeader("Access-Control-Allow-Headers", "*")
                            // 暴露给前端的响应头（文件下载时前端需要读取 Content-Disposition 获取文件名）
                            .setHeader("Access-Control-Expose-Headers", "Content-Disposition")
                            // 有效时间
                            .setHeader("Access-Control-Max-Age", "3600");

                    // 如果是预检请求，直接返回
                    if (SaRouter.isMatchCurrURI("/**") && 
                        SaHolder.getRequest().getMethod().equalsIgnoreCase(SaHttpMethod.OPTIONS.name())) {
                        SaRouter.back();
                    }
                });
    }
}
