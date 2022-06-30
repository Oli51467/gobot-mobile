package com.irlab.login;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.irlab.login.utils.ButtonListenerUtil;
import com.irlab.login.watcher.HideTextWatcher;
import com.irlab.login.watcher.ValidationWatcher;
import com.irlab.base.MyApplication;
import com.irlab.base.dao.UserDAO;
import com.irlab.base.entity.User;
import com.irlab.base.utils.ToastUtil;

public class Register extends Activity implements View.OnClickListener{

    public static final int MAX_LENGTH = 10;

    // 声明组件
    private ImageView imageView;

    private EditText userName;
    private EditText password;
    private EditText passwordConfirm;

    private Button register;

    private UserDAO userDAO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userDAO = MyApplication.getInstance().getUserDatabase().userDAO();

        // 初始化布局元素
        initViews();
        // 设置注册按钮是否可点击
        ButtonListenerUtil.buttonEnabled(register, userName, password, passwordConfirm);
        // 监听按钮变色
        ButtonListenerUtil.buttonChangeColor(this, register, userName, password, passwordConfirm); // 监听登录按钮变色
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
    }

    private void setListener() {
        register.setOnClickListener(this);
        imageView.setOnClickListener(this);
        userName.addTextChangedListener(new HideTextWatcher(userName, MAX_LENGTH, this));
        password.addTextChangedListener(new HideTextWatcher(password, MAX_LENGTH, this));
        passwordConfirm.addTextChangedListener(new HideTextWatcher(passwordConfirm, MAX_LENGTH, this));
        userName.addTextChangedListener(new ValidationWatcher(userName, 3,8, "用户名"));
        password.addTextChangedListener(new ValidationWatcher(password, 6, 12,"密码"));

    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_register) {
            String userName = this.userName.getText().toString();
            String password = this.password.getText().toString();
            String passwordConfirm = this.passwordConfirm.getText().toString();
            if (userDAO.findByName(userName) == null) {
                if (password.equals(passwordConfirm)) {
                    User user = new User();
                    user.setName(userName);
                    user.setPassWord(password);
                    userDAO.insert(user);
                    ToastUtil.show(this, "注册成功");
                    Intent intent = new Intent(this, Login.class);
                    // 同时设置 Flag_ACTIVITY_SINGLE_TOP, 则直接使用栈内的对应 Activity
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                else {
                    ToastUtil.show(this, "两次输入的密码不一致!");
                }
            }
            else {
                ToastUtil.show(this, "该用户名已注册");
            }
        }
        else if (vid == R.id.iv_return) {
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }
}