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

public class ArchiveFragment extends Fragment {

    View view;

    SharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);
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
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}