package com.irlab.view.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {
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
            e.printStackTrace();
        }
        return jsonParam.toString();
    }
}
