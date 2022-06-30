package com.irlab.base.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.irlab.base.dao.UserDAO;
import com.irlab.base.entity.User;


@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {

    public abstract UserDAO userDAO();
}
