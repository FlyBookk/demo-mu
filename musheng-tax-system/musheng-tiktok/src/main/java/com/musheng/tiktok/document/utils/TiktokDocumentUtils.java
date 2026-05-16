package com.musheng.tiktok.document.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TK单据工具类（工作日计算、DN周期、编号生成）
 *
 * @author wanhua
 * 19:54 2026年05月15日
 */
public final class TiktokDocumentUtils {

    private static final int DN_PERIOD_DAYS = 21;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmssSSS");

    /** 香港公众假期（简化版，覆盖2025-2027） */
    private static final Set<LocalDate> HOLIDAYS = Set.of(
            // 2025
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 29), LocalDate.of(2025, 1, 30),
            LocalDate.of(2025, 1, 31), LocalDate.of(2025, 4, 4), LocalDate.of(2025, 4, 18),
            LocalDate.of(2025, 4, 19), LocalDate.of(2025, 4, 21), LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 5), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 7), LocalDate.of(2025, 10, 29), LocalDate.of(2025, 12, 25),
            LocalDate.of(2025, 12, 26),
            // 2026
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
            LocalDate.of(2026, 2, 19), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 4),
            LocalDate.of(2026, 4, 6), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 24),
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 19),
            LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26),
            // 2027
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 6), LocalDate.of(2027, 2, 7),
            LocalDate.of(2027, 2, 8), LocalDate.of(2027, 3, 26), LocalDate.of(2027, 3, 27),
            LocalDate.of(2027, 3, 29), LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 13),
            LocalDate.of(2027, 7, 1), LocalDate.of(2027, 10, 1), LocalDate.of(2027, 12, 25),
            LocalDate.of(2027, 12, 27)
    );

    private TiktokDocumentUtils() {}

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return !HOLIDAYS.contains(date);
    }

    public static LocalDate nearestWorkingDay(LocalDate date) {
        while (!isWorkingDay(date)) date = date.plusDays(1);
        return date;
    }

    /**
     * 计算DN日期序列：anchor + N×21天，非工作日顺延
     */
    public static List<LocalDate> calculateDnDates(LocalDate anchor, LocalDate rangeEnd) {
        List<LocalDate> dates = new ArrayList<>();
        int n = 0;
        while (true) {
            LocalDate raw = anchor.plusDays((long) n * DN_PERIOD_DAYS);
            if (raw.isAfter(rangeEnd)) break;
            dates.add(nearestWorkingDay(raw));
            n++;
        }
        return dates;
    }

    /**
     * 生成单据编号：{YYYYMMDD}{HHmmssSSS}{3位序号}
     */
    public static String generateDocNo(LocalDate date, int sequence) {
        if (sequence < 1 || sequence > 999) throw new IllegalArgumentException("序号必须在1~999之间");
        return date.format(DATE_FMT) + LocalDateTime.now().format(TIME_FMT) + String.format("%03d", sequence);
    }
}
