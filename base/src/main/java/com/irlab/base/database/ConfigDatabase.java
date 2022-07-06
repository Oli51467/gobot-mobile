package com.irlab.base.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.irlab.base.dao.ConfigDAO;
import com.irlab.base.entity.Config;

@Database(entities = {Config.class}, version = 1, exportSchema = false)
public abstract class ConfigDatabase extends RoomDatabase {

    private static final String DB_NAME = "Config.db";
    private static volatile ConfigDatabase instance;

    public static synchronized ConfigDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static ConfigDatabase create(final Context context) {
        return Room.databaseBuilder(context, ConfigDatabase.class, DB_NAME).
                allowMainThreadQueries().
                addMigrations().
                build();
    }

    public abstract ConfigDAO configDAO();
}
