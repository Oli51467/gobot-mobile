package com.irlab.view.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.irlab.base.MyApplication;
import com.irlab.base.dao.ConfigDAO;
import com.irlab.base.entity.Config;
import com.irlab.base.utils.ButtonListenerUtil;
import com.irlab.view.CellData;
import com.irlab.view.R;

/*
结构同添加配置界面
 */
public class EditConfigActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener {

    private static final String[] T = {"让先", "让2子", "让3子", "让4子", "让5子", "让6子", "让7子", "让8子", "让9子"};

    // 选择让几子的String适配器
    private ArrayAdapter<String> tAdapter;

    // 分别为选择规则的下拉列表
    private Spinner tSpinner;

    private RadioButton chineseRule, japaneseRule;

    private RadioGroup mRule;

    private Button save = null;

    private ImageView back = null;

    private EditText mPlayerBlack, mPlayerWhite, mTitle, mDescription;

    private CellData configInfo = null;

    private ConfigDAO configDAO = null;

    private Config config = null;

    private int pos = 0;

    private int rule = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_config);
        configDAO = MyApplication.getInstance().getConfigDatabase().configDAO();
        // 从bundle中拿到传来的CellData
        configInfo = (CellData) getIntent().getSerializableExtra("configItem");
        // 根据id找到对应的配置信息
        int id = configInfo.getId();
        config = configDAO.findById(id);
        // 初始化界面和数据
        initView();
        initData();
        ButtonListenerUtil.buttonEnabled(save, 0, 100,mPlayerBlack, mPlayerWhite, mTitle, mDescription);
        ButtonListenerUtil.buttonChangeColor(0, 100, this, save, mPlayerBlack, mPlayerWhite, mTitle, mDescription);
    }

    private void initView() {
        mRule = findViewById(R.id.rg_rule);
        tSpinner = findViewById(R.id.spinner_T);
        save = findViewById(R.id.btn_save);
        back = findViewById(R.id.header_back);
        mPlayerBlack = findViewById(R.id.et_player_black);
        mPlayerWhite = findViewById(R.id.et_player_white);
        mTitle = findViewById(R.id.et_title);
        mDescription = findViewById(R.id.et_desc);
        chineseRule = findViewById(R.id.rb_chinese_rule);
        japaneseRule = findViewById(R.id.rb_japanese_rule);

        // 设置内容
        mPlayerBlack.setText(config.getPlayerBlack());
        mPlayerWhite.setText(config.getPlayerWhite());
        mTitle.setText(config.getTitle());
        mDescription.setText(config.getDesc());
        pos = config.getT();
        if (config.getRule() == 0) {
            chineseRule.setChecked(true);
        } else {
            japaneseRule.setChecked(true);
        }

        // 设置事件
        back.setOnClickListener(this);
        save.setOnClickListener(this);
        mRule.setOnCheckedChangeListener(this);
    }

    private void initData() {
        tAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, T);
        // 将adapter 添加到spinner中
        tSpinner.setAdapter(tAdapter);
        // 添加事件Spinner事件监听
        tSpinner.setSelection(pos);
        tSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, PlayConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (vid == R.id.btn_save) {
            // 获取输入框的数据
            String playerBlack = mPlayerBlack.getText().toString();
            String playerWhite = mPlayerWhite.getText().toString();
            String title = mTitle.getText().toString();
            String desc = mDescription.getText().toString();
            // 将该配置封装成一个对象插入到数据库
            config.setTitle(title);
            config.setDesc(desc);
            config.setPlayerBlack(playerBlack);
            config.setPlayerWhite(playerWhite);
            config.setRule(rule);
            config.setT(pos);
            configDAO.update(config);
            // 插入成功后跳转
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PlayConfigActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        pos = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_chinese_rule) {
            rule = 0;
        } else {
            rule = 1;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
