package com.ntu.qidong.digitaltwin.bridge;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.ntu.qidong.digitaltwin.model.WeatherData;
import com.ntu.qidong.digitaltwin.service.WeatherService;

/**
 * WebView JavaScript 注入器
 * 负责向前端页面注入天气数据和其他原生方法
 */
public class JavaScriptInjector {

    private static final String TAG = "JavaScriptInjector";

    // JavaScript 接口对象名称
    public static final String JS_INTERFACE_WEATHER = "WeatherBridge";
    public static final String JS_INTERFACE_ANDROID = "AndroidBridge";

    private final Context context;
    private final WeatherService weatherService;
    private final WebView webView;

    public JavaScriptInjector(Context context, WeatherService weatherService, WebView webView) {
        this.context = context;
        this.weatherService = weatherService;
        this.webView = webView;
    }

    /**
     * 向前端页面注入 JavaScript 接口
     */
    public void injectJavaScriptInterfaces() {
        if (webView == null) {
            Log.e(TAG, "WebView 为空，无法注入 JavaScript 接口");
            return;
        }

        // 创建 JSBridge 实现
        JSBridgeImpl jsBridge = new JSBridgeImpl(context, weatherService);

        // 注入 WeatherBridge 接口 (供前端调用原生)
        webView.addJavascriptInterface(jsBridge, JS_INTERFACE_WEATHER);
        webView.addJavascriptInterface(jsBridge, JS_INTERFACE_ANDROID);

        Log.d(TAG, "JavaScript 接口注入完成");
    }

    /**
     * 向前端页面注入实时天气数据
     * 调用前端的 window.updateWeather 方法
     *
     * @param weatherData 天气数据
     */
    public void injectWeatherData(WeatherData weatherData) {
        if (webView == null) {
            Log.e(TAG, "WebView 为空，无法注入天气数据");
            return;
        }

        if (weatherData == null) {
            Log.w(TAG, "天气数据为空，跳过注入");
            return;
        }

        String weatherJson = weatherService.weatherDataToJson(weatherData);
        String jsCode = String.format("javascript:if(typeof window.updateWeather === 'function') { window.updateWeather(%s); }", weatherJson);

        webView.evaluateJavascript(jsCode, value -> {
            Log.d(TAG, "天气数据注入结果: " + value);
        });
    }

    /**
     * 注入自定义 JavaScript 代码
     *
     * @param jsCode JavaScript 代码
     */
    public void injectCustomScript(String jsCode) {
        if (webView == null) {
            Log.e(TAG, "WebView 为空，无法执行自定义脚本");
            return;
        }

        webView.evaluateJavascript(jsCode, value -> {
            Log.d(TAG, "自定义脚本执行结果: " + value);
        });
    }

    /**
     * 注入全局配置对象
     */
    public void injectGlobalConfig() {
        String configScript = String.format(
                "javascript:(function() { " +
                        "window.APP_CONFIG = { " +
                        "locationId: '%s', " +
                        "locationName: '%s', " +
                        "platform: 'android', " +
                        "version: '%s', " +
                        "uiMode: 'webview-overlay', " +
                        "features: { " +
                        "weather: true, " +
                        "statistics: true, " +
                        "history: true, " +
                        "chart: true " +
                        "} " +
                        "}; " +
                        "console.log('App config injected:', window.APP_CONFIG);" +
                        "})();",
                "101190503",
                "启东市",
                getAppVersion()
        );

        injectCustomScript(configScript);
    }

    /**
     * 获取应用版本号
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
