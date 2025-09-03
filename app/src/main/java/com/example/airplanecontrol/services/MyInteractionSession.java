package com.example.airplanecontrol.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * 数字助理交互会话
 * 处理飞行模式控制命令
 */
public class MyInteractionSession extends VoiceInteractionSession {
    
    private static final String TAG = "MyInteractionSession";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Bundle lastSessionArgs; // 存储最后的session参数
    
    public MyInteractionSession(Context context) {
        super(context);
        Log.d(TAG, "MyInteractionSession 已创建");
    }
    
    /**
     * 设置session参数
     */
    public void setSessionArgs(Bundle args) {
        this.lastSessionArgs = args;
        if (args != null) {
            String command = args.getString("command");
            Log.d(TAG, "设置Session参数，命令: " + command);
        } else {
            Log.d(TAG, "设置Session参数为null");
        }
    }
    
    @Override
    public void onHandleAssist(@NonNull AssistState state) {
        super.onHandleAssist(state);
        Log.d(TAG, "onHandleAssist 被调用！");
        
        // 尝试从多个来源获取Bundle参数
        Bundle args = state.getAssistData();
        String command = null;
        
        if (args != null) {
            command = args.getString("command");
            Log.d(TAG, "从AssistData获取到命令: " + command);
        }
        
        // 如果AssistData为空，尝试其他方式
        if (command == null) {
            // 检查是否有其他Bundle来源
            if (lastSessionArgs != null) {
                command = lastSessionArgs.getString("command");
                Log.d(TAG, "从SessionArgs获取到命令: " + command);
            }
        }
        Log.d(TAG,"session 中的 command："+ command);
        // 如果仍然没有命令，默认执行智能切换
        if (command == null) {
            Log.w(TAG, "未获取到命令参数，默认执行智能切换");
            command = "timed_toggle";
        }
        
        Log.d(TAG, "最终执行命令: " + command);
        
        // 根据命令分发任务
        switch (command) {
            case "timed_toggle":
                // 定时切换: 开启 -> 等待2秒 -> 关闭
                executeTimedToggle();
                break;
            case "smart_toggle":
                // 智能切换: 检查状态，开->关，关->开
                executeSmartToggle();
                break;
            case "turn_on":
                // 直接开启飞行模式
                sendAirplaneModeIntent(true);
                break;
            case "turn_off":
                // 直接关闭飞行模式
                sendAirplaneModeIntent(false);
                break;
            default:
                Log.w(TAG, "未知命令: " + command);
                break;
        }
    }
    
    /**
     * 执行定时切换任务: 开启 -> 等待2秒 -> 关闭
     */
    private void executeTimedToggle() {
        Log.d(TAG, "执行定时切换任务...");
        
        // 1. 立即开启飞行模式
        sendAirplaneModeIntent(true);
        Log.d(TAG, "飞行模式已开启");
        
        // 2. 使用 Handler 延迟2秒后执行关闭操作
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "2秒已过，关闭飞行模式...");
            sendAirplaneModeIntent(false);
        }, 2000); // 2000毫秒 = 2秒
    }
    
    /**
     * 执行智能切换任务: 检查当前状态并执行相反操作
     */
    private void executeSmartToggle() {
        Log.d(TAG, "执行智能切换任务...");
        
        // 1. 检查当前飞行模式状态
        boolean isCurrentlyOn = isAirplaneModeOn(getContext());
        Log.d(TAG, "当前飞行模式状态: " + (isCurrentlyOn ? "开启" : "关闭"));
        
        // 2. 执行相反的操作
        sendAirplaneModeIntent(!isCurrentlyOn);
        Log.d(TAG, "切换飞行模式到: " + (!isCurrentlyOn ? "开启" : "关闭"));
//        finish();
    }
    
    /**
     * 发送控制飞行模式的Intent
     * @param enable true 开启，false 关闭
     */
    private void sendAirplaneModeIntent(boolean enable) {
        try {
            Intent intent = new Intent("android.settings.VOICE_CONTROL_AIRPLANE_MODE");
            intent.putExtra("airplane_mode_enabled", enable);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startVoiceActivity(intent);
            Log.i(TAG, "已发送飞行模式控制Intent: " + (enable ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "发送飞行模式控制Intent失败", e);
        }
    }
    
    /**
     * 检查飞行模式是否开启
     * @param context 上下文
     * @return true 如果飞行模式开启，false 否则
     */
    private boolean isAirplaneModeOn(Context context) {
        try {
            // 1 表示开启, 0 表示关闭
            int airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);
            return airplaneMode != 0;
        } catch (Exception e) {
            Log.e(TAG, "检查飞行模式状态失败", e);
            return false;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceInteractionSession onCreate");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VoiceInteractionSession onDestroy");
    }
}
