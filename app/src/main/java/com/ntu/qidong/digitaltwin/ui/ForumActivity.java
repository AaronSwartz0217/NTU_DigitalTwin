package com.ntu.qidong.digitaltwin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ntu.qidong.digitaltwin.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForumActivity extends AppCompatActivity {

    private EditText etPostContent;
    private Button btnPost;
    private LinearLayout postsContainer;
    private ScrollView scrollView;
    private Button btnEnterDigitalTwin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        initViews();
    }

    private void initViews() {
        etPostContent = findViewById(R.id.et_post_content);
        btnPost = findViewById(R.id.btn_post);
        postsContainer = findViewById(R.id.posts_container);
        scrollView = findViewById(R.id.scroll_view);
        btnEnterDigitalTwin = findViewById(R.id.btn_enter_digital_twin);

        // 发布按钮点击事件
        btnPost.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();
            if (!content.isEmpty()) {
                addPost(content);
                etPostContent.setText("");
            }
        });

        // 进入数字孪生按钮点击事件 - 先播放开屏动画，再进入数字孪生
        btnEnterDigitalTwin.setOnClickListener(v -> {
            Intent intent = new Intent(ForumActivity.this, SplashActivity.class);
            startActivity(intent);
        });

        // 添加一些示例帖子
        addSamplePosts();
    }

    private void addPost(String content) {
        LinearLayout postItem = new LinearLayout(this);
        postItem.setOrientation(LinearLayout.VERTICAL);
        postItem.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        postItem.setBackgroundResource(R.drawable.panel_background_transparent);
        postItem.setPadding(12, 12, 12, 12);

        // 时间戳
        TextView timeText = new TextView(this);
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                .format(new Date());
        timeText.setText(currentTime);
        timeText.setTextColor(getResources().getColor(R.color.cyan));
        timeText.setTextSize(10);
        postItem.addView(timeText);

        // 内容
        TextView contentText = new TextView(this);
        contentText.setText(content);
        contentText.setTextColor(getResources().getColor(R.color.white));
        contentText.setTextSize(12);
        contentText.setPadding(0, 8, 0, 0);
        postItem.addView(contentText);

        // 添加到容器
        postsContainer.addView(postItem, 0);

        // 滚动到顶部
        scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
    }

    private void addSamplePosts() {
        addPost("欢迎来到南通大学启东校区数字孪生论坛！");
        addPost("这是第一个示例帖子，大家可以在这里交流讨论。");
        addPost("论坛功能刚刚上线，欢迎大家提出建议和反馈。");
    }
}