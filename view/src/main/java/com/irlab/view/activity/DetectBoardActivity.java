package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.squeezencnn;

import static com.irlab.base.MyApplication.threadPool;
import static com.irlab.view.engine.EngineInterface.clearBoard;
import static com.irlab.view.engine.EngineInterface.saveGameAsSgf;
import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.BoardUtil.transformIndex;
import static com.irlab.view.utils.ImageUtils.matToBitmap;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.models.Board;
import com.irlab.view.models.Player;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.initialBoardDetector.InitialBoardDetector;
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
import java.util.concurrent.CountDownLatch;

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
    public static final int SINGLE_THREAD_TASK = 19;
    public static final int BOARD_WIDTH = 1000;
    public static final int BOARD_HEIGHT = 1000;
    public static final int INFO_WIDTH = 880;
    public static final int INFO_HEIGHT = 350;
    public static final String TAG = "Detector";
    public static final String Logger = "djnxyxy";

    public static int previousX, previousY;
    public static boolean init = true;
    public static Drawer drawer;

    public Context mContext;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Button btnFixBoardPosition;

    private Mat orthogonalBoard = null;

    private MatOfPoint boardContour;

    private InitialBoardDetector initialBoardDetector;

    public BoardDetector boardDetector;

    private String userName, playPosition, blackPlayer, whitePlayer, komi, rule, engine;

    private Bitmap[][] bitmapMatrix;

    public int[][] curBoard;
    public int[][] lastBoard;

    public Board board;
    public Board previousBoard;

    public Point lastMove;

    // 蓝牙服务
    BluetoothService bluetoothService;

    // opencv与app交互的回调函数
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            } else {
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
        mContext = this;
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        getInfoFromActivity();
        initBoard();
        initViews();
        initDetector();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothService = BluetoothActivity.bluetoothService;
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
        bluetoothService = BluetoothActivity.bluetoothService;
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        clearBoard(userName);
    }


    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            // 手动捕获棋盘
            // detectBoard();
            identifyChessboardAndGenMove();

        } else if (vid == R.id.btn_saveSGF) {
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
                        saveGameAsSgf(getApplicationContext(), userName, board, blackPlayer, whitePlayer, komi);
                        Intent intent = new Intent(this, SelectConfigActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    })
                    .build();
            dialog.show();
        } else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        // 开启摄像头 模拟落子信号
        else if (vid == R.id.btn_return_camera) {
            CountDownLatch ctl = new CountDownLatch(1);
            runOnUiThread(() -> {
                setContentView(R.layout.activity_detect_board);
                if (!OpenCVLoader.initDebug()) {
                    Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
                } else {
                    Log.d(TAG, "OpenCV library found inside package. Using it!");
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
                initViews();
                ctl.countDown();
            });
            // TODO: 这种自动化检测方法待商榷
            try {
                ctl.await();
            } catch (InterruptedException e) {
                Log.d(Logger, e.toString());
            }

            while (true) {
                // 是否检测到新的走棋
                if (identifyChessboardAndGenMove()) {
                    break;
                }
            }

        } else if (vid == R.id.btn_exit) {
            clearBoard(userName);
            Intent intent = new Intent(this, SelectConfigActivity.class);
            startActivity(intent);
        }
    }


    /**
     * 识别棋盘并走棋
     *
     * @return 是否落子
     */
    public boolean identifyChessboardAndGenMove() {

        // 处理图像，并做图像透视变换
        orthogonalBoard = initialBoardDetector.getPerspectiveTransformImage();

        if (orthogonalBoard == null) {
            // 如果未获取到棋盘，直接返回
            String error = "未获取到棋盘信息";
            Log.e(TAG, error);
            Toast.makeText(MyApplication.getContext(), error, Toast.LENGTH_SHORT).show();

            return false;
        }

        int moveX, moveY;
        Bitmap bitmap = matToBitmap(orthogonalBoard);
        bitmapMatrix = splitImage(bitmap, WIDTH);
        for (int threadIndex = 0; threadIndex < THREAD_NUM; threadIndex++) {
            int innerT = threadIndex;
            Runnable runnable = () -> {
                for (int mTask = 0; mTask < SINGLE_THREAD_TASK; mTask++) {
                    // 由循环得到cnt, 再由cnt得到位置(i, j) cnt从0开始
                    int cnt = innerT * 19 + mTask;
                    int i = cnt / 19 + 1;
                    int j = cnt % 19 + 1;
                    String result = squeezencnn.Detect(bitmapMatrix[i][j], true);
                    if (result.equals("black")) {
                        curBoard[i][j] = BLACK;
                    } else if (result.equals("white")) {
                        curBoard[i][j] = WHITE;
                    } else {
                        curBoard[i][j] = BLANK;
                    }
                }
            };
            threadPool.execute(runnable);
        }
        Pair<Integer, Integer> move = getMoveByDiff();
        if (move == null) {
            ToastUtil.show(this, "未落子");
            return false;
        } else {
            moveX = move.first;
            moveY = move.second;
            ToastUtil.show(this, moveX + " " + moveY);
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
                Log.d(TAG, "合法落子");
                if (!init) {
                    previousBoard.play(previousX, previousY, board.getLastPlayer());
                } else {
                    init = false;
                }
                // 更新一下矩阵棋盘 更新为落完子后的棋盘局面 因为可能有提子
                updateMetricBoard();
                board.nextPlayer();
                previousX = moveX;
                previousY = moveY;
                // 将落子传到引擎 引擎走棋 更新
                lastMove = board.getPoint(previousX, previousY);

                // TODO 当前这里发送给引擎是通过共享变量 lastMove传递的
                sendToEngine(mContext);
                Log.d(Logger, board + "--------------\n" + "lastBoard:\n");
                Log.d(Logger, previousBoard.toString());
                return true;
            } else {
                Log.e(TAG, "这里不可以落子");
                curBoard[moveX][moveY] = BLANK;
                return false;
            }
        }
    }


    /**
     * 开启摄像头并检测棋盘
     * TODO 完善此方法
     */
    public static void switchCameraAndDetectboard() {

    }


    private void getInfoFromActivity() {
        Intent intent = getIntent();
        blackPlayer = intent.getStringExtra("blackPlayer");
        whitePlayer = intent.getStringExtra("whitePlayer");
        komi = intent.getStringExtra("komi");
        rule = intent.getStringExtra("rule");
        engine = intent.getStringExtra("engine");
    }

    // 这里获取到图像输出
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat inputImage = inputFrame.rgba();

        // 将输出图像的副本传入到棋盘检测
        initialBoardDetector.setImage(inputImage.clone());
        initialBoardDetector.setPreviewImage(inputImage);

        /*
        if (initialBoardDetector.process()) {
            // 拿到轮廓检测后的棋盘 Mat && MatOfPoint
            Mat boardPositionInImage = initialBoardDetector.getPositionOfBoardInImage();
            boardContour = convertToMatOfPoint(boardPositionInImage);
            orthogonalBoard = matRotateClockWise90(transformOrthogonally(inputImage, boardPositionInImage));
            if (boardContour != null) Drawer.drawBoardContour(inputImage, boardContour);
            if (initNet) {
                runOnUiThread(() -> btnFixBoardPosition.setEnabled(true));
            }
        }
        // 在图像上画出轮廓
        else if (boardContour != null) {
            Drawer.drawBoardContour(inputImage, boardContour);
        }
         */
        // 关闭摄像头 显示友好界面
        return inputImage;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    /**
     * 初始化上一个棋盘为空
     */
    private void initBoard() {
        drawer = new Drawer();
        board = new Board(WIDTH, HEIGHT, 0);
        previousBoard = new Board(WIDTH, HEIGHT, 0);

        lastBoard = new int[WIDTH + 1][HEIGHT + 1];
        curBoard = new int[WIDTH + 1][HEIGHT + 1];
        for (int i = 1; i <= WIDTH; i++) {
            Arrays.fill(lastBoard[i], BLANK);
            Arrays.fill(curBoard[i], BLANK);
        }
    }

    private void initViews() {
        // 设置一些CameraView的基本状态信息
        mOpenCvCameraView = findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        btnFixBoardPosition.setEnabled(true);

        Button btnSaveSGF = findViewById(R.id.btn_saveSGF);
        btnSaveSGF.setOnClickListener(this);

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
    }

    private void initDetector() {
        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();
    }

    /**
     * 通过比较现在的棋盘和上一个棋盘获得落子位置
     */
    private Pair<Integer, Integer> getMoveByDiff() {
        Pair<Integer, Integer> move;
        for (int i = 1; i <= WIDTH; i++) {
            for (int j = 1; j <= HEIGHT; j++) {
                if (lastBoard[i][j] == BLANK && curBoard[i][j] != BLANK) {
                    move = new Pair<>(i, j);
                    return move;
                }
            }
        }
        return null;
    }

    /**
     * 更新矩阵棋盘
     */
    private void updateMetricBoard() {
        for (int i = 1; i <= WIDTH; i++) {
            for (int j = 1; j <= HEIGHT; j++) {
                Point cross = board.points[i][j];
                if (cross.getGroup() == null) {
                    lastBoard[i][j] = BLANK;
                } else {
                    lastBoard[i][j] = cross.getGroup().getOwner().getIdentifier();
                }
            }
        }
    }


    /**
     * 检测棋盘
     * 备注：这是之前的方法，目前先不用
     */
    public boolean detectBoard() {
        int moveX, moveY;
        Bitmap bitmap = matToBitmap(orthogonalBoard);
        bitmapMatrix = splitImage(bitmap, WIDTH);
        // savePNG_After(bitmap, "total");
        for (int threadIndex = 0; threadIndex < THREAD_NUM; threadIndex++) {
            int innerT = threadIndex;
            Runnable runnable = () -> {
                for (int mTask = 0; mTask < SINGLE_THREAD_TASK; mTask++) {
                    // 由循环得到cnt, 再由cnt得到位置(i, j) cnt从0开始
                    int cnt = innerT * 19 + mTask;
                    int i = cnt / 19 + 1;
                    int j = cnt % 19 + 1;
                    String result = squeezencnn.Detect(bitmapMatrix[i][j], true);
                    if (result.equals("black")) {
                        curBoard[i][j] = BLACK;
                    } else if (result.equals("white")) {
                        curBoard[i][j] = WHITE;
                    } else {
                        curBoard[i][j] = BLANK;
                    }
                }
            };
            threadPool.execute(runnable);
        }
        Pair<Integer, Integer> move = getMoveByDiff();
        if (move == null) {
            ToastUtil.show(this, "未落子");
            return false;
        } else {
            moveX = move.first;
            moveY = move.second;
            ToastUtil.show(this, moveX + " " + moveY);
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
                Log.d(TAG, "合法落子");
                if (!init) {
                    previousBoard.play(previousX, previousY, board.getLastPlayer());
                } else {
                    init = false;
                }
                // 更新一下矩阵棋盘 更新为落完子后的棋盘局面 因为可能有提子
                updateMetricBoard();
                board.nextPlayer();
                previousX = moveX;
                previousY = moveY;
                // 将落子传到引擎 引擎走棋 更新
                lastMove = board.getPoint(previousX, previousY);
                sendToEngine(mContext);
                Log.d(Logger, board + "--------------\n" + "lastBoard:\n");
                Log.d(Logger, previousBoard.toString());
                return true;
            } else {
                Log.w(TAG, "这里不可以落子");
                curBoard[moveX][moveY] = BLANK;
                return false;
            }
        }
    }

    /**
     * 将落子信息发送给引擎，如果发送成功，则继续由引擎产生下一步
     *
     * @param context
     */
    public void sendToEngine(Context context) {
        String playCmd = genPlayCmd(lastMove);
        String json = JsonUtil.getJsonFormOfPlayIndex(userName, playCmd);
        Log.d(Logger, playCmd);

        // 将指令发送给围棋引擎
        Log.i(TAG, playCmd);
        ToastUtil.show(this, playCmd);

        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "走棋指令发送引擎，连接失败！";
                Log.e(TAG, error);
                ToastUtil.show(MyApplication.getContext(), error);
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
                        int identifier = lastMove.getGroup().getOwner().getIdentifier();
                        if (identifier == Board.BLACK_STONE) {
                            genMove(getApplicationContext(), "W");
                        } else if (identifier == Board.WHITE_STONE) {
                            genMove(getApplicationContext(), "B");
                        }
                    } else if (code == 4001) {
                        msg.what = ResponseCode.CANNOT_PLAY.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "无法落子");
                    } else {
                        msg.what = ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getCode();
                        handler.sendMessage(msg);
                        Log.d(Logger, "传递给引擎失败");
                    }
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }


    /**
     * 引擎产生下一步
     *
     * @param context
     */
    private void genMove(Context context, String whichPlayer) {

        String json = JsonUtil.getJsonFormOfgenMove(userName, whichPlayer);
        Log.d(Logger, json);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "引擎自动走棋指令发送失败，连接失败！";
                Log.e(TAG, error);
                ToastUtil.show(mContext, error);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, "引擎走棋回调：" + jsonObject);
                    int code = jsonObject.getInt("code");
                    Message msg = new Message();
                    msg.obj = context;
                    if (code == 1000) {
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
                            Pair<Integer, Integer> enginePlay = transformIndex(playPosition);
                            Log.d(Logger, "转换后的落子坐标:" + enginePlay.first + " " + enginePlay.second);
                            // 将引擎下的棋走上 并更新棋盘
                            board.play(enginePlay.second, enginePlay.first, board.getPlayer());
                            previousBoard.play(previousX, previousY, board.getLastPlayer());
                            updateMetricBoard();
                            board.nextPlayer();
                            previousX = enginePlay.second;
                            previousY = enginePlay.first;
                            lastMove = board.getPoint(previousX, previousY);

                            // TODO: 将引擎落子位置传给下位机
                            if (bluetoothService != null) {
                                Log.d(Logger, "将引擎落子通过蓝牙发给下位机， data: " + "L" + playPosition + "Z");
                                bluetoothService.sendData("L" + playPosition + "Z", false);
                            } else {
                                Log.d(Logger, "落子位置发给下位机失败！");
                                Message tempMsg = new Message();
                                tempMsg.obj = context;
                                tempMsg.what = ResponseCode.BLUETOOTH_SERVICE_FAILED.getCode();
                                handler.sendMessage(tempMsg);
                            }

                            // 跳转到展示对战信息页面
                            runOnUiThread(() -> {
                                mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);
                                mOpenCvCameraView.disableView();
                                setContentView(R.layout.activity_battle_info);
                                beginDrawing();
                            });
                        }
                    } else {
                        msg.what = ResponseCode.ENGINE_PLAY_FAILED.getCode();
                        Log.d(Logger, "引擎gen move 失败");
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                }
            }
        });
    }

    /**
     * 画棋盘，展示棋局
     */
    private void beginDrawing() {
        int identifier = lastMove.getGroup().getOwner().getIdentifier();
        Button btn_return_play = findViewById(R.id.btn_return_camera);
        btn_return_play.setOnClickListener(this);

        Button btn_return = findViewById(R.id.btn_exit);
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

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.ENGINE_CONNECT_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
            } else if (msg.what == ResponseCode.CANNOT_PLAY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.CANNOT_PLAY.getMsg());
            } else if (msg.what == ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.PLAY_PASS_TO_ENGINE_FAILED.getMsg());
            } else if (msg.what == ResponseCode.ENGINE_RESIGN.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_RESIGN.getMsg());
            } else if (msg.what == ResponseCode.ENGINE_PASS.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_PASS.getMsg());
            } else if (msg.what == ResponseCode.BLUETOOTH_SERVICE_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.BLUETOOTH_SERVICE_FAILED.getMsg());
            }
        }
    };
}
