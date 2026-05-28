package com.ntu.qidong.digitaltwin.bridge;

/**
 * JSBridge 接口契约
 * 定义原生 Android 与 WebView 前端之间的双向通信接口
 *
 * 接口设计原则:
 * 1. 原生主导: 所有接口由安卓端定义，前端仅作为无状态可视化渲染器
 * 2. 标准化: 统一使用 JSON 格式进行数据交换
 * 3. 异步化: 所有交互均为异步执行，避免阻塞 UI
 *
 * 前端调用原生的接口前缀: AndroidBridge
 * 原生向前端注入数据的接口前缀: window.updateWeather / window.WeatherBridge
 */
public interface JSBridgeInterface {

    /**
     * 获取天气历史数据
     * 前端通过 window.WeatherBridge.getWeatherHistory(days) 调用
     *
     * @param days 查询的天数
     * @return JSON 格式的天气历史数据
     */
    String getWeatherHistory(int days);

    /**
     * 获取最新天气数据
     * 前端通过 window.WeatherBridge.getCurrentWeather() 调用
     *
     * @return JSON 格式的当前天气数据
     */
    String getCurrentWeather();

    /**
     * 建筑点击事件
     * 前端通过 window.AndroidBridge.showBuildingInfo(buildingId) 调用
     *
     * @param buildingId 建筑 ID
     * @return 操作结果
     */
    String showBuildingInfo(String buildingId);

    /**
     * 获取应用版本信息
     * 前端通过 window.AndroidBridge.getAppVersion() 调用
     *
     * @return JSON 格式的版本信息
     */
    String getAppVersion();

    /**
     * 记录前端事件
     * 前端通过 window.AndroidBridge.logEvent(eventType, eventData) 调用
     *
     * @param eventType 事件类型
     * @param eventData 事件数据 (JSON 格式)
     * @return 操作结果
     */
    String logEvent(String eventType, String eventData);
}
