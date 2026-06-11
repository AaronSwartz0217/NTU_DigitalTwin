package com.ntu.qidong.digitaltwin.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;
import com.ntu.qidong.digitaltwin.db.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageFragment extends Fragment {

    private LinearLayout messagesContainer;
    private AppDatabase db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        messagesContainer = view.findViewById(R.id.messages_container);
        db = AppDatabase.getInstance(getActivity());

        loadMessages();

        return view;
    }

    private void loadMessages() {
        messagesContainer.removeAllViews();

        List<Message> messages = db.messageDao().getAllMessages();

        if (messages.isEmpty()) {
            TextView emptyText = new TextView(getActivity());
            emptyText.setText("暂无消息");
            emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, 20, 0, 0);
            emptyText.setGravity(View.TEXT_ALIGNMENT_CENTER);
            messagesContainer.addView(emptyText);
            return;
        }

        for (Message message : messages) {
            LinearLayout messageItem = new LinearLayout(getActivity());
            messageItem.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            messageItem.setLayoutParams(params);
            messageItem.setBackgroundResource(R.drawable.post_card);
            messageItem.setPadding(16, 16, 16, 16);
            messageItem.setClickable(true);
            messageItem.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.setScaleX(1f);
                            v.setScaleY(1f);
                            handleMessageClick(message);
                        })
                        .start();
            });

            TextView titleText = new TextView(getActivity());
            titleText.setText(message.getTitle());
            titleText.setTextColor(0xFF1A1A2E);
            titleText.setTextSize(16);
            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
            messageItem.addView(titleText);

            TextView contentText = new TextView(getActivity());
            contentText.setText(message.getContent());
            contentText.setTextColor(0xFF8E8E93);
            contentText.setTextSize(13);
            contentText.setPadding(0, 8, 0, 0);
            contentText.setLineSpacing(0, 1.4f);
            messageItem.addView(contentText);

            TextView timeText = new TextView(getActivity());
            timeText.setText(formatTime(message.getCreatedAt()));
            timeText.setTextColor(0xFF8E8E93);
            timeText.setTextSize(12);
            timeText.setPadding(0, 8, 0, 0);
            messageItem.addView(timeText);

            if (!message.isRead()) {
                View unreadIndicator = new View(getActivity());
                LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(8, 8);
                indicatorParams.setMargins(0, 8, 0, 0);
                unreadIndicator.setLayoutParams(indicatorParams);
                unreadIndicator.setBackgroundColor(0xFF4A6CF7);
                messageItem.addView(unreadIndicator);
            }

            messagesContainer.addView(messageItem);
        }
    }

    private void handleMessageClick(Message message) {
        db.messageDao().markAsRead(message.getId());
        loadMessages();
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    public void refreshMessages() {
        loadMessages();
    }
}