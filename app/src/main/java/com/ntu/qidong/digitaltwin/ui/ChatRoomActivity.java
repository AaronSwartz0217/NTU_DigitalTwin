package com.ntu.qidong.digitaltwin.ui;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.service.ServerApiService;

import java.util.ArrayList;
import java.util.List;

/**
 * 公共聊天室Activity - 类似QQ聊天界面
 * TODO: 后端WebSocket API完成后实现实时通信
 */
public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView tvOnlineStatus;
    private TextView tvOnlineCountHeader;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    // WebSocket连接状态
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        
        // TODO: 后端WebSocket完成后，在这里建立连接
        // connectWebSocket();
        
        updateConnectionStatus(false, "连接中...");
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

        messageAdapter = new ChatMessageAdapter(this, messageList);
        rvChatMessages.setAdapter(messageAdapter);
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

    /**
     * 发送消息
     */
    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        // 检查登录状态
        ServerApiService serverApi = ServerApiService.getInstance(this);
        if (!serverApi.isLoggedIn()) {
            showToast("请先登录");
            return;
        }

        // TODO: 通过WebSocket发送到服务器
        // websocket.send(content);
        
        // 临时：本地显示发送的消息（待WebSocket实现后删除）
        ChatMessage message = new ChatMessage(
                serverApi.getAuthToken(),  // 使用Token作为临时标识
                getUserName(),
                content,
                System.currentTimeMillis(),
                true  // isSelf = true
        );
        
        addMessage(message);
        etMessageInput.setText("");
    }

    /**
     * 获取当前用户名（临时方案）
     */
    private String getUserName() {
        // TODO: 从用户资料中获取真实用户名
        return "我";
    }

    /**
     * 添加消息到列表
     */
    private void addMessage(ChatMessage message) {
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);
    }

    /**
     * 更新连接状态显示
     */
    private void updateConnectionStatus(boolean connected, String statusText) {
        isConnected = connected;
        if (tvOnlineStatus != null) {
            tvOnlineStatus.setText(statusText);
            if (connected) {
                tvOnlineStatus.setTextColor(0xFF4ADE80);  // 绿色
            } else {
                tvOnlineStatus.setTextColor(0xFFFBBF24);  // 黄色
            }
        }
    }

    /**
     * 更新在线人数
     */
    public void updateOnlineCount(int count) {
        if (tvOnlineCountHeader != null) {
            tvOnlineCountHeader.setText(count + "人在线");
        }
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: 断开WebSocket连接
        // disconnectWebSocket();
    }

    // ==================== 聊天消息数据模型 ====================
    
    public static class ChatMessage {
        private String userId;
        private String userName;
        private String content;
        private long timestamp;
        private boolean isSelf;

        public ChatMessage(String userId, String userName, String content, long timestamp, boolean isSelf) {
            this.userId = userId;
            this.userName = userName;
            this.content = content;
            this.timestamp = timestamp;
            this.isSelf = isSelf;
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        public boolean isSelf() { return isSelf; }
    }
}
