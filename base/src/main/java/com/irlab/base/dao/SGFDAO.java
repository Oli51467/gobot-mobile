package com.irlab.base.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.irlab.base.entity.SGF;

import java.util.List;

@Dao
public interface SGFDAO {

    @Insert
    void insert(SGF... sgfs);

    @Query("SELECT * FROM SGF WHERE id = :id")
    SGF findById(int id);

    @Query("SELECT * FROM SGF")
    List<SGF> findAll();

    @Query("DELETE FROM SGF")
    void deleteAll();
}
