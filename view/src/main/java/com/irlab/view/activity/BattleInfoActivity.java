package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;

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
    public static final String Logger = "djnxyxy";
    public static final int BOARD_WIDTH = 1000;
    public static final int BOARD_HEIGHT = 1000;
    public static final int INFO_WIDTH = 880;
    public static final int INFO_HEIGHT = 350;
    public static Drawer drawer;

    private String blackPlayer, whitePlayer, komi, rule, engine, playPosition;

    private int identifier;

    private String userName;

    private Board board = null;
    private Point lastMove = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_info);
        drawer = new Drawer();
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        getInfoFromActivity();
        initView();
        showBoardByEngine(getApplicationContext());
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        showBoardByEngine(getApplicationContext());
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
        Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, identifier, playPosition);

        ImageView playerInfoView = findViewById(R.id.iv_player_info);
        ImageView boardView = findViewById(R.id.iv_board);
        ImageView playView = findViewById(R.id.iv_play_info);

        boardView.setImageBitmap(showBoard);
        playerInfoView.setImageBitmap(playerInfo);
        playView.setImageBitmap(playInfo);
    }

    /*
    从DetectBoardActivity拿到数据
     */
    private void getInfoFromActivity() {
        Intent intent = getIntent();
        if (intent.getSerializableExtra("board") != null) {
            board = (Board) intent.getSerializableExtra("board");
            lastMove = (Point) intent.getSerializableExtra("lastMove");
            blackPlayer = intent.getStringExtra("blackPlayer");
            whitePlayer = intent.getStringExtra("whitePlayer");
            komi = intent.getStringExtra("komi");
            rule = intent.getStringExtra("rule");
            engine = intent.getStringExtra("engine");
            playPosition = intent.getStringExtra("playPosition");
            identifier = lastMove.getGroup().getOwner().getIdentifier();
        }
    }


    /*
    展示棋盘
     */
    private void showBoardByEngine(Context context) {
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
                        Message msg = new Message();
                        msg.obj = context;
                        msg.what = ResponseCode.SHOW_BOARD_FAILED.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "展示棋盘失败");
                    }
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
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
            if (msg.what == ResponseCode.SHOW_BOARD_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SHOW_BOARD_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.SHOW_BOARD_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SHOW_BOARD_FAILED.getMsg());
            }
        }
    };
}