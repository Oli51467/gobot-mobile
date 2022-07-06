package com.irlab.base.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.irlab.base.entity.Config;

import java.util.List;

@Dao
public interface ConfigDAO {
    @Insert
    void insert(Config... configs);

    @Query("SELECT * FROM Config WHERE title = :title ORDER BY id DESC limit 1")
    Config findByTitle(String title);

    @Query("SELECT * FROM Config WHERE id = :id")
    Config findById(int id);

    @Query("SELECT * FROM Config")
    List<Config> findAll();

    @Query("DELETE FROM Config")
    void deleteAll();

    @Update
    void update(Config config);

    @Query("DELETE FROM Config WHERE id = :id")
    void deleteById(int id);
}
