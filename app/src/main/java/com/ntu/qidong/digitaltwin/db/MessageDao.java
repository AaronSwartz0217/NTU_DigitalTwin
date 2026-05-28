package com.ntu.qidong.digitaltwin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    long insert(Message message);

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    List<Message> getAllMessages();

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    int getUnreadCount();

    @Query("UPDATE messages SET isRead = 1 WHERE id = :messageId")
    void markAsRead(int messageId);

    @Query("UPDATE messages SET isRead = 1")
    void markAllAsRead();
}