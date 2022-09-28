package com.irlab.view.fragment;

import static android.app.Activity.RESULT_OK;
import static com.irlab.base.MyApplication.SERVER;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.irlab.base.MyApplication;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothActivity;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.utils.ImageUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SettingsFragment extends Fragment implements View.OnClickListener {

    public static final String Logger = SettingsFragment.class.getName();

    // 控件
    private View view;
    ShapeableImageView profile;
    TextView showInfo;
    TextView edittext;  // 发送指令内容
    Button sendButton;  // 发送按钮
    TextView commandHistory;    //指令记录

    private String userName;
    private boolean hasPermissions = true;

    private File outputImagePath;   // 存储拍完照后的图片

    private BottomSheetDialog bottomSheetDialog;    // 底部弹窗

    private ActivityResultLauncher<Intent> openAlbumLauncher, openCameraLauncher;

    protected BluetoothService bluetoothService;    // 蓝牙服务

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);
        setView(view);
        HttpUtil.sendOkHttpRequest(SERVER + "/api/getProfile" + "?userName=" + userName, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Logger, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                Log.d(Logger, responseData);
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d(Logger, jsonObject.toString());
                    int code = jsonObject.getInt("code");
                    if (code == 200) {
                        Message msg = new Message();
                        msg.obj = jsonObject.getString("data");
                        msg.what = 1;
                        handler.sendMessage(msg);
                    }
                } catch (JSONException e) {
                    Log.d(Logger, e.toString());
                }
            }
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        bluetoothService = BluetoothActivity.bluetoothService;
        initLauncher();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onStart() {
        super.onStart();
        showInfo.setText(userName);
        bluetoothService = BluetoothActivity.bluetoothService;
    }

    // TODO: 选择照片时切出应用再切回有主页面Fragment显示错误的bug
    @Override
    public void onPause() {
        super.onPause();
    }

    private void initLauncher() {
        openAlbumLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // 跳转的result回调方法
            if (result.getResultCode() == RESULT_OK) {
                assert result.getData() != null;
                String imagePath = ImageUtils.getImageOnKitKatPath(result.getData(), getActivity());
                // 显示图片
                displayImage(imagePath);
            }
        });
        openCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                displayImage(outputImagePath.getAbsolutePath());
            }
        });
    }

    private void setView(View view) {
        this.view = view;
        showInfo = view.findViewById(R.id.tv_show_username);
        profile = view.findViewById(R.id.iv_profile);
        profile.setVisibility(View.VISIBLE);
        edittext = view.findViewById(R.id.editText1);
        sendButton = view.findViewById(R.id.button1);
        commandHistory = view.findViewById(R.id.layout_bluetooth_content);

        commandHistory.setMovementMethod(new ScrollingMovementMethod());
        sendButton.setOnClickListener(this);
        profile.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //获取editText控件的数据
        int vid = v.getId();
        if (vid == R.id.button1) {
            String my_string = edittext.getText().toString();
            //判断有无输入
            if (TextUtils.isEmpty(my_string)) {
                //在手机上输出
                //Toast.LENGTH_SHORT:函数功能为显示时间短
                //Toast.LENGTH_LONG :显示时间长
                commandHistory.append("\n");
                commandHistory.append("未发送数据！");
            } else {
                commandHistory.append("\n" + "send: " + my_string);

                bluetoothService.sendData(my_string, false);

                int line = commandHistory.getLineCount();

                //超出屏幕自动滚动显示(3是当前页面显示的最大行数)
                if (line > 3) {
                    int offset = commandHistory.getLineCount() * commandHistory.getLineHeight();
                    commandHistory.scrollTo(0, offset - commandHistory.getHeight() + commandHistory.getLineHeight());
                }
            }
        } else if (vid == R.id.iv_profile) {
            requestPermissions();
            bottomSheetDialog = new BottomSheetDialog(requireActivity());
            //弹窗视图
            @SuppressLint("InflateParams") View bottomView = getLayoutInflater().inflate(R.layout.dialog_bottom, null);
            bottomSheetDialog.setContentView(bottomView);
            TextView tvTakePictures = bottomView.findViewById(R.id.tv_take_pictures);
            TextView tvOpenAlbum = bottomView.findViewById(R.id.tv_open_album);
            TextView tvCancel = bottomView.findViewById(R.id.tv_cancel);

            // 拍照
            tvTakePictures.setOnClickListener(v1 -> {
                takePhoto();
                bottomSheetDialog.cancel();
            });
            // 打开相册
            tvOpenAlbum.setOnClickListener(v1 -> {
                openAlbum();
                bottomSheetDialog.cancel();
            });
            // 取消
            tvCancel.setOnClickListener(v1 -> bottomSheetDialog.cancel());
            bottomSheetDialog.show();
        }
    }

    @SuppressLint("CheckResult")
    private void requestPermissions() {
        // 权限请求
        RxPermissions rxPermissions = new RxPermissions(requireActivity());
        rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(granted -> hasPermissions = granted);
    }

    /**
     * 拍照
     */
    @SuppressLint("SimpleDateFormat")
    private void takePhoto() {
        if (!hasPermissions) {
            ToastUtil.show(getActivity(), "未获取到权限");
            requestPermissions();
            return;
        }
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String filename = timeStampFormat.format(new Date());
        outputImagePath = new File(requireActivity().getExternalCacheDir(), filename + ".jpg");
        Intent takePhotoIntent = ImageUtils.getTakePhotoIntent(getActivity(), outputImagePath);
        openCameraLauncher.launch(takePhotoIntent);
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        if (!hasPermissions) {
            ToastUtil.show(getActivity(), "未获取到权限");
            requestPermissions();
            return;
        }
        openAlbumLauncher.launch(ImageUtils.getSelectPhotoIntent());
    }

    /**
     * 通过图片路径显示图片
     */
    private void displayImage(String imagePath) {
        if (!TextUtils.isEmpty(imagePath)) {
            //Bitmap orc_bitmap = BitmapFactory.decodeFile(imagePath); 压缩拍照和相册获取的Bitmap, 暂时不用
            Glide.with(this).load(imagePath).into(profile);
            HashMap<String, String> kv = new HashMap<>();
            kv.put("userName", userName);
            kv.put("path", imagePath);
            // 将该配置封装成一个对象插入到数据库
            HttpUtil.request(SERVER + "/api/updateProfile", kv, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(Logger, e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d(Logger, Objects.requireNonNull(response.body()).string());
                }
            });
        } else {
            ToastUtil.show(getActivity(), "图片获取失败");
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Glide.with(SettingsFragment.this).load((String) msg.obj).into(profile);
            }
        }
    };
}