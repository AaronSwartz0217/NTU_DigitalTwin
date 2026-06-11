package com.ntu.qidong.digitaltwin.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.service.ServerApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Request;

public class AIFragment extends Fragment {

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private ImageView btnClearChat;
    private ProgressBar progressLoading;

    // 登录检查相关
    private View loginRequiredView;
    private View chatContainerView;
    private SharedPreferences userPrefs;
    private AppDatabase db;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    // 讯飞Spark X2 WebSocket API配置
    private static final String HOST = "spark-api.xf-yun.com";
    private static final String PATH = "/x2";
    private static final String WS_URL = "wss://" + HOST + PATH;
    private static final String APP_ID = "5941b320";
    private static final String API_KEY = "e286ff09f39a8b4662e2824ae3ae81c7";
    private static final String API_SECRET = "ZmNjYTdlNTU1NGZjNjg1MWI0NDRlOGVi";

    private OkHttpClient client;
    private WebSocket webSocket;
    private boolean isGenerating = false;
    private StringBuilder currentReply = new StringBuilder();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        // 初始化用户偏好和数据库
        userPrefs = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        db = AppDatabase.getInstance(getActivity());

        // 查找视图
        loginRequiredView = view.findViewById(R.id.login_required_view);
        chatContainerView = view.findViewById(R.id.chat_container_view);

        // 检查登录状态
        checkLoginStatus(view);

        return view;
    }

    /**
     * 检查登录状态并切换界面
     */
    private void checkLoginStatus(View view) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());

        if (!serverApi.isLoggedIn()) {
            // 未登录（无JWT Token），显示登录提示界面
            showLoginRequiredView(view);
        } else {
            // 已登录（有Token），显示聊天界面
            showChatView(view);
        }
    }

    /**
     * 显示需要登录的提示界面
     */
    private void showLoginRequiredView(View view) {
        loginRequiredView.setVisibility(View.VISIBLE);
        chatContainerView.setVisibility(View.GONE);

        // 设置"去登录"按钮点击事件
        TextView btnGoToLogin = view.findViewById(R.id.btn_go_to_login);
        btnGoToLogin.setOnClickListener(v -> {
            // 跳转到个人中心页面（通过MainContainerActivity）
            if (getActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) getActivity()).showFragment(
                        ((MainContainerActivity) getActivity()).getProfileFragment()
                );  // 跳转到"我的"页面
                Toast.makeText(getContext(), "请先登录后再使用AI助手", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 显示聊天界面
     */
    private void showChatView(View view) {
        loginRequiredView.setVisibility(View.GONE);
        chatContainerView.setVisibility(View.VISIBLE);

        initViews(view);
        initRecyclerView();
        initOkHttp();
        addWelcomeMessage();
    }

    private void initViews(View view) {
        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        etMessageInput = view.findViewById(R.id.et_message_input);
        btnSend = view.findViewById(R.id.btn_send);
        btnClearChat = view.findViewById(R.id.btn_clear_chat);
        progressLoading = view.findViewById(R.id.progress_loading);

        btnSend.setOnClickListener(v -> sendMessage());
        btnClearChat.setOnClickListener(v -> clearChat());
    }

    private void initRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatMessages.setAdapter(chatAdapter);

        // 设置Item动画
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(300);
        animator.setRemoveDuration(300);
        animator.setChangeDuration(300);
        animator.setMoveDuration(300);
        rvChatMessages.setItemAnimator(animator);
    }

    private void initOkHttp() {
        client = new OkHttpClient.Builder()
                .build();
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMsg = new ChatMessage(
                "ai",
                "你好！我是南通大学启东校区小助手，基于讯飞星火Spark X2模型。\n\n我可以帮你解答关于校园生活、课程安排、数字孪生等各类问题。请问有什么可以帮你的？",
                System.currentTimeMillis()
        );
        messageList.add(welcomeMsg);
        chatAdapter.notifyItemInserted(0);
        scrollToBottom();
    }

    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(getContext(), "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGenerating) {
            Toast.makeText(getContext(), "正在生成回复，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加用户消息
        ChatMessage userMsg = new ChatMessage("user", content, System.currentTimeMillis());
        messageList.add(userMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // 发送按钮动画
        btnSend.animate()
                .rotation(360f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(200)
                .withEndAction(() -> {
                    btnSend.setRotation(0f);
                    btnSend.setScaleX(1f);
                    btnSend.setScaleY(1f);
                })
                .start();

        etMessageInput.setText("");

        isGenerating = true;
        progressLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        currentReply.setLength(0);
        connectAndSend(content);
    }

    /**
     * 连接WebSocket并发送消息
     */
    private void connectAndSend(String userMessage) {
        try {
            String authUrl = createAuthUrl();

            Request request = new Request.Builder()
                    .url(authUrl)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                    // 连接成功，发送消息
                    sendChatRequest(userMessage);
                }

                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                    handleResponse(text);
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                    showError("连接失败：" + t.getMessage());
                }

                @Override
                public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    if (isGenerating && currentReply.length() > 0) {
                        finishCurrentReply();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("创建连接失败：" + e.getMessage());
        }
    }

    /**
     * 生成鉴权URL
     */
    private String createAuthUrl() throws Exception {
        // 1. 生成RFC1123格式的date
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        // 2. 构建签名字符串
        StringBuilder tmp = new StringBuilder();
        tmp.append("host: ").append(HOST).append("\n");
        tmp.append("date: ").append(date).append("\n");
        tmp.append("GET ").append(PATH).append(" HTTP/1.1");

        // 3. HMAC-SHA256签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                API_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(tmp.toString().getBytes(StandardCharsets.UTF_8));
        String signature = android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP);

        // 4. 构建authorization_origin
        String authorizationOrigin = String.format(
                "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                API_KEY, signature);

        // 5. Base64编码得到最终authorization
        String authorization = android.util.Base64.encodeToString(
                authorizationOrigin.getBytes(StandardCharsets.UTF_8),
                android.util.Base64.NO_WRAP);

        // 6. 组装最终URL
        return WS_URL + "?"
                + "authorization=" + URLEncoder.encode(authorization, "UTF-8")
                + "&date=" + URLEncoder.encode(date, "UTF-8")
                + "&host=" + URLEncoder.encode(HOST, "UTF-8");
    }

    /**
     * 发送聊天请求（按讯飞官方格式）
     */
    private void sendChatRequest(String userMessage) {
        try {
            JSONObject requestBody = new JSONObject();

            // header部分
            JSONObject header = new JSONObject();
            header.put("app_id", APP_ID);
            header.put("uid", "user_" + System.currentTimeMillis());
            requestBody.put("header", header);

            // parameter部分
            JSONObject parameter = new JSONObject();
            JSONObject chatParam = new JSONObject();
            chatParam.put("domain", "spark-x");
            chatParam.put("temperature", 0.7);
            chatParam.put("max_tokens", 4096);
            chatParam.put("top_k", 5);
            parameter.put("chat", chatParam);
            requestBody.put("parameter", parameter);

            // payload部分 - 构建消息历史
            JSONArray textArray = new JSONArray();

            // 系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是南通大学启东校区小助手，基于讯飞星火Spark X2模型。你可以回答关于校园生活、课程安排、数字孪生技术、建筑管理、天气信息等各类问题。请用简洁友好、专业准确的中文回答。");
            textArray.put(systemMsg);

            // 历史对话（最近10轮）
            int startIndex = Math.max(0, messageList.size() - 20);
            for (int i = startIndex; i < messageList.size(); i++) {
                ChatMessage msg = messageList.get(i);
                String role = msg.getRole();
                // 转换角色名：ai/ai_generating -> assistant
                if ("ai".equals(role) || "ai_generating".equals(role)) {
                    role = "assistant";
                }
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", role);
                msgObj.put("content", msg.getContent());
                textArray.put(msgObj);
            }

            // 当前用户消息
            JSONObject currentUserMsg = new JSONObject();
            currentUserMsg.put("role", "user");
            currentUserMsg.put("content", userMessage);
            textArray.put(currentUserMsg);

            JSONObject payload = new JSONObject();
            JSONObject message = new JSONObject();
            message.put("text", textArray);
            payload.put("message", message);
            requestBody.put("payload", payload);

            webSocket.send(requestBody.toString());

        } catch (Exception e) {
            e.printStackTrace();
            showError("构建请求失败：" + e.getMessage());
        }
    }

    /**
     * 处理WebSocket响应
     */
    private void handleResponse(String text) {
        try {
            JSONObject json = new JSONObject(text);
            JSONObject header = json.optJSONObject("header");

            if (header != null) {
                int code = header.optInt("code", -1);
                if (code != 0) {
                    String errorMsg = header.optString("message", "未知错误");
                    showError("API错误[" + code + "]：" + errorMsg);
                    return;
                }
            }

            JSONObject payload = json.optJSONObject("payload");
            if (payload == null) return;

            JSONObject choices = payload.optJSONObject("choices");
            if (choices == null) return;

            int status = choices.optInt("status", 0);
            JSONArray textArr = choices.optJSONArray("text");

            if (textArr != null && textArr.length() > 0) {
                JSONObject textObj = textArr.getJSONObject(0);
                String content = textObj.optString("content", "");

                if (!content.isEmpty()) {
                    currentReply.append(content);

                    mainHandler.post(() -> updateOrAddAIMessage(currentReply.toString()));
                }
            }

            // status=2 表示最后一个结果
            if (status == 2) {
                finishCurrentReply();
                webSocket.close(1000, "normal close");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOrAddAIMessage(String content) {
        // 检查最后一条是否是AI正在生成的消息
        int lastIndex = messageList.size() - 1;
        if (lastIndex >= 0) {
            ChatMessage lastMsg = messageList.get(lastIndex);
            if ("ai_generating".equals(lastMsg.getRole())) {
                lastMsg.setContent(content);
                chatAdapter.notifyItemChanged(lastIndex);
                scrollToBottom();
                return;
            }
        }

        // 新增一条生成中的消息
        ChatMessage aiMsg = new ChatMessage("ai_generating", content, System.currentTimeMillis());
        messageList.add(aiMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void finishCurrentReply() {
        mainHandler.post(() -> {
            int lastIndex = messageList.size() - 1;
            if (lastIndex >= 0) {
                ChatMessage lastMsg = messageList.get(lastIndex);
                if ("ai_generating".equals(lastMsg.getRole())) {
                    lastMsg.setRole("ai");
                    lastMsg.setContent(currentReply.toString());
                    chatAdapter.notifyItemChanged(lastIndex);
                }
            }

            isGenerating = false;
            progressLoading.setVisibility(View.GONE);
            btnSend.setEnabled(true);
        });
    }

    private void showError(String error) {
        mainHandler.post(() -> {
            ChatMessage errorMsg = new ChatMessage(
                    "ai",
                    "抱歉，出现了问题：\n" + error + "\n\n请稍后重试或检查网络连接。",
                    System.currentTimeMillis()
            );
            messageList.add(errorMsg);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();

            isGenerating = false;
            progressLoading.setVisibility(View.GONE);
            btnSend.setEnabled(true);
        });
    }

    private void clearChat() {
        messageList.clear();
        chatAdapter.notifyDataSetChanged();
        addWelcomeMessage();
        Toast.makeText(getContext(), "聊天记录已清空", Toast.LENGTH_SHORT).show();
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            rvChatMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocket != null) {
            webSocket.close(1000, "fragment destroyed");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时重新检查登录状态（用户可能刚完成登录）
        if (loginRequiredView != null && chatContainerView != null) {
            checkLoginStatus(getView());
        }
    }

    // ==================== 数据模型 ====================

    public static class ChatMessage {
        private String role;      // "user", "ai", or "ai_generating"
        private String content;
        private long timestamp;

        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public long getTimestamp() { return timestamp; }
    }

    // ==================== 适配器 ====================

    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_USER = 1;
        private static final int TYPE_AI = 2;

        private List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = messages.get(position);
            String role = msg.getRole();
            return "user".equals(role) ? TYPE_USER : TYPE_AI;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());

            if (viewType == TYPE_USER) {
                View view = inflater.inflate(R.layout.item_chat_message_user, parent, false);
                return new UserViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_chat_message_ai, parent, false);
                return new AIViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);

            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).bind(msg);
            } else if (holder instanceof AIViewHolder) {
                ((AIViewHolder) holder).bind(msg);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;

        UserViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(ChatMessage msg) {
            tvContent.setText(msg.getContent());
            tvTime.setText(formatTime(msg.getTimestamp()));
        }
    }

    private class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;

        AIViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(ChatMessage msg) {
            tvContent.setText(msg.getContent());
            tvTime.setText(formatTime(msg.getTimestamp()));
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }
}
