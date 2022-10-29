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
import com.irlab.view.utils.ButtonListenerUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.watcher.HideTextWatcher;
import com.irlab.view.watcher.ValidationWatcher;
import com.irlab.view.R;
import com.irlab.view.impl.DatabaseInterface;

public class RegisterActivity extends Activity implements View.OnClickListener {

    public static final String TAG = RegisterActivity.class.getName();

    public static final int MAX_LENGTH = 10;

    // 声明组件
    private ImageView imageView;
    private EditText userName;
    private EditText password;
    private EditText passwordConfirm;
    private Button register;
    private DatabaseInterface databaseInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // 初始化布局元素
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
        // 得到所有的组件
        imageView = findViewById(R.id.iv_return);
        userName = this.findViewById(R.id.et_userName);
        password = this.findViewById(R.id.et_psw);
        passwordConfirm = this.findViewById(R.id.et_pswConfirm);
        register = this.findViewById(R.id.btn_register);
        databaseInterface = new DatabaseInterface(this);
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
            int result = databaseInterface.checkName(userName.getText().toString(), password);
            Message msg = new Message();
            msg.obj = this;
            if (result == ResponseCode.ADD_USER_SUCCESSFULLY.getCode()) {
                msg.what = ResponseCode.ADD_USER_SUCCESSFULLY.getCode();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (result == ResponseCode.USER_ALREADY_REGISTERED.getCode()) {
                msg.what = ResponseCode.USER_ALREADY_REGISTERED.getCode();
            } else if (result == ResponseCode.SERVER_FAILED.getCode()) {
                msg.what = ResponseCode.SERVER_FAILED.getCode();
            } else if (result == ResponseCode.JSON_EXCEPTION.getCode()) {
                msg.what = ResponseCode.JSON_EXCEPTION.getCode();
            }
            handler.sendMessage(msg);
        }
        else if (vid == R.id.iv_return) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
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