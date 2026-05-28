package com.ntu.qidong.digitaltwin.model;

/**
 * 天气数据模型
 * 用于存储从和风天气API获取的实时天气信息
 */
public class WeatherData {
    private String locationId;      //  location ID
    private String locationName;    //  location 名称
    private String temperature;      // 实时气温 (单位: ℃)
    private String feelsLike;       // 体感温度
    private int humidity;           // 相对湿度 (%)
    private String weatherCondition; // 天气状况
    private String weatherCode;     // 天气代码
    private String windDirection;   // 风向
    private String windSpeed;       // 风速
    private long updateTime;        // 更新时间戳

    public WeatherData() {
    }

    public WeatherData(String locationId, String locationName, String temperature,
                       String feelsLike, int humidity, String weatherCondition,
                       String weatherCode, String windDirection, String windSpeed, long updateTime) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.weatherCondition = weatherCondition;
        this.weatherCode = weatherCode;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.updateTime = updateTime;
    }

    // Getter 方法
    public String getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public String getWeatherCode() {
        return weatherCode;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    // Setter 方法
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public void setFeelsLike(String feelsLike) {
        this.feelsLike = feelsLike;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public void setWeatherCode(String weatherCode) {
        this.weatherCode = weatherCode;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "locationId='" + locationId + '\'' +
                ", locationName='" + locationName + '\'' +
                ", temperature='" + temperature + '\'' +
                ", feelsLike='" + feelsLike + '\'' +
                ", humidity=" + humidity +
                ", weatherCondition='" + weatherCondition + '\'' +
                ", weatherCode='" + weatherCode + '\'' +
                ", windDirection='" + windDirection + '\'' +
                ", windSpeed='" + windSpeed + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}
