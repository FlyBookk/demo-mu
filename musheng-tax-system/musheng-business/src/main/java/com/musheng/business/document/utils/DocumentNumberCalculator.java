package com.musheng.business.document.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 单据编号生成器
 *
 * <p>纯函数工具类，无状态。根据日期、时间戳和序号生成唯一单据编号。
 * 编号格式：{YYYYMMDD}{HHmmssSSS}{3位序号}，如 202509021035421230001。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class DocumentNumberCalculator {

    /** 日期格式化器：yyyyMMdd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 时间戳格式化器：HHmmssSSS */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");

    /** 序号最小值 */
    private static final int MIN_SEQUENCE = 1;

    /** 序号最大值 */
    private static final int MAX_SEQUENCE = 999;

    private DocumentNumberCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成单据编号（带时间戳，确保全局唯一）
     *
     * <p>编号格式：{YYYYMMDD}{HHmmssSSS}{3位序号}，每次生成时间戳不同，不会冲突。
     * 例如：2025-09-02 10:35:42.123 序号1 → "202509021035421230001"。</p>
     *
     * @param date 单据日期，不能为 null
     * @param sequence 序号，必须在 1~999 之间
     * @return 单据编号字符串
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
        String timePart = LocalDateTime.now().format(TIME_FORMATTER);
        String seqPart = String.format("%03d", sequence);
        return datePart + timePart + seqPart;
    }
}
