package com.example.airplanecontrol;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {
    private static final String TAG = "AppLifecycleObserver";
    public static boolean isAppInForeground = false;

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // 应用进入前台
        isAppInForeground = true;
        Log.d(TAG, "应用进入前台");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // 应用进入后台
        isAppInForeground = false;
        Log.d(TAG, "应用进入后台");
    }
}