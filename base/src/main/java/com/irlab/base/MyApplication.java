package com.irlab.base;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.database.ConfigDatabase;

public class MyApplication extends Application {
    public static final String TAG = "MyApplication";

    private boolean isDebugARouter = true;

    // 提供自己的唯一实例
    private static MyApplication MyApp;

    // 声明数据库对象
    private ConfigDatabase configDatabase;

    // 声明公共的信息映射对象, 可当作全局变量使用 读内存比读磁盘快很多
    public SharedPreferences preferences;

    protected static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
        MyApp = this;
        if (isDebugARouter) {
            ARouter.openLog();
            ARouter.openDebug();
        }
        ARouter.init(MyApplication.this);
        // 建立数据库、初始化数据库对象
        configDatabase = ConfigDatabase.getInstance(context);

        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate");
    }

    // 提供获取自己实例的唯一方法
    public synchronized static MyApplication getInstance() {
        return MyApp;
    }

    // 提供获取数据库实例的方法

    public synchronized ConfigDatabase getConfigDatabase() { return configDatabase; }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static Context getContext() {
        return context;
    }
}
