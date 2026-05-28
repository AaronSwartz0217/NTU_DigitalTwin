package com.ntu.qidong.digitaltwin.network;

import com.ntu.qidong.digitaltwin.model.WeatherData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 和风天气 API 服务类
 * 负责从和风天气 API 获取实时天气数据
 */
public class WeatherApiService {

    // 和风天气 API 配置
    private static final String API_KEY = "4bce3a4f452b46efbab516801d6a24b7";
    private static final String LOCATION_ID = "101190503"; // 启东市
    private static final String BASE_URL_NOW = "https://pw3yftqgrn.re.qweatherapi.com/v7/weather/now";
    private static final String BASE_URL_FORECAST = "https://pw3yftqgrn.re.qweatherapi.com/v7/weather/7d";

    // OkHttp 客户端配置
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 10;
    private static final int WRITE_TIMEOUT = 10;

    private final OkHttpClient httpClient;
    private final SimpleDateFormat dateFormat;

    // 单例实例
    private static WeatherApiService instance;

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String errorMessage);
    }

    private WeatherApiService() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    }

    /**
     * 获取 WeatherApiService 单例实例
     */
    public static synchronized WeatherApiService getInstance() {
        if (instance == null) {
            instance = new WeatherApiService();
        }
        return instance;
    }

    /**
     * 获取实时天气数据
     *
     * @param callback 回调接口
     */
    public void getCurrentWeather(WeatherCallback callback) {
        String url = BASE_URL_NOW + "?key=" + API_KEY + "&location=" + LOCATION_ID;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response == null || !response.isSuccessful()) {
                    if (callback != null) {
                        String errorMsg = "服务器响应异常：" + response;
                        if (response != null && response.code() == 403) {
                            errorMsg += " (API Key 无效或权限不足，请检查和风天气控制台配置)";
                        }
                        callback.onError(errorMsg);
                    }
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    WeatherData weatherData = parseWeatherResponse(responseBody);
                    if (callback != null) {
                        callback.onSuccess(weatherData);
                    }
                } catch (JSONException e) {
                    if (callback != null) {
                        callback.onError("数据解析异常：" + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 解析天气 API 响应数据
     *
     * @param jsonResponse JSON 响应字符串
     * @return WeatherData 对象
     * @throws JSONException JSON 解析异常
     */
    private WeatherData parseWeatherResponse(String jsonResponse) throws JSONException {
        JSONObject root = new JSONObject(jsonResponse);

        // 检查 API 返回状态
        if (!"200".equals(root.optString("code"))) {
            throw new JSONException("API 返回错误码: " + root.optString("code"));
        }

        JSONObject now = root.getJSONObject("now");
        JSONObject location = root.optJSONObject("location");
        
        // 如果 location 不存在，使用默认值
        String locationId = (location != null) ? location.optString("id") : LOCATION_ID;
        String locationName = (location != null) ? location.optString("name") : "启东市";

        WeatherData weatherData = new WeatherData();
        weatherData.setLocationId(locationId);
        weatherData.setLocationName(locationName);
        weatherData.setTemperature(now.optString("temp"));
        weatherData.setFeelsLike(now.optString("feelsLike"));
        weatherData.setHumidity(parseIntOrDefault(now.optString("humidity"), 0));
        weatherData.setWeatherCondition(now.optString("text"));
        weatherData.setWeatherCode(now.optString("icon"));
        weatherData.setWindDirection(now.optString("windDir"));
        weatherData.setWindSpeed(now.optString("windSpeed"));
        weatherData.setUpdateTime(System.currentTimeMillis());

        return weatherData;
    }

    /**
     * 安全解析整数
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取7天天气预报数据
     *
     * @param callback 回调接口
     */
    public void get7DayForecast(WeatherCallback callback) {
        String url = BASE_URL_FORECAST + "?key=" + API_KEY + "&location=" + LOCATION_ID;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (callback != null) {
                    callback.onError("预报数据请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response == null || !response.isSuccessful()) {
                    if (callback != null) {
                        String errorMsg = "预报数据响应异常：" + response;
                        if (response != null && response.code() == 403) {
                            errorMsg += " (API Key 无效或权限不足)";
                        }
                        callback.onError(errorMsg);
                    }
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    WeatherData weatherData = parseForecastResponse(responseBody);
                    if (callback != null) {
                        callback.onSuccess(weatherData);
                    }
                } catch (JSONException e) {
                    if (callback != null) {
                        callback.onError("预报数据解析异常：" + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 解析7天预报 API 响应数据
     */
    private WeatherData parseForecastResponse(String jsonResponse) throws JSONException {
        JSONObject root = new JSONObject(jsonResponse);

        if (!"200".equals(root.optString("code"))) {
            throw new JSONException("API 返回错误码: " + root.optString("code"));
        }

        JSONArray daily = root.optJSONArray("daily");
        if (daily != null && daily.length() > 0) {
            JSONObject firstDay = daily.getJSONObject(0);
            WeatherData weatherData = new WeatherData();
            weatherData.setLocationId(LOCATION_ID);
            weatherData.setLocationName("启东市");
            weatherData.setTemperature(firstDay.optString("tempMax"));
            weatherData.setFeelsLike(firstDay.optString("tempMin"));
            weatherData.setHumidity(parseIntOrDefault(firstDay.optString("humidity"), 0));
            weatherData.setWeatherCondition(firstDay.optString("textDay"));
            weatherData.setWeatherCode(firstDay.optString("iconDay"));
            weatherData.setWindDirection(firstDay.optString("windDirDay"));
            weatherData.setWindSpeed(firstDay.optString("windSpeedDay"));
            weatherData.setUpdateTime(System.currentTimeMillis());
            return weatherData;
        }

        throw new JSONException("预报数据为空");
    }

    /**
     * 获取当前日期字符串
     */
    public String getCurrentDateString() {
        return dateFormat.format(new Date());
    }
}
