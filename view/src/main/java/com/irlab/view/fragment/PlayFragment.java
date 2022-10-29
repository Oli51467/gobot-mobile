package com.irlab.view.fragment;

import static android.app.Activity.RESULT_OK;
import static com.irlab.base.MyApplication.SERVER;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.irlab.base.MyApplication;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothAppActivity;
import com.irlab.view.adapter.FunctionAdapter;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.utils.ImageUtils;
import com.irlab.view.models.MyFunction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PlayFragment extends Fragment implements View.OnClickListener {

    private final MyFunction[] functions = {
            new MyFunction("开始对弈", R.drawable.play),
            new MyFunction("规则设置", R.drawable.rules_setting),
            new MyFunction("蓝牙连接", R.drawable.ic_bluetooth),
            new MyFunction("下棋说明", R.drawable.introduction),
            new MyFunction("语音测试", R.drawable.icon_speech)
    };
    private final List<MyFunction> funcList = new ArrayList<>();

    public static final String Logger = PlayFragment.class.getName();

    // 控件
    private View view;
    ShapeableImageView profile;
    TextView showInfo;
    private String userName;
    private File outputImagePath;   // 存储拍完照后的图片
    private BottomSheetDialog bottomSheetDialog;    // 底部弹窗
    private ActivityResultLauncher<Intent> openAlbumLauncher, openCameraLauncher;
    protected BluetoothService bluetoothService;    // 蓝牙服务

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_play, container, false);
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
        bluetoothService = BluetoothAppActivity.bluetoothService;
        initLauncher();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFunction();
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        FunctionAdapter functionAdapter = new FunctionAdapter(funcList);
        recyclerView.setAdapter(functionAdapter);
    }

    // 初始化卡片中的功能模块
    public void initFunction() {
        Collections.addAll(funcList, functions);
    }

    @Override
    public void onStart() {
        super.onStart();
        showInfo.setText(userName);
        bluetoothService = BluetoothAppActivity.bluetoothService;
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
        profile.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //获取editText控件的数据
        int vid = v.getId();
        if (vid == R.id.iv_profile) {
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

    /**
     * 拍照
     */
    @SuppressLint("SimpleDateFormat")
    private void takePhoto() {
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
        openAlbumLauncher.launch(ImageUtils.getSelectPhotoIntent());
    }

    /**
     * 通过图片路径显示图片
     */
    private void displayImage(String imagePath) {
        if (!TextUtils.isEmpty(imagePath)) {
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
                Glide.with(PlayFragment.this).load((String) msg.obj).into(profile);
            }
        }
    };
}