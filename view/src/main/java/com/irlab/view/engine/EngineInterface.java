package com.irlab.view.engine;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
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
        String json = JsonUtil.getCmd2JsonForm(userName, "clear_board");
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
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
        String json = JsonUtil.getJsonFormOfInitEngine(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
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
                    Message msg = new Message();
                    msg.obj = context;
                    if (code == 1000) {
                        msg.what = ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode();
                        Log.d(Logger, "初始化成功");
                    } else {
                        msg.what = ResponseCode.ENGINE_CONNECT_FAILED.getCode();
                    }
                    // 目前是发送toast通知的形式来展示是否已经连接引擎
                    // TODO: 后期应该改为状态展示的方式，在页面上展示引擎连接状态，比如一个绿灯
                    handler.sendMessage(msg);
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
        String json = JsonUtil.getCmd2JsonForm(userName, "quit");
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
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
     * 人下棋
     * {
     *     "username":"xxx",
     *     "cmd":"play B Q5"
     * }
     */
    public String sendIndexes2Engine(String jsonInfo) {
        final String[] result = new String[1];
        CountDownLatch cdl = new CountDownLatch(1);
        RequestBody requestBody = RequestBody.Companion.create(jsonInfo, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "走棋指令发送引擎，连接失败！";
                Log.e(Logger, error);
                result[0] = "failed";
                cdl.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, "send to engine ..." + jsonObject);
                    int code = jsonObject.getInt("code");
                    Message msg = new Message();
                    msg.obj = context;
                    if (code == 1000) {
                        msg.what = ResponseCode.PLAY_PASS_TO_ENGINE_SUCCESSFULLY.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "发送给引擎成功");
                        result[0] = "success";
                    } else if (code == 4001) {
                        msg.what = ResponseCode.CANNOT_PLAY.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "无法落子");
                        result[0] = "unplayable";
                    } else {
                        msg.what = ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "传递给引擎失败");
                        result[0] = "failed";
                    }
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                    result[0] = "failed";
                }
                cdl.countDown();
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
     * 引擎下棋
     * {
     *     "username":"xxx",
     *     "cmd":"genmove B/W"
     * }
     */
    public String genMove(String jsonInfo) {
        final String[] result = new String[1];
        CountDownLatch cdl = new CountDownLatch(1);
        RequestBody requestBody = RequestBody.Companion.create(jsonInfo, JSON);
        Message msg = new Message();
        msg.obj = context;
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "引擎自动走棋指令发送失败，连接失败！";
                Log.e(Logger, error + "\n" + e.getMessage());
                msg.what = ResponseCode.ENGINE_PLAY_FAILED.getCode();
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
                        Log.d(Logger, "引擎落子坐标:" + callBackData);
                        playPosition = callBackData.getString("position");
                        Log.d(Logger, "回调坐标:" + playPosition);
                        if (playPosition.equals("resign")) {
                            Log.d(Logger, "引擎认输");
                            result[0] = "引擎认输";
                            getGameAndSave();
                            msg.what = ResponseCode.ENGINE_RESIGN.getCode();
                        } else if (playPosition.equals("pass")) {
                            Log.d(Logger, "引擎停一手");
                            result[0] = "引擎停一手";
                            msg.what = ResponseCode.ENGINE_PASS.getCode();
                        } else {
                            msg.what = ResponseCode.ENGINE_PLAY_SUCCESSFULLY.getCode();
                            result[0] = playPosition;
                        }
                    } else {
                        msg.what = ResponseCode.ENGINE_PLAY_FAILED.getCode();
                        Log.d(Logger, "引擎gen move 失败");
                        result[0] = "failed";
                    }
                    handler.sendMessage(msg);
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
     * 展示棋盘
     * {
     *     "username":"xxx",
     *     "cmd":"showboard"
     * }
     */
    public void showBoard() {
        CountDownLatch cdl = new CountDownLatch(1);
        String json = JsonUtil.getJsonFormOfShowBoard(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "展示棋盘错误：" + e.getMessage());
                cdl.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, "展示棋盘" + jsonObject);
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        String engineBoard = jsonObject.getString("data");
                        Log.d(Logger, "展示棋盘成功：" + engineBoard);
                    } else {
                        Log.d(Logger, "展示棋盘失败");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "展示棋盘Json异常：" + e.getMessage());
                } finally {
                    cdl.countDown();
                }}
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.d(Logger, e.getMessage());
        }
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
        String json = JsonUtil.getCmd2JsonForm(userName, cmd);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
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
                    Log.d(Logger, "展示棋盘Json异常：" + e.getMessage());
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
        String json = JsonUtil.getCmd2JsonForm(userName, cmd);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "数子异常：" + e.getMessage());
                result[0] = "Error";
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
                }
            }
        });
        return result[0];
    }

    /**
     * 得到棋谱文件
     * {
     *     "username":"xxx",
     *     "cmd":"printsgf xxx.sgf"
     * }
     */
    public void getGameAndSave() {
        final String[] game = new String[5];
        String cmd = "printsgf 00001.sgf";
        String json = JsonUtil.getCmd2JsonForm(userName, cmd);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "输出棋谱sgf文件错误：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        Log.d(Logger, "获取棋谱文件成功");
                        game[0] = jsonObject.getString("code");
                        game[1] = jsonObject.getString("result");
                        game[2] = "黑方: " + blackPlayer + " " + "白方: " + whitePlayer;
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "展示棋盘Json异常：" + e.getMessage());
                }
            }
        });
        saveGame(game[0], game[1], game[2]);
    }


    /**
     *
     * @param code SGF码
     * @param result 结果
     * @param playInfo 对局双方的信息
     */
    public void saveGame(String code, String result, String playInfo) {
        String json = JsonUtil.getJsonFormOfGame(userName, playInfo, result, code);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(SERVER + "/api/saveGame", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, "save game failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String status = jsonObject.getString("status");
                    Message msg = new Message();
                    msg.obj = context;
                    if (status.equals("success")) {
                        msg.what = ResponseCode.SAVE_SGF_SUCCESSFULLY.getCode();
                    } else {
                        msg.what = ResponseCode.SERVER_FAILED.getCode();
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(Logger, "save game json error:" + e.getMessage());
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private static final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.SAVE_SGF_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SAVE_SGF_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            }
        }
    };
}
