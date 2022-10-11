package com.irlab.view.activity;

import static com.irlab.base.MyApplication.squeezencnn;

import static com.irlab.base.MyApplication.threadPool;
import static com.irlab.view.activity.DefineBoardPositionActivity.corners;
import static com.irlab.view.activity.DefineBoardPositionActivity.imageCapture;
import static com.irlab.view.activity.DefineBoardPositionActivity.mExecutorService;
import static com.irlab.view.utils.AssetsUtil.getFileFromAssets;
import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.BoardUtil.getMoveByDiff;
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
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.engine.EngineInterface;
import com.irlab.view.models.Board;
import com.irlab.view.models.GameTurn;
import com.irlab.view.models.Player;
import com.irlab.view.models.Point;
import com.irlab.view.utils.Drawer;
import com.irlab.view.process.InitialBoardDetector;
import com.irlab.view.utils.JsonUtil;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class DetectBoardActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int BLANK = 0, BLACK = 1, WHITE = 2, WIDTH = 20, HEIGHT = 20, THREAD_NUM = 19, SINGLE_THREAD_TASK = 19;
    public static final int BOARD_WIDTH = 1000, BOARD_HEIGHT = 1000, INFO_WIDTH = 880, INFO_HEIGHT = 350;
    public static final String Logger = "djnxyxy";
    public static String[] STONE_CLASSES = new String[]{"black", "blank", "white",};

    private final Context mContext = this;

    public static boolean init = true;
    public static Drawer drawer;

    private String userName, playPosition, blackPlayer, whitePlayer, komi, rule, engine;
    private ImageView playView, playerInfoView, boardView;
    private Bitmap[][] bitmapMatrix;
    private Bitmap boardBitmap, bitmap4PlayerInfo, bitmap4PlayInfo;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    public int[][] curBoard;
    public int[][] lastBoard;
    public Board board;
    public Point lastMove;
    public EngineInterface engineInterface;
    // 蓝牙服务
    protected BluetoothService bluetoothService;

    Module module = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_battle_info);
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        initLoaders();
        initArgs();
        initViews();
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
        engineInterface.clearBoard();
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
                    identifyChessboardAndGenMove(originBoard);
                    Log.d(Logger, "图像捕获成功");
                    imageProxy.close();
                }
                @Override
                public void onError(@NonNull ImageCaptureException e) {
                    super.onError(e);
                    Log.e(Logger, "拍照失败，错误：" + e.getMessage());
                }
            });
        } else if (vid == R.id.btn_exit) {
            engineInterface.showBoard();
            engineInterface.clearBoard();
            Intent intent = new Intent(this, SelectConfigActivity.class);
            startActivity(intent);
        } else if (vid == R.id.btn_undo) {
            if (board.gameRecord.getSize() == 1) return;
            undo();
        }
    }

    public void undo() {
        if (!board.undo()) return;
        lastBoard = board.gameRecord.getLastTurn().boardState;
        lastMove = board.getPoint(board.gameRecord.getLastTurn().x, board.gameRecord.getLastTurn().y);
        if (lastMove == null) playPosition = "";
        else playPosition = getPositionByIndex(lastMove.getX(), lastMove.getY());
        Log.d(Logger, "undo后的棋盘：" + board.toString());
        beginDrawing();
    }

    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // 创建CameraProvider
                cameraProvider = cameraProviderFuture.get();
                // 选择相机并绑定生命周期和用例
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Logger, "start camera failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        // 绑定Preview
        Preview preview = new Preview.Builder().build();
        // 将 Preview 连接到 PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        // 指定所需的相机 LensFacing 选项
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        // 缩放类型
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        // 将所选相机和任意用例绑定到生命周期
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    /**
     * 识别棋盘并走棋
     */
    public void identifyChessboardAndGenMove(Mat originBoard) {
        InitialBoardDetector initialBoardDetector = new InitialBoardDetector(corners);
        Bitmap orthogonalBoard = initialBoardDetector.getPerspectiveTransformImage(originBoard);

        if (orthogonalBoard == null) {
            // 如果未获取到棋盘，直接返回
            String error = "未获取到棋盘信息";
            Log.e(Logger, error);
            return;
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
                    /*// 准备输入张量
                    // bitmapToFloat32Tensor creates tensors in the torchvision format using Bitmap as a source.
                    Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                            bitmapMatrix[i][j],
                            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                            TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                            MemoryFormat.CHANNELS_LAST);

                    // running the model
                    Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

                    // getting tensor content as java array of floats
                    final float[] scores = outputTensor.getDataAsFloatArray();

                    // searching for the index with maximum score
                    float maxScore = -Float.MAX_VALUE;
                    int maxScoreIdx = -1;
                    for (int k = 0; k < scores.length; k++) {
                        if (scores[k] > maxScore) {
                            maxScore = scores[k];
                            maxScoreIdx = k;
                        }
                    }
                    if (maxScoreIdx == -1) {
                        for (int u = 0; u < scores.length; u ++ ) {
                            Log.e(Logger, scores[u] + "\n");
                        }
                        break;
                    }
                    String resultClass = STONE_CLASSES[maxScoreIdx];
                    if (resultClass.equals("black")) {
                        curBoard[i][j] = BLACK;
                    } else if (resultClass.equals("white")) {
                        curBoard[i][j] = WHITE;
                    } else if (resultClass.equals("blank")) {
                        curBoard[i][j] = BLANK;
                    }*/
                }
            };
            threadPool.execute(runnable);
        }
        Pair<Integer, Integer> move = getMoveByDiff(lastBoard, curBoard, board);
        if (move == null) {
            Log.i(Logger, "未落子");
            playPosition = "未检测到落子";
            Bitmap bitmap4PlayInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
            Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, 0, playPosition);
            playView.setImageBitmap(playInfo);
        } else {
            moveX = move.first;
            moveY = move.second;
            Log.d(Logger, moveX + " " + moveY);
            playPosition = getPositionByIndex(moveX, moveY);
            Player player = board.getPlayer();
            // 可以落子
            if (board.play(moveX, moveY, player)) {
                // 落子后被吃掉的棋子
                Set<Point> capturedStones = board.capturedStones;
                // 吃子结果
                Log.d(Logger, "被吃掉的棋子位置：");
                for (Point capturedStone : capturedStones) {
                    Log.i(Logger, capturedStone.getX() + "" + capturedStone.getY());
                }
                Log.d(Logger, "落子后的棋盘： \n" + board.toString());
                board.nextPlayer();
                // TODO 当前这里发送给引擎是通过共享变量 lastMove传递的
                GameTurn lastTurn = board.gameRecord.getLastTurn();
                lastBoard = lastTurn.boardState;
                lastMove = board.getPoint(lastTurn.x, lastTurn.y);
                beginDrawing();
                conn2Engine(getApplicationContext());
            } else {
                Log.e(Logger, "这里不可以落子");
                curBoard[moveX][moveY] = BLANK;
            }
        }
    }

    /**
     * 将落子信息发送给引擎，如果发送成功，则继续由引擎产生下一步
     */
    public void conn2Engine(Context context) {
        String playCmd = genPlayCmd(lastMove);
        String jsonInfo = JsonUtil.getJsonFormOfPlayIndex(userName, playCmd);
        Log.d(Logger, "手动走棋指令json格式的数据: " + jsonInfo);
        Log.i(Logger, playCmd);
        // 将指令发送给围棋引擎

        String result = engineInterface.sendIndexes2Engine(jsonInfo);
        if (result.equals("success")) {
            int identifier = lastMove.getGroup().getOwner().getIdentifier();
            String json = "";
            if (identifier == Board.BLACK_STONE) {
                json = JsonUtil.getJsonFormOfgenMove(userName, "W");
            } else if (identifier == Board.WHITE_STONE) {
                json = JsonUtil.getJsonFormOfgenMove(userName, "B");
            }
            Log.d(Logger, "引擎走棋指令json格式的数据: " + json);
            String genMoveResult = engineInterface.genMove(json);
            if (!genMoveResult.equals("failed") && !genMoveResult.equals("")) {
                playPosition = genMoveResult;
                if (!genMoveResult.equals("resign") && !genMoveResult.equals("pass")) {
                    Pair<Integer, Integer> enginePlay = transformIndex(playPosition);
                    Log.d(Logger, "转换后的落子坐标:" + enginePlay.first + " " + enginePlay.second);
                    // 将引擎下的棋走上 并更新棋盘信息
                    board.play(enginePlay.second, enginePlay.first, board.getPlayer());
                    GameTurn lastTurn = board.gameRecord.getLastTurn();
                    lastBoard = lastTurn.boardState;
                    Log.d(Logger, "lastMove上一步：" + lastTurn.x + " " + lastTurn.y);
                    lastMove = board.getPoint(lastTurn.x, lastTurn.y);
                    board.nextPlayer();
                }
                beginDrawing();
                // TODO:将引擎落子位置传给下位机
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
        } else if (result.equals("unplayable")){
            playPosition = "此处无法落子";
            beginDrawing();
        }
    }

    private void initLoaders() {
        OpenCVLoader.initDebug();
        /*// load模型权重文件
        try {
            module = LiteModuleLoader.load(getFileFromAssets(this, "model1010.pt"));
        } catch (IOException e) {
            Log.e(Logger, "load模型失败:" + e.getMessage());
        }*/
    }

    private void initViews() {
        playerInfoView = findViewById(R.id.iv_player_info);
        boardView = findViewById(R.id.iv_board);
        playView = findViewById(R.id.iv_play_info);
        Button btn_return_play = findViewById(R.id.btn_take_picture);
        btn_return_play.setOnClickListener(this);

        Button btn_return = findViewById(R.id.btn_exit);
        btn_return.setOnClickListener(this);

        Button btn_undo = findViewById(R.id.btn_undo);
        btn_undo.setOnClickListener(this);

        boardBitmap = Bitmap.createBitmap(BOARD_WIDTH, BOARD_HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap4PlayerInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap4PlayInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    private void initArgs() {
        Intent intent = getIntent();
        blackPlayer = intent.getStringExtra("blackPlayer");
        whitePlayer = intent.getStringExtra("whitePlayer");
        komi = intent.getStringExtra("komi");
        rule = intent.getStringExtra("rule");
        engine = intent.getStringExtra("engine");
        userName = MyApplication.getInstance().preferences.getString("userName", null).replaceAll("\n", "");
        previewView = findViewById(R.id.previewView);
    }

    /**
     * 初始化上一个棋盘为空
     */
    private void initBoard() {
        drawer = new Drawer();
        board = new Board(WIDTH, HEIGHT, 0);
        lastBoard = new int[WIDTH + 1][HEIGHT + 1];
        curBoard = new int[WIDTH + 1][HEIGHT + 1];
        for (int i = 1; i <= WIDTH; i++) {
            Arrays.fill(lastBoard[i], BLANK);
            Arrays.fill(curBoard[i], BLANK);
        }
        engineInterface = new EngineInterface(userName, mContext);
    }

    /**
     * 画棋盘，展示棋局
     */
    private void beginDrawing() {
        int identifier = (lastMove == null || lastMove.getGroup() == null) ? 0 : lastMove.getGroup().getOwner().getIdentifier();

        Bitmap showBoard = drawer.drawBoard(boardBitmap, lastBoard, lastMove, 0, 0);
        Bitmap playerInfo = drawer.drawPlayerInfo(bitmap4PlayerInfo, blackPlayer, whitePlayer, rule, komi, engine);
        Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, identifier, playPosition);

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
