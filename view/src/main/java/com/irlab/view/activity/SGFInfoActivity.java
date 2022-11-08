package com.irlab.view.activity;

import static com.irlab.view.activity.DetectBoardActivity.BLANK;
import static com.irlab.view.activity.DetectBoardActivity.HEIGHT;
import static com.irlab.view.activity.DetectBoardActivity.WIDTH;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.models.Board;
import com.irlab.view.models.Player;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;
import com.irlab.view.utils.SGFUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SGFInfoActivity extends Activity implements View.OnClickListener {

    private static final int BOARD_WIDTH = 1000, BOARD_HEIGHT = 1000;
    private List<Point> moves;
    private ImageView boardImageView;
    private final Drawer drawer = new Drawer();
    private Bitmap boardBitmap;
    private int[][] lastBoard;
    private Board board;
    private Point lastMove;
    private int curPointer = 0, undoPointer = 0, source;
    private String playInfo, result, createTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sgfactivity);
        getInfo();
        initView();
        initBoard();
        drawBoard();
    }

    private void initView() {
        boardImageView = findViewById(R.id.iv_board);
        boardBitmap = Bitmap.createBitmap(BOARD_WIDTH, BOARD_HEIGHT, Bitmap.Config.ARGB_8888);
        findViewById(R.id.header_back).setOnClickListener(this);
        findViewById(R.id.iv_undo).setOnClickListener(this);
        findViewById(R.id.iv_proceed).setOnClickListener(this);
        findViewById(R.id.iv_fast_proceed).setOnClickListener(this);
        findViewById(R.id.iv_fast_undo).setOnClickListener(this);
        TextView tv_playInfo = findViewById(R.id.tv_player_info);
        TextView tv_date = findViewById(R.id.tv_date);
        TextView tv_result = findViewById(R.id.tv_result);
        tv_playInfo.setText(playInfo);
        tv_date.setText(createTime);
        tv_result.setText(result);
    }

    private void getInfo() {
        moves = new ArrayList<>();
        // bundle接收跳转过来的Activity传递来的数据
        Bundle bundle = getIntent().getExtras();
        String code = bundle.getString("code");
        playInfo = bundle.getString("playInfo");
        result = bundle.getString("result");
        createTime = bundle.getString("createTime");
        source = bundle.getInt("source");
        moves = SGFUtil.parseSGF(code, source);
    }

    private void initBoard() {
        lastBoard = new int[WIDTH + 1][HEIGHT + 1];
        for (int i = 1; i <= WIDTH; i++) {
            Arrays.fill(lastBoard[i], BLANK);
        }
        board = new Board(WIDTH, HEIGHT, 0);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("id",1);
            startActivity(intent);
        } else if (vid == R.id.iv_undo) {
            if (curPointer <= 0) return;
            undo();
            undoPointer --;
        } else if (vid == R.id.iv_proceed) {
            if (undoPointer < curPointer) {
                board.redo();
                lastBoard = board.gameRecord.preceding.peek().boardState;
                lastMove = board.getPoint(board.gameRecord.preceding.peek().x, board.gameRecord.preceding.peek().y);
                undoPointer ++;
                drawBoard();
            }
            else {
                if (curPointer >= moves.size()) return;
                proceed(curPointer);
                curPointer ++;
                undoPointer ++;
            }
        } else if (vid == R.id.iv_fast_proceed) {
            for(int i = 0; i < 5; i ++ ) {
                if (undoPointer < curPointer) {
                    board.redo();
                    lastBoard = board.gameRecord.preceding.peek().boardState;
                    lastMove = board.getPoint(board.gameRecord.preceding.peek().x, board.gameRecord.preceding.peek().y);
                    undoPointer ++;
                    drawBoard();
                }
                else {
                    if (curPointer >= moves.size()) return;
                    proceed(curPointer);
                    curPointer ++;
                    undoPointer ++;
                }
            }
        } else if (vid == R.id.iv_fast_undo) {
            for (int i = 0; i < 5; i ++ ) {
                if (curPointer <= 0) return;
                undo();
                undoPointer --;
            }
        }
    }

    public void undo() {
        board.undo();
        lastBoard = board.gameRecord.getLastTurn().boardState;
        lastMove = board.getPoint(board.gameRecord.getLastTurn().x, board.gameRecord.getLastTurn().y);
        drawBoard();
    }

    private void proceed(int cursor) {
        Player player = board.getPlayer();
        board.play(moves.get(cursor).getX(), moves.get(cursor).getY(), player);
        board.nextPlayer();
        lastBoard = board.gameRecord.getLastTurn().boardState;
        lastMove = board.getPoint(board.gameRecord.getLastTurn().x, board.gameRecord.getLastTurn().y);
        drawBoard();
    }

    private void drawBoard() {
        runOnUiThread(() -> {
            Bitmap board = drawer.drawBoard(boardBitmap, lastBoard, lastMove, 0, 0);
            boardImageView.setImageBitmap(board);
        });
    }
}