package com.musheng.business.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额转换工具类
 * 提供货币转换相关的通用方法
 *
 * @author wanhua
 * 18:50 2026年02月01日
 */
@Slf4j
public final class MoneyConvertUtils {

    /**
     * 默认小数位数
     */
    private static final int DEFAULT_SCALE = 2;

    /**
     * 默认舍入模式
     */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyConvertUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将金额按汇率转换为人民币
     * 
     * @param amount 原始金额
     * @param exchangeRate 汇率
     * @return 转换后的人民币金额，如果输入无效则返回 BigDecimal.ZERO
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static BigDecimal convertToCny(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有汇率，返回0（避免错误累加）
            log.warn("缺少汇率信息，金额转换返回0: amount={}", amount);
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }

    /**
     * 将金额按汇率转换为人民币（带精度控制）
     * 
     * @param amount 原始金额
     * @param exchangeRate 汇率
     * @param scale 小数位数
     * @return 转换后的人民币金额
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static BigDecimal convertToCny(BigDecimal amount, BigDecimal exchangeRate, int scale) {
        BigDecimal result = convertToCny(amount, exchangeRate);
        if (result.compareTo(BigDecimal.ZERO) == 0) {
            return result;
        }
        return result.setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 将金额按汇率转换为人民币（使用默认2位小数）
     * 
     * @param amount 原始金额
     * @param exchangeRate 汇率
     * @return 转换后的人民币金额（保留2位小数）
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static BigDecimal convertToCnyWithScale(BigDecimal amount, BigDecimal exchangeRate) {
        return convertToCny(amount, exchangeRate, DEFAULT_SCALE);
    }

    /**
     * 安全地进行金额加法运算
     * 
     * @param a 第一个金额
     * @param b 第二个金额
     * @return 两个金额之和，null 值视为 0
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        BigDecimal first = a != null ? a : BigDecimal.ZERO;
        BigDecimal second = b != null ? b : BigDecimal.ZERO;
        return first.add(second);
    }

    /**
     * 安全地进行金额减法运算
     * 
     * @param a 被减数
     * @param b 减数
     * @return 差值，null 值视为 0
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        BigDecimal first = a != null ? a : BigDecimal.ZERO;
        BigDecimal second = b != null ? b : BigDecimal.ZERO;
        return first.subtract(second);
    }

    /**
     * 检查金额是否为正数
     * 
     * @param amount 金额
     * @return 如果金额大于0返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查金额是否为负数
     * 
     * @param amount 金额
     * @return 如果金额小于0返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 检查金额是否为零
     * 
     * @param amount 金额
     * @return 如果金额等于0或为null返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    public static boolean isZeroOrNull(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
