package com.irlab.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.view.network.api.ApiService;
import com.irlab.view.bean.UserResponse;
import com.irlab.view.network.NetworkRequiredInfo;
import com.irlab.view.utils.ButtonListenerUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.utils.JsonUtil;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;

import okhttp3.RequestBody;

@SuppressLint("CheckResult")
@Route(path = "/auth/login")
public class LoginActivity extends Activity implements View.OnClickListener{

    public static final String Logger = LoginActivity.class.getName();

    private TextView register;
    private EditText userName;
    private EditText password;
    private Button login;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // 注入Router
        ARouter.getInstance().inject(this);
        // 获得SharedPreferences文件
        preferences = MyApplication.getInstance().preferences;
        // 初始化
        initView();
        // 设置事件
        setEvent();
        // 初始化network
        NetworkApi.init(new NetworkRequiredInfo(MyApplication.getInstance()));
    }

    private void initView() {
        register = findViewById(R.id.tv_register);
        userName = findViewById(R.id.et_userName);
        password = findViewById(R.id.et_password);
        login = findViewById(R.id.btn_login);
    }

    private void setEvent() {
        login.setOnClickListener(this);
        register.setOnClickListener(this);
        ButtonListenerUtil.buttonEnabled(login, 2, 8, userName, password);
        ButtonListenerUtil.buttonChangeColor(2, 8, this, login, userName, password);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.tv_register) {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if(vid == R.id.btn_login) {
            String userName = this.userName.getText().toString();
            String password = this.password.getText().toString();
            RequestBody requestBody = JsonUtil.addUser2Json(userName, password);
            Message msg = new Message();
            msg.obj = this;
            NetworkApi.createService(ApiService.class)
                    .checkUserInfo(requestBody)
                    .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                        @Override
                        public void onSuccess(UserResponse userResponse) {
                            String status = userResponse.getStatus();
                            if (status.equals("success")) {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("userName", userName);
                                editor.apply();
                                msg.what = ResponseCode.LOGIN_SUCCESSFULLY.getCode();
                            } else if (status.equals("userNameNotExist")) {
                                msg.what = ResponseCode.USER_NAME_NOT_REGISTER.getCode();
                            } else if (status.equals("wrongPassword")) {
                                msg.what = ResponseCode.WRONG_PASSWORD.getCode();
                            } else {
                                msg.what = ResponseCode.SERVER_FAILED.getCode();
                            }
                            handler.sendMessage(msg);
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            Log.e(Logger, "check user information onFailure:" + e.getMessage());
                            msg.what = ResponseCode.SERVER_FAILED.getCode();
                            handler.sendMessage(msg);
                        }
                    }));
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.SAVE_SGF_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SAVE_SGF_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.LOGIN_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.LOGIN_SUCCESSFULLY.getMsg());
                ARouter.getInstance().build("/view/main").withFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).navigation();
                finish();
            } else if (msg.what == ResponseCode.USER_NAME_NOT_REGISTER.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.USER_NAME_NOT_REGISTER.getMsg());
            } else if (msg.what == ResponseCode.WRONG_PASSWORD.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.WRONG_PASSWORD.getMsg());
            } else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            }
        }
    };
}