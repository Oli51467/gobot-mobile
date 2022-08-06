package com.irlab.view.activity;

import static com.irlab.base.MyApplication.SERVER;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.irlab.base.MyApplication;
import com.irlab.base.entity.CellData;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.adapter.RecyclerViewAdapter;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PlayConfigActivity extends AppCompatActivity implements View.OnClickListener, RecyclerViewAdapter.setClick, RecyclerViewAdapter.setLongClick {

    public static final String TAG = PlayConfigActivity.class.getName();

    private RecyclerView mRecyclerView = null;

    private RecyclerViewAdapter mAdapter = null;

    // 每一条数据都是一个CellData实体 放到list中
    public List<CellData> list = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_config);
        getSupportActionBar().hide();
        sharedPreferences = MyApplication.getInstance().preferences;
        initData();
    }

    // 初始化界面及事件
    private void initViews() {
        mRecyclerView = findViewById(R.id.play_setting_item);
        ImageView back = findViewById(R.id.header_back);
        TextView addSetting = findViewById(R.id.header_add);

        back.setOnClickListener(this);
        addSetting.setOnClickListener(this);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
    }

    // 初始化数据
    private void initData() {
        list = new ArrayList<>();
        // 从数据库拿到所有已经配置好的配置信息
        String userName = sharedPreferences.getString("userName", null);
        HttpUtil.sendOkHttpRequest( SERVER + "/api/getPlayConfig?userName=" + userName, new Callback() {
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
                    Long id = cellData.getId();
                    HttpUtil.sendOkHttpDelete(SERVER + "/api/deletePlayConfig?id=" + id, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() -> ToastUtil.show(PlayConfigActivity.this, "服务器异常 删除失败!"));
                        }
                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                            runOnUiThread(() -> {
                                ToastUtil.show(PlayConfigActivity.this, "删除成功!");
                                finish();
                                startActivity(new Intent(PlayConfigActivity.this, PlayConfigActivity.class));
                            });
                        }
                    });
                })
                .build();
        dialog.show();
        return false;
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

    /**
     * 在线程中更新UI会产生 Only the original thread that created a view hierarchy can touch its views 异常。
     * 原因是只有创建这个View的线程才能去操作这个view，普通会认为是将view创建在非UI线程中才会出现这个错误，因此采用handle，
     * 个人理解为消息队列，线程产生的消息，由消息队列保管，最后依次去更新UI。
     * 为了保证线程安全，Android禁止在非UI线程中更新UI，其中相关View和控件操作都不是线程安全的。
     */
    //用handler更新UI,动态获取
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                // 初始化适配器 将数据填充进去
                mAdapter = new RecyclerViewAdapter(list);
                initViews();
                // 线性布局 第二个参数是容器的走向, 第三个时候反转意思就是以中间为对称轴左右两边互换。
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PlayConfigActivity.this, LinearLayoutManager.VERTICAL, false);
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

