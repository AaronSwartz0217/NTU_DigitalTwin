package com.ntu.qidong.digitaltwin.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类
 * 提供日期格式化和计算功能
 */
public class DateUtils {

    private static final String TAG = "DateUtils";

    /**
     * 获取日期字符串
     *
     * @param date   Date 对象
     * @param format 日期格式
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Date date, String format) {
        if (date == null) {
            return "";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.CHINA);
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "日期格式化失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 获取今天的日期字符串
     *
     * @param format 日期格式
     * @return 格式化后的日期字符串
     */
    public static String getTodayString(String format) {
        return formatDate(new Date(), format);
    }

    /**
     * 获取 N 天前的日期字符串
     *
     * @param days   天数
     * @param format 日期格式
     * @return 格式化后的日期字符串
     */
    public static String getDaysAgoString(int days, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        return formatDate(calendar.getTime(), format);
    }

    /**
     * 获取最近 N 天日期字符串数组
     *
     * @param days   天数
     * @param format 日期格式
     * @return 日期字符串数组
     */
    public static String[] getRecentDaysArray(int days, String format) {
        String[] dates = new String[days];
        for (int i = days - 1; i >= 0; i--) {
            dates[days - 1 - i] = getDaysAgoString(i, format);
        }
        return dates;
    }

    /**
     * 解析日期字符串
     *
     * @param dateString 日期字符串
     * @param format     日期格式
     * @return Date 对象
     */
    public static Date parseDate(String dateString, String format) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.CHINA);
            return sdf.parse(dateString);
        } catch (Exception e) {
            Log.e(TAG, "日期解析失败: " + e.getMessage());
            return null;
        }
    }
}
