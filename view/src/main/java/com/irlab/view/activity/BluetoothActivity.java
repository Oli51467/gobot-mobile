package com.irlab.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.adapter.DeviceAdapter;
import com.irlab.view.bluetooth.BluetoothService;

import java.util.Objects;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;
    public static final int CREATE_BOND = 0x05;
    public static final int REMOVE_BOND = 0x06;
    public static final String TAG = "BluetoothApp";
    public static final String MY_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";  //蓝牙通讯的uuid

    public static BluetoothService bluetoothService; //静态变量,供其他Activity调用
    public static boolean connect_status = false;

    private Context mContext;
    public AppCompatActivity appCompatActivity;
    private DeviceAdapter deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        Objects.requireNonNull(getSupportActionBar()).hide();
        mContext = this;
        getPermissionUseful();
        initLayout();
        initBluetooth();
        initDevices();
    }

    // 获取蓝牙权限
    private void getPermissionUseful() {
        String [] permissions = new String[0];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {permission}, 10);
            }
        }
    }

    /**
     * 在线程中更新UI会产生 Only the original thread that created a view hierarchy can touch its views 异常。
     * 原因是只有创建这个View的线程才能去操作这个view，普通会认为是将view创建在非UI线程中才会出现这个错误，因此采用handle，
     * 个人理解为消息队列，线程产生的消息，由消息队列保管，最后依次去更新UI。
     * 为了保证线程安全，Android禁止在非UI线程中更新UI，其中相关View和控件操作都不是线程安全的。
     */
    //用handler更新UI,动态获取
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CONNECTED_SUCCESS_STATUE) {
                ToastUtil.show(mContext,"设备连接成功");
                connect_status = true;
            } else if (msg.what == CONNECTED_FAILURE_STATUE) {
                connect_status = false;
                ToastUtil.show(mContext, "设备断开");
            }
        }
    };

    //初始化界面的组件，设置一些监听器
    public void initLayout() {
        findViewById(R.id.header_back).setOnClickListener(this);
        findViewById(R.id.btn_refresh).setOnClickListener(this);
    }

    //初始化蓝牙
    public void initBluetooth() {
        deviceAdapter = new DeviceAdapter(this);
        bluetoothService = new BluetoothService(this, deviceAdapter, this, handler);
        bluetoothService.initBluetooth();
        bluetoothService.ensureDiscoverable();
        bluetoothService.RegisterBroadcast();
        bluetoothService.disBondAllDevices();
    }

    // 可用设备列表
    @SuppressLint("MissingPermission")
    public void initDevices() {
        //获取蓝牙列表
        deviceAdapter.clear();
        ListView devices = findViewById(R.id.device);
        devices.setAdapter(deviceAdapter);
        devices.setOnItemClickListener((adapterView, view, i, l) -> {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) deviceAdapter.getItem(i);
            ToastUtil.show(this, "开始匹配 " + bluetoothDevice.getName());
            // 配对后连接
            bluetoothService.prepareConnect(bluetoothDevice, true);
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View view) {
        int vid = view.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(BluetoothActivity.this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (vid == R.id.btn_refresh) {
            bluetoothService.scanBluetooth(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothService.addAllPairedDevices();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (bluetoothService.getConnectedThread() != null) {
            bluetoothService.getConnectedThread().cancel();
        }
        if (bluetoothService.getConnectThread() != null) {
            bluetoothService.getConnectThread().cancel();
        }
        bluetoothService.addAllPairedDevices();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BluetoothApp is onDestroy: ");
        bluetoothService.deleteBroadcast(); //注销广播接收
    }
}