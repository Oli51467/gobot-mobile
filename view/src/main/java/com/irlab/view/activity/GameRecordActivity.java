package com.irlab.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.gson.JsonArray;
import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.SPUtils;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.adapter.ArchiveAdapter;
import com.irlab.view.bean.GameInfo;
import com.irlab.view.bean.UserResponse;
import com.irlab.view.network.api.ApiService;
import com.irlab.view.utils.JsonUtil;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.RequestBody;

@SuppressLint("checkResult")
public class GameRecordActivity extends AppCompatActivity implements ArchiveAdapter.setClick,
        AdapterView.OnItemClickListener, ArchiveAdapter.setLongClick, View.OnClickListener {

    public static final String Logger = GameRecordActivity.class.getName();

    RecyclerView mRecyclerView = null;
    ArchiveAdapter mAdapter = null;
    LinearLayoutManager linearLayoutManager = null;
    private List<GameInfo> list = new ArrayList<>();
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game_record);
        Objects.requireNonNull(getSupportActionBar()).hide();   // ???????????????
        userName = SPUtils.getString("userName");
        findViewById(R.id.header_back).setOnClickListener(this);
        loadData(this);
    }

    private void initViews() {
        mRecyclerView = findViewById(R.id.archive_item);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
    }

    private void loadData(Context context) {
        list = new ArrayList<>();
        RequestBody requestBody = JsonUtil.userName2Json(userName);
        Message msg = new Message();
        NetworkApi.createService(ApiService.class)
                .getGames(requestBody)
                .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                    @Override
                    public void onSuccess(JsonArray gameInfo) {
                        addDataToMap(gameInfo.toString(), context);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.e(Logger, "get games onFailure:" + e.getMessage());
                        msg.what = ResponseCode.SERVER_FAILED.getCode();
                        handler.sendMessage(msg);
                    }
                }));
    }

    private void addDataToMap(String jsonData, Context context) {
        list.clear();
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = jsonArray.length() - 1; i >= 0; i -- ) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int gid = jsonObject.getInt("id");
                String gPlayInfo = jsonObject.getString("play_info");
                String gResult = jsonObject.getString("result");
                String gCode = jsonObject.getString("code");
                String gCreateTime = jsonObject.getString("end_time");
                int gSource = jsonObject.getInt("source");
                GameInfo gameInfo = new GameInfo(gid, gPlayInfo, gResult, gCode, gCreateTime, gSource);
                list.add(gameInfo);
            }
            Message msg = new Message();
            msg.what = 1;
            msg.obj = context;
            handler.sendMessage(msg);
        } catch (JSONException e) {
            Log.d(Logger, e.toString());
        }
    }


    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                // ????????????????????????, ?????????listview
                mAdapter = new ArchiveAdapter(list);
                initViews();
                // ??? RecyclerView??????LayoutManger
                mRecyclerView.setLayoutManager(linearLayoutManager);
                // ??????item????????????
                mRecyclerView.setHasFixedSize(true);
                // ????????????????????????
                mRecyclerView.setLayoutManager(new LinearLayoutManager((Context) msg.obj));
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    };

    /**
     * ?????????????????????list, ??????getItemAtPosition??????????????????map??????
     * ?????????get("id")?????????????????????list??????sgf????????????????????????
     * ?????????????????????id?????????list?????????SGF
     * ??????SGF??????????????????code??????bundle?????????????????????????????????, ?????????????????????, ?????????????????????code??????????????????????????????
     * @param view ??????
     * @param position ???????????????
     */
    @Override
    public void onItemClickListener(View view, int position) {
        // ????????????????????? ????????????????????????code
        GameInfo gameInfo = list.get(position);
        Intent intent = new Intent(this, SGFInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // ??????bundle????????????activity?????????????????? ???????????????????????????????????????
        Bundle bundle = new Bundle();
        bundle.putString("code", gameInfo.getCode());
        bundle.putString("playInfo", gameInfo.getPlayInfo());
        bundle.putString("result", gameInfo.getResult());
        bundle.putString("createTime", gameInfo.getCreateTime());
        bundle.putInt("source", gameInfo.getSource());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClickListener(View view, int position) {
        SmileDialog dialog = new SmileDialogBuilder(this, SmileDialogType.ERROR)
                .hideTitle(true)
                .setContentText("??????????????????")
                .setConformBgResColor(R.color.delete)
                .setConformTextColor(Color.WHITE)
                .setCancelTextColor(Color.BLACK)
                .setCancelButton("??????")
                .setCancelBgResColor(R.color.whiteSmoke)
                .setWindowAnimations(R.style.dialog_style)
                .setConformButton("??????", () -> {
                    int id = list.get(position).getId();
                    RequestBody requestBody = JsonUtil.id2Json(id);
                    NetworkApi.createService(ApiService.class)
                            .deleteGame(requestBody)
                            .compose(NetworkApi.applySchedulers(new BaseObserver<>() {
                                @Override
                                public void onSuccess(UserResponse userResponse) {
                                    if (userResponse.getStatus().equals("success")) {
                                        Log.d(Logger, "??????????????????");
                                    }
                                }

                                @Override
                                public void onFailure(Throwable e) {
                                    Log.e(Logger, "???????????????????????????????????????" + e.getMessage());
                                }
                            }));
                }).build();
        dialog.show();
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {}

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.header_back) {
            Intent intent = new Intent(this, MainView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}