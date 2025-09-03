package com.example.airplanecontrol;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.ProcessLifecycleOwner;

/**
 * 飞行模式控制应用主类
 * 负责应用的基本初始化工作
 */
public class AirplaneControlApplication extends Application {

    private static final String TAG = "AirplaneControl";

    @Override
    public void onCreate() {
        super.onCreate();

        // 注册应用生命周期观察者
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());

        Log.d(TAG, "飞行模式控制应用初始化完成");
    }
} 