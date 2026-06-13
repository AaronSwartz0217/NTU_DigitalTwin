package com.ntu.qidong.digitaltwin.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.service.ServerApiService;
import com.ntu.qidong.digitaltwin.db.Comment;
import com.ntu.qidong.digitaltwin.db.Post;
import com.ntu.qidong.digitaltwin.db.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForumFragment extends Fragment {

    private static final String TAG = "ForumFragment";

    private EditText etTitle, etContent, etComment;
    private Button btnPost, btnCancelPost, btnComment;
    private ImageButton btnBack, btnNewPost;
    private LinearLayout postsContainer, postDetailLayout, postDialogLayout, commentsContainer;
    private ScrollView forumContentView;
    private LinearLayout loginRequiredView;
    private TextView tvWelcome, tvDetailTitle, tvDetailAuthor, tvDetailTime, tvDetailContent, tvDetailLikes, tvDetailComments;
    private TextView tagAll, tagHot, tagNew, tagMy;

    private SharedPreferences userPrefs;
    private AppDatabase db;
    private long currentUserId = -1;
    private boolean isAdmin = false;  // 管理员标识

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);

        initViews(view);
        initData();
        checkLoginStatus(view);

        return view;
    }

    /**
     * Fragment 显示/隐藏时的生命周期回调
     * 解决：从底部导航栏切换回来时，重置帖子详情状态
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            // 切回论坛页面，如果之前在查看详情页则重置
            if (postDetailLayout != null && postDetailLayout.getVisibility() == View.VISIBLE) {
                postDetailLayout.setVisibility(View.GONE);
                currentViewingPostId = -1;
                // 清理动态添加的管理员操作按钮
                View existingActions = postDetailLayout.findViewWithTag("detail_admin_actions");
                if (existingActions != null) {
                    ((android.view.ViewGroup) existingActions.getParent()).removeView(existingActions);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment 可见时刷新数据（处理返回场景）
        if (!isHidden() && isAdded()) {
            // 重新检查登录状态（用户可能在其他页面登录/登出）
            View rootView = getView();
            if (rootView != null) {
                checkLoginStatus(rootView);
                // 重新检查管理员权限
                refreshAdminStatus();
            }

            // 如果在列表页，刷新帖子
            if (postDetailLayout != null && postDetailLayout.getVisibility() != View.VISIBLE) {
                loadPosts();
            }
        }
    }

    /**
     * 刷新管理员状态（登录/登出后重新检测）
     */
    private void refreshAdminStatus() {
        isAdmin = false;

        // 检查本地数据库
        if (currentUserId > 0 && db != null) {
            User currentUser = db.userDao().getUserById(currentUserId);
            if (currentUser != null && currentUser.isAdmin()) {
                isAdmin = true;
                Log.d(TAG, "refreshAdminStatus: 本地DB检测到管理员");
            }
        }

        // 检查后端缓存的用户数据
        if (!isAdmin) {
            ServerApiService serverApi = ServerApiService.getInstance(getActivity());
            if (serverApi.isLoggedIn()) {
                String cachedJson = userPrefs.getString("cached_account_json", null);
                if (cachedJson != null && !cachedJson.isEmpty()) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(cachedJson);
                        String role = json.optString("role", "");
                        int roleInt = json.optInt("roleId", 0);
                        boolean isSuperAdmin = json.optBoolean("isSuperAdmin", false);
                        if ("admin".equals(role) || "Administrator".equals(role) ||
                                roleInt >= 1 || isSuperAdmin || "admin".equals(json.optString("userName", ""))) {
                            isAdmin = true;
                            Log.d(TAG, "refreshAdminStatus: 后端缓存检测到管理员 role=" + role);
                        }
                    } catch (Exception e) {
                        // 解析失败不影响
                    }
                }
            }
        }

        Log.d(TAG, "refreshAdminStatus: 最终 isAdmin=" + isAdmin);
    }

    private void initViews(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etContent = view.findViewById(R.id.et_content);
        etComment = view.findViewById(R.id.et_comment);
        btnPost = view.findViewById(R.id.btn_post);
        btnNewPost = view.findViewById(R.id.btn_new_post);
        btnCancelPost = view.findViewById(R.id.btn_cancel_post);
        btnComment = view.findViewById(R.id.btn_comment);
        btnBack = view.findViewById(R.id.btn_back);

        postsContainer = view.findViewById(R.id.posts_container);
        postDetailLayout = view.findViewById(R.id.post_detail_layout);
        postDialogLayout = view.findViewById(R.id.post_dialog_layout);
        commentsContainer = view.findViewById(R.id.comments_container);
        forumContentView = view.findViewById(R.id.forum_content_view);
        loginRequiredView = view.findViewById(R.id.login_required_view);

        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvDetailTitle = view.findViewById(R.id.tv_detail_title);

        // 初始化标签
        tagAll = view.findViewById(R.id.tag_all);
        tagHot = view.findViewById(R.id.tag_hot);
        tagNew = view.findViewById(R.id.tag_new);
        tagMy = view.findViewById(R.id.tag_my);

        // 标签点击事件（带动画）
        tagAll.setOnClickListener(v -> selectTag(tagAll));
        tagHot.setOnClickListener(v -> selectTag(tagHot));
        tagNew.setOnClickListener(v -> selectTag(tagNew));
        tagMy.setOnClickListener(v -> selectTag(tagMy));
        tvDetailAuthor = view.findViewById(R.id.tv_detail_author);
        tvDetailTime = view.findViewById(R.id.tv_detail_time);
        tvDetailContent = view.findViewById(R.id.tv_detail_content);
        tvDetailLikes = view.findViewById(R.id.tv_detail_likes);
        tvDetailComments = view.findViewById(R.id.tv_detail_comments);

        btnNewPost.setOnClickListener(v -> showPostDialog());
        btnPost.setOnClickListener(v -> publishPost());
        btnCancelPost.setOnClickListener(v -> hidePostDialog());
        btnComment.setOnClickListener(v -> addComment());
        btnBack.setOnClickListener(v -> backToList());
    }

    private void initData() {
        userPrefs = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        db = AppDatabase.getInstance(getActivity());
        currentUserId = userPrefs.getLong("current_user_id", -1);

        // 检查是否为管理员
        if (currentUserId > 0) {
            User currentUser = db.userDao().getUserById(currentUserId);
            if (currentUser != null && currentUser.isAdmin()) {
                isAdmin = true;
            }
        }

        // 检查后端登录用户是否为管理员（通过缓存的用户数据）
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        if (!isAdmin && serverApi.isLoggedIn()) {
            String cachedJson = userPrefs.getString("cached_account_json", null);
            if (cachedJson != null && !cachedJson.isEmpty()) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(cachedJson);
                    // 检查是否有角色/权限字段
                    String role = json.optString("role", "");
                    int roleInt = json.optInt("roleId", 0);
                    boolean isSuperAdmin = json.optBoolean("isSuperAdmin", false);
                    if ("admin".equals(role) || "Administrator".equals(role) || 
                        roleInt >= 1 || isSuperAdmin || "admin".equals(json.optString("userName", ""))) {
                        isAdmin = true;
                    }
                } catch (Exception e) {
                    // 解析失败不影响
                }
            }
        }

        // 初始化管理员账号（如果不存在）
        initAdminAccount();
    }

    /**
     * 检查登录状态并切换界面
     * 帖子列表是公开接口，未登录用户也可以查看
     */
    private void checkLoginStatus(View view) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());

        // 无论是否登录都显示论坛内容（帖子列表是公开接口）
        showForumView(view);

        boolean loggedIn = serverApi.isLoggedIn();
        Log.d(TAG, "checkLoginStatus: isLoggedIn=" + loggedIn
                + ", token长度=" + (serverApi.getAuthToken() != null ? serverApi.getAuthToken().length() : 0)
                + ", isAdmin=" + isAdmin
                + ", currentUserId=" + currentUserId);

        // 如果未登录，隐藏发帖按钮
        if (!loggedIn) {
            btnNewPost.setVisibility(View.GONE);
            tvWelcome.setText("欢迎来到论坛");
        } else {
            btnNewPost.setVisibility(View.VISIBLE);
            updateWelcome();
        }
    }

    /**
     * 显示需要登录的提示界面
     */
    private void showLoginRequiredView(View view) {
        loginRequiredView.setVisibility(View.VISIBLE);
        forumContentView.setVisibility(View.GONE);

        // 设置"去登录"按钮点击事件
        TextView btnGoToLogin = view.findViewById(R.id.btn_go_to_login_forum);
        btnGoToLogin.setOnClickListener(v -> {
            // 跳转到个人中心页面（通过MainContainerActivity）
            if (getActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) getActivity()).showFragment(
                        ((MainContainerActivity) getActivity()).getProfileFragment()
                );
                Toast.makeText(getContext(), "请先登录后再使用论坛", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 显示论坛主界面
     */
    private void showForumView(View view) {
        loginRequiredView.setVisibility(View.GONE);
        forumContentView.setVisibility(View.VISIBLE);

        loadPosts();
        updateWelcome();
    }

    /**
     * 初始化管理员账号
     * 账号: admin / 123456
     */
    private void initAdminAccount() {
        User existingAdmin = db.userDao().getUserByUsername("admin");
        if (existingAdmin == null) {
            User admin = new User("admin", "123456", "管理员", true);
            db.userDao().insert(admin);
        }
    }

    /**
     * 确保用户在本地数据库中存在（处理远程登录用户）
     * 如果远程登录的用户在本地不存在，则创建一个本地记录
     */
    private void ensureUserExistsLocally(long userId) {
        if (userId <= 0) return;

        User localUser = db.userDao().getUserById(userId);
        if (localUser == null) {
            // 用户不存在于本地数据库，尝试从缓存创建
            String cachedJson = userPrefs.getString("cached_account_json", null);
            if (cachedJson != null && !cachedJson.isEmpty()) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(cachedJson);
                    String userName = json.optString("userName", "用户" + userId);
                    String displayName = json.optString("name");
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = userName;
                    }

                    // 创建本地用户记录
                    User newUser = new User(userName, "", displayName, false);
                    // 注意：这里需要手动设置userId，但Room的自增主键通常不允许
                    // 所以我们使用insert方法让系统分配新的ID，然后更新引用
                    long newId = db.userDao().insert(newUser);

                    // 更新当前用户ID为新生成的ID
                    if (newId > 0) {
                        userPrefs.edit().putLong("current_user_id", newId).apply();
                        currentUserId = newId;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果解析失败，创建一个默认用户
                    User defaultUser = new User("user_" + userId, "", "用户" + userId, false);
                    long newId = db.userDao().insert(defaultUser);
                    if (newId > 0) {
                        userPrefs.edit().putLong("current_user_id", newId).apply();
                        currentUserId = newId;
                    }
                }
            }
        }
    }

    private void updateWelcome() {
        if (currentUserId > 0) {
            User user = db.userDao().getUserById(currentUserId);
            if (user != null) {
                if (user.isAdmin()) {
                    tvWelcome.setText("管理员模式 · " + user.getNickname());
                } else {
                    tvWelcome.setText("欢迎回来, " + user.getNickname() + "!");
                }
            }
        } else {
            tvWelcome.setText("分享你的校园生活");
        }
    }

    private void showPostDialog() {
        postDialogLayout.setVisibility(View.VISIBLE);
        Animation slideUp = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
        postDialogLayout.startAnimation(slideUp);

        // 加号按钮脉冲动画
        if (btnNewPost != null) {
            Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
            btnNewPost.startAnimation(pulse);
        }
    }

    private void hidePostDialog() {
        Animation slideDown = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                postDialogLayout.setVisibility(View.GONE);
                etTitle.setText("");
                etContent.setText("");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        postDialogLayout.startAnimation(slideDown);
    }

    private void publishPost() {
        // 检查登录状态（使用JWT Token）
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        if (!serverApi.isLoggedIn()) {
            Toast.makeText(getActivity(), "请先登录后再发帖", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getActivity(), "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(getActivity(), "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "发帖请求: title=" + title + ", content长度=" + content.length()
                + ", token存在=" + (serverApi.getAuthToken() != null)
                + ", token长度=" + (serverApi.getAuthToken() != null ? serverApi.getAuthToken().length() : 0));

        // 显示加载提示
        Toast.makeText(getActivity(), "正在发布...", Toast.LENGTH_SHORT).show();

        // 调用远程API发布帖子
        serverApi.createPost(title, content, (success, message, postData) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success) {
                    hidePostDialog();
                    loadPosts();  // 重新从服务器加载帖子列表
                    updateWelcome();
                    Toast.makeText(getActivity(), message != null ? message : "发布成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), message != null ? message : "发布失败", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadPosts() {
        // 优先从远程服务器加载帖子
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        
        // 显示加载状态
        postsContainer.removeAllViews();
        TextView loadingText = new TextView(getActivity());
        loadingText.setText("正在加载帖子...");
        loadingText.setTextColor(0xFF6B7280);
        loadingText.setTextSize(14);
        loadingText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 50, 0, 0);
        loadingText.setLayoutParams(lp);
        postsContainer.addView(loadingText);

        serverApi.getPosts((success, message, postsArray) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success && postsArray != null) {
                    displayPostsFromJson(postsArray);
                } else {
                    // 远程加载失败，显示错误或空列表
                    postsContainer.removeAllViews();
                    TextView errorText = new TextView(getActivity());
                    errorText.setText(message != null ? message : "无法加载帖子");
                    errorText.setTextColor(0xFF9CA3AF);
                    errorText.setTextSize(14);
                    errorText.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    errLp.setMargins(0, 50, 0, 0);
                    errorText.setLayoutParams(errLp);
                    postsContainer.addView(errorText);
                    
                    Log.w("ForumFragment", "加载帖子失败: " + message);
                }
            });
        });
    }

    /**
     * 从JSON数组渲染帖子列表
     */
    private void displayPostsFromJson(JSONArray postsArray) {
        postsContainer.removeAllViews();

        if (postsArray.length() == 0) {
            TextView emptyText = new TextView(getActivity());
            emptyText.setText("暂无帖子，快来发布第一条吧！");
            emptyText.setTextColor(0xFF9CA3AF);
            emptyText.setTextSize(15);
            emptyText.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 80, 0, 0);
            emptyText.setLayoutParams(lp);
            postsContainer.addView(emptyText);
            return;
        }

        try {
            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject postObj = postsArray.getJSONObject(i);

                String title = postObj.optString("title", "无标题");
                String content = postObj.optString("content", "");
                
                // 获取作者信息
                String authorName = "匿名用户";
                if (postObj.has("author") && !postObj.isNull("author")) {
                    JSONObject author = postObj.getJSONObject("author");
                    authorName = author.optString("nickname", author.optString("userName", "匿名用户"));
                } else if (postObj.has("authorName")) {
                    authorName = postObj.optString("authorName", "匿名用户");
                }
                
                // 获取时间
                String timeStr = "";
                if (postObj.has("createdAt")) {
                    timeStr = formatTime(parseTime(postObj.getString("createdAt")));
                } else if (postObj.has("createdTime")) {
                    timeStr = formatTime(parseTime(postObj.getString("createdTime")));
                }

                // 获取统计数据
                int likeCount = postObj.optInt("likeCount", 0);
                int commentCount = postObj.optInt("commentCount", 0);
                long postId = postObj.optLong("id", postObj.optLong("postId", -1));

                // 创建帖子卡片UI（与之前保持一致）
                LinearLayout postItem = createPostCard(postId, title, content, authorName, timeStr, likeCount, commentCount);
                postsContainer.addView(postItem);
            }
        } catch (Exception e) {
            Log.e("ForumFragment", "解析帖子数据失败: " + e.getMessage());
            Toast.makeText(getActivity(), "解析帖子数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建单个帖子卡片视图
     */
    private LinearLayout createPostCard(long postId, String title, String content, 
                                        String authorName, String timeStr, 
                                        int likeCount, int commentCount) {
        LinearLayout postItem = new LinearLayout(getActivity());
        postItem.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        postItem.setLayoutParams(params);
        postItem.setBackgroundResource(R.drawable.card_background);
        postItem.setPadding(24, 20, 24, 20);
        postItem.setClickable(true);
        final long finalPostId = postId;
        postItem.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        // 从服务器获取帖子详情
                        loadPostDetail(finalPostId);
                    })
                    .start();
        });

        // 标题行 + 删除按钮（管理员可见，放在右上角）
        LinearLayout titleRow = new LinearLayout(getActivity());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(getActivity());
        titleText.setText(title);
        titleText.setTextColor(0xFF1F2937);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(titleText);

        Log.d(TAG, "createPostCard: postId=" + postId + ", isAdmin=" + isAdmin
                + ", currentUserId=" + currentUserId);

        // 管理员删除按钮（标题右侧，红色醒目）
        if (isAdmin) {
            TextView deleteBtn = new TextView(getActivity());
            deleteBtn.setText("删除");
            deleteBtn.setTextColor(0xFFFFFFFF);
            deleteBtn.setTextSize(12);
            deleteBtn.setPadding(10, 4, 10, 4);
            deleteBtn.setBackgroundResource(R.drawable.btn_delete_bg);
            final long postIdToDelete = finalPostId;
            deleteBtn.setOnClickListener(v -> {
                v.setClickable(false);  // 防止重复点击
                new android.app.AlertDialog.Builder(getActivity())
                        .setTitle("确认删除")
                        .setMessage("确定要删除该帖子吗？此操作不可恢复。")
                        .setPositiveButton("删除", (dialog, which) -> {
                            ServerApiService serverApi = ServerApiService.getInstance(getActivity());
                            if (!serverApi.isLoggedIn()) {
                                Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
                                v.setClickable(true);
                                return;
                            }

                            Toast.makeText(getActivity(), "正在删除...", Toast.LENGTH_SHORT).show();
                            serverApi.deletePost(postIdToDelete, (success, message) -> {
                                if (getActivity() == null || getActivity().isFinishing()) return;
                                getActivity().runOnUiThread(() -> {
                                    if (success) {
                                        backToList();
                                        loadPosts();
                                        Toast.makeText(getActivity(), message != null ? message : "帖子已删除", Toast.LENGTH_SHORT).show();
                                    } else {
                                        v.setClickable(true);
                                        Toast.makeText(getActivity(), message != null ? message : "删除失败", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                        })
                        .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                v.setClickable(true);
                            }
                        })
                        .show();
            });
            titleRow.addView(deleteBtn);
        }

        postItem.addView(titleRow);

        TextView metaText = new TextView(getActivity());
        metaText.setText(authorName + " · " + timeStr);
        metaText.setTextColor(0xFF6B7280);
        metaText.setTextSize(14);
        metaText.setPadding(0, 8, 0, 0);
        postItem.addView(metaText);

        if (content != null && !content.isEmpty()) {
            TextView contentPreview = new TextView(getActivity());
            String preview = content;
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            contentPreview.setText(preview);
            contentPreview.setTextColor(0xFF4B5563);
            contentPreview.setTextSize(15);
            contentPreview.setPadding(0, 10, 0, 0);
            contentPreview.setLineSpacing(4f, 1.3f);
            postItem.addView(contentPreview);
        }

        LinearLayout statsRow = new LinearLayout(getActivity());
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(0, 12, 0, 0);

        TextView likesText = new TextView(getActivity());
        likesText.setText(String.valueOf(likeCount));
        likesText.setTextColor(0xFF9CA3AF);
        likesText.setTextSize(13);
        statsRow.addView(likesText);

        TextView commentsText = new TextView(getActivity());
        commentsText.setText(String.valueOf(commentCount));
        commentsText.setTextColor(0xFF9CA3AF);
        commentsText.setTextSize(13);
        commentsText.setPadding(20, 0, 0, 0);
        statsRow.addView(commentsText);

        postItem.addView(statsRow);
            return postItem;
    }

    /**
     * 当前查看的帖子ID（用于评论）
     */
    private long currentViewingPostId = -1;

    /**
     * 从服务器加载帖子详情
     */
    private void loadPostDetail(long postId) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        
        // 重置状态
        currentViewingPostId = -1;
        
        // 清理之前动态添加的管理员操作按钮
        View existingActions = postDetailLayout.findViewWithTag("detail_admin_actions");
        if (existingActions != null) {
            ((android.view.ViewGroup) existingActions.getParent()).removeView(existingActions);
        }
        
        serverApi.getPostDetail(postId, (success, message, postData) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;
            
            getActivity().runOnUiThread(() -> {
                if (success && postData != null) {
                    showPostDetailFromServer(postData);
                } else {
                    Toast.makeText(getActivity(), message != null ? message : "获取帖子详情失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 显示从服务器获取的帖子详情
     */
    private void showPostDetailFromServer(JSONObject postData) {
        try {
            // 提取帖子信息
            String title = postData.optString("title", "无标题");
            String content = postData.optString("content", "");
            String authorName = postData.optString("userName", postData.optString("authorName", "匿名用户"));
            String timeStr = "";
            
            if (postData.has("createdTime")) {
                timeStr = formatTime(parseTime(postData.getString("createdTime")));
            } else if (postData.has("createdAt")) {
                timeStr = formatTime(parseTime(postData.getString("createdAt")));
            }
            
            int likeCount = postData.optInt("likeCount", 0);
            int commentCount = postData.optInt("commentCount", 0);
            currentViewingPostId = postData.optLong("id", postData.optLong("postId", -1));

            // 更新UI
            tvDetailTitle.setText(title);
            tvDetailAuthor.setText(authorName);
            tvDetailTime.setText(timeStr);
            tvDetailContent.setText(content);
            tvDetailLikes.setText("点赞: " + likeCount);
            tvDetailComments.setText("评论: " + commentCount);

            // 清除之前动态添加的管理员操作按钮（避免重复添加）
            View existingActions = postDetailLayout.findViewWithTag("detail_admin_actions");
            if (existingActions != null) {
                ((android.view.ViewGroup) existingActions.getParent()).removeView(existingActions);
            }

            // 管理员删除按钮（详情页 - 红色背景醒目按钮）
            if (isAdmin && currentViewingPostId > 0) {
                LinearLayout detailActions = new LinearLayout(getActivity());
                detailActions.setTag("detail_admin_actions");  // 设置tag以便后续清理
                detailActions.setOrientation(LinearLayout.HORIZONTAL);
                detailActions.setGravity(android.view.Gravity.END);
                detailActions.setPadding(16, 8, 8, 8);

                TextView deleteDetailBtn = new TextView(getActivity());
                deleteDetailBtn.setText("🗑 删除此帖");
                deleteDetailBtn.setTextColor(0xFFFFFFFF);
                deleteDetailBtn.setTextSize(14);
                deleteDetailBtn.setPadding(20, 10, 20, 10);
                deleteDetailBtn.setBackgroundResource(R.drawable.btn_delete_bg);
                final long postIdForDelete = currentViewingPostId;
                deleteDetailBtn.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(getActivity())
                            .setTitle("确认删除")
                            .setMessage("确定要删除该帖子吗？此操作不可恢复。")
                            .setPositiveButton("删除", (dialog, which) -> {
                                ServerApiService serverApi = ServerApiService.getInstance(getActivity());
                                if (!serverApi.isLoggedIn()) {
                                    Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                Toast.makeText(getActivity(), "正在删除...", Toast.LENGTH_SHORT).show();
                                serverApi.deletePost(postIdForDelete, (success, message) -> {
                                    if (getActivity() == null || getActivity().isFinishing()) return;
                                    getActivity().runOnUiThread(() -> {
                                        if (success) {
                                            backToList();
                                            loadPosts();
                                            Toast.makeText(getActivity(), message != null ? message : "帖子已删除", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getActivity(), message != null ? message : "删除失败", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                });
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });
                detailActions.addView(deleteDetailBtn);

                postDetailLayout.addView(detailActions);
            }

            // 从服务器加载评论
            loadCommentsFromServer(currentViewingPostId);

            postDetailLayout.setVisibility(View.VISIBLE);
            Animation slideInRight = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_right);
            postDetailLayout.startAnimation(slideInRight);
            
        } catch (Exception e) {
            Log.e("ForumFragment", "显示帖子详情失败: " + e.getMessage());
            Toast.makeText(getActivity(), "解析帖子数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从服务器加载评论列表
     */
    private void loadCommentsFromServer(long postId) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        
        commentsContainer.removeAllViews();
        
        serverApi.getComments(postId, (success, message, commentsArray) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;
            
            getActivity().runOnUiThread(() -> {
                if (success && commentsArray != null) {
                    displayCommentsFromJson(commentsArray);
                } else {
                    // 显示空状态或错误信息
                    TextView emptyText = new TextView(getActivity());
                    emptyText.setText(message != null ? message : "暂无评论");
                    emptyText.setTextColor(0xFF9CA3AF);
                    emptyText.setTextSize(14);
                    emptyText.setGravity(android.view.Gravity.CENTER);
                    commentsContainer.addView(emptyText);
                }
            });
        });
    }

    /**
     * 从JSON数组渲染评论列表
     */
    private void displayCommentsFromJson(JSONArray commentsArray) {
        commentsContainer.removeAllViews();

        if (commentsArray.length() == 0) {
            TextView emptyText = new TextView(getActivity());
            emptyText.setText("暂无评论，快来发表第一条吧！");
            emptyText.setTextColor(0xFF9CA3AF);
            emptyText.setTextSize(14);
            emptyText.setGravity(android.view.Gravity.CENTER);
            commentsContainer.addView(emptyText);
            return;
        }

        try {
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject commentObj = commentsArray.getJSONObject(i);

                String content = commentObj.optString("content", "");
                String authorName = commentObj.optString("userName", commentObj.optString("authorName", "匿名用户"));
                
                String timeStr = "";
                if (commentObj.has("createdTime")) {
                    timeStr = formatTime(parseTime(commentObj.getString("createdTime")));
                } else if (commentObj.has("createdAt")) {
                    timeStr = formatTime(parseTime(commentObj.getString("createdAt")));
                }

                LinearLayout commentItem = new LinearLayout(getActivity());
                commentItem.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 12);
                commentItem.setLayoutParams(params);
                commentItem.setBackgroundResource(R.drawable.card_background);
                commentItem.setPadding(20, 16, 20, 16);

                TextView authorText = new TextView(getActivity());
                authorText.setText(authorName);
                authorText.setTextColor(0xFF374151);
                authorText.setTextSize(15);
                authorText.setTypeface(null, android.graphics.Typeface.BOLD);
                commentItem.addView(authorText);

                TextView timeText = new TextView(getActivity());
                timeText.setText(timeStr);
                timeText.setTextColor(0xFF9CA3AF);
                timeText.setTextSize(12);
                timeText.setPadding(0, 4, 0, 0);
                commentItem.addView(timeText);

                TextView contentText = new TextView(getActivity());
                contentText.setText(content);
                contentText.setTextColor(0xFF4B5563);
                contentText.setTextSize(15);
                contentText.setPadding(0, 8, 0, 0);
                contentText.setLineSpacing(4f, 1.3f);
                commentItem.addView(contentText);

                commentsContainer.addView(commentItem);
            }
        } catch (Exception e) {
            Log.e("ForumFragment", "解析评论数据失败: " + e.getMessage());
            Toast.makeText(getActivity(), "解析评论数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除帖子（仅管理员，调用服务器API）
     */
    private void deletePost(long postId) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        if (!serverApi.isLoggedIn()) {
            Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getActivity(), "正在删除...", Toast.LENGTH_SHORT).show();
        serverApi.deletePost(postId, (success, message) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;
            getActivity().runOnUiThread(() -> {
                if (success) {
                    backToList();
                    loadPosts();
                    updateWelcome();
                    Toast.makeText(getActivity(), message != null ? message : "帖子已删除", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), message != null ? message : "删除失败", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void addComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getActivity(), "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentViewingPostId <= 0) {
            Toast.makeText(getActivity(), "无法确定帖子ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查登录状态
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        if (!serverApi.isLoggedIn()) {
            Toast.makeText(getActivity(), "请先登录后再评论", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载提示
        Toast.makeText(getActivity(), "正在发表评论...", Toast.LENGTH_SHORT).show();

        // 调用服务器API发表评论
        serverApi.createComment(currentViewingPostId, content, null, (success, message, commentData) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success) {
                    etComment.setText("");
                    loadCommentsFromServer(currentViewingPostId);  // 重新加载评论列表
                    loadPosts();  // 刷新帖子列表（更新评论数）
                    Toast.makeText(getActivity(), message != null ? message : "评论成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), message != null ? message : "评论失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 解析时间字符串（支持多种格式）
     */
    private long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return System.currentTimeMillis();
        
        try {
            // 尝试ISO 8601格式: 2026-06-11T06:54:44.8697031Z
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US);
            Date date = isoFormat.parse(timeStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            try {
                // 尝试标准ISO格式: 2026-06-11T06:54:44Z
                SimpleDateFormat stdIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                Date date = stdIso.parse(timeStr);
                return date != null ? date.getTime() : System.currentTimeMillis();
            } catch (Exception e2) {
                try {
                    // 尝试普通日期格式
                    SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                    Date date = simple.parse(timeStr);
                    return date != null ? date.getTime() : System.currentTimeMillis();
                } catch (Exception e3) {
                    return System.currentTimeMillis();
                }
            }
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    public void backToList() {
        // 重置状态
        currentViewingPostId = -1;
        
        // 清理动态添加的管理员操作按钮
        View existingActions = postDetailLayout.findViewWithTag("detail_admin_actions");
        if (existingActions != null) {
            ((android.view.ViewGroup) existingActions.getParent()).removeView(existingActions);
        }
        
        Animation slideOutRight = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_right);
        slideOutRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                postDetailLayout.setVisibility(View.GONE);
                loadPosts();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        postDetailLayout.startAnimation(slideOutRight);
    }

    private void selectTag(TextView selectedTag) {
        // 重置所有标签样式
        TextView[] tags = {tagAll, tagHot, tagNew, tagMy};
        for (TextView tag : tags) {
            if (tag == selectedTag) continue;

            // 未选中动画
            tag.animate()
                    .scaleX(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        tag.setBackgroundResource(R.drawable.tag_unselected_modern);
                        tag.setTextColor(getResources().getColor(R.color.text_secondary));
                        tag.animate().scaleX(1f).setDuration(100).start();
                    })
                    .start();
        }

        // 选中标签动画
        selectedTag.animate()
                .scaleX(1.05f)
                .setDuration(100)
                .withEndAction(() -> {
                    selectedTag.setBackgroundResource(R.drawable.tag_selected);
                    selectedTag.setTextColor(getResources().getColor(android.R.color.white));
                    selectedTag.animate().scaleX(1f).setDuration(100).start();
                })
                .start();

        // 根据标签筛选帖子
        String sortBy = "time";
        String tag = null;
        
        if (selectedTag == tagHot) {
            sortBy = "hot";  // 按热度排序
        } else if (selectedTag == tagNew) {
            sortBy = "time";  // 按时间排序（最新）
        } else if (selectedTag == tagMy) {
            // 我的帖子：调用 GET /api/posts/my（需要登录）
            loadMyPosts();
            return;
        }

        // 调用服务器API加载对应排序的帖子
        loadPostsWithFilter(sortBy, tag);
    }

    /**
     * 加载"我的帖子"列表
     * 调用 GET /api/posts/my（需要JWT认证）
     */
    private void loadMyPosts() {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());

        if (!serverApi.isLoggedIn()) {
            Toast.makeText(getActivity(), "请先登录查看我的帖子", Toast.LENGTH_SHORT).show();
            return;
        }

        postsContainer.removeAllViews();
        TextView loadingText = new TextView(getActivity());
        loadingText.setText("正在加载我的帖子...");
        loadingText.setTextColor(0xFF6B7280);
        loadingText.setTextSize(14);
        loadingText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (120 * getResources().getDisplayMetrics().density)
        );
        lp.setMargins(0, 20, 0, 0);
        loadingText.setLayoutParams(lp);
        postsContainer.addView(loadingText);

        serverApi.getMyPosts(1, 50, new ServerApiService.PostsCallback() {
            @Override
            public void onResult(boolean success, String message, org.json.JSONArray posts) {
                getActivity().runOnUiThread(() -> {
                    postsContainer.removeAllViews();

                    if (!success) {
                        TextView errorText = new TextView(getActivity());
                        errorText.setText(message != null ? message : "加载失败");
                        errorText.setTextColor(0xFFEF4444);
                        errorText.setTextSize(14);
                        errorText.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (int) (80 * getResources().getDisplayMetrics().density)
                        );
                        errLp.setMargins(0, 40, 0, 0);
                        errorText.setLayoutParams(errLp);
                        postsContainer.addView(errorText);
                        return;
                    }

                    if (posts.length() == 0) {
                        TextView emptyText = new TextView(getActivity());
                        emptyText.setText("你还没有发布过帖子");
                        emptyText.setTextColor(0xFF9CA3AF);
                        emptyText.setTextSize(14);
                        emptyText.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams empLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (int) (80 * getResources().getDisplayMetrics().density)
                        );
                        empLp.setMargins(0, 40, 0, 0);
                        emptyText.setLayoutParams(empLp);
                        postsContainer.addView(emptyText);
                        return;
                    }

                    for (int i = 0; i < posts.length(); i++) {
                        try {
                            org.json.JSONObject postObj = posts.getJSONObject(i);
                            String title = postObj.optString("title", "无标题");
                            String content = postObj.optString("content", "");
                            String authorName = postObj.optString("userName", "匿名用户");
                            if (authorName.isEmpty()) authorName = "匿名用户";
                            String timeStr = "";
                            if (postObj.has("createdTime")) {
                                timeStr = formatTime(parseTime(postObj.getString("createdTime")));
                            } else if (postObj.has("createdAt")) {
                                timeStr = formatTime(parseTime(postObj.getString("createdAt")));
                            }
                            int likeCount = postObj.optInt("likeCount", 0);
                            int commentCount = postObj.optInt("commentCount", 0);
                            long postId = postObj.optLong("id", -1);
                            LinearLayout postItem = createPostCard(postId, title, content, authorName, timeStr, likeCount, commentCount);
                            postsContainer.addView(postItem);
                        } catch (Exception e) {
                            Log.e(TAG, "解析我的帖子失败: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    /**
     * 带筛选条件加载帖子
     */
    private void loadPostsWithFilter(String sortBy, String tag) {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());
        
        postsContainer.removeAllViews();
        TextView loadingText = new TextView(getActivity());
        loadingText.setText("正在加载...");
        loadingText.setTextColor(0xFF6B7280);
        loadingText.setTextSize(14);
        loadingText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 50, 0, 0);
        loadingText.setLayoutParams(lp);
        postsContainer.addView(loadingText);

        serverApi.getPosts(1, 10, sortBy, tag, (success, message, postsArray) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success && postsArray != null) {
                    displayPostsFromJson(postsArray);
                } else {
                    postsContainer.removeAllViews();
                    TextView errorText = new TextView(getActivity());
                    errorText.setText(message != null ? message : "加载失败");
                    errorText.setTextColor(0xFF9CA3AF);
                    errorText.setTextSize(14);
                    errorText.setGravity(android.view.Gravity.CENTER);
                    postsContainer.addView(errorText);
                }
            });
        });
    }
}