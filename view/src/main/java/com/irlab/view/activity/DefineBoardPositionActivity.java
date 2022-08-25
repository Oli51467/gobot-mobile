package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINE_SERVER;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.initNet;

import static com.irlab.base.MyApplication.squeezencnn;
import static com.irlab.view.engine.EngineInterface.clearBoard;
import static com.irlab.view.utils.ImageUtils.convertToMatOfPoint;
import static com.irlab.view.utils.ImageUtils.matToBitmap;
import static com.irlab.view.utils.ImageUtils.splitBitmap;
import static com.irlab.view.utils.ImageUtils.splitImage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
import com.irlab.view.utils.Drawer;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.initialBoardDetector.InitialBoardDetector;
import com.irlab.view.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DefineBoardPositionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static final String TAG = "DefineContour";
    public static final String Logger = "djnxyxy";

    public static boolean init = true;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Button btnFixBoardPosition;

    private MatOfPoint boardContour;

    private InitialBoardDetector initialBoardDetector;

    public BoardDetector boardDetector;

    private String blackPlayer, whitePlayer, komi, rule, engine, userName;

    public static ThreadPoolExecutor threadPool;
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
        setContentView(R.layout.activity_define_board_position);
        Objects.requireNonNull(getSupportActionBar()).hide();
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        initEngine(getApplicationContext());
        clearBoard(userName);
        initViews();
        initDetector();
        getInfoFromActivity();

        threadPool = new ThreadPoolExecutor(19, 19 + 2, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(361));
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else {
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

    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            Intent intent = new Intent(DefineBoardPositionActivity.this, DetectBoardActivity.class);
            intent.putExtra("blackPlayer", blackPlayer);
            intent.putExtra("whitePlayer", whitePlayer);
            intent.putExtra("komi", komi);
            intent.putExtra("rule", rule);
            intent.putExtra("engine", engine);
            startActivity(intent);
        }
        else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (vid == R.id.btnTestFinkMarker){

            // TODO find marker测试，图片分割和识别
            Mat boardMat = initialBoardDetector.getPerspectiveTransformImage();
            int[][] curBoard = new int[20][20];

            Bitmap bitmap = matToBitmap(boardMat);
            Bitmap[][] bitmapMatrix = splitBitmap(bitmap, 19);
            for (int threadIndex = 0; threadIndex < 19; threadIndex++) {
                int innerT = threadIndex;
                Runnable runnable = () -> {
                    for (int mTask = 0; mTask < 19; mTask++) {
                        // 由循环得到cnt, 再由cnt得到位置(i, j) cnt从0开始
                        int cnt = innerT * 19 + mTask;
                        int i = cnt / 19 + 1;
                        int j = cnt % 19 + 1;
                        String result = squeezencnn.Detect(bitmapMatrix[i][j], true);
                        if (result.equals("black")) {
                            curBoard[i][j] = 1;
                        } else if (result.equals("white")) {
                            curBoard[i][j] = 2;
                        } else {
                            curBoard[i][j] = 0;
                        }
                    }
                };
                threadPool.execute(runnable);
            }

            Message msg = new Message();
            msg.obj = MyApplication.getContext();
            msg.what = ResponseCode.FIND_MARKER.getCode();;
            handler.sendMessage(msg);
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
            Drawer.drawBoardContour(inputImage, boardContour);
            if (initNet) runOnUiThread(() -> btnFixBoardPosition.setEnabled(true));
        }
        // 在图像上画出轮廓
        else if (boardContour != null) {
            Drawer.drawBoardContour(inputImage, boardContour);
        }
        // 关闭摄像头 显示友好界面
        return inputImage;
    }
    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    private void initViews() {
        // 设置一些CameraView的基本状态信息
        mOpenCvCameraView = findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        // 设置确定位置按钮
        btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        btnFixBoardPosition.setEnabled(false);

        // 测试按钮
        Button testReturn = findViewById(R.id.btnTestFinkMarker);
        testReturn.setOnClickListener(this);

        // 设置返回按钮
        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
    }

    /**
     * 初始化棋盘检测
     */
    private void initDetector() {
        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();
    }

    /**
     * 初始化围棋引擎
     */
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
                    // 目前是发送toast通知的形式来展示是否已经连接引擎
                    // TODO: 后期应该改为状态展示的方式，在页面上展示引擎连接状态，比如一个绿灯
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

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            }
            else if (msg.what == ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getMsg());
            }
            else if (msg.what == ResponseCode.ENGINE_CONNECT_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
            }
            else if (msg.what == ResponseCode.FIND_MARKER.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.FIND_MARKER.getMsg());
            }
        }
    };
}
