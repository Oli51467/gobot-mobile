package com.irlab.view.activity;

import static com.irlab.view.utils.ImageUtils.convertToMatOfPoint;
import static com.irlab.view.utils.ImageUtils.matToBitmap;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import com.irlab.view.R;
import com.irlab.view.SqueezeNcnn;
import com.irlab.view.models.Board;
import com.irlab.view.models.Player;
import com.irlab.view.utils.Drawer;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.initialBoardDetector.InitialBoardDetector;
import com.irlab.view.utils.ImageUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.Arrays;

public class DetectBoardActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static final int BLANK = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;
    public static final int WIDTH = 19;
    public static final int HEIGHT = 19;
    public static final String TAG = "Detector";

    public static int previousX, previousY;
    public static boolean init = true;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnFixBoardPosition, btnReturn;

    private Mat orthogonalBoard = null;
    private Mat boardPositionInImage = null;
    private MatOfPoint boardContour;

    InitialBoardDetector initialBoardDetector;
    BoardDetector boardDetector;

    private String blackPlayer;
    private String whitePlayer;
    private String komi;

    private Bitmap[][] bitmapMatrix;

    public int curBoard[][];
    public int lastBoard[][];

    long timeOfLastBoardDetection;

    private SqueezeNcnn squeezencnn;

    public Board board;
    public Board previousBoard;

    MyTask initNcnn;

    // opencv与app交互的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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
        // 初始化Ncnn
        initNcnn = new MyTask();
        initNcnn.execute(squeezencnn);
        initBoard();
        initViews();
        initDetector();

        Intent i = getIntent();
        blackPlayer = i.getStringExtra("blackPlayer");
        whitePlayer = i.getStringExtra("whitePlayer");
        komi = i.getStringExtra("komi");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
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
        initNcnn.cancel(true);
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    // 模拟按下按钮提示机器已经落子的情况
    // TODO: 将点击事件改为收到落子信号
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            int moveX, moveY;
            Bitmap bitmap = matToBitmap(orthogonalBoard);
            bitmapMatrix = splitImage(bitmap, WIDTH, HEIGHT);
            //savePNG_After(bitmap, "total");

            for (int i = 0; i < WIDTH; i ++ ) {
                for (int j = 0; j < HEIGHT; j ++ ) {
                    String result = squeezencnn.Detect(bitmapMatrix[i][j], true);
                    if (result == null) result = "-1";
                    else if (result.equals("black")) curBoard[i][j] = BLACK;
                    else if (result.equals("white")) curBoard[i][j] = WHITE;
                    else if (result.equals("blank")) curBoard[i][j] = BLANK;
                }
            }

            Pair<Integer, Integer> move = getMoveByDiff();
            if (move == null) Log.d(TAG, "未落子");
            else {
                moveX = move.first;
                moveY = move.second;
                Log.d(TAG, moveX + " " + moveY);
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
                    updateMetrixBoard();
                    board.nextPlayer();
                    previousX = moveX;
                    previousY = moveY;
                    // TODO: 将落子位置传到引擎

                }
                else {
                    Log.d(TAG, "这里不可以落子");
                    curBoard[moveX][moveY] = BLANK;
                }
                Log.d(TAG, board + "--------------\n");
                Log.d(TAG, lastBoard.toString());
            }
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

        timeOfLastBoardDetection = System.currentTimeMillis();
        if (initialBoardDetector.process()) {
            // 拿到轮廓检测后的棋盘 Mat && MatOfPoint
            boardPositionInImage = initialBoardDetector.getPositionOfBoardInImage();
            boardContour = convertToMatOfPoint(boardPositionInImage);
            orthogonalBoard = ImageUtils.transformOrthogonally(inputImage, boardPositionInImage);
            runOnUiThread(() -> btnFixBoardPosition.setEnabled(true));
            timeOfLastBoardDetection = System.currentTimeMillis();
        }
        // 在图像上画出轮廓
        else if (boardContour != null) {
            Drawer.drawBoardContour(inputImage, boardContour);
        }
        return inputImage;
    }

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
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        btnFixBoardPosition.setEnabled(false);

        btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
        btnReturn.setEnabled(true);
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

    private void updateMetrixBoard() {
        for (int i = 0; i < WIDTH; i ++ ) {
            for (int j = 0; j < HEIGHT; j ++ ) {
                com.irlab.view.models.Point cross = board.points[i][j];
                if (cross.getGroup() == null) {
                    curBoard[i][j] = BLANK;
                    lastBoard[i][j] = BLANK;
                }
                else {
                    curBoard[i][j] = cross.getGroup().getOwner().getIdentifier();
                    lastBoard[i][j] = cross.getGroup().getOwner().getIdentifier();
                }
            }
        }
    }

    public class MyTask extends AsyncTask<SqueezeNcnn, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(SqueezeNcnn... squeezeNcnns) {
            squeezencnn = new SqueezeNcnn();
            boolean ret_init = squeezencnn.Init(getAssets());
            if (!ret_init) {
                Log.e(TAG, "squeezencnn Init failed");
            }
            return ret_init;
        }
    }
}
