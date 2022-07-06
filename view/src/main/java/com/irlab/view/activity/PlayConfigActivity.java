package com.irlab.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.irlab.base.MyApplication;
import com.irlab.base.dao.ConfigDAO;
import com.irlab.base.entity.CellData;
import com.irlab.base.entity.Config;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.adapter.RecyclerViewAdapter;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;

import java.util.ArrayList;
import java.util.List;

public class PlayConfigActivity extends AppCompatActivity implements View.OnClickListener, RecyclerViewAdapter.setClick, RecyclerViewAdapter.setLongClick {

    RecyclerView mRecyclerView = null;

    RecyclerViewAdapter mAdapter = null;

    ImageView back = null;

    TextView addSetting = null;

    LinearLayoutManager linearLayoutManager = null;

    ConfigDAO configDAO;

    // 每一条数据都是一个CellData实体 放到list中
    List<CellData> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_config);
        getSupportActionBar().hide();
        configDAO = MyApplication.getInstance().getConfigDatabase().configDAO();
        initData();
        // 初始化适配器 将数据填充进去
        mAdapter = new RecyclerViewAdapter(list);

        initViews();
        // 线性布局 第二个参数是容器的走向, 第三个时候反转意思就是以中间为对称轴左右两边互换。
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        // 为 RecyclerView设置LayoutManger
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // 设置item固定大小
        mRecyclerView.setHasFixedSize(true);

        // 为视图添加适配器
        mRecyclerView.setAdapter(mAdapter);
    }

    // 初始化界面及事件
    private void initViews() {
        mRecyclerView = findViewById(R.id.play_setting_item);
        back = findViewById(R.id.header_back);
        addSetting = findViewById(R.id.header_add);

        back.setOnClickListener(this);
        addSetting.setOnClickListener(this);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
    }

    // 初始化数据
    private void initData() {
        list = new ArrayList<>();
        // 从数据库拿到所有已经配置好的配置信息
        List<Config> configs = configDAO.findAll();
        for (Config config : configs) {
            // 将title和description填充到CardView中
            String title = config.getTitle();
            String desc = config.getDesc();
            // 这里要把id放进去 方便后续点击该cardView时找到在数据库中相应的配置信息
            int id = config.getId();
            CellData cellData = new CellData(title, desc, id);
            list.add(cellData);
        }
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_add) {
            Intent intent = new Intent(this, AddConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if (vid == R.id.header_back) {
            Intent intent = new Intent(this, MainView.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onItemClickListener(View view, int position) {
        // 根据点击的位置 先拿到CellData 通过CellData拿到该配置信息的id
        CellData cellData = list.get(position);
        Intent intent = new Intent(this, EditConfigActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 通过bundle向下一个activity传递一个对象 该对象必须先实现序列化接口
        Bundle bundle = new Bundle();
        bundle.putSerializable("configItem", cellData);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onItemLongClickListener(View view, int position) {
        // 第三方的提示框
        @SuppressLint("ResourceAsColor") SmileDialog dialog = new SmileDialogBuilder(this, SmileDialogType.ERROR)
                .hideTitle(true)
                .setContentText("你确定删除吗")
                .setConformBgResColor(R.color.delete)
                .setConformTextColor(Color.WHITE)
                .setCancelTextColor(Color.BLACK)
                .setCancelButton("取消")
                .setCancelBgResColor(R.color.whiteSmoke)
                .setWindowAnimations(R.style.dialog_style)
                // 删除一条选中的数据 根据position拿到对应的cellData 再拿到配置的id
                .setConformButton("删除", () -> {
                    CellData cellData = list.get(position);
                    int id = cellData.getId();
                    configDAO.deleteById(id);
                    finish();
                    startActivity(new Intent(this, PlayConfigActivity.class));
                })
                .build();
        dialog.show();
        return false;
    }
}

