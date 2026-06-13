package com.ntu.qidong.digitaltwin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.service.ServerApiService;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 好友列表Fragment
 * 
 * 调用 GET /api/users 获取数据库中全部注册用户，默认所有人互为好友。
 * 任意已登录用户均可查看完整用户列表。
 */
public class FriendsFragment extends Fragment {

    private ScrollView friendsContainer;
    private LinearLayout emptyView;
    private LinearLayout publicChatroomEntry;
    private LinearLayout friendsListContainer;
    private TextView friendCountText;
    private TextView placeholderText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        initViews(view);
        setupClickListeners();
        loadUsers();

        return view;
    }

    private void initViews(View view) {
        friendsContainer = view.findViewById(R.id.friends_container);
        emptyView = view.findViewById(R.id.empty_view);
        publicChatroomEntry = view.findViewById(R.id.ll_public_chatroom);
        friendCountText = view.findViewById(R.id.tv_friend_count);
        placeholderText = view.findViewById(R.id.tv_friends_placeholder);

        // 找到好友列表容器（ScrollView > LinearLayout）
        if (friendsContainer != null && friendsContainer.getChildCount() > 0) {
            friendsListContainer = (LinearLayout) friendsContainer.getChildAt(0);
        }
    }

    private void setupClickListeners() {
        // 公共聊天室点击事件
        if (publicChatroomEntry != null) {
            publicChatroomEntry.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.setScaleX(1f);
                            v.setScaleY(1f);
                            openPublicChatRoom();
                        })
                        .start();
            });
        }
    }

    /**
     * 从后端加载全部用户列表
     * 接口: GET /api/users （任意已登录用户可调用，返回全量用户数据）
     */
    private void loadUsers() {
        ServerApiService serverApi = ServerApiService.getInstance(getActivity());

        if (!serverApi.isLoggedIn()) {
            showNotLoggedIn();
            return;
        }

        // 显示加载状态
        if (friendCountText != null) {
            friendCountText.setText("加载中...");
        }

        serverApi.getAllUsers((success, message, users) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success && users != null && users.length() > 0) {
                    displayUsers(users);
                } else {
                    showEmptyState(message != null ? message : "暂无用户");
                }
            });
        });
    }

    /**
     * 渲染用户列表为好友卡片
     */
    private void displayUsers(JSONArray users) {
        // 清空旧列表（保留静态元素：聊天标题、聊天室卡片、分割线、占位文字）
        if (friendsListContainer != null) {
            while (friendsListContainer.getChildCount() > 3) {
                friendsListContainer.removeViewAt(friendsListContainer.getChildCount() - 1);
            }
        }

        // 隐藏占位文字
        if (placeholderText != null) {
            placeholderText.setVisibility(View.GONE);
        }

        // 更新好友数量统计
        if (friendCountText != null) {
            friendCountText.setText("共 " + users.length() + " 位用户");
        }

        try {
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);

                // 字段读取（兼容多种命名）
                int userId = user.optInt("id", user.optInt("userId", -1));
                String userName = user.optString("userName", user.optString("name", "未知用户"));
                String displayName = user.optString("name", userName); // 优先显示 name，回退到 userName
                String avatar = user.optString("avatar", null);
                String role = user.optString("role", user.optString("roleId", ""));

                // 判断是否为管理员
                boolean isAdminUser = "admin".equalsIgnoreCase(role)
                        || "administrator".equalsIgnoreCase(role)
                        || "admin".equalsIgnoreCase(userName)
                        || user.optBoolean("isSuperAdmin", false);

                // 创建好友卡片
                LinearLayout friendCard = createFriendCard(userId, displayName, isAdminUser);
                
                if (friendsListContainer != null) {
                    friendsListContainer.addView(friendCard);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyState("解析用户数据失败");
        }
    }

    /**
     * 创建单个好友卡片视图
     */
    private LinearLayout createFriendCard(int userId, String displayName, boolean isAdminUser) {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 10);
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(16, 14, 16, 14);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);

        // 点击 → 打开私聊（待对接 /api/channels）
        final int finalUserId = userId;
        final String finalName = displayName;
        card.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        Toast.makeText(getActivity(),
                                "与 " + finalName + " 的私聊功能开发中...",
                                Toast.LENGTH_SHORT).show();
                    })
                    .start();
        });

        // 头像圆圈
        View avatarCircle = new View(getActivity());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                dpToPx(44), dpToPx(44));
        avatarCircle.setLayoutParams(avatarParams);
        avatarCircle.setBackgroundResource(R.drawable.circle_gradient_blue);
        card.addView(avatarCircle);

        // 用户信息区（名称 + 角色标签）
        LinearLayout infoLayout = new LinearLayout(getActivity());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMarginStart(dpToPx(14));
        infoLayout.setLayoutParams(infoParams);

        // 用户名行（名称 + 管理员标签）
        LinearLayout nameRow = new LinearLayout(getActivity());
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView nameText = new TextView(getActivity());
        nameText.setText(displayName);
        nameText.setTextColor(0xFF1E293B);
        nameText.setTextSize(15);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameRow.addView(nameText);

        // 管理员角色标签
        if (isAdminUser) {
            TextView adminBadge = new TextView(getActivity());
            adminBadge.setText("管理员");
            adminBadge.setTextColor(0xFFFFFFFF);
            adminBadge.setTextSize(10);
            adminBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            adminBadge.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            adminBadge.setBackgroundResource(R.drawable.badge_background);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeParams.setMarginStart(dpToPx(8));
            adminBadge.setLayoutParams(badgeParams);
            nameRow.addView(adminBadge);
        }

        infoLayout.addView(nameRow);

        // 副标题：显示"已注册用户"
        TextView subtitleText = new TextView(getActivity());
        subtitleText.setText("已注册用户");
        subtitleText.setTextColor(0xFF94A3B8);
        subtitleText.setTextSize(12);
        subtitleText.setPadding(0, dpToPx(2), 0, 0);
        infoLayout.addView(subtitleText);

        card.addView(infoLayout);

        // 右侧在线状态绿点
        View statusDot = new View(getActivity());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                dpToPx(10), dpToPx(10));
        statusDot.setLayoutParams(dotParams);
        statusDot.setBackgroundResource(R.drawable.online_status_dot);
        card.addView(statusDot);

        return card;
    }

    /**
     * 未登录状态提示
     */
    private void showNotLoggedIn() {
        if (friendCountText != null) {
            friendCountText.setText("请先登录");
        }
        if (placeholderText != null) {
            placeholderText.setVisibility(View.VISIBLE);
            placeholderText.setText("登录后可查看所有注册用户");
        }
    }

    /**
     * 空状态 / 错误状态
     */
    private void showEmptyState(String msg) {
        if (friendCountText != null) {
            friendCountText.setText(msg);
        }
        if (placeholderText != null) {
            placeholderText.setVisibility(View.VISIBLE);
            placeholderText.setText(msg);
        }
    }

    /**
     * 打开公共聊天室
     */
    private void openPublicChatRoom() {
        Intent intent = new Intent(getActivity(), ChatRoomActivity.class);
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面可见时刷新列表
        if (!isHidden() && isAdded()) {
            loadUsers();
        }
    }
}
