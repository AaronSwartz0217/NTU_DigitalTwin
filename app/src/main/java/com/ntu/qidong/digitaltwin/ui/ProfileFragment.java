package com.ntu.qidong.digitaltwin.ui;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.db.User;
import com.ntu.qidong.digitaltwin.model.AccountProfile;
import com.ntu.qidong.digitaltwin.service.ServerApiService;

public class ProfileFragment extends Fragment {

    private LinearLayout loginLayout;
    private ScrollView profileLayout;
    private TextView tvNickname;
    private TextView tvUsername;
    private TextView tvPostCount;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnChangeNickname;
    private View btnSettings;
    private View btnAbout;
    private Button btnServerSettings;

    // 档案展示相关视图
    private TextView tvUserId, tvUserNo, tvUserName, tvGender, tvEthnicGroup;
    private TextView tvNativePlace, tvBirthday, tvWeight, tvHeight, tvIdNumber;
    private LinearLayout profileInfoSection;

    private SharedPreferences userPrefs;
    private AppDatabase db;
    private ServerApiService serverApi;

    // 当前登录的账号档案（从服务器获取）
    private AccountProfile currentAccount = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        initData();
        updateUI();

        return view;
    }

    /**
     * 每次Fragment变为可见时调用（从其他Tab切回来时）
     * 关键：确保登录状态正确显示
     */
    @Override
    public void onResume() {
        super.onResume();
        
        // 每次回到这个页面都重新检查状态并更新UI
        if (serverApi != null) {
            Log.d("ProfileFragment", "onResume - 检查登录状态");
            
            if (serverApi.isLoggedIn()) {
                // 有Token，确保显示档案界面
                if (currentAccount == null) {
                    // 没有缓存数据，尝试加载
                    loadCachedAccount();
                }
                
                if (currentAccount != null) {
                    displayRemoteAccountInfo();
                } else {
                    // 还是没有数据，显示加载中并刷新
                    loginLayout.setVisibility(View.GONE);
                    profileLayout.setVisibility(View.VISIBLE);
                    tvNickname.setText("加载中...");
                    refreshUserProfile();
                }
            } else {
                // 无Token，显示登录界面
                currentAccount = null;
                updateUI();
            }
        }
    }

    private void initViews(View view) {
        loginLayout = view.findViewById(R.id.login_layout);
        profileLayout = view.findViewById(R.id.profile_layout);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvUsername = view.findViewById(R.id.tv_username);
        tvPostCount = view.findViewById(R.id.tv_post_count);
        btnLogin = view.findViewById(R.id.btn_login);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnChangeNickname = view.findViewById(R.id.btn_change_nickname);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnAbout = view.findViewById(R.id.btn_about);
        btnServerSettings = view.findViewById(R.id.btn_server_settings);

        // 档案信息视图
        tvUserId = view.findViewById(R.id.tv_user_id_display);
        tvUserNo = view.findViewById(R.id.tv_user_no_display);
        tvUserName = view.findViewById(R.id.tv_user_name_display);
        tvGender = view.findViewById(R.id.tv_gender_display);
        tvEthnicGroup = view.findViewById(R.id.tv_ethnic_group_display);
        tvNativePlace = view.findViewById(R.id.tv_native_place_display);
        tvBirthday = view.findViewById(R.id.tv_birthday_display);
        tvWeight = view.findViewById(R.id.tv_weight_display);
        tvHeight = view.findViewById(R.id.tv_height_display);
        tvIdNumber = view.findViewById(R.id.tv_id_number_display);
        profileInfoSection = view.findViewById(R.id.profile_info_section);

        btnLogin.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        showLoginDialog();
                    })
                    .start();
        });
        btnLogout.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        logout();
                    })
                    .start();
        });
        btnChangeNickname.setOnClickListener(v -> showEditProfileDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnAbout.setOnClickListener(v -> showAboutDialog());
        btnServerSettings.setOnClickListener(v -> showServerAddressDialog());

        // 新增菜单项点击事件
        view.findViewById(R.id.menu_my_posts).setOnClickListener(v ->
                Toast.makeText(getActivity(), "我的帖子功能开发中", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.menu_favorites).setOnClickListener(v ->
                Toast.makeText(getActivity(), "收藏夹功能开发中", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.menu_history).setOnClickListener(v ->
                Toast.makeText(getActivity(), "浏览历史功能开发中", Toast.LENGTH_SHORT).show());
    }

    private void initData() {
        userPrefs = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        db = AppDatabase.getInstance(getActivity());
        serverApi = ServerApiService.getInstance(getActivity());

        // 检查是否有有效的JWT Token（远程登录状态）
        if (serverApi.isLoggedIn()) {
            Log.d("ProfileFragment", "检测到有效Token，恢复登录状态");
            // 有Token，先从缓存恢复账号档案（立即显示）
            loadCachedAccount();
            // 然后异步刷新最新数据（后台更新）
            refreshUserProfile();
        } else {
            Log.d("ProfileFragment", "无Token，显示登录界面");
            // 无Token，清除可能的过期数据
            currentAccount = null;
        }
    }

    /**
     * 刷新用户资料（从服务器）- 仅更新数据，不改变UI状态
     */
    private void refreshUserProfile() {
        if (!serverApi.isLoggedIn()) return;

        serverApi.getCurrentUser((success, message, account) -> {
            if (getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (success && account != null) {
                    Log.d("ProfileFragment", "刷新用户资料成功: " + account.getUserName());
                    currentAccount = account;
                    saveCachedAccount(account);
                    // 更新UI显示最新数据
                    displayRemoteAccountInfo();
                } else {
                    Log.w("ProfileFragment", "刷新用户资料失败: " + message);
                    // 刷新失败不影响当前显示，继续使用缓存的account
                    if (currentAccount != null) {
                        displayRemoteAccountInfo();
                    }
                }
            });
        });
    }

    /**
     * 从本地缓存加载账号档案
     */
    private void loadCachedAccount() {
        String cachedJson = userPrefs.getString("cached_account_json", null);
        Log.d("ProfileFragment", "加载缓存: " + (cachedJson != null ? "有数据" : "无数据"));

        if (cachedJson != null && !cachedJson.isEmpty()) {
            try {
                org.json.JSONObject json = new org.json.JSONObject(cachedJson);
                currentAccount = parseAccountFromJson(json);
                if (currentAccount != null) {
                    Log.d("ProfileFragment", "缓存恢复成功: " + currentAccount.getUserName());
                } else {
                    Log.w("ProfileFragment", "缓存解析返回null");
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "解析缓存失败: " + e.getMessage());
                currentAccount = null;
            }
        } else {
            Log.d("ProfileFragment", "无缓存数据，等待服务器刷新");
            // 即使没有缓存也不要清除currentAccount，让refreshUserProfile处理
        }
    }

    /**
     * 保存账号档案到本地缓存
     */
    private void saveCachedAccount(AccountProfile account) {
        if (account == null) return;
        try {
            // 过滤密码字段再保存
            account.setPassword("");  // 安全：不保存密码
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("userId", account.getUserId());
            json.put("userName", account.getUserName());
            json.put("no", account.getNo());
            json.put("name", account.getName());
            json.put("idNumber", account.getIdNumber());
            json.put("gender", account.getGender());
            json.put("ethnicGroup", account.getEthnicGroup());
            json.put("nativePlace", account.getNativePlace());
            json.put("birthday", account.getBirthday());
            json.put("weight", account.getWeight());
            json.put("height", account.getHeight());
            json.put("hasProfile", account.isHasProfile());
            json.put("role", account.getRole());  // 管理员权限

            userPrefs.edit().putString("cached_account_json", json.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        boolean hasToken = serverApi.isLoggedIn();
        Log.d("ProfileFragment", "updateUI - Token: " + hasToken + ", Account: " + (currentAccount != null ? currentAccount.getUserName() : "null"));

        // 基于JWT Token判断登录状态
        if (hasToken && currentAccount != null) {
            // 已登录且有账号信息，显示档案
            Log.d("ProfileFragment", "显示档案界面");
            displayRemoteAccountInfo();
            return;
        }

        if (hasToken && currentAccount == null) {
            // 有Token但还没加载到数据（正在刷新中），显示加载中或等待
            Log.d("ProfileFragment", "有Token但无数据，显示加载中");
            loginLayout.setVisibility(View.GONE);
            profileLayout.setVisibility(View.VISIBLE);
            tvNickname.setText("加载中...");
            return;
        }

        // 未登录
        Log.d("ProfileFragment", "未登录，显示登录界面");
        loginLayout.setVisibility(View.VISIBLE);
        profileLayout.setVisibility(View.GONE);
    }

    /**
     * 显示远程账号的完整信息
     */
    private void displayRemoteAccountInfo() {
        if (currentAccount == null) return;

        // 基本账号信息
        String displayName = currentAccount.getName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = currentAccount.getUserName();
        }
        tvNickname.setText(displayName);
        tvUsername.setText("ID: " + currentAccount.getUserId());

        // 学生档案信息
        tvUserId.setText(String.valueOf(currentAccount.getUserId()));
        tvUserNo.setText(currentAccount.getNo() != null ? currentAccount.getNo() : "未填写");
        tvUserName.setText(currentAccount.getName() != null ? currentAccount.getName() : "未填写");
        tvGender.setText(currentAccount.getGenderText());
        tvEthnicGroup.setText(currentAccount.getEthnicGroupText());
        tvNativePlace.setText(currentAccount.getNativePlace() != null ? currentAccount.getNativePlace() : "未填写");
        tvBirthday.setText(currentAccount.getBirthdayDisplay());
        tvWeight.setText(currentAccount.getWeight() != null ? currentAccount.getWeight() + " kg" : "未填写");
        tvHeight.setText(currentAccount.getHeight() != null ? String.format("%.2f m", currentAccount.getHeight()) : "未填写");
        tvIdNumber.setText(currentAccount.getIdNumber() != null ? maskIdNumber(currentAccount.getIdNumber()) : "未填写");

        // 统计数据（模拟）
        int postCount = db.postDao().getPostCountByAuthor(currentAccount.getUserId());
        tvPostCount.setText(String.valueOf(postCount));

        // 显示档案界面，隐藏登录界面
        loginLayout.setVisibility(View.GONE);
        profileLayout.setVisibility(View.VISIBLE);

        // 确保操作按钮可见（编辑资料、退出登录、设置等）
        if (btnChangeNickname != null) btnChangeNickname.setVisibility(View.VISIBLE);
        if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
        if (btnSettings != null) btnSettings.setVisibility(View.VISIBLE);
        if (btnAbout != null) btnAbout.setVisibility(View.VISIBLE);
    }

    /**
     * 身份证号脱敏显示
     */
    private String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 8) return idNumber;
        return idNumber.substring(0, 4) + "****" + idNumber.substring(idNumber.length() - 4);
    }

    // ==================== 登录功能 ====================

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("用户登录");

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_login, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);

        // 添加提示文字
        TextView tvHint = new TextView(getActivity());
        tvHint.setText("提示：如需新账号请联系管理员");
        tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvHint.setTextSize(12);
        tvHint.setPadding(0, 8, 0, 0);

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(dialogView);
        layout.addView(tvHint);

        builder.setView(layout);

        builder.setPositiveButton("登录", (dialog, which) -> {
            performLogin(etUsername.getText().toString().trim(),
                        etPassword.getText().toString().trim());
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 执行登录操作（仅使用远程服务器）
     */
    private void performLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog loadingDialog = new AlertDialog.Builder(getActivity())
                .setMessage("正在连接服务器...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // 调用新的登录API (POST /api/auth/login)
        serverApi.login(username, password, (success, message, account) -> {
            getActivity().runOnUiThread(() -> {
                try {
                    // 确保loading对话框被关闭
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.e("ProfileFragment", "关闭对话框异常: " + e.getMessage());
                }

                if (success && account != null) {
                    // 远程登录成功（JWT Token已自动保存）
                    onRemoteLoginSuccess(account);
                } else {
                    // 登录失败 - 显示具体错误信息
                    String displayMsg = (message != null && !message.isEmpty()) 
                            ? message 
                            : "登录失败，请检查用户名和密码";
                    
                    Toast.makeText(getActivity(), displayMsg, Toast.LENGTH_LONG).show();
                    Log.w("ProfileFragment", "登录失败: " + displayMsg);
                }
            });
        });
    }

    /**
     * 远程登录成功处理
     */
    private void onRemoteLoginSuccess(AccountProfile account) {
        Log.d("ProfileFragment", "登录成功！用户: " + account.getUserName());
        this.currentAccount = account;

        // 保存到本地缓存（过滤密码）
        saveCachedAccount(account);
        Log.d("ProfileFragment", "已保存到缓存");

        // JWT Token已在ServerApiService.login()中自动保存
        Log.d("ProfileFragment", "Token状态: " + serverApi.isLoggedIn());

        updateUI();

        String welcomeName = account.getName();
        if (welcomeName == null || welcomeName.isEmpty()) {
            welcomeName = account.getUserName();
        }
        Toast.makeText(getActivity(), "登录成功，欢迎 " + welcomeName + "！", Toast.LENGTH_SHORT).show();

        // 检查是否需要完善档案
        if (!account.isHasProfile() || !account.hasStudentProfile()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("完善个人信息")
                    .setMessage("检测到您的学生档案尚未完善\n是否现在补充？")
                    .setPositiveButton("去完善", (d, w) -> showEditProfileDialog())
                    .setNegativeButton("稍后再说", null)
                    .show();
        }
    }

    // ==================== 编辑资料功能 ====================

    /**
     * 编辑资料对话框
     */
    private void showEditProfileDialog() {
        if (currentAccount == null) {
            Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("编辑个人资料");

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_register_full, null);

        // 隐藏必填区域（只编辑选填）
        dialogView.findViewById(R.id.et_reg_username).setVisibility(View.GONE);
        dialogView.findViewById(R.id.et_reg_password).setVisibility(View.GONE);
        dialogView.findViewById(R.id.et_reg_confirm_pwd).setVisibility(View.GONE);

        // 选填字段 - 填充当前值
        EditText etNo = dialogView.findViewById(R.id.et_reg_no);
        EditText etName = dialogView.findViewById(R.id.et_reg_name);
        EditText etIdNumber = dialogView.findViewById(R.id.et_reg_id_number);
        Spinner spinnerGender = dialogView.findViewById(R.id.spinner_gender);
        Spinner spinnerEthnic = dialogView.findViewById(R.id.spinner_ethnic);
        EditText etNativePlace = dialogView.findViewById(R.id.et_reg_native_place);
        EditText etBirthday = dialogView.findViewById(R.id.et_reg_birthday);
        EditText etWeight = dialogView.findViewById(R.id.et_reg_weight);
        EditText etHeight = dialogView.findViewById(R.id.et_reg_height);

        // 设置当前值
        if (currentAccount.getNo() != null) etNo.setText(currentAccount.getNo());
        if (currentAccount.getName() != null) etName.setText(currentAccount.getName());
        if (currentAccount.getIdNumber() != null) etIdNumber.setText(currentAccount.getIdNumber());
        if (currentAccount.getNativePlace() != null) etNativePlace.setText(currentAccount.getNativePlace());
        if (currentAccount.getBirthday() != null) etBirthday.setText(currentAccount.getBirthdayDisplay());
        if (currentAccount.getWeight() != null) etWeight.setText(String.valueOf(currentAccount.getWeight()));
        if (currentAccount.getHeight() != null) etHeight.setText(String.valueOf(currentAccount.getHeight()));

        // 性别Spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.gender_options,
                android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
        if (currentAccount.getGender() != null) {
            spinnerGender.setSelection(currentAccount.getGender() + 1);  // +1因为index 0是"不填"
        }

        // 民族Spinner
        ArrayAdapter<CharSequence> ethnicAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.ethnic_group_options,
                android.R.layout.simple_spinner_item);
        ethnicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEthnic.setAdapter(ethnicAdapter);
        if (currentAccount.getEthnicGroup() != null) {
            spinnerEthnic.setSelection(currentAccount.getEthnicGroup() + 1);  // +1因为index 0是"不填"
        }

        builder.setView(dialogView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            performUpdateProfile(etNo, etName, etIdNumber,
                    spinnerGender, spinnerEthnic,
                    etNativePlace, etBirthday, etWeight, etHeight);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 执行更新档案操作
     */
    private void performUpdateProfile(EditText etNo, EditText etName, EditText etIdNumber,
                                      Spinner spinnerGender, Spinner spinnerEthnic,
                                      EditText etNativePlace, EditText etBirthday,
                                      EditText etWeight, EditText etHeight) {

        // 构建更新数据
        AccountProfile updateData = new AccountProfile();

        String no = etNo.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String idNumber = etIdNumber.getText().toString().trim();
        String nativePlace = etNativePlace.getText().toString().trim();
        String birthday = etBirthday.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();

        if (!no.isEmpty()) updateData.setNo(no);
        if (!name.isEmpty()) updateData.setName(name);
        if (!idNumber.isEmpty()) updateData.setIdNumber(idNumber);
        if (!nativePlace.isEmpty()) updateData.setNativePlace(nativePlace);
        if (!birthday.isEmpty()) updateData.setBirthday(birthday);
        if (!weightStr.isEmpty()) updateData.setWeight(Integer.parseInt(weightStr));
        if (!heightStr.isEmpty()) updateData.setHeight(Double.parseDouble(heightStr));

        int genderPos = spinnerGender.getSelectedItemPosition();
        if (genderPos > 0) updateData.setGender(genderPos - 1);

        int ethnicPos = spinnerEthnic.getSelectedItemPosition();
        if (ethnicPos > 0) updateData.setEthnicGroup(ethnicPos - 1);

        // 发送更新请求 (PUT /api/users/me)
        AlertDialog loadingDialog = new AlertDialog.Builder(getActivity())
                .setMessage("正在保存...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        serverApi.updateCurrentUser(updateData, (success, message, updatedAccount) -> {
            getActivity().runOnUiThread(() -> {
                loadingDialog.dismiss();

                if (success && updatedAccount != null) {
                    currentAccount = updatedAccount;
                    saveCachedAccount(updatedAccount);
                    displayRemoteAccountInfo();
                    Toast.makeText(getActivity(), "资料更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ==================== 其他功能 ====================

    private void logout() {
        new AlertDialog.Builder(getActivity())
                .setTitle("退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 调用登出API（会自动清除本地Token）
                    serverApi.logout((success, message) -> {
                        // 必须在主线程更新UI
                        if (getActivity() == null || getActivity().isFinishing()) return;

                        getActivity().runOnUiThread(() -> {
                            // 无论API是否成功，都清除本地状态
                            userPrefs.edit()
                                    .remove("cached_account_json")
                                    .apply();
                            currentAccount = null;

                            // 清空聊天室缓存
                            ChatRoomActivity.clearChatCache(getActivity());

                            updateUI();
                            Toast.makeText(getActivity(), "已退出登录", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showSettingsDialog() {
        String[] options = {"通知开关", "清空缓存", "服务器地址"};
        new AlertDialog.Builder(getActivity())
                .setTitle("设置")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(getActivity(), "通知开关功能开发中", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "缓存已清空", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            showServerAddressDialog();
                            break;
                    }
                })
                .setPositiveButton("关闭", null)
                .show();
    }

    /**
     * 服务器地址配置对话框（模拟器/真机 二选一）
     */
    private void showServerAddressDialog() {
        String currentUrl = serverApi.getServerUrl();

        // 预设服务器地址选项
        final String URL_EMULATOR = "http://10.0.2.2:5002";   // Android模拟器访问宿主机
        final String URL_REAL_DEVICE = "http://192.168.137.1:5002";  // 真机通过电脑热点访问

        String[] options = {
                "📱 模拟器 (Android Emulator)\n    " + URL_EMULATOR + "\n    仅适用于Android Studio模拟器",
                "📲 真机调试 (Real Device)\n    " + URL_REAL_DEVICE + "\n    电脑开热点，手机连接后使用"
        };

        // 判断当前选中哪一项
        int checkedItem = currentUrl.contains("10.0.2.2") ? 0 : 1;

        new AlertDialog.Builder(getActivity())
                .setTitle("选择运行环境")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    // 选择后不自动关闭，等用户点确认
                })
                .setPositiveButton("确认", (dialog, which) -> {
                    ListView listView = ((AlertDialog) dialog).getListView();
                    int selected = listView.getCheckedItemPosition();
                    String selectedUrl = (selected == 0) ? URL_EMULATOR : URL_REAL_DEVICE;

                    serverApi.setServerUrl(selectedUrl);
                    Toast.makeText(getActivity(), "已切换到: " +
                            (selected == 0 ? "模拟器模式" : "真机模式"), Toast.LENGTH_SHORT).show();

                    // 测试连接
                    serverApi.testConnection((success, message) -> {
                        getActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(getActivity(), "✓ 服务器连接成功！", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "✗ 连接失败: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("关于我们")
                .setMessage(
                        "南通大学启东校区\n" +
                        "数字孪生平台\n\n" +
                        "版本号: v2.0.0\n\n" +
                        "基于讯飞星火Spark X2模型\n" +
                        "为校园生活提供智能化服务"
                )
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * 从JSON解析账号档案（复用）
     */
    private AccountProfile parseAccountFromJson(org.json.JSONObject json) throws Exception {
        AccountProfile profile = new AccountProfile();
        profile.setUserId(json.optLong("userId", -1));
        profile.setUserName(json.optString("userName", ""));

        if (!json.isNull("no")) profile.setNo(json.optString("no"));
        if (!json.isNull("name")) profile.setName(json.optString("name"));
        if (!json.isNull("idNumber")) profile.setIdNumber(json.optString("idNumber"));
        if (!json.isNull("gender")) profile.setGender(json.optInt("gender"));
        if (!json.isNull("ethnicGroup")) profile.setEthnicGroup(json.optInt("ethnicGroup"));
        if (!json.isNull("nativePlace")) profile.setNativePlace(json.optString("nativePlace"));
        if (!json.isNull("birthday")) profile.setBirthday(json.optString("birthday"));
        if (!json.isNull("weight")) profile.setWeight(json.optInt("weight"));
        if (!json.isNull("height")) profile.setHeight(json.optDouble("height"));

        profile.setHasProfile(json.optBoolean("hasProfile", false));
        if (!json.isNull("role")) profile.setRole(json.optString("role", "user"));

        return profile;
    }
}
