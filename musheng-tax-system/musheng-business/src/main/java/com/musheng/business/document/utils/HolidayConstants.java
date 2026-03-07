package com.musheng.business.document.utils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 节假日常量类
 *
 * <p>硬编码2025年和2026年的主要节假日，用于工作日判定。
 * 仅包含主要节日，不做调休处理。每年手动更新一次即可。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class HolidayConstants {

    private HolidayConstants() {
        // 工具类，禁止实例化
    }

    /** 2025年主要节假日 */
    public static final List<LocalDate> HOLIDAYS_2025 = List.of(
            // 元旦
            LocalDate.of(2025, 1, 1),
            // 春节（1月28日-2月4日）
            LocalDate.of(2025, 1, 28),
            LocalDate.of(2025, 1, 29),
            LocalDate.of(2025, 1, 30),
            LocalDate.of(2025, 1, 31),
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 2, 2),
            LocalDate.of(2025, 2, 3),
            LocalDate.of(2025, 2, 4),
            // 清明节（4月4日-4月6日）
            LocalDate.of(2025, 4, 4),
            LocalDate.of(2025, 4, 5),
            LocalDate.of(2025, 4, 6),
            // 劳动节（5月1日-5月5日）
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 2),
            LocalDate.of(2025, 5, 3),
            LocalDate.of(2025, 5, 4),
            LocalDate.of(2025, 5, 5),
            // 端午节（5月31日-6月2日）
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2025, 6, 1),
            LocalDate.of(2025, 6, 2),
            // 国庆节+中秋节（10月1日-10月7日，中秋10月6日与国庆连休）
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 2),
            LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 4),
            LocalDate.of(2025, 10, 5),
            LocalDate.of(2025, 10, 6),
            LocalDate.of(2025, 10, 7),
            // 圣诞节（12月25日-12月26日）
            LocalDate.of(2025, 12, 25),
            LocalDate.of(2025, 12, 26)
    );

    /** 2026年主要节假日 */
    public static final List<LocalDate> HOLIDAYS_2026 = List.of(
            // 元旦（1月1日-1月2日）
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            // 春节（2月16日-2月22日）
            LocalDate.of(2026, 2, 16),
            LocalDate.of(2026, 2, 17),
            LocalDate.of(2026, 2, 18),
            LocalDate.of(2026, 2, 19),
            LocalDate.of(2026, 2, 20),
            LocalDate.of(2026, 2, 21),
            LocalDate.of(2026, 2, 22),
            // 清明节（4月5日-4月6日）
            LocalDate.of(2026, 4, 5),
            LocalDate.of(2026, 4, 6),
            // 劳动节（5月1日-5月3日）
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 2),
            LocalDate.of(2026, 5, 3),
            // 端午节（6月19日-6月21日）
            LocalDate.of(2026, 6, 19),
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            // 中秋节（9月25日）
            LocalDate.of(2026, 9, 25),
            // 国庆节（10月1日-10月7日）
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 3),
            LocalDate.of(2026, 10, 4),
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 6),
            LocalDate.of(2026, 10, 7),
            // 圣诞节（12月25日-12月26日）
            LocalDate.of(2026, 12, 25),
            LocalDate.of(2026, 12, 26)
    );

    /** 所有节假日集合（用于快速查找） */
    private static final Set<LocalDate> ALL_HOLIDAYS;

    static {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.addAll(HOLIDAYS_2025);
        holidays.addAll(HOLIDAYS_2026);
        ALL_HOLIDAYS = Collections.unmodifiableSet(holidays);
    }

    /**
     * 判断给定日期是否为节假日
     *
     * @param date 待判断的日期
     * @return 是节假日返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static boolean isHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }
        return ALL_HOLIDAYS.contains(date);
    }
}
