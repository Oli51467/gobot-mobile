package com.irlab.view.activity;

import static com.irlab.base.MyApplication.threadPool;
import static com.irlab.view.activity.DefineBoardPositionActivity.corners;
import static com.irlab.view.activity.DefineBoardPositionActivity.imageCapture;
import static com.irlab.view.activity.DefineBoardPositionActivity.mExecutorService;
import static com.irlab.view.utils.AssetsUtil.getFileFromAssets;
import static com.irlab.view.utils.BoardUtil.genPlayCmd;
import static com.irlab.view.utils.BoardUtil.getPositionByIndex;
import static com.irlab.view.utils.BoardUtil.transformIndex;
import static com.irlab.view.utils.ImageUtils.convertImageProxyToBitmap;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.SPUtils;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.network.api.ApiService;
import com.irlab.view.bean.UserResponse;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.impl.EngineInterface;
import com.irlab.view.models.Board;
import com.irlab.view.models.GameTurn;
import com.irlab.view.models.Player;
import com.irlab.view.models.Point;
import com.irlab.view.utils.InitialBoardDetector;
import com.irlab.view.utils.Drawer;
import com.irlab.view.utils.JsonUtil;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;

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

import okhttp3.RequestBody;

public class DetectBoardActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int BLANK = 0, BLACK = 1, WHITE = 2, WIDTH = 20, HEIGHT = 20;
    public static final int BOARD_WIDTH = 1000, BOARD_HEIGHT = 1000, INFO_WIDTH = 880, INFO_HEIGHT = 350;
    private static final int THREAD_NUM = 19, TASK_NUM = 19;
    public static final String Logger = DetectBoardActivity.class.getName();

    private final Context mContext = this;
    // ??????????????????????????????????????????
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("play")) {
                getImageAndProcess();
            }
        }
    };

    public static String[] STONE_CLASSES = new String[]{"black", "blank", "white"};
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
    private EngineInterface engineInterface;
    // ????????????
    protected BluetoothService bluetoothService;

    Module[] mobileNetModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detect_board);
        Objects.requireNonNull(getSupportActionBar()).hide();   // ???????????????
        initBoard();
        initLoaders();
        initArgs();
        initViews();
        initFilter();
        drawBoard();
        startCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothService = BluetoothAppActivity.bluetoothService;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        bluetoothService = BluetoothAppActivity.bluetoothService;
    }

    public void onDestroy() {
        super.onDestroy();
        engineInterface.clearBoard();
        engineInterface.closeEngine();
        unregisterReceiver(broadcastReceiver);
    }

    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_take_picture) { // ????????????
            getImageAndProcess();
        } else if (vid == R.id.btn_exit) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            startActivity(intent);
        } else if (vid == R.id.btn_undo) {
            if (board.gameRecord.getSize() == 1) return;
            undo();
        } else if (vid == R.id.save_play) {
            getGameAndSave();
            engineInterface.closeEngine();
        }
    }

    public void getImageAndProcess() {
        Log.d(Logger, "begin processing...");
        Mat originBoard = new Mat();
        // ???????????? -> ????????????
        imageCapture.takePicture(mExecutorService, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                super.onCaptureSuccess(imageProxy);
                Bitmap bitmap = convertImageProxyToBitmap(imageProxy);
                Utils.bitmapToMat(bitmap, originBoard);
                identifyChessboardAndGenMove(originBoard);
                Log.d(Logger, "??????????????????");
                imageProxy.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                super.onError(e);
                Log.e(Logger, "????????????????????????" + e.getMessage());
            }
        });
    }

    public void undo() {
        if (!board.undo()) return;
        lastBoard = board.gameRecord.getLastTurn().boardState;
        lastMove = board.getPoint(board.gameRecord.getLastTurn().x, board.gameRecord.getLastTurn().y);
        if (lastMove == null) playPosition = "";
        else playPosition = getPositionByIndex(lastMove.getX(), lastMove.getY());
        Log.d(Logger, "undo???????????????" + board.toString());
        drawBoard();
    }

    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // ??????CameraProvider
                cameraProvider = cameraProviderFuture.get();
                // ??????????????????????????????????????????
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Logger, "start camera failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        // ??????Preview
        Preview preview = new Preview.Builder().build();
        // ??? Preview ????????? PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        // ????????????????????? LensFacing ??????
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        // ????????????
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        // ???????????????????????????????????????????????????
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    /**
     * ?????????????????????
     */
    public void identifyChessboardAndGenMove(Mat originBoard) {
        InitialBoardDetector initialBoardDetector = new InitialBoardDetector(corners);
        Bitmap orthogonalBoard = initialBoardDetector.getPerspectiveTransformImage(originBoard);

        if (orthogonalBoard == null) {
            // ???????????????????????????????????????
            String error = "????????????????????????";
            Log.e(Logger, error);
            return;
        }
        splitImage(orthogonalBoard, WIDTH, bitmapMatrix);
        int moveX, moveY;

        final Pair<Integer, Integer>[] move = new Pair[]{null};
        CountDownLatch cdl = new CountDownLatch(THREAD_NUM);
        for (int threadIndex = 0; threadIndex < THREAD_NUM; threadIndex++) {
            int innerT = threadIndex;
            Runnable task = () -> {
                for (int mTask = 0; mTask < THREAD_NUM; mTask++) {
                    if (move[0] != null) break;
                    // ???????????????cnt, ??????cnt????????????(i, j) cnt???0??????
                    int cnt = innerT * TASK_NUM + mTask;
                    int i = cnt / 19 + 1;
                    int j = cnt % 19 + 1;
                    if (detectStone(i, j, innerT)) {
                        move[0] = new Pair<>(i, j);
                        break;
                    }
                }
                cdl.countDown();
            };
            if (move[0] == null) threadPool.execute(task);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Log.e(Logger, e.getMessage());
        }
        if (move[0] == null) {
            Log.i(Logger, "?????????");
            playPosition = "??????????????????";
            Bitmap bitmap4PlayInfo = Bitmap.createBitmap(INFO_WIDTH, INFO_HEIGHT, Bitmap.Config.ARGB_8888);
            Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, 0, playPosition);
            playView.setImageBitmap(playInfo);
        } else {
            moveX = move[0].first;
            moveY = move[0].second;
            Log.d(Logger, moveX + " " + moveY);
            playPosition = getPositionByIndex(moveX, moveY);
            Player player = board.getPlayer();
            // ????????????
            if (board.play(moveX, moveY, player)) {
                // ???????????????????????????
                Set<Point> capturedStones = board.capturedStones;
                // ????????????
                Log.d(Logger, "???????????????????????????");
                for (Point capturedStone : capturedStones) {
                    Log.i(Logger, capturedStone.getX() + "" + capturedStone.getY());
                }
                Log.d(Logger, "????????????????????? \n" + board.toString());
                board.nextPlayer();
                GameTurn lastTurn = board.gameRecord.getLastTurn();
                lastBoard = lastTurn.boardState;
                lastMove = board.getPoint(lastTurn.x, lastTurn.y);
                drawBoard();
                conn2Engine();
            } else {
                Log.e(Logger, "?????????????????????");
                playPosition = "?????????????????????";
                curBoard[moveX][moveY] = BLANK;
            }
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void conn2Engine() {
        String playCmd = genPlayCmd(lastMove);
        RequestBody requestBody = JsonUtil.getCmd(userName, playCmd);
        // ???????????????????????????????????????????????????
        playPosition = engineInterface.playGenMove(requestBody);
        // ????????????????????????????????????????????????????????????????????????????????????
        if (!playPosition.equals("resign") && !playPosition.equals("pass") && !playPosition.equals("failed") && !playPosition.equals("unplayable")) {
            Pair<Integer, Integer> enginePlay = transformIndex(playPosition);
            Log.d(Logger, "????????????????????????:" + enginePlay.first + " " + enginePlay.second);
            // ???????????????????????? ?????????????????????
            board.play(enginePlay.first, enginePlay.second, board.getPlayer());
            GameTurn lastTurn = board.gameRecord.getLastTurn();
            lastBoard = lastTurn.boardState;
            Log.d(Logger, "lastMove????????????" + lastTurn.x + " " + lastTurn.y);
            lastMove = board.getPoint(lastTurn.x, lastTurn.y);
            board.nextPlayer();
            drawBoard();
            // ?????????????????????
            // TODO:??????????????????????????????
            if (bluetoothService != null) {
                Log.d(Logger, "????????????????????????????????????????????? data: " + "L" + playPosition + "Z");
                boolean result = bluetoothService.sendData("L" + playPosition + "Z", false);
                if (!result) {
                    Log.e(Logger, "send play position failed");
                } else {
                    Log.i(Logger, "send play position success");
                }
            } else {
                Log.d(Logger, "????????????????????????????????????");
            }
        } else {
            Log.e(Logger, playPosition);
        }
    }

    /**
     * ???????????????????????????????????????
     * @param x ?????????
     * @param y ?????????
     * @param threadId ????????????id????????????????????????????????? ??????????????????????????????
     * @return ?????????????????????
     */
    private boolean detectStone(int x, int y, int threadId) {
        if (bitmapMatrix[x][y] == null || lastBoard[x][y] != BLANK) return false;
        // load??????????????????
        Module module = mobileNetModule[threadId];
        Tensor tensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmapMatrix[x][y],
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST);

        // running the model
        Tensor outputTensor = module.forward(IValue.from(tensor)).toTensor();

        // getting tensor content as java array of floats
        float[] scores = outputTensor.getDataAsFloatArray();

        // searching for the index with maximum score
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int k = 0; k < scores.length; k++) {
            if (scores[k] > maxScore) {
                maxScore = scores[k];
                maxScoreIdx = k;
            }
        }
        String resultClass = STONE_CLASSES[maxScoreIdx];
        if (resultClass.equals("black")) {
            curBoard[x][y] = BLACK;
        } else if (resultClass.equals("white")) {
            curBoard[x][y] = WHITE;
        } else {
            curBoard[x][y] = BLANK;
        }
        if (lastBoard[x][y] == BLANK && curBoard[x][y] != BLANK && curBoard[x][y] == board.getPlayer().getIdentifier()) {
            return true;
        }
        return false;
    }

    private void initLoaders() {
        OpenCVLoader.initDebug();
        CountDownLatch cdl = new CountDownLatch(THREAD_NUM);
        for (int i = 0; i < THREAD_NUM; i ++ ) {
            try {
                mobileNetModule[i] = LiteModuleLoader.load(getFileFromAssets(this, "test_model.pt"));
            } catch (IOException e) {
                Log.e(Logger, "load????????????:" + e.getMessage());
            } finally {
                cdl.countDown();
            }
        }
        try {
            cdl.await();
        } catch (Exception e) {
            Log.d(Logger, e.getMessage());
        }
    }

    private void initViews() {
        playerInfoView = findViewById(R.id.iv_player_info);
        boardView = findViewById(R.id.iv_board);
        playView = findViewById(R.id.iv_play_info);
        Button btn_play = findViewById(R.id.btn_take_picture);
        btn_play.setOnClickListener(this);

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
        rule = "????????????";
        engine = intent.getStringExtra("engine");
        userName = SPUtils.getString("userName").replaceAll("\n", "");
        previewView = findViewById(R.id.previewView);
    }

    /**
     * ??????????????????????????????
     */
    private void initBoard() {
        drawer = new Drawer();
        board = new Board(WIDTH, HEIGHT, 0);
        lastBoard = new int[WIDTH + 1][HEIGHT + 1];
        curBoard = new int[WIDTH + 1][HEIGHT + 1];
        bitmapMatrix = new Bitmap[WIDTH + 1][HEIGHT + 1];
        mobileNetModule = new Module[WIDTH + 1];
        for (int i = 1; i <= WIDTH; i++) {
            Arrays.fill(lastBoard[i], BLANK);
            Arrays.fill(curBoard[i], BLANK);
        }
        engineInterface = new EngineInterface(userName, mContext, blackPlayer, whitePlayer);
    }

    private void initFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("play");
        this.registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * ????????????????????????
     */
    private void drawBoard() {
        int identifier = (lastMove == null || lastMove.getGroup() == null) ? 0 : lastMove.getGroup().getOwner().getIdentifier();
        Bitmap showBoard = drawer.drawBoard(boardBitmap, lastBoard, lastMove, 0, 0);
        Bitmap playerInfo = drawer.drawPlayerInfo(bitmap4PlayerInfo, blackPlayer, whitePlayer, rule, komi, engine);
        Bitmap playInfo = drawer.drawPlayInfo(bitmap4PlayInfo, identifier, playPosition);

        boardView.setImageBitmap(showBoard);
        playerInfoView.setImageBitmap(playerInfo);
        playView.setImageBitmap(playInfo);
    }

    /**
     * ??????????????????
     * {
     *     "username":"xxx",
     *     "cmd":"printsgf xxx.sgf"
     * }
     */
    public void getGameAndSave() {
        String code = board.saveGame();
        String result = engineInterface.getScore();
        String playInfo = "??????: " + blackPlayer + " " + "??????: " + whitePlayer;
        saveGame(code, result, playInfo);
        /*String cmd = "printsgf 00001.sgf";
        RequestBody requestBody = JsonUtil.getCmd(userName, cmd);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "/exec", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(Logger, "????????????sgf???????????????" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    int responseCode = jsonObject.getInt("code");
                    if (responseCode == 1000) {
                        Log.d(Logger, "????????????????????????");
                        JSONObject callBackData = jsonObject.getJSONObject("data");
                        String code = callBackData.getString("code");
                        String result = callBackData.getString("result");
                        if (result.equals("")) result = engineInterface.getScore();
                        String playInfo = "??????: " + blackPlayer + " " + "??????: " + whitePlayer;
                        saveGame(code, result, playInfo);
                    }
                } catch (JSONException e) {
                    Log.d(Logger, "????????????Json?????????" + e.getMessage());
                }
            }
        });*/
    }

    @SuppressLint("CheckResult")
    private void saveGame(String code, String result, String playInfo) {
        Message msg = new Message();
        msg.obj = this;
        RequestBody requestBody = JsonUtil.getGame(userName, playInfo, result, code);
        NetworkApi.createService(ApiService.class)
                .saveGame(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        Log.i("save Game", "successfully");
                        msg.what = ResponseCode.SAVE_SGF_SUCCESSFULLY.getCode();
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        msg.what = ResponseCode.SERVER_FAILED.getCode();
                        handler.sendMessage(msg);
                    }
                }));
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(Looper.getMainLooper()) {
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
