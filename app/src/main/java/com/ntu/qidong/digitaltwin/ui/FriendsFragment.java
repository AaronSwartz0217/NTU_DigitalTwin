package com.ntu.qidong.digitaltwin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.fragment.app.Fragment;

import com.ntu.qidong.digitaltwin.R;

/**
 * 好友列表Fragment - 类似QQ好友列表界面
 */
public class FriendsFragment extends Fragment {

    private ScrollView friendsContainer;
    private LinearLayout emptyView;
    private LinearLayout publicChatroomEntry;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        
        initViews(view);
        setupClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        friendsContainer = view.findViewById(R.id.friends_container);
        emptyView = view.findViewById(R.id.empty_view);
        publicChatroomEntry = view.findViewById(R.id.ll_public_chatroom);
    }

    private void setupClickListeners() {
        // 公共聊天室点击事件
        if (publicChatroomEntry != null) {
            // 点击缩放动画
            publicChatroomEntry.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.setScaleX(1f);
                            v.setScaleY(1f);
                            // 跳转到公共聊天室
                            openPublicChatRoom();
                        })
                        .start();
            });
        }
    }

    /**
     * 打开公共聊天室
     */
    private void openPublicChatRoom() {
        Intent intent = new Intent(getActivity(), ChatRoomActivity.class);
        startActivity(intent);
    }
}
