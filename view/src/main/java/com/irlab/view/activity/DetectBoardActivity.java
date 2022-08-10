package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;
import static com.irlab.base.MyApplication.initNet;
import static com.irlab.base.MyApplication.squeezencnn;

import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.ImageUtils.convertToMatOfPoint;
import static com.irlab.view.utils.ImageUtils.matToBitmap;
import static com.irlab.view.utils.ImageUtils.savePNG_After;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.models.Board;
import com.irlab.view.models.Player;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.initialBoardDetector.InitialBoardDetector;
import com.irlab.view.utils.ImageUtils;
import com.irlab.view.utils.JsonUtil;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectBoardActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    public static final int BLANK = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;
    public static final int WIDTH = 19;
    public static final int HEIGHT = 19;
    public static final int THREAD_NUM = 19;
    public static final int STONE_NUM = 361;
    public static final int SINGLE_THREAD_TASK = 19;
    public static final String TAG = "Detector";

    public static int previousX, previousY;
    public static boolean init = true, showUI = false;
    public static ThreadPoolExecutor threadPool;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Button btnFixBoardPosition;

    private Mat orthogonalBoard = null;

    private MatOfPoint boardContour;

    private InitialBoardDetector initialBoardDetector;

    public BoardDetector boardDetector;

    private String blackPlayer, whitePlayer, komi, rule, engine, userName;

    private Bitmap[][] bitmapMatrix;

    public int[][] curBoard;
    public int[][] lastBoard;

    public Board board;
    public Board previousBoard;

    // opencv与app交互的回调函数
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detect_board);
        Objects.requireNonNull(getSupportActionBar()).hide();
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        threadPool = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM + 1, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(STONE_NUM));
        initEngine(getApplicationContext());
        clearBoard();
        initBoard();
        initViews();
        initDetector();
        getInfoFromActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        clearBoard();
    }

    // 模拟按下按钮提示机器已经落子的情况
    // TODO: 将点击事件改为收到落子信号
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            detectBoard();
        }
        else if (vid == R.id.btn_saveSGF) {
            SmileDialog dialog = new SmileDialogBuilder(this, SmileDialogType.WARNING)
                    .hideTitle(true)
                    .setContentText(getString(R.string.dialog_finish_recording))
                    .setConformBgResColor(com.irlab.base.R.color.warning)
                    .setConformTextColor(Color.WHITE)
                    .setCancelTextColor(Color.BLACK)
                    .setCancelButton("取消")
                    .setCancelBgResColor(R.color.whiteSmoke)
                    .setWindowAnimations(R.style.dialog_style)
                    .setConformButton("确定", () -> {
                        saveGameAsSgf(getApplicationContext());
                        Intent intent = new Intent(this, SelectConfigActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    })
                    .build();
            dialog.show();
        }
        else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    // 这里获取到图像输出
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat inputImage = inputFrame.rgba();

        // 将输出图像的副本传入到棋盘检测
        initialBoardDetector.setImage(inputImage.clone());
        initialBoardDetector.setPreviewImage(inputImage);

        if (initialBoardDetector.process()) {
            // 拿到轮廓检测后的棋盘 Mat && MatOfPoint
            Mat boardPositionInImage = initialBoardDetector.getPositionOfBoardInImage();
            boardContour = convertToMatOfPoint(boardPositionInImage);
            orthogonalBoard = ImageUtils.transformOrthogonally(inputImage, boardPositionInImage);
            if (boardContour != null) Drawer.drawBoardContour(inputImage, boardContour);
            orthogonalBoard = ImageUtils.matRotateClockWise90(orthogonalBoard);
            if (initNet) runOnUiThread(() -> btnFixBoardPosition.setEnabled(true));
        }
        // 在图像上画出轮廓
        else if (boardContour != null) {
            Drawer.drawBoardContour(inputImage, boardContour);
        }
        // 关闭摄像头 显示友好界面
        if (showUI){
            showUI = false;
            Intent intent = new Intent(this, BattleInfoActivity.class);
            intent.putExtra("blackPlayer", blackPlayer);
            intent.putExtra("whitePlayer", whitePlayer);
            intent.putExtra("komi", komi);
            intent.putExtra("rule", rule);
            intent.putExtra("engine", engine);
            intent.putExtra("board", board);
            intent.putExtra("lastMove", board.getPoint(previousX, previousY));
            startActivity(intent);
        }
        return inputImage;
    }
    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    /**
     * 初始化上一个棋盘为空
     */
    private void initBoard() {
        board = new Board(WIDTH, HEIGHT, 0);
        previousBoard = new Board(WIDTH, HEIGHT, 0);

        lastBoard = new int[WIDTH][HEIGHT];
        curBoard = new int[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i ++ ) {
            Arrays.fill(lastBoard[i], BLANK);
            Arrays.fill(curBoard[i], BLANK);
        }
    }

    private void initViews() {
        // 设置一些CameraView的基本状态信息
        mOpenCvCameraView = findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        btnFixBoardPosition.setEnabled(false);

        Button btnSaveSGF = findViewById(R.id.btn_saveSGF);
        btnSaveSGF.setOnClickListener(this);

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
    }

    private void initDetector() {
        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();
    }

    private void initEngine(Context context) {
        String json = JsonUtil.getJsonFormOfInitEngine(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/init", requestBody, new Callback() {
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
                        msg.what = ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode();
                    }
                    else {
                        msg.what = ResponseCode.ENGINE_CONNECT_FAILED.getCode();
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    private void getInfoFromActivity() {
        Intent i = getIntent();
        blackPlayer = i.getStringExtra("blackPlayer");
        whitePlayer = i.getStringExtra("whitePlayer");
        komi = i.getStringExtra("komi");
        rule = i.getStringExtra("rule");
        engine = i.getStringExtra("engine");
    }

    /**
     * 通过比较现在的棋盘和上一个棋盘获得落子位置
     */
    private Pair<Integer, Integer> getMoveByDiff() {
        Pair<Integer, Integer> move;
        for (int i = 0; i < WIDTH; i ++ ) {
            for (int j = 0; j < HEIGHT; j ++ ) {
                if (lastBoard[i][j] == BLANK && curBoard[i][j] != BLANK) {
                    move = new Pair<>(i, j);
                    return move;
                }
            }
        }
        return null;
    }

    private void updateMetricBoard() {
        for (int i = 0; i < WIDTH; i ++ ) {
            for (int j = 0; j < HEIGHT; j ++ ) {
                Point cross = board.points[i][j];
                if (cross.getGroup() == null) {
                    lastBoard[i][j] = BLANK;
                }
                else {
                    lastBoard[i][j] = cross.getGroup().getOwner().getIdentifier();
                }
            }
        }
    }

    public void detectBoard() {
        int moveX, moveY;
        Bitmap bitmap = matToBitmap(orthogonalBoard);
        bitmapMatrix = splitImage(bitmap, WIDTH);
        savePNG_After(bitmap, "total");
        for (int threadIndex = 0; threadIndex < THREAD_NUM; threadIndex ++ ) {
            int innerT = threadIndex;
            Runnable runnable = () -> {
                for (int mTask = 0; mTask < SINGLE_THREAD_TASK; mTask++) {
                    // 由循环得到cnt, 再由cnt得到位置(i, j) cnt从0开始
                    int cnt = innerT * 19 + mTask;
                    int i = cnt / 19;
                    int j = cnt % 19;
                    String result = squeezencnn.Detect(bitmapMatrix[i][j], true);
                    if (result.equals("black")) curBoard[i][j] = BLACK;
                    else if (result.equals("white")) curBoard[i][j] = WHITE;
                    else curBoard[i][j] = BLANK;
                }
            };
            threadPool.execute(runnable);
        }
        Pair<Integer, Integer> move = getMoveByDiff();
        if (move == null) ToastUtil.show(this, "未落子");
        else {
            moveX = move.first;
            moveY = move.second;
            ToastUtil.show(this, (moveX + 1) + " " + (moveY + 1));
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
                Log.d(TAG, "合法落子");
                if (!init) {
                    previousBoard.play(previousX, previousY, board.getLastPlayer());
                }
                else {
                    init = false;
                }
                // 更新一下矩阵棋盘 更新为落完子后的棋盘局面 因为可能有提子
                updateMetricBoard();
                board.nextPlayer();
                previousX = moveX;
                previousY = moveY;
                showUI = true;
            }
            else {
                Log.w(TAG, "这里不可以落子");
                curBoard[moveX][moveY] = BLANK;
            }
            Log.d(TAG, board + "--------------\n" + "lastBoard:\n");
            Log.d(TAG, previousBoard.toString());
        }
    }

    public void saveGameAsSgf(Context context) {
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

    private void clearBoard() {
        String json = JsonUtil.getJsonFormOfClearBoard(userName);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.SAVE_SGF_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SAVE_SGF_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            }
            else if (msg.what == ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.ENGINE_CONNECT_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
            }
        }
    };
}
