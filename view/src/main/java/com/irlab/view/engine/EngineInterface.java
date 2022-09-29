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
import com.irlab.view.models.Board;
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
    public static final String TAG = "Detector";
    public static final String Logger = "djnxyxy";

    public String userName;
    public Context context;

    public EngineInterface(String userName, Context context) {
        this.userName = userName;
        this.context = context;
    }

    public void clearBoard() {
        String json = JsonUtil.getCmd2JsonForm(userName, "clear_board");
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    /**
     * 初始化围棋引擎
     */
    public void initEngine() {
        String json = JsonUtil.getJsonFormOfInitEngine(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/init", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, e.getMessage());
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
                    Log.d(Logger, e.toString());
                }
            }
        });
    }

    public void closeEngine() {
        String json = JsonUtil.getCmd2JsonForm(userName, "quit");
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, e.getMessage());
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
                    Log.d(Logger, e.toString());
                }
            }
        });
    }

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
            Log.d(Logger, e.getMessage());
        }
        return result[0];
    }

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
                            msg.what = ResponseCode.ENGINE_RESIGN.getCode();
                        } else if (playPosition.equals("pass")) {
                            Log.d(Logger, "引擎停一手");
                            msg.what = ResponseCode.ENGINE_PASS.getCode();
                        } else {
                            msg.what = ResponseCode.ENGINE_PLAY_SUCCESSFULLY.getCode();
                        }
                        result[0] = playPosition;
                    } else {
                        msg.what = ResponseCode.ENGINE_PLAY_FAILED.getCode();
                        Log.d(Logger, "引擎gen move 失败");
                        result[0] = "failed";
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                }
                cdl.countDown();
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.d(Logger, e.getMessage());
        }
        return result[0];
    }

    public void showBoard() {
        String json = JsonUtil.getJsonFormOfShowBoard(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
                    Log.d(Logger, e.toString());
                }
            }
        });
    }

    /**
     * 保存棋谱
     * @param board       棋盘
     * @param blackPlayer 黑方
     * @param whitePlayer 白方
     * @param komi        贴目
     */
    public void saveGameAsSgf(Board board, String blackPlayer, String whitePlayer, String komi) {
        String playInfo = "黑方:   " + blackPlayer + "     白方:   " + whitePlayer;
        String json = JsonUtil.getJsonFormOfGame(userName, playInfo, "白中盘胜", board.generateSgf(blackPlayer, whitePlayer, komi));
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(SERVER + "/api/saveGame", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
                    Log.d(TAG, e.toString());
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
