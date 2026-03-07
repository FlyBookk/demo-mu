package com.musheng.business.document.utils;

import net.jqwik.api.*;
import net.jqwik.time.api.Dates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentNumberCalculator 属性测试
 *
 * <p>使用 jqwik 框架验证单据编号生成器的通用正确性属性。
 * 每个属性测试运行100次迭代，覆盖2020-2030年日期范围和1-999序号范围。</p>
 *
 * <p><b>Validates: Requirements 2.5, 2.6, 3.5, 4.4, 5.3</b></p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
class DocumentNumberCalculatorProperties {

    /** 编号正则：8位日期 + 3位序号，共11位纯数字 */
    private static final String DOCUMENT_NUMBER_REGEX = "^\\d{8}\\d{3}$";

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 提供2020-2030年范围内的随机日期
     *
     * @return 日期 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<LocalDate> validDates() {
        return Dates.dates()
                .between(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31));
    }

    /**
     * 提供1到999范围内的随机序号
     *
     * @return 序号 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<Integer> validSequences() {
        return Arbitraries.integers().between(1, 999);
    }

    /**
     * 提供两个不同的序号对（用于唯一性测试）
     *
     * @return 两个不同序号的元组 Arbitrary
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Provide
    Arbitrary<int[]> twoDistinctSequences() {
        return Arbitraries.integers().between(1, 999)
                .tuple2()
                .filter(t -> !t.get1().equals(t.get2()))
                .map(t -> new int[]{t.get1(), t.get2()});
    }

    // ==================== 属性 4：单据编号格式与唯一性 ====================

    // Feature: fba-document-generation, Property 4: 单据编号格式与唯一性
    // 编号匹配正则 ^\d{8}\d{3}$（8位日期+3位序号，共11位纯数字）
    /**
     * 属性4-1：编号匹配正则 ^\d{8}\d{3}$
     *
     * <p>生成的编号应为11位纯数字，前8位为日期，后3位为序号。</p>
     *
     * <p><b>Validates: Requirements 2.5, 2.6, 3.5, 4.4, 5.3</b></p>
     *
     * @param date 随机生成的日期
     * @param sequence 随机生成的序号
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void documentNumberShouldMatchRegex(
            @ForAll("validDates") LocalDate date,
            @ForAll("validSequences") int sequence) {
        String number = DocumentNumberCalculator.generate(date, sequence);

        assertTrue(number.matches(DOCUMENT_NUMBER_REGEX),
                "编号 '" + number + "' 不匹配正则 " + DOCUMENT_NUMBER_REGEX);
    }

    // Feature: fba-document-generation, Property 4: 单据编号格式与唯一性
    // 同一日期不同序号生成的编号互不相同
    /**
     * 属性4-2：同一日期不同序号生成的编号互不相同
     *
     * <p>对于同一日期，不同序号生成的编号应保证唯一性。</p>
     *
     * <p><b>Validates: Requirements 2.6, 4.4, 5.3</b></p>
     *
     * @param date 随机生成的日期
     * @param seqPair 两个不同的序号
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void sameDateDifferentSequenceShouldProduceDifferentNumbers(
            @ForAll("validDates") LocalDate date,
            @ForAll("twoDistinctSequences") int[] seqPair) {
        String number1 = DocumentNumberCalculator.generate(date, seqPair[0]);
        String number2 = DocumentNumberCalculator.generate(date, seqPair[1]);

        assertNotEquals(number1, number2,
                "同一日期 " + date + " 序号 " + seqPair[0] + " 和 " + seqPair[1]
                        + " 生成了相同的编号：" + number1);
    }

    // Feature: fba-document-generation, Property 4: 单据编号格式与唯一性
    // 编号前8位为有效日期格式（YYYYMMDD）
    /**
     * 属性4-3：编号前8位为有效日期格式（YYYYMMDD）
     *
     * <p>编号的前8位应能解析为与输入日期一致的有效日期。</p>
     *
     * <p><b>Validates: Requirements 2.5, 3.5, 4.4, 5.3</b></p>
     *
     * @param date 随机生成的日期
     * @param sequence 随机生成的序号
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void first8DigitsShouldBeValidDate(
            @ForAll("validDates") LocalDate date,
            @ForAll("validSequences") int sequence) {
        String number = DocumentNumberCalculator.generate(date, sequence);
        String datePart = number.substring(0, 8);

        // 前8位应能解析为有效日期
        LocalDate parsedDate = LocalDate.parse(datePart, DATE_FORMATTER);

        // 解析出的日期应与输入日期一致
        assertEquals(date, parsedDate,
                "编号 '" + number + "' 前8位 '" + datePart + "' 解析日期为 "
                        + parsedDate + "，与输入日期 " + date + " 不一致");
    }

    // Feature: fba-document-generation, Property 4: 单据编号格式与唯一性
    // 编号后3位为序号的3位补零表示
    /**
     * 属性4-4：编号后3位为序号的3位补零表示
     *
     * <p>编号的后3位应为输入序号的3位补零格式。</p>
     *
     * <p><b>Validates: Requirements 2.5, 3.5, 4.4, 5.3</b></p>
     *
     * @param date 随机生成的日期
     * @param sequence 随机生成的序号
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Property(tries = 100)
    void last3DigitsShouldBeZeroPaddedSequence(
            @ForAll("validDates") LocalDate date,
            @ForAll("validSequences") int sequence) {
        String number = DocumentNumberCalculator.generate(date, sequence);
        String seqPart = number.substring(8);

        // 后3位应为序号的3位补零表示
        String expectedSeq = String.format("%03d", sequence);
        assertEquals(expectedSeq, seqPart,
                "编号 '" + number + "' 后3位 '" + seqPart + "' 应为序号 "
                        + sequence + " 的补零表示 '" + expectedSeq + "'");
    }
}
