package com.irlab.view.bluetooth;

import static android.content.Context.MODE_PRIVATE;
import static com.irlab.view.activity.BluetoothActivity.CREATE_BOND;
import static com.irlab.view.activity.BluetoothActivity.MY_BLUETOOTH_UUID;
import static com.irlab.view.activity.BluetoothActivity.REMOVE_BOND;
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
import android.widget.Toast;

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

    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;
    public static final String Logger = BluetoothService.class.getName();

    private final Context mContext;
    public AppCompatActivity appCompatActivity;
    private final DeviceAdapter deviceAdapter;
    private final Handler handler;

    private BluetoothAdapter bluetoothAdapter;
    private ConnectedThread connectedThread; // 管理连接的线程
    private ConnectThread connectThread;

    private boolean curConnState = false; // 当前设备连接状态
    private BluetoothDevice curBluetoothDevice;
    public String lastConnectedDeviceAddress;

    public BluetoothService(Context context, DeviceAdapter deviceAdapter, AppCompatActivity appCompatActivity, Handler handler) {
        this.mContext = context;
        this.deviceAdapter = deviceAdapter;
        this.handler = handler;
        this.appCompatActivity = appCompatActivity;
        lastConnectedDeviceAddress = mContext.getSharedPreferences("device", MODE_PRIVATE).getString("last_connected_address", "");
    }

    // 打开蓝牙
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        deviceAdapter.clear();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(mContext, "当前手机设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            //手机设备支持蓝牙，判断蓝牙是否已开启
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(mContext, "手机蓝牙已开启", Toast.LENGTH_SHORT).show();
            } else {
                // 如果手机蓝牙未开启，则开启手机蓝牙
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mContext.startActivity(enableBtIntent);
            }
        }
    }


    /**
     * 蓝牙设备可被发现的时间
     * 原来是 bluetooth activity 初始化时候进行调用,先改为不调用
     */
    @SuppressLint("MissingPermission")
    public void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); // 设置蓝牙可见性
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // 搜索300秒
            mContext.startActivity(discoverIntent);
        }
    }

    @SuppressLint("MissingPermission")
    public void addAllPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceAdapter.addBondedDevice(pairedDevices);
    }

    @SuppressLint("MissingPermission")
    public void disBondAllDevices() {
        // 获取已经绑定的设备
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        // 将已经绑定的设备解绑
        for (BluetoothDevice bondedDevice : bondedDevices) {
            createOrRemoveBond(REMOVE_BOND, bondedDevice);
        }
    }


    @SuppressLint("MissingPermission")
    public void scanBluetooth(Context view) {
        if (bluetoothAdapter != null) { //是否支持蓝牙
            if (bluetoothAdapter.isEnabled()) { //打开
                // 开始扫描周围的蓝牙设备,如果扫描到蓝牙设备，通过广播接收器发送广播
                if (deviceAdapter != null) {    //当适配器不为空时，这时就说明已经有数据了，所以清除列表数据，再进行扫描
                    deviceAdapter.clear();
                }
                bluetoothAdapter.startDiscovery();
            } else {  // 未打开
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mContext.startActivity(intent);
            }
        } else {
            ToastUtil.show(view, "你的设备不支持蓝牙");
        }
    }

    @SuppressLint("MissingPermission")
    public void prepareConnect(BluetoothDevice device, boolean isClick) {

        if (device.getBondState() == BluetoothDevice.BOND_BONDED && !isClick){
            // 已经匹配，并且上次练过，直接连接
            // 数据库存储蓝牙设备信息
            SharedPreferences.Editor editor = mContext.getSharedPreferences("device", MODE_PRIVATE).edit();
            editor.putString("last_connected_address", device.getAddress());
            editor.apply();
            // 开始连接
            startConnectDevice(device, MY_BLUETOOTH_UUID);
        }
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            createOrRemoveBond(CREATE_BOND, device);    // 建立匹配
            //数据库存储蓝牙设备信息
            SharedPreferences.Editor editor = mContext.getSharedPreferences("device", MODE_PRIVATE).edit();
            editor.putString("last_connected_address", device.getAddress());
            editor.apply();
            // 开始连接
            startConnectDevice(device, MY_BLUETOOTH_UUID);
        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED && isClick) {
            SmileDialog dialog = new SmileDialogBuilder(appCompatActivity, SmileDialogType.WARNING)
                    .hideTitle(true)
                    .setContentText("确定取消配对吗")
                    .setConformBgResColor(R.color.warning)
                    .setConformTextColor(Color.WHITE)
                    .setCancelTextColor(Color.BLACK)
                    .setCancelButton("取消")
                    .setCancelBgResColor(R.color.whiteSmoke)
                    .setConformButton("确定", () -> {
                        createOrRemoveBond(REMOVE_BOND, device);  //取消匹配
                    })
                    .build();
            dialog.show();
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
            if (type == CREATE_BOND) {
                method = BluetoothDevice.class.getMethod("createBond");
                method.invoke(device);
            } else if (type == REMOVE_BOND) {
                method = BluetoothDevice.class.getMethod("removeBond");
                method.invoke(device);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(Logger, e.toString());
        }
    }

    /**
     * 定义广播接收器
     * 接收并改变蓝牙状态信息
     */
    @SuppressLint("MissingPermission")
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                deviceAdapter.addBondedDevice(pairedDevices);
                // 获取周围蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //  添加device
                if (device.getName() != null) { //如果名字是null 就不加入列表中显示
                    deviceAdapter.addDevice(device);
                }
                if (device.getAddress().equals(lastConnectedDeviceAddress)) {
                    prepareConnect(device, false);
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) { // 设备绑定状态发生改变
                deviceAdapter.notifyDataSetChanged();   // 刷新适配器
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {  // 正在断开连接
                deviceAdapter.notifyDataSetChanged();
            }
        }
    };


    /**
     * 注册蓝牙广播过滤器
     * 在初始化蓝牙 init bluetooth 时候调用
     */
    @SuppressLint("MissingPermission")
    public void RegisterBroadcast() {
        Log.d("Register", "注册广播");
        IntentFilter bluetoothIntent = new IntentFilter();
        bluetoothIntent.addAction(BluetoothDevice.ACTION_FOUND);
        // 注册广播这里先取消下面两个action
        // bluetoothIntent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        // bluetoothIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        Log.d("Register", "广播信息过滤注册");
        mContext.registerReceiver(broadcastReceiver, bluetoothIntent);
        bluetoothAdapter.startDiscovery();
    }

    /**
     * 注销蓝牙广播
     * 在bluetooth activity destroy 时调用
     */
    @SuppressLint("MissingPermission")
    public void deleteBroadcast() {
        if (bluetoothAdapter == null) {
            Log.e("Delete", "broadcastReceiver或者bluetoothAdapter为空，不能终止");
            Toast.makeText(mContext, "broadcastReceiver或者bluetoothAdapter为空，不能终止", Toast.LENGTH_SHORT).show();
            return;
        }
        mContext.unregisterReceiver(broadcastReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    // 利用线程连接蓝牙设备
    public void startConnectDevice(final BluetoothDevice bluetoothDevice, String uuid) {
        if (bluetoothDevice == null) {
            Log.e("startConnectDevice", "startConnectDevice-->bluetoothDevice == null");
            return;
        }
        if (bluetoothAdapter == null) {
            Log.e("startConnectDevice", "startConnectDevice-->bluetoothAdapter == null");
            return;
        }
        //发起连接
        connectThread = new ConnectThread(bluetoothAdapter, bluetoothDevice, uuid);
        // 蓝牙连接为耗时任务，需要写在线程中，用回调来实现异步
        connectThread.setOnBluetoothConnectListener(new ConnectThread.OnBluetoothConnectListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onStartConn() {
                Log.e("startConnectDevice", "startConnectDevice-->开始连接..." + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
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
            }

            @Override
            public void onConnFailure(String errorMsg) {
                curConnState = false;
                BluetoothActivity.connect_status = false;
                curBluetoothDevice = null;
                Log.e("onConnFailure", "onConnFailure-->连接失败...");
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
                Log.w("onSendDataSuccess", "发送数据成功,长度" + data.length + "->" + bytes2HexString(data, data.length));
                BluetoothActivity.connect_status = true;
            }

            @Override
            public void onSendDataError(byte[] data, String errorMsg) {
                Log.e("onSendDataError", "发送数据出错,长度" + data.length + "->" + bytes2HexString(data, data.length));
                // 发送数据出错，说明蓝牙未连接
                BluetoothActivity.connect_status = false;
                BluetoothActivity.bluetoothService.setCurBluetoothDevice(null);
            }

            @Override
            public void onReceiveDataSuccess(byte[] buffer) {
                Log.w("onReceiveDataSuccess", "成功接收数据,长度" + buffer.length + "->" + bytes2HexString(buffer, buffer.length));
                // 落子成功
                if (buffer[0]==76){
                    sendData("WZ", false);
                }

                BluetoothActivity.connect_status = true;
            }

            @Override
            public void onReceiveDataError(String errorMsg) {
                Log.e("onReceiveDataError", "接收数据出错：" + errorMsg);
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
            Log.e("sendData", "sendData:string -->connectedThread == null " + data);
            return false;
        }
        if (data == null || data.length() == 0) {
            Log.e("sendData", "sendData:string-->要发送的数据为空");
            return false;
        }

        if (isHex) {  //是16进制字符串
            data.replace(" ", "");  //取消空格
            //检查16进制数据是否合法
            if (data.length() % 2 != 0) {
                //不合法，最后一位自动填充0
                String lasts = "0" + data.charAt(data.length() - 1);
                data = data.substring(0, data.length() - 2) + lasts;
            }
            Log.d("sendData", "sendData:string -->准备写入：" + data);  //加空格显示
            return connectedThread.write(hexString2Bytes(data));
        }
        //普通字符串
        Log.d("sendData", "sendData:string -->准备写入：" + data);
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
