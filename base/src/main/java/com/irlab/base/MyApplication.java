package com.irlab.base;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;

import okhttp3.MediaType;

public class MyApplication extends Application {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final String TAG = "MyApplication";

    private boolean isDebugARouter = true;

    // 提供自己的唯一实例
    private static MyApplication MyApp;

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

        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate");
    }

    // 提供获取自己实例的唯一方法
    public synchronized static MyApplication getInstance() {
        return MyApp;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static Context getContext() {
        return context;
    }
}
