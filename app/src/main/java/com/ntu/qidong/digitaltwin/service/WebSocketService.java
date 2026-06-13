package com.ntu.qidong.digitaltwin.service;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket 实时通信服务
 *
 * 后端地址: ws://localhost:5002/ws (或对应IP)
 * 消息类型: auth, chat, typing, read, system, online, offline, online_list, error
 *
 * 使用流程:
 *   1. connect(token) - 建立连接并发送JWT认证
 *   2. sendChatMessage(toUserId/content/channelId) - 发送聊天消息
 *   3. sendTyping(toUserId) - 发送输入状态
 *   4. disconnect() - 断开连接
 */
public class WebSocketService {
    private static final String TAG = "WebSocketService";

    // WebSocket 地址（与 HTTP 服务同端口）
    private static final String WS_URL_TEMPLATE = "%s/ws";

    private OkHttpClient client;
    private WebSocket webSocket;
    private Context context;
    private String serverUrl;  // 如 http://10.0.2.2:5002

    // 连接状态
    public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;

    // 是否为不可恢复错误（如404端点不存在），避免反复重连
    private boolean isUnrecoverable = false;
    private String unrecoverableReason = "";

    // 连续失败次数（超过阈值后暂停自动重连）
    private int consecutiveFailCount = 0;
    private static final int MAX_CONSECUTIVE_FAILS = 3;  // 连续失败3次后停止自动重连

    // 监听器列表
    private List<WebSocketEventListener> listeners = new ArrayList<>();

    // 单例
    private static WebSocketService instance;

    public static synchronized WebSocketService getInstance(Context context) {
        if (instance == null) {
            instance = new WebSocketService(context.getApplicationContext());
        }
        return instance;
    }

    private WebSocketService(Context context) {
        this.context = context;
        this.serverUrl = ServerApiService.DEFAULT_SERVER_URL;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)  // 心跳保活
                .build();
    }

    /**
     * 设置服务器URL（用于动态切换）
     */
    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    /**
     * 获取当前连接状态
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED && webSocket != null;
    }

    // ==================== 连接管理 ====================

    /**
     * 建立WebSocket连接并认证
     * @param jwtToken 登录获取的JWT Token
     */
    public void connect(String jwtToken) {
        if (isUnrecoverable) {
            Log.w(TAG, "上次连接失败（" + unrecoverableReason + "），跳过连接。如需重试请调用 reset()");
            return;
        }

        if (consecutiveFailCount >= MAX_CONSECUTIVE_FAILS) {
            Log.w(TAG, "连续失败 " + consecutiveFailCount + " 次，暂停自动重连。请调用 reset() 后重试");
            return;
        }

        if (isConnected()) {
            Log.w(TAG, "WebSocket 已连接，跳过重复连接");
            return;
        }

        if (connectionState == ConnectionState.CONNECTING) {
            Log.w(TAG, "WebSocket 正在连接中，跳过重复请求");
            return;
        }

        // WebSocket 独立端口（后端架构：Kestrel 5002 = REST API，TouchSocket 5003 = WebSocket）
        String wsUrl = serverUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .replace(":5002", ":5003")  // REST端口 → WS端口
                + "/ws";
        Log.d(TAG, "连接 WebSocket: " + wsUrl);

        connectionState = ConnectionState.CONNECTING;
        notifyStateChange(ConnectionState.CONNECTING);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 连接成功，发送认证消息...");
                connectionState = ConnectionState.CONNECTED;
                consecutiveFailCount = 0;  // 连接成功，重置失败计数
                notifyStateChange(ConnectionState.CONNECTED);

                // 连接成功后立即发送JWT认证
                try {
                    JSONObject authMsg = new JSONObject();
                    authMsg.put("type", "auth");
                    authMsg.put("token", jwtToken);
                    webSocket.send(authMsg.toString());
                    Log.d(TAG, "认证消息已发送 (Token长度: " +
                            (jwtToken != null ? jwtToken.length() : "null") + ")");
                } catch (JSONException e) {
                    Log.e(TAG, "构建认证消息失败: " + e.getMessage());
                    notifyError("构建认证消息失败");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到消息: " + text);
                handleMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "收到二进制消息，长度: " + bytes.size());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 正在关闭: code=" + code + ", reason=" + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 已关闭: code=" + code + ", reason=" + reason);
                connectionState = ConnectionState.DISCONNECTED;
                notifyStateChange(ConnectionState.DISCONNECTED);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                String errorMsg = t.getMessage();
                boolean isEndpointNotFound = false;

                consecutiveFailCount++;
                Log.e(TAG, "WebSocket 连接失败 (第" + consecutiveFailCount + "次): " + errorMsg);

                // 检测404（WebSocket端点不存在）
                if (response != null && response.code() == 404) {
                    isUnrecoverable = true;
                    isEndpointNotFound = true;
                    unrecoverableReason = "服务端WebSocket端点未启用 (HTTP 404)";
                    errorMsg = unrecoverableReason;
                    Log.e(TAG, "提示：请确认后端已启动 WebSocket 中间件，路径为 /ws");
                } else if (consecutiveFailCount >= MAX_CONSECUTIVE_FAILS) {
                    // 连续失败达到阈值，标记为不可恢复（连接超时/拒绝等）
                    isUnrecoverable = true;
                    unrecoverableReason = "连续" + consecutiveFailCount + "次连接失败: " + errorMsg;
                    Log.e(TAG, unrecoverableReason);
                    Log.e(TAG, "提示：请检查后端 WebSocket 服务(" +
                            serverUrl.replace(":5002", ":5003") + ") 是否已启动");
                }

                connectionState = ConnectionState.DISCONNECTED;
                notifyStateChange(ConnectionState.DISCONNECTED);
                notifyError(errorMsg, isEndpointNotFound);
            }
        });
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocket != null) {
            Log.d(TAG, "断开 WebSocket 连接");
            webSocket.close(1000, "用户主动断开");
            webSocket = null;
        }
        connectionState = ConnectionState.DISCONNECTED;
        // 不断开时不清除 isUnrecoverable，让用户主动调用 reset()
        notifyStateChange(ConnectionState.DISCONNECTED);
    }

    /**
     * 重置不可恢复状态（后端修复后可手动调用重试）
     */
    public void reset() {
        isUnrecoverable = false;
        unrecoverableReason = "";
        consecutiveFailCount = 0;
        Log.d(TAG, "WebSocket 状态已重置，可以重新尝试连接");
    }

    /**
     * 是否处于不可恢复状态（如404端点不存在）
     */
    public boolean isUnrecoverable() {
        return isUnrecoverable;
    }

    // ==================== 消息发送 ====================

    /**
     * 发送私聊消息（点对点）
     * @param toUserId 目标用户ID
     * @param content 消息内容
     */
    public void sendChatMessage(int toUserId, String content) {
        sendMessageInternal("chat", toUserId, content, null);
    }

    /**
     * 发送群聊/频道消息
     * @param channelId 频道ID
     * @param content 消息内容
     */
    public void sendChannelMessage(int channelId, String content) {
        sendMessageInternal("chat", 0, content, channelId);
    }

    /**
     * 发送广播消息（所有在线用户）
     * @param content 消息内容
     */
    public void sendBroadcastMessage(String content) {
        sendMessageInternal("chat", 0, content, 0);
    }

    /**
     * 发送输入状态提示
     * @param toUserId 对方用户ID
     */
    public void sendTyping(int toUserId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "typing");
            msg.put("toUserId", toUserId);
            sendRaw(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建typing消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送已读回执
     * @param channelId 频道ID
     */
    public void sendReadReceipt(int channelId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "read");
            msg.put("channelId", channelId);
            sendRaw(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建read消息失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：构造并发送聊天消息
     */
    private void sendMessageInternal(String type, int toUserId, String content, Integer channelId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", type);
            msg.put("content", content);

            if (toUserId > 0) {
                msg.put("toUserId", toUserId);
                msg.put("channelId", JSONObject.NULL);  // 私聊时为null
            } else if (channelId != null && channelId > 0) {
                msg.put("channelId", channelId);
                msg.put("toUserId", JSONObject.NULL);  // 群聊时为null
            } else {
                // 广播
                msg.put("toUserId", JSONObject.NULL);
                msg.put("channelId", JSONObject.NULL);
            }

            sendRaw(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建消息失败: " + e.getMessage());
        }
    }

    /**
     * 底层发送原始文本
     */
    private boolean sendRaw(String text) {
        if (!isConnected()) {
            Log.w(TAG, "WebSocket 未连接，无法发送消息");
            notifyError("未连接到服务器");
            return false;
        }
        boolean sent = webSocket.send(text);
        Log.d(TAG, "发送消息: " + (sent ? "成功" : "失败"));
        return sent;
    }

    // ==================== 消息接收处理 ====================

    /**
     * 解析收到的消息并分发
     */
    private void handleMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String type = json.optString("type", "unknown");

            switch (type) {
                case "auth":
                    // 认证响应（一般不会收到，认证通过后会收到system欢迎消息）
                    break;

                case "system":
                    handleSystemMessage(json);
                    break;

                case "chat":
                    handleChatMessage(json);
                    break;

                case "online":
                    handleOnlineNotification(json);
                    break;

                case "offline":
                    handleOfflineNotification(json);
                    break;

                case "typing":
                    handleTypingNotification(json);
                    break;

                case "online_list":
                    handleOnlineList(json);
                    break;

                case "error":
                    handleError(json);
                    break;

                default:
                    Log.w(TAG, "未知消息类型: " + type);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析消息JSON失败: " + e.getMessage());
        }
    }

    private void handleSystemMessage(JSONObject json) throws JSONException {
        String content = json.getString("content");
        for (WebSocketEventListener listener : listeners) {
            listener.onSystemMessage(content);
        }
    }

    private void handleChatMessage(JSONObject json) throws JSONException {
        int fromUserId = json.optInt("fromUserId", 0);
        String fromUserName = json.optString("fromUserName", "未知用户");
        int toUserId = json.optInt("toUserId", 0);
        String content = json.getString("content");
        long messageId = json.optLong("messageId", 0);
        String timestamp = json.optString("timestamp", "");
        int channelId = json.optInt("channelId", 0);

        for (WebSocketEventListener listener : listeners) {
            listener.onChatMessage(fromUserId, fromUserName, toUserId, content, messageId, timestamp, channelId);
        }
    }

    private void handleOnlineNotification(JSONObject json) throws JSONException {
        int userId = json.getInt("fromUserId");
        String userName = json.getString("fromUserName");

        for (WebSocketEventListener listener : listeners) {
            listener.onUserOnline(userId, userName);
        }
    }

    private void handleOfflineNotification(JSONObject json) throws JSONException {
        int userId = json.getInt("fromUserId");
        String userName = json.optString("fromUserName", "");

        for (WebSocketEventListener listener : listeners) {
            listener.onUserOffline(userId, userName);
        }
    }

    private void handleTypingNotification(JSONObject json) throws JSONException {
        int fromUserId = json.getInt("fromUserId");
        String fromUserName = json.getString("fromUserName");
        int toUserId = json.getInt("toUserId");

        for (WebSocketEventListener listener : listeners) {
            listener.onUserTyping(fromUserId, fromUserName, toUserId);
        }
    }

    private void handleOnlineList(JSONObject json) throws JSONException {
        String contentStr = json.getString("content");
        JSONArray onlineArray = new JSONArray(contentStr);
        List<OnlineUser> onlineUsers = new ArrayList<>();

        for (int i = 0; i < onlineArray.length(); i++) {
            JSONObject userObj = onlineArray.getJSONObject(i);
            OnlineUser user = new OnlineUser(
                    userObj.optInt("userId", 0),
                    userObj.optString("userName", ""),
                    userObj.optString("nickname", ""),
                    userObj.optString("avatar", "")
            );
            onlineUsers.add(user);
        }

        for (WebSocketEventListener listener : listeners) {
            listener.onOnlineListUpdated(onlineUsers);
        }
    }

    private void handleError(JSONObject json) throws JSONException {
        String errorMsg = json.optString("content", "未知错误");
        notifyError(errorMsg);
    }

    // ==================== 监听器管理 ====================

    public void addListener(WebSocketEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(WebSocketEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyStateChange(ConnectionState state) {
        for (WebSocketEventListener listener : listeners) {
            listener.onConnectionStateChanged(state);
        }
    }

    private void notifyError(String error) {
        notifyError(error, false);
    }

    private void notifyError(String error, boolean isEndpointNotFound) {
        for (WebSocketEventListener listener : listeners) {
            listener.onError(error, isEndpointNotFound);
        }
    }

    // ==================== 数据模型 ====================

    /**
     * 在线用户信息
     */
    public static class OnlineUser {
        public int userId;
        public String userName;
        public String nickname;
        public String avatar;

        public OnlineUser(int userId, String userName, String nickname, String avatar) {
            this.userId = userId;
            this.userName = userName;
            this.nickname = nickname;
            this.avatar = avatar;
        }
    }

    // ==================== 事件监听接口 ====================

    /**
     * WebSocket 事件监听接口
     * 实现此接口即可接收所有WebSocket事件
     */
    public interface WebSocketEventListener {
        /** 连接状态变化 */
        default void onConnectionStateChanged(ConnectionState state) {}

        /** 系统消息（如欢迎信息） */
        default void onSystemMessage(String content) {}

        /** 收到聊天消息 */
        default void onChatMessage(int fromUserId, String fromUserName, int toUserId,
                                   String content, long messageId, String timestamp, int channelId) {}

        /** 用户上线通知 */
        default void onUserOnline(int userId, String userName) {}

        /** 用户下线通知 */
        default void onUserOffline(int userId, String userName) {}

        /** 用户正在输入 */
        default void onUserTyping(int fromUserId, String fromUserName, int toUserId) {}

        /** 在线用户列表更新 */
        default void onOnlineListUpdated(List<OnlineUser> users) {}

        /** 错误信息
         * @param error 错误描述
         * @param isEndpointNotFound 是否为端点不存在（如404），此时不应自动重连 */
        default void onError(String error, boolean isEndpointNotFound) {
            // 向后兼容：默认只传 error
            onError(error);
        }

        /** 错误信息（向后兼容） */
        default void onError(String error) {}
    }
}
