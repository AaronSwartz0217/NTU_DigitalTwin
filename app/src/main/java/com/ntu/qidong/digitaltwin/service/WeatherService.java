package com.ntu.qidong.digitaltwin.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO;
import com.ntu.qidong.digitaltwin.model.WeatherData;
import com.ntu.qidong.digitaltwin.model.WeatherHistory;
import com.ntu.qidong.digitaltwin.network.WeatherApiService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 天气服务类
 * 负责天气数据的获取、缓存、自动刷新和历史记录存储
 * 采用 Handler 实现 15 分钟自动刷新机制
 */
public class WeatherService {

    private static final String TAG = "WeatherService";
    private static final long REFRESH_INTERVAL = 15 * 60 * 1000; // 15 分钟
    private static final int MAX_RETRY_COUNT = 3;               // 最大重试次数
    private static final long RETRY_DELAY = 5 * 1000;           // 重试间隔 5 秒

    private final Context context;
    private final WeatherApiService apiService;
    private final WeatherHistoryDAO weatherHistoryDAO;
    private final Handler handler;
    private final SimpleDateFormat dateFormat;

    // 当前缓存的天气数据
    private WeatherData currentWeatherData;
    // 刷新回调
    private WeatherRefreshCallback refreshCallback;
    // 重试计数器
    private int retryCount = 0;

    public interface WeatherRefreshCallback {
        void onWeatherRefreshed(WeatherData weatherData);
        void onRefreshError(String errorMessage);
    }

    public WeatherService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = WeatherApiService.getInstance();
        this.weatherHistoryDAO = new WeatherHistoryDAO(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    }

    /**
     * 设置天气刷新回调
     */
    public void setRefreshCallback(WeatherRefreshCallback callback) {
        this.refreshCallback = callback;
    }

    /**
     * 获取当前缓存的天气数据
     */
    public WeatherData getCurrentWeatherData() {
        return currentWeatherData;
    }

    /**
     * 刷新天气数据（同时获取实时天气和7天预报，形成14天数据）
     * 支持自动重试机制
     */
    public void refreshWeather() {
        // 先获取实时天气
        apiService.getCurrentWeather(new WeatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                retryCount = 0;
                currentWeatherData = weatherData;

                // 保存实时天气到历史记录
                saveToHistory(weatherData);

                // 同步获取7天预报数据
                fetch7DayForecast();

                // 通知回调
                if (refreshCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        refreshCallback.onWeatherRefreshed(weatherData);
                    });
                }

                Log.d(TAG, "实时天气数据刷新成功");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "实时天气数据刷新失败: " + errorMessage);
                // 即使实时天气获取失败，仍尝试获取预报数据
                fetch7DayForecast();
                handleRefreshError(errorMessage);
            }
        });
    }

    /**
     * 获取7天天气预报数据并保存到历史记录
     */
    private void fetch7DayForecast() {
        apiService.get7DayForecast(new WeatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData forecastData) {
                // 保存预报数据到历史记录（会自动处理日期）
                saveForecastToHistory();
                Log.d(TAG, "7天预报数据获取成功");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "7天预报数据获取失败: " + errorMessage);
                // 预报获取失败不影响主流程
            }
        });
    }

    /**
     * 保存7天预报数据到历史记录
     */
    private void saveForecastToHistory() {
        try {
            // 获取当前日期
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String today = dateFormat.format(calendar.getTime());

            // 获取已有的历史记录
            List<WeatherHistory> existingHistory = weatherHistoryDAO.findRecentDays(7);
            
            // 创建未来7天的预报记录（模拟数据，实际应从API获取）
            for (int i = 1; i <= 7; i++) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
                String forecastDate = dateFormat.format(calendar.getTime());
                
                // 检查是否已存在该日期的记录
                boolean exists = false;
                for (WeatherHistory history : existingHistory) {
                    if (forecastDate.equals(history.getDate())) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    // 创建预报记录（使用当前天气数据作为基础）
                    double baseTemp = currentWeatherData != null 
                            ? parseDoubleOrDefault(currentWeatherData.getTemperature(), 20) 
                            : 20;
                    
                    // 添加一些温度变化模拟
                    double tempMax = baseTemp + (Math.random() * 4 - 2);
                    double tempMin = tempMax - 6 - Math.random() * 4;
                    
                    WeatherHistory forecast = new WeatherHistory(
                            forecastDate,
                            (tempMax + tempMin) / 2,  // 平均温度
                            tempMax,                   // 最高温度
                            tempMin,                   // 最低温度
                            currentWeatherData != null ? currentWeatherData.getHumidity() : 60,
                            getForecastCondition(i),   // 预报天气状况
                            System.currentTimeMillis()
                    );
                    
                    weatherHistoryDAO.insertOrUpdate(forecast);
                }
            }
            
            Log.d(TAG, "7天预报数据已保存");
            
        } catch (Exception e) {
            Log.e(TAG, "保存预报数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成模拟的预报天气状况
     */
    private String getForecastCondition(int dayOffset) {
        String[] conditions = {"晴", "多云", "阴", "小雨", "阵雨"};
        // 根据天数偏移生成不同的天气状况
        int index = (dayOffset + 2) % conditions.length;
        return conditions[index];
    }

    /**
     * 处理刷新错误，支持自动重试
     */
    private void handleRefreshError(String errorMessage) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "正在第 " + retryCount + " 次重试...");

            handler.postDelayed(() -> refreshWeather(), RETRY_DELAY);
        } else {
            retryCount = 0;
            if (refreshCallback != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    refreshCallback.onRefreshError(errorMessage);
                });
            }
        }
    }

    /**
     * 保存天气数据到历史记录
     */
    private void saveToHistory(WeatherData weatherData) {
        if (weatherData == null) {
            return;
        }

        try {
            String today = dateFormat.format(new Date());

            // 尝试解析温度值
            double temp = parseDoubleOrDefault(weatherData.getTemperature(), 0);
            double feelsLike = parseDoubleOrDefault(weatherData.getFeelsLike(), temp);

            // 创建历史记录
            WeatherHistory history = new WeatherHistory(
                    today,
                    feelsLike,           // 使用体感温度作为日平均
                    temp,                 // 日最高
                    temp - 3,             // 估算日最低 (实际应由API提供)
                    weatherData.getHumidity(),
                    weatherData.getWeatherCondition(),
                    System.currentTimeMillis()
            );

            // 保存到数据库
            weatherHistoryDAO.insertOrUpdate(history);
            Log.d(TAG, "天气历史记录已保存: " + today);

        } catch (Exception e) {
            Log.e(TAG, "保存天气历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 安全解析双精度浮点数
     */
    private double parseDoubleOrDefault(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * 获取最近 N 天的历史天气数据（包括历史记录和预报）
     */
    public List<WeatherHistory> getRecentWeatherHistory(int days) {
        // 如果请求天数超过7天，确保先刷新预报数据
        if (days > 7) {
            fetch7DayForecast();
        }
        return weatherHistoryDAO.findRecentDays(days);
    }
    
    /**
     * 获取完整的14天天气数据（7天历史 + 7天预报）
     */
    public List<WeatherHistory> get14DayWeatherData() {
        return weatherHistoryDAO.findRecentDays(14);
    }

    /**
     * 获取 JSON 格式的历史天气数据 (用于 JSBridge)
     */
    public String getWeatherHistoryJson(int days) {
        List<WeatherHistory> historyList = weatherHistoryDAO.findRecentDays(days);

        try {
            JSONArray jsonArray = new JSONArray();
            for (WeatherHistory history : historyList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("date", history.getDate());
                jsonObject.put("tempAvg", history.getTempAvg());
                jsonObject.put("tempMax", history.getTempMax());
                jsonObject.put("tempMin", history.getTempMin());
                jsonObject.put("humidity", history.getHumidity());
                jsonObject.put("weatherCondition", history.getWeatherCondition());
                jsonArray.put(jsonObject);
            }
            return jsonArray.toString();
        } catch (JSONException e) {
            Log.e(TAG, "历史天气数据转 JSON 失败: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * 启动天气服务（仅初始化，不启动自动刷新）
     * 刷新改为完全手动触发
     */
    public void startAutoRefresh() {
        // 立即刷新一次
        refreshWeather();
        Log.d(TAG, "天气服务已启动（手动刷新模式）");
    }

    /**
     * 停止自动刷新任务
     */
    public void stopAutoRefresh() {
        handler.removeCallbacks(refreshRunnable);
        Log.d(TAG, "自动刷新任务已停止");
    }

    /**
     * 自动刷新任务
     */
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
            // 继续安排下一次刷新
            handler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    /**
     * 将天气数据转换为 JSON 字符串 (用于 JSBridge)
     */
    public String weatherDataToJson(WeatherData data) {
        if (data == null) {
            return "{}";
        }

        try {
            JSONObject json = new JSONObject();
            json.put("locationId", data.getLocationId());
            json.put("locationName", data.getLocationName());
            json.put("temperature", data.getTemperature());
            json.put("feelsLike", data.getFeelsLike());
            json.put("humidity", data.getHumidity());
            json.put("weatherCondition", data.getWeatherCondition());
            json.put("weatherCode", data.getWeatherCode());
            json.put("windDirection", data.getWindDirection());
            json.put("windSpeed", data.getWindSpeed());
            json.put("updateTime", data.getUpdateTime());
            return json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "天气数据转 JSON 失败: " + e.getMessage());
            return "{}";
        }
    }
}
