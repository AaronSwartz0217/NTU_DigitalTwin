package com.ntu.qidong.digitaltwin.model;

/**
 * 每日天气历史数据模型
 * 用于 SQLite weather_history 表的存储
 */
public class WeatherHistory {
    private long id;
    private String date;           // 日期 (格式: yyyy-MM-dd)
    private double tempAvg;         // 日平均气温
    private double tempMax;         // 日最高气温
    private double tempMin;         // 日最低气温
    private int humidity;           // 湿度百分比
    private String weatherCondition; // 天气状况
    private long createdAt;         // 创建时间戳

    public WeatherHistory() {
    }

    public WeatherHistory(String date, double tempAvg, double tempMax,
                          double tempMin, int humidity, String weatherCondition, long createdAt) {
        this.date = date;
        this.tempAvg = tempAvg;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.humidity = humidity;
        this.weatherCondition = weatherCondition;
        this.createdAt = createdAt;
    }

    // Getter 方法
    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public double getTempAvg() {
        return tempAvg;
    }

    public double getTempMax() {
        return tempMax;
    }

    public double getTempMin() {
        return tempMin;
    }

    public int getHumidity() {
        return humidity;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setter 方法
    public void setId(long id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTempAvg(double tempAvg) {
        this.tempAvg = tempAvg;
    }

    public void setTempMax(double tempMax) {
        this.tempMax = tempMax;
    }

    public void setTempMin(double tempMin) {
        this.tempMin = tempMin;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "WeatherHistory{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", tempAvg=" + tempAvg +
                ", tempMax=" + tempMax +
                ", tempMin=" + tempMin +
                ", humidity=" + humidity +
                ", weatherCondition='" + weatherCondition + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
