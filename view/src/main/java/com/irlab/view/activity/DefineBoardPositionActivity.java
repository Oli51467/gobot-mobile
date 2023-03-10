package com.irlab.view.activity;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

import static com.irlab.base.MyApplication.initEngine;
import static com.irlab.view.utils.InputUtil.initWindow;
import static com.irlab.view.utils.ImageUtils.JPEGImageToByteArray;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
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
import com.irlab.base.utils.SPUtils;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.impl.EngineInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefineBoardPositionActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String Logger = "djnxyxy";
    public static final ImageCapture imageCapture = new ImageCapture.Builder()
            // .setTargetResolution(new Size(1024, 1024))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(Surface.ROTATION_0)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build();
    public static List<Pair<Double, Double>> corners = new ArrayList<>();

    private String blackPlayer, whitePlayer, komi, engine, userName;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    public static ExecutorService mExecutorService; // ???????????????????????????
    private Python py;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_define_board_position);
        userName = SPUtils.getString("userName"); // ?????? username ?????????????????????????????????
        initWindow(this);
        initViews();
        initInfo();
        initPython();
        if (!initEngine) {
            EngineInterface engineInterface = new EngineInterface(userName, this, blackPlayer, whitePlayer);
            engineInterface.initEngine();
            engineInterface.clearBoard();
            initEngine = true;
        }
        startCamera();
    }

    private void initInfo() {
        blackPlayer = userName;
        whitePlayer = "kataGo";
        komi = "??????7.5???";
        engine = SPUtils.getString("level");
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        // ????????????????????????
        Button btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        // ??????????????????
        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
        mExecutorService = Executors.newSingleThreadExecutor(); // ??????????????????????????????
    }

    // ?????????Python??????
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
    }

    @SuppressLint("RestrictedApi")
    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // ??????CameraProvider????????????
        cameraProviderFuture.addListener(() -> {
            try {
                // ??????CameraProvider
                cameraProvider = cameraProviderFuture.get();
                // ??????????????????????????????????????????
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Logger, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        // ??????Preview
        Preview preview = new Preview.Builder().build();
        // ????????????????????? LensFacing ??????
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        /*??????????????????????????? View ???
        PERFORMANCE ??????????????????PreviewView ????????? SurfaceView ???????????????????????????????????????????????????????????? TextureView???
        SurfaceView ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        ???????????????????????????????????????????????????????????? GPU ????????????????????????????????????????????????????????????*/
        //previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        // ????????????
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        // ???????????????????????????????????????????????????
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        // ??? Preview ????????? PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btnFixBoardPosition) {
            Message msg = new Message();
            msg.obj = this;
            imageCapture.takePicture(mExecutorService, new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                            Image image = imageProxy.getImage();    // ImageProxy ??? Bitmap
                            assert image != null;
                            byte[] byteArray = JPEGImageToByteArray(image);   // ????????????Image????????????JPEG ??????YUV
                            // ???????????????python??????
                            PyObject obj = py.getModule("CCTProcess").callAttr("main", new Kwarg("byte_array", byteArray));
                            // ?????????????????? -> ?????????
                            double[][] result = obj.toJava(double[][].class);
                            corners.clear();
                            for (double[] doubles : result) {
                                Pair<Double, Double> pair = new Pair<>(doubles[1], doubles[2]);
                                corners.add(pair);
                            }
                            imageProxy.close();
                            if (corners.size() != 4) {
                                // ???????????????
                                msg.what = ResponseCode.NOT_FIND_MARKER.getCode();
                                handler.sendMessage(msg);
                                return;
                            }
                            // ??????????????????????????? ??????-??????-??????-?????? ?????????????????????
                            // 1. ????????? y ??????
                            corners.sort((obj1, obj2) -> (int) (obj1.second - obj2.second));

                            // 2. ???????????????2 ??? ???2???
                            List<Pair<Double, Double>> startTwo = corners.subList(0, 2);
                            List<Pair<Double, Double>> endTwo = corners.subList(2, 4);

                            // 3. ????????????????????????
                            startTwo.sort((obj1, obj2) -> (int) (obj1.first - obj2.first));

                            // 4. ????????????????????????
                            endTwo.sort((obj1, obj2) -> (int) (obj2.first - obj1.first));

                            // ??????toast??????
                            msg.what = ResponseCode.FIND_MARKER.getCode();
                            handler.sendMessage(msg);

                            Intent intent = new Intent(DefineBoardPositionActivity.this, DetectBoardActivity.class);
                            intent.putExtra("blackPlayer", blackPlayer);
                            intent.putExtra("whitePlayer", whitePlayer);
                            intent.putExtra("komi", komi);
                            intent.putExtra("engine", engine);
                            startActivity(intent);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            Log.e(Logger, exception.getMessage());
                        }
                    }
            );
        } else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.SERVER_FAILED.getMsg());
            } else if (msg.what == ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.ENGINE_CONNECT_FAILED.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.ENGINE_CONNECT_FAILED.getMsg());
            } else if (msg.what == ResponseCode.FIND_MARKER.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.FIND_MARKER.getMsg());
            } else if (msg.what == ResponseCode.NOT_FIND_MARKER.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.NOT_FIND_MARKER.getMsg());
            }
        }
    };
}
