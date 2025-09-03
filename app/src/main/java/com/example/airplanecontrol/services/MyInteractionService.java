package com.example.airplanecontrol.services;

import android.service.voice.VoiceInteractionService;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;

/**
 * 数字助理交互服务
 * 用于获取系统级权限以控制飞行模式
 */
public class MyInteractionService extends VoiceInteractionService {
    
    private static final String TAG = "MyInteractionService";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceInteractionService 已创建");
    }
    
    @Override
    public void onReady() {
        super.onReady();
        Log.d(TAG, "VoiceInteractionService 已就绪");
    }
    
    // VoiceInteractionService 不需要重写onNewSession方法
    // 会话由系统自动管理，通过interaction_service.xml配置
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VoiceInteractionService 已销毁");
    }
}