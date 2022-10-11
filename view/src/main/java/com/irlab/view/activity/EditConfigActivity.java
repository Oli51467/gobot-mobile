package com.irlab.view.activity;

import static com.irlab.base.MyApplication.ENGINES;
import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.MyApplication;
import com.irlab.base.entity.CellData;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.ButtonListenerUtil;
import com.irlab.base.utils.HttpUtil;
import com.irlab.view.R;
import com.irlab.view.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

/*
结构同添加配置界面
 */
public class EditConfigActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener {

    public static final String TAG = EditConfigActivity.class.getName();

    private Spinner tSpinner, engineSpinner;    // 分别为选择规则的下拉列表
    private Button save = null;
    private EditText mPlayerBlack, mPlayerWhite, mDescription;
    private Map<String, Object> config;
    private Long id;
    private int posT = 0, posEngine = 0, rule = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_config);
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        // 从bundle中拿到传来的CellData
        CellData configInfo = (CellData) getIntent().getSerializableExtra("configItem");
        // 根据id找到对应的配置信息
        id = configInfo.getId();
        HttpUtil.sendOkHttpRequest(SERVER + "/api/getPlayConfigById?id=" + id, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "config信息获取失败:" + e.getMessage());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                config = new HashMap<>();
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    config.put("playerBlack", jsonObject.getString("playerBlack"));
                    config.put("playerWhite", jsonObject.getString("playerWhite"));
                    config.put("engine", jsonObject.getString("engine"));
                    config.put("desc", jsonObject.getString("desc"));
                    config.put("komi", jsonObject.getInt("komi"));
                    config.put("rule", jsonObject.getInt("rule"));
                    Message msg = new Message();
                    msg.what = ResponseCode.GET_PLAY_CONFIG_SUCCESSFULLY.getCode();
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    private void initView() {
        RadioGroup mRule = findViewById(R.id.rg_rule);
        ImageView back = findViewById(R.id.header_back);
        RadioButton chineseRule = findViewById(R.id.rb_chinese_rule);
        RadioButton japaneseRule = findViewById(R.id.rb_japanese_rule);
        mPlayerBlack = findViewById(R.id.et_player_black);
        mPlayerWhite = findViewById(R.id.et_player_white);
        mDescription = findViewById(R.id.et_desc);
        tSpinner = findViewById(R.id.spinner_T);
        engineSpinner = findViewById(R.id.spinner_engine);
        save = findViewById(R.id.btn_save);

        // 设置内容
        mPlayerBlack.setText(Objects.requireNonNull(config.get("playerBlack")).toString());
        mPlayerWhite.setText(Objects.requireNonNull(config.get("playerWhite")).toString());
        mDescription.setText(Objects.requireNonNull(config.get("desc")).toString());
        posT = (Integer) Objects.requireNonNull(config.get("komi"));
        // 判断选择的是哪个段位来确定引擎
        for (int i = 0; i < ENGINES.length; i ++ ) {
            if (Objects.requireNonNull(config.get("engine")).equals(ENGINES[i])) {
                posEngine = i;
                break;
            }
        }
        if ((Integer) Objects.requireNonNull(config.get("rule")) == 0) {
            chineseRule.setChecked(true);
        } else {
            japaneseRule.setChecked(true);
        }

        // 设置事件
        back.setOnClickListener(this);
        save.setOnClickListener(this);
        mRule.setOnCheckedChangeListener(this);
    }

    private void initAdapter() {
        // 初始化适配器
        // 选择让几子的String适配器
        ArrayAdapter<String> tAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MyApplication.T);
        ArrayAdapter<String> engineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MyApplication.ENGINES);
        // 将adapter 添加到spinner中
        tSpinner.setAdapter(tAdapter);
        engineSpinner.setAdapter(engineAdapter);
        // 添加事件Spinner事件监听
        tSpinner.setSelection(posT);
        engineSpinner.setSelection(posEngine);
        tSpinner.setOnItemSelectedListener(this);
        engineSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, PlayConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if (vid == R.id.btn_save) {
            // 获取输入框的数据
            String userName = MyApplication.getInstance().preferences.getString("userName", null);
            String playerBlack = mPlayerBlack.getText().toString();
            String playerWhite = mPlayerWhite.getText().toString();
            String desc = mDescription.getText().toString();
            // 将该配置封装成一个对象插入到数据库
            String json = JsonUtil.getJsonFormOfPlayConfig(userName, playerBlack, playerWhite, MyApplication.ENGINES[posEngine], desc, posT, rule);
            RequestBody requestBody = RequestBody.Companion.create(json, JSON);
            // 插入成功后跳转
            HttpUtil.sendOkHttpResponse(SERVER + "/api/updatePlayConfig?id=" + id, requestBody, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "配置信息保存失败" + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    runOnUiThread(() -> {
                        // 插入成功后跳转
                        Toast.makeText(EditConfigActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(EditConfigActivity.this, PlayConfigActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
            });
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int pid = parent.getId();
        if (pid == R.id.spinner_T) {
            posT = position;
        } else if (pid == R.id.spinner_engine) {
            posEngine = position;
        }
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

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ResponseCode.GET_PLAY_CONFIG_SUCCESSFULLY.getCode()) {
                // 初始化界面和数据
                initView();
                initAdapter();
                ButtonListenerUtil.buttonEnabled(save, 0, 100,mPlayerBlack, mPlayerWhite, mDescription);
                ButtonListenerUtil.buttonChangeColor(0, 100, EditConfigActivity.this, save, mPlayerBlack, mPlayerWhite, mDescription);
            }
        }
    };
}
