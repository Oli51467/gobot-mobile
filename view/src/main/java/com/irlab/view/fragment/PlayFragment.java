package com.irlab.view.fragment;

import static android.app.Activity.RESULT_OK;

import static com.irlab.base.utils.SPUtils.getInt;
import static com.irlab.base.utils.SPUtils.saveString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
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
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.SPUtils;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothAppActivity;
import com.irlab.view.activity.UserInfoActivity;
import com.irlab.view.adapter.FunctionAdapter;
import com.irlab.view.bean.UserResponse;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.bean.MyFunction;
import com.irlab.view.network.api.ApiService;
import com.irlab.view.utils.ImageUtils;
import com.irlab.view.utils.JsonUtil;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import okhttp3.RequestBody;

@SuppressLint("checkResult")
public class PlayFragment extends Fragment implements View.OnClickListener{

    private final MyFunction[] functions = {
            new MyFunction("????????????", R.drawable.play),
            new MyFunction("????????????", R.drawable.icon_set_level),
            new MyFunction("????????????", R.drawable.icon_mygame),
            new MyFunction("????????????", R.drawable.ic_bluetooth),
            new MyFunction("????????????", R.drawable.icon_introduction)
    };
    private final List<MyFunction> funcList = new ArrayList<>();

    public static final String Logger = PlayFragment.class.getName();

    // ??????
    private View view;
    ShapeableImageView profile;
    private String userName;
    protected BluetoothService bluetoothService;    // ????????????
    private BottomSheetDialog bottomSheetDialog;    // ????????????
    private ActivityResultLauncher<Intent> openAlbumLauncher;


    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_play, container, false);
        setView(view);
        RequestBody requestBody = JsonUtil.userName2Json(userName);
        Message msg = new Message();
        msg.obj = this.getActivity();
        String base64_str = SPUtils.getString("avatar");
        if (base64_str != null) {
            setAvatar(base64_str);
            return view;
        }
        NetworkApi.createService(ApiService.class)
                .loadAvatar(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        int code = userResponse.getCode();
                        String base64_code = userResponse.getStatus();
                        saveString("avatar", base64_code);
                        if (code == 200) {
                            msg.what = ResponseCode.LOAD_AVATAR_SUCCESSFULLY.getCode();
                            setAvatar(base64_code);
                        } else if (code == 404) {
                            msg.what = ResponseCode.RESOURCE_NOT_FOUND.getCode();
                        } else if (code == 502) {
                            msg.what = ResponseCode.LOAD_AVATAR_FAILED.getCode();
                        }
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        msg.what = ResponseCode.SERVER_FAILED.getCode();
                        handler.sendMessage(msg);
                    }
                }));
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userName = SPUtils.getString("userName");
        bluetoothService = BluetoothAppActivity.bluetoothService;
        initLauncher();
    }

    @Override
    public void onResume() {
        super.onResume();
        userName = SPUtils.getString("userName");
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

    // ?????????????????????????????????
    public void initFunction() {
        Collections.addAll(funcList, functions);
    }

    @Override
    public void onStart() {
        super.onStart();
        bluetoothService = BluetoothAppActivity.bluetoothService;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setAvatar(String base64_code) {
        byte [] input = Base64.getMimeDecoder().decode(base64_code);
        Bitmap bitmap = BitmapFactory.decodeByteArray(input, 0, input.length);
        Glide.with(PlayFragment.this).load(bitmap).into(profile);
    }

    private void setView(View view) {
        this.view = view;
        userName = SPUtils.getString("userName");
        TextView playLevel = view.findViewById(R.id.play_level);
        TextView battleRecord = view.findViewById(R.id.battle_record);
        profile = view.findViewById(R.id.iv_profile);
        profile.setOnClickListener(this);
        StringBuilder pl = new StringBuilder();
        pl.append("???????????????").append(getInt("play_level"));
        playLevel.setText(pl);
        StringBuilder br = new StringBuilder();
        br.append("?????????").append(getInt("win")).append("???  ").append(getInt("lose")).append("???");
        battleRecord.setText(br);
        view.findViewById(R.id.personal_info).setOnClickListener(this);
    }

    private void initLauncher() {
        openAlbumLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // ?????????result????????????
            if (result.getResultCode() == RESULT_OK) {
                assert result.getData() != null;
                String imagePath = ImageUtils.getImageOnKitKatPath(result.getData(), getActivity());
                // ????????????
                displayImage(imagePath);
            }
        });
    }

    @Override
    @SuppressLint("InflateParams")
    public void onClick(View v) {
        //??????editText???????????????
        int vid = v.getId();
        if (vid == R.id.iv_profile) {
            bottomSheetDialog = new BottomSheetDialog(requireActivity());
            //????????????
            View bottomView = getLayoutInflater().inflate(R.layout.dialog_bottom, null);
            bottomSheetDialog.setContentView(bottomView);
            TextView tvOpenAlbum = bottomView.findViewById(R.id.tv_open_album);
            TextView tvCancel = bottomView.findViewById(R.id.tv_cancel);

            // ????????????
            tvOpenAlbum.setOnClickListener(v1 -> {
                openAlbum();
                bottomSheetDialog.cancel();
            });
            // ??????
            tvCancel.setOnClickListener(v1 -> bottomSheetDialog.cancel());
            bottomSheetDialog.show();
        } else if (vid == R.id.personal_info) {
            Intent intent = new Intent(this.getActivity(), UserInfoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    /**
     * ????????????
     */
    private void openAlbum() {
        openAlbumLauncher.launch(ImageUtils.getSelectPhotoIntent());
    }

    /**
     * ??????????????????????????????
     */
    private void displayImage(String imagePath) {
        if (!TextUtils.isEmpty(imagePath)) {
            Glide.with(this).load(imagePath).into(profile);
            try {
                String base64Image = ImageUtils.bitmapToString(imagePath);
                saveString("avatar", base64Image);
                RequestBody requestBody = JsonUtil.image2Json(userName, base64Image);
                updateAvatar(requestBody);
            } catch (Exception e) {
                Log.e(Logger, "?????????????????????base64??????");
                Glide.with(this).load(R.mipmap.default_profile).into(profile);
            }
        } else {
            ToastUtil.show(getActivity(), "??????????????????");
        }
    }

    private void updateAvatar(RequestBody requestBody) {
        Message msg = new Message();
        msg.obj = this.getActivity();
        NetworkApi.createService(ApiService.class)
                .updateAvatar(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        int code = userResponse.getCode();
                        if (code == 200) {
                            msg.what = ResponseCode.UPDATE_AVATAR_SUCCESSFULLY.getCode();
                        } else {
                            msg.what = ResponseCode.UPDATE_AVATAR_FAILED.getCode();
                        }
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
            Context context = (Context) msg.obj;
            if (msg.what == ResponseCode.LOAD_AVATAR_SUCCESSFULLY.getCode()) {
                ToastUtil.show(context, ResponseCode.LOAD_AVATAR_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.UPDATE_AVATAR_SUCCESSFULLY.getCode()) {
                ToastUtil.show(context, ResponseCode.UPDATE_AVATAR_SUCCESSFULLY.getMsg());
            } else if (msg.what == ResponseCode.UPDATE_AVATAR_FAILED.getCode()) {
                ToastUtil.show(context, ResponseCode.UPDATE_AVATAR_FAILED.getMsg());
            } else if (msg.what == ResponseCode.SERVER_FAILED.getCode()) {
                ToastUtil.show(context, ResponseCode.SERVER_FAILED.getMsg());
            }
        }
    };

}