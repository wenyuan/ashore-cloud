package com.example.ashore.framework.common.util.date;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.common.enums.DateIntervalEnum;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static cn.hutool.core.date.DatePattern.*;

/**
 * LocalDateTime 时间工具类
 * {@link LocalDateTime} 是 Java 8 引入的新时间 API，相比传统的 Date 类，它是不可变且线程安全的，推荐在新项目中使用。
 *
 * 区别：
 * {@link DateUtils}：主要处理传统的 {@link java.util.Date} 类型，用于兼容老代码
 * {@link LocalDateTimeUtils}：专门处理 Java 8+ 的 {@link LocalDateTime} 类型，提供更丰富的功能
 *
 * 核心依赖库：
 * - Hutool (cn.hutool.core.date)：提供便捷的日期时间操作方法
 * - Java Time API (java.time.*)：Java 8 标准时间库
 */
public class LocalDateTimeUtils {

    /**
     * 空的 LocalDateTime 对象，值为 1970-01-01 00:00:00 (Unix 纪元时间的起点)
     * 使用场景：当数据库字段不允许 NULL，但又需要表示"未设置"的状态时
     */
    public static LocalDateTime EMPTY = buildTime(1970, 1, 1);

    /**
     * UTC 时间格式化器，支持毫秒和时区偏移量
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSXXX
     * 示例：2024-01-15T14:30:45.123+08:00
     * 用途：用于解析和格式化带时区信息的 UTC 时间字符串，常见于 API 接口的时间传输
     */
    public static DateTimeFormatter UTC_MS_WITH_XXX_OFFSET_FORMATTER = createFormatter(UTC_MS_WITH_XXX_OFFSET_PATTERN);

    /**
     * 智能解析时间字符串为 LocalDateTime 对象
     *
     * 此方法相比 Hutool 的 {@link LocalDateTimeUtil#parse(CharSequence)} 更加智能，会尝试多种格式进行解析，直到成功为止，提高了时间解析的成功率。
     * - 首先尝试按标准日期格式 "yyyy-MM-dd" 解析
     * - 如果失败，则使用 Hutool 的自动识别功能解析(支持多种常见格式)
     *
     * @param time                    时间字符串，例如："2024-01-15"、"2024-01-15 14:30:45"、"2024/01/15" 等
     * @return                        LocalDateTime 解析后的时间对象
     * @throws DateTimeParseException 如果所有解析尝试都失败，则抛出此异常
     */
    public static LocalDateTime parse(String time) {
        try {
            // 首先尝试按标准日期格式 "yyyy-MM-dd" 解析
            return LocalDateTimeUtil.parse(time, DatePattern.NORM_DATE_PATTERN);
        } catch (DateTimeParseException e) {
            // 如果失败，使用 Hutool 自动识别格式进行解析
            return LocalDateTimeUtil.parse(time);
        }
    }

    /**
     * 在当前时间基础上增加指定的时间间隔
     *
     * @param duration 时间间隔，例如：Duration.ofHours(2) 表示 2 小时
     * @return         增加时间间隔后的 LocalDateTime 对象
     */
    public static LocalDateTime addTime(Duration duration) {
        return LocalDateTime.now().plus(duration);
    }

    /**
     * 在当前时间基础上减少指定的时间间隔
     *
     * @param duration 时间间隔
     * @return         减少时间间隔后的 LocalDateTime 对象
     */
    public static LocalDateTime minusTime(Duration duration) {
        return LocalDateTime.now().minus(duration);
    }

    /**
     * 判断指定时间是否在当前时间之前
     * @param date     待判断的时间，例如：LocalDateTime.of(2024, 1, 10, 10, 0, 0)
     * @return boolean true-指定时间在当前时间之前，false-在当前时间之后或相等
     */
    public static boolean beforeNow(LocalDateTime date) {
        return date.isBefore(LocalDateTime.now());
    }

    /**
     * 判断指定时间是否在当前时间之后
     * @param date     待判断的时间，例如：LocalDateTime.of(2024, 1, 20, 10, 0, 0)
     * @return boolean true-指定时间在当前时间之后，false-在当前时间之前或相等。
     */
    public static boolean afterNow(LocalDateTime date) {
        return date.isAfter(LocalDateTime.now());
    }

    /**
     * 创建指定日期的时间(时分秒默认为 00:00:00)
     *
     * @param year  年份
     * @param month 月份，取值范围 1-12
     * @param day   日期，取值范围 1-31
     * @return      创建的 LocalDateTime 对象
     */
    public static LocalDateTime buildTime(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0, 0);
    }

    /**
     * 创建一个时间范围数组，包含开始时间和结束时间
     * @param year1            开始年份
     * @param month1           开始月份，取值范围 1-12
     * @param day1             开始日期，取值范围 1-31
     * @param year2            结束年份
     * @param month2           结束月份，取值范围 1-12
     * @param day2             结束日期，取值范围 1-31
     * @return LocalDateTime[] 包含两个元素的数组，[0] 是开始时间， [1] 是结束时间
     *                         例如：buildBetweenTime(2024, 1, 1, 2024, 1, 31) 返回 [2024-01-01T00:00:00, 2024-01-31T00:00:00]
     */
    public static LocalDateTime[] buildBetweenTime(int year1, int month1, int day1,
                                                   int year2, int month2, int day2) {
        // 创建包含开始和结束时间的数组,常用于时间范围查询
        return new LocalDateTime[]{buildTime(year1, month1, day1), buildTime(year2, month2, day2)};
    }

    /**
     * 判断指定时间(Timestamp 类型)是否在给定的时间范围内
     *
     * @param startTime 开始时间，例如：LocalDateTime.of(2024, 1, 1, 0, 0, 0)
     * @param endTime   结束时间，例如：LocalDateTime.of(2024, 1, 31, 23, 59, 59)
     * @param time      待判断的时间(Timestamp 类型)，例如: new Timestamp(1705299045000L)
     * @return boolean  true-在范围内(包含边界)， false-不在范围内或参数为 null
     *                  例如：范围 2024-01-01 到 2024-01-31，传入 2024-01-15 返回 true
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime, Timestamp time) {
        // 防御性编程：如果任何参数为 null，直接返回 false
        if (startTime == null || endTime == null || time == null) {
            return false;
        }
        // LocalDateTimeUtil.of(time) 将 Timestamp 转换为 LocalDateTime
        // LocalDateTimeUtil.isIn() 判断时间是否在范围内
        return LocalDateTimeUtil.isIn(LocalDateTimeUtil.of(time), startTime, endTime);
    }

    /**
     * 判断指定时间(字符串类型)是否在给定的时间范围内
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param time      待判断的时间字符串，例如："2024-01-15" 或 "2024-01-15 14:30:45"
     * @return          是否
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime, String time) {
        // 防御性编程: 如果任何参数为 null, 直接返回 false
        if (startTime == null || endTime == null || time == null) {
            return false;
        }
        // parse(time) 将字符串解析为 LocalDateTime
        // LocalDateTimeUtil.isIn() 判断时间是否在范围内
        return LocalDateTimeUtil.isIn(parse(time), startTime, endTime);
    }

    /**
     * 判断当前时间是否在给定的时间范围内
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return          是否
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime) {
        // 防御性编程: 如果任何参数为 null, 直接返回 false
        if (startTime == null || endTime == null) {
            return false;
        }
        // LocalDateTime.now() 获取当前时间
        // LocalDateTimeUtil.isIn() 判断当前时间是否在范围内
        return LocalDateTimeUtil.isIn(LocalDateTime.now(), startTime, endTime);
    }

    /**
     * 判断当前时间是否在今天的某个时间段内(仅比较时分秒，不考虑日期)
     * 此方法用于判断当前时间是否在今天的某个时间范围内，常用于营业时间判断、活动时间判断等场景。
     *
     * @param startTime 开始时间(时分秒)，例如："09:00:00"
     * @param endTime   结束时间(时分秒)，例如："18:00:00"
     * @return          是否
     *
     * 示例：
     * // 判断当前是否在营业时间 09:00-18:00 内
     * boolean isOpen = isBetween("09:00:00", "18:00:00");
     */
    public static boolean isBetween(String startTime, String endTime) {
        // 防御性编程: 如果任何参数为 null, 直接返回 false
        if (startTime == null || endTime == null) {
            return false;
        }
        // LocalDate.now() 获取今天的日期
        LocalDate nowDate = LocalDate.now();
        // LocalTime.parse() 解析时间字符串(时分秒)
        // LocalDateTime.of(nowDate, localTime) 将今天的日期和指定的时间组合成 LocalDateTime
        return LocalDateTimeUtil.isIn(LocalDateTime.now(),
                LocalDateTime.of(nowDate, LocalTime.parse(startTime)),
                LocalDateTime.of(nowDate, LocalTime.parse(endTime)));
    }

    /**
     * 判断两个时间段是否重叠(仅比较时分秒，不考虑日期)
     * 此方法用于检测两个时间段是否有重叠部分，常用于排班冲突检测、会议室预定冲突检测等场景。
     *
     * @param startTime1 第一个时间段的开始时间，例如：LocalTime.of(9, 0, 0) 表示 09:00:00
     * @param endTime1   第一个时间段的结束时间，例如：LocalTime.of(12, 0, 0) 表示 12:00:00
     * @param startTime2 第二个时间段的开始时间，例如：LocalTime.of(11, 0, 0) 表示 11:00:00
     * @param endTime2   第二个时间段的结束时间，例如：LocalTime.of(14, 0, 0) 表示 14:00:00
     * @return boolean   true-有重叠，false-没有重叠
     *                   例如：时间段1是 09:00-12:00，时间段2是 11:00-14:00，返回 true (重叠1小时)
     *
     * 示例：
     * // 检测会议室预定是否冲突
     * LocalTime meeting1Start = LocalTime.of(9, 0);   // 09:00
     * LocalTime meeting1End = LocalTime.of(11, 0);    // 11:00
     * LocalTime meeting2Start = LocalTime.of(10, 30); // 10:30
     * LocalTime meeting2End = LocalTime.of(12, 0);    // 12:00
     * boolean hasConflict = isOverlap(meeting1Start, meeting1End, meeting2Start, meeting2End); // 返回 true
     */
    public static boolean isOverlap(LocalTime startTime1, LocalTime endTime1, LocalTime startTime2, LocalTime endTime2) {
        // LocalDate.now() 获取今天的日期(用于将 LocalTime 转换为 LocalDateTime)
        LocalDate nowDate = LocalDate.now();
        // LocalDateTime.of(nowDate, localTime) 将日期和时间组合
        // LocalDateTimeUtil.isOverlap() 判断两个时间段是否重叠
        return LocalDateTimeUtil.isOverlap(LocalDateTime.of(nowDate, startTime1), LocalDateTime.of(nowDate, endTime1),
                LocalDateTime.of(nowDate, startTime2), LocalDateTime.of(nowDate, endTime2));
    }

    /**
     * 获取指定日期所在月份的开始时间(即该月的第一天 00:00:00.000)
     *
     * @param date           待查询的日期，例如：LocalDateTime.of(2024, 1, 15, 14, 30, 45)
     * @return LocalDateTime 该月的开始时间。例如：传入 2024-01-15 14:30:45，返回 2024-01-01 00:00:00.000
     */
    public static LocalDateTime beginOfMonth(LocalDateTime date) {
        // TemporalAdjusters.firstDayOfMonth() 调整到该月的第一天
        // LocalTime.MIN 表示 00:00:00.000,即一天的开始时间
        return date.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
    }

    /**
     * 获取指定日期所在月份的最后时间(即该月的最后一天 23:59:59.999999999)
     *
     * @param date           待查询的日期，例如：LocalDateTime.of(2024, 1, 15, 14, 30, 45)
     * @return LocalDateTime 该月的结束时间。例如：传入 2024-01-15 14:30:45，返回 2024-01-31 23:59:59.999999999
     */
    public static LocalDateTime endOfMonth(LocalDateTime date) {
        // TemporalAdjusters.lastDayOfMonth() 调整到该月的最后一天
        // LocalTime.MAX 表示 23:59:59.999999999,即一天的最后时间
        return date.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在的季度(1-4)
     *
     * @param date 待查询的日期，例如：LocalDateTime.of(2024, 5, 15, 0, 0, 0)
     * @return int 所在季度，取值范围 1-4。例如：5月份返回 2 (第2季度)
     */
    public static int getQuarterOfYear(LocalDateTime date) {
        // getMonthValue() 获取月份值 (1-12)
        // 算法: (月份-1) / 3 + 1
        // 例如: 5月 → (5-1)/3+1 = 4/3+1 = 1+1 = 2 (第2季度)
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * 计算指定日期距离当前时间的天数差
     * 如果指定日期在当前日期之前，返回正数；如果在当前日期之后，返回负数。
     *
     * @param dateTime 指定日期
     * @return Long    相差天数
     */
    public static Long between(LocalDateTime dateTime) {
        // LocalDateTimeUtil.between() 计算两个时间之间的差值
        // ChronoUnit.DAYS 指定以"天"为单位计算
        return LocalDateTimeUtil.between(dateTime, LocalDateTime.now(), ChronoUnit.DAYS);
    }

    /**
     * 获取今天的开始时间(今天 00:00:00.000)
     *
     * @return LocalDateTime 今天的开始时间
     */
    public static LocalDateTime getToday() {
        // LocalDateTime.now() 获取当前时间
        // LocalDateTimeUtil.beginOfDay() 获取该日期的开始时间(00:00:00.000)
        return LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
    }

    /**
     * 获取昨天的开始时间(昨天 00:00:00.000)
     *
     * @return LocalDateTime 昨天的开始时间
     */
    public static LocalDateTime getYesterday() {
        // minusDays(1) 减去1天,即昨天
        // LocalDateTimeUtil.beginOfDay() 获取该日期的开始时间(00:00:00.000)
        return LocalDateTimeUtil.beginOfDay(LocalDateTime.now().minusDays(1));
    }

    /**
     * 获取本月的开始时间(本月第一天 00:00:00.000)
     *
     * @return LocalDateTime 本月的开始时间
     */
    public static LocalDateTime getMonth() {
        // 调用 beginOfMonth() 获取本月的开始时间
        return beginOfMonth(LocalDateTime.now());
    }

    /**
     * 获取本年的开始时间(本年第一天 00:00:00.000)
     *
     * @return LocalDateTime 本年的开始时间
     */
    public static LocalDateTime getYear() {
        // TemporalAdjusters.firstDayOfYear() 调整到本年的第一天(1月1日)
        // LocalTime.MIN 表示 00:00:00.000
        return LocalDateTime.now().with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
    }

    /**
     * 将一个大时间段按指定的时间间隔分割成多个小时间段(时间范围列表)
     *
     * 此方法主要用于数据统计和报表生成场景，将一个时间范围按照指定粒度(小时/天/周/月/季/年)分割成多个连续的时间段。
     * 每个时间段包含开始时间和结束时间，时间段之间无缝衔接，不会有重叠或遗漏。
     *
     * 主要特点：
     * - 自动对齐：开始时间自动调整到当天的 00:00:00，结束时间调整到当天的 23:59:59.999999999
     * - 无缝衔接：所有时间段首尾相连，前一个时间段的结束时间的下一纳秒是后一个时间段的开始时间
     * - 边界处理: 最后一个时间段的结束时间会调整为参数中的 endTime，确保完全覆盖整个范围
     * - 自然分割: 按周/月/季/年分割时,会按照自然周期(如自然月、自然季度)进行分割
     *
     * @param startTime 大时间段的开始时间，例如：LocalDateTime.of(2024, 1, 1, 10, 30, 0)
     * @param endTime   大时间段的结束时间，例如：LocalDateTime.of(2024, 3, 31, 18, 45, 0)
     * @param interval  时间间隔类型，对应 {@link DateIntervalEnum} 的值：
     *                                  1 - HOUR(小时)：按小时分割
     *                                  2 - DAY(天)：按天分割
     *                                  3 - WEEK(周)：按周分割(周日结束)
     *                                  4 - MONTH(月)：按自然月分割
     *                                  5 - QUARTER(季度)：按季度分割
     *                                  6 - YEAR(年)：按年分割
     * @return          时间段列表，每个元素是一个包含两个 LocalDateTime 的数组
     *                  - [0]：该时间段的开始时间
     *                  - [1]：该时间段的结束时间
     * @throws IllegalArgumentException 如果 interval 对应的枚举不存在或无效
     *
     * 使用场景：
     * - 数据报表：生成按天/月/季度的销售统计报表
     * - 图表展示：为折线图、柱状图生成时间轴的数据点
     * - 批量查询：将大时间范围拆分成多个小范围，分批查询数据库避免超时
     *
     * 示例 1: 按天分割
     * LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
     * LocalDateTime end = LocalDateTime.of(2024, 1, 3, 18, 0, 0);
     * List<LocalDateTime[]> ranges = getDateRangeList(start, end, 2); // 2 = DAY
     *
     * // 返回结果 (3个时间段):
     * // [0]: [2024-01-01 00:00:00.000, 2024-01-01 23:59:59.999999999]
     * // [1]: [2024-01-02 00:00:00.000, 2024-01-02 23:59:59.999999999]
     * // [2]: [2024-01-03 00:00:00.000, 2024-01-03 23:59:59.999999999]
     *
     * 示例 2: 按月分割
     * LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
     * LocalDateTime end = LocalDateTime.of(2024, 3, 20, 0, 0, 0);
     * List<LocalDateTime[]> ranges = getDateRangeList(start, end, 4); // 4 = MONTH
     *
     * // 返回结果 (3个自然月):
     * // [0]: [2024-01-15 00:00:00, 2024-01-31 23:59:59.999999999] (从开始日期到1月末)
     * // [1]: [2024-02-01 00:00:00, 2024-02-29 23:59:59.999999999] (整个2月,闰年)
     * // [2]: [2024-03-01 00:00:00, 2024-03-20 23:59:59.999999999] (3月初到结束日期)
     *
     * 示例 3: 实际应用 - 生成销售统计报表
     * // 生成2024年第一季度的月度销售报表
     * LocalDateTime q1Start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
     * LocalDateTime q1End = LocalDateTime.of(2024, 3, 31, 23, 59, 59);
     * List<LocalDateTime[]> monthRanges = getDateRangeList(q1Start, q1End, 4); // 按月分割
     *
     * for (LocalDateTime[] range : monthRanges) {
     *     // 查询每个月的销售数据
     *     BigDecimal monthlySales = salesService.querySales(range[0], range[1]);
     *     System.out.println("销售额: " + monthlySales);
     * }
     */
    public static List<LocalDateTime[]> getDateRangeList(LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         Integer interval) {
        // 1.1 根据传入的 interval 参数找到对应的枚举值
        // DateIntervalEnum.valueOf() 将整数值转换为枚举对象
        DateIntervalEnum intervalEnum = DateIntervalEnum.valueOf(interval);
        Assert.notNull(intervalEnum, "interval({}} 找不到对应的枚举", interval);

        // 1.2 将时间对齐到天的边界
        // beginOfDay() 将开始时间调整到当天的 00:00:00.000
        // endOfDay() 将结束时间调整到当天的 23:59:59.999999999
        startTime = LocalDateTimeUtil.beginOfDay(startTime);
        endTime = LocalDateTimeUtil.endOfDay(endTime);

        // 2. 循环遍历，根据不同的时间间隔类型生成时间范围列表
        List<LocalDateTime[]> timeRanges = new ArrayList<>();
        switch (intervalEnum) {
            case HOUR:
                // 按小时分割：每次增加1小时
                while (startTime.isBefore(endTime)) {
                    // minusNanos(1) 减去1纳秒，使结束时间为该小时的最后一纳秒
                    // 例如：10:00:00 到 10:59:59.999999999
                    timeRanges.add(new LocalDateTime[]{startTime, startTime.plusHours(1).minusNanos(1)});
                    startTime = startTime.plusHours(1);
                }
            case DAY:
                // 按天分割：每次增加1天
                while (startTime.isBefore(endTime)) {
                    // 例如: 2024-01-01 00:00:00 到 2024-01-01 23:59:59.999999999
                    timeRanges.add(new LocalDateTime[]{startTime, startTime.plusDays(1).minusNanos(1)});
                    startTime = startTime.plusDays(1);
                }
                break;
            case WEEK:
                // 按周分割: 每周从当前日期到周日
                while (startTime.isBefore(endTime)) {
                    // with(DayOfWeek.SUNDAY) 调整到本周的周日
                    // plusDays(1).minusNanos(1) 得到周日的 23:59:59.999999999
                    LocalDateTime endOfWeek = startTime.with(DayOfWeek.SUNDAY).plusDays(1).minusNanos(1);
                    timeRanges.add(new LocalDateTime[]{startTime, endOfWeek});
                    // plusNanos(1) 移动到下一周的开始(下周一 00:00:00)
                    startTime = endOfWeek.plusNanos(1);
                }
                break;
            case MONTH:
                // 按月分割：每次到当月的最后一天
                while (startTime.isBefore(endTime)) {
                    // TemporalAdjusters.lastDayOfMonth() 调整到当月最后一天
                    // plusDays(1).minusNanos(1) 得到当月最后一天的 23:59:59.999999999
                    LocalDateTime endOfMonth = startTime.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).minusNanos(1);
                    timeRanges.add(new LocalDateTime[]{startTime, endOfMonth});
                    // plusNanos(1) 移动到下个月的第一天 00:00:00
                    startTime = endOfMonth.plusNanos(1);
                }
                break;
            case QUARTER:
                // 按季度分割
                while (startTime.isBefore(endTime)) {
                    // 获取当前所在季度(1-4)
                    int quarterOfYear = getQuarterOfYear(startTime);
                    LocalDateTime quarterEnd;
                    if (quarterOfYear == 4) {
                        // 如果是第4季度，结束时间为本年最后一天
                        quarterEnd = startTime.with(TemporalAdjusters.lastDayOfYear()).plusDays(1).minusNanos(1);
                    } else {
                        // 其他季度，结束时间为下个季度第一个月的前一天
                        // 例如：第1季度(1-3月)结束于3月31日 = 4月1日的前一纳秒
                        quarterEnd = startTime.withMonth(quarterOfYear * 3 + 1).withDayOfMonth(1).minusNanos(1);
                    }
                    timeRanges.add(new LocalDateTime[]{startTime, quarterEnd});
                    // 移动到下个季度的开始
                    startTime = quarterEnd.plusNanos(1);
                }
                break;
            case YEAR:
                // 按年分割
                while (startTime.isBefore(endTime)) {
                    // TemporalAdjusters.lastDayOfYear() 调整到本年最后一天(12月31日)
                    LocalDateTime endOfYear = startTime.with(TemporalAdjusters.lastDayOfYear()).plusDays(1).minusNanos(1);
                    timeRanges.add(new LocalDateTime[]{startTime, endOfYear});
                    // 移动到下一年的1月1日
                    startTime = endOfYear.plusNanos(1);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid interval: " + interval);
        }
        // 3. 兜底处理：调整最后一个时间段的结束时间为参数中的 endTime
        // 因为循环可能会超出原始的 endTime，需要将最后一个时间段的结束时间调整回来
        // 例如：endTime 为 2024-01-15，但按月分割会到 2024-01-31，需要调整回 2024-01-15
        LocalDateTime[] lastTimeRange = CollUtil.getLast(timeRanges);
        if (lastTimeRange != null) {
            lastTimeRange[1] = endTime;
        }
        return timeRanges;
    }

    /**
     * 根据时间间隔类型格式化时间范围的显示文本
     *
     * 此方法将指定时间范围按照指定的时间间隔类型格式化为易读的字符串,常用于报表标题、图表横轴标签等场景。
     * 不同的时间间隔会采用不同的格式化方式，以最直观的方式展示时间信息。
     *
     * 格式化规则：
     * HOUR(小时)：格式化为 "yyyy-MM-dd HH:mm"，例如："2024-01-15 14:30"
     * DAY(天)：格式化为 "yyyy-MM-dd"，例如："2024-01-15"
     * WEEK(周)：格式化为 "yyyy-MM-dd(第 N 周)"，例如："2024-01-15(第 3 周)"
     * MONTH(月)：格式化为 "yyyy-MM"，例如："2024-01"
     * QUARTER(季度)：格式化为 "yyyy-QN"，例如："2024-Q1"
     * YEAR(年)：格式化为 "yyyy"，例如："2024"
     *
     * @param startTime 时间范围的开始时间，例如：LocalDateTime.of(2024, 1, 15, 14, 30, 0)
     * @param endTime   时间范围的结束时间，例如：LocalDateTime.of(2024, 1, 15, 18, 30, 0) (注意：此参数当前未被使用)
     * @param interval  时间间隔类型，对应 {@link DateIntervalEnum} 的值：
     *                                  1 - HOUR(小时)
     *                                  2 - DAY(天)
     *                                  3 - WEEK(周)
     *                                  4 - MONTH(月)
     *                                  5 - QUARTER(季度)
     *                                  6 - YEAR(年)
     * @return String 格式化后的时间范围字符串
     * @throws IllegalArgumentException 如果 interval 对应的枚举不存在或无效
     *
     * 使用场景：
     * - 报表生成：为统计报表的时间列生成易读的标签
     * - 图表展示：为折线图、柱状图的横轴生成时间标签
     * - 界面显示：在前端页面展示时间段信息
     *
     * 示例：
     * LocalDateTime time = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
     *
     * formatDateRange(time, null, 1)  → "2024-01-15 14:30"  (按小时)
     * formatDateRange(time, null, 2)  → "2024-01-15"        (按天)
     * formatDateRange(time, null, 3)  → "2024-01-15(第 3 周)" (按周,假设是第3周)
     * formatDateRange(time, null, 4)  → "2024-01"           (按月)
     * formatDateRange(time, null, 5)  → "2024-Q1"           (按季度,1月属于第1季度)
     * formatDateRange(time, null, 6)  → "2024"              (按年)
     *
     * 配合 getDateRangeList 使用示例：
     * // 生成月度报表的时间标签
     * LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
     * LocalDateTime end = LocalDateTime.of(2024, 3, 31, 23, 59, 59);
     * List<LocalDateTime[]> ranges = getDateRangeList(start, end, 4); // 按月分割
     *
     * for (LocalDateTime[] range : ranges) {
     *     String label = formatDateRange(range[0], range[1], 4);
     *     System.out.println("月份: " + label); // 输出: 2024-01, 2024-02, 2024-03
     * }
     */
    public static String formatDateRange(LocalDateTime startTime, LocalDateTime endTime, Integer interval) {
        // 1. 根据 interval 参数找到对应的枚举值
        DateIntervalEnum intervalEnum = DateIntervalEnum.valueOf(interval);
        Assert.notNull(intervalEnum, "interval({}} 找不到对应的枚举", interval);

        // 2. 根据不同的时间间隔类型,选择不同的格式化方式
        switch (intervalEnum) {
            case HOUR:
                // 按小时：格式化为 "yyyy-MM-dd HH:mm"
                // NORM_DATETIME_MINUTE_PATTERN = "yyyy-MM-dd HH:mm"
                return LocalDateTimeUtil.format(startTime, DatePattern.NORM_DATETIME_MINUTE_PATTERN);
            case DAY:
                // 按天：格式化为 "yyyy-MM-dd"
                // NORM_DATE_PATTERN = "yyyy-MM-dd"
                return LocalDateTimeUtil.format(startTime, DatePattern.NORM_DATE_PATTERN);
            case WEEK:
                // 按周：格式化为 "yyyy-MM-dd(第 N 周)"
                // weekOfYear() 获取该日期是一年中的第几周
                return LocalDateTimeUtil.format(startTime, DatePattern.NORM_DATE_PATTERN)
                        + StrUtil.format("(第 {} 周)", LocalDateTimeUtil.weekOfYear(startTime));
            case MONTH:
                // 按月：格式化为 "yyyy-MM"
                // NORM_MONTH_PATTERN = "yyyy-MM"
                return LocalDateTimeUtil.format(startTime, DatePattern.NORM_MONTH_PATTERN);
            case QUARTER:
                // 按季度：格式化为 "yyyy-QN" (N为1-4)
                // 例如：2024年第1季度 → "2024-Q1"
                return StrUtil.format("{}-Q{}", startTime.getYear(), getQuarterOfYear(startTime));
            case YEAR:
                // 按年：格式化为 "yyyy"
                // NORM_YEAR_PATTERN = "yyyy"
                return LocalDateTimeUtil.format(startTime, DatePattern.NORM_YEAR_PATTERN);
            default:
                throw new IllegalArgumentException("Invalid interval: " + interval);
        }
    }

    /**
     * 将 LocalDateTime 转换为 Unix 时间戳(秒数)
     *
     * 此方法将 {@link LocalDateTime} 对象转换为从 Unix 纪元时间(1970-01-01 00:00:00 UTC)到指定时间经过的秒数。
     * 这种时间戳格式在很多场景下非常有用，如与前端 JavaScript 交互、数据库存储、API 传输等。
     *
     * 注意事项：
     * - 返回的是秒数，不是毫秒数。如需毫秒数，请使用 {@code toEpochSecond(time) * 1000}
     * - 转换时会使用系统默认时区，,不同时区会得到不同的时间戳
     * - Unix 时间戳的起点是 1970-01-01 00:00:00 UTC，早于此时间的值为负数
     *
     * @param sourceDateTime 需要转换的 LocalDateTime 对象，不能为 null
     *                       例如：LocalDateTime.of(2024, 1, 15, 14, 30, 45)
     * @return Long          从 1970-01-01 00:00:00 UTC 起经过的秒数。
     *                       例如：2024-01-15 14:30:45 (UTC+8) → 1705299045 秒
     * @throws NullPointerException 如果 sourceDateTime 为 null
     * @throws DateTimeException    如果转换过程中发生时间超出范围或其他时间处理异常
     *
     * 使用场景：
     * - 前后端交互：JavaScript 使用时间戳(毫秒)，后端提供秒数，前端乘以1000即可
     * - 数据库存储：某些数据库使用时间戳存储时间，节省存储空间
     * - API 接口：统一的时间格式，避免时区和格式问题
     * - 缓存过期：Redis 等缓存系统通常使用时间戳设置过期时间
     *
     * 示例：
     * // 示例1：基本使用
     * LocalDateTime time = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
     * Long epochSecond = toEpochSecond(time);
     * System.out.println(epochSecond); // 输出：1705299045 (假设时区为 UTC+8)
     *
     * // 示例2：转换为毫秒时间戳(用于前端)
     * Long epochMilli = toEpochSecond(time) * 1000;
     * System.out.println(epochMilli); // 输出：1705299045000
     *
     * // 示例3：与 Unix 纪元时间的关系
     * LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
     * Long zero = toEpochSecond(epoch);
     * System.out.println(zero); // 输出：可能不是0，因为要考虑时区偏移
     *
     * // 示例4：计算两个时间的时间戳差值
     * LocalDateTime time1 = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
     * LocalDateTime time2 = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
     * Long diff = toEpochSecond(time2) - toEpochSecond(time1);
     * System.out.println("相差 " + diff + " 秒"); // 输出：相差 16245 秒 (4小时30分45秒)
     */
    public static Long toEpochSecond(LocalDateTime sourceDateTime) {
        // TemporalAccessorUtil.toInstant() 将 LocalDateTime 转换为 Instant (时间戳)
        // getEpochSecond() 获取从 Unix 纪元时间起经过的秒数
        // 注意: 转换过程会考虑系统默认时区
        return TemporalAccessorUtil.toInstant(sourceDateTime).getEpochSecond();
    }

}
