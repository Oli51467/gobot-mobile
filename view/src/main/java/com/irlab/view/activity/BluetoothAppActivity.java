package com.irlab.view.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.bluetooth.BluetoothService;
import com.irlab.view.adapter.LVDevicesAdapter;

import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothAppActivity extends AppCompatActivity implements OnClickListener {

    public static final String TAG = "BluetoothApp";
    private static final int CONNECTED_DEVICE_NAME = 0x01;
    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;
    public static final String MY_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";  //蓝牙通讯的uuid

    public static BluetoothService bluetoothService; //静态变量,供其他Activity调用
    public static boolean connect_status = false;

    private Toast mToast;
    private LVDevicesAdapter lvDevicesAdapter;
    private LVDevicesAdapter pairedDevicesAdapter;
    private TimerTask timerTaskConnectDevice; // 获取当前是否连接，在textView中显示
    private Timer timerConnectDevice;
    private TextView displayConnectDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bluetooth_app);
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        initLayout();
        initBluetooth();
        initOptionDevices();
        initPairedDevices();
        getTextViewConnectDevice();
    }


    /**
     * 动态获取设备
     */
    public void getTextViewConnectDevice() {
        timerConnectDevice = new Timer(true);
        timerTaskConnectDevice = new TimerTask() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                Message message = new Message();
                message.what = CONNECTED_DEVICE_NAME;
                if (bluetoothService.getCurConDevice() == null)
                    message.obj = "当前无设备连接";
                else
                    message.obj = "当前连接设备: " + bluetoothService.getCurConDevice().getName() + " ";
                handler.sendMessage(message);
            }
        };
        timerConnectDevice.schedule(timerTaskConnectDevice, 0, 1000);
    }

    public void stopTextViewConnectDevice() {
        if (timerTaskConnectDevice == null)
            return;
        timerTaskConnectDevice.cancel();
    }

    /**
     * 初始化界面的组件，设置一些监听器
     */
    public void initLayout() {
        displayConnectDevice = findViewById(R.id.connectedDevice);
        findViewById(R.id.RefreshPairedDevice).setOnClickListener(BluetoothAppActivity.this);
        findViewById(R.id.RefreshOptionDevice).setOnClickListener(BluetoothAppActivity.this);
    }


    /**
     * 初始化蓝牙
     */
    public void initBluetooth() {
        pairedDevicesAdapter = new LVDevicesAdapter(BluetoothAppActivity.this);
        // 使用MainView中的BluetoothService，或者初始化之
        if (MainView.bluetoothService == null) {
            lvDevicesAdapter = new LVDevicesAdapter(BluetoothAppActivity.this);
            bluetoothService = new BluetoothService(BluetoothAppActivity.this, lvDevicesAdapter, handler);
        } else {
            bluetoothService = MainView.bluetoothService;
            lvDevicesAdapter = MainView.lvDevicesAdapter;
        }

        MainView.bluetoothService = bluetoothService; // 初始化ShowActivity.bluetoothService
        MainView.lvDevicesAdapter = lvDevicesAdapter;
        bluetoothService.initBluetooth();
        bluetoothService.getBluetoothDevices();
        bluetoothService.RegisterBroadcast();

    }

    /**
     * 初始化可用设备列表，设置点击事件
     */
    public void initOptionDevices() {
        //获取蓝牙列表
        lvDevicesAdapter.clear();
        ListView lvDevices = findViewById(R.id.option_devices);
        // TODO 获取extra device

        lvDevices.setAdapter(lvDevicesAdapter);
        lvDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) lvDevicesAdapter.getItem(i);
            //curBluetoothDevice = bluetoothDevice; 获取当前选中的蓝牙，下一步进行蓝牙连接，其中conOutTime暂时未启用
            //连接前先进行配对
            boolean flag = bluetoothService.boundDevice(bluetoothDevice);
            if (flag) {
                Log.i("boundDevice", "success");
            } else {
                Log.e("boundDevice", "failure!");
            }
        });
        lvDevices.setOnItemLongClickListener((parent, view, position, id) -> {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) lvDevicesAdapter.getItem(position);
            boolean flag = bluetoothService.cancelBoundDevice(bluetoothDevice);
            return flag;
        });
    }

    /**
     * 初始化已配对列表，设置点击事件
     */
    @SuppressLint("MissingPermission")
    public void initPairedDevices() {
        pairedDevicesAdapter.clear();
        Set<BluetoothDevice> bluetoothDevices = bluetoothService.getBluetoothDevices();
        ListView pairedDevices = findViewById(R.id.paired_device);
        Log.d("BluetoothDeviceSet", bluetoothDevices + "");
        if (bluetoothDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                Log.d("Bluetooth", bluetoothDevice.getName() + "  |  " + bluetoothDevice.getAddress());
                pairedDevicesAdapter.addDevice(bluetoothDevice);
            }
        }
        pairedDevices.setAdapter(pairedDevicesAdapter);
        pairedDevices.setOnItemClickListener((adapterView, view, i, l) -> {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) pairedDevicesAdapter.getItem(i);
            if (bluetoothService.getCurConDevice() != null && bluetoothService.getCurConDevice().getAddress().equals(bluetoothDevice.getAddress())) {
                // 取消配对
                bluetoothService.clearConnectedThread();
                getTextViewConnectDevice();
                showTip("断开连接！");
            } else {
                // 开始配对
                bluetoothService.startConnectDevice(bluetoothDevice, MY_BLUETOOTH_UUID);
                Log.e("ConnectSuccess", bluetoothService.getCurConnState() + "");
            }
        });
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
        @SuppressLint("MissingPermission")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECTED_DEVICE_NAME: // 动态获取当前的连接设备
                    displayConnectDevice.setText((String) msg.obj);
                    break;
                case CONNECTED_SUCCESS_STATUE: // 连接成功跳转退出
                    showTip("设备连接成功");
                    getTextViewConnectDevice();
                    connect_status = true;
                    // 设备连接成功就退出
                    finish();
                    MainView.bluetoothService = bluetoothService;
                    break;
                case CONNECTED_FAILURE_STATUE:
                    connect_status = false;
                    showTip("设备连接失败，请检查设备是否开启蓝牙");
                    break;
                default:

            }
        }
    };


    //弹出消息
    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.RefreshPairedDevice) {
            initPairedDevices();
            Log.e("ConnectSuccess", bluetoothService.getCurConnState() + "");
            if (bluetoothService.getCurConnState()) {
                Log.e("ConnectSuccess", bluetoothService.getCurConDevice().getName() + "设备连接成功");
                showTip(bluetoothService.getCurConDevice().getName() + "设备连接成功");
            } else {
                showTip("当前无设备连接");
            }
        } else if (id == R.id.RefreshOptionDevice) {
            initOptionDevices();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (bluetoothService.getCurConDevice() != null) {
            bluetoothService.getConnectedThread().cancel();
            bluetoothService.getConnectThread().cancel();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTextViewConnectDevice();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BluetoothApp is onDestroy: ");

        //注销广播接收
        bluetoothService.deleteBroadcast();
    }
}