# 南通大学启东校区数字孪生应用 - 安卓客户端

> **当前版本: 2.1.0 (将近完成版)** - 核心功能已全部实现并通过联调测试，进入最终优化阶段。

## 项目简介

本项目是南通大学启东校区移动端数字孪生应用的安卓客户端，采用 **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板** 的架构模式，集成 **Furion 后端框架** 实现完整的论坛功能、用户认证系统和 **WebSocket 实时聊天** 功能。

### 架构特点

- **原生主导**: 所有业务逻辑、数据处理、状态管理 100% 在原生层实现
- **WebView 背景**: 前端作为无状态可视化渲染器，负责 Three.js 场景、GLTF 模型加载、CSS2D 标签等展示
- **原生UI悬浮**: 天气信息、统计图表、论坛界面等以悬浮面板形式覆盖在 WebView 之上
- **JSBridge 通信**: 原生与前端通过标准化 JSON 接口进行双向通信
- **Furion 后端**: 基于 .NET 7 + Furion 框架实现用户认证、论坛、消息等服务
- **WebSocket 实时通信**: 基于 TouchSocket (端口5003) 实现公共聊天室实时消息收发

---

## 技术栈

### 安卓客户端
| 组件 | 版本 |
|------|------|
| 开发语言 | Java |
| 目标 SDK | Android 34 |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 |
| WebSocket | OkHttp WebSocket |
| 数据库 | SQLite (Room) - 本地缓存 + SharedPreferences 消息缓存 |
| 图表库 | MPAndroidChart v3.1.0 |

### Furion 后端架构
| 模块 | 职责 | 技术栈 | 端口 |
|------|------|--------|------|
| Chat.Core | 核心层（模型、数据库上下文） | SqlSugar、MySQL | - |
| Chat.Application | 应用服务层（DTO、业务逻辑） | JWT、Mapster | - |
| Chat.Server | 后端 API 服务器 (REST) | ASP.NET Core | **5002** |
| Chat.Server | WebSocket 服务器 (实时通信) | TouchSocket | **5003** |
| Chat.Desktop | 桌面客户端 | Avalonia UI | - |

**后端数据库**: MySQL

---

## 项目结构

```
NTU_Qidong_DigitalTwin/
├── app/src/main/java/com/ntu/qidong/digitaltwin/
│   ├── NTUQidongApp.java              # Application 类
│   ├── bridge/                         # JSBridge 接口
│   │   ├── JSBridgeInterface.java
│   │   ├── JSBridgeImpl.java
│   │   └── JavaScriptInjector.java
│   ├── db/                             # 数据库层 (Room)
│   │   ├── AppDatabase.java
│   │   ├── User.java / UserDao.java
│   │   ├── Post.java / PostDao.java
│   │   ├── Comment.java / CommentDao.java
│   │   ├── Message.java / MessageDao.java
│   │   └── WeatherHistoryDAO.java
│   ├── model/                          # 数据模型
│   │   ├── AccountProfile.java         # 用户档案(含role/admin权限)
│   │   ├── WeatherData.java
│   │   └── WeatherHistory.java
│   ├── network/                        # 网络层
│   │   └── WeatherApiService.java
│   ├── service/                        # 服务层
│   │   ├── ServerApiService.java       # Furion后端API服务(Token刷新/帖子CRUD)
│   │   └── WebSocketService.java       # WebSocket实时通信服务
│   ├── ui/                             # 界面层
│   │   ├── MainActivity.java           # 数字孪生主界面
│   │   ├── MainContainerActivity.java  # 底部导航容器
│   │   ├── ChatRoomActivity.java       # 公共聊天室(WS实时通信+本地缓存)
│   │   ├── StatisticsActivity.java     # 统计页面
│   │   ├── SplashActivity.java         # 启动页
│   │   ├── ForumFragment.java          # 论坛模块(发帖/删帖/我的帖子)
│   │   ├── AIFragment.java             # AI聊天模块
│   │   ├── FriendsFragment.java        # 好友列表(全员好友)
│   │   └── ProfileFragment.java        # 个人中心/登录/服务器设置
│   └── utils/                          # 工具类
├── app/src/main/res/
│   ├── layout/                         # 布局文件
│   ├── drawable/
│   │   ├── btn_delete_bg.xml           # 删除按钮背景
│   │   └── online_status_dot.xml       # 在线状态指示点
│   └── xml/network_security_config.xml # 网络安全配置(HTTP明文允许)
└── docs/                               # 文档目录
```

---

## 核心功能 (v2.1.0)

### 1. 数字孪生可视化
- Three.js 3D 校园模型渲染 (GitHub Pages)
- GLTF 建筑模型加载 + CSS2D 标签交互
- WebView 全屏背景 + 原生悬浮面板架构

### 2. 天气服务
- 和风天气 API 集成 (启东市: 101190503)
- 手动刷新模式 + 错误自动重试 (最多3次)
- 历史数据图表展示 (MPAndroidChart)

### 3. 用户认证系统 (Furion 后端)
- JWT Bearer Token 认证 (`POST /api/auth/login`)
- Token 自动刷新机制 (RefreshToken → 新 AccessToken)
- 登录状态持久化 (SharedPreferences)
- 个人资料管理 (GET/PUT `/api/users/me`)
- **角色权限**: `user` / `admin`，管理员可删除任意帖子

### 4. 论坛模块 (完整对接后端 API)
- 帖子列表: `GET /api/posts` (分页、双层嵌套解析)
- 发帖功能: `POST /api/posts`
- "我的帖子": `GET /api/posts/my` (JWT认证)
- **帖子删除**: `DELETE /api/posts/{id}` (管理员可删所有帖)
- 帖子详情查看 + 点赞数/评论数显示
- 管理员专属红色"删除"按钮 (标题右侧)

### 5. 公共聊天室 (WebSocket 实时通信)
- **WebSocket 连接**: `ws://服务器:5003/ws` (TouchSocket独立端口)
- **JWT 认证**: 连接成功后自动发送 `{type:"auth", token:"..."}`
- **实时消息收发**: 广播消息 (`sendBroadcastMessage`)
- **消息类型支持**: chat(聊天)、system(系统)、online/offline(上下线)、error(错误)
- **Token 过期自动刷新**: WS 认证失败时自动调用 `/api/auth/refresh` 重连
- **消息去重**: 本地发送的消息不重复显示
- **本地消息缓存**: SharedPreferences 缓存最近200条消息，退出重进不丢失
- **智能重连策略**: 失败3次后停止自动重连，避免无效请求

### 6. 好友系统
- 全员默认为好友 (无需单独添加)
- 用户列表: `GET /api/users` (获取所有注册用户)

### 7. 服务器地址切换 (模拟器/真机)
- **模拟器调试**: `http://10.0.2.2:5002` (Android Studio 模拟器专用地址)
- **真机调试**: `http://192.168.137.1:5002` (电脑开热点，手机连接使用)
- 未登录状态下也可切换 (登录前即可配置)
- 单选对话框选择，一键切换 + 自动测试连接

### 8. 数据库 (Room + SharedPreferences)
- SQLite 本地缓存 (用户/帖子/评论/消息)
- SharedPreferences 聊天消息缓存
- DAO 层完整 CRUD 实现

### 9. JSBridge 接口
- 原生与前端双向 JSON 通信
- 天气数据注入、建筑点击事件处理

---

## 后端 API 接口清单

### 认证接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 (返回 accessToken + refreshToken) |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/refresh` | POST | 用 RefreshToken 换新 AccessToken |

### 用户接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/users/me` | GET | 获取当前用户信息 (含 role 字段) |
| `/api/users/me` | PUT | 更新用户资料 |
| `/api/users` | GET | 获取所有用户列表 (好友列表用) |

### 帖子接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/posts` | GET | 获取帖子列表 (分页, 双层嵌套 data.data) |
| `/api/posts/my` | GET | 获取"我的帖子" (需JWT) |
| `/api/posts/user/{id}` | GET | 获取指定用户帖子 |
| `/api/posts` | POST | 创建帖子 |
| `/api/posts/{id}` | DELETE | 删除帖子 (管理员可删全部) |
| `/api/posts/{id}/like` | POST | 点赞帖子 |
| `/api/posts/{id}/favorite` | POST | 收藏帖子 |

### WebSocket 接口
| 地址 | 协议 | 说明 |
|------|------|------|
| `ws://服务器:5003/ws` | WebSocket | 实时聊天通道 (TouchSocket) |

#### WS 消息格式
```json
// C→S 认证
{"type":"auth","token":"JWT_TOKEN"}

// C→S 发送广播消息
{"type":"chat","content":"Hello"}

// S→C 聊天消息
{"type":"chat","fromUserId":1,"fromUserName":"测试用户","content":"Hello","timestamp":"..."}

// S→C 系统消息
{"type":"system","fromUserName":"系统","content":"欢迎 测试用户，您已上线！"}

// S→C 错误
{"type":"error","content":"认证失败：无效的Token"}
```

---

## 构建说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34, JDK 17
- Furion 后端服务运行中 (REST:5002, WS:5003)

### 构建步骤
1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 配置服务器地址: 设置页面选择 **模拟器** 或 **真机调试**
4. 构建 Debug APK 并安装

### 运行前提
- 后端 `Chat.Server` 已启动 (REST API 端口 5002)
- 后端 TouchSocket WebSocket 已启动 (端口 5003)
- 手机/模拟器能访问后端 IP

---

## 性能指标

| 指标 | 目标 | 当前状态 |
|------|------|----------|
| 内存占用 | ≤ 300MB | ✅ 达标 |
| 数据库查询 | < 10ms | ✅ 达标 |
| 图表加载 | < 200ms | ✅ 达标 |
| 冷启动 | < 2s | ✅ 达标 |
| APK 大小 | ~15MB | ✅ 达标 |
| WS 消息延迟 | < 100ms | ✅ 正常 |

---

## 文档

| 文档 | 说明 |
|------|------|
| [规格说明书](SPEC.md) | 技术规格与功能模块详细定义 |
| [JSBridge 接口文档](docs/JSBridge_接口文档.md) | 原生-前端双向通信协议 |
| [数据库设计文档](docs/数据库设计文档.md) | 本地SQLite + 后端MySQL设计 |
| [功能测试报告](docs/功能测试报告.md) | 功能测试用例与结果记录 |
| [WebView悬浮UI前端集成指南](docs/WebView悬浮UI前端集成指南.md) | 前端集成规范 |

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-05-26 | 初始版本，第一阶段安卓本体 |
| 1.0.1~1.0.3 | 2026-05-27 | UI架构优化、建筑查询、启动页美化 |
| 2.0.0 | 2026-06-11 | 集成 Furion 后端，登录系统、论坛、消息功能 |
| **2.1.0** | **2026-06-13** | **将近完成版: WebSocket实时聊天、Token自动刷新、管理员删帖、消息本地缓存、模拟器/真机切换、全员好友、网络安全配置修复** |

---

## 开源协议

本项目仅供学习交流使用。
