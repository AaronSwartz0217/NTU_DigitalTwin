# 南通大学启东校区数字孪生应用 - 第一阶段安卓本体规格说明书

## 1. 项目概述

**项目名称**: NTU_Qidong_DigitalTwin
**项目类型**: Android 原生应用 + WebView 前端渲染
**核心架构**: **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板**

**项目背景**: 南通大学启东校区移动端数字孪生应用的第一阶段安卓本体开发，所有业务逻辑、数据处理、状态管理 100% 在原生层实现，前端仅作为无状态可视化渲染器，以全屏背景形式展示数字孪生模型，原生UI组件以悬浮面板形式覆盖在上方。

## 2. 技术栈规格

| 组件 | 技术规格 |
|------|---------|
| 开发语言 | Java |
| 目标 SDK | Android SDK 34 (Android 14) |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 |
| 数据库 | SQLite (Android 原生) |
| 图表库 | MPAndroidChart v3.1.0 |
| 定时任务 | Handler + Runnable |
| WebView | Android WebView (启用硬件加速) |

## 3. 功能模块规格

### 3.1 天气服务模块
- **数据源**: 和风天气 API (启东市 Location ID: 101190503)
- **数据内容**: 气温、体感温度、湿度、天气状况、风向、风速
- **刷新机制**: **手动刷新模式**（点击刷新按钮触发）
- **错误处理**: 失败自动重试 (最多 3 次，间隔 5 秒)
- **缓存策略**: 本地数据库缓存，启动时加载缓存数据

### 3.2 数据库模块
- **数据库名称**: ntu_qidong.db
- **表结构**:
  - `weather_history`: 存储每日气温数据
    - `id` (INTEGER PRIMARY KEY)
    - `date` (TEXT, 格式: yyyy-MM-dd)
    - `temp_avg` (REAL, 日平均气温)
    - `temp_max` (REAL, 日最高气温)
    - `temp_min` (REAL, 日最低气温)
    - `humidity` (INTEGER, 湿度百分比)
    - `weather_condition` (TEXT, 天气状况)
    - `created_at` (INTEGER, 时间戳)

  - `app_visit`: 记录应用启动访问量
    - `id` (INTEGER PRIMARY KEY)
    - `visit_date` (TEXT, 格式: yyyy-MM-dd)
    - `visit_count` (INTEGER, 访问次数)
    - `last_visit_time` (INTEGER, 最后访问时间戳)

### 3.3 JSBridge 接口规格

| 接口 | 方向 | 功能 |
|------|------|------|
| `window.updateWeather` | 原生 → 前端 | 注入实时天气数据 |
| `window.WeatherBridge.getWeatherHistory` | 前端 → 原生 | 查询历史天气数据 |
| `window.WeatherBridge.getCurrentWeather` | 前端 → 原生 | 获取当前天气数据 |
| `window.AndroidBridge.showBuildingInfo` | 前端 → 原生 | 建筑点击事件 |
| `window.AndroidBridge.getAppState` | 前端 → 原生 | 获取完整应用状态 |
| `window.AndroidBridge.getAppVersion` | 前端 → 原生 | 获取应用版本信息 |
| `window.AndroidBridge.logEvent` | 前端 → 原生 | 上报事件日志 |
| `window.AndroidBridge.refreshWeather` | 前端 → 原生 | 请求刷新天气 |

### 3.4 WebView 模块
- 启用硬件加速 (`setLayerType(View.LAYER_TYPE_HARDWARE)`)
- 缓存策略: `LOAD_CACHE_ELSE_NETWORK`
- 渲染优先级: HIGH
- 错误处理 (加载失败友好提示 + 自动重试)
- 生命周期管理 (初始化、销毁回收)
- 本地 HTML 占位页面 (用于自测)
- 网络安全配置 (允许 GitHub Pages 和和风天气 API)

### 3.5 UI 组件规格

#### MainActivity (主界面)
- **布局架构**: FrameLayout 悬浮层架构
- **WebView**: 全屏背景，加载 GitHub Pages 数字孪生模型
- **左侧面板**: 天气信息卡片、数据概览、建筑查询搜索
- **右侧面板**: 温度趋势图表（MPAndroidChart）、历史记录列表
- **底部控制**: 刷新按钮、显示/隐藏侧边栏按钮

#### 左侧面板组件
| 组件 | 功能 |
|------|------|
| 天气信息卡片 | 显示温度、天气状况、湿度、风速、体感温度 |
| 数据概览 | 累计访问、今日访问、平均温度、更新时间 |
| 建筑查询 | 搜索栏、分类选择（宿舍/教学）、搜索按钮 |

#### 右侧面板组件
| 组件 | 功能 |
|------|------|
| 温度趋势图表 | MPAndroidChart折线图，支持今日/本周切换 |
| 历史记录列表 | 可滚动的天气历史记录列表 |

#### StatisticsActivity (统计页面)
- 最近 7 天日平均气温趋势折线图 (MPAndroidChart)
- 累计访问次数展示

### 3.6 建筑查询功能
- **搜索栏**: 输入建筑名称进行搜索
- **分类选择**: 单选按钮组（宿舍 / 教学），必须选择一个
- **搜索按钮**: 触发搜索操作（搜索逻辑待实现）

### 3.7 启动页面模块 (SplashActivity)
- **布局**: FrameLayout，全屏显示启动图片
- **背景**: 启动图片 (`startimage.png`)，居中裁剪显示
- **标题**: 顶部居中显示"启东校区可视化"文字
- **标题样式**: 青色 (#00ffff)，24sp，粗体
- **标题背景**: 半透明黑色框 (#99000000)，8dp padding
- **屏幕方向**: 强制横屏 (`screenOrientation="landscape"`)
- **跳转逻辑**: 延迟2秒后自动跳转至 MainActivity

## 4. 性能指标规格

| 指标 | 要求 |
|------|------|
| 内存占用 | ≤ 300MB |
| 数据库查询响应时间 | < 10ms |
| 原生图表加载时间 | < 200ms |
| 冷启动时间 | < 2s |
| APK 大小 | ≤ 15MB |
| 目标设备 | Android 10+ |

## 5. 异常处理规格

| 异常类型 | 处理方式 |
|----------|----------|
| 网络异常 | Toast 提示 + 自动重试（最多3次） |
| 数据解析异常 | 记录日志 + 友好提示 |
| 数据库异常 | 捕获并提示 |
| WebView 加载异常 | 友好错误页面 + 重试按钮 |
| API Key 无效 | 日志记录 + 用户提示 |

## 6. 安全规格

| 项目 | 说明 |
|------|------|
| 网络安全配置 | 仅允许 HTTPS 连接，配置 network_security_config.xml |
| API Key 存储 | 当前明文存储（生产环境需加密） |
| 权限管理 | 仅申请必要权限（INTERNET, ACCESS_NETWORK_STATE） |

## 7. 交付物清单

1. Android Studio 项目源码 (含完整注释)
2. 可独立运行的 APK 安装包 (~13MB)
3. JSBridge 接口文档
4. 数据库设计文档
5. 功能测试报告
6. WebView悬浮UI前端集成指南

## 8. 前端对接说明

- 前端页面部署到 GitHub Pages 后，仅需修改 `MainActivity` 中的 `WEB_VIEW_URL` 常量即可完成集成
- 前端应部署至: `https://aaronswartz0217.github.io/NTU_Building/`
- JSBridge 接口契约由安卓端定义，前端仅需按照接口规范调用

## 9. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-05-26 | 初始版本，第一阶段安卓本体 |
| 1.0.1 | 2024-05-27 | UI架构优化：WebView全屏背景 + 原生悬浮面板 |
| 1.0.2 | 2024-05-27 | 添加建筑查询功能，修复性能问题，改为手动刷新 |
| 1.0.3 | 2024-05-27 | 启动页面优化：添加标题文字、强制横屏、自定义应用图标 |