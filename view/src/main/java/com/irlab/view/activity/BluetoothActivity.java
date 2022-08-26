package com.irlab.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.adapter.DeviceAdapter;
import com.irlab.view.bluetooth.BluetoothService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {
    // 权限请求码
    public static final int REQUEST_PERMISSION_CODE = 9527;
    private static final int CONNECTED_SUCCESS_STATUE = 0x01;
    private static final int CONNECTED_FAILURE_STATUE = 0x02;
    public static boolean connect_status = false;

    // 蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    // 列表适配器
    private DeviceAdapter deviceAdapter;

    private ActivityResultLauncher<Intent> openBluetoothLauncher;

    private LinearLayout layConnectingLoading;  // 等待连接

    @SuppressLint("StaticFieldLeak")
    public static BluetoothService bluetoothService;

    private List<BluetoothDevice> mList;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        Objects.requireNonNull(getSupportActionBar()).hide();
        mContext = this;
        initLauncher();
        initView();
        bluetoothService = new BluetoothService(this, deviceAdapter, mHandler, openBluetoothLauncher, layConnectingLoading, this);
        bluetoothService.initBroadcastReceiver();
        bluetoothService.initClick();
        requestPermission();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        bluetoothService.getConnectedThread().cancel();
        bluetoothService.getConnectThread().cancel();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.deleteBroadcastReceiver(); //注销广播接收
    }

    private void initLauncher() {
        mList = new ArrayList<>();
        openBluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                if (bluetoothAdapter.isEnabled()) {
                    ToastUtil.show(this, "蓝牙已打开");
                } else {
                    ToastUtil.show(this, "请打开蓝牙");
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void initView() {
        RecyclerView rvDevice = findViewById(R.id.rv_device);
        TextView startScan = findViewById(R.id.btn_start_scan);
        ImageView iv_back = findViewById(R.id.header_back);
        layConnectingLoading = findViewById(R.id.loading_lay);

        iv_back.setOnClickListener(this);
        startScan.setOnClickListener(this);
        //列表配置
        deviceAdapter = new DeviceAdapter(R.layout.item_device_list, mList);
        deviceAdapter.setAnimationEnable(true);  // 启用动画
        deviceAdapter.setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInRight); // 设置动画方式
        rvDevice.setLayoutManager(new LinearLayoutManager(this));
        rvDevice.setAdapter(deviceAdapter);
    }


    /**
     * 请求权限
     */
    private void requestPermission() {
        List<String> neededPermissions = new ArrayList<>();
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
        } else {
            bluetoothService.initBluetooth();
            bluetoothService.ensureDiscoverable();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothService.initBluetooth();
                bluetoothService.ensureDiscoverable();
            } else {
                ToastUtil.show(this, "您没有开启权限");
            }
        }
    }

    @SuppressLint({"MissingPermission"})
    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_start_scan) {
            bluetoothService.scanBluetooth();
        } else if (vid == R.id.header_back) {
            Intent intent = new Intent(BluetoothActivity.this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CONNECTED_SUCCESS_STATUE) {
                ToastUtil.show(mContext, "设备连接成功");
                connect_status = true;
            } else if (msg.what == CONNECTED_FAILURE_STATUE) {
                connect_status = false;
                ToastUtil.show(mContext, "设备连接失败，请检查设备是否开启蓝牙");
            }
        }
    };
}