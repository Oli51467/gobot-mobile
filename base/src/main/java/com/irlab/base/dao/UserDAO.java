package com.irlab.base.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.irlab.base.entity.User;


@Dao
public interface UserDAO {

    @Insert
    void insert(User... user);

    @Query("SELECT * FROM User WHERE name = :name ORDER BY id DESC limit 1")
    User findByName(String name);
}
