package com.musheng.business.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 查询条件构建工具类
 * 提供通用的查询条件构建方法，减少重复代码
 *
 * @author wanhua
 * 18:30 2026年02月01日
 */
@Slf4j
public final class QueryWrapperUtils {

    private QueryWrapperUtils() {
        // 工具类禁止实例化
    }

    /**
     * 添加店铺ID过滤条件
     *
     * @param wrapper 查询包装器
     * @param shopIdGetter 店铺ID字段的getter方法引用
     * @param shopId 店铺ID
     * @param <T> 实体类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T> void applyShopIdFilter(LambdaQueryWrapper<T> wrapper,
                                              SFunction<T, Long> shopIdGetter,
                                              Long shopId) {
        if (shopId != null) {
            wrapper.eq(shopIdGetter, shopId);
        }
    }

    /**
     * 添加日期范围过滤条件（使用 LocalDateTime）
     *
     * @param wrapper 查询包装器
     * @param dateGetter 日期字段的getter方法引用
     * @param startDate 开始日期字符串（格式：yyyy-MM-dd）
     * @param endDate 结束日期字符串（格式：yyyy-MM-dd）
     * @param <T> 实体类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T> void applyDateRangeFilter(LambdaQueryWrapper<T> wrapper,
                                                 SFunction<T, LocalDateTime> dateGetter,
                                                 String startDate,
                                                 String endDate) {
        LocalDateTime start = DateParseUtils.parseStartDate(startDate);
        if (start != null) {
            wrapper.ge(dateGetter, start);
        }
        LocalDateTime end = DateParseUtils.parseEndDate(endDate);
        if (end != null) {
            wrapper.le(dateGetter, end);
        }
    }

    /**
     * 添加模糊查询过滤条件
     *
     * @param wrapper 查询包装器
     * @param fieldGetter 字段的getter方法引用
     * @param value 查询值
     * @param <T> 实体类型
     * @param <V> 字段值类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T, V> void applyLikeFilter(LambdaQueryWrapper<T> wrapper,
                                               SFunction<T, V> fieldGetter,
                                               String value) {
        if (StringUtils.hasText(value)) {
            wrapper.like(fieldGetter, value);
        }
    }

    /**
     * 添加精确匹配过滤条件（字符串类型）
     *
     * @param wrapper 查询包装器
     * @param fieldGetter 字段的getter方法引用
     * @param value 查询值
     * @param <T> 实体类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T> void applyEqFilter(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, String> fieldGetter,
                                          String value) {
        if (StringUtils.hasText(value)) {
            wrapper.eq(fieldGetter, value);
        }
    }

    /**
     * 添加精确匹配过滤条件（Long类型）
     *
     * @param wrapper 查询包装器
     * @param fieldGetter 字段的getter方法引用
     * @param value 查询值
     * @param <T> 实体类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T> void applyEqFilter(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Long> fieldGetter,
                                          Long value) {
        if (value != null) {
            wrapper.eq(fieldGetter, value);
        }
    }

    /**
     * 添加精确匹配过滤条件（Integer类型）
     *
     * @param wrapper 查询包装器
     * @param fieldGetter 字段的getter方法引用
     * @param value 查询值
     * @param <T> 实体类型
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static <T> void applyEqFilter(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Integer> fieldGetter,
                                          Integer value) {
        if (value != null) {
            wrapper.eq(fieldGetter, value);
        }
    }
}
