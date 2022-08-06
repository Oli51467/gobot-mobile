package com.irlab.base;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.utils.ToastUtil;

import okhttp3.MediaType;

public class MyApplication extends Application {

    public static final String SERVER = "http://8.142.10.225:8081";
    public static final String ENGINE_SERVER = "";
    public static final String[] T = {"让先", "让2子", "让3子", "让4子", "让5子", "让6子", "让7子", "让8子", "让9子"};
    public static final String[] ENGINES = {"b20", "b40"};
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String TAG = "MyApplication";
    public static SqueezeNcnn squeezencnn;
    public static boolean initNet = false;
    private static MyApplication MyApp; // 提供自己的唯一实例
    protected static Context context;

    private boolean isDebugARouter = true;
    public SharedPreferences preferences;
    public MyTask initNcnn;

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
        // 初始化Ncnn
        initNcnn = new MyTask();
        initNcnn.execute(squeezencnn);
        Log.d(TAG, "onCreate");
    }

    // 提供获取自己实例的唯一方法
    public synchronized static MyApplication getInstance() {
        return MyApp;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        initNcnn.cancel(true);
    }

    public static Context getContext() {
        return context;
    }

    @SuppressLint("StaticFieldLeak")
    public class MyTask extends AsyncTask<SqueezeNcnn, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(SqueezeNcnn... squeezeNcnns) {
            squeezencnn = new SqueezeNcnn();
            boolean ret_init = squeezencnn.Init(getAssets());
            if (!ret_init) {
                Log.e(TAG, "squeezencnn Init failed");
            }
            else initNet = true;
            return ret_init;
        }
    }
}
