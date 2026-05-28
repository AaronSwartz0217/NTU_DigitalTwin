package com.ntu.qidong.digitaltwin.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ntu.qidong.digitaltwin.model.WeatherHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * 天气历史数据访问对象 (DAO)
 * 提供 weather_history 表的完整 CRUD 操作
 * 查询性能目标: < 10ms
 */
public class WeatherHistoryDAO {

    private static final String TABLE_NAME = "weather_history";
    private final DatabaseHelper dbHelper;

    public WeatherHistoryDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * 插入或更新天气历史数据
     * 如果当日数据已存在则更新，不存在则插入
     *
     * @param history 天气历史数据
     * @return 操作是否成功
     */
    public boolean insertOrUpdate(WeatherHistory history) {
        if (history == null || history.getDate() == null) {
            return false;
        }

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("date", history.getDate());
            values.put("temp_avg", history.getTempAvg());
            values.put("temp_max", history.getTempMax());
            values.put("temp_min", history.getTempMin());
            values.put("humidity", history.getHumidity());
            values.put("weather_condition", history.getWeatherCondition());
            values.put("created_at", history.getCreatedAt());

            // 使用 REPLACE 策略: 如果日期已存在则替换
            long result = db.insertWithOnConflict(TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeDatabase(db);
        }
    }

    /**
     * 根据日期查询天气历史数据
     *
     * @param date 日期 (格式: yyyy-MM-dd)
     * @return 天气历史数据，未找到则返回 null
     */
    public WeatherHistory findByDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE_NAME, null, "date = ?",
                    new String[]{date}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToHistory(cursor);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeCursor(cursor);
            closeDatabase(db);
        }
    }

    /**
     * 获取最近 N 天的天气历史数据
     *
     * @param days 天数
     * @return 天气历史数据列表 (按日期升序排列)
     */
    public List<WeatherHistory> findRecentDays(int days) {
        List<WeatherHistory> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE_NAME, null, null, null, null, null,
                    "date ASC", String.valueOf(days));

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToHistory(cursor));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeCursor(cursor);
            closeDatabase(db);
        }
        return list;
    }

    /**
     * 获取所有天气历史数据
     *
     * @return 所有天气历史数据列表
     */
    public List<WeatherHistory> findAll() {
        return findRecentDays(Integer.MAX_VALUE);
    }

    /**
     * 将 Cursor 转换为 WeatherHistory 对象
     */
    private WeatherHistory cursorToHistory(Cursor cursor) {
        WeatherHistory history = new WeatherHistory();
        history.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        history.setDate(cursor.getString(cursor.getColumnIndexOrThrow("date")));
        history.setTempAvg(cursor.getDouble(cursor.getColumnIndexOrThrow("temp_avg")));
        history.setTempMax(cursor.getDouble(cursor.getColumnIndexOrThrow("temp_max")));
        history.setTempMin(cursor.getDouble(cursor.getColumnIndexOrThrow("temp_min")));
        history.setHumidity(cursor.getInt(cursor.getColumnIndexOrThrow("humidity")));
        history.setWeatherCondition(cursor.getString(cursor.getColumnIndexOrThrow("weather_condition")));
        history.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return history;
    }

    /**
     * 关闭 Cursor
     */
    private void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * 关闭数据库连接
     */
    private void closeDatabase(SQLiteDatabase db) {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
