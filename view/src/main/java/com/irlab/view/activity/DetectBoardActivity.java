package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.initNet;
import static com.irlab.base.MyApplication.squeezencnn;
import static com.irlab.view.utils.ImageUtils.convertToMatOfPoint;
import static com.irlab.view.utils.ImageUtils.matToBitmap;
import static com.irlab.view.utils.ImageUtils.savePNG_After;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectBoardActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

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

    private String blackPlayer, whitePlayer, komi, rule, engine;

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
        threadPool = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM + 1, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(STONE_NUM));
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
        //initViews();
        //detectBoard();
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
    }

    // 模拟按下按钮提示机器已经落子的情况
    // TODO: 将点击事件改为收到落子信号
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            detectBoard();
        }
        else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
        btnReturn.setEnabled(true);
    }

    private void initDetector() {
        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();
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
        //cdl = new CountDownLatch(THREAD_NUM);   // 计数器
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
        //====== 打印测试
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 19; i ++ ) {
            for (int j = 0; j < 19; j ++ ){
                if (curBoard[i][j] == BLANK) res.append("· ");
                else if (curBoard[i][j] == BLACK) res.append("1 ");
                else res.append("2 ");
            }
            res.append("\n");
        }
        Log.d(TAG, "识别后的棋盘" + "\n" + res + "\n");
        //====== 打印测试结束
        Pair<Integer, Integer> move = getMoveByDiff();
        if (move == null) ToastUtil.show(this, "未落子");
        else {
            moveX = move.first;
            moveY = move.second;
            ToastUtil.show(this, moveX + " " + moveY);
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

    // 初始化引擎
    public void InitEngine(String komi, String userName) {
        String json = JsonUtil.getJsonFormOfInitEngine(userName);
        RequestBody requestBody = FormBody.create(JSON, json);
        HttpUtil.sendOkHttpResponse(ENGINE_SERVER + "", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }
}
