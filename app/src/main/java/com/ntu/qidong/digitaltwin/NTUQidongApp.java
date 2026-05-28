package com.ntu.qidong.digitaltwin;

import android.app.Application;

import com.ntu.qidong.digitaltwin.db.AppVisitDAO;
import com.ntu.qidong.digitaltwin.db.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Application 类
 * 负责应用级别的初始化和全局状态管理
 */
public class NTUQidongApp extends Application {

    private static NTUQidongApp instance;
    private DatabaseHelper databaseHelper;
    private AppVisitDAO appVisitDAO;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化数据库
        databaseHelper = DatabaseHelper.getInstance(this);
        appVisitDAO = new AppVisitDAO(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        // 记录应用访问
        recordAppVisit();

        // 初始化数据库 (触发表创建)
        databaseHelper.getReadableDatabase();

        android.util.Log.d("NTUQidongApp", "应用初始化完成");
    }

    /**
     * 获取 Application 单例
     */
    public static NTUQidongApp getInstance() {
        return instance;
    }

    /**
     * 获取 DatabaseHelper
     */
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    /**
     * 获取 AppVisitDAO
     */
    public AppVisitDAO getAppVisitDAO() {
        return appVisitDAO;
    }

    /**
     * 获取日期格式化器
     */
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * 记录应用访问
     */
    private void recordAppVisit() {
        if (appVisitDAO != null) {
            String today = dateFormat.format(new Date());
            appVisitDAO.recordVisit(today);
        }
    }

    /**
     * 获取当前日期字符串
     */
    public String getCurrentDateString() {
        return dateFormat.format(new Date());
    }
}
