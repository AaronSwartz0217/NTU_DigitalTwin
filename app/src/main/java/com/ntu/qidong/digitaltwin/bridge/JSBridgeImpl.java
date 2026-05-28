package com.ntu.qidong.digitaltwin.bridge;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.ntu.qidong.digitaltwin.service.WeatherService;

/**
 * JSBridge 实现类
 * 实现前端调用原生的接口
 *
 * 接口命名规范:
 * - 前端调用原生: AndroidBridge.xxx / WeatherBridge.xxx
 * - 原生向前端注入: window.updateWeather / window.WeatherBridge
 */
public class JSBridgeImpl {

    private static final String TAG = "JSBridgeImpl";

    private final Context context;
    private final WeatherService weatherService;
    private final Handler mainHandler;

    public JSBridgeImpl(Context context, WeatherService weatherService) {
        this.context = context;
        this.weatherService = weatherService;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取天气历史数据
     * 前端调用: window.WeatherBridge.getWeatherHistory(days)
     *
     * @param days 查询的天数
     * @return JSON 格式的天气历史数据
     */
    @JavascriptInterface
    public String getWeatherHistory(int days) {
        Log.d(TAG, "前端请求天气历史数据，天数: " + days);
        return weatherService.getWeatherHistoryJson(Math.min(days, 30)); // 限制最多30天
    }

    /**
     * 获取当前天气数据
     * 前端调用: window.WeatherBridge.getCurrentWeather()
     *
     * @return JSON 格式的当前天气数据
     */
    @JavascriptInterface
    public String getCurrentWeather() {
        Log.d(TAG, "前端请求当前天气数据");
        return weatherService.weatherDataToJson(weatherService.getCurrentWeatherData());
    }

    /**
     * 显示建筑信息 (建筑点击事件)
     * 前端调用: window.AndroidBridge.showBuildingInfo(buildingId)
     *
     * @param buildingId 建筑 ID
     * @return JSON 格式的操作结果
     */
    @JavascriptInterface
    public String showBuildingInfo(String buildingId) {
        Log.d(TAG, "前端触发建筑点击事件，建筑ID: " + buildingId);

        // 在实际项目中，这里应该打开建筑详情页面或返回更多信息
        // 当前阶段仅记录日志和显示 Toast
        mainHandler.post(() -> {
            Toast.makeText(context, "建筑点击事件: " + buildingId, Toast.LENGTH_SHORT).show();
        });

        // 使用 JSONObject 避免 JSON 注入
        try {
            org.json.JSONObject result = new org.json.JSONObject();
            result.put("success", true);
            result.put("buildingId", buildingId);
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "构建JSON失败", e);
            return "{\"success\":false}";
        }
    }

    /**
     * 获取应用版本信息
     * 前端调用: window.AndroidBridge.getAppVersion()
     *
     * @return JSON 格式的版本信息
     */
    @JavascriptInterface
    public String getAppVersion() {
        try {
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            int versionCode = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;

            // 使用 JSONObject 避免 JSON 注入
            org.json.JSONObject result = new org.json.JSONObject();
            result.put("versionName", versionName);
            result.put("versionCode", versionCode);
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取版本信息失败: " + e.getMessage());
            return "{\"error\":\"Failed to get version info\"}";
        }
    }

    /**
     * 记录前端事件
     * 前端调用: window.AndroidBridge.logEvent(eventType, eventData)
     *
     * @param eventType 事件类型
     * @param eventData 事件数据 (JSON 格式)
     * @return JSON 格式的操作结果
     */
    @JavascriptInterface
    public String logEvent(String eventType, String eventData) {
        Log.d(TAG, "前端事件: type=" + eventType + ", data=" + eventData);
        return "{\"success\":true}";
    }

    /**
     * 获取完整的应用状态数据（用于前端UI显示）
     * 前端调用: window.AndroidBridge.getAppState()
     *
     * @return JSON 格式的完整应用状态
     */
    @JavascriptInterface
    public String getAppState() {
        try {
            com.ntu.qidong.digitaltwin.db.AppVisitDAO visitDAO = 
                new com.ntu.qidong.digitaltwin.db.AppVisitDAO(context);
            
            int totalVisits = visitDAO.getTotalVisitCount();
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
            int todayVisits = visitDAO.getTodayVisitCount(today);
            
            // 计算平均温度（通过获取历史数据计算）
            com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO weatherDAO = 
                new com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO(context);
            java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> allHistory = weatherDAO.findAll();
            double avgTemp = 0;
            if (!allHistory.isEmpty()) {
                double sum = 0;
                for (com.ntu.qidong.digitaltwin.model.WeatherHistory h : allHistory) {
                    sum += h.getTempAvg();
                }
                avgTemp = sum / allHistory.size();
            }
            
            String versionName = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
            
            org.json.JSONObject state = new org.json.JSONObject();
            state.put("version", versionName);
            state.put("platform", "android");
            state.put("uiMode", "webview-overlay");
            
            org.json.JSONObject statistics = new org.json.JSONObject();
            statistics.put("totalVisits", totalVisits);
            statistics.put("todayVisits", todayVisits);
            statistics.put("averageTemperature", avgTemp);
            state.put("statistics", statistics);
            
            // 手动构建当前天气JSON对象
            com.ntu.qidong.digitaltwin.model.WeatherData weatherData = weatherService.getCurrentWeatherData();
            org.json.JSONObject currentWeather = new org.json.JSONObject();
            currentWeather.put("locationId", weatherData.getLocationId());
            currentWeather.put("locationName", weatherData.getLocationName());
            currentWeather.put("temperature", weatherData.getTemperature());
            currentWeather.put("feelsLike", weatherData.getFeelsLike());
            currentWeather.put("humidity", weatherData.getHumidity());
            currentWeather.put("weatherCondition", weatherData.getWeatherCondition());
            currentWeather.put("weatherCode", weatherData.getWeatherCode());
            currentWeather.put("windDirection", weatherData.getWindDirection());
            currentWeather.put("windSpeed", weatherData.getWindSpeed());
            currentWeather.put("updateTime", weatherData.getUpdateTime());
            state.put("currentWeather", currentWeather);
            
            java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> history = 
                weatherService.get14DayWeatherData();
            org.json.JSONArray historyArray = new org.json.JSONArray();
            for (com.ntu.qidong.digitaltwin.model.WeatherHistory h : history) {
                org.json.JSONObject historyItem = new org.json.JSONObject();
                historyItem.put("date", h.getDate());
                historyItem.put("tempAvg", h.getTempAvg());
                historyItem.put("tempMax", h.getTempMax());
                historyItem.put("tempMin", h.getTempMin());
                historyItem.put("humidity", h.getHumidity());
                historyItem.put("weatherCondition", h.getWeatherCondition());
                historyArray.put(historyItem);
            }
            state.put("weatherHistory", historyArray);
            
            return state.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取应用状态失败: " + e.getMessage());
            return "{\"error\":\"Failed to get app state\"}";
        }
    }

    /**
     * 刷新天气数据（前端主动请求刷新）
     * 前端调用: window.AndroidBridge.refreshWeather()
     *
     * @return JSON 格式的操作结果
     */
    @JavascriptInterface
    public String refreshWeather() {
        Log.d(TAG, "前端请求刷新天气数据");
        weatherService.refreshWeather();
        return "{\"success\":true,\"message\":\"Weather refresh requested\"}";
    }

    /**
     * 获取访问统计数据
     * 前端调用: window.AndroidBridge.getVisitStats()
     *
     * @return JSON 格式的访问统计
     */
    @JavascriptInterface
    public String getVisitStats() {
        try {
            com.ntu.qidong.digitaltwin.db.AppVisitDAO visitDAO = 
                new com.ntu.qidong.digitaltwin.db.AppVisitDAO(context);
            
            int totalVisits = visitDAO.getTotalVisitCount();
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
            int todayVisits = visitDAO.getTodayVisitCount(today);
            
            org.json.JSONObject stats = new org.json.JSONObject();
            stats.put("totalVisits", totalVisits);
            stats.put("todayVisits", todayVisits);
            stats.put("today", today);
            
            return stats.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取访问统计失败: " + e.getMessage());
            return "{\"error\":\"Failed to get visit stats\"}";
        }
    }
}
