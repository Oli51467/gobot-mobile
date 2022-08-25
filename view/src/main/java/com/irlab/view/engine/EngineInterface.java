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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EngineInterface {
    public static final String TAG = "Detector";
    public static final String Logger = "djnxyxy";

    public static void clearBoard(String userName) {
        String json = JsonUtil.getJsonFormOfClearBoard(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, String.valueOf(jsonObject));
                    int code = jsonObject.getInt("code");
                    if (code == 1000) {
                        Log.d(Logger, "关闭检测器，清空棋盘");
                    }
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    /**
     * 展示棋盘
     * @param userName
     */
    public static void showBoardByEngine(String userName) {
        String json = JsonUtil.getJsonFormOfShowBoard(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

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
                    }
                    else {
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
     * @param context
     * @param userName
     * @param board
     * @param blackPlayer
     * @param whitePlayer
     * @param komi
     */
    public static void saveGameAsSgf(Context context, String userName, Board board, String blackPlayer, String whitePlayer, String komi) {
        String playInfo = "黑方:   " + blackPlayer + "     白方:   " + whitePlayer;
        String json = JsonUtil.getJsonFormOfGame(userName, playInfo, "白中盘胜", board.generateSgf(blackPlayer, whitePlayer, komi));
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(SERVER + "/api/saveGame", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

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
                    }
                    else {
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
            }
            else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            }
        }
    };
}
