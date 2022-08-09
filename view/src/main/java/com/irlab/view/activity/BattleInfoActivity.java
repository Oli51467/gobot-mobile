package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.BoardUtil.getPositionByIndex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.models.Board;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;
import com.irlab.view.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BattleInfoActivity extends Activity implements View.OnClickListener {

    public static final String TAG = BattleInfoActivity.class.getName();
    public static final int BOARD_WIDTH = 1000;
    public static final int BOARD_HEIGHT = 1000;
    public static final int INFO_WIDTH = 880;
    public static final int INFO_HEIGHT = 350;
    public static Drawer drawer;

    private String blackPlayer, whitePlayer, komi, rule, engine;

    private static String userName;

    private Board board = null;
    private Point lastMove = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_info);
        drawer = new Drawer();
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        getInfoFromActivity();
        sendToEngine(getApplicationContext());
        initView();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getInfoFromActivity();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        genMove(getApplicationContext());
    }

    /**
     * 这里必须要有onNewIntent 方法调用在onResume之前
     * 不调用intent永远无法得到更新从而无法得到正确信息
     * 而且必须调用setIntent()
     * @param intent 最新的intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void initView() {
        Button btn_return = findViewById(R.id.btn_return);
        btn_return.setOnClickListener(this);

        Bitmap boardBitmap = Bitmap.createBitmap(BOARD_WIDTH, BOARD_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap showBoard = drawer.drawBoard(boardBitmap, board, lastMove, 0, 0);

        Bitmap bitmap4PlayerInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap bitmap4PlayInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap playerInfo = drawer.drawPlayerInfo(bitmap4PlayerInfo, blackPlayer, whitePlayer, rule, komi, engine);
        Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, lastMove.getGroup().getOwner().getIdentifier(), getPositionByIndex(lastMove.getX(), lastMove.getY()));

        ImageView playerInfoView = findViewById(R.id.iv_player_info);
        ImageView boardView = findViewById(R.id.iv_board);
        ImageView playView = findViewById(R.id.iv_play_info);

        boardView.setImageBitmap(showBoard);
        playerInfoView.setImageBitmap(playerInfo);
        playView.setImageBitmap(playInfo);
    }

    private void getInfoFromActivity() {
        Intent intent = getIntent();
        board = (Board) intent.getSerializableExtra("board");
        lastMove = (Point) intent.getSerializableExtra("lastMove");
        blackPlayer = intent.getStringExtra("blackPlayer");
        whitePlayer = intent.getStringExtra("whitePlayer");
        komi = intent.getStringExtra("komi");
        rule = intent.getStringExtra("rule");
        engine = intent.getStringExtra("engine");
    }

    // 将落子传到引擎
    public void sendToEngine(Context context) {
        String json = JsonUtil.getJsonFormOfPlayIndex(userName, genPlayCmd(lastMove));
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    Message msg = new Message();
                    msg.obj = context;
                    if (code == 1000) {
                        msg.what = ResponseCode.PLAY_PASS_TO_ENGINE_SUCCESSFULLY.getCode();
                    }
                    else {
                        msg.what = ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getCode();
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    private void genMove(Context context) {
        String json = JsonUtil.getJsonFormOfgenMove(userName, lastMove.getGroup().getOwner().getIdentifier() == Board.BLACK_STONE ? "W" : "B");
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int code = jsonObject.getInt("code");
                    Message msg = new Message();
                    msg.obj = context;
                    if (code == 1000) {
                        msg.what = ResponseCode.ENGINE_PLAY_SUCCESSFULLY.getCode();
                        // TODO: 从返回信息中拿到引擎落子位置及并传给下位机下白棋
                    }
                    else {
                        msg.what = ResponseCode.ENGINE_PLAY_FAILED.getCode();
                        // TODO: 分析不同的错误码 并做出相应的处理
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, DetectBoardActivity.class);
            startActivity(intent);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.PLAY_PASS_TO_ENGINE_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.PLAY_PASS_TO_ENGINE_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getMsg());
            }
            if (msg.what == ResponseCode.ENGINE_PLAY_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_PLAY_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.ENGINE_PLAY_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
            }
        }
    };
}