package com.irlab.view.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.irlab.base.utils.FileUtil;
import com.irlab.view.R;
import com.irlab.view.adapter.ArchiveAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveFragment extends Fragment {

    View view;

    // Fragment内的Listview
    private ListView listView;

    // map存放数据
    private List<Map<String,Object>> list = new ArrayList<>();

    private File sgfPath;

    private List<File> fileList;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);
        // 获取存储sgf文件的外部存储路径
        sgfPath = new File(Environment.getExternalStorageDirectory() + "/archive_recorder");
        // 获取该路径下所有.sgf文件
        fileList = FileUtil.getFilesEndWithSameSuffix(sgfPath, ".sgf");
        // 初始化组件
        listView = view.findViewById(R.id.listView);

        // 初始化数据
        try {
            initData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 创建自定义适配器, 设置给listview
        ArchiveAdapter adapter = new ArchiveAdapter(getActivity().getApplicationContext(), list);
        listView.setAdapter(adapter);
        return view;
    }

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
        list.clear();
        try {
            initData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}