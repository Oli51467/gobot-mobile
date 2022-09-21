package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.squeezencnn;

import static com.irlab.base.MyApplication.threadPool;
import static com.irlab.view.activity.DefineBoardPositionActivity.corners;
import static com.irlab.view.activity.DefineBoardPositionActivity.imageCapture;
import static com.irlab.view.activity.DefineBoardPositionActivity.mExecutorService;
import static com.irlab.view.engine.EngineInterface.clearBoard;
import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.BoardUtil.getPositionByIndex;
import static com.irlab.view.utils.BoardUtil.transformIndex;
import static com.irlab.view.utils.ImageUtils.JPEGImageToBitmap;
import static com.irlab.view.utils.ImageUtils.adjustPhotoRotation;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
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
import com.irlab.view.process.InitialBoardDetector;
import com.irlab.view.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectBoardActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int BLANK = 0, BLACK = 1, WHITE = 2, WIDTH = 20, HEIGHT = 20, THREAD_NUM = 19, SINGLE_THREAD_TASK = 19;
    public static final int BOARD_WIDTH = 1000, BOARD_HEIGHT = 1000, INFO_WIDTH = 880, INFO_HEIGHT = 350;
    private static final List<Pair<Integer, Integer>> playSets = new ArrayList<>();
    public static final String Logger = "djnxyxy";
    private final Context mContext = this;

    public static int previousX, previousY;
    public static boolean init = true;
    public static Drawer drawer;

    private String userName, playPosition, blackPlayer, whitePlayer, komi, rule, engine;

    private Bitmap[][] bitmapMatrix;

    public int[][] curBoard;
    public int[][] lastBoard;

    public Board board;
    public Board previousBoard;

    public Point lastMove;

    // 蓝牙服务
    protected BluetoothService bluetoothService;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private PreviewView previewView;

    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_battle_info);
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        OpenCVLoader.initDebug();
        initArgs();
        initBoard();
        beginDrawing();
        startCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothService = BluetoothActivity.bluetoothService;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        bluetoothService = BluetoothActivity.bluetoothService;
    }

    public void onDestroy() {
        super.onDestroy();
        clearBoard(userName);
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_take_picture) { // 直接拍照
            Mat originBoard = new Mat();
            // 捕获棋盘 -> 识别棋子
            imageCapture.takePicture(mExecutorService, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                    super.onCaptureSuccess(imageProxy);
                    Image image = imageProxy.getImage();
                    assert image != null;
                    Bitmap bitmap = adjustPhotoRotation(JPEGImageToBitmap(image), 0);
                    Utils.bitmapToMat(bitmap, originBoard);
                    runOnUiThread(() -> ToastUtil.show(mContext, "请稍后..."));
                    // tempFun(); 演示用
                    identifyChessboardAndGenMove(originBoard);
                    Log.d(Logger, "图像捕获成功");
                    imageProxy.close();
                }
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    super.onError(exception);
                    Log.e(Logger, exception.getMessage());
                }
            });
        } else if (vid == R.id.btn_exit) {
            clearBoard(userName);
            Intent intent = new Intent(this, SelectConfigActivity.class);
            startActivity(intent);
        }
    }

    private void tempFun() {
        for (Pair<Integer, Integer> playSet : playSets) {
            int moveX = playSet.first;
            int moveY = playSet.second;
            playPosition = getPositionByIndex(moveX, moveY);
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
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
                beginDrawing();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // 创建CameraProvider
                cameraProvider = cameraProviderFuture.get();
                // 选择相机并绑定生命周期和用例
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Logger, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        // 绑定Preview
        Preview preview = new Preview.Builder().build();
        // 将 Preview 连接到 PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        // 指定所需的相机 LensFacing 选项
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        /*将预览流渲染到目标 View 上
        PERFORMANCE 是默认模式。PreviewView 会使用 SurfaceView 显示视频串流，但在某些情况下会回退为使用 TextureView。
        SurfaceView 具有专用的绘图界面，该对象更有可能通过内部硬件合成器实现硬件叠加层，尤其是当预览视频上面没有其他界面元素（如按钮）时。
        通过使用硬件叠加层进行渲染，视频帧会避开 GPU 路径，从而能降低平台功耗并缩短延迟时间。*/
        //previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        // 缩放类型
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        // 将所选相机和任意用例绑定到生命周期
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    /**
     * 识别棋盘并走棋
     */
    public boolean identifyChessboardAndGenMove(Mat originBoard) {
        InitialBoardDetector initialBoardDetector = new InitialBoardDetector(corners);
        Bitmap orthogonalBoard = initialBoardDetector.getPerspectiveTransformImage(originBoard);

        if (orthogonalBoard == null) {
            // 如果未获取到棋盘，直接返回
            String error = "未获取到棋盘信息";
            Log.e(Logger, error);
            return false;
        }
        int moveX, moveY;
        bitmapMatrix = splitImage(orthogonalBoard, WIDTH);
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

        //执行shutdown
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
            Log.d(Logger, "start shutdown...");
            //等待执行结束
            try {
                threadPool.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Pair<Integer, Integer> move = getMoveByDiff();
        if (move == null) {
            Log.d(Logger, "未落子");
            return false;
        } else {
            moveX = move.first;
            moveY = move.second;
            Log.d(Logger, moveX + " " + moveY);
            playPosition = getPositionByIndex(moveX, moveY);
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
                Log.d(Logger, "合法落子");
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
                beginDrawing();

                // TODO 当前这里发送给引擎是通过共享变量 lastMove传递的
                //sendToEngine(getApplicationContext());
                Log.d(Logger, board + "--------------\n" + "lastBoard:\n");
                Log.d(Logger, previousBoard.toString());
                return true;
            } else {
                Log.e(Logger, "这里不可以落子");
                curBoard[moveX][moveY] = BLANK;
                return false;
            }
        }
    }

    private void initArgs() {
        Intent intent = getIntent();
        blackPlayer = intent.getStringExtra("blackPlayer");
        whitePlayer = intent.getStringExtra("whitePlayer");
        komi = intent.getStringExtra("komi");
        rule = intent.getStringExtra("rule");
        engine = intent.getStringExtra("engine");
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        previewView = findViewById(R.id.previewView);
        for (int i = 1; i <= 4; i ++ ) {
            for (int j = 7; j >= 5; j -- ) {
                Pair<Integer, Integer> playSet = new Pair<>(i, j);
                playSets.add(playSet);
            }
        }
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
     * 将落子信息发送给引擎，如果发送成功，则继续由引擎产生下一步
     */
    public void sendToEngine(Context context) {

        String playCmd = genPlayCmd(lastMove);
        String json = JsonUtil.getJsonFormOfPlayIndex(userName, playCmd);

        // 将指令发送给围棋引擎
        Log.i(Logger, playCmd);

        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "走棋指令发送引擎，连接失败！";
                Log.e(Logger, error);
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
                    Log.d(Logger, e.toString());
                }
            }
        });
    }


    /**
     * 引擎产生下一步
     */
    private void genMove(Context context, String whichPlayer) {
        String json = JsonUtil.getJsonFormOfgenMove(userName, whichPlayer);
        Log.d(Logger, json);
        RequestBody requestBody = RequestBody.Companion.create(json, JSON);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error = "引擎自动走棋指令发送失败，连接失败！";
                Log.e(Logger, error);
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

                            // 将引擎落子位置传给下位机
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
        int identifier = lastMove == null ? 0 : lastMove.getGroup().getOwner().getIdentifier();
        Button btn_return_play = findViewById(R.id.btn_take_picture);
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
