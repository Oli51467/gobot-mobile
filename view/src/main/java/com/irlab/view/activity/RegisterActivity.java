package com.irlab.view.activity;

import android.annotation.SuppressLint;
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
import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.SPUtils;
import com.irlab.view.network.api.ApiService;
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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.RequestBody;

@SuppressLint("checkResult")
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = RegisterActivity.class.getName();
    public static final int MAX_LENGTH = 11;

    // 声明组件
    private ImageView imageView;
    private EditText userName, password, passwordConfirm, phoneNumber, email;
    private Button register;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Objects.requireNonNull(getSupportActionBar()).hide();
        initViews();
        // 设置注册按钮是否可点击
        ButtonListenerUtil.buttonEnabled(2, 11, register, userName, password, passwordConfirm, phoneNumber);
        // 监听按钮变色
        ButtonListenerUtil.buttonChangeColor(2, 11, this, register, userName, password, passwordConfirm, phoneNumber);
        // 设置点击事件
        setListener();
    }

    /*
    获取到每个需要用到的控件的实例
    */
    public void initViews() {
        imageView = findViewById(R.id.iv_return);
        userName = findViewById(R.id.et_userName);
        password = findViewById(R.id.et_psw);
        passwordConfirm = findViewById(R.id.et_pswConfirm);
        register = findViewById(R.id.btn_register);
        phoneNumber = findViewById(R.id.et_phone);
        email = findViewById(R.id.et_email);
    }

    private void setListener() {
        register.setOnClickListener(this);
        imageView.setOnClickListener(this);
        userName.addTextChangedListener(new HideTextWatcher(userName, MAX_LENGTH, this));
        password.addTextChangedListener(new HideTextWatcher(password, MAX_LENGTH, this));
        phoneNumber.addTextChangedListener(new HideTextWatcher(phoneNumber, MAX_LENGTH, this));
        passwordConfirm.addTextChangedListener(new HideTextWatcher(passwordConfirm, MAX_LENGTH, this));
        userName.addTextChangedListener(new ValidationWatcher(userName, 3,8, "用户名"));
        password.addTextChangedListener(new ValidationWatcher(password, 3, 8,"密码"));
        phoneNumber.addTextChangedListener(new ValidationWatcher(phoneNumber, 0, 11, "手机号"));
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_register) {
            String password = this.password.getText().toString();
            String passwordConfirm = this.passwordConfirm.getText().toString();
            String phoneNum = this.phoneNumber.getText().toString();
            String email = this.email.getText().toString();
            if (!password.equals(passwordConfirm)) {
                ToastUtil.show(this, "两次输入的密码不一致!");
                return;
            } else if (!isValidPhoneNumber(phoneNum)) {
                ToastUtil.show(this, "手机号格式不正确!");
                return;
            } else if (!isValidEmail(email)) {
                ToastUtil.show(this, "邮箱格式不正确!");
                return;
            }
            Message msg = new Message();
            msg.obj = this;
            RequestBody requestBody = JsonUtil.userNamePhoneNumber2Json(userName.getText().toString(), phoneNum);
            NetworkApi.createService(ApiService.class)
                    .checkUser(requestBody)
                    .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                        @Override
                        public void onSuccess(UserResponse userResponse) {
                            int code = userResponse.getCode();
                            // 用户名没有被注册
                            if (code == 404) {
                                addUser(userName.getText().toString(), password, phoneNum, email);
                            } else {    // 用户名已被注册
                                msg.what = ResponseCode.USER_ALREADY_REGISTERED.getCode();
                                handler.sendMessage(msg);
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            msg.what = ResponseCode.SERVER_FAILED.getCode();
                            handler.sendMessage(msg);
                        }
                    }));
        }
        else if (vid == R.id.iv_return) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if ((phoneNumber != null) && (!phoneNumber.isEmpty())) {
            return Pattern.matches("^1[3-9]\\d{9}$", phoneNumber);
        }
        return false;
    }

    /**
     * 判断输入的邮箱格式是否正确
     * @param str 输入的邮箱地址
     * @return 返回邮箱地址是否正确
     */
    public static boolean isValidEmail(String str) {
        String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p;
        Matcher m;
        p = Pattern.compile(regEx1);
        m = p.matcher(str);
        return m.matches();
    }

    private void addUser(String userName, String password, String phoneNum, String email) {
        Message msg = new Message();
        msg.obj = this;
        RequestBody requestBody = JsonUtil.Register2Json(userName, password, phoneNum, email);
        NetworkApi.createService(ApiService.class)
                .addUser(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        String status = userResponse.getStatus();
                        if (status.equals("success")) {
                            SPUtils.saveInt("play_level", 1);
                            SPUtils.saveString("phone_number", phoneNum);
                            SPUtils.saveString("password", password);
                            SPUtils.saveString("email", email);
                            SPUtils.saveInt("win", 0);
                            SPUtils.saveInt("lose", 0);
                            msg.what = ResponseCode.ADD_USER_SUCCESSFULLY.getCode();
                            handler.sendMessage(msg);
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
                ToastUtil.show((AppCompatActivity) msg.obj, 1, ResponseCode.USER_ALREADY_REGISTERED.getMsg());
            } else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((AppCompatActivity) msg.obj, 2, ResponseCode.SERVER_FAILED.getMsg());
            } else if (msg.what == ResponseCode.JSON_EXCEPTION.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.JSON_EXCEPTION.getMsg());
            } else if (msg.what == ResponseCode.ADD_USER_SUCCESSFULLY.getCode()) {
                ToastUtil.show((AppCompatActivity) msg.obj, 0, ResponseCode.ADD_USER_SUCCESSFULLY.getMsg());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        }
    };
}