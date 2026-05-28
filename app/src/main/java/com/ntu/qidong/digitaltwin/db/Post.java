package com.ntu.qidong.digitaltwin.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "posts",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "authorId", onDelete = ForeignKey.SET_NULL))
public class Post {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String content;
    private long authorId;
    private long createdAt;
    private int likeCount;
    private int commentCount;

    public Post(String title, String content, long authorId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.createdAt = System.currentTimeMillis();
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}