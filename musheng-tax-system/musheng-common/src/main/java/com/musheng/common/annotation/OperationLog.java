package com.musheng.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作类型：CREATE, UPDATE, DELETE, IMPORT, EXPORT, LOGIN, LOGOUT 等
     */
    String operation() default "";
}
