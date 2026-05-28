package com.ntu.qidong.digitaltwin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CommentDao {
    @Insert
    long insert(Comment comment);

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    List<Comment> getCommentsByPostId(int postId);
}