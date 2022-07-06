package com.irlab.robotgo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.MyApplication;

@Route(path = "/app/main")
public class MainActivity extends Activity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = MyApplication.getInstance().preferences;
        ARouter.getInstance().inject(this);
        //initCv();
        enter();
    }

    /**
    根据用户是否登录决定跳转到哪个界面
     */
    private void enter() {
        if (preferences.getString("userName", null) != null) {
            ARouter.getInstance()
                    .build("/view/main")
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .navigation();
            finish();
        }
        else {
            ARouter.getInstance()
                    .build("/auth/login")
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .navigation();
            finish();
        }
    }
}