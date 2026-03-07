package com.musheng.business.document.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 单据编号生成器
 *
 * <p>纯函数工具类，无状态。根据日期和序号生成确定性的单据编号。
 * 编号格式：{YYYYMMDD}{3位序号}，如 20250902001。</p>
 *
 * <p>确定性保障：序号由排序位置决定，不依赖数据库自增或随机数。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class DocumentNumberCalculator {

    /** 日期格式化器：yyyyMMdd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 序号最小值 */
    private static final int MIN_SEQUENCE = 1;

    /** 序号最大值 */
    private static final int MAX_SEQUENCE = 999;

    private DocumentNumberCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成单据编号
     *
     * <p>编号格式：{YYYYMMDD}{3位序号}，日期格式化为8位数字，序号格式化为3位补零。
     * 例如：2025-09-02 序号1 → "20250902001"。</p>
     *
     * @param date 单据日期，不能为 null
     * @param sequence 序号，必须在 1~999 之间
     * @return 11位单据编号字符串
     * @throws IllegalArgumentException 如果 date 为 null 或 sequence 不在有效范围内
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static String generate(LocalDate date, int sequence) {
        if (date == null) {
            throw new IllegalArgumentException("日期参数不能为 null");
        }
        if (sequence < MIN_SEQUENCE || sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "序号必须在 " + MIN_SEQUENCE + " 到 " + MAX_SEQUENCE + " 之间，当前值：" + sequence);
        }
        String datePart = date.format(DATE_FORMATTER);
        String seqPart = String.format("%03d", sequence);
        return datePart + seqPart;
    }
}
