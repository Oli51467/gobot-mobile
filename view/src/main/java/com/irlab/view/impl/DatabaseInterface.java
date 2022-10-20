package com.irlab.view.impl;

import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.view.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DatabaseInterface {
    private static final String Logger = "database-logger";

    public Context context;

    public DatabaseInterface(Context context) {
        this.context = context;
    }

    /**
     * 判断输入的密码是否与数据库中的密码一致
     * @param password 密码
     */
    public String checkInfo(String userName, String password) {
        CountDownLatch cdl = new CountDownLatch(1);
        String[] result = new String[2];
        HttpUtil.sendOkHttpRequest(SERVER + "/api/checkUserInfo?userName=" + userName + "&password=" + password, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "check user information onFailure:" + e.getMessage());
                result[0] = "serverFailed";
                cdl.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    result[0] = jsonObject.getString("status");
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                    result[0] = "serverFailed";
                } finally {
                    cdl.countDown();
                }
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.e(Logger, "cdl exception" + e.getMessage());
        }
        return result[0];
    }

    /**
     * 查询是否重名
     */
    public int checkName(String userName, String password) {
        int[] result = new int[1];
        CountDownLatch cdl = new CountDownLatch(1);
        // 查询是否重名
        HttpUtil.sendOkHttpRequest(SERVER + "/api/getUserByName?userName=" + userName, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "check userName onFailure" + e.getMessage());
                result[0] = ResponseCode.SERVER_FAILED.getCode();
                cdl.countDown();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String status = jsonObject.getString("status");
                    // 该用户名没有被注册
                    if (status.equals("nullObject")) {
                        result[0] = addUser(userName, password);
                    }
                    // 用户名已被注册
                    else {
                        result[0] = ResponseCode.USER_ALREADY_REGISTERED.getCode();
                    }
                } catch (JSONException e) {
                    result[0] = ResponseCode.JSON_EXCEPTION.getCode();
                    Log.d(Logger, "check userName JsonException" + e.getMessage());
                } finally {
                    cdl.countDown();
                }
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.e(Logger, "cdl exception " + e.getMessage());
        }
        return result[0];
    }

    /**
     * 在数据库中注册用户
     * @param password 密码
     * @return 注册状态码
     */
    private int addUser(String userName, String password) {
        String json = JsonUtil.getJsonFormOfLogin(userName, password);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        int[] result = new int[1];
        HttpUtil.sendOkHttpResponse(SERVER + "/api/addUser", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "add user onFailure " + e.getMessage());
                result[0] = ResponseCode.SERVER_FAILED.getCode();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String status = jsonObject.getString("status");
                    if (status.equals("success")) {
                        result[0] = ResponseCode.ADD_USER_SUCCESSFULLY.getCode();
                    } else {
                        result[0] = ResponseCode.ADD_USER_SERVER_EXCEPTION.getCode();
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "add user jsonException " + e.getMessage());
                    result[0] = ResponseCode.JSON_EXCEPTION.getCode();
                }
            }
        });
        return result[0];
    }
}
