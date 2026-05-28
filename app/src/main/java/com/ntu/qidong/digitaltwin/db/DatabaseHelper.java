package com.ntu.qidong.digitaltwin.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite 数据库辅助类
 * 负责数据库创建、版本管理和表结构初始化
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ntu_qidong.db";
    private static final int DATABASE_VERSION = 1;

    // weather_history 表的建表语句
    private static final String CREATE_WEATHER_HISTORY_TABLE =
            "CREATE TABLE IF NOT EXISTS weather_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT NOT NULL UNIQUE," +
                    "temp_avg REAL NOT NULL," +
                    "temp_max REAL NOT NULL," +
                    "temp_min REAL NOT NULL," +
                    "humidity INTEGER NOT NULL," +
                    "weather_condition TEXT," +
                    "created_at INTEGER NOT NULL" +
                    ");";

    // app_visit 表的建表语句
    private static final String CREATE_APP_VISIT_TABLE =
            "CREATE TABLE IF NOT EXISTS app_visit (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "visit_date TEXT NOT NULL UNIQUE," +
                    "visit_count INTEGER NOT NULL DEFAULT 0," +
                    "last_visit_time INTEGER NOT NULL" +
                    ");";

    private static DatabaseHelper instance;

    /**
     * 获取数据库单例实例
     * 采用单例模式确保数据库只被打开一次
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建天气历史记录表
        db.execSQL(CREATE_WEATHER_HISTORY_TABLE);
        // 创建应用访问记录表
        db.execSQL(CREATE_APP_VISIT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 当数据库版本升级时执行
        // 实际项目中应进行数据迁移，此处简化为删除重建
        db.execSQL("DROP TABLE IF EXISTS weather_history");
        db.execSQL("DROP TABLE IF EXISTS app_visit");
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // 启用外键约束
        db.setForeignKeyConstraintsEnabled(true);
    }
}
