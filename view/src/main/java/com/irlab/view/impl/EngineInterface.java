package com.irlab.view.impl;

import static com.irlab.base.MyApplication.ENGINE_SERVER;

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

public class EngineInterface {
    public static final String Logger = "engine-Logger";

    public String userName, blackPlayer, whitePlayer;
    public Context context;

    public EngineInterface(String userName, Context context, String blackPlayer, String whitePlayer) {
        this.userName = userName;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.context = context;
    }

    /**
     * 清空棋盘
     * {
     *     "username":"xxx",
     *     "cmd":"clear_board"
     * }
     */
    public void clearBoard() {
        RequestBody requestBody = JsonUtil.getCmd(userName, "clear_board");
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "clearBoard:" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, String.valueOf(jsonObject));
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        Log.d(Logger, "清空棋盘...Done");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "clear_board JsonException" + e.getMessage());
                }
            }
        });
    }

    /**
     * 初始化围棋引擎
     * {
     *     "username" : "xxx"
     *     "weight" : "40b"
     * }
     */
    public void initEngine() {
        RequestBody requestBody = JsonUtil.getJsonFormOfInitEngine(userName);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/init", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "初始化引擎出错:" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, String.valueOf(jsonObject));
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        Log.d(Logger, "初始化成功");
                    } else {
                        Log.e(Logger, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "初始化引擎JsonException:" + e.getMessage());
                }
            }
        });
    }

    /**
     * 关闭引擎连接
     * {
     *     "username":"xxx",
     *     "cmd":"quit"
     * }
     */
    public void closeEngine() {
        RequestBody requestBody = JsonUtil.getCmd(userName, "quit");
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "关闭引擎出错：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        Log.d(Logger, "关闭引擎...");
                    } else {
                        Log.d(Logger, "关闭引擎失败");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "关闭引擎Json格式错误：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 将落子发送给引擎，然后引擎产生下一步落子
     * {
     *     "username":"xxx",
     *     "cmd":"genmove B/W"
     * }
     */
    public String playGenMove(RequestBody requestBody) {
        final String[] result = new String[1];
        CountDownLatch cdl = new CountDownLatch(1);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "引擎自动走棋指令发送失败，连接失败！";
                Log.e(Logger, error + "\n" + e.getMessage());
                result[0] = "failed";
                cdl.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, "引擎走棋回调：" + jsonObject);
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        String playPosition;
                        Log.d(Logger, "引擎gen move 成功");
                        JSONObject callBackData = jsonObject.getJSONObject("data");
                        playPosition = callBackData.getString("position");
                        Log.d(Logger, "引擎落子坐标:" + playPosition);
                        if (playPosition.equals("resign")) {
                            Log.d(Logger, "引擎认输");
                            result[0] = "引擎认输";
                        } else if (playPosition.equals("pass")) {
                            Log.d(Logger, "引擎停一手");
                            result[0] = "引擎停一手";
                        } else {
                            result[0] = playPosition;
                        }
                    } else if (code == 4001) {
                        Log.d(Logger, "这里不可以落子");
                        result[0] = "unplayable";
                    } else {
                        result[0] = "failed";
                    }
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                    result[0] = "failed";
                } finally {
                    cdl.countDown();
                }
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.e(Logger, e.getMessage());
        }
        return result[0];
    }

    /**
     * 设置规则
     * @param rule 规则
     * {
     *     "username":"xxx",
     *     "cmd":"kata-set-rules chinese/japanese"
     * }
     */
    public void setRules(String rule) {
        String cmd = "kata-set-rules " + rule;
        RequestBody requestBody = JsonUtil.getCmd(userName, cmd);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "设置规则错误：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    if (code == 200) {
                        Log.d(Logger, "设置规则成功");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "设置规则Json异常：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 点目
     */
    public String getScore() {
        final String[] result = new String[1];
        String cmd = "final_score";
        CountDownLatch cdl = new CountDownLatch(1);
        RequestBody requestBody = JsonUtil.getCmd(userName, cmd);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "数子异常：" + e.getMessage());
                result[0] = "Error";
                cdl.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        JSONObject callBackData = jsonObject.getJSONObject("data");
                        result[0] = callBackData.getString("final_score");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "数子Json异常：" + e.getMessage());
                    result[0] = "Error1";
                } finally {
                    cdl.countDown();
                }
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.e(Logger, e.getMessage());
        }
        return result[0];
    }
}
