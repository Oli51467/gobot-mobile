package com.irlab.view.activity;

import android.app.Activity;
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
import com.irlab.view.R;

/*
添加对局设置界面
 */
public class AddConfigActivity extends Activity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener {

    private static final String[] T = {"让先", "让2子", "让3子", "让4子", "让5子", "让6子", "让7子", "让8子", "让9子"};

    // 分别为选择规则的下拉列表
    private Spinner tSpinner;

    private SharedPreferences preferences;

    // 选择让几子的String适配器
    private ArrayAdapter<String> tAdapter;

    private ConfigDAO configDAO;

    // 控件
    private ImageView back;

    private Button buttonSave;

    private EditText mPlayerBlack, mPlayerWhite, mDescription;

    private RadioGroup mRule;

    private RadioButton chineseRule, japaneseRule;

    // 0: 中国规则 1: 日本规则
    private int rule = 0;

    // 选择让几子 0:让先  1:让2子  2:让3子 ...
    private int pos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_setting);
        preferences = MyApplication.getInstance().preferences;
        // 通过Application拿到DAO
        configDAO = MyApplication.getInstance().getConfigDatabase().configDAO();
        // 初始化
        initViews();
        initData();
        // 从sharedPreference加载上次填写的数据
        reload();
        ButtonListenerUtil.buttonEnabled(buttonSave, 0, 100, mPlayerBlack, mPlayerWhite, mDescription);
        ButtonListenerUtil.buttonChangeColor(0, 100, this, buttonSave, mPlayerBlack, mPlayerWhite, mDescription);
    }

    private void initViews() {
        back = findViewById(R.id.header_back);
        tSpinner = findViewById(R.id.spinner_T);
        buttonSave = findViewById(R.id.btn_save);
        mPlayerBlack = findViewById(R.id.et_player_black);
        mPlayerWhite = findViewById(R.id.et_player_white);
        mDescription = findViewById(R.id.et_desc);
        mRule = findViewById(R.id.rg_rule);
        chineseRule = findViewById(R.id.rb_chinese_rule);
        japaneseRule = findViewById(R.id.rb_japanese_rule);

        chineseRule.setChecked(true);
        back.setOnClickListener(this);
        buttonSave.setOnClickListener(this);
        mRule.setOnCheckedChangeListener(this);

    }

    private void initData() {
        // 初始化适配器
        tAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, T);
        // 将adapter 添加到spinner中
        tSpinner.setAdapter(tAdapter);
        // 添加事件Spinner事件监听
        tSpinner.setOnItemSelectedListener(this);
    }

    private void reload() {
        String playerBlack = preferences.getString("playerBlack", null);
        String playerWhite = preferences.getString("playerWhite", null);
        String title = preferences.getString("title", null);
        String desc = preferences.getString("desc", null);
        int rule = preferences.getInt("rule", 0);
        int pos = preferences.getInt("pos", 0);

        if (playerBlack != null) {
            mPlayerBlack.setText(playerBlack);
        }
        if (playerWhite != null)  {
            mPlayerWhite.setText(playerWhite);
        }
        if (desc != null) {
            mDescription.setText(desc);
        }
        if (rule != 0) chineseRule.setChecked(true);
        else japaneseRule.setChecked(true);
        tSpinner.setSelection(pos);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, PlayConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (vid == R.id.btn_save) {
            // 获取输入框的数据
            String playerBlack = mPlayerBlack.getText().toString();
            String playerWhite = mPlayerWhite.getText().toString();
            String desc = mDescription.getText().toString();
            // 将该配置封装成一个对象插入到数据库
            Config config = new Config();
            config.setDesc(desc);
            config.setPlayerBlack(playerBlack);
            config.setPlayerWhite(playerWhite);
            config.setRule(rule);
            config.setT(pos);
            // #这里以后要开多线程
            configDAO.insert(config);
            // 插入成功后跳转
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PlayConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        pos = position;     // 拿到位置 代表选择了哪一条
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_chinese_rule) {
            rule = 0;
        }
        else {
            rule = 1;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 将中途写了一半的数据加载到sharedPreference
        String playerBlack = mPlayerBlack.getText().toString();
        String playerWhite = mPlayerWhite.getText().toString();
        String desc = mDescription.getText().toString();
        // 获得编辑器 用编辑器来保存
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("playerBlack", playerBlack);
        editor.putString("playerWhite", playerWhite);
        editor.putString("desc", desc);
        editor.putInt("rule", rule);
        editor.putInt("pos", pos);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buttonSave.setBackgroundResource(com.irlab.base.R.drawable.btn_login_normal);
        ButtonListenerUtil.buttonEnabled(buttonSave, 0, 100, mPlayerBlack, mPlayerWhite, mDescription);
        ButtonListenerUtil.buttonChangeColor(0, 100, this, buttonSave, mPlayerBlack, mPlayerWhite, mDescription);
    }
}