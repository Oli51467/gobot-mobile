package com.irlab.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.MyApplication;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.activity.InstructionActivity;
import com.irlab.view.activity.PlayConfigActivity;
import com.irlab.view.activity.SelectConfigActivity;
import com.irlab.view.fragment.PlayFragment;
import com.irlab.view.fragment.ArchiveFragment;
import com.irlab.view.fragment.SettingsFragment;

import java.util.Objects;

@Route(path = "/view/main")
public class MainView extends AppCompatActivity implements View.OnClickListener {

    // 三个布局界面
    private PlayFragment playFragment = null;
    private SettingsFragment settingsFragment = null;
    private ArchiveFragment archiveFragment = null;

    // 三个显示布局
    private View playLayout = null;
    private View settingsLayout = null;
    private View archiveLayout = null;

    // 声明组件变量
    private ImageView playImg = null;
    private ImageView settingsImg = null;
    private ImageView archiveImg = null;

    private TextView playText = null;
    private TextView settingsText = null;
    private TextView archiveText = null;

    @Override
    public boolean navigateUpTo(Intent upIntent) {
        return super.navigateUpTo(upIntent);
    }

    // 用于对 Fragment进行管理
    public FragmentManager fragmentManager = null;

    SharedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 要求窗口没有 title
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setContentView(R.layout.activity_main_view);
        Objects.requireNonNull(getSupportActionBar()).hide();
        // 注入Arouter
        ARouter.getInstance().inject(this);
        // 拿到SharedPreference
        preferences = MyApplication.getInstance().preferences;
        // 初始化布局元素
        initViews();
        // 设置监听事件
        setEvents();
        // 初始化Fragment
        initFragment();
        // 设置默认的显示界面
        setTabSelection(2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 这里初始化Fragment的组件必须在onStart()中进行, 若在onCreate中初始化, 子fragment有可能未初始化完成, 导致找不到对应组件
        initFragmentViewsAndEvents();
    }

    /**
     * 在这里面获取到每个需要用到的控件的实例
     */
    public void initViews() {
        fragmentManager = getSupportFragmentManager();

        // 初始化控件
        playLayout = findViewById(R.id.layout_play);
        settingsLayout = findViewById(R.id.layout_settings);
        archiveLayout = findViewById(R.id.layout_archive);

        playImg = findViewById(R.id.img_play);
        settingsImg = findViewById(R.id.img_settings);
        archiveImg = findViewById(R.id.img_archive);

        playText = findViewById(R.id.tv_play);
        settingsText = findViewById(R.id.tv_settings);
        archiveText = findViewById(R.id.tv_archive);
    }

    // 处理activity中控件的点击事件 fragment控件的点击事件必须在onStart()中进行
    public void setEvents() {
        playLayout.setOnClickListener(this);
        settingsLayout.setOnClickListener(this);
        archiveLayout.setOnClickListener(this);
    }

    public void initFragment() {
        // 开启一个Fragment事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        settingsFragment = new SettingsFragment();
        playFragment = new PlayFragment();
        archiveFragment = new ArchiveFragment();
        // 通过事务将子fragment添加到主布局中
        transaction.add(R.id.fragment, settingsFragment,"settings");
        transaction.add(R.id.fragment, playFragment, "play");
        transaction.add(R.id.fragment, archiveFragment, "archive");
        // 提交事务
        transaction.commit();
    }

    // 初始化fragment中的控件并设置监听事件
    public void initFragmentViewsAndEvents() {
        RelativeLayout openBluetooth = findViewById(R.id.layout_bluetooth);
        Button logout = findViewById(R.id.btn_logout);
        Button play = findViewById(R.id.btn_play);
        Button playSettings = findViewById(R.id.btn_play_settings);
        Button instruction = findViewById(R.id.btn_instruction);

        openBluetooth.setOnClickListener(this);
        logout.setOnClickListener(this);
        play.setOnClickListener(this);
        playSettings.setOnClickListener(this);
        instruction.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.layout_settings) {
            setTabSelection(0);
        }
        else if (vid == R.id.layout_play) {
            setTabSelection(2);
        }
        else if (vid == R.id.layout_archive) {
            setTabSelection(1);
        }
        else if (vid == R.id.layout_bluetooth) {
            ARouter.getInstance().build("/base/bluetooth").navigation();
        }
        else if (vid == R.id.btn_logout) {
            // 退出登录时, 清空SharedPreferences中保存的用户信息, 下次登录时不再自动登录
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("userName");
            editor.apply();
            ToastUtil.show(this, "退出登录");
            // 跳转到登录界面
            ARouter.getInstance().build("/auth/login")
                    .withFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .navigation();
        }
        // 跳转到下棋界面
        else if (vid == R.id.btn_play) {
            Intent intent = new Intent(this, SelectConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
        // 对局设置
        else if (vid == R.id.btn_play_settings) {
            Intent intent = new Intent(this, PlayConfigActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
        // 使用说明
        else if (vid == R.id.btn_instruction) {
            Intent intent = new Intent(this, InstructionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    /**
     * 根据传入的index参数来设置选中的tab页 每个tab页对应的下标。
     */
    private void setTabSelection(int index) {
        // 每次选中之前先清除掉上次的选中状态
        clearSelection();
        // 开启一个Fragment事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // 先隐藏掉所有的Fragment, 防止有多个Fragment显示在界面上的情况
        hideFragments(transaction);
        // 设置界面
        if (index == 0) {
            settingsImg.setImageResource(R.drawable.tab_settings_pressed);//修改布局中的图片
            settingsText.setTextColor(Color.parseColor("#07c160"));//修改字体颜色
            transaction.show(settingsFragment);
        }
        // 棋谱界面
        else if (index == 1) {
            archiveImg.setImageResource(R.drawable.tab_archive_pressed);
            archiveText.setTextColor(Color.parseColor("#07c160"));
            transaction.show(archiveFragment);
        }
        // 下棋界面
        else if (index == 2) {
            playImg.setImageResource(R.drawable.tab_play_pressed);
            playText.setTextColor(Color.parseColor("#07c160"));
            transaction.show(playFragment);
        }
        transaction.commit();
    }

    /**
     * 清除掉所有的选中状态 取消相应控件的颜色
     */
    private void clearSelection() {
        playImg.setImageResource(R.drawable.tab_play_normal);
        playText.setTextColor(Color.parseColor("#82858b"));

        settingsImg.setImageResource(R.drawable.tab_settings_normal);
        settingsText.setTextColor(Color.parseColor("#82858b"));

        archiveImg.setImageResource(R.drawable.tab_archive_normal);
        archiveText.setTextColor(Color.parseColor("#82858b"));
    }

    /**
     * 将所有的Fragment都设置为隐藏状态 用于对Fragment执行操作的事务
     */
    private void hideFragments(FragmentTransaction transaction) {
        if (playFragment != null) {
            transaction.hide(playFragment);
        }
        if (settingsFragment != null) {
            transaction.hide(settingsFragment);
        }
        if (archiveFragment != null) {
            transaction.hide(archiveFragment);
        }
    }
}