# JSBridge 接口文档

## 概述

本文档定义南通大学启东校区数字孪生应用中 Android 原生层与 WebView 前端之间的双向通信接口。

**设计原则**:
- 原生主导: 所有接口由安卓端定义，前端仅作为无状态可视化渲染器
- 标准化: 统一使用 JSON 格式进行数据交换
- 异步化: 所有交互均为异步执行，避免阻塞 UI

---

## 接口命名规范

| 前缀 | 说明 |
|------|------|
| `window.updateWeather` | 原生向前端注入实时天气数据 |
| `window.WeatherBridge` | 前端调用原生的天气相关接口 |
| `window.AndroidBridge` | 前端调用原生的通用接口 |
| `window.APP_CONFIG` | 原生注入的全局配置信息 |

---

## 1. window.updateWeather (原生 → 前端)

### 功能说明
原生层主动向前端页面注入实时天气数据。

### 调用时机
- WebView 页面加载完成后
- 天气数据刷新成功后

### 参数说明
```javascript
window.updateWeather(weatherData)
```

**weatherData 参数结构**:
```json
{
  "locationId": "101190503",      // String: 位置ID (启东市)
  "locationName": "启东市",         // String: 位置名称
  "temperature": "25",             // String: 实时气温 (℃)
  "feelsLike": "27",               // String: 体感温度 (℃)
  "humidity": 65,                  // Number: 相对湿度 (%)
  "weatherCondition": "多云",       // String: 天气状况
  "weatherCode": "101",            // String: 天气代码
  "windDirection": "东南风",         // String: 风向
  "windSpeed": "12",               // String: 风速 (km/h)
  "updateTime": 1716700800000      // Number: 更新时间戳
}
```

### 前端示例代码
```javascript
// 定义接收函数
window.updateWeather = function(weatherData) {
  console.log('收到天气数据:', weatherData);
  // 更新页面显示
  document.getElementById('temperature').textContent = weatherData.temperature + '°C';
  document.getElementById('condition').textContent = weatherData.weatherCondition;
};
```

---

## 2. WeatherBridge.getWeatherHistory (前端 → 原生)

### 功能说明
前端查询天气历史数据。

### 接口签名
```javascript
window.WeatherBridge.getWeatherHistory(days: number): string
```

### 参数说明
| 参数 | 类型 | 说明 |
|------|------|------|
| days | number | 查询的天数 (建议 7-30) |

### 返回值
JSON 格式的天气历史数据数组:
```json
[
  {
    "date": "2024-05-20",
    "tempAvg": 23.5,
    "tempMax": 28.0,
    "tempMin": 19.0,
    "humidity": 65,
    "weatherCondition": "多云"
  },
  ...
]
```

### 前端示例代码
```javascript
const history = window.WeatherBridge.getWeatherHistory(7);
const historyData = JSON.parse(history);
console.log('近7天天气:', historyData);
```

---

## 3. WeatherBridge.getCurrentWeather (前端 → 原生)

### 功能说明
前端获取当前缓存的天气数据。

### 接口签名
```javascript
window.WeatherBridge.getCurrentWeather(): string
```

### 返回值
JSON 格式的当前天气数据 (同 window.updateWeather 参数结构)。

### 前端示例代码
```javascript
const current = window.WeatherBridge.getCurrentWeather();
const weather = JSON.parse(current);
console.log('当前天气:', weather);
```

---

## 4. AndroidBridge.showBuildingInfo (前端 → 原生)

### 功能说明
前端触发建筑点击事件，原生层处理建筑信息展示。

### 接口签名
```javascript
window.AndroidBridge.showBuildingInfo(buildingId: string): string
```

### 参数说明
| 参数 | 类型 | 说明 |
|------|------|------|
| buildingId | string | 建筑唯一标识符 |

### 返回值
JSON 格式的操作结果:
```json
{
  "success": true,
  "buildingId": "building_a"
}
```

### 前端示例代码
```javascript
// 建筑点击事件处理
document.querySelectorAll('.building').forEach(building => {
  building.addEventListener('click', function() {
    const buildingId = this.dataset.id;
    const result = window.AndroidBridge.showBuildingInfo(buildingId);
    console.log('建筑点击结果:', result);
  });
});
```

---

## 5. AndroidBridge.getAppVersion (前端 → 原生)

### 功能说明
前端获取应用版本信息。

### 接口签名
```javascript
window.AndroidBridge.getAppVersion(): string
```

### 返回值
JSON 格式的版本信息:
```json
{
  "versionName": "1.0.2",
  "versionCode": 3
}
```

### 前端示例代码
```javascript
const version = window.AndroidBridge.getAppVersion();
const appInfo = JSON.parse(version);
console.log('应用版本:', appInfo.versionName);
```

---

## 6. AndroidBridge.logEvent (前端 → 原生)

### 功能说明
前端向原生层上报事件日志。

### 接口签名
```javascript
window.AndroidBridge.logEvent(eventType: string, eventData: string): string
```

### 参数说明
| 参数 | 类型 | 说明 |
|------|------|------|
| eventType | string | 事件类型 |
| eventData | string | 事件数据 (JSON 格式字符串) |

### 返回值
JSON 格式的操作结果:
```json
{
  "success": true
}
```

### 前端示例代码
```javascript
window.AndroidBridge.logEvent('page_view', JSON.stringify({
  page: 'main',
  timestamp: Date.now()
}));
```

---

## 7. AndroidBridge.getAppState (前端 → 原生)

### 功能说明
前端获取完整的应用状态，包括当前天气、历史数据、统计信息等。

### 接口签名
```javascript
window.AndroidBridge.getAppState(): string
```

### 返回值
JSON 格式的完整应用状态:
```json
{
  "currentWeather": {
    "locationId": "101190503",
    "locationName": "启东市",
    "temperature": "25",
    "feelsLike": "27",
    "humidity": 65,
    "weatherCondition": "多云",
    "weatherCode": "101",
    "windDirection": "东南风",
    "windSpeed": "12",
    "updateTime": 1716700800000
  },
  "stats": {
    "totalVisits": 1250,
    "todayVisits": 15,
    "averageTemperature": 23.5
  },
  "weatherHistory": [
    {
      "date": "2024-05-26",
      "tempAvg": 24.0,
      "weatherCondition": "晴"
    },
    {
      "date": "2024-05-25",
      "tempAvg": 22.5,
      "weatherCondition": "多云"
    }
  ]
}
```

### 前端示例代码
```javascript
const appState = window.AndroidBridge.getAppState();
const state = JSON.parse(appState);
console.log('应用状态:', state);
```

---

## 8. AndroidBridge.refreshWeather (前端 → 原生)

### 功能说明
前端请求手动刷新天气数据。

### 接口签名
```javascript
window.AndroidBridge.refreshWeather(): string
```

### 返回值
JSON 格式的操作结果:
```json
{
  "success": true,
  "message": "Weather refresh requested"
}
```

### 前端示例代码
```javascript
// 刷新按钮点击事件
document.getElementById('refresh-btn').addEventListener('click', function() {
  const result = window.AndroidBridge.refreshWeather();
  console.log('刷新请求结果:', result);
});
```

---

## 9. window.APP_CONFIG (原生 → 前端)

### 功能说明
原生向前端注入全局配置信息。

### 数据结构
```json
{
  "appName": "NTU Qidong Digital Twin",
  "version": "1.0.2",
  "uiMode": "webview-overlay",
  "features": {
    "weather": true,
    "statistics": true,
    "buildingQuery": true
  },
  "location": {
    "id": "101190503",
    "name": "启东市"
  }
}
```

### 前端示例代码
```javascript
// 读取全局配置
console.log('应用配置:', window.APP_CONFIG);
console.log('UI模式:', window.APP_CONFIG.uiMode);
```

---

## 接口可用性检测

前端页面应在使用接口前进行可用性检测:

```javascript
function checkJSBridgeAvailability() {
  if (typeof window.WeatherBridge !== 'undefined' &&
      typeof window.AndroidBridge !== 'undefined') {
    console.log('JSBridge 接口可用');
    return true;
  } else {
    console.warn('JSBridge 接口不可用');
    return false;
  }
}
```

---

## 错误处理

所有接口调用应包含错误处理:

```javascript
try {
  const result = window.WeatherBridge.getWeatherHistory(7);
  if (result) {
    const data = JSON.parse(result);
    // 处理数据
  }
} catch (e) {
  console.error('接口调用失败:', e);
}
```

---

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-05-26 | 初始版本 |
| 1.0.1 | 2024-05-27 | 添加 getAppState 和 refreshWeather 接口 |
| 1.0.2 | 2024-05-27 | 添加 APP_CONFIG 全局配置注入 |
| 1.0.3 | 2024-05-27 | 启动页面优化：添加标题文字、强制横屏、自定义应用图标 |