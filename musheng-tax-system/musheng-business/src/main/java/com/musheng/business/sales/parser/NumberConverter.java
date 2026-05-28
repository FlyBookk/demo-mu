package com.musheng.business.sales.parser;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * 数字转换器
 * 处理不同站点的数字格式差异（小数点/千位分隔符）
 * 使用 Hutool 进行数字解析
 * 
 * 格式说明：
 * - 英文格式（US/UK/CA）: 1,234.56 （逗号为千位分隔符，点为小数点）
 * - 欧洲格式（DE/FR/IT/ES）: 1.234,56 （点为千位分隔符，逗号为小数点）
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Slf4j
public class NumberConverter {
    
    /**
     * 使用逗号作为小数点的欧洲站点集合
     */
    private static final Set<String> COMMA_DECIMAL_SITES = Set.of("DE", "FR", "IT", "ES");
    
    /**
     * 解析数字字符串为BigDecimal
     * 
     * @param numberStr 数字字符串
     * @param siteCode 站点编码（用于确定格式）
     * @return BigDecimal，如果解析失败返回 BigDecimal.ZERO
     */
    public static BigDecimal parse(String numberStr, String siteCode) {
        if (StrUtil.isBlank(numberStr)) {
            return BigDecimal.ZERO;
        }
        
        String trimmed = numberStr.trim();
        
        // 移除可能的货币符号
        trimmed = trimmed.replaceAll("[\\$€£¥]", "").trim();
        
        // 空值处理
        if (StrUtil.isBlank(trimmed) || "-".equals(trimmed)) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 欧洲站点（DE/FR/IT/ES）使用逗号作为小数点
            if (siteCode != null && COMMA_DECIMAL_SITES.contains(siteCode.toUpperCase())) {
                return parseGerman(trimmed);
            } else {
                return parseEnglish(trimmed);
            }
        } catch (Exception e) {
            log.warn("数字解析失败: {} (站点: {}), 错误: {}", numberStr, siteCode, e.getMessage());
            // 返回零而非 null，避免 NPE
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 解析英文格式数字
     * 格式: 1,234.56 或 1234.56
     */
    private static BigDecimal parseEnglish(String numberStr) {
        // 移除千位分隔符（逗号）
        String normalized = numberStr.replace(",", "");
        
        // 处理负数括号表示法 (1234.56) -> -1234.56
        normalized = handleBracketNegative(normalized);
        
        return new BigDecimal(normalized);
    }
    
    /**
     * 解析欧洲格式数字（DE/FR/IT/ES）
     * 格式: 1.234,56 或 1234,56
     */
    private static BigDecimal parseGerman(String numberStr) {
        // 德文格式：点是千位分隔符，逗号是小数点
        // 1. 先移除千位分隔符（点）
        // 2. 将小数点（逗号）替换为点
        String normalized = numberStr.replace(".", "").replace(",", ".");
        
        // 处理负数括号表示法
        normalized = handleBracketNegative(normalized);
        
        return new BigDecimal(normalized);
    }
    
    /**
     * 处理括号表示的负数
     * (1234.56) -> -1234.56
     */
    private static String handleBracketNegative(String str) {
        if (str.startsWith("(") && str.endsWith(")")) {
            return "-" + str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    /**
     * 自动检测格式并解析
     * 用于无法确定站点的情况
     */
    public static BigDecimal parseAuto(String numberStr) {
        if (StrUtil.isBlank(numberStr)) {
            return BigDecimal.ZERO;
        }
        
        String trimmed = numberStr.trim().replaceAll("[\\$€£¥]", "").trim();
        
        if (StrUtil.isBlank(trimmed) || "-".equals(trimmed)) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 检测格式
            // 德文格式特征：逗号在最后3-4位（作为小数点）
            // 如: 1.234,56 或 1234,56
            if (trimmed.matches(".*,\\d{1,2}$") && !trimmed.matches(".*\\.\\d{1,2}$")) {
                // 可能是德文格式
                return parseGerman(trimmed);
            }
            
            // 默认尝试英文格式
            return parseEnglish(trimmed);
        } catch (Exception e) {
            log.warn("自动解析数字失败: {}, 错误: {}", numberStr, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 安全解析，使用 Hutool
     * 自动处理各种格式
     */
    public static BigDecimal parseSafe(String numberStr) {
        if (StrUtil.isBlank(numberStr)) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 使用 Hutool 的智能解析
            return NumberUtil.toBigDecimal(numberStr);
        } catch (Exception e) {
            // 尝试自动检测
            return parseAuto(numberStr);
        }
    }
    
    /**
     * 格式化BigDecimal为字符串
     * 
     * @param value 数值
     * @param decimalPlaces 小数位数
     * @return 格式化后的字符串
     */
    public static String format(BigDecimal value, int decimalPlaces) {
        if (value == null) {
            return "0";
        }
        return value.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString();
    }
}
