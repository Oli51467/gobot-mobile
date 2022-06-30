package com.irlab.base.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.irlab.base.R;
import com.irlab.base.adapter.LVDevicesAdapter;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@Route(path = "/base/bluetooth")
public class BluetoothActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "BluetoothApp";
    private Toast mToast;
    private ListView lvDevices;
    private ListView pairedDevices;
    private LVDevicesAdapter lvDevicesAdapter;
    private LVDevicesAdapter pairedDevicesAdapter;

    public static BluetoothService bluetoothService; //静态变量,供其他Activity调用
    public static Ringtone ringtone;
    public static Vibrator vibrator;
    public static boolean connect_status = false;

    public static final String MY_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";  //蓝牙通讯的uuid
    private TimerTask timerTaskConnectDevice; // 获取当前是否连接，在textView中显示
    private Timer timerConnectDevice;
    private TextView displayConnectDevice;

    private static final int CONNECTED_DEVICE_NAME = 0x01;
    private static final int WAKEUP_STATE = 0x02;
    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        getPermissionUseful();
        initLayout();
        initBluetooth();
        initOptionDevices();
        initPairedDevices();
        getTextViewConnectDevice();
//        getPermissionUseful();
    }

    // 获取蓝牙权限
    private void getPermissionUseful() {
        Log.d(TAG, "getPermissionUseful: 获取蓝牙权限");
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 10);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 10);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 10);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 10);
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
    private Handler handler = new Handler() {
        @SuppressLint("MissingPermission")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECTED_DEVICE_NAME: //动态获取当前的连接设备
                    displayConnectDevice.setText((String) msg.obj);
                    break;
                case CONNECTED_SUCCESS_STATUE: // 连接成功跳转到MainActivity
                    showTip("设备连接成功");
                    connect_status = true;
                    //Intent intent = new Intent(this, MainView.class);
                    //startActivity(intent);
                    break;
                case CONNECTED_FAILURE_STATUE:
                    connect_status = false;
                    showTip("设备连接失败，请检查设备是否开启蓝牙");
                    break;
                default:

            }
        }
    };


    //动态获取设备
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
                    message.obj = "当前连接设备: " + bluetoothService.getCurConDevice().getName();
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

    //初始化界面的组件，设置一些监听器
    public void initLayout() {
        displayConnectDevice = findViewById(R.id.connectedDevice);
        findViewById(R.id.RefreshPairedDevice).setOnClickListener(this);
        findViewById(R.id.RefreshOptionDevice).setOnClickListener(this);
    }


    //初始化蓝牙
    public void initBluetooth() {
        lvDevicesAdapter = new LVDevicesAdapter(this);
        pairedDevicesAdapter = new LVDevicesAdapter(this);
        bluetoothService = new BluetoothService(this, lvDevicesAdapter, handler);
        bluetoothService.initBluetooth();
        bluetoothService.ensureDiscoverable();
        bluetoothService.getBluetoothDevices();
        bluetoothService.RegisterBroadcast();

    }

    // 初始化列表，设置点击事件
    // 可用设备列表
    public void initOptionDevices() {
        //获取蓝牙列表
        lvDevicesAdapter.clear();
        lvDevices = findViewById(R.id.option_devices);
        lvDevices.setAdapter(lvDevicesAdapter);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) lvDevicesAdapter.getItem(i);
                showTip(bluetoothDevice.getName() + bluetoothDevice.getAddress());
                //curBluetoothDevice = bluetoothDevice; 获取当前选中的蓝牙，下一步进行蓝牙连接，其中conOutTime暂时未启用
                //连接前先进行配对
                bluetoothService.boundDevice(bluetoothDevice);
            }
        });
    }

    // 初始化列表，设置点击事件
    // 已配对列表
    @SuppressLint("MissingPermission")
    public void initPairedDevices() {
        pairedDevicesAdapter.clear();
        Set<BluetoothDevice> bluetoothDevices = bluetoothService.getBluetoothDevices();
        pairedDevices = findViewById(R.id.paired_device);
        Log.d("BluetoothDeviceSet", bluetoothDevices + "");
        if (bluetoothDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                Log.d("Bluetooth", bluetoothDevice.getName() + "  |  " + bluetoothDevice.getAddress());
                pairedDevicesAdapter.addDevice(bluetoothDevice);
            }
        }
        pairedDevices.setAdapter(pairedDevicesAdapter);
        pairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) pairedDevicesAdapter.getItem(i);
                showTip(bluetoothDevice.getName() + bluetoothDevice.getAddress());
                //curBluetoothDevice = bluetoothDevice; 获取当前选中的蓝牙，下一步进行蓝牙连接，其中conOutTime暂时未启用
                bluetoothService.startConnectDevice(bluetoothDevice, MY_BLUETOOTH_UUID, 123);

                Log.e("ConnectSuccess", bluetoothService.getCurConnState() + "");
            }
        });


    }

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
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        getTextViewConnectDevice();
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
        stopTextViewConnectDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BluetoothApp is onDestroy: ");
        bluetoothService.deleteBroadcast(); //注销广播接收
    }
}