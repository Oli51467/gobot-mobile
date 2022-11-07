package com.irlab.view.network.api;

import com.google.gson.JsonArray;
import com.irlab.view.bean.UserResponse;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.HTTP;

/**
 * ApiService接口 统一管理应用所有的接口
 */
public interface ApiService {

    /**
     * 检查用户名是否被注册
     */
    @HTTP(method = "POST", path = "/api/getUserByName", hasBody = true)
    Observable<UserResponse> checkUser(@Body RequestBody requestBody);

    /**
     * 添加一个用户
     */
    @HTTP(method = "POST", path = "/api/addUser", hasBody = true)
    Observable<UserResponse> addUser(@Body RequestBody requestBody);

    /**
     * 检查用户信息
     */
    @HTTP(method = "POST", path = "/api/checkUserInfo", hasBody = true)
    Observable<UserResponse> checkUserInfo(@Body RequestBody requestBody);

    /**
     * 保存对局信息
     */
    @HTTP(method = "POST", path = "/api/saveGame", hasBody = true)
    Observable<UserResponse> saveGame(@Body RequestBody requestBody);

    /**
     * 获取一个用户的所有对局信息
     */
    @HTTP(method = "POST", path = "/api/getGames", hasBody = true)
    Observable<JsonArray> getGames(@Body RequestBody requestBody);

    /**
     * 获取一个用户的所有对局信息
     */
    @HTTP(method = "DELETE", path = "/api/deleteGame", hasBody = true)
    Observable<UserResponse> deleteGame(@Body RequestBody requestBody);
}

