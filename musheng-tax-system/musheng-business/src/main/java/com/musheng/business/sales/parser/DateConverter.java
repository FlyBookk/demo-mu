package com.musheng.business.sales.parser;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期转换器
 * 使用 Hutool DateUtil 处理多种日期格式
 * 
 * 支持的格式包括：
 * - 美国站: Jul 1, 2025 12:01:49 AM PDT
 * - 德国站: 01.07.2025 00:01:49 UTC
 * - 英国站: 1 Jul 2025 00:01:49 BST
 * - ERP数据: 2025-09-30 23:59:08
 * - ISO格式: 2025-07-01T12:01:49
 * - 中文格式: 2025年07月01日 12:01:49
 * - 以及更多 Hutool 支持的格式
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Slf4j
public class DateConverter {
    
    /**
     * 时区缩写到ZoneId的映射
     */
    private static final Map<String, ZoneId> TIMEZONE_MAP = new HashMap<>();
    
    /**
     * 匹配时区缩写的正则（字符串末尾的2-4个大写字母）
     */
    private static final Pattern TIMEZONE_PATTERN = Pattern.compile("\\s([A-Z]{2,4})$");
    
    /**
     * 匹配 AM/PM 的正则
     */
    private static final Pattern AMPM_PATTERN = Pattern.compile("\\s(AM|PM)\\s", Pattern.CASE_INSENSITIVE);
    
    static {
        // 美国时区
        TIMEZONE_MAP.put("PDT", ZoneId.of("America/Los_Angeles"));
        TIMEZONE_MAP.put("PST", ZoneId.of("America/Los_Angeles"));
        TIMEZONE_MAP.put("MDT", ZoneId.of("America/Denver"));
        TIMEZONE_MAP.put("MST", ZoneId.of("America/Denver"));
        TIMEZONE_MAP.put("CDT", ZoneId.of("America/Chicago"));
        TIMEZONE_MAP.put("CST", ZoneId.of("America/Chicago"));
        TIMEZONE_MAP.put("EDT", ZoneId.of("America/New_York"));
        TIMEZONE_MAP.put("EST", ZoneId.of("America/New_York"));
        
        // 欧洲时区
        TIMEZONE_MAP.put("GMT", ZoneId.of("Europe/London"));
        TIMEZONE_MAP.put("BST", ZoneId.of("Europe/London"));
        TIMEZONE_MAP.put("CET", ZoneId.of("Europe/Berlin"));
        TIMEZONE_MAP.put("CEST", ZoneId.of("Europe/Berlin"));
        TIMEZONE_MAP.put("UTC", ZoneId.of("UTC"));
        
        // 其他时区
        TIMEZONE_MAP.put("JST", ZoneId.of("Asia/Tokyo"));
        TIMEZONE_MAP.put("AEST", ZoneId.of("Australia/Sydney"));
        TIMEZONE_MAP.put("AEDT", ZoneId.of("Australia/Sydney"));
    }
    
    /**
     * 解析日期字符串，自动识别格式
     * 使用 Hutool DateUtil 进行智能解析
     * 
     * @param dateStr 日期字符串
     * @param siteCode 站点编码（用于辅助判断格式）
     * @return LocalDateTime，如果解析失败返回null
     */
    public static LocalDateTime parse(String dateStr, String siteCode) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        
        String trimmed = dateStr.trim();
        
        try {
            // 优先尝试特定格式（避免 Hutool 智能解析的误判）
            LocalDateTime result = parseSpecificFormats(trimmed, siteCode);
            if (result != null) {
                return result;
            }
            
            // 预处理：移除时区缩写（Hutool 不直接支持 PDT/PST 等缩写）
            String processed = removeTimezoneAbbr(trimmed);
            
            // 预处理：处理 AM/PM 格式转换为24小时制
            processed = normalizeAmPm(processed);
            
            // 使用 Hutool 智能解析
            DateTime dateTime = DateUtil.parse(processed);
            if (dateTime != null) {
                return toLocalDateTime(dateTime);
            }
            
            log.warn("日期解析失败: {} (站点: {})", dateStr, siteCode);
            return null;
            
        } catch (Exception e) {
            log.warn("日期解析异常: {} (站点: {}), 错误: {}", dateStr, siteCode, e.getMessage());
            return null;
        }
    }
    
    /**
     * 移除时区缩写
     */
    private static String removeTimezoneAbbr(String dateStr) {
        Matcher matcher = TIMEZONE_PATTERN.matcher(dateStr);
        if (matcher.find()) {
            String tzAbbr = matcher.group(1);
            if (TIMEZONE_MAP.containsKey(tzAbbr)) {
                return dateStr.substring(0, matcher.start()).trim();
            }
        }
        return dateStr;
    }
    
    /**
     * 标准化 AM/PM 格式
     * 将 "Jul 1, 2025 12:01:49 AM" 转换为 Hutool 可识别的格式
     */
    private static String normalizeAmPm(String dateStr) {
        // 处理美国格式: "Jul 1, 2025 12:01:49 AM"
        // 尝试识别并转换
        if (dateStr.matches(".*\\d{1,2}:\\d{2}:\\d{2}\\s*(AM|PM).*")) {
            try {
                // 使用 Hutool 的自定义格式解析
                // 先尝试标准处理
                String normalized = dateStr.replaceAll("\\s+", " ");
                
                // 提取时间部分并转换
                Pattern timePattern = Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})\\s*(AM|PM)", Pattern.CASE_INSENSITIVE);
                Matcher timeMatcher = timePattern.matcher(normalized);
                
                if (timeMatcher.find()) {
                    int hour = Integer.parseInt(timeMatcher.group(1));
                    String minutes = timeMatcher.group(2);
                    String seconds = timeMatcher.group(3);
                    String ampm = timeMatcher.group(4).toUpperCase();
                    
                    // 转换为24小时制
                    if ("AM".equals(ampm)) {
                        if (hour == 12) hour = 0;
                    } else {
                        if (hour != 12) hour += 12;
                    }
                    
                    String time24 = String.format("%02d:%s:%s", hour, minutes, seconds);
                    return normalized.substring(0, timeMatcher.start()) + time24 + 
                           normalized.substring(timeMatcher.end());
                }
            } catch (Exception e) {
                // 转换失败，返回原字符串
            }
        }
        return dateStr;
    }
    
    /**
     * 尝试特定格式解析
     * 原则：按字面日期/时间解析，不做时区转换（用户要求：不要搞时区，正常按照日期所在时区处理）
     */
    private static LocalDateTime parseSpecificFormats(String dateStr, String siteCode) {
        String processed = removeTimezoneAbbr(dateStr);
        
        // 英国/欧洲 Amazon 格式: 30 Jun 2025 23:01:46 UTC, 1 Sept 2025 03:28:04 UTC（dd MMM yyyy HH:mm:ss）
        // 月份支持 3-4 字母缩写：Jun, Jul, Sept, June, July 等
        // 按字面值解析，不做时区转换
        if (processed.matches("^\\d{1,2}\\s+[A-Za-z]{3,4}\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}.*")) {
            try {
                String[] parts = processed.split("\\s+");
                if (parts.length >= 4) {
                    int day = Integer.parseInt(parts[0]);
                    int month = parseMonth(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    int hour = 0, minute = 0, second = 0;
                    if (parts[3].contains(":")) {
                        String[] timeParts = parts[3].split(":");
                        hour = Integer.parseInt(timeParts[0]);
                        minute = Integer.parseInt(timeParts[1]);
                        second = timeParts.length > 2 ? Integer.parseInt(timeParts[2]) : 0;
                    }
                    return LocalDateTime.of(year, month, day, hour, minute, second);
                }
            } catch (Exception e) {
                log.debug("dd MMM yyyy HH:mm:ss 格式解析失败: {}, 错误: {}", dateStr, e.getMessage());
            }
        }
        
        // 德国格式: 30.06.2025 22:04:35 UTC 或 30.06.2025 22:04:35
        // 匹配 dd.MM.yyyy 开头的格式
        if (processed.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}.*")) {
            try {
                // 手动解析德语日期格式
                // 格式: dd.MM.yyyy HH:mm:ss
                String[] parts = processed.split("\\s+");
                String datePart = parts[0]; // 30.06.2025
                
                String[] dateParts = datePart.split("\\.");
                if (dateParts.length == 3) {
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(dateParts[2]);
                    
                    int hour = 0, minute = 0, second = 0;
                    
                    // 解析时间部分（如果存在）
                    if (parts.length >= 2 && parts[1].contains(":")) {
                        String timePart = parts[1]; // 22:04:35
                        String[] timeParts = timePart.split(":");
                        if (timeParts.length >= 2) {
                            hour = Integer.parseInt(timeParts[0]);
                            minute = Integer.parseInt(timeParts[1]);
                            second = timeParts.length > 2 ? Integer.parseInt(timeParts[2]) : 0;
                        }
                    }
                    
                    return LocalDateTime.of(year, month, day, hour, minute, second);
                }
            } catch (Exception e) {
                log.debug("德国格式解析失败: {}, 错误: {}", dateStr, e.getMessage());
            }
        }
        
        // 英国格式: 1 Jul 2025 00:01:49 BST
        if (processed.matches("^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}.*")) {
            try {
                // 手动解析英国格式
                String[] parts = processed.split("\\s+");
                if (parts.length >= 4) {
                    int day = Integer.parseInt(parts[0]);
                    int month = parseMonth(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    
                    int hour = 0, minute = 0, second = 0;
                    if (parts[3].contains(":")) {
                        String[] timeParts = parts[3].split(":");
                        hour = Integer.parseInt(timeParts[0]);
                        minute = Integer.parseInt(timeParts[1]);
                        second = timeParts.length > 2 ? Integer.parseInt(timeParts[2]) : 0;
                    }
                    
                    return LocalDateTime.of(year, month, day, hour, minute, second);
                }
            } catch (Exception e) {
                log.debug("英国格式解析失败: {}, 错误: {}", dateStr, e.getMessage());
            }
        }
        
        // 美国格式: Jul 1, 2025 12:01:49 AM PDT
        if (processed.matches("^[A-Za-z]{3}\\s+\\d{1,2},?\\s+\\d{4}.*")) {
            try {
                // 移除逗号并解析
                String normalized = processed.replace(",", "");
                String[] parts = normalized.split("\\s+");
                if (parts.length >= 4) {
                    int month = parseMonth(parts[0]);
                    int day = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    
                    int hour = 0, minute = 0, second = 0;
                    boolean isPM = false;
                    
                    if (parts[3].contains(":")) {
                        String[] timeParts = parts[3].split(":");
                        hour = Integer.parseInt(timeParts[0]);
                        minute = Integer.parseInt(timeParts[1]);
                        second = timeParts.length > 2 ? Integer.parseInt(timeParts[2]) : 0;
                    }
                    
                    // 检查 AM/PM
                    if (parts.length >= 5) {
                        String ampm = parts[4].toUpperCase();
                        if ("PM".equals(ampm) && hour != 12) {
                            hour += 12;
                        } else if ("AM".equals(ampm) && hour == 12) {
                            hour = 0;
                        }
                    }
                    
                    return LocalDateTime.of(year, month, day, hour, minute, second);
                }
            } catch (Exception e) {
                log.debug("美国格式解析失败: {}, 错误: {}", dateStr, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 解析月份英文缩写（支持 3-4 字母：Jun, Jul, Sept, June, July 等）
     */
    private static int parseMonth(String monthStr) {
        if (monthStr == null || monthStr.length() < 3) return 1;
        String lower = monthStr.toLowerCase();
        if (lower.startsWith("jan")) return 1;
        if (lower.startsWith("feb")) return 2;
        if (lower.startsWith("mar")) return 3;
        if (lower.startsWith("apr")) return 4;
        if (lower.startsWith("may")) return 5;
        if (lower.startsWith("jun")) return 6;
        if (lower.startsWith("jul")) return 7;
        if (lower.startsWith("aug")) return 8;
        if (lower.startsWith("sep")) return 9;
        if (lower.startsWith("oct")) return 10;
        if (lower.startsWith("nov")) return 11;
        if (lower.startsWith("dec")) return 12;
        return 1;
    }
    
    /**
     * Date 转 LocalDateTime
     */
    private static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * 提取时区信息
     * 
     * @param dateStr 日期字符串
     * @return 时区缩写，如 "PDT"、"UTC" 等
     */
    public static String extractTimezone(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        Matcher matcher = TIMEZONE_PATTERN.matcher(dateStr);
        if (matcher.find()) {
            String tzAbbr = matcher.group(1);
            // 验证是否是已知时区
            if (TIMEZONE_MAP.containsKey(tzAbbr)) {
                return tzAbbr;
            }
        }
        return null;
    }
    
    /**
     * 获取时区对应的 ZoneId
     * 
     * @param tzAbbr 时区缩写
     * @return ZoneId，如果未知返回系统默认时区
     */
    public static ZoneId getZoneId(String tzAbbr) {
        if (StrUtil.isBlank(tzAbbr)) {
            return ZoneId.systemDefault();
        }
        return TIMEZONE_MAP.getOrDefault(tzAbbr, ZoneId.systemDefault());
    }
}
