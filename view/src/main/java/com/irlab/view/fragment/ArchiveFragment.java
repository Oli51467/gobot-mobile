package com.irlab.view.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.irlab.base.entity.CellData;
import com.irlab.base.utils.FileUtil;
import com.irlab.view.R;
import com.irlab.view.activity.EditConfigActivity;
import com.irlab.view.activity.SGFInfoActivity;
import com.irlab.view.adapter.ArchiveAdapter;
import com.irlab.view.adapter.RecyclerViewAdapter;
import com.irlab.view.adapter.RecyclerViewEmptySupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveFragment extends Fragment implements ArchiveAdapter.setClick {

    RecyclerViewEmptySupport mRecyclerView = null;

    ArchiveAdapter mAdapter = null;

    LinearLayoutManager linearLayoutManager = null;

    View view;

    private TextView emptyView;

    // map存放数据
    private List<Map<String,Object>> list = new ArrayList<>();

    private File sgfPath;

    private List<File> fileList = new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);
        // 获取存储sgf文件的外部存储路径
        sgfPath = new File(Environment.getExternalStorageDirectory() + "/archive_recorder");
        if (!sgfPath.exists() && !sgfPath.isDirectory()) {
            sgfPath.mkdir();
        }
        // 获取该路径下所有.sgf文件
        fileList = FileUtil.getFilesEndWithSameSuffix(sgfPath, ".sgf");
        // 初始化数据
        try {
            initData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initViews();
        // 创建自定义适配器, 设置给listview
        mAdapter = new ArchiveAdapter(list);
        // 为 RecyclerView设置LayoutManger
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // 设置item固定大小
        mRecyclerView.setHasFixedSize(true);
        // 为视图添加适配器
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);
        return view;
    }

    private void initViews() {
        mRecyclerView = view.findViewById(R.id.archive_item);
        emptyView = view.findViewById(R.id.empty);
        linearLayoutManager = new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false);
    }

    // 从SDCard读取数据并放到list中
    private void initData() throws IOException {
        for (File file : fileList) {
            Map<String, Object> map = new HashMap<>();
            // 根据文件名字符串截取出各种信息 先截取最前面的时间戳
            String afterTrimDate = file.getName().substring(17);
            // 再将时间戳后面的后缀去掉
            afterTrimDate = afterTrimDate.substring(0, afterTrimDate.length() - 4);
            // 利用分隔符得到黑方和白方的对战信息
            int splitPosition = afterTrimDate.indexOf('-');
            String BlackInfo = afterTrimDate.substring(0, splitPosition);
            String WhiteInfo = afterTrimDate.substring(splitPosition + 1);
            // 读取该sgf的内容得到code
            try {
                FileInputStream inputStream = new FileInputStream(file);
                int length = inputStream.available();
                byte[] bytes = new byte[length];
                inputStream.read(bytes);
                map.put("code", new String(bytes, StandardCharsets.UTF_8));
                inputStream.close();
            }
            catch (IOException e) {
                Log.d("ArchiveFragment", e.getMessage());
            }
            // 将数据传递给适配器
            map.put("info", "黑方:   " + BlackInfo + "     白方:   " + WhiteInfo);
            map.put("date", "对局日期:   " + file.getName().substring(0, 16));
            list.add(map);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClickListener(View view, int position) {
        // 根据点击的位置 拿到该配置信息的code
        Map<String, Object> map = list.get(position);
        Intent intent = new Intent(this.getActivity(), SGFInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 通过bundle向下一个activity传递一个对象 该对象必须先实现序列化接口
        Bundle bundle = new Bundle();
        bundle.putString("code", map.get("code").toString());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.setOnItemClickListener(this);
    }
}