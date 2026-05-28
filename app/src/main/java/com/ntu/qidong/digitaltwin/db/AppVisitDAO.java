package com.ntu.qidong.digitaltwin.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ntu.qidong.digitaltwin.model.AppVisit;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用访问记录数据访问对象 (DAO)
 * 提供 app_visit 表的完整 CRUD 操作
 */
public class AppVisitDAO {

    private static final String TABLE_NAME = "app_visit";
    private final DatabaseHelper dbHelper;

    public AppVisitDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * 记录一次应用访问
     * 如果当日记录已存在则累加访问次数，否则创建新记录
     *
     * @param visitDate 访问日期 (格式: yyyy-MM-dd)
     * @return 操作是否成功
     */
    public boolean recordVisit(String visitDate) {
        if (visitDate == null || visitDate.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            long currentTime = System.currentTimeMillis();

            // 查询当日是否已有访问记录
            Cursor cursor = db.query(TABLE_NAME, null, "visit_date = ?",
                    new String[]{visitDate}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // 已有记录，累加访问次数
                int currentCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"));
                ContentValues values = new ContentValues();
                values.put("visit_count", currentCount + 1);
                values.put("last_visit_time", currentTime);

                int rows = db.update(TABLE_NAME, values, "visit_date = ?",
                        new String[]{visitDate});
                cursor.close();
                return rows > 0;
            } else {
                // 无记录，创建新记录
                if (cursor != null) {
                    cursor.close();
                }
                ContentValues values = new ContentValues();
                values.put("visit_date", visitDate);
                values.put("visit_count", 1);
                values.put("last_visit_time", currentTime);

                long result = db.insert(TABLE_NAME, null, values);
                return result != -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeDatabase(db);
        }
    }

    /**
     * 获取累计访问次数
     *
     * @return 累计访问次数
     */
    public int getTotalVisitCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT SUM(visit_count) FROM " + TABLE_NAME, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeCursor(cursor);
            closeDatabase(db);
        }
    }

    /**
     * 根据日期获取访问记录
     *
     * @param visitDate 访问日期 (格式: yyyy-MM-dd)
     * @return 访问记录，未找到则返回 null
     */
    public AppVisit findByDate(String visitDate) {
        if (visitDate == null || visitDate.isEmpty()) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE_NAME, null, "visit_date = ?",
                    new String[]{visitDate}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToVisit(cursor);
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
     * 获取今日访问次数
     *
     * @param todayDate 今日日期 (格式: yyyy-MM-dd)
     * @return 今日访问次数
     */
    public int getTodayVisitCount(String todayDate) {
        AppVisit visit = findByDate(todayDate);
        return (visit != null) ? visit.getVisitCount() : 0;
    }

    /**
     * 获取所有访问记录
     *
     * @return 访问记录列表
     */
    public List<AppVisit> findAll() {
        List<AppVisit> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(TABLE_NAME, null, null, null, null, null, "visit_date ASC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToVisit(cursor));
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
     * 将 Cursor 转换为 AppVisit 对象
     */
    private AppVisit cursorToVisit(Cursor cursor) {
        AppVisit visit = new AppVisit();
        visit.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        visit.setVisitDate(cursor.getString(cursor.getColumnIndexOrThrow("visit_date")));
        visit.setVisitCount(cursor.getInt(cursor.getColumnIndexOrThrow("visit_count")));
        visit.setLastVisitTime(cursor.getLong(cursor.getColumnIndexOrThrow("last_visit_time")));
        return visit;
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
