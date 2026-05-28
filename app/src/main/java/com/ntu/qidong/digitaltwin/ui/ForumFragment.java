package com.ntu.qidong.digitaltwin.ui;

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

import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.db.Comment;
import com.ntu.qidong.digitaltwin.db.Post;
import com.ntu.qidong.digitaltwin.db.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForumFragment extends Fragment {

    private EditText etTitle, etContent, etComment;
    private Button btnPost, btnNewPost, btnCancelPost, btnComment, btnBack;
    private LinearLayout postsContainer, postDetailLayout, postDialogLayout, commentsContainer;
    private TextView tvWelcome, tvPostCount, tvDetailTitle, tvDetailAuthor, tvDetailTime, tvDetailContent, tvDetailLikes, tvDetailComments;

    private SharedPreferences userPrefs;
    private AppDatabase db;
    private long currentUserId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);

        initViews(view);
        initData();
        loadPosts();
        updateWelcome();

        return view;
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

        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvPostCount = view.findViewById(R.id.tv_post_count);
        tvDetailTitle = view.findViewById(R.id.tv_detail_title);
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
    }

    private void updateWelcome() {
        if (currentUserId > 0) {
            User user = db.userDao().getUserById(currentUserId);
            if (user != null) {
                tvWelcome.setText("Welcome back, " + user.getNickname() + "!");
            }
        } else {
            tvWelcome.setText("Welcome to Digital Twin Forum!");
        }
    }

    private void showPostDialog() {
        postDialogLayout.setVisibility(View.VISIBLE);
    }

    private void hidePostDialog() {
        postDialogLayout.setVisibility(View.GONE);
        etTitle.setText("");
        etContent.setText("");
    }

    private void publishPost() {
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

        Post post = new Post(title, content, currentUserId);
        db.postDao().insert(post);

        hidePostDialog();
        loadPosts();
        updateWelcome();

        Toast.makeText(getActivity(), "发布成功！", Toast.LENGTH_SHORT).show();
    }

    private void loadPosts() {
        postsContainer.removeAllViews();

        List<Post> posts = db.postDao().getAllPosts();
        tvPostCount.setText(posts.size() + " 篇帖子");

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            User author = db.userDao().getUserById(post.getAuthorId());
            String authorName = author != null ? author.getNickname() : "匿名用户";

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
            postItem.setOnClickListener(v -> showPostDetail(post));

            TextView titleText = new TextView(getActivity());
            titleText.setText(post.getTitle());
            titleText.setTextColor(0xFF1F2937);
            titleText.setTextSize(18);
            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
            postItem.addView(titleText);

            TextView metaText = new TextView(getActivity());
            metaText.setText(authorName + " · " + formatTime(post.getCreatedAt()));
            metaText.setTextColor(0xFF6B7280);
            metaText.setTextSize(14);
            metaText.setPadding(0, 8, 0, 0);
            postItem.addView(metaText);

            if (post.getContent() != null && !post.getContent().isEmpty()) {
                TextView contentPreview = new TextView(getActivity());
                String content = post.getContent();
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                contentPreview.setText(content);
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
            likesText.setText("❤️ " + post.getLikeCount());
            likesText.setTextColor(0xFF9CA3AF);
            likesText.setTextSize(13);
            statsRow.addView(likesText);

            TextView commentsText = new TextView(getActivity());
            commentsText.setText("💬 " + post.getCommentCount());
            commentsText.setTextColor(0xFF9CA3AF);
            commentsText.setTextSize(13);
            commentsText.setPadding(20, 0, 0, 0);
            statsRow.addView(commentsText);

            postItem.addView(statsRow);
            postsContainer.addView(postItem);
        }
    }

    private void showPostDetail(Post post) {
        User author = db.userDao().getUserById(post.getAuthorId());
        String authorName = author != null ? author.getNickname() : "匿名用户";

        tvDetailTitle.setText(post.getTitle());
        tvDetailAuthor.setText(authorName);
        tvDetailTime.setText(formatTime(post.getCreatedAt()));
        tvDetailContent.setText(post.getContent());
        tvDetailLikes.setText("点赞: " + post.getLikeCount());
        tvDetailComments.setText("评论: " + post.getCommentCount());

        loadComments(post.getId());

        postDetailLayout.setVisibility(View.VISIBLE);
    }

    private void loadComments(int postId) {
        commentsContainer.removeAllViews();

        List<Comment> comments = db.commentDao().getCommentsByPostId(postId);

        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            User author = db.userDao().getUserById(comment.getAuthorId());
            String authorName = author != null ? author.getNickname() : "匿名用户";

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
            timeText.setText(formatTime(comment.getCreatedAt()));
            timeText.setTextColor(0xFF9CA3AF);
            timeText.setTextSize(12);
            timeText.setPadding(0, 4, 0, 0);
            commentItem.addView(timeText);

            TextView contentText = new TextView(getActivity());
            contentText.setText(comment.getContent());
            contentText.setTextColor(0xFF4B5563);
            contentText.setTextSize(15);
            contentText.setPadding(0, 8, 0, 0);
            contentText.setLineSpacing(4f, 1.3f);
            commentItem.addView(contentText);

            commentsContainer.addView(commentItem);
        }
    }

    private void addComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getActivity(), "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        int postId = getCurrentPostId();
        Comment comment = new Comment(postId, currentUserId, content);
        db.commentDao().insert(comment);
        db.postDao().incrementCommentCount(postId);

        etComment.setText("");
        loadComments(postId);
        loadPosts();

        Toast.makeText(getActivity(), "评论成功！", Toast.LENGTH_SHORT).show();
    }

    private int getCurrentPostId() {
        String title = tvDetailTitle.getText().toString();
        List<Post> posts = db.postDao().getAllPosts();
        for (Post post : posts) {
            if (post.getTitle().equals(title)) {
                return post.getId();
            }
        }
        return 0;
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    public void backToList() {
        postDetailLayout.setVisibility(View.GONE);
        loadPosts();
    }
}