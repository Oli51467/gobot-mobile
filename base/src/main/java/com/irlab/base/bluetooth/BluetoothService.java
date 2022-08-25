package com.irlab.base.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.irlab.base.adapter.LVDevicesAdapter;
import com.irlab.base.utils.ClsUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class BluetoothService {

    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private LVDevicesAdapter lvDevicesAdapter;
    private ConnectedThread connectedThread; // 管理连接的线程
    private IntentFilter bluetoothIntent;
    private ConnectThread connectThread;

    private boolean curConnState = false; // 当前设备连接状态
    private BluetoothDevice curBluetoothDevice;
    private Handler handler;
    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;

    public BluetoothService(Context context, LVDevicesAdapter lvDevicesAdapter, Handler handler) {
        this.context = context;
        this.lvDevicesAdapter = lvDevicesAdapter;
        this.handler = handler;
    }

    // 打开蓝牙
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        lvDevicesAdapter.clear();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "当前手机设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            //手机设备支持蓝牙，判断蓝牙是否已开启
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(context, "手机蓝牙已开启", Toast.LENGTH_SHORT).show();
            } else {
                //蓝牙没有打开，去打开蓝牙。推荐使用第二种打开蓝牙方式
                //第一种方式：直接打开手机蓝牙，没有任何提示
                //bluetoothAdapter.enable();  //BLUETOOTH_ADMIN权限
                //第二种方式：友好提示用户打开蓝牙
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        }
    }

    // 蓝牙设备可被发现的时间
    @SuppressLint("MissingPermission")
    public void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); // 设置蓝牙可见性，最多300秒
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); //搜索300秒
            context.startActivity(discoverIntent);
        }
    }

    // 获取配对过的设备
    @SuppressLint("MissingPermission")
    public Set getBluetoothDevices() {
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        if (bluetoothDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                Log.d("Bluetooth", bluetoothDevice.getName() + "  |  " + bluetoothDevice.getAddress());
                //Toast.makeText(context, bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "没有配对过的设备", Toast.LENGTH_SHORT).show();
        }
        return bluetoothDevices;
    }

    @SuppressLint("MissingPermission")
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //添加device
                if (device.getName() != null) {//如果名字是null 就不加入列表中显示
                    lvDevicesAdapter.addDevice(device);
                    Log.d("device", device.getName());
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d("Match", "广播中蓝牙已经配对");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.d("Match", "广播中蓝牙正在搜索");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d("Match", "广播中蓝牙停止搜索");
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Toast.makeText(context, "广播中蓝牙停止搜索", Toast.LENGTH_SHORT).show();
                Log.d("Match", "广播中蓝牙停止搜索");
                Boolean b = bluetoothAdapter.startDiscovery();
            }
        }
    };


    // 注册蓝牙广播过滤器
    @SuppressLint("MissingPermission")
    public void RegisterBroadcast() {
        Log.d("Register", "注册广播");
        bluetoothIntent = new IntentFilter();
        bluetoothIntent.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        Log.d("Register", "广播信息过滤注册");
        context.registerReceiver(broadcastReceiver, bluetoothIntent);
        Boolean b = bluetoothAdapter.startDiscovery();
        if (b) {
            Log.d("Register", "广播蓝牙开启搜索");
        } else {
            Log.d("Register", b + " 启动蓝牙广播失败");
        }
    }

    // 注销蓝牙广播
    @SuppressLint("MissingPermission")
    public void deleteBroadcast() {
        if (broadcastReceiver == null || bluetoothAdapter == null) {
            Log.e("Delete", "broadcastReceiver或者bluetoothAdapter为空，不能终止");
            Toast.makeText(context, "broadcastReceiver或者bluetoothAdapter为空，不能终止", Toast.LENGTH_SHORT).show();
            return;
        }
        context.unregisterReceiver(broadcastReceiver);
        Boolean b = bluetoothAdapter.cancelDiscovery();
        if (b) {
            Log.d("Register", "广播蓝牙关闭搜索");
        } else {
            Log.d("Register", b + "");
        }
    }

    // 利用线程连接蓝牙设备
    public void startConnectDevice(final BluetoothDevice bluetoothDevice, String uuid, long conOutTime) {
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
                //数据库存储蓝牙设备信息
                SharedPreferences.Editor editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
                editor.putString("last_connected_address", bluetoothDevice.getAddress().toString());
                editor.apply();
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

                // 收到人下棋完成信号，来自于手动按绿色按钮
                if (bytes2HexString(buffer, buffer.length).equals("41")){

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

    //配对设备
    public boolean boundDevice(BluetoothDevice bluetoothDevice) {
        Toast.makeText(context, "开始配对", Toast.LENGTH_SHORT).show();
        if (bluetoothDevice == null) {
            Log.e("bound", "boundDevice-->bluetoothDevice == null");
            return false;
        }
        try {
            return ClsUtils.createBond(BluetoothDevice.class, bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //解绑蓝牙
    public boolean disBoundDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e("disBoundDevice", "disBoundDevice-->bluetoothDevice == null");
            return false;
        }
        try {
            return ClsUtils.removeBond(BluetoothDevice.class, bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 字节数组-->16进制字符串
     *
     * @param b      字节数组
     * @param length 字节数组长度
     * @return 16进制字符串 有空格类似“0A D5 CD 8F BD E5 F8”
     */
    public static String bytes2HexString(byte[] b, int length) {
        StringBuffer result = new StringBuffer();
        String hex;
        for (int i = 0; i < length; i++) {
            hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            result.append(hex.toUpperCase()).append(" ");
        }
        return result.toString();
    }

    /**
     * hexString2Bytes
     * 16进制字符串-->字节数组
     *
     * @param src 16进制字符串
     * @return 字节数组
     */
    public static byte[] hexString2Bytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer
                    .valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }

    //获取日期，用于Log显示
    public String getDate() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return format.format(date);
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
