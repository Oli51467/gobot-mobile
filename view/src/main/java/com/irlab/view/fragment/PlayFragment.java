package com.irlab.view.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.irlab.base.MyApplication;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothAppActivity;
import com.irlab.view.adapter.FunctionAdapter;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.bean.MyFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayFragment extends Fragment implements View.OnClickListener {

    private final MyFunction[] functions = {
            new MyFunction("开始对弈", R.drawable.play),
            new MyFunction("选择棋力", R.drawable.rules_setting),
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
    protected BluetoothService bluetoothService;    // 蓝牙服务

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_play, container, false);
        setView(view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        bluetoothService = BluetoothAppActivity.bluetoothService;
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

    private void setView(View view) {
        this.view = view;
        showInfo = view.findViewById(R.id.tv_show_username);
        profile = view.findViewById(R.id.iv_profile);
    }

    @Override
    public void onClick(View v) {

    }
}