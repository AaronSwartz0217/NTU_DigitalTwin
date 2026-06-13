package com.ntu.qidong.digitaltwin.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.service.ServerApiService;
import com.ntu.qidong.digitaltwin.service.WebSocketService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 公共聊天室Activity - 基于WebSocket的实时通信
 *
 * 后端API:
 *   - WS: ws://localhost:5002/ws  (JWT认证后实时收发消息)
 *   - 消息类型: auth, chat, typing, read, system, online, offline, online_list, error
 */
public class ChatRoomActivity extends AppCompatActivity implements WebSocketService.WebSocketEventListener {

    private static final String TAG = "ChatRoom";
    private static final String PREFS_CHAT_CACHE = "chat_message_cache";
    private static final String KEY_CACHED_MESSAGES = "cached_messages";
    private static final int MAX_CACHE_SIZE = 200;  // 最多缓存200条消息

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView tvOnlineStatus;
    private TextView tvOnlineCountHeader;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    // WebSocket服务
    private WebSocketService wsService;
    private ServerApiService serverApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        initViews();
        setupRecyclerView();
        setupClickListeners();

        serverApi = ServerApiService.getInstance(this);
        wsService = WebSocketService.getInstance(this);

        // 注册事件监听（只注册一次）
        wsService.addListener(this);

        // 检查登录状态，已登录则连接WebSocket
        if (serverApi.isLoggedIn()) {
            connectWebSocket();
        } else {
            updateConnectionStatus(false, "请先登录");
            Toast.makeText(this, "请先登录后再进入聊天室", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        tvOnlineStatus = findViewById(R.id.tv_online_status);
        tvOnlineCountHeader = findViewById(R.id.tv_online_count_header);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);  // 新消息在底部
        rvChatMessages.setLayoutManager(layoutManager);

        // 先加载本地缓存的消息
        loadCachedMessages();

        messageAdapter = new ChatMessageAdapter(this, messageList);
        rvChatMessages.setAdapter(messageAdapter);

        // 滚动到最新消息
        if (!messageList.isEmpty()) {
            rvChatMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    private void setupClickListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 发送按钮
        btnSend.setOnClickListener(v -> sendMessage());

        // 输入框回车发送
        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    // ==================== WebSocket 连接管理 ====================

    /**
     * 建立WebSocket连接并发送JWT认证
     */
    private void connectWebSocket() {
        updateConnectionStatus(false, "连接中...");

        String token = serverApi.getAuthToken();
        Log.d(TAG, "获取Token (长度: " + (token != null ? token.length() : "null") + ")");
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法连接WebSocket");
            updateConnectionStatus(false, "认证失败：无Token");
            return;
        }

        // 同步服务器地址
        wsService.setServerUrl(serverApi.getServerUrl());

        // 开始连接（连接成功后会自动发送auth消息）
        wsService.connect(token);
    }

    /**
     * 断开WebSocket连接
     */
    private void disconnectWebSocket() {
        if (wsService != null) {
            wsService.removeListener(this);
            wsService.disconnect();
        }
    }

    // ==================== 发送消息 ====================

    /**
     * 发送聊天消息（通过WebSocket）
     */
    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        if (!serverApi.isLoggedIn()) {
            showToast("请先登录");
            return;
        }

        if (!wsService.isConnected()) {
            if (wsService.isUnrecoverable()) {
                showToast("聊天室服务暂未开放");
            } else {
                showToast("未连接到聊天服务器，正在重连...");
                connectWebSocket();
            }
            return;
        }

        // 通过WebSocket发送广播消息（公共聊天室场景）
        wsService.sendBroadcastMessage(content);

        // 本地立即显示发送的消息（乐观更新）
        String userName = getUserName();
        int userId = getCurrentUserId();
        ChatMessage localMsg = new ChatMessage(
                userId,
                userName,
                content,
                System.currentTimeMillis(),
                true  // isSelf = true
        );
        addMessage(localMsg);

        etMessageInput.setText("");
    }

    /**
     * 获取当前用户ID
     */
    private int getCurrentUserId() {
        try {
            ServerApiService.UserProfile profile = serverApi.getCachedUserProfile();
            if (profile != null && profile.userId > 0) {
                return (int) profile.userId;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * 获取当前用户名
     */
    private String getUserName() {
        try {
            ServerApiService.UserProfile profile = serverApi.getCachedUserProfile();
            if (profile != null && profile.userName != null) {
                return profile.userName;
            }
        } catch (Exception ignored) {}
        return "我";
    }

    /**
     * 添加消息到列表并滚动到底部
     */
    private void addMessage(ChatMessage message) {
        runOnUiThread(() -> {
            messageList.add(message);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            rvChatMessages.scrollToPosition(messageList.size() - 1);

            // 异步保存到本地缓存
            saveMessageToCache(message);
        });
    }

    // ==================== 本地消息缓存 ====================

    /**
     * 从 SharedPreferences 加载缓存的聊天消息
     */
    private void loadCachedMessages() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_CHAT_CACHE, MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_CACHED_MESSAGES, "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);

            messageList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ChatMessage msg = new ChatMessage(
                        obj.optInt("userId", 0),
                        obj.optString("userName", "未知用户"),
                        obj.optString("content", ""),
                        obj.optLong("timestamp", 0),
                        obj.optBoolean("isSelf", false)
                );
                msg.messageId = obj.optLong("messageId", 0);
                messageList.add(msg);
            }

            Log.d(TAG, "加载缓存消息: " + messageList.size() + " 条");
        } catch (JSONException e) {
            Log.e(TAG, "加载聊天缓存失败: " + e.getMessage());
        }
    }

    /**
     * 追加一条消息到本地缓存（异步，不阻塞UI）
     */
    private void saveMessageToCache(ChatMessage message) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences(PREFS_CHAT_CACHE, MODE_PRIVATE);
                String jsonStr = prefs.getString(KEY_CACHED_MESSAGES, "[]");
                JSONArray jsonArray = new JSONArray(jsonStr);

                JSONObject obj = new JSONObject();
                obj.put("userId", message.userId);
                obj.put("userName", message.userName);
                obj.put("content", message.content);
                obj.put("timestamp", message.timestamp);
                obj.put("isSelf", message.isSelf);
                obj.put("messageId", message.messageId);

                jsonArray.put(obj);

                // 限制缓存大小，超出则删除旧消息
                while (jsonArray.length() > MAX_CACHE_SIZE) {
                    jsonArray.remove(0);
                }

                prefs.edit().putString(KEY_CACHED_MESSAGES, jsonArray.toString()).apply();
            } catch (JSONException e) {
                Log.e(TAG, "保存聊天缓存失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 清空本地聊天缓存（登出时调用）
     */
    public static void clearChatCache(android.content.Context context) {
        context.getSharedPreferences(PREFS_CHAT_CACHE, MODE_PRIVATE)
                .edit()
                .remove(KEY_CACHED_MESSAGES)
                .apply();
    }

    // ==================== UI 状态更新 ====================

    /**
     * 更新连接状态显示
     */
    private void updateConnectionStatus(boolean connected, String statusText) {
        runOnUiThread(() -> {
            if (tvOnlineStatus != null) {
                tvOnlineStatus.setText(statusText);
                tvOnlineStatus.setTextColor(connected ? 0xFF4ADE80 : 0xFFFBBF24);
            }
        });
    }

    /**
     * 更新在线人数显示
     */
    public void updateOnlineCount(int count) {
        runOnUiThread(() -> {
            if (tvOnlineCountHeader != null) {
                tvOnlineCountHeader.setText(count + "人在线");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ==================== WebSocket 事件回调实现 ====================

    @Override
    public void onConnectionStateChanged(WebSocketService.ConnectionState state) {
        switch (state) {
            case CONNECTING:
                updateConnectionStatus(false, "连接中...");
                break;
            case CONNECTED:
                updateConnectionStatus(true, "已连接");
                break;
            case DISCONNECTED:
                updateConnectionStatus(false, "未连接");
                break;
        }
    }

    @Override
    public void onSystemMessage(String content) {
        Log.d(TAG, "系统消息: " + content);
        runOnUiThread(() -> showToast(content));
    }

    @Override
    public void onChatMessage(int fromUserId, String fromUserName, int toUserId,
                              String content, long messageId, String timestamp, int channelId) {
        Log.d(TAG, "收到消息 from=" + fromUserName + ": " + content);

        // 跳过自己的消息（发送时已做乐观更新显示）
        boolean isSelf = (fromUserId == getCurrentUserId());
        if (isSelf) {
            Log.d(TAG, "跳过自己消息的回显（已通过乐观更新显示）");
            return;
        }
        long msgTime = System.currentTimeMillis();  // 默认用当前时间

        // 尝试解析服务端时间戳
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(timestamp.replace("Z", "+0000"));
                if (date != null) msgTime = date.getTime();
            } catch (Exception e) {
                // 解析失败使用本地时间
            }
        }

        ChatMessage msg = new ChatMessage(fromUserId, fromUserName, content, msgTime, isSelf);
        msg.messageId = messageId;
        addMessage(msg);
    }

    @Override
    public void onUserOnline(int userId, String userName) {
        Log.d(TAG, "用户上线: " + userName);
        runOnUiThread(() -> showToast(userName + " 上线了"));
    }

    @Override
    public void onUserOffline(int userId, String userName) {
        Log.d(TAG, "用户下线: " + userName);
    }

    @Override
    public void onUserTyping(int fromUserId, String fromUserName, int toUserId) {
        // 可以在这里显示"对方正在输入..."提示
        Log.d(TAG, fromUserName + " 正在输入...");
    }

    @Override
    public void onOnlineListUpdated(List<WebSocketService.OnlineUser> users) {
        Log.d(TAG, "在线用户列表更新，共 " + users.size() + " 人");
        updateOnlineCount(users.size());
    }

    @Override
    public void onError(String error, boolean isEndpointNotFound) {
        Log.e(TAG, "WebSocket错误: " + error);
        runOnUiThread(() -> {
            // 检测Token过期/无效，尝试自动刷新
            if (error != null && (error.contains("认证失败") || error.contains("无效的Token") || error.contains("expired"))) {
                updateConnectionStatus(false, "正在刷新登录...");
                showToast("登录已过期，正在重新连接...");

                new Thread(() -> {
                    boolean refreshed = serverApi.refreshAccessTokenSync();
                    runOnUiThread(() -> {
                        if (refreshed) {
                            Log.d(TAG, "Token刷新成功，重新连接WebSocket");
                            wsService.reset();  // 重置失败计数
                            connectWebSocket();
                        } else {
                            Log.e(TAG, "Token刷新失败，需要重新登录");
                            updateConnectionStatus(false, "登录已过期");
                            showToast("登录已过期，请重新登录");
                        }
                    });
                }).start();
                return;
            }

            if (isEndpointNotFound) {
                // 端点不存在（404），显示明确提示
                updateConnectionStatus(false, "聊天服务未启用");
                showToast("聊天室服务暂未开放，请稍后再试");
            } else if (wsService.isUnrecoverable()) {
                // 连续多次连接失败（超时/拒绝等）
                updateConnectionStatus(false, "无法连接服务器");
                showToast("聊天室服务暂时不可用（" + error + "）");
            } else {
                showToast("连接错误: " + error);
            }
        });
    }

    @Override
    public void onError(String error) {
        onError(error, false);
    }

    // ==================== 生命周期管理 ====================

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时检查重连（仅非不可恢复错误时）
        if (serverApi.isLoggedIn() && !wsService.isConnected() && !wsService.isUnrecoverable()) {
            connectWebSocket();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectWebSocket();
    }

    // ==================== 聊天消息数据模型 ====================

    public static class ChatMessage {
        public int userId;
        public String userName;
        public String content;
        public long timestamp;
        public boolean isSelf;
        public long messageId;  // 服务端消息ID

        public ChatMessage(int userId, String userName, String content, long timestamp, boolean isSelf) {
            this.userId = userId;
            this.userName = userName;
            this.content = content;
            this.timestamp = timestamp;
            this.isSelf = isSelf;
            this.messageId = 0;
        }

        // Getter 方法（供 Adapter 使用）
        public boolean isSelf() { return isSelf; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        public String getUserName() { return userName; }

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
