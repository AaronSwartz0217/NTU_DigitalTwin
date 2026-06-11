# 南通大学启东校区数字孪生应用 - 安卓客户端

## 项目简介

本项目是南通大学启东校区移动端数字孪生应用的安卓客户端，采用 **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板** 的架构模式，集成 **Furion 后端框架** 实现完整的论坛功能和用户认证系统。

### 架构特点

- **原生主导**: 所有业务逻辑、数据处理、状态管理 100% 在原生层实现
- **WebView 背景**: 前端作为无状态可视化渲染器，负责 Three.js 场景、GLTF 模型加载、CSS2D 标签等展示
- **原生UI悬浮**: 天气信息、统计图表、论坛界面等以悬浮面板形式覆盖在 WebView 之上
- **JSBridge 通信**: 原生与前端通过标准化 JSON 接口进行双向通信
- **Furion 后端**: 基于 .NET 7 + Furion 框架实现用户认证、论坛、消息等服务

---

## 技术栈

### 安卓客户端
| 组件 | 版本 |
|------|------|
| 开发语言 | Java |
| 目标 SDK | Android 34 |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 |
| 数据库 | SQLite (Room) - 本地缓存 |
| 图表库 | MPAndroidChart v3.1.0 |
| 定时任务 | Handler |

### Furion 后端架构
| 模块 | 职责 | 技术栈 |
|------|------|--------|
| Chat.Core | 核心层（模型、数据库上下文） | SqlSugar、MySQL/PostgreSQL |
| Chat.Application | 应用服务层（DTO、业务逻辑） | JWT、Mapster |
| Chat.Server | 后端 API 服务器 | ASP.NET Core、TouchSocket WebSocket |
| Chat.Desktop | 桌面客户端 | Avalonia UI、CommunityToolkit.Mvvm、Semi.Avalonia |

**后端数据库**: MySQL

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
│   │   │   ├── db/                         # 数据库层 (Room)
│   │   │   │   ├── AppDatabase.java
│   │   │   │   ├── User.java
│   │   │   │   ├── UserDao.java
│   │   │   │   ├── Post.java
│   │   │   │   ├── PostDao.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── CommentDao.java
│   │   │   │   ├── Message.java
│   │   │   │   ├── MessageDao.java
│   │   │   │   └── WeatherHistoryDAO.java
│   │   │   ├── model/                      # 数据模型
│   │   │   │   ├── AccountProfile.java     # 用户档案模型
│   │   │   │   ├── WeatherData.java
│   │   │   │   └── WeatherHistory.java
│   │   │   ├── network/                    # 网络层
│   │   │   │   ├── WeatherApiService.java
│   │   │   │   └── WeatherApiService.java
│   │   │   ├── service/                    # 服务层
│   │   │   │   ├── WeatherService.java
│   │   │   │   └── ServerApiService.java   # Furion后端API服务
│   │   │   ├── ui/                         # 界面层
│   │   │   │   ├── MainActivity.java       # 数字孪生主界面
│   │   │   │   ├── MainContainerActivity.java  # 底部导航容器
│   │   │   │   ├── ChatRoomActivity.java   # 聊天室
│   │   │   │   ├── StatisticsActivity.java # 统计页面
│   │   │   │   ├── SplashActivity.java     # 启动页
│   │   │   │   ├── ForumFragment.java      # 论坛模块
│   │   │   │   ├── AIFragment.java         # AI聊天模块
│   │   │   │   ├── FriendsFragment.java    # 好友列表
│   │   │   │   └── ProfileFragment.java    # 个人中心/登录
│   │   │   └── utils/                      # 工具类
│   │   │       ├── DateUtils.java
│   │   │       └── NetworkUtils.java
│   │   ├── res/                            # 资源文件
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   ├── drawable/
│   │   │   ├── anim/
│   │   │   └── xml/network_security_config.xml
│   │   ├── assets/
│   │   │   └── placeholder.html            # 本地占位页面
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

### 1. 数字孪生可视化
- Three.js 3D 校园模型渲染
- GLTF 建筑模型加载
- CSS2D 标签交互
- 建筑信息展示

### 2. 天气服务
- 和风天气 API 集成 (启东市: 101190503)
- 手动刷新模式
- 错误自动重试 (最多 3 次)
- 历史数据图表展示

### 3. 用户认证系统 (Furion 后端)
- JWT Token 认证
- 用户登录/注册
- 个人资料管理
- Token 本地缓存

### 4. 论坛模块
- 帖子列表展示
- 发帖/评论功能
- 帖子搜索
- 点赞/收藏

### 5. 消息系统
- 好友列表
- 实时聊天
- 消息通知
- AI 聊天助手

### 6. 数据库 (Room)
- 用户表、帖子表、评论表、消息表
- DAO 层完整实现
- 本地数据缓存

### 7. JSBridge 接口
- 原生与前端双向通信
- 天气数据注入
- 建筑点击事件处理
- 应用状态获取

---

## 布局架构

```
┌─────────────────────────────────────────────────────────────┐
│  MainContainerActivity                                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  WebView (全屏背景 - GitHub Pages 数字孪生模型)        │  │
│  │                                                       │  │
│  │  ┌────────────┬───────────────────────────────────┐   │  │
│  │  │   左侧面板 │                                 │   │   │  │
│  │  │  (悬浮层) │                                 │   │   │  │
│  │  │  ├─天气信息│                                 │   │   │  │
│  │  │  ├─数据概览│                                 │   │   │  │
│  │  │  └─建筑查询│                                 │   │   │  │
│  │  └────────────┴───────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  [论坛]  [AI]  [消息]  [可视化]  [个人中心]          │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 后端集成 (Furion)

### API 接口

| 接口 | 方法 | 功能 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/users/me` | GET | 获取当前用户 |
| `/api/users/me` | PUT | 更新用户资料 |
| `/api/posts` | GET | 获取帖子列表 |
| `/api/posts` | POST | 创建帖子 |
| `/api/posts/{id}` | GET | 获取帖子详情 |
| `/api/comments` | POST | 创建评论 |
| `/api/messages` | GET | 获取消息列表 |

### 认证机制

- JWT Bearer Token
- Token 有效期管理
- 自动登录状态恢复

---

## 构建说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- JDK 17

### 构建步骤

1. **使用 Android Studio 打开项目**

2. **等待 Gradle 同步完成**

3. **配置后端服务器地址**
   编辑 `app/src/main/java/com/ntu/qidong/digitaltwin/service/ServerApiService.java`
   ```java
   public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:5002";
   ```

4. **配置和风天气 API Key**
   编辑 `app/src/main/java/com/ntu/qidong/digitaltwin/network/WeatherApiService.java`
   ```java
   private static final String API_KEY = "YOUR_API_KEY_HERE";
   ```

5. **构建 Debug APK**

### 前端集成

WebView 加载 GitHub Pages 页面：
- URL: `https://aaronswartz0217.github.io/NTU_Building/`

---

## 性能指标

| 指标 | 目标 |
|------|------|
| 内存占用 | ≤ 300MB |
| 数据库查询 | < 10ms |
| 图表加载 | < 200ms |
| 冷启动 | < 2s |
| APK 大小 | ~15MB |

---

## 注意事项

1. **API Key 安全**: 当前 API Key 直接写在代码中，实际部署时应使用加密存储或后端代理
2. **网络权限**: 应用需要 INTERNET 和 ACCESS_NETWORK_STATE 权限
3. **最低版本**: 应用最低支持 Android 10 (API 29)
4. **后端依赖**: 需要部署 Furion 后端服务

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
| 2.0.0 | 2026-06-11 | 集成 Furion 后端，添加完整的登录系统、论坛、消息功能 |