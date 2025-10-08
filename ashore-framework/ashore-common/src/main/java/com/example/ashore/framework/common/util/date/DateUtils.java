package com.example.ashore.framework.common.util.date;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.*;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间日期工具类
 * 包括：
 * - Date 与 LocalDateTime 之间的类型转换
 * - 时间计算与比较(增加时间间隔、获取最大值等)
 * - 日期构建(根据年月日时分秒创建日期对象)
 * - 日期判断(是否今天、是否昨天、是否过期等)
 */
public class DateUtils {

    // 时区 - 默认(东八区,北京时间)
    public static final String TIME_ZONE_DEFAULT = "GMT+8";

    // 秒转换成毫秒的倍数
    public static final long SECOND_MILLIS = 1000;

    // 日期格式：年-月-日，例如：2024-01-15
    public static final String FORMAT_YEAR_MONTH_DAY = "yyyy-MM-dd";

    // 日期时间格式：年-月-日 时:分:秒，例如：2024-01-15 14:30:45
    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将 LocalDateTime 转换成 Date
     *
     * @param date LocalDateTime 对象，例如：LocalDateTime.of(2024, 1, 15, 14, 30, 45)
     *             可以为 null
     * @return     Date 对象
     *             如果参数为 null 则返回 null
     */
    public static Date of(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        // 将此日期时间与时区相结合以创建 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        // 本地时间线 LocalDateTime 到即时时间线 Instant 时间戳
        Instant instant = zonedDateTime.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return Date.from(instant);
    }

    /**
     * 将 Date 转换成 LocalDateTime
     *
     * @param date Date 对象，例如：new Date(1705299045000L)
     *             可以为 null
     * @return     LocalDateTime 对象
     *             如果参数为 null 则返回 null
     */
    public static LocalDateTime of(Date date) {
        if (date == null) {
            return null;
        }
        // 转为时间戳
        Instant instant = date.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * 在当前时间基础上增加指定的时间间隔
     *
     * @param duration 时间间隔，例如：Duration.ofHours(2) 表示 2 小时
     * @return         增加时间间隔后的 Date 对象
     */
    public static Date addTime(Duration duration) {
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }

    /**
     * 判断指定时间是否已过期(是否在当前时间之前)
     *
     * @param time     待判断的时间,例如:LocalDateTime.of(2024, 1, 1, 10, 0, 0)
     * @return boolean true-已过期，false-未过期
     */
    public static boolean isExpired(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(time);
    }

    /**
     * 创建指定日期的时间(时分秒默认为 00:00:00)
     *
     * @param year  年份
     * @param month 月份，取值范围 1-12
     * @param day   日期，取值范围 1-31
     * @return      创建的 Date 对象。例如：buildTime(2024, 1, 15) 返回 2024-01-15 00:00:00
     */
    public static Date buildTime(int year, int month, int day) {
        return buildTime(year, month, day, 0, 0, 0);
    }

    /**
     * 创建指定日期和时间
     *
     * @param year   年份
     * @param month  月份，取值范围 1-12
     * @param day    日期，取值范围 1-31
     * @param hour   小时，取值范围 0-23
     * @param minute 分钟，取值范围 0-59
     * @param second 秒，取值范围 0-59
     * @return       创建的 Date 对象，毫秒数固定为 0。例如:buildTime(2024, 1, 15, 14, 30, 45) 返回 2024-01-15 14:30:45.000
     */
    public static Date buildTime(int year, int month, int day,
                                 int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0); // 一般情况下，都是 0 毫秒
        return calendar.getTime();
    }

    /**
     * 获取两个日期中的最大值(较晚的日期)
     *
     * @param a 第一个日期，例如：2024-01-01 10:00:00，可以为 null
     * @param b 第二个日期，例如：2024-01-02 10:00:00，可以为 null
     * @return 返回较晚的 Date 对象，
     *         如果其中一个为 null 则返回另一个，
     *         如果都为 null 则返回 null
     */
    public static Date max(Date a, Date b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) > 0 ? a : b;
    }

    /**
     * 获取两个 LocalDateTime 中的最大值(较晚的时间)
     *
     * @param a 第一个日期时间，例如：LocalDateTime.of(2024, 1, 1, 10, 0, 0)，可以为 null
     * @param b 第二个日期时间，例如：LocalDateTime.of(2024, 1, 2, 10, 0, 0)，可以为 null
     * @return  返回较晚的 LocalDateTime 对象，
     *          如果其中一个为 null 则返回另一个，
     *          如果都为 null 则返回 null
     */
    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 判断指定日期是否为今天
     *
     * @param date     待判断的日期时间，例如:LocalDateTime.of(2024, 1, 15, 14, 30, 45)
     * @return boolean true-是今天，false-不是今天
     */
    public static boolean isToday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now());
    }

    /**
     * 判断指定日期是否为昨天
     *
     * @param date     待判断的日期时间，例如：LocalDateTime.of(2024, 1, 14, 14, 30, 45)
     * @return boolean true-是昨天，false-不是昨天
     */
    public static boolean isYesterday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now().minusDays(1));
    }

}