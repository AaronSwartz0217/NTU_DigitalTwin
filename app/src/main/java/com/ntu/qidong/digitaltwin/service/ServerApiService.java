package com.ntu.qidong.digitaltwin.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ntu.qidong.digitaltwin.model.AccountProfile;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 服务器API服务类
 * 连接 Chat.Server (ASP.NET Core)
 * 使用 JWT Token 认证
 *
 * 实际API端点:
 * - POST /api/auth/login          登录获取Token
 * - GET  /api/users/me            获取当前用户资料
 * - PUT  /api/users/me            更新个人资料
 */
public class ServerApiService {
    private static final String TAG = "ServerApiService";

    // 服务器配置 - 已更新为实际后端地址
    // 注意：
    //   - 真机调试：使用电脑的局域网IP，如 http://192.168.x.x:5002
    //   - 模拟器调试：使用 10.0.2.2:5002 访问宿主机
    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:5002";
    public static final int CONNECT_TIMEOUT = 10;  // 秒
    public static final int READ_TIMEOUT = 15;     // 秒

    private OkHttpClient client;
    private Context context;
    private SharedPreferences serverPrefs;
    private SharedPreferences authPrefs;  // 用于存储认证信息

    // JSON媒体类型
    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // 单例
    private static ServerApiService instance;

    public static synchronized ServerApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ServerApiService(context.getApplicationContext());
        }
        return instance;
    }

    private ServerApiService(Context context) {
        this.context = context;
        this.serverPrefs = context.getSharedPreferences("server_config", Context.MODE_PRIVATE);
        this.authPrefs = context.getSharedPreferences("auth_config", Context.MODE_PRIVATE);

        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 设置服务器地址
     */
    public void setServerUrl(String url) {
        serverPrefs.edit().putString("server_url", url).apply();
    }

    /**
     * 获取服务器地址
     */
    public String getServerUrl() {
        return serverPrefs.getString("server_url", DEFAULT_SERVER_URL);
    }

    // ==================== Token管理 ====================

    /**
     * 保存JWT Token到本地
     */
    public void saveAuthToken(String token) {
        authPrefs.edit().putString("jwt_token", token).apply();
        Log.d(TAG, "JWT Token已保存");
    }

    /**
     * 获取本地保存的JWT Token
     */
    public String getAuthToken() {
        return authPrefs.getString("jwt_token", null);
    }

    /**
     * 清除Token（登出时调用）
     */
    public void clearAuthToken() {
        authPrefs.edit().remove("jwt_token").apply();
        Log.d(TAG, "JWT Token已清除");
    }

    /**
     * 检查是否已登录（有有效Token）
     */
    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.isEmpty();
    }

    /**
     * 创建带认证头的Request.Builder
     */
    private Request.Builder createAuthenticatedRequestBuilder(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        String token = getAuthToken();
        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        return builder;
    }

    // ==================== API接口方法 ====================

    /**
     * 测试服务器连接
     * GET /api/posts (公开接口，无需认证)
     */
    public void testConnection(ConnectionCallback callback) {
        String url = getServerUrl() + "/api/posts";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "连接失败: " + e.getMessage());
                callback.onResult(false, "连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();  // 关闭响应体，不需要读取内容
                if (response.isSuccessful()) {
                    callback.onResult(true, "连接成功 - 服务器运行正常");
                } else if (response.code() == 401 || response.code() == 403) {
                    // 401/403也说明服务器可达，只是需要认证
                    callback.onResult(true, "连接成功 - 需要认证的接口");
                } else {
                    callback.onResult(false, "服务器返回错误: HTTP " + response.code());
                }
            }
        });
    }

    /**
     * 用户登录
     * POST /api/auth/login
     * Body: {"userName": "...", "password": "..."}
     *
     * 响应示例:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIs...",
     *   "user": { ... }
     * }
     */
    public void login(String username, String password, LoginCallback callback) {
        String url = getServerUrl() + "/api/auth/login";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("userName", username);
            jsonBody.put("password", password);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "登录请求失败: " + e.getMessage());
                    callback.onResult(false, "网络错误: " + e.getMessage(), null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    handleLoginResponse(response, callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "构建登录请求失败: " + e.getMessage());
            callback.onResult(false, "构建请求失败: " + e.getMessage(), null);
        }
    }

    /**
     * 处理登录响应
     */
    private void handleLoginResponse(Response response, LoginCallback callback) throws IOException {
        String responseBody = "";
        try {
            responseBody = response.body().string();
        } catch (Exception e) {
            Log.e(TAG, "读取响应体失败: " + e.getMessage());
        }

        Log.d(TAG, "登录响应 [HTTP " + response.code() + "]: " + responseBody);

        try {
            // 首先检查HTTP状态码
            if (!response.isSuccessful()) {
                String errorMsg = "登录失败";
                
                // 尝试从响应体提取错误信息
                if (responseBody != null && !responseBody.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("message")) {
                            errorMsg = json.getString("message");
                        } else if (json.has("msg")) {
                            errorMsg = json.getString("msg");
                        } else if (json.has("error")) {
                            errorMsg = json.getString("error");
                        }
                    } catch (Exception e) {
                        // 响应体不是JSON，使用原始内容
                        if (responseBody.length() < 100) {
                            errorMsg = responseBody;
                        }
                    }
                }
                
                // 如果还是没有具体信息，根据状态码设置默认消息
                switch (response.code()) {
                    case 400:
                        if (errorMsg.equals("登录失败")) errorMsg = "请求参数错误";
                        break;
                    case 401:
                        if (errorMsg.equals("登录失败")) errorMsg = "用户名或密码错误";
                        break;
                    case 403:
                        if (errorMsg.equals("登录失败")) errorMsg = "账号已被禁用";
                        break;
                    case 404:
                        if (errorMsg.equals("登录失败")) errorMsg = "登录接口不存在";
                        break;
                    case 429:
                        if (errorMsg.equals("登录失败")) errorMsg = "操作过于频繁，请稍后再试";
                        break;
                    case 500:
                    case 502:
                    case 503:
                        if (errorMsg.equals("登录失败")) errorMsg = "服务器内部错误";
                        break;
                    default:
                        if (errorMsg.equals("登录失败")) errorMsg = "服务器错误 (HTTP " + response.code() + ")";
                }

                Log.w(TAG, "登录失败: " + errorMsg);
                callback.onResult(false, errorMsg, null);
                return;
            }

            // HTTP 200-299，尝试解析成功响应
            if (responseBody == null || responseBody.isEmpty()) {
                Log.e(TAG, "登录响应体为空");
                callback.onResult(false, "服务器返回空数据", null);
                return;
            }

            JSONObject json = new JSONObject(responseBody);

            AccountProfile account = null;
            String token = null;
            boolean success = true;
            String message = "";

            // ========== 支持多种后端响应格式 ==========
            
            // 格式1: Furion统一结果包装 { statusCode, data: {...}, succeeded }
            // 格式2: 直接返回 { token, user, success, message }
            
            JSONObject dataObj = null;
            
            if (json.has("data") && !json.isNull("data")) {
                Object data = json.get("data");
                if (data instanceof JSONObject) {
                    dataObj = (JSONObject) data;
                    Log.d(TAG, "检测到Furion统一结果格式");
                }
            }
            
            // 确定实际的数据源（优先使用data内部的对象）
            JSONObject sourceData = (dataObj != null) ? dataObj : json;

            // 1. 提取Token（支持 accessToken / token 字段）
            if (sourceData.has("accessToken")) {
                token = sourceData.optString("accessToken", "");
                Log.d(TAG, "从accessToken字段获取Token");
            } else if (sourceData.has("token")) {
                token = sourceData.optString("token", "");
                Log.d(TAG, "从token字段获取Token");
            } else if (json.has("accessToken")) {
                token = json.optString("accessToken", "");
                Log.d(TAG, "从根级别accessToken获取Token");
            }

            // 保存有效的Token
            if (token != null && !token.isEmpty() && !"null".equals(token)) {
                saveAuthToken(token);
                Log.d(TAG, "JWT Token已保存到本地 (长度: " + token.length() + ")");
            } else {
                Log.w(TAG, "未找到有效Token或Token为空");
                token = null;
            }

            // 2. 提取用户信息（支持 userInfo / user / account 字段）
            if (sourceData.has("userInfo")) {
                account = parseUserFromJson(sourceData.getJSONObject("userInfo"));
                Log.d(TAG, "从userInfo字段解析用户信息");
            } else if (sourceData.has("user")) {
                account = parseUserFromJson(sourceData.getJSONObject("user"));
                Log.d(TAG, "从user字段解析用户信息");
            } else if (sourceData.has("account")) {
                account = parseUserFromJson(sourceData.getJSONObject("account"));
                Log.d(TAG, "从account字段解析用户信息");
            } else if (json.has("user")) {
                account = parseUserFromJson(json.getJSONObject("user"));
                Log.d(TAG, "从根级别user字段解析用户信息");
            } else if (json.has("account")) {
                account = parseUserFromJson(json.getJSONObject("account"));
            }

            // 3. 提取成功/失败状态
            if (sourceData.has("success")) {
                success = sourceData.optBoolean("success", true);
            } else if (json.has("success")) {
                success = json.optBoolean("success", true);
            } else if (json.has("succeeded")) {
                success = json.optBoolean("succeeded", true);
            }

            // 4. 提取消息
            if (sourceData.has("message")) {
                message = sourceData.optString("message", "");
            } else if (sourceData.has("msg")) {
                message = sourceData.optString("msg", "");
            } else if (json.has("message")) {
                message = json.optString("message", "");
            } else if (json.has("msg")) {
                message = json.optString("msg", "");
            }

            // 5. 最终判断：是否真的成功
            boolean hasValidToken = (token != null && !token.isEmpty());
            boolean hasUserInfo = (account != null);
            
            if (!success || (!hasValidToken && !hasUserInfo)) {
                // 明确失败或既没有Token也没有用户信息
                success = false;
                if (message.isEmpty()) {
                    message = "登录失败";
                }
                Log.w(TAG, "登录被拒绝 - success=" + success + ", token=" + hasValidToken + ", user=" + hasUserInfo);
            }

            if (success) {
                Log.d(TAG, "✅ 登录成功! 用户: " + (account != null ? account.getUserName() : "未知") + 
                      ", Token: " + (hasValidToken ? "已保存" : "无"));
                callback.onResult(true,
                        message.isEmpty() ? "登录成功" : message,
                        account);
            } else {
                Log.w(TAG, "❌ 登录失败: " + message);
                callback.onResult(false,
                        message.isEmpty() ? "登录失败" : message,
                        null);
            }

        } catch (Exception e) {
            Log.e(TAG, "解析登录响应异常: " + e.getMessage(), e);
            callback.onResult(false, "服务器响应格式异常: " + e.getMessage(), null);
        }
    }

    /**
     * 刷新Token
     * POST /api/auth/refresh
     */
    public void refreshToken(TokenRefreshCallback callback) {
        String url = getServerUrl() + "/api/auth/refresh";

        // 可能需要当前Token作为参数（取决于后端实现）
        RequestBody body = RequestBody.create("{}", JSON_MEDIA_TYPE);  // 空对象或带旧token

        Request request = createAuthenticatedRequestBuilder(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "刷新Token失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "刷新Token响应: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    boolean success = json.optBoolean("success", response.isSuccessful());
                    String newToken = json.optString("token", "");

                    if (success && !newToken.isEmpty()) {
                        saveAuthToken(newToken);
                        callback.onResult(true, "刷新成功", newToken);
                    } else {
                        String msg = json.optString("message", json.optString("msg", "刷新失败"));
                        callback.onResult(false, msg, null);
                    }
                } catch (Exception e) {
                    callback.onResult(false, "解析响应失败: " + e.getMessage(), null);
                }
            }
        });
    }

    /**
     * 获取当前用户资料
     * GET /api/users/me
     * Header: Authorization: Bearer <token>
     */
    public void getCurrentUser(ProfileCallback callback) {
        String url = getServerUrl() + "/api/users/me";

        Request request = createAuthenticatedRequestBuilder(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取用户资料失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleProfileResponse(response, callback);
            }
        });
    }

    /**
     * 更新/补充个人资料
     * PUT /api/users/me
     * Header: Authorization: Bearer <token>
     * Body: {需要更新的字段...}
     */
    public void updateCurrentUser(AccountProfile profile, ProfileUpdateCallback callback) {
        String url = getServerUrl() + "/api/users/me";

        try {
            JSONObject jsonBody = new JSONObject();

            // 只传需要更新的字段（非null才传）
            if (profile.getName() != null)
                jsonBody.put("name", profile.getName());
            if (profile.getNo() != null)
                jsonBody.put("no", profile.getNo());
            if (profile.getIdNumber() != null)
                jsonBody.put("idNumber", profile.getIdNumber());
            if (profile.getGender() != null)
                jsonBody.put("gender", profile.getGender());
            if (profile.getEthnicGroup() != null)
                jsonBody.put("ethnicGroup", profile.getEthnicGroup());
            if (profile.getNativePlace() != null)
                jsonBody.put("nativePlace", profile.getNativePlace());
            if (profile.getBirthday() != null)
                jsonBody.put("birthday", profile.getBirthday());
            if (profile.getWeight() != null)
                jsonBody.put("weight", profile.getWeight());
            if (profile.getHeight() != null)
                jsonBody.put("height", profile.getHeight());

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);

            Request request = createAuthenticatedRequestBuilder(url)
                    .put(body)  // 注意：使用PUT方法
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "更新资料失败: " + e.getMessage());
                    callback.onResult(false, "网络错误: " + e.getMessage(), null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    handleUpdateProfileResponse(response, callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "构建更新请求失败: " + e.getMessage());
            callback.onResult(false, "构建请求失败: " + e.getMessage(), null);
        }
    }

    /**
     * 登出
     * POST /api/auth/logout
     * 同时清除本地Token
     */
    public void logout(SimpleCallback callback) {
        String url = getServerUrl() + "/api/auth/logout";

        Request request = createAuthenticatedRequestBuilder(url)
                .post(RequestBody.create("", JSON_MEDIA_TYPE))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 即使网络请求失败，也清除本地Token
                clearAuthToken();
                Log.d(TAG, "登出完成（网络异常但已清除本地Token）");
                callback.onResult(true, "已登出");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 无论服务端是否成功，都清除本地Token
                clearAuthToken();
                response.close();
                Log.d(TAG, "登出成功");
                callback.onResult(true, "登出成功");
            }
        });
    }

    // ==================== 工具方法 ====================

    /**
     * 从JSON解析用户资料对象
     * 兼容多种可能的字段名
     */
    private AccountProfile parseUserFromJson(JSONObject userJson) throws Exception {
        AccountProfile profile = new AccountProfile();

        // 用户ID（可能叫 userId, id, Id）
        long userId = userJson.optLong("userId", -1);
        if (userId == -1) userId = userJson.optLong("id", -1);
        if (userId == -1) userId = userJson.optLong("Id", -1);
        profile.setUserId(userId);

        // 用户名
        String userName = userJson.optString("userName", "");
        if (userName.isEmpty()) userName = userJson.optString("username", "");
        if (userName.isEmpty()) userName = userJson.optString("name", "");
        profile.setUserName(userName);

        // 昵称/显示名（如果有的话，用于UI显示）
        String nickname = userJson.optString("nickname", "");
        if (!nickname.isEmpty() && (profile.getName() == null || profile.getName().isEmpty())) {
            profile.setName(nickname);
        }

        // 学生档案字段（保持与AccountProfileDto兼容）
        if (!userJson.isNull("no"))
            profile.setNo(userJson.optString("no"));
        if (!userJson.isNull("name") && profile.getName() == null)
            profile.setName(userJson.optString("name"));
        if (!userJson.isNull("idNumber") || !userJson.isNull("idnumber"))
            profile.setIdNumber(userJson.optString("idNumber", userJson.optString("idnumber", "")));
        if (!userJson.isNull("gender"))
            profile.setGender(userJson.optInt("gender"));
        if (!userJson.isNull("ethnicGroup"))
            profile.setEthnicGroup(userJson.optInt("ethnicGroup"));
        if (!userJson.isNull("nativePlace"))
            profile.setNativePlace(userJson.optString("nativePlace"));
        if (!userJson.isNull("birthday"))
            profile.setBirthday(userJson.optString("birthday"));
        if (!userJson.isNull("weight"))
            profile.setWeight(userJson.optInt("weight"));
        if (!userJson.isNull("height"))
            profile.setHeight(userJson.optDouble("height"));

        // 检查是否有档案标记（后端返回的hasProfile字段）
        if (userJson.has("hasProfile")) {
            profile.setHasProfile(userJson.optBoolean("hasProfile", false));
        } else {
            // 兼容旧逻辑：根据是否有档案数据判断
            profile.setHasProfile(
                    profile.getName() != null ||
                    profile.getNo() != null ||
                    profile.getIdNumber() != null
            );
        }

        return profile;
    }

    /**
     * URL参数编码
     */
    private String encodeParam(String param) {
        try {
            return java.net.URLEncoder.encode(param, "UTF-8");
        } catch (Exception e) {
            return param;
        }
    }

    /**
     * 处理获取用户资料响应
     */
    private void handleProfileResponse(Response response, ProfileCallback callback) throws IOException {
        String responseBody = response.body().string();
        Log.d(TAG, "获取用户资料响应: " + responseBody);

        try {
            JSONObject json = new JSONObject(responseBody);

            boolean success = response.isSuccessful();
            String message = "";
            AccountProfile account = null;

            // 尝试提取用户数据
            if (json.has("user")) {
                account = parseUserFromJson(json.getJSONObject("user"));
            } else if (json.has("data")) {
                Object dataObj = json.get("data");
                if (dataObj instanceof JSONObject) {
                    account = parseUserFromJson((JSONObject) dataObj);
                }
            } else {
                // 如果整个JSON就是用户数据
                account = parseUserFromJson(json);
            }

            // 提取消息
            if (json.has("message")) {
                message = json.optString("message", "");
            } else if (json.has("msg")) {
                message = json.optString("msg", "");
            }

            // 处理HTTP错误状态
            if (response.code() == 401) {
                success = false;
                message = "登录已过期，请重新登录";
                // 可选：自动尝试刷新Token
            } else if (response.code() == 403) {
                success = false;
                message = "无权限访问";
            }

            callback.onResult(success,
                    message.isEmpty() ? (success ? "获取成功" : "获取失败") : message,
                    account);

        } catch (Exception e) {
            Log.e(TAG, "解析用户资料响应失败: " + e.getMessage());
            callback.onResult(false, "解析响应失败: " + e.getMessage(), null);
        }
    }

    /**
     * 处理更新用户资料响应
     */
    private void handleUpdateProfileResponse(Response response, ProfileUpdateCallback callback) throws IOException {
        String responseBody = response.body().string();
        Log.d(TAG, "更新用户资料响应: " + responseBody);

        try {
            JSONObject json = new JSONObject(responseBody);

            boolean success = response.isSuccessful();
            String message = "";
            AccountProfile account = null;

            // 尝试提取更新后的用户数据
            if (json.has("user")) {
                account = parseUserFromJson(json.getJSONObject("user"));
            } else if (json.has("data")) {
                Object dataObj = json.get("data");
                if (dataObj instanceof JSONObject) {
                    account = parseUserFromJson((JSONObject) dataObj);
                }
            }

            // 提取消息
            if (json.has("message")) {
                message = json.optString("message", "");
            } else if (json.has("msg")) {
                message = json.optString("msg", "");
            }

            callback.onResult(success,
                    message.isEmpty() ? (success ? "更新成功" : "更新失败") : message,
                    account);

        } catch (Exception e) {
            Log.e(TAG, "解析更新响应失败: " + e.getMessage());
            callback.onResult(false, "解析响应失败: " + e.getMessage(), null);
        }
    }

    // ==================== 回调接口 ====================

    /**
     * 连接测试回调
     */
    public interface ConnectionCallback {
        void onResult(boolean success, String message);
    }

    /**
     * 登录回调
     */
    public interface LoginCallback {
        void onResult(boolean success, String message, AccountProfile account);
    }

    /**
     * 用户资料查询回调
     */
    public interface ProfileCallback {
        void onResult(boolean success, String message, AccountProfile account);
    }

    /**
     * 用户资料更新回调
     */
    public interface ProfileUpdateCallback {
        void onResult(boolean success, String message, AccountProfile account);
    }

    // ==================== 帖子/论坛 API ====================

    /**
     * 帖子数据回调（返回JSON数组）
     */
    public interface PostsCallback {
        void onResult(boolean success, String message, org.json.JSONArray posts);
    }

    /**
     * 帖子详情回调（返回单个帖子）
     */
    public interface PostDetailCallback {
        void onResult(boolean success, String message, JSONObject postData);
    }

    /**
     * 单个操作回调（发帖等）
     */
    public interface PostOperationCallback {
        void onResult(boolean success, String message, org.json.JSONObject postData);
    }

    /**
     * 发布新帖子
     * POST /api/posts
     * Body: {"title": "...", "content": "..."}
     * 需要认证 (JWT Token)
     */
    public void createPost(String title, String content, PostOperationCallback callback) {
        String url = getServerUrl() + "/api/posts";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", title);
            jsonBody.put("content", content);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);
            Request request = createAuthenticatedRequestBuilder(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "发帖请求失败: " + e.getMessage());
                    callback.onResult(false, "网络错误: " + e.getMessage(), null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "发帖响应 [HTTP " + response.code() + "]: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject json = new JSONObject(responseBody);
                            // 尝试提取创建的帖子数据
                            JSONObject postData = null;
                            if (json.has("data")) {
                                Object data = json.get("data");
                                if (data instanceof JSONObject) {
                                    postData = (JSONObject) data;
                                }
                            } else if (json.has("post")) {
                                postData = json.getJSONObject("post");
                            } else {
                                postData = json;  // 整个响应作为帖子数据
                            }
                            
                            String message = json.optString("message", "发布成功");
                            callback.onResult(true, message, postData);
                        } else {
                            // 处理错误响应
                            String errorMsg = "发布失败";
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                if (errorJson.has("message")) {
                                    errorMsg = errorJson.getString("message");
                                } else if (errorJson.has("msg")) {
                                    errorMsg = errorJson.getString("msg");
                                }
                            } catch (Exception ignored) {}
                            
                            switch (response.code()) {
                                case 401: errorMsg = "请先登录"; break;
                                case 403: errorMsg = "没有权限发帖"; break;
                                case 429: errorMsg = "操作过于频繁"; break;
                            }
                            callback.onResult(false, errorMsg, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析发帖响应失败: " + e.getMessage());
                        callback.onResult(false, "服务器响应异常", null);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建发帖请求失败: " + e.getMessage());
            callback.onResult(false, "构建请求失败", null);
        }
    }

    /**
     * 获取帖子列表
     * GET /api/posts (公开接口，无需认证)
     * 支持分页和排序参数
     */
    public void getPosts(PostsCallback callback) {
        getPosts(1, 10, "time", null, callback);
    }

    /**
     * 获取帖子列表（带参数）
     * GET /api/posts?pageIndex=1&pageSize=10&sortBy=hot&tag=xxx
     */
    public void getPosts(int pageIndex, int pageSize, String sortBy, String tag, PostsCallback callback) {
        String url = getServerUrl() + "/api/posts?" +
                "pageIndex=" + pageIndex +
                "&pageSize=" + pageSize +
                "&sortBy=" + (sortBy != null ? sortBy : "time");
        
        if (tag != null && !tag.isEmpty()) {
            url += "&tag=" + encodeParam(tag);
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取帖子列表失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "获取帖子列表响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray postsArray = null;

                        // 支持多种格式：{ data: [...] } 或 [...]
                        if (json.has("data")) {
                            Object data = json.get("data");
                            if (data instanceof JSONArray) {
                                postsArray = (JSONArray) data;
                            } else if (data instanceof JSONObject && ((JSONObject) data).has("items")) {
                                postsArray = ((JSONObject) data).getJSONArray("items");
                            }
                        } else if (json.has("posts")) {
                            postsArray = json.getJSONArray("posts");
                        } else if (json.has("items")) {
                            postsArray = json.getJSONArray("items");
                        }

                        if (postsArray != null) {
                            Log.d(TAG, "获取到 " + postsArray.length() + " 条帖子");
                            callback.onResult(true, "获取成功", postsArray);
                        } else {
                            // 返回空数组
                            callback.onResult(true, "暂无帖子", new JSONArray());
                        }
                    } else {
                        callback.onResult(false, "服务器错误 (HTTP " + response.code() + ")", null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析帖子列表失败: " + e.getMessage());
                    callback.onResult(false, "解析响应失败", null);
                }
            }
        });
    }

    /**
     * 获取帖子详情
     * GET /api/posts/{postId} (公开接口，无需认证)
     */
    public void getPostDetail(long postId, PostDetailCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取帖子详情失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "获取帖子详情响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        JSONObject postData = null;

                        // 支持多种格式
                        if (json.has("data")) {
                            Object data = json.get("data");
                            if (data instanceof JSONObject) {
                                postData = (JSONObject) data;
                            }
                        } else {
                            postData = json;
                        }

                        callback.onResult(true, "获取成功", postData);
                    } else {
                        String errorMsg = "获取失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "获取失败"));
                        } catch (Exception ignored) {}
                        callback.onResult(false, errorMsg, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析帖子详情失败: " + e.getMessage());
                    callback.onResult(false, "解析响应失败", null);
                }
            }
        });
    }

    /**
     * 点赞帖子
     * POST /api/posts/{postId}/like (需要认证)
     */
    public void likePost(long postId, SimpleCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/like";

        Request request = createAuthenticatedRequestBuilder(url)
                .post(RequestBody.create("", JSON_MEDIA_TYPE))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "点赞失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "点赞响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String message = json.optString("message", json.optString("msg", "操作成功"));
                        callback.onResult(true, message);
                    } else {
                        String errorMsg = "点赞失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "点赞失败"));
                        } catch (Exception ignored) {}
                        
                        if (response.code() == 401) {
                            errorMsg = "请先登录";
                        }
                        callback.onResult(false, errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析点赞响应失败: " + e.getMessage());
                    callback.onResult(false, "服务器响应异常");
                }
            }
        });
    }

    /**
     * 取消点赞
     * DELETE /api/posts/{postId}/like (需要认证)
     */
    public void unlikePost(long postId, SimpleCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/like";

        Request request = createAuthenticatedRequestBuilder(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "取消点赞失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "取消点赞响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String message = json.optString("message", json.optString("msg", "操作成功"));
                        callback.onResult(true, message);
                    } else {
                        String errorMsg = "取消点赞失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "取消点赞失败"));
                        } catch (Exception ignored) {}
                        
                        if (response.code() == 401) {
                            errorMsg = "请先登录";
                        }
                        callback.onResult(false, errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析取消点赞响应失败: " + e.getMessage());
                    callback.onResult(false, "服务器响应异常");
                }
            }
        });
    }

    /**
     * 收藏帖子
     * POST /api/posts/{postId}/favorite (需要认证)
     */
    public void favoritePost(long postId, SimpleCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/favorite";

        Request request = createAuthenticatedRequestBuilder(url)
                .post(RequestBody.create("", JSON_MEDIA_TYPE))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "收藏失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "收藏响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String message = json.optString("message", json.optString("msg", "收藏成功"));
                        callback.onResult(true, message);
                    } else {
                        String errorMsg = "收藏失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "收藏失败"));
                        } catch (Exception ignored) {}
                        
                        if (response.code() == 401) {
                            errorMsg = "请先登录";
                        }
                        callback.onResult(false, errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析收藏响应失败: " + e.getMessage());
                    callback.onResult(false, "服务器响应异常");
                }
            }
        });
    }

    /**
     * 取消收藏
     * DELETE /api/posts/{postId}/favorite (需要认证)
     */
    public void unfavoritePost(long postId, SimpleCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/favorite";

        Request request = createAuthenticatedRequestBuilder(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "取消收藏失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "取消收藏响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String message = json.optString("message", json.optString("msg", "取消收藏成功"));
                        callback.onResult(true, message);
                    } else {
                        String errorMsg = "取消收藏失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "取消收藏失败"));
                        } catch (Exception ignored) {}
                        
                        if (response.code() == 401) {
                            errorMsg = "请先登录";
                        }
                        callback.onResult(false, errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析取消收藏响应失败: " + e.getMessage());
                    callback.onResult(false, "服务器响应异常");
                }
            }
        });
    }

    // ==================== 评论 API ====================

    /**
     * 评论数据回调
     */
    public interface CommentsCallback {
        void onResult(boolean success, String message, JSONArray comments);
    }

    /**
     * 获取帖子评论列表
     * GET /api/posts/{postId}/comments (公开接口，无需认证)
     */
    public void getComments(long postId, CommentsCallback callback) {
        getComments(postId, 1, 20, callback);
    }

    /**
     * 获取帖子评论列表（带分页）
     * GET /api/posts/{postId}/comments?pageIndex=1&pageSize=20
     */
    public void getComments(long postId, int pageIndex, int pageSize, CommentsCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/comments?" +
                "pageIndex=" + pageIndex +
                "&pageSize=" + pageSize;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取评论失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "获取评论响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray commentsArray = null;

                        if (json.has("data")) {
                            Object data = json.get("data");
                            if (data instanceof JSONArray) {
                                commentsArray = (JSONArray) data;
                            } else if (data instanceof JSONObject && ((JSONObject) data).has("items")) {
                                commentsArray = ((JSONObject) data).getJSONArray("items");
                            }
                        } else if (json.has("comments")) {
                            commentsArray = json.getJSONArray("comments");
                        }

                        if (commentsArray != null) {
                            callback.onResult(true, "获取成功", commentsArray);
                        } else {
                            callback.onResult(true, "暂无评论", new JSONArray());
                        }
                    } else {
                        callback.onResult(false, "服务器错误 (HTTP " + response.code() + ")", null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析评论失败: " + e.getMessage());
                    callback.onResult(false, "解析响应失败", null);
                }
            }
        });
    }

    /**
     * 发表评论
     * POST /api/posts/{postId}/comments (需要认证)
     */
    public void createComment(long postId, String content, Long parentId, PostOperationCallback callback) {
        String url = getServerUrl() + "/api/posts/" + postId + "/comments";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("content", content);
            if (parentId != null && parentId > 0) {
                jsonBody.put("parentId", parentId);
            }

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);
            Request request = createAuthenticatedRequestBuilder(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "发表评论失败: " + e.getMessage());
                    callback.onResult(false, "网络错误: " + e.getMessage(), null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "发表评论响应 [HTTP " + response.code() + "]: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject json = new JSONObject(responseBody);
                            JSONObject commentData = null;
                            
                            if (json.has("data")) {
                                Object data = json.get("data");
                                if (data instanceof JSONObject) {
                                    commentData = (JSONObject) data;
                                }
                            } else {
                                commentData = json;
                            }
                            
                            String message = json.optString("message", json.optString("msg", "评论成功"));
                            callback.onResult(true, message, commentData);
                        } else {
                            String errorMsg = "评论失败";
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMsg = errorJson.optString("message", errorJson.optString("msg", "评论失败"));
                            } catch (Exception ignored) {}
                            
                            if (response.code() == 401) {
                                errorMsg = "请先登录";
                            }
                            callback.onResult(false, errorMsg, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析评论响应失败: " + e.getMessage());
                        callback.onResult(false, "服务器响应异常", null);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建评论请求失败: " + e.getMessage());
            callback.onResult(false, "构建请求失败", null);
        }
    }

    /**
     * 删除评论
     * DELETE /api/comments/{commentId} (需要认证)
     */
    public void deleteComment(long commentId, SimpleCallback callback) {
        String url = getServerUrl() + "/api/comments/" + commentId;

        Request request = createAuthenticatedRequestBuilder(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "删除评论失败: " + e.getMessage());
                callback.onResult(false, "网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "删除评论响应 [HTTP " + response.code() + "]: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String message = json.optString("message", json.optString("msg", "删除成功"));
                        callback.onResult(true, message);
                    } else {
                        String errorMsg = "删除失败";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            errorMsg = errorJson.optString("message", errorJson.optString("msg", "删除失败"));
                        } catch (Exception ignored) {}
                        
                        if (response.code() == 401) {
                            errorMsg = "请先登录";
                        } else if (response.code() == 403) {
                            errorMsg = "没有权限删除此评论";
                        }
                        callback.onResult(false, errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析删除评论响应失败: " + e.getMessage());
                    callback.onResult(false, "服务器响应异常");
                }
            }
        });
    }

    /**
     * Token刷新回调
     */
    public interface TokenRefreshCallback {
        void onResult(boolean success, String message, String newToken);
    }

    /**
     * 简单操作回调（登出等）
     */
    public interface SimpleCallback {
        void onResult(boolean success, String message);
    }
}
