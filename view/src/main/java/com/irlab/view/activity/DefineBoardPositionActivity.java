package com.irlab.view.activity;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

import static com.irlab.view.engine.EngineInterface.clearBoard;
import static com.irlab.view.engine.EngineInterface.initEngine;
import static com.irlab.view.utils.ImageUtils.JPEGImageToByteArray;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefineBoardPositionActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String Logger = "djnxyxy";
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private final int REQUEST_CODE_PERMISSIONS = 101;
    public static final ImageCapture imageCapture = new ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(Surface.ROTATION_0)
            .build();

    private String blackPlayer, whitePlayer, komi, rule, engine, userName;

    private PreviewView previewView;

    private ProcessCameraProvider cameraProvider;

    private ExecutorService mExecutorService; // 声明一个线程池对象

    private Python py;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    public static List<Pair<Double, Double>> corners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_define_board_position);
        initWindow();
        initEngine(getApplicationContext(), userName);
        clearBoard(userName);
        initViews();
        getInfoFromActivity();
        initPython();
        if (requestPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initWindow() {
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);   // 透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);   // 透明导航栏
        userName = MyApplication.getInstance().preferences.getString("userName", null); // 获取 username 作为连接引擎的唯一主键
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        // 设置确定位置按钮
        Button btnFixBoardPosition = findViewById(R.id.btnFixBoardPosition);
        btnFixBoardPosition.setOnClickListener(this);
        // 设置返回按钮
        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(this);
        mExecutorService = Executors.newSingleThreadExecutor(); // 创建一个单线程线程池
    }

    // 初始化Python环境
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
    }

    private void getInfoFromActivity() {
        Intent i = getIntent();
        blackPlayer = i.getStringExtra("blackPlayer");
        whitePlayer = i.getStringExtra("whitePlayer");
        komi = i.getStringExtra("komi");
        rule = i.getStringExtra("rule");
        engine = i.getStringExtra("engine");
    }

    private boolean requestPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (requestPermissions()) {
                startCamera();
            } else {
                ToastUtil.show(this, "Permissions not granted by the user");
                finish();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // 检查CameraProvider的可用性
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
        // 将 Preview 连接到 PreviewView
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
                            Image image = imageProxy.getImage();    // ImageProxy 转 Bitmap
                            assert image != null;
                            byte[] byteArray = JPEGImageToByteArray(image);   // 注意这里Image的格式是JPEG 不是YUV
                            // 将图像交给python处理
                            PyObject obj = py.getModule("CCTProcess").callAttr("main", new Kwarg("byte_array", byteArray));
                            // 获得返回数据 -> 四个角
                            double[][] result = obj.toJava(double[][].class);
                            corners.clear();
                            // ======= 打印测试 =======
                            for (double[] doubles : result) {
                                for (int j = 0; j < doubles.length; j++) {
                                    if (j > 0) {
                                        Pair<Double, Double> pair = new Pair<>(doubles[j], doubles[j + 1]);
                                        corners.add(pair);
                                        break;
                                    } else {
                                        Log.d(Logger, doubles[j] + "\n");
                                    }
                                }
                            }
                            Collections.sort(corners, Comparator.comparing(o -> o.first));
                            for (Pair<Double, Double> corner : corners) {
                                Log.d(Logger, corner.first + "---" + corner.second);
                            }
                            // ======= 打印测试结束 =======
                            imageProxy.close();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            Log.e(Logger, exception.getMessage());
                        }
                    }
            );

            // TODO: 如果找到四个角点，则继续进入下一步
            if (1==1) {
                msg.what = ResponseCode.FIND_MARKER.getCode();
                handler.sendMessage(msg);

                Intent intent = new Intent(DefineBoardPositionActivity.this, DetectBoardActivity.class);
                intent.putExtra("blackPlayer", blackPlayer);
                intent.putExtra("whitePlayer", whitePlayer);
                intent.putExtra("komi", komi);
                intent.putExtra("rule", rule);
                intent.putExtra("engine", engine);
                //intent.putExtra("corners", (Parcelable) corners);
                startActivity(intent);
            }
            else {
                // 未找到棋盘
                msg.what = ResponseCode.NOT_FIND_MARKER.getCode();
                handler.sendMessage(msg);
            }
        }
        else if (vid == R.id.btn_return) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
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
            else if (msg.what == ResponseCode.NOT_FIND_MARKER.getCode()) {
                ToastUtil.show((Context) msg.obj, ResponseCode.NOT_FIND_MARKER.getMsg());
            }
        }
    };
}
