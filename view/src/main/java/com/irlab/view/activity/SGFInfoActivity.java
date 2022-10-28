package com.irlab.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.fragment.ArchiveFragment;

import onion.w4v3xrmknycexlsd.lib.sgfcharm.SgfController;
import onion.w4v3xrmknycexlsd.lib.sgfcharm.view.SgfView;

public class SGFInfoActivity extends Activity implements View.OnClickListener {

    private SgfView sgfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sgfactivity);
        initView();
        getInfo();
    }

    private void initView() {
        sgfView = findViewById(R.id.sgfView);
        findViewById(R.id.header_back).setOnClickListener(this);
    }

    private void getInfo() {
        // bundle接收跳转过来的Activity传递来的数据
        Bundle bundle = getIntent().getExtras();
        String code = bundle.getString("code");
        // 通过第三方包获取SGF控制器
        SgfController sgfController = new SgfController(false, SgfController.InteractionMode.DISABLE);
        // 将指定代码的棋谱加载到view中
        sgfController.load(code).into(sgfView);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("id",1);
            startActivity(intent);
        }
    }
}