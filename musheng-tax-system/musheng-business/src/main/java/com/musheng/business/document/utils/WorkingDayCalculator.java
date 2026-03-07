package com.musheng.business.document.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 工作日计算器
 *
 * <p>纯函数工具类，无状态。判断工作日时排除周末和节假日（通过 HolidayConstants）。
 * 所有方法为 static，确保确定性输出。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class WorkingDayCalculator {

    /** 最大迭代天数，防止死循环 */
    private static final int MAX_ITERATION_DAYS = 365;

    private WorkingDayCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 判断给定日期是否为工作日
     *
     * <p>排除周末（周六、周日）和节假日（HolidayConstants 中定义的日期）。</p>
     *
     * @param date 待判断的日期，不能为 null
     * @return 是工作日返回 true，否则返回 false
     * @throws IllegalArgumentException 如果 date 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static boolean isWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("日期参数不能为 null");
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !HolidayConstants.isHoliday(date);
    }

    /**
     * 返回给定日期之后的下一个工作日
     *
     * <p>从给定日期的下一天开始查找，直到找到工作日为止。
     * 设置最大迭代365天防止死循环。</p>
     *
     * @param date 起始日期，不能为 null
     * @return 给定日期之后的下一个工作日
     * @throws IllegalArgumentException 如果 date 为 null
     * @throws IllegalStateException 如果365天内未找到工作日
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static LocalDate nextWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("日期参数不能为 null");
        }
        LocalDate candidate = date.plusDays(1);
        int iteration = 0;
        while (!isWorkingDay(candidate)) {
            candidate = candidate.plusDays(1);
            iteration++;
            if (iteration >= MAX_ITERATION_DAYS) {
                throw new IllegalStateException(
                        "在365天内未找到工作日，请检查节假日配置。起始日期：" + date);
            }
        }
        return candidate;
    }

    /**
     * 返回给定日期当天或之后的最近工作日
     *
     * <p>如果给定日期本身是工作日，则返回当天；否则向后查找下一个工作日。
     * 设置最大迭代365天防止死循环。</p>
     *
     * @param date 起始日期，不能为 null
     * @return 当天或之后的最近工作日
     * @throws IllegalArgumentException 如果 date 为 null
     * @throws IllegalStateException 如果365天内未找到工作日
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static LocalDate nearestWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("日期参数不能为 null");
        }
        if (isWorkingDay(date)) {
            return date;
        }
        return nextWorkingDay(date);
    }
}
