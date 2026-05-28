# WebView 悬浮UI 前端集成指南

## 概述

本项目采用 **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板** 的架构模式。前端网页作为无状态可视化渲染器，负责 Three.js 场景、GLTF 模型加载、CSS2D 标签等展示，Android 原生层提供数据支持、业务逻辑和原生UI组件。

## 架构特点

- **原生主导**: 所有业务逻辑、数据处理、状态管理 100% 在原生层实现
- **WebView 背景**: 前端作为无状态可视化渲染器，全屏显示数字孪生模型
- **原生UI悬浮**: 天气信息、统计图表等以原生悬浮面板形式覆盖在 WebView 之上
- **JSBridge 通信**: 通过标准化 JSON 接口进行双向通信

## 全局配置

应用启动时会自动注入全局配置对象：

```javascript
window.APP_CONFIG = {
  appName: "NTU Qidong Digital Twin",
  locationId: "101190503",
  locationName: "启东市",
  platform: "android",
  version: "1.0.2",
  uiMode: "webview-overlay",
  features: {
    weather: true,
    statistics: true,
    buildingQuery: true
  }
}
```

## JSBridge 接口

### 1. 获取完整应用状态

**接口**: `window.AndroidBridge.getAppState()`

**返回数据结构**:
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
      "tempMax": 28.0,
      "tempMin": 20.0,
      "humidity": 60,
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

**使用示例**:
```javascript
const appState = JSON.parse(window.AndroidBridge.getAppState());
console.log('当前天气:', appState.currentWeather);
console.log('访问统计:', appState.stats);
```

### 2. 获取当前天气数据

**接口**: `window.WeatherBridge.getCurrentWeather()`

**返回数据结构**:
```json
{
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
}
```

### 3. 获取天气历史数据

**接口**: `window.WeatherBridge.getWeatherHistory(days)`

**参数**: `days` - 查询天数（1-30）

**返回数据结构**:
```json
[
  {
    "date": "2024-05-20",
    "tempAvg": 23.5,
    "tempMax": 28.0,
    "tempMin": 19.0,
    "humidity": 65,
    "weatherCondition": "多云"
  }
]
```

### 4. 获取应用版本信息

**接口**: `window.AndroidBridge.getAppVersion()`

**返回数据结构**:
```json
{
  "versionName": "1.0.2",
  "versionCode": 3
}
```

### 5. 手动刷新天气数据

**接口**: `window.AndroidBridge.refreshWeather()`

**返回数据结构**:
```json
{
  "success": true,
  "message": "Weather refresh requested"
}
```

### 6. 接收实时天气更新

**接口**: `window.updateWeather(weatherData)`

**说明**: 原生层会自动调用此方法向前端推送天气更新

**使用示例**:
```javascript
window.updateWeather = function(weatherData) {
  console.log('收到天气更新:', weatherData);
  updateWeatherUI(weatherData);
};
```

### 7. 建筑点击事件

**接口**: `window.AndroidBridge.showBuildingInfo(buildingId)`

**参数**: `buildingId` - 建筑ID

**返回数据结构**:
```json
{
  "success": true,
  "buildingId": "building_001"
}
```

### 8. 上报事件日志

**接口**: `window.AndroidBridge.logEvent(eventType, eventData)`

**参数**: 
- `eventType` - 事件类型
- `eventData` - 事件数据（JSON字符串）

**返回数据结构**:
```json
{
  "success": true
}
```

## 前端UI组件建议

### 1. 数字孪生场景渲染

**位置**: 全屏背景
**功能**: 
- Three.js 3D场景渲染
- GLTF 模型加载
- CSS2D 标签系统
- 建筑交互事件绑定

### 2. 建筑点击交互

```javascript
document.querySelectorAll('.building-mesh').forEach(building => {
  building.addEventListener('click', function() {
    const buildingId = this.dataset.id;
    window.AndroidBridge.showBuildingInfo(buildingId);
  });
});
```

## 数据更新机制

### 手动刷新模式

当前应用采用手动刷新模式，天气数据不会自动更新：

```javascript
function refreshAllData() {
  window.AndroidBridge.refreshWeather();
  setTimeout(() => {
    const appState = JSON.parse(window.AndroidBridge.getAppState());
    updateAllUI(appState);
  }, 1000);
}
```

### 实时监听

```javascript
window.updateWeather = function(weatherData) {
  updateWeatherUI(weatherData);
};

function updateWeatherUI(data) {
  console.log('天气数据已更新:', data);
}
```

## 样式建议

### 数字孪生场景容器

```css
#scene-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #0a1a2a;
  overflow: hidden;
}
```

## 性能优化建议

1. **数据缓存**: 前端缓存获取的数据，避免频繁调用JSBridge
2. **防抖处理**: 刷新操作添加防抖，避免重复请求
3. **懒加载**: 非关键资源按需加载
4. **动画优化**: 使用requestAnimationFrame进行动画更新

## 调试技巧

### 查看注入的配置

```javascript
console.log('应用配置:', window.APP_CONFIG);
```

### 测试JSBridge接口

```javascript
console.log('应用状态:', window.AndroidBridge.getAppState());
console.log('当前天气:', window.WeatherBridge.getCurrentWeather());
```

### 监控数据更新

```javascript
let lastUpdateTime = 0;
window.updateWeather = function(data) {
  const now = Date.now();
  console.log(`天气更新: ${(now - lastUpdateTime) / 1000}s 间隔`);
  lastUpdateTime = now;
};
```

## 接口可用性检测

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

## 错误处理

```javascript
function safeGetAppState() {
  try {
    return JSON.parse(window.AndroidBridge.getAppState());
  } catch (error) {
    console.error('获取应用状态失败:', error);
    return null;
  }
}
```

## 完整示例

```html
<!DOCTYPE html>
<html>
<head>
  <style>
    body {
      margin: 0;
      padding: 0;
      overflow: hidden;
      background: #0a1a2a;
    }
    
    #info-panel {
      position: fixed;
      top: 20px;
      left: 20px;
      background: rgba(13, 42, 63, 0.9);
      border: 1px solid rgba(0, 255, 255, 0.3);
      border-radius: 12px;
      padding: 15px;
      color: #ffffff;
      z-index: 1000;
      font-family: 'Arial', sans-serif;
    }
    
    .temperature {
      font-size: 24px;
      font-weight: bold;
      color: #00ffff;
    }
  </style>
</head>
<body>
  <div id="scene-container"></div>
  <div id="info-panel">
    <div class="temperature" id="temperature">--°C</div>
    <div id="condition">加载中...</div>
  </div>
  
  <script>
    window.updateWeather = function(data) {
      document.getElementById('temperature').textContent = data.temperature + '°C';
      document.getElementById('condition').textContent = data.weatherCondition;
    };
    
    function init() {
      if (checkJSBridgeAvailability()) {
        const appState = JSON.parse(window.AndroidBridge.getAppState());
        window.updateWeather(appState.currentWeather);
      }
    }
    
    function checkJSBridgeAvailability() {
      return typeof window.AndroidBridge !== 'undefined';
    }
    
    window.onload = init;
  </script>
</body>
</html>
```

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-05-26 | 初始版本 |
| 1.0.1 | 2024-05-27 | 更新架构为原生主导模式 |
| 1.0.2 | 2024-05-27 | 添加建筑查询功能支持，改为手动刷新模式 |
| 1.0.3 | 2024-05-27 | 启动页面优化：添加标题文字、强制横屏、自定义应用图标 |

## 总结

通过本集成指南，前端开发者可以：

1. **了解架构**: 理解Android原生主导 + WebView背景的架构模式
2. **使用接口**: 通过JSBridge接口与原生层进行数据交互
3. **实现交互**: 绑定建筑点击事件，触发原生响应
4. **接收更新**: 监听天气数据更新事件

前端仅负责数字孪生模型的可视化渲染，所有业务逻辑由Android原生层处理。