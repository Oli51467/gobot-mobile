package com.irlab.view.utils;

import static com.irlab.base.MyApplication.JSON;
import static com.irlab.base.MyApplication.SERVER;
import static com.irlab.base.MyApplication.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.irlab.base.MyApplication;
import com.irlab.base.utils.HttpUtil;
import com.irlab.base.utils.ToastUtil;
import com.irlab.view.models.Game;
import com.irlab.view.processing.cornerDetector.Corner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileHelper {

    private String userName;
    private String playInfo;
    private File gameRecordFolder;

    public FileHelper(Game game) {
        playInfo = "黑方:   " + game.getBlackPlayer() + "     白方:   " + game.getWhitePlayer();
        SharedPreferences sharedPreferences = MyApplication.getInstance().preferences;
        userName = sharedPreferences.getString("userName", null);
        gameRecordFolder = new File(Environment.getExternalStorageDirectory() + "/archive_recorder");
    }

    public File getTempFile() {
        return new File(gameRecordFolder, "temp_file");
    }


    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void storeGameTemporarily(Game game, Corner[] corners) {
        File file = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(file, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(game);
                oos.writeObject(corners);
                oos.close();
                fos.close();
                Log.i("Recorder", "Game temporarily saved in " + file.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("Recorder", "External storage not available to store temporary game state.");
        }
    }

    public void restoreGameStoredTemporarily(Game game, Corner[] boardCorners) {
        File file = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);

                game.copy((Game) ois.readObject());
                Corner[] savedCorners = (Corner[]) ois.readObject();
                for (int i = 0; i < 4; i++) {
                    boardCorners[i].copy(savedCorners[i]);
                }

                ois.close();
                fis.close();
                Log.i("Recorder", "恢复游戏");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("Recorder", "External storage not available to restore temporary game state.");
        }
    }

    public void saveGame(Game game, Context context) {
        String json = JsonUtil.getJsonFormOfGame(userName, playInfo, "黑中盘胜", game.sgf());
        RequestBody requestBody = FormBody.create(JSON, json);
        HttpUtil.sendOkHttpResponse(SERVER + "/api/saveGame", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String status = jsonObject.getString("status");
                    Message msg = new Message();
                    msg.obj = context;
                    if (status.equals("success")) {
                        msg.what = 1;
                    }
                    else {
                        msg.what = 2;
                    }
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    //用handler更新UI,动态获取
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                ToastUtil.show((Context) msg.obj, "保存成功");
            }
            else if (msg.what == 2) {
                ToastUtil.show((Context) msg.obj, "服务器异常");
            }
        }
    };
}
