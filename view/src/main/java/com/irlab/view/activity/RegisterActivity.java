package com.irlab.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.irlab.base.response.ResponseCode;
import com.irlab.view.api.ApiService;
import com.irlab.view.bean.UserResponse;
import com.irlab.view.utils.ButtonListenerUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.utils.JsonUtil;
import com.irlab.view.watcher.HideTextWatcher;
import com.irlab.view.watcher.ValidationWatcher;
import com.irlab.view.R;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;
import com.sdu.network.utils.KLog;

import okhttp3.RequestBody;

@SuppressLint("checkResult")
public class RegisterActivity extends Activity implements View.OnClickListener {

    public static final String TAG = RegisterActivity.class.getName();
    public static final int MAX_LENGTH = 10;

    // 声明组件
    private ImageView imageView;
    private EditText userName;
    private EditText password;
    private EditText passwordConfirm;
    private Button register;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        // 设置注册按钮是否可点击
        ButtonListenerUtil.buttonEnabled(register, 2, 8, userName, password, passwordConfirm);
        // 监听按钮变色
        ButtonListenerUtil.buttonChangeColor(2, 8, this, register, userName, password, passwordConfirm); // 监听登录按钮变色
        // 设置点击事件
        setListener();
    }

    /*
    获取到每个需要用到的控件的实例
    */
    public void initViews() {
        imageView = findViewById(R.id.iv_return);
        userName = this.findViewById(R.id.et_userName);
        password = this.findViewById(R.id.et_psw);
        passwordConfirm = this.findViewById(R.id.et_pswConfirm);
        register = this.findViewById(R.id.btn_register);
    }

    private void setListener() {
        register.setOnClickListener(this);
        imageView.setOnClickListener(this);
        userName.addTextChangedListener(new HideTextWatcher(userName, MAX_LENGTH, this));
        password.addTextChangedListener(new HideTextWatcher(password, MAX_LENGTH, this));
        passwordConfirm.addTextChangedListener(new HideTextWatcher(passwordConfirm, MAX_LENGTH, this));
        userName.addTextChangedListener(new ValidationWatcher(userName, 3,8, "用户名"));
        password.addTextChangedListener(new ValidationWatcher(password, 3, 8,"密码"));
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_register) {
            String password = this.password.getText().toString();
            String passwordConfirm = this.passwordConfirm.getText().toString();
            if (!password.equals(passwordConfirm)) {
                ToastUtil.show(this, "两次输入的密码不一致!");
                return;
            }
            Message msg = new Message();
            msg.obj = this;
            NetworkApi.createService(ApiService.class)
                    .checkUser(userName.getText().toString())
                    .compose(NetworkApi.applySchedulers(new BaseObserver<UserResponse>() {
                        @Override
                        public void onSuccess(UserResponse userResponse) {
                            String status = userResponse.getStatus();
                            // 用户名没有被注册
                            if (status.equals("nullObject")) {
                                RequestBody requestBody = JsonUtil.addUser2Json(userName.getText().toString(), password);
                                addUser(requestBody);
                            } else {    // 用户名已被注册
                                msg.what = ResponseCode.USER_ALREADY_REGISTERED.getCode();
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            msg.what = ResponseCode.SERVER_FAILED.getCode();
                        }
                    }));
            handler.sendMessage(msg);
        }
        else if (vid == R.id.iv_return) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private void addUser(RequestBody requestBody) {
        Message msg = new Message();
        msg.obj = RegisterActivity.this;
        NetworkApi.createService(ApiService.class)
                .addUser(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<UserResponse>() {
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        String status = userResponse.getStatus();
                        if (status.equals("success")) {
                            msg.what = ResponseCode.ADD_USER_SUCCESSFULLY.getCode();
                            handler.sendMessage(msg);
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        msg.what = ResponseCode.SERVER_FAILED.getCode();
                        handler.sendMessage(msg);
                        KLog.e("RegisterActivity", e.toString());
                    }
                }));
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.USER_ALREADY_REGISTERED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.USER_ALREADY_REGISTERED.getMsg());
            } else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            } else if (msg.what == ResponseCode.JSON_EXCEPTION.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.JSON_EXCEPTION.getMsg());
            } else if (msg.what == ResponseCode.ADD_USER_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ADD_USER_SUCCESSFULLY.getMsg());
            }
        }
    };
}