package com.irlab.view.fragment;

import static com.irlab.base.MyApplication.SERVER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.gson.JsonArray;
import com.irlab.base.MyApplication;
import com.irlab.base.response.ResponseCode;
import com.irlab.base.utils.HttpUtil;
import com.irlab.view.R;
import com.irlab.view.activity.SGFInfoActivity;
import com.irlab.view.adapter.ArchiveAdapter;
import com.irlab.view.api.ApiService;
import com.irlab.view.bean.GameInfo;
import com.irlab.view.utils.JsonUtil;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;
import com.sdu.network.NetworkApi;
import com.sdu.network.observer.BaseObserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressLint("checkResult")
public class ArchiveFragment extends Fragment implements ArchiveAdapter.setClick, AdapterView.OnItemClickListener, ArchiveAdapter.setLongClick {

    public static final String Logger = ArchiveFragment.class.getName();

    RecyclerView mRecyclerView = null;
    ArchiveAdapter mAdapter = null;
    LinearLayoutManager linearLayoutManager = null;
    View view;
    private List<GameInfo> list = new ArrayList<>();
    private String userName;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);
        userName = MyApplication.getInstance().preferences.getString("userName", null);
        // 获取棋谱
        // 初始化数据
        initData(this.getActivity());
        return view;
    }

    private void initViews() {
        mRecyclerView = view.findViewById(R.id.archive_item);
        linearLayoutManager = new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
    }

    // 从SDCard读取数据并放到list中
    private void initData(Context context) {
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
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = jsonArray.length() - 1; i >= 0; i -- ) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int gid = jsonObject.getInt("id");
                String gPlayInfo = jsonObject.getString("play_info");
                String gResult = jsonObject.getString("result");
                String gCode = jsonObject.getString("code");
                String gCreateTime = jsonObject.getString("create_time");
                GameInfo gameInfo = new GameInfo(gid, gPlayInfo, gResult, gCode, gCreateTime);
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
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                // 创建自定义适配器, 设置给listview
                mAdapter = new ArchiveAdapter(list);
                initViews();
                // 为 RecyclerView设置LayoutManger
                mRecyclerView.setLayoutManager(linearLayoutManager);
                // 设置item固定大小
                mRecyclerView.setHasFixedSize(true);
                // 为视图添加适配器
                mRecyclerView.setLayoutManager(new LinearLayoutManager((Context) msg.obj));
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    };

    @Override
    public void onItemClickListener(View view, int position) {
        // 根据点击的位置 拿到该配置信息的code
        GameInfo gameInfo = list.get(position);
        Intent intent = new Intent(this.getActivity(), SGFInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 通过bundle向下一个activity传递一个对象 该对象必须先实现序列化接口
        Bundle bundle = new Bundle();
        bundle.putString("code", gameInfo.getCode());
        bundle.putString("playInfo", gameInfo.getPlayInfo());
        bundle.putString("result", gameInfo.getResult());
        bundle.putString("createTime", gameInfo.getCreateTime());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClickListener(View view, int position) {
        @SuppressLint("ResourceAsColor") SmileDialog dialog = new SmileDialogBuilder((AppCompatActivity) this.getActivity(), SmileDialogType.ERROR)
                .hideTitle(true)
                .setContentText("你确定删除吗")
                .setConformBgResColor(R.color.delete)
                .setConformTextColor(Color.WHITE)
                .setCancelTextColor(Color.BLACK)
                .setCancelButton("取消")
                .setCancelBgResColor(R.color.whiteSmoke)
                .setWindowAnimations(R.style.dialog_style)
                .setConformButton("删除", () -> {
                    GameInfo gameInfo = list.get(position);
                    int id = gameInfo.getId();
                    HttpUtil.sendOkHttpDelete(SERVER + "/api/deleteGame?id=" + id, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {

                        }
                    });
                })
                .build();
        dialog.show();
        return false;
    }

    /**
     * 通过选中的棋谱list, 通过getItemAtPosition获取到对应的map数据
     * 再通过get("id")获取到附加在该list上的sgf的数据库索引信息
     * 再通过查找对应id获取该list对应的SGF
     * 将该SGF的棋谱信息码code通过bundle传递到展示棋谱的界面中, 该界面只有一个, 根据每次的入参code的不同展示不同的棋谱
     * @param adapterView 适配器
     * @param view 视图
     * @param pos 选中的位置
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
        // 拿到对应item的map信息
        GameInfo gameInfo = (GameInfo) adapterView.getItemAtPosition(pos);
        // 获取该条目的id 该id即对应SGF的id
        String code = gameInfo.getCode();
        // 跳转
        Intent intent = new Intent(this.getActivity(), SGFInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 在bundle中传递SGF的code给展示activity
        Bundle bundle = new Bundle();
        bundle.putString("code", code);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}