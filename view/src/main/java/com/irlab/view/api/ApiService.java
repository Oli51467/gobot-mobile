package com.irlab.view.api;

import com.irlab.view.bean.UserResponse;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * ApiService接口 统一管理应用所有的接口
 */
public interface ApiService {

    /**
     * 检查用户名是否被注册
     */
    @GET("/api/getUserByName")
    Observable<UserResponse> checkUser(@Query("userName") String userName);

    /**
     * 添加一个用户
     */
    @POST("/api/addUser")
    Observable<UserResponse> addUser(@Body RequestBody requestBody);

    /**
     * 检查用户信息
     */
    @GET("/api/checkUserInfo")
    Observable<UserResponse> checkUserInfo(@Query("userName") String userName, @Query("password") String password);

    /**
     * 保存对局信息
     */
    @POST("/api/saveGame")
    Observable<UserResponse> saveGame(@Body RequestBody requestBody);
}

