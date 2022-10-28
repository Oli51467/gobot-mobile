package com.irlab.view.bluetooth;

import static com.irlab.view.activity.BluetoothAppActivity.MY_BLUETOOTH_UUID;
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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.irlab.view.activity.BluetoothAppActivity;
import com.irlab.view.adapter.LVDevicesAdapter;
import com.irlab.view.utils.ClsUtils;

import java.util.Set;

public class BluetoothService {

    private static final int CONNECTED_SUCCESS_STATUE = 0x03;
    private static final int CONNECTED_FAILURE_STATUE = 0x04;
    @SuppressLint("MissingPermission")
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 添加device, 如果名字是null 就不加入列表中显示
                if (device.getName() != null) {
                    lvDevicesAdapter.addDevice(device);
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d("Match", "广播中蓝牙已经配对");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d("Match", "广播中蓝牙正在搜索");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("Match", "广播中蓝牙停止搜索");
            }
        }
    };

    private final Handler handler;
    private final Context context;
    private final LVDevicesAdapter lvDevicesAdapter;

    private BluetoothAdapter bluetoothAdapter;
    private ConnectedThread connectedThread; // 管理连接的线程
    private ConnectThread connectThread;
    private boolean curConnState = false; // 当前设备连接状态
    private BluetoothDevice curBluetoothDevice;

    public BluetoothService(Context context, LVDevicesAdapter lvDevicesAdapter, Handler handler) {
        this.context = context;
        this.lvDevicesAdapter = lvDevicesAdapter;
        this.handler = handler;
    }

    // 自动连接
    public void autoConnect() {
        // 获取上一次连接的device_address
        SharedPreferences pref = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        String address = pref.getString("last_connected_address", "");

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        startConnectDevice(bluetoothDevice, MY_BLUETOOTH_UUID, 123);
    }

    // 打开蓝牙
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        lvDevicesAdapter.clear();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "当前手机设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            // 手机设备支持蓝牙，判断蓝牙是否已开启
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(context, "手机蓝牙已开启", Toast.LENGTH_SHORT).show();
            } else {
                // 蓝牙没有打开，去打开蓝牙。推荐使用第二种打开蓝牙方式
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        }
        autoConnect();
    }

    // 获取配对过的设备
    @SuppressLint("MissingPermission")
    public Set getBluetoothDevices() {
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        if (bluetoothDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                Log.d("Bluetooth", bluetoothDevice.getName() + "  |  " + bluetoothDevice.getAddress());
            }
        }
        return bluetoothDevices;
    }

    // 注册蓝牙广播过滤器
    @SuppressLint("MissingPermission")
    public void RegisterBroadcast() {
        Log.d("Register", "注册广播");
        IntentFilter bluetoothIntent = new IntentFilter();
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
        if (bluetoothAdapter == null) {
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
        // 发起连接
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
                // 标记当前连接状态为true
                curConnState = true;
                BluetoothAppActivity.connect_status = true;
                curBluetoothDevice = bluetoothDevice;
                // 管理连接，收发数据
                managerConnectSendReceiveData(bluetoothSocket);
                // handler传递连接成功状态
                Message message = new Message();
                message.what = CONNECTED_SUCCESS_STATUE;
                handler.sendMessage(message);
                // 数据库存储蓝牙设备信息
                SharedPreferences.Editor editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
                editor.putString("last_connected_address", bluetoothDevice.getAddress().toString());
                editor.apply();
            }

            @Override
            public void onConnFailure(String errorMsg) {
                curConnState = false;
                BluetoothAppActivity.connect_status = false;
                curBluetoothDevice = null;
                Log.e("onConnFailure", "onConnFailure-->连接失败...");
                Message message = new Message();
                message.what = CONNECTED_FAILURE_STATUE;
                handler.sendMessage(message);

            }
        });
        connectThread.start();

    }

    // 管理现有连接，收发数据
    public void managerConnectSendReceiveData(BluetoothSocket bluetoothSocket) {
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        connectedThread.setOnSendReceiveDataListener(new ConnectedThread.OnSendReceiveDataListener() {
            @Override
            public void onSendDataSuccess(byte[] data) {
                Log.w("onSendDataSuccess", "发送数据成功,长度" + data.length + "->" + bytes2HexString(data, data.length));
                BluetoothAppActivity.connect_status = true;
            }

            @Override
            public void onSendDataError(byte[] data, String errorMsg) {
                Log.e("onSendDataError", "发送数据出错,长度" + data.length + "->" + bytes2HexString(data, data.length));
                // 发送数据出错，说明蓝牙未连接
                BluetoothAppActivity.connect_status = false;
                BluetoothAppActivity.bluetoothService.setCurBluetoothDevice(null);
            }

            @Override
            public void onReceiveDataSuccess(byte[] buffer) {
                Log.w("onReceiveDataSuccess", "成功接收数据,长度" + buffer.length + "->" + bytes2HexString(buffer, buffer.length));
                BluetoothAppActivity.connect_status = true;
                // 落子成功
                if (buffer[0] == 76) {
                    sendData("WZ", false);
                } else if (buffer[0] == 65) {   // 按键落子
                    try {
                        Intent intent = new Intent("play");
                        intent.setPackage(context.getPackageName());
                        context.sendOrderedBroadcast(intent, null);
                    } catch (Exception e) {
                        Log.e("onReceiveDataSuccess", "调用方法错误: " + e.getMessage());
                    }
                } else if (buffer[0] == 67) {   // 没有棋子或者棋子不透光:收到C 返回发送CZ
                    sendData("CZ", false);
                }
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
        Log.d("sendData", data);
        if (connectedThread == null) {
            Log.e("sendData", "sendData:string -->connectedThread == null ");
            return false;
        }
        if (data == null || data.length() == 0) {
            Log.e("sendData", "sendData:string-->要发送的数据为空");
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

    // 解绑蓝牙
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

    // 取消配对
    public boolean cancelBoundDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e("disBoundDevice", "disBoundDevice-->bluetoothDevice == null");
            return false;
        }
        try {
            return ClsUtils.cancelBondProcess(BluetoothDevice.class, bluetoothDevice);
        } catch (Exception e) {
            Log.e("cancelBoundDevice", e.getMessage());
        }
        return true;
    }


    // 断开已经连接的设备
    public void clearConnectedThread() {
        Log.d("", "clearConnectedThread-->即将断开");

        // connectedThread断开已有连接
        if (connectedThread == null) {
            Log.e("TAG", "clearConnectedThread-->connectedThread == null");
            return;
        }
        connectedThread.terminalClose(connectThread);
        connectedThread.cancel();  //释放连接
        connectedThread = null;
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
