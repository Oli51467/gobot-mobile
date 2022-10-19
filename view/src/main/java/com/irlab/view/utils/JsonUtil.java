package com.irlab.view.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {
    public static final String Logger = "djnxyxy";

    // 将提交到服务器的数据转换为json格式
    public static String getJsonFormOfPlayConfig(String userName, String playerBlack, String playerWhite, String engine, String description, int komi, int rule) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
            jsonParam.put("playerBlack", playerBlack);
            jsonParam.put("playerWhite", playerWhite);
            jsonParam.put("engine", engine);
            jsonParam.put("rule", komi);
            jsonParam.put("komi", rule);
            jsonParam.put("desc", description);
        } catch (JSONException e) {
            Log.d(Logger, "PlayConfig转换json格式出错, 错误:" + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getJsonFormOfGame(String userName, String playInfo, String result, String code) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("userName", userName);
            jsonParam.put("playInfo", playInfo);
            jsonParam.put("result", result);
            jsonParam.put("code", code);
        } catch (JSONException e) {
            Log.e(Logger, "Game的json格式转化错误：" + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getJsonFormOfInitEngine(String userName) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("weight", "6D");
        } catch (JSONException e) {
            Log.e(Logger, "初始化引擎Json格式转化错误: " + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getJsonFormOfPlayIndex(String userName, String playIndex) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("cmd", playIndex);
        } catch (JSONException e) {
            Log.e(Logger, "坐标Json转化错误: " + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getJsonFormOfgenMove(String userName, String which) {
        JSONObject jsonParam = new JSONObject();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("genmove ");
        stringBuilder.append(which);
        try {
            jsonParam.put("username", userName);
            jsonParam.put("cmd", stringBuilder);
        } catch (JSONException e) {
            Log.e(Logger, "genmove的Json格式转化错误: " + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getJsonFormOfShowBoard(String userName) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("cmd", "showboard");
        } catch (JSONException e) {
            Log.e(Logger, "show_board的Json格式转化错误：" + e.getMessage());
        }
        return jsonParam.toString();
    }

    public static String getCmd2JsonForm(String userName, String cmd) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("username", userName);
            jsonParam.put("cmd", cmd);
        } catch (JSONException e) {
            Log.e(Logger, "cmd指令的Json格式转换错误: " + e.getMessage());
        }
        return jsonParam.toString();
    }
}
