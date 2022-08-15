package com.irlab.view.activity;

import static com.irlab.base.MyApplication.SERVER;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.irlab.base.MyApplication;
import com.irlab.base.entity.CellData;
import com.irlab.base.utils.HttpUtil;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.adapter.RecyclerViewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SelectConfigActivity extends AppCompatActivity implements View.OnClickListener, RecyclerViewAdapter.setClick {

    public static final String TAG = SelectConfigActivity.class.getName();

    public static final int PERMISSION_REQUEST_CODE = 123;

    private RecyclerView mRecyclerView = null;

    private RecyclerViewAdapter mAdapter = null;

    private Button begin = null;

    // 每一条数据都是一个CellData实体 放到list中
    private List<CellData> list;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_config);
        getSupportActionBar().hide();
        sharedPreferences = MyApplication.getInstance().preferences;
        initData();
    }

    // 初始化界面及事件
    private void initViews() {
        mRecyclerView = findViewById(R.id.play_setting_item);
        ImageView back = findViewById(R.id.header_back);
        begin = findViewById(R.id.btn_begin);

        back.setOnClickListener(this);
        begin.setOnClickListener(this);
        mAdapter.setOnItemClickListener(this);
    }

    // 初始化数据
    private void initData() {
        list = new ArrayList<>();
        String userName = sharedPreferences.getString("userName", null);
        // 从数据库拿到所有已经配置好的配置信息
        HttpUtil.sendOkHttpRequest(SERVER + "/api/getPlayConfig?userName=" + userName, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                addCellDataToList(responseData);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, MainView.class);
            startActivity(intent);
            finish();
        }
        else if (vid == R.id.btn_begin) {
            checkCameraPermission();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onItemClickListener(View view, int position) {
        begin.setBackgroundResource(com.irlab.base.R.drawable.btn_login_normal);
        begin.setEnabled(true);
        mAdapter.setmPosition(position);
        mAdapter.notifyDataSetChanged();
    }



    private void checkCameraPermission() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(SelectConfigActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(SelectConfigActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(SelectConfigActivity.this, neededPermissions.toArray(new String[neededPermissions.size()]), PERMISSION_REQUEST_CODE);
        } else {
            startDetectBoardAcitivity();
        }
    }

    private void startDetectBoardAcitivity() {
        // 根据点击的位置 先拿到CellData 通过CellData拿到配置信息
        CellData cellData = list.get(mAdapter.getmPosition());
        // 传递黑方、白方、贴目设置
        String blackPlayer = cellData.getPlayerBlack();
        String whitePlayer = cellData.getPlayerWhite();
        String komi = cellData.getRule() == 0 ? "7.5" : "6.5";
        String engine = cellData.getEngine();
        String rule = cellData.getKomi() == 0 ? "中国规则" : "日本规则";
        Intent intent = new Intent(this, DefineBoardPositionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 通过bundle向下一个activity传递一个对象 该对象必须先实现序列化接口
        intent.putExtra("blackPlayer", blackPlayer);
        intent.putExtra("whitePlayer", whitePlayer);
        intent.putExtra("komi", komi);
        intent.putExtra("rule", rule);
        intent.putExtra("engine", engine);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDetectBoardAcitivity();
            } else {
                Toast.makeText(SelectConfigActivity.this, getResources().getString(R.string.toast_camera_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addCellDataToList(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i ++ ) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Long cid = jsonObject.getLong("id");
                String cPlayerBlack = jsonObject.getString("playerBlack");
                String cPlayerWhite = jsonObject.getString("playerWhite");
                String cEngine = jsonObject.getString("engine");
                String cDescription = jsonObject.getString("desc");
                int cKomi = jsonObject.getInt("komi");
                int cRule = jsonObject.getInt("rule");
                CellData cellData = new CellData(cid, cPlayerBlack, cPlayerWhite, cEngine, cDescription, cKomi, cRule);
                list.add(cellData);
            }
            Message msg = new Message();
            msg.what = 1;
            handler.sendMessage(msg);
        } catch (JSONException e) {
            Log.d(TAG, e.toString());
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                // 初始化适配器 将数据填充进去
                mAdapter = new RecyclerViewAdapter(list);
                initViews();
                // 线性布局 第二个参数是容器的走向, 第三个时候反转意思就是以中间为对称轴左右两边互换。
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(SelectConfigActivity.this, LinearLayoutManager.VERTICAL, false);
                // 为 RecyclerView设置LayoutManger
                mRecyclerView.setLayoutManager(linearLayoutManager);
                // 设置item固定大小
                mRecyclerView.setHasFixedSize(true);
                // 为视图添加适配器
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    };
}