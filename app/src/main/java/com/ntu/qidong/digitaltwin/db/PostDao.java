package com.ntu.qidong.digitaltwin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PostDao {
    @Insert
    long insert(Post post);

    @Update
    void update(Post post);

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    List<Post> getAllPosts();

    @Query("SELECT * FROM posts WHERE id = :postId")
    Post getPostById(int postId);

    @Query("UPDATE posts SET likeCount = likeCount + 1 WHERE id = :postId")
    void incrementLikeCount(int postId);

    @Query("UPDATE posts SET commentCount = commentCount + 1 WHERE id = :postId")
    void incrementCommentCount(int postId);
}