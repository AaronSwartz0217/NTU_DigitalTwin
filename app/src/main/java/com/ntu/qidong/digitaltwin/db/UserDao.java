package com.ntu.qidong.digitaltwin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    User login(String username, String password);

    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(long userId);

    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    @Update
    void update(User user);

    // 管理员相关查询
    @Query("SELECT * FROM users WHERE isAdmin = 1 LIMIT 1")
    User getAdminUser();

    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId AND isAdmin = 1")
    boolean isUserAdmin(long userId);
}