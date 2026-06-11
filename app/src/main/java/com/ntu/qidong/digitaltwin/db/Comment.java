package com.ntu.qidong.digitaltwin.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "comments",
        foreignKeys = {
            @ForeignKey(entity = Post.class, parentColumns = "id", childColumns = "postId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "authorId", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@androidx.room.Index(value = {"postId"}), @androidx.room.Index(value = {"authorId"})})
public class Comment {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int postId;
    private long authorId;
    private String content;
    private long createdAt;

    public Comment(int postId, long authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}