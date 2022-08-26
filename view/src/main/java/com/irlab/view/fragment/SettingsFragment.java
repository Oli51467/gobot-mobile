package com.irlab.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.irlab.base.MyApplication;
import com.irlab.view.R;
import com.irlab.view.activity.BluetoothActivity;
import com.irlab.view.bluetooth.BluetoothService;

public class SettingsFragment extends Fragment {

    TextView showInfo;

    View view;

    String userName;

    SharedPreferences preferences;

    Button logout;

    // 发送指令内容
    TextView edittext;
    // 发送按钮
    Button sendButton;
    //指令记录
    TextView commandHistory;

    BluetoothService bluetoothService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);
        setView(view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = MyApplication.getInstance().preferences;

        bluetoothService =  BluetoothActivity.bluetoothService;

    }

    private class MyonclickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            //获取editText控件的数据
            String my_string = edittext.getText().toString();
            //判断有无输入
            if(TextUtils.isEmpty(my_string))
            {
                //在手机上输出
                //Toast.LENGTH_SHORT:函数功能为显示时间短
                //Toast.LENGTH_LONG :显示时间长
                commandHistory.append("\n");
                commandHistory.append("未发送数据！");
            }
            else{
                commandHistory.append("\n" + "send: " + my_string);

                bluetoothService.sendData(my_string, false);

                int line = commandHistory.getLineCount();

                //超出屏幕自动滚动显示(3是当前页面显示的最大行数)
                if (line > 3) {
                    int offset = commandHistory.getLineCount() * commandHistory.getLineHeight();
                    commandHistory.scrollTo(0, offset - commandHistory.getHeight() + commandHistory.getLineHeight());
                }
            }

        }

    }

    public void setView(View view) {
        this.view = view;
        showInfo = view.findViewById(R.id.tv_show_username);
        logout = view.findViewById(R.id.btn_logout);

        edittext = view.findViewById(R.id.editText1);
        sendButton = view.findViewById(R.id.button1);
        commandHistory = view.findViewById(R.id.layout_bluetooth_content);

        commandHistory.setMovementMethod(new ScrollingMovementMethod());

    }

    @Override
    public void onStart() {
        super.onStart();
        // 在SharedPreferences中获取用户数据
        userName = preferences.getString("userName", null);
        showInfo.setText("Hi! " + userName);
        sendButton.setOnClickListener(new MyonclickListener());

        bluetoothService =  BluetoothActivity.bluetoothService;
    }
}