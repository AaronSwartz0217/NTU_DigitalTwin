# 南通大学启东校区数字孪生应用 - 安卓客户端规格说明书

> **版本: 2.1.0 (将近完成版)** | 更新日期: 2026-06-13

## 1. 项目概述

**项目名称**: NTU_Qidong_DigitalTwin
**项目类型**: Android 原生应用 + WebView 前端渲染 + Furion 后端集成
**核心架构**: **Android 原生主导 + WebView 全屏背景 + 原生UI悬浮面板 + Furion 后端服务 + WebSocket 实时通信**

**当前状态**: 核心功能全部实现并通过联调测试，进入最终优化阶段。

---

## 2. 技术栈规格

### 2.1 安卓客户端技术栈

| 组件 | 技术规格 |
|------|---------|
| 开发语言 | Java |
| 目标 SDK | Android SDK 34 (Android 14) |
| 最低 SDK | Android 10 (API 29) |
| HTTP 客户端 | OkHttp 4.12.0 (REST API + WebSocket) |
| 数据库 | SQLite (Room ORM) - 本地缓存 + SharedPreferences - 消息缓存 |
| 图表库 | MPAndroidChart v3.1.0 |
| 认证方式 | JWT Bearer Token (自动刷新) |

### 2.2 Furion 后端架构

| 模块 | 职责 | 技术栈 | 端口 |
|------|------|--------|------|
| Chat.Core | 核心层（模型、数据库上下文） | SqlSugar、MySQL | - |
| Chat.Application | 应用服务层（DTO、业务逻辑） | JWT、Mapster | - |
| Chat.Server (REST) | 后端 API 服务器 | ASP.NET Core | **5002** |
| Chat.Server (WS) | WebSocket 实时通信服务器 | TouchSocket | **5003** |
| Chat.Desktop | 桌面客户端 | Avalonia UI | - |

**后端数据库**: MySQL (`chat_db`)

---

## 3. 功能模块规格 (v2.1.0)

### 3.1 数字孪生可视化模块
- **前端渲染**: Three.js 3D 校园模型 (GitHub Pages)
- **模型格式**: GLTF/GLB 建筑模型
- **交互方式**: CSS2D 标签、建筑点击事件 (JSBridge)
- **WebView 加载**: GitHub Pages 静态页面 + 本地占位页兜底

### 3.2 天气服务模块
- **数据源**: 和风天气 API (启东市 Location ID: 101190503)
- **刷新机制**: 手动刷新模式（点击刷新按钮触发）
- **错误处理**: 失败自动重试 (最多 3 次，间隔 5 秒)
- **缓存策略**: SQLite 本地数据库缓存历史数据
- **图表展示**: MPAndroidChart 温度趋势图

### 3.3 用户认证模块 (Furion 后端)
- **认证方式**: JWT Bearer Token
- **登录接口**: `POST /api/auth/login` → 返回 accessToken + refreshToken
- **登出接口**: `POST /api/auth/logout`
- **Token 刷新**: `POST /api/auth/refresh` (过期时自动调用)
- **用户信息**: `GET /api/users/me` (含 role 字段: user/admin)
- **资料更新**: `PUT /api/users/me`
- **Token 存储**: SharedPreferences (auth_config)
- **角色权限**: admin 角色可删除任意帖子
- **自动登录恢复**: 启动时检查 Token 并恢复状态

### 3.4 论坛模块 (完整对接后端 API)
- **帖子列表**: `GET /api/posts` (分页, 双层嵌套 `{data:{data:[...]}}`)
- **发帖功能**: `POST /api/posts`
- **我的帖子**: `GET /api/posts/my` (需 JWT 认证)
- **帖子详情**: 点击卡片查看详情
- **帖子删除**: `DELETE /api/posts/{id}` (管理员可删所有帖, 普通用户删自己的)
- **点赞/收藏**: `POST /api/posts/{id}/like`, `/favorite`
- **管理员 UI**: 红色"删除"按钮显示在帖子标题右侧
- **onResume 刷新**: 每次回到论坛页面自动刷新管理员状态和登录状态

### 3.5 公共聊天室模块 (WebSocket 实时通信) ⭐ 新增 v2.1.0
- **连接地址**: `ws://服务器IP:5003/ws` (TouchSocket 独立端口)
- **认证流程**: 连接成功 → 发送 `{type:"auth", token:"JWT"}` → 等待确认
- **消息发送**: `{type:"chat", content:"..."}` (广播模式)
- **接收消息**: chat(聊天)、system(系统欢迎)、online/offline、error(错误)
- **Token 过期处理**: WS 返回"无效Token"时 → 自动调用 refreshAccessTokenSync() → 重连
- **消息去重**: 收到 fromUserId == 自己的消息时不重复添加
- **本地缓存**: SharedPreferences 缓存最近200条聊天消息, 进入聊天室时加载
- **智能重连**: 失败3次后标记为不可恢复, 避免无限重连
- **UI 线程安全**: 所有 WS 回调均通过 runOnUiThread 更新 UI
- **监听器单例**: addListener 只在 onCreate 调用一次, 避免 onMessage 双重回调

#### ChatRoomActivity.ChatMessage 数据模型
```java
public static class ChatMessage {
    public int userId;
    public String userName;
    public String content;
    public long timestamp;
    public boolean isSelf;      // 是否自己发的
    public long messageId;      // 服务端消息ID
    
    // Getter 方法供 ChatMessageAdapter 使用
    public boolean isSelf() { return isSelf; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public String getUserName() { return userName; }
}
```

### 3.6 好友系统模块
- **全员好友**: 默认所有注册用户互为好友, 无需单独添加
- **用户列表**: `GET /api/users` (获取所有注册用户作为好友列表)
- **移除在线/离线状态显示**: 后端无此数据, 简化 UI

### 3.7 服务器设置模块 ⭐ 新增 v2.1.0
- **模拟器模式**: `http://10.0.2.2:5002` (Android Studio 模拟器专用)
- **真机调试模式**: `http://192.168.137.1:5002` (电脑开热点, 手机连接使用)
- **未登录可用**: 登录界面底部即可看到"服务器设置"按钮
- **单选对话框**: 两个预设选项, 一键切换
- **自动映射**: REST 端口 5002 → WS 端口 5003 自动转换
- **网络安全**: network_security_config.xml 已配置 cleartextTrafficPermitted=true

### 3.8 数据库模块

#### 3.8.1 本地数据库（SQLite + Room）
- **数据库名称**: `ntu_qidong.db`
- **用途**: 用户/帖子/评论/消息/天气历史 本地缓存
- **表结构**: user, post, comment, message, weather_history, app_visit

#### 3.8.2 SharedPreferences 缓存 ⭐ 新增 v2.1.0
- **聊天消息缓存**: key=`chat_messages_cache`, 最近200条 JSON 数组
- **认证配置**: auth_config (accessToken, refreshToken, serverUrl)
- **用户资料缓存**: cached_account_json (含 role 字段)

#### 3.8.3 后端数据库（MySQL + SqlSugar）
- **数据库名称**: `chat_db`
- **用途**: 核心数据存储, 多端同步

### 3.9 JSBridge 接口规格

| 接口 | 方向 | 功能 |
|------|------|------|
| `window.updateWeather` | 原生→前端 | 注入实时天气数据 |
| `window.WeatherBridge.getWeatherHistory` | 前端→原生 | 查询历史天气数据 |
| `window.WeatherBridge.getCurrentWeather` | 前端→原生 | 获取当前天气数据 |
| `window.AndroidBridge.showBuildingInfo` | 前端→原生 | 建筑点击事件 |
| `window.AndroidBridge.getAppState` | 前端→原生 | 获取完整应用状态 |
| `window.AndroidBridge.getAppVersion` | 前端→原生 | 获取应用版本信息 |
| `window.AndroidBridge.logEvent` | 前端→原生 | 上报事件日志 |
| `window.AndroidBridge.refreshWeather` | 前端→原生 | 请求刷新天气 |

### 3.10 WebSocketService 服务规格 ⭐ 新增 v2.1.0

```java
public class WebSocketService implements ... {
    // 连接状态枚举
    enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }
    
    // 核心方法
    void connect(String jwtToken);              // 建立 WS 连接 + 发送认证
    void disconnect();                          // 断开连接
    boolean isConnected();                       // 是否已连接
    void setServerUrl(String url);               // 设置服务器地址
    
    // 消息发送
    void sendBroadcastMessage(String content);   // 广播消息(公共聊天室)
    
    // 事件监听
    interface WebSocketEventListener {
        void onConnected();                      // 连接成功+认证通过
        void onDisconnected();                  // 断开连接
        void onError(String error, boolean isEndpointNotFound);
        void onChatMessage(ChatMessage msg);     // 聊天消息
        void onSystemMessage(String content);    // 系统消息
        void onUserOnline(int userId, String name);   // 上线通知
        // ...
    }
}
```

---

## 4. 性能指标规格

| 指标 | 要求 | 当前状态 |
|------|------|----------|
| 内存占用 | ≤ 300MB | ✅ 达标 |
| 数据库查询响应时间 | < 10ms | ✅ 达标 |
| 原生图表加载时间 | < 200ms | ✅ 达标 |
| 冷启动时间 | < 2s | ✅ 达标 |
| APK 大小 | ≤ 15MB | ✅ ~15MB |
| WS 消息延迟 | < 100ms | ✅ 正常 |
| 目标设备 | Android 10+ | ✅ 支持 |

---

## 5. 后端接口完整规格

### 认证接口
| 接口 | 方法 | 请求体 | 说明 |
|------|------|--------|------|
| `/api/auth/login` | POST | `{userName, password}` | 返回 accessToken + refreshToken |
| `/api/auth/logout` | POST | - | 清除 Token |
| `/api/auth/refresh` | POST | `{refreshToken}` | 用 RefreshToken 换新 Token |

### 用户接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/users/me` | GET | 获取当前用户 (含 role/userName/nickname 等) |
| `/api/users/me` | PUT | 更新资料 (name/no/gender/birthday 等) |
| `/api/users` | GET | 获取所有用户列表 (好友列表) |

### 帖子接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/posts` | GET | 帖子列表 (pageIndex, pageSize 分页) |
| `/api/posts/my` | GET | 我的帖子 (需JWT) |
| `/api/posts/user/{id}` | GET | 指定用户帖子 |
| `/api/posts` | POST | 创建帖子 (title, content) |
| `/api/posts/{id}` | DELETE | 删除帖子 (admin 可删全部) |
| `/api/posts/{id}/like` | POST | 点赞 |
| `/api/posts/{id}/favorite` | POST | 收藏 |

### WebSocket 接口
| 地址 | 协议 | 端口 | 说明 |
|------|------|------|------|
| `/ws` | WebSocket | 5003 | TouchSocket 实时通信通道 |

---

## 6. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2026-05-26 | 初始版本，第一阶段安卓本体 |
| 1.0.1~1.0.3 | 2026-05-27 | UI架构优化、建筑查询、启动页美化 |
| 2.0.0 | 2026-06-11 | 集成 Furion 后端，登录系统、论坛、消息功能 |
| **2.1.0** | **2026-06-13** | **将近完成版: WebSocket实时聊天、Token自动刷新、管理员删帖(含role字段)、消息本地缓存、模拟器/真机切换、全员好友、网络安全配置修复、ChatMessage getter方法、ForumFragment onResume刷新** |
