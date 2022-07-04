package com.irlab.view.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.irlab.base.MyApplication;
import com.irlab.base.dao.SGFDAO;
import com.irlab.base.entity.SGF;
import com.irlab.view.R;
import com.irlab.view.adapter.ArchiveAdapter;

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

    private SGFDAO sgfDao;

    private List<SGF> allSGF;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);
        sgfDao = MyApplication.getInstance().getSgfDatabase().sgfDAO();
        // 初始化组件
        listView = view.findViewById(R.id.listView);
        allSGF = sgfDao.findAll();
        // 初始化数据
        initData();
        // 创建自定义适配器, 设置给listview
        ArchiveAdapter adapter = new ArchiveAdapter(getActivity().getApplicationContext(), list);
        listView.setAdapter(adapter);
        return view;
    }

    private void initData() {
        for (SGF sgf : allSGF) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", sgf.getTitle());
            map.put("desc", sgf.getDesc());
            map.put("result", sgf.getResult());
            map.put("id", sgf.getId());
            list.add(map);
        }
    }
}