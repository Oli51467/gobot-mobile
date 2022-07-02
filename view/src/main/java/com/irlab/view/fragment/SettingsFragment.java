package com.irlab.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.irlab.base.MyApplication;
import com.irlab.view.R;

public class SettingsFragment extends Fragment {

    TextView showInfo;

    View view;

    String userName;

    SharedPreferences preferences;

    Button logout;

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
    }

    public void setView(View view) {
        this.view = view;
        showInfo = view.findViewById(R.id.tv_sss);
        logout = view.findViewById(R.id.btn_logout);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 在SharedPreferences中获取用户数据
        userName = preferences.getString("userName", null);
        showInfo.setText("Hi! " + userName);
    }
}