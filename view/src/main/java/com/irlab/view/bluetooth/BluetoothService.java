package com.irlab.view.bluetooth;

import static com.irlab.view.utils.BluetoothUtil.bytes2HexString;
import static com.irlab.view.utils.BluetoothUtil.hexString2Bytes;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.utils.ToastUtil;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothActivity;
import com.irlab.view.adapter.DeviceAdapter;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class BluetoothService {
    public static final String Logger = BluetoothActivity.class.getName();

    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;
    public static final String MY_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";  //蓝牙通讯的uuid

    public BluetoothAdapter bluetoothAdapter;
    private final Context mContext;
    public DeviceAdapter deviceAdapter;
    private BluetoothReceiver bluetoothReceiver;
    private ConnectedThread connectedThread; // 管理连接的线程
    private ConnectThread connectThread;

    private boolean curConnState = false; // 当前设备连接状态
    private BluetoothDevice curBluetoothDevice;
    private final Handler handler;
    public LinearLayout layLoading;
    public AppCompatActivity appCompatActivity;

    private final ActivityResultLauncher<Intent> openBluetoothLauncher;

    public BluetoothService(Context context, DeviceAdapter deviceAdapter, Handler handler, ActivityResultLauncher<Intent> launcher, LinearLayout layLoading, AppCompatActivity appCompatActivity) {
        this.mContext = context;
        this.deviceAdapter = deviceAdapter;
        this.handler = handler;
        this.openBluetoothLauncher = launcher;
        this.layLoading = layLoading;
        this.appCompatActivity = appCompatActivity;
    }

    @SuppressLint("MissingPermission")
    public void initClick() {
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            // 连接设备 判断当前是否还是正在搜索周边设备，如果是则暂停搜索
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            layLoading.setVisibility(View.VISIBLE);
            BluetoothDevice bluetoothDevice = deviceAdapter.getItem(position);
            // 获取已经绑定的设备
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            // 先将已经绑定的设备解绑
            for (BluetoothDevice bondedDevice : bondedDevices) {
                if (bondedDevice != bluetoothDevice){
                    createOrRemoveBond(2, bondedDevice);
                }
            }
            connectDevice(bluetoothDevice);
            Log.e(Logger, getCurConnState() + "");
        });
    }

    // 初始化蓝牙
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                ToastUtil.show(mContext, "蓝牙已打开");
            } else {
                openBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
        } else {
            ToastUtil.show(mContext, "你的设备不支持蓝牙");
        }
    }

    // 这个就放在activity里
    public void initBroadcastReceiver() {
        // 接收广播
        IntentFilter intentFilter = new IntentFilter();     // 创建一个IntentFilter对象
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);   // 获得扫描结果
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  // 绑定状态变化
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);  // 开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // 扫描结束
        // 蓝牙广播接收器
        bluetoothReceiver = new BluetoothReceiver();    // 实例化广播接收器
        mContext.registerReceiver(bluetoothReceiver, intentFilter);  // 注册广播接收器
    }

    // 注销蓝牙广播
    @SuppressLint("MissingPermission")
    public void deleteBroadcastReceiver() {
        if (bluetoothReceiver == null || bluetoothAdapter == null) {
            Log.e(Logger, "broadcastReceiver或者bluetoothAdapter为空，不能终止");
            Toast.makeText(mContext, "broadcastReceiver或者bluetoothAdapter为空，不能终止", Toast.LENGTH_SHORT).show();
            return;
        }
        mContext.unregisterReceiver(bluetoothReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    // 蓝牙设备可被发现的时间
    @SuppressLint("MissingPermission")
    public void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); // 设置蓝牙可见性，最多300秒
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); //搜索300秒
            mContext.startActivity(discoverIntent);
        }
    }

    /**
     * 创建或者取消匹配
     *
     * @param type   处理类型 1 匹配  2  取消匹配
     * @param device 设备
     */
    public void createOrRemoveBond(int type, BluetoothDevice device) {
        Method method;
        try {
            if (type == 1) {
                method = BluetoothDevice.class.getMethod("createBond");
                method.invoke(device);
            } else if (type == 2) {
                method = BluetoothDevice.class.getMethod("removeBond");
                method.invoke(device);
                deviceAdapter.removeDevice(device); // 清除列表中已经取消了配对的设备
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(Logger, e.toString());
        }
    }

    @SuppressLint("MissingPermission")
    public void scanBluetooth() {
        if (bluetoothAdapter != null) { //是否支持蓝牙
            if (bluetoothAdapter.isEnabled()) { //打开
                // 开始扫描周围的蓝牙设备,如果扫描到蓝牙设备，通过广播接收器发送广播
                if (deviceAdapter != null) {    //当适配器不为空时，这时就说明已经有数据了，所以清除列表数据，再进行扫描
                    deviceAdapter.clear();
                }
                bluetoothAdapter.startDiscovery();
            } else {  // 未打开
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                openBluetoothLauncher.launch(intent);
            }
        } else {
            ToastUtil.show(mContext, "你的设备不支持蓝牙");
        }
    }

    /**
     * 连接设备
     */
    @SuppressLint("MissingPermission")
    public void connectDevice(BluetoothDevice btDevice) {
        if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            createOrRemoveBond(1, btDevice);    // 建立匹配
            // 开始连接
            startConnectDevice(btDevice, MY_BLUETOOTH_UUID);
        } else {
            SmileDialog dialog = new SmileDialogBuilder(appCompatActivity, SmileDialogType.WARNING)
                    .hideTitle(true)
                    .setContentText("确定取消配对吗")
                    .setConformBgResColor(R.color.warning)
                    .setConformTextColor(Color.WHITE)
                    .setCancelTextColor(Color.BLACK)
                    .setCancelButton("取消")
                    .setCancelBgResColor(R.color.whiteSmoke)
                    .setConformButton("确定", () -> {
                        createOrRemoveBond(2, btDevice);//取消匹配
                    })
                    .build();
            dialog.show();
        }
    }

    /**
     * 广播接收器
     */
    @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
    public final class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 扫描到设备
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                deviceAdapter.getBondedDevice(pairedDevices);
                //获取周围蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceAdapter.addDevice(device);
            }
            // 设备绑定状态发生改变
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                deviceAdapter.notifyDataSetChanged();   // 刷新适配器
            }
            // 开始扫描
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                layLoading.setVisibility(View.VISIBLE);   // 显示加载布局
            }
            // 扫描结束
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                layLoading.setVisibility(View.GONE);  // 隐藏加载布局
            }
        }
    }

    // 利用线程连接蓝牙设备
    public void startConnectDevice(final BluetoothDevice bluetoothDevice, String uuid) {
        if (bluetoothDevice == null) {
            Log.e(Logger, "startConnectDevice-->bluetoothDevice == null");
            return;
        }
        if (bluetoothAdapter == null) {
            Log.e(Logger, "startConnectDevice-->bluetoothAdapter == null");
            return;
        }
        //发起连接
        connectThread = new ConnectThread(bluetoothAdapter, bluetoothDevice, uuid);
        // 蓝牙连接为耗时任务，需要写在线程中，用回调来实现异步
        connectThread.setOnBluetoothConnectListener(new ConnectThread.OnBluetoothConnectListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onStartConn() {
                Log.e(Logger, "startConnectDevice-->开始连接..." + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
            }

            @Override
            public void onConnSuccess(BluetoothSocket bluetoothSocket) {
                //标记当前连接状态为true
                curConnState = true;
                BluetoothActivity.connect_status = true;
                curBluetoothDevice = bluetoothDevice;
                //管理连接，收发数据
                managerConnectSendReceiveData(bluetoothSocket);
                //handler传递连接成功状态
                Message message = new Message();
                message.what = CONNECTED_SUCCESS_STATUE;
                handler.sendMessage(message);
                //数据库存储蓝牙设备信息
                SharedPreferences.Editor editor = mContext.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
                editor.putString("last_connected_address", bluetoothDevice.getAddress());
                editor.apply();
            }

            @Override
            public void onConnFailure(String errorMsg) {
                curConnState = false;
                BluetoothActivity.connect_status = false;
                curBluetoothDevice = null;
                Log.e(Logger, "onConnFailure-->连接失败...");
                Message message = new Message();
                message.what = CONNECTED_FAILURE_STATUE;
                handler.sendMessage(message);
            }
        });
        connectThread.start();
    }

    //管理现有连接，收发数据
    public void managerConnectSendReceiveData(BluetoothSocket bluetoothSocket) {
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        connectedThread.setOnSendReceiveDataListener(new ConnectedThread.OnSendReceiveDataListener() {
            @Override
            public void onSendDataSuccess(byte[] data) {
                Log.w(Logger, "发送数据成功,长度" + data.length + "->" + bytes2HexString(data, data.length));
                BluetoothActivity.connect_status = true;
            }

            @Override
            public void onSendDataError(byte[] data, String errorMsg) {
                Log.e(Logger, "发送数据出错,长度" + data.length + "->" + bytes2HexString(data, data.length));
                // 发送数据出错，说明蓝牙未连接
                BluetoothActivity.connect_status = false;
                BluetoothActivity.bluetoothService.setCurBluetoothDevice(null);
            }

            @Override
            public void onReceiveDataSuccess(byte[] buffer) {
                Log.w(Logger, "成功接收数据,长度" + buffer.length + "->" + bytes2HexString(buffer, buffer.length));
                // 收到人下棋完成信号，来自于手动按绿色按钮
                if (bytes2HexString(buffer, buffer.length).equals("41")){

                }
                BluetoothActivity.connect_status = true;
            }

            @Override
            public void onReceiveDataError(String errorMsg) {
                Log.e(Logger, "接收数据出错：" + errorMsg);
            }
        });
    }

    /**
     * 发送数据
     *
     * @param data  要发送的数据 字符串
     * @param isHex 是否是16进制字符串
     * @return true 发送成功  false 发送失败
     */
    public boolean sendData(String data, boolean isHex) {
        if (connectedThread == null) {
            Log.e(Logger, "sendData:string -->connectedThread == null " + data);
            return false;
        }
        if (data == null || data.length() == 0) {
            Log.e(Logger, "sendData:string-->要发送的数据为空");
            return false;
        }

        if (isHex) {  //是16进制字符串
            data = data.replace(" ", "");  //取消空格
            //检查16进制数据是否合法
            if (data.length() % 2 != 0) {
                //不合法，最后一位自动填充0
                String lasts = "0" + data.charAt(data.length() - 1);
                data = data.substring(0, data.length() - 2) + lasts;
            }
            Log.d(Logger, "sendData:string -->准备写入：" + data);  //加空格显示
            return connectedThread.write(hexString2Bytes(data));
        }
        //普通字符串
        Log.d(Logger, "sendData:string -->准备写入：" + data);
        return connectedThread.write(data.getBytes());
    }

    // 以下为一些get方法
    public ConnectThread getConnectThread() {
        return connectThread;
    }

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    public boolean getCurConnState() {
        return curConnState;
    }

    public BluetoothDevice getCurConDevice() {
        return curBluetoothDevice;
    }

    public void setCurBluetoothDevice(BluetoothDevice curBluetoothDevice) {
        this.curBluetoothDevice = curBluetoothDevice;
    }
}