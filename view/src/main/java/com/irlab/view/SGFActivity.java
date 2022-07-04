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
        Bundle bundle = getIntent().getExtras();
        String code = bundle.getString("code");
        SgfController sgfController = new SgfController(true, SgfController.InteractionMode.FREE_PLAY);
        SgfView sgfView = findViewById(R.id.sgfview);
        sgfController.load(code).into(sgfView);
    }
}