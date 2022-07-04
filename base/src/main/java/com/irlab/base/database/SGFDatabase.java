package com.irlab.base.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.irlab.base.dao.SGFDAO;
import com.irlab.base.dao.UserDAO;
import com.irlab.base.entity.SGF;
import com.irlab.base.entity.User;

@Database(entities = {SGF.class}, version = 1, exportSchema = false)
public abstract class SGFDatabase extends RoomDatabase {

    private static final String DB_NAME = "SGFDatabase.db";
    private static volatile SGFDatabase instance;

    public static synchronized SGFDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static SGFDatabase create(final Context context) {
        return Room.databaseBuilder(
                context,
                SGFDatabase.class,
                DB_NAME).build();
    }

    public abstract SGFDAO sgfDAO();
}
