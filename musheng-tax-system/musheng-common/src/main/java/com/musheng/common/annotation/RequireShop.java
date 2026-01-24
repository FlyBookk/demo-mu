package com.musheng.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要店铺上下文的接口
 * <p>
 * 使用此注解的 Controller 方法必须在请求头中携带有效的 X-Shop-Id
 * 如果没有店铺ID，将直接返回错误响应
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireShop {
    
    /**
     * 错误提示信息
     */
    String message() default "请先选择店铺后再操作";
}
