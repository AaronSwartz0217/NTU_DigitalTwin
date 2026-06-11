package com.ntu.qidong.digitaltwin.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private long userId;

    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private boolean isAdmin;  // 管理员标识
    private long createdAt;

    public User(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = "";
        this.isAdmin = false;
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public User(String username, String password, String nickname, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = "";
        this.isAdmin = isAdmin;
        this.createdAt = System.currentTimeMillis();
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isAdmin() { return isAdmin; }
    public void setIsAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}