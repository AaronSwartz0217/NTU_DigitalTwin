package com.ntu.qidong.digitaltwin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ntu.qidong.digitaltwin.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 聊天消息适配器 - 支持自己发送和他人发送的消息
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SELF = 1;      // 自己的消息（右侧）
    private static final int VIEW_TYPE_OTHER = 2;     // 他人的消息（左侧）

    private final Context context;
    private final List<ChatRoomActivity.ChatMessage> messages;

    public ChatMessageAdapter(Context context, List<ChatRoomActivity.ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatRoomActivity.ChatMessage message = messages.get(position);
        return message.isSelf() ? VIEW_TYPE_SELF : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        
        if (viewType == VIEW_TYPE_SELF) {
            // 自己的消息：右对齐
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_self, parent, false);
            return new SelfMessageViewHolder(view);
        } else {
            // 他人的消息：左对齐
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_other, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatRoomActivity.ChatMessage message = messages.get(position);
        
        if (holder instanceof SelfMessageViewHolder) {
            SelfMessageViewHolder selfHolder = (SelfMessageViewHolder) holder;
            selfHolder.tvContent.setText(message.getContent());
            selfHolder.tvTime.setText(formatTime(message.getTimestamp()));
        } else if (holder instanceof OtherMessageViewHolder) {
            OtherMessageViewHolder otherHolder = (OtherMessageViewHolder) holder;
            otherHolder.tvUserName.setText(message.getUserName());
            otherHolder.tvContent.setText(message.getContent());
            otherHolder.tvTime.setText(formatTime(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    // ==================== ViewHolder类 ====================

    static class SelfMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvTime;

        SelfMessageViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }
    }

    static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        TextView tvContent;
        TextView tvTime;

        OtherMessageViewHolder(View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }
    }
}
