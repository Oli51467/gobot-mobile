package com.irlab.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.MyApplication;
import com.irlab.base.dao.UserDAO;
import com.irlab.base.entity.User;
import com.irlab.base.utils.ButtonListenerUtil;
import com.irlab.base.utils.ToastUtil;

@Route(path = "/auth/login")
public class Login extends Activity implements View.OnClickListener{

    private TextView register;

    private EditText userName;

    private EditText password;

    private Button login;

    private UserDAO userDAO;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userDAO = MyApplication.getInstance().getUserDatabase().userDAO();
        // 注入Router
        ARouter.getInstance().inject(this);
        // 获得SharedPreferences文件
        preferences = MyApplication.getInstance().preferences;
        // 初始化
        initView();
        // 设置事件
        setEvent();
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
            Intent intent = new Intent(Login.this, Register.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if(vid == R.id.btn_login) {
            String userName = this.userName.getText().toString();
            String password = this.password.getText().toString();
            boolean isCorrect = checkInfo(userName, password);
            if (isCorrect) {
                ARouter.getInstance().build("/view/main").withFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).navigation();
                finish();
            }
        }
    }

    /**
     * 判断输入的密码是否与数据库中的密码一致
     * @param userName 用户名
     * @param password 密码
     * @return true/false
     */
    public boolean checkInfo(String userName, String password) {
        User user = userDAO.findByName(userName);
        if (user != null) {
            if (user.getPassWord().equals(password)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("userName", userName);
                editor.commit();
                ToastUtil.show(this, "登录成功");
                return true;
            }
            else {
                ToastUtil.show(this, "用户名或密码错误");
                return false;
            }
        }
        else {
            ToastUtil.show(this, "该用户名未注册");
            return false;
        }
    }
}