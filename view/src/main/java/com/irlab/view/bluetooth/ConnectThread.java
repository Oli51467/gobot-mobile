package com.irlab.view.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.irlab.view.activity.BluetoothAppActivity;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;

    @SuppressLint("MissingPermission")
    public ConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice, String uuid) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.bluetoothDevice = bluetoothDevice;

        //使用临时变量
        //bluetoothSocket是静态的
        BluetoothSocket tmp = null;

        if (bluetoothSocket != null) {
            Log.e(TAG, "ConnectThread-->bluetoothSocket != null先去释放");
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "ConnectThread-->bluetoothSocket != null已经释放");

        //1、获取bluetoothSocket
        try {
            tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread-->获取BluetoothSocket异常!" + e.getMessage());
        }

        bluetoothSocket = tmp;
        if (bluetoothSocket != null) {
            Log.w(TAG, "ConnectThread-->已获取BluetoothSocket");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        //连接之前先取消发现设备，否则会大幅降低连接尝试的速度，并增加连接失败的可能性
        if (bluetoothAdapter == null) {
            Log.e(TAG, "ConnnectThread:run-->bluetoothAdapter == null");
            return;
        }
        //取消发现设备
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (bluetoothSocket == null) {
            Log.e(TAG, "ConnectThread-->bluetoothSocket == null");
            return;
        }

        //2、通过socket去连接设备
        try {
            Log.d(TAG, "ConnectThread:run-->去连接....");
            if (onBluetoothConnectListener != null) {
                onBluetoothConnectListener.onStartConn(); // 开始去连接回调
            }
            bluetoothSocket.connect(); //connect()为阻塞调用，连接失败或 connect() 方法超时（大约 12 秒之后），它将会引发异常

            if (onBluetoothConnectListener != null) {
                onBluetoothConnectListener.onConnSuccess(bluetoothSocket);//连接成功回调
                Log.w(TAG, "ConnectThread:run-->连接成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread:run-->连接异常！" + e.getMessage());
            if (onBluetoothConnectListener != null) {
                onBluetoothConnectListener.onConnFailure("连接异常：" + e.getMessage());
            }
            cancel();
        }

    }

    public void cancel() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                Log.d(TAG, "ConnectThread:cancel-->bluetoothSocket.isConnected() = " + bluetoothSocket.isConnected());
                bluetoothSocket.close();
                bluetoothSocket = null;
                return;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
            BluetoothAppActivity.connect_status=false;
            BluetoothAppActivity.bluetoothService.setCurBluetoothDevice(null);
            Log.d(TAG, "ConnectThread:cancel-->关闭已连接的套接字释放资源");
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread:cancel-->关闭已连接的套接字释放资源异常!" + e.getMessage());
        }

    }

    private OnBluetoothConnectListener onBluetoothConnectListener;

    public void setOnBluetoothConnectListener(OnBluetoothConnectListener onBluetoothConnectListener) {
        this.onBluetoothConnectListener = onBluetoothConnectListener;
    }

    //连接状态监听者
    public interface OnBluetoothConnectListener {
        void onStartConn();  //开始连接

        void onConnSuccess(BluetoothSocket bluetoothSocket);  //连接成功

        void onConnFailure(String errorMsg);  //连接失败
    }
}