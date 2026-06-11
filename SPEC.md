# 南通大学启东校区数字孪生应用 - 安卓客户端规格说明书

## 1. 项目概述

**项目名称**: NTU_Qidong_DigitalTwin
**项目类型**: Android 原生应用 + WebView 前端渲染 + Furion 后端集成
**核心架构**: **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板 + Furion 后端服务**

**项目背景**: 南通大学启东校区移动端数字孪生应用的安卓客户端开发，集成 Furion (.NET 7) 后端框架，实现完整的用户认证、论坛、消息系统，并通过 WebView 展示 Three.js 数字孪生校园模型。

## 2. 技术栈规格

### 2.1 安卓客户端技术栈

| 组件 | 技术规格 |
|------|---------|
| 开发语言 | Java |
| 目标 SDK | Android SDK 34 (Android 14) |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 |
| 数据库 | SQLite (Room ORM) - 本地缓存 |
| 图表库 | MPAndroidChart v3.1.0 |
| 定时任务 | Handler + Runnable |
| WebView | Android WebView (启用硬件加速) |
| 认证方式 | JWT Bearer Token |

### 2.2 Furion 后端架构

| 模块 | 职责 | 技术栈 |
|------|------|--------|
| Chat.Core | 核心层（模型、数据库上下文） | SqlSugar、MySQL/PostgreSQL |
| Chat.Application | 应用服务层（DTO、业务逻辑） | JWT、Mapster |
| Chat.Server | 后端 API 服务器 | ASP.NET Core、TouchSocket WebSocket |
| Chat.Desktop | 桌面客户端 | Avalonia UI、CommunityToolkit.Mvvm、Semi.Avalonia |

**后端数据库**: MySQL

## 3. 功能模块规格

### 3.1 数字孪生可视化模块
- **前端渲染**: Three.js 3D 校园模型
- **模型格式**: GLTF/GLB 建筑模型
- **交互方式**: CSS2D 标签、建筑点击事件
- **WebView 加载**: GitHub Pages 静态页面

### 3.2 天气服务模块
- **数据源**: 和风天气 API (启东市 Location ID: 101190503)
- **数据内容**: 气温、体感温度、湿度、天气状况、风向、风速
- **刷新机制**: 手动刷新模式（点击刷新按钮触发）
- **错误处理**: 失败自动重试 (最多 3 次，间隔 5 秒)
- **缓存策略**: 本地数据库缓存，启动时加载缓存数据

### 3.3 用户认证模块 (Furion 后端)
- **认证方式**: JWT Bearer Token
- **登录接口**: POST /api/auth/login
- **登出接口**: POST /api/auth/logout
- **用户信息**: GET /api/users/me
- **资料更新**: PUT /api/users/me
- **Token 存储**: SharedPreferences (auth_config)
- **自动登录**: 启动时检查 Token 并恢复登录状态

### 3.4 论坛模块
- **帖子列表**: GET /api/posts
- **帖子创建**: POST /api/posts
- **帖子详情**: GET /api/posts/{id}
- **评论功能**: POST /api/comments
- **帖子搜索**: 支持标题/内容搜索
- **点赞收藏**: 支持帖子互动

### 3.5 消息模块
- **好友列表**: GET /api/friends
- **消息列表**: GET /api/messages
- **发送消息**: POST /api/messages
- **消息通知**: 本地通知推送
- **AI 聊天**: 内置 AI 助手对话

### 3.6 数据库模块
- **数据库名称**: ntu_qidong.db
- **表结构**:
  - `user`: 用户信息表
  - `post`: 帖子表
  - `comment`: 评论表
  - `message`: 消息表
  - `weather_history`: 天气历史记录表
  - `app_visit`: 应用访问记录表

### 3.7 JSBridge 接口规格

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

### 3.8 WebView 模块
- 启用硬件加速 (`setLayerType(View.LAYER_TYPE_HARDWARE)`)
- 缓存策略: `LOAD_CACHE_ELSE_NETWORK`
- 渲染优先级: HIGH
- 错误处理 (加载失败友好提示 + 自动重试)
- 生命周期管理 (初始化、销毁回收)
- 本地 HTML 占位页面 (用于自测)
- 网络安全配置 (允许 GitHub Pages 和和风天气 API)

### 3.9 UI 组件规格

#### MainContainerActivity (主容器)
- **底部导航**: 论坛、AI、消息、可视化、个人中心
- **Fragment 切换**: ViewPager2 + BottomNavigationView

#### MainActivity (数字孪生主界面)
- **布局架构**: FrameLayout 悬浮层架构
- **WebView**: 全屏背景，加载 GitHub Pages 数字孪生模型
- **左侧面板**: 天气信息卡片、数据概览、建筑查询搜索
- **右侧面板**: 温度趋势图表（MPAndroidChart）、历史记录列表
- **底部控制**: 刷新按钮、显示/隐藏侧边栏按钮

#### ProfileFragment (个人中心)
- **登录界面**: 用户名密码输入、登录按钮
- **用户档案**: 头像、姓名、学号、个人资料展示
- **功能按钮**: 编辑资料、登出、设置

#### ForumFragment (论坛)
- **帖子列表**: 卡片式展示、下拉刷新
- **发帖功能**: 标题、内容输入
- **评论功能**: 评论列表、发表评论

#### AIFragment (AI聊天)
- **聊天界面**: 消息气泡展示
- **输入框**: 文本输入、发送按钮
- **消息类型**: 用户消息、AI 回复

#### FriendsFragment (好友列表)
- **好友列表**: 头像、昵称、最后消息
- **点击进入**: 跳转到聊天室

#### ChatRoomActivity (聊天室)
- **聊天消息列表**: 支持多种消息类型
- **输入框**: 文本输入、表情、图片
- **消息发送**: 实时发送、状态反馈

#### SplashActivity (启动页)
- **布局**: FrameLayout，全屏显示启动图片
- **背景**: 启动图片 (`startimage.png`)，居中裁剪显示
- **标题**: 顶部居中显示"启东校区可视化"文字
- **标题样式**: 青色 (#00ffff)，24sp，粗体
- **标题背景**: 半透明黑色框 (#99000000)，8dp padding
- **屏幕方向**: 强制横屏 (`screenOrientation="landscape"`)
- **跳转逻辑**: 延迟2秒后自动跳转至 MainContainerActivity

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
| 登录失败 | 友好错误消息提示 |

## 6. 安全规格

| 项目 | 说明 |
|------|------|
| 网络安全配置 | 仅允许 HTTPS 连接，配置 network_security_config.xml |
| API Key 存储 | 当前明文存储（生产环境需加密） |
| Token 存储 | SharedPreferences MODE_PRIVATE |
| 密码处理 | 不存储密码，仅用于登录请求 |
| 身份证脱敏 | 显示时隐藏中间8位 |
| 权限管理 | 仅申请必要权限（INTERNET, ACCESS_NETWORK_STATE） |

## 7. 后端接口规格 (Furion)

### 认证接口
| 接口 | 方法 | 请求体 | 响应体 |
|------|------|--------|--------|
| `/api/auth/login` | POST | `{ userName, password }` | `{ token, user }` |
| `/api/auth/logout` | POST | - | `{ success, message }` |

### 用户接口
| 接口 | 方法 | 请求体 | 响应体 |
|------|------|--------|--------|
| `/api/users/me` | GET | - | `{ userId, userName, name, ... }` |
| `/api/users/me` | PUT | `{ name, gender, ... }` | `{ success, message }` |

### 帖子接口
| 接口 | 方法 | 请求体 | 响应体 |
|------|------|--------|--------|
| `/api/posts` | GET | - | `[{ id, title, content, author, createdAt }]` |
| `/api/posts` | POST | `{ title, content }` | `{ id, title, content, ... }` |
| `/api/posts/{id}` | GET | - | `{ id, title, content, comments }` |

### 评论接口
| 接口 | 方法 | 请求体 | 响应体 |
|------|------|--------|--------|
| `/api/comments` | POST | `{ postId, content }` | `{ id, content, author, ... }` |

### 消息接口
| 接口 | 方法 | 请求体 | 响应体 |
|------|------|--------|--------|
| `/api/messages` | GET | - | `[{ id, senderId, content, createdAt }]` |
| `/api/messages` | POST | `{ receiverId, content }` | `{ id, content, ... }` |

## 8. 交付物清单

1. Android Studio 项目源码 (含完整注释)
2. 可独立运行的 APK 安装包 (~15MB)
3. JSBridge 接口文档
4. 数据库设计文档
5. 功能测试报告
6. WebView悬浮UI前端集成指南
7. 后端 API 接口文档

## 9. 前端对接说明

- 前端页面部署到 GitHub Pages 后，仅需修改 `MainActivity` 中的 `WEB_VIEW_URL` 常量即可完成集成
- 前端应部署至: `https://aaronswartz0217.github.io/NTU_Building/`
- JSBridge 接口契约由安卓端定义，前端仅需按照接口规范调用

## 10. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-05-26 | 初始版本，第一阶段安卓本体 |
| 1.0.1 | 2024-05-27 | UI架构优化：WebView全屏背景 + 原生悬浮面板 |
| 1.0.2 | 2024-05-27 | 添加建筑查询功能，修复性能问题，改为手动刷新 |
| 1.0.3 | 2024-05-27 | 启动页面优化：添加标题文字、强制横屏、自定义应用图标 |
| 2.0.0 | 2024-06-11 | 集成 Furion 后端，添加完整的登录系统、论坛、消息、AI聊天功能 |