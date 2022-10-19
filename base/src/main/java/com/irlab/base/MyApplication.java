package com.irlab.base;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;

public class MyApplication extends Application {

    public static final String SERVER = "http://8.142.10.225:8081";
    public static final String ENGINE_SERVER = "http://8.142.10.225:5000"; // 阿里云服务器
    public static final String[] T = {"让先", "让2子", "让3子", "让4子", "让5子", "让6子", "让7子", "让8子", "让9子"};
    public static final String[] ENGINES = {"10级", "9级", "8级", "7级", "6级", "5级", "4级", "3级", "2级", "1级",
            "业余1段", "业余2段", "业余3段", "业余4段", "业余5段", "业余6段", "职业初段", "P2", "P3"};
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String TAG = MyApplication.class.getName();
    public static final String appid = "a716e470";
    public static final int THREAD_NUM = 19;
    public static boolean initEngine = false;
    public static ThreadPoolExecutor threadPool;
    private static MyApplication MyApp; // 提供自己的唯一实例
    protected static Context context;

    public SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
        MyApp = this;
        ARouter.openLog();
        ARouter.openDebug();
        ARouter.init(MyApplication.this);

        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        // 初始化线程池 可复用
        threadPool = new ThreadPoolExecutor(THREAD_NUM, 30, 5, TimeUnit.MINUTES,
                new LinkedBlockingDeque<>());
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
