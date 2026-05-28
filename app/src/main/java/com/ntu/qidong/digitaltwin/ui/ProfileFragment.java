package com.ntu.qidong.digitaltwin.ui;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.db.User;

public class ProfileFragment extends Fragment {

    private LinearLayout loginLayout;
    private LinearLayout profileLayout;
    private TextView tvNickname;
    private TextView tvUsername;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnChangeNickname;
    private Button btnSettings;
    private Button btnAbout;

    private SharedPreferences userPrefs;
    private AppDatabase db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        initData();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        loginLayout = view.findViewById(R.id.login_layout);
        profileLayout = view.findViewById(R.id.profile_layout);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvUsername = view.findViewById(R.id.tv_username);
        btnLogin = view.findViewById(R.id.btn_login);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnChangeNickname = view.findViewById(R.id.btn_change_nickname);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnAbout = view.findViewById(R.id.btn_about);

        btnLogin.setOnClickListener(v -> showLoginDialog());
        btnLogout.setOnClickListener(v -> logout());
        btnChangeNickname.setOnClickListener(v -> showChangeNicknameDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void initData() {
        userPrefs = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        db = AppDatabase.getInstance(getActivity());
    }

    private void updateUI() {
        long userId = userPrefs.getLong("current_user_id", -1);

        if (userId != -1) {
            User user = db.userDao().getUserById(userId);
            if (user != null) {
                tvNickname.setText(user.getNickname());
                tvUsername.setText("用户名: " + user.getUsername());
                loginLayout.setVisibility(View.GONE);
                profileLayout.setVisibility(View.VISIBLE);
                return;
            }
        }

        loginLayout.setVisibility(View.VISIBLE);
        profileLayout.setVisibility(View.GONE);
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("登录");

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_login, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);

        builder.setView(dialogView);

        builder.setPositiveButton("登录", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            User user = db.userDao().login(username, password);
            if (user != null) {
                userPrefs.edit().putLong("current_user_id", user.getUserId()).apply();
                updateUI();
                Toast.makeText(getActivity(), "登录成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void logout() {
        userPrefs.edit().remove("current_user_id").apply();
        updateUI();
        Toast.makeText(getActivity(), "已退出登录", Toast.LENGTH_SHORT).show();
    }

    private void showChangeNicknameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("修改昵称");

        EditText etNewNickname = new EditText(getActivity());
        etNewNickname.setHint("请输入新昵称");
        etNewNickname.setTextColor(getResources().getColor(android.R.color.white));
        etNewNickname.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        etNewNickname.setBackgroundResource(R.drawable.search_bar_background);
        etNewNickname.setPadding(12, 12, 12, 12);

        builder.setView(etNewNickname);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newNickname = etNewNickname.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                long userId = userPrefs.getLong("current_user_id", -1);
                User user = db.userDao().getUserById(userId);
                if (user != null) {
                    user.setNickname(newNickname);
                    db.userDao().update(user);
                    tvNickname.setText(newNickname);
                    Toast.makeText(getActivity(), "昵称修改成功", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("设置");

        String[] options = {"通知开关", "清空缓存", "关于"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    Toast.makeText(getActivity(), "通知开关功能开发中", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getActivity(), "缓存已清空", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    showAboutDialog();
                    break;
            }
        });

        builder.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("关于");
        builder.setMessage("南通大学启东校区数字孪生平台\n版本号: v1.0.4");
        builder.setPositiveButton("确定", null);
        builder.show();
    }
}