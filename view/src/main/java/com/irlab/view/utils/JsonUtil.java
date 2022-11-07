package com.irlab.view.utils;

import static com.irlab.base.MyApplication.JSON;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.RequestBody;

public class JsonUtil {
    public static final String Logger = JsonUtil.class.getName();

    public static RequestBody getGame(String userName, String playInfo, String result, String code) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
            jsonParam.put("playInfo", playInfo);
            jsonParam.put("result", result);
            jsonParam.put("code", code);
        } catch (JSONException e) {
            Log.e(Logger, "Game的json格式转化错误：" + e.getMessage());
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }

    public static RequestBody getJsonFormOfInitEngine(String userName) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("weight", "6D");
        } catch (JSONException e) {
            Log.e(Logger, "初始化引擎Json格式转化错误: " + e.getMessage());
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }

    /**
     * 将引擎指令转化为json格式
     * @param userName 用户名
     * @param cmd 指令参数
     * @return RequestBody
     */
    public static RequestBody getCmd(String userName, String cmd) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("cmd", cmd);
        } catch (JSONException e) {
            Log.e(Logger, "cmd指令的Json格式转换错误: cmd: " + cmd + e.getMessage());
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }

    public static RequestBody addUser2Json(String userName, String password) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
            jsonParam.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }

    public static RequestBody userName2Json(String userName) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }

    public static RequestBody id2Json(int id) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonParam.toString());
        return requestBody;
    }
}
