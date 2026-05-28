# 南通大学启东校区数字孪生应用 - 安卓本体

## 项目简介

本项目是南通大学启东校区移动端数字孪生应用的第一阶段安卓本体，采用 **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板** 的架构模式。

### 架构特点

- **原生主导**: 所有业务逻辑、数据处理、状态管理 100% 在原生层实现
- **WebView 背景**: 前端作为无状态可视化渲染器，负责 Three.js 场景、GLTF 模型加载、CSS2D 标签等展示
- **原生UI悬浮**: 天气信息、统计图表等以悬浮面板形式覆盖在 WebView 之上
- **JSBridge 通信**: 原生与前端通过标准化 JSON 接口进行双向通信

---

## 技术栈

| 组件 | 版本 |
|------|------|
| 开发语言 | Java |
| 目标 SDK | Android 34 |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 |
| 数据库 | SQLite |
| 图表库 | MPAndroidChart v3.1.0 |
| 定时任务 | Handler |

---

## 项目结构

```
NTU_Qidong_DigitalTwin/
├── app/
│   ├── src/main/
│   │   ├── java/com/ntu/qidong/digitaltwin/
│   │   │   ├── NTUQidongApp.java          # Application 类
│   │   │   ├── bridge/                     # JSBridge 接口
│   │   │   │   ├── JSBridgeInterface.java
│   │   │   │   ├── JSBridgeImpl.java
│   │   │   │   └── JavaScriptInjector.java
│   │   │   ├── db/                         # 数据库层
│   │   │   │   ├── DatabaseHelper.java
│   │   │   │   ├── WeatherHistoryDAO.java
│   │   │   │   └── AppVisitDAO.java
│   │   │   ├── model/                     # 数据模型
│   │   │   │   ├── WeatherData.java
│   │   │   │   ├── WeatherHistory.java
│   │   │   │   └── AppVisit.java
│   │   │   ├── network/                   # 网络层
│   │   │   │   └── WeatherApiService.java
│   │   │   ├── service/                   # 服务层
│   │   │   │   └── WeatherService.java
│   │   │   ├── ui/                        # 界面层
│   │   │   │   ├── MainActivity.java
│   │   │   │   └── StatisticsActivity.java
│   │   │   └── utils/                     # 工具类
│   │   │       ├── DateUtils.java
│   │   │       └── NetworkUtils.java
│   │   ├── res/                           # 资源文件
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   ├── drawable/
│   │   │   └── xml/network_security_config.xml
│   │   ├── assets/
│   │   │   └── placeholder.html           # 本地占位页面
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── docs/                                   # 文档
│   ├── JSBridge_接口文档.md
│   ├── 数据库设计文档.md
│   ├── 功能测试报告.md
│   └── WebView悬浮UI前端集成指南.md
├── SPEC.md                                # 规格说明书
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 核心功能

### 1. 天气服务
- 和风天气 API 集成 (启东市: 101190503)
- **手动刷新模式** (点击刷新按钮触发)
- 错误自动重试 (最多 3 次)

### 2. 数据库
- `weather_history`: 气温历史数据存储
- `app_visit`: 应用访问量记录
- DAO 层完整实现，查询性能 < 10ms

### 3. JSBridge 接口
- `window.updateWeather`: 原生 → 前端注入天气数据
- `window.WeatherBridge.getWeatherHistory`: 前端 → 原生查询历史
- `window.AndroidBridge.showBuildingInfo`: 建筑点击事件
- `window.AndroidBridge.getAppState`: 获取完整应用状态

### 4. WebView 全屏背景
- 硬件加速启用
- 完整生命周期管理
- 错误处理与友好提示
- 缓存策略优化

### 5. 原生UI悬浮面板
- **左侧面板**: 天气信息卡片、数据概览、建筑查询搜索
- **右侧面板**: 温度趋势图表、历史记录列表
- **底部控制**: 刷新按钮、显示/隐藏侧边栏按钮

### 6. 建筑查询功能
- 搜索栏输入建筑名称
- 分类选择：宿舍 / 教学
- 搜索按钮触发

### 7. 启动页面（闪屏页）
- 全屏显示启动图片（`startimage.png`）
- 顶部居中显示"启东校区可视化"标题（青色 #00ffff，24sp）
- 半透明黑色背景框（#99000000）
- 强制横屏模式
- 2秒后自动跳转到主页面

---

## 布局架构

```
┌─────────────────────────────────────────────────────────────┐
│  WebView (全屏背景 - GitHub Pages 数字孪生模型)              │
├────────────┬───────────────────────────────┬───────────────┤
│   左侧面板 │                               │   右侧面板     │
│  (悬浮层) │                               │   (悬浮层)     │
│  ├─天气信息│                               │  ├─温度图表    │
│  ├─数据概览│                               │  └─历史记录    │
│  └─建筑查询│                               │                │
├────────────┴───────────────────────────────┴───────────────┤
│              [刷新]         [隐藏/显示]                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 构建说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- JDK 17

### 构建步骤

1. **使用 Android Studio 打开项目**
   ```
   File → Open → 选择项目根目录
   ```

2. **等待 Gradle 同步完成**
   - Android Studio 会自动下载 Gradle Wrapper 和所有依赖
   - 首次同步可能需要较长时间（下载依赖）

3. **配置和风天气 API Key**
   编辑 `app/src/main/java/com/ntu/qidong/digitaltwin/network/WeatherApiService.java`
   ```java
   private static final String API_KEY = "YOUR_API_KEY_HERE";
   ```

4. **构建 Debug APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

5. **安装到设备**
   ```
   Run → Run 'app'
   ```

### 构建问题处理

如果遇到 Gradle 版本兼容性问题：

1. 打开 `gradle/wrapper/gradle-wrapper.properties`
2. 确保 `distributionUrl` 指向兼容的 Gradle 版本：
   ```
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
   ```

### 前端集成

当前 WebView 加载 GitHub Pages 页面：
- URL: `https://aaronswartz0217.github.io/NTU_Building/`

前端按照 `docs/JSBridge_接口文档.md` 实现接口调用即可完成集成。

---

## 性能指标

| 指标 | 目标 |
|------|------|
| 内存占用 | ≤ 300MB |
| 数据库查询 | < 10ms |
| 图表加载 | < 200ms |
| 冷启动 | < 2s |
| APK 大小 | ~13MB |

---

## 注意事项

1. **API Key 安全**: 当前 API Key 直接写在代码中，实际部署时应使用加密存储或后端代理
2. **网络权限**: 应用需要 INTERNET 和 ACCESS_NETWORK_STATE 权限
3. **最低版本**: 应用最低支持 Android 10 (API 29)
4. **网络安全配置**: 已配置允许 GitHub Pages 和和风天气 API 的 HTTPS 连接

---

## 文档

- [规格说明书](SPEC.md)
- [JSBridge 接口文档](docs/JSBridge_接口文档.md)
- [数据库设计文档](docs/数据库设计文档.md)
- [功能测试报告](docs/功能测试报告.md)
- [WebView悬浮UI前端集成指南](docs/WebView悬浮UI前端集成指南.md)

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-05-26 | 初始版本，第一阶段安卓本体 |
| 1.0.1 | 2026-05-27 | UI架构优化：WebView全屏背景 + 原生悬浮面板 |
| 1.0.2 | 2026-05-27 | 添加建筑查询功能，修复性能问题，改为手动刷新 |
| 1.0.3 | 2026-05-27 | 启动页面优化：添加标题文字、强制横屏、自定义应用图标 |