package com.irlab.view;

import android.app.Activity;
import android.os.Bundle;

import onion.w4v3xrmknycexlsd.lib.sgfcharm.SgfController;
import onion.w4v3xrmknycexlsd.lib.sgfcharm.view.SgfView;

public class SGFActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sgfactivity);
        // bundle接收跳转过来的Activity传递来的数据
        Bundle bundle = getIntent().getExtras();
        String code = bundle.getString("code");
        // 通过第三方包获取SGF控制器
        SgfController sgfController = new SgfController(true, SgfController.InteractionMode.FREE_PLAY);
        sgfController.setShowVariations(false);
        SgfView sgfView = findViewById(R.id.sgfView);
        // 将指定代码的棋谱加载到view中
        sgfController.load(code).into(sgfView);
    }
}