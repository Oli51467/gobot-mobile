package com.irlab.base;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.room.Room;

import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.database.UserDatabase;
import com.irlab.base.entity.User;

import java.util.HashMap;

public class MyApplication extends Application {
    public static final String TAG = "MyApplication";

    private boolean isDebugARouter = true;

    // 提供自己的唯一实例
    private static MyApplication MyApp;

    // 声明数据库对象
    private UserDatabase userDatabase;

    // 声明公共的信息映射对象, 可当作全局变量使用 读内存比读磁盘快很多
    public SharedPreferences preferences;
    public HashMap<String, String> infoMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        MyApp = this;
        if (isDebugARouter) {
            ARouter.openLog();
            ARouter.openDebug();
        }
        ARouter.init(MyApplication.this);
        // 建立数据库、初始化数据库对象
        userDatabase = Room.databaseBuilder(this, UserDatabase.class, "User")
                // 暂时允许在主线程中操作数据库 因为太耗时
                .allowMainThreadQueries()
                // 允许迁移数据库 数据库变更时默认删除原数据库再创建新数据库
                .addMigrations()
                .build();
        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate");
    }

    // 提供获取自己实例的唯一方法
    public synchronized static MyApplication getInstance() {
        return MyApp;
    }

    // 提供获取数据库实例的方法
    public synchronized UserDatabase getUserDatabase() {
        return userDatabase;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
