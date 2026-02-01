package com.musheng.business.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期解析工具类
 * 
 * 提供统一的日期字符串解析方法，用于替换各 Service 中重复的日期解析逻辑。
 * 
 * ⚠️ 重要：此工具类的行为必须与原有 Service 中的日期解析逻辑完全一致，
 * 以确保重构不改变任何业务输出。
 * 
 * 原有逻辑来源：
 * - SalesDataServiceImpl.parseStartDate()
 * - SalesDataServiceImpl.parseEndDate()
 * - RateServiceImpl 中的日期解析
 * 
 * @author wanhua
 * 18:10 2026年02月01日
 */
@Slf4j
public final class DateParseUtils {
    
    private DateParseUtils() {
        // 工具类禁止实例化
    }
    
    /**
     * 解析开始日期字符串为 LocalDateTime
     * 
     * 返回当天的 00:00:00 时刻
     * 
     * @param dateStr 日期字符串，格式：YYYY-MM-DD
     * @return LocalDateTime 或 null（输入为空或格式错误时）
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static LocalDateTime parseStartDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            log.warn("无效的开始日期格式: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 解析结束日期字符串为 LocalDateTime
     * 
     * 返回当天的 23:59:59 时刻
     * 
     * @param dateStr 日期字符串，格式：YYYY-MM-DD
     * @return LocalDateTime 或 null（输入为空或格式错误时）
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static LocalDateTime parseEndDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atTime(23, 59, 59);
        } catch (Exception e) {
            log.warn("无效的结束日期格式: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 解析日期字符串为 LocalDate
     * 
     * @param dateStr 日期字符串，格式：YYYY-MM-DD
     * @return LocalDate 或 null（输入为空或格式错误时）
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("无效的日期格式: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 检查日期字符串是否有效
     * 
     * @param dateStr 日期字符串
     * @return true 如果有效，false 如果无效或为空
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static boolean isValidDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return false;
        }
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
