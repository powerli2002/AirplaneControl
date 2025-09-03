package com.example.airplanecontrol.services;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;

/**
 * 数字助理会话服务
 * 管理VoiceInteractionSession的生命周期
 */
public class MyInteractionSessionService extends VoiceInteractionSessionService {
    
    private static final String TAG = "MyInteractionSessionService";
    
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        Log.d(TAG, "创建新的 VoiceInteractionSession，参数: " + args);
        if (args != null) {
            String command = args.getString("command");
            Log.d(TAG, "Session参数中的命令: " + command);
        }
        MyInteractionSession session = new MyInteractionSession(this);
        session.setSessionArgs(args);  // 传递参数到session
        return session;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceInteractionSessionService 已创建");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VoiceInteractionSessionService 已销毁");
    }
}
