package com.ntu.qidong.digitaltwin.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int type;
    private String title;
    private String content;
    private int relatedPostId;
    private boolean isRead;
    private long createdAt;

    public static final int TYPE_REPLY = 1;
    public static final int TYPE_LIKE = 2;

    public Message(int type, String title, String content, int relatedPostId) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.relatedPostId = relatedPostId;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getRelatedPostId() { return relatedPostId; }
    public void setRelatedPostId(int relatedPostId) { this.relatedPostId = relatedPostId; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}