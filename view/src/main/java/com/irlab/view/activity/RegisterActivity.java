package com.irlab.view.activity;

import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.irlab.base.utils.ButtonListenerUtil;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.base.watcher.HideTextWatcher;
import com.irlab.base.watcher.ValidationWatcher;
import com.irlab.view.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

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
            String userName = this.userName.getText().toString();
            String password = this.password.getText().toString();
            String passwordConfirm = this.passwordConfirm.getText().toString();
            if (!password.equals(passwordConfirm)) {
                ToastUtil.show(this, "两次输入的密码不一致!");
                return;
            }
            // 查询是否重名
            HttpUtil.sendOkHttpRequest(SERVER + "/api/getUserByName?userName=" + userName, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = Objects.requireNonNull(response.body()).string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        String status = jsonObject.getString("status");
                        // 该用户名没有被注册
                        if (status.equals("nullObject")) {
                            String json = getJson(userName, password);
                            RequestBody requestBody = RequestBody.Companion.create(json, JSON);
                            HttpUtil.sendOkHttpResponse(SERVER + "/api/addUser", requestBody, new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                                }
                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                    String responseData = Objects.requireNonNull(response.body()).string();
                                    try {
                                        JSONObject jsonObject = new JSONObject(responseData);
                                        String status = jsonObject.getString("status");
                                        if (status.equals("success")) {
                                            runOnUiThread(() -> {
                                                ToastUtil.show(RegisterActivity.this, "注册成功");
                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                startActivity(intent);
                                            });
                                        } else {
                                            runOnUiThread(() -> ToastUtil.show(RegisterActivity.this, "服务器异常"));
                                        }
                                    } catch (JSONException e) {
                                        Log.d(TAG, e.toString());
                                    }
                                }
                            });
                        }
                        // 用户名已被注册
                        else {
                            runOnUiThread(() -> ToastUtil.show(RegisterActivity.this, "该用户名已被注册"));
                        }
                    } catch (JSONException e) {
                        Log.d(TAG, e.toString());
                    }
                }
            });
        }
        else if (vid == R.id.iv_return) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    // 将提交到服务器的数据转换为json格式
    private String getJson(String userName, String password) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
            jsonParam.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonParam.toString();
    }
}