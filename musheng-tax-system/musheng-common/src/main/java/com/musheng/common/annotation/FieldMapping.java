package com.musheng.common.annotation;

import java.lang.annotation.*;

/**
 * 字段映射注解
 * 用于标注实体类字段的映射元信息，自动生成目标字段配置
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldMapping {

    /**
     * 中文标签
     */
    String label();

    /**
     * 字段描述
     */
    String description() default "";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 最大长度（String 类型有效）
     */
    int maxLength() default 0;

    /**
     * 精度（Number 类型有效）
     */
    int precision() default 2;

    /**
     * 排序序号，数值越小越靠前
     */
    int order() default 100;

    /**
     * 是否忽略此字段（不作为目标字段）
     */
    boolean ignore() default false;
}
