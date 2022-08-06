package com.irlab.view.activity;

import static com.irlab.view.utils.BoardUtil.getPositionByIndex;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.irlab.view.R;
import com.irlab.view.models.Board;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;

public class BattleInfoActivity extends Activity implements View.OnClickListener {

    public static final int BOARD_WIDTH = 1000;
    public static final int BOARD_HEIGHT = 1000;
    public static final int INFO_WIDTH = 880;
    public static final int INFO_HEIGHT = 350;
    public static Drawer drawer;

    private String blackPlayer, whitePlayer, komi, rule, engine;

    private Board board = null;
    private Point lastMove = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_info);
        drawer = new Drawer();
        getInfoFromActivity();
        initView();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getInfoFromActivity();
        initView();
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

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, DetectBoardActivity.class);
            startActivity(intent);
        }
    }

    // TODO: 将落子位置传到引擎

}