package com.irlab.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import com.irlab.view.R;
import com.irlab.view.processing.Drawer;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.initialBoardDetector.InitialBoardDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class DetectBoardActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static final String TAG = "Detector";

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnFixBoardPosition, btnReturn;

    private int boardDimension;
    private Mat boardPositionInImage = null;
    private MatOfPoint boardContour;
    InitialBoardDetector initialBoardDetector;
    BoardDetector boardDetector;

    private String blackPlayer;
    private String whitePlayer;
    private String komi;

    // opencv与app交互的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detect_board);

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

        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();

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
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    // 这里获取到图像输出
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat inputImage = inputFrame.rgba();

        // 将输出图像的副本传入到棋盘检测
        initialBoardDetector.setImage(inputImage.clone());
        initialBoardDetector.setPreviewImage(inputImage);
        if (initialBoardDetector.process()) {
            boardPositionInImage = initialBoardDetector.getPositionOfBoardInImage();
            boardContour = convertToMatOfPoint(boardPositionInImage);
            boardDimension = initialBoardDetector.getBoardDimension();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnFixBoardPosition.setEnabled(true);
                }
            });
        }
        else if (boardContour != null) {
            Drawer.drawBoardContour(inputImage, boardContour);
        }

        return inputImage;
    }

    private MatOfPoint convertToMatOfPoint(Mat boardPositionInImage) {
        Point[] corners = {
                new Point(boardPositionInImage.get(0, 0)[0], boardPositionInImage.get(0, 0)[1]),
                new Point(boardPositionInImage.get(1, 0)[0], boardPositionInImage.get(1, 0)[1]),
                new Point(boardPositionInImage.get(2, 0)[0], boardPositionInImage.get(2, 0)[1]),
                new Point(boardPositionInImage.get(3, 0)[0], boardPositionInImage.get(3, 0)[1])
        };
        boardContour = new MatOfPoint(corners);
        return boardContour;
    }

    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            int[] matrix = new int[8];
            matrix[0] = (int) boardPositionInImage.get(0, 0)[0];
            matrix[1] = (int) boardPositionInImage.get(0, 0)[1];
            matrix[2] = (int) boardPositionInImage.get(1, 0)[0];
            matrix[3] = (int) boardPositionInImage.get(1, 0)[1];
            matrix[4] = (int) boardPositionInImage.get(2, 0)[0];
            matrix[5] = (int) boardPositionInImage.get(2, 0)[1];
            matrix[6] = (int) boardPositionInImage.get(3, 0)[0];
            matrix[7] = (int) boardPositionInImage.get(3, 0)[1];

            Intent i = new Intent(this, RecordGameActivity.class);
            i.putExtra("blackPlayer", blackPlayer);
            i.putExtra("whitePlayer", whitePlayer);
            i.putExtra("komi", komi);
            i.putExtra("boardPositionInImage", matrix);
            i.putExtra("boardDimension", 19);
            startActivity(i);
        }
        else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }
}
