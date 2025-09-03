package com.example.airplanecontrol.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.airplanecontrol.AppLifecycleObserver;
import com.example.airplanecontrol.R;
import com.example.airplanecontrol.ui.AirplaneModeActivity;
import com.example.airplanecontrol.ui.TransparentActivity;
import com.example.airplanecontrol.utils.AirplaneModeUtils;

import android.content.SharedPreferences;
import android.content.Context;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.core.content.ContextCompat;

public class AutoTaskService extends Service {

    private static final String TAG = "AutoTaskService";
    private static final String CHANNEL_ID = "auto_task_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timedToggleRunnable;
    private boolean isTaskRunning = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AutoTaskService启动");
        // 处理一次性定时切换命令
        if (intent != null && intent.hasExtra("command")) {
            String command = intent.getStringExtra("command");
            if ("timed_toggle".equals(command)) {
                handleTimedToggleCommand(intent);
                return START_NOT_STICKY; // 一次性命令，不需要重启
            }
        }

        if (isTaskRunning) {
            Log.d(TAG, "服务已在运行，忽略新的启动命令，但会更新间隔");
        }

        // 读取偏好：控制方式与间隔
        SharedPreferences prefs = getSharedPreferences("airplane_mode_prefs", Context.MODE_PRIVATE);
        final boolean useSecure = prefs.getBoolean("control_mode_secure", false);
        int intervalMinutes = prefs.getInt("toggle_interval", 15);
        if (intent != null && intent.hasExtra("interval_minutes")) {
            intervalMinutes = intent.getIntExtra("interval_minutes", intervalMinutes);
        }
        final long intervalMillis = intervalMinutes * 60L * 1000L;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification(intervalMinutes));

        Log.d(TAG, "前台服务启动/更新，切换间隔: " + intervalMinutes + " 分钟");
        isTaskRunning = true;

        final int secureWaitMs = 2000; // Secure方式下开->等待->关
        // 若已存在旧任务，先移除，保证新间隔立即生效
        if (timedToggleRunnable != null) {
            handler.removeCallbacks(timedToggleRunnable);
        }
        timedToggleRunnable = new Runnable() {
            @Override
            public void run() {
                // 每次触发时重新读取控制方式，保证切换后立即生效
                boolean currentUseSecure;
                try {
                    SharedPreferences p = getSharedPreferences("airplane_mode_prefs", Context.MODE_PRIVATE);
                    currentUseSecure = p.getBoolean("control_mode_secure", false);
                } catch (Throwable t) {
                    currentUseSecure = useSecure;
                }

                Log.d(TAG, "执行一次定时切换, mode=" + (currentUseSecure ? "SECURE" : "ASSISTANT"));
                if (currentUseSecure) {
                    try {
                        boolean before = AirplaneModeUtils.isAirplaneModeOn(AutoTaskService.this);
                        boolean okOn = AirplaneModeUtils.setAirplaneMode(AutoTaskService.this, true);
                        Log.d(TAG, "WSS set ON result=" + okOn + ", before=" + before + ", after=" + AirplaneModeUtils.isAirplaneModeOn(AutoTaskService.this));
                        handler.postDelayed(() -> {
                            boolean okOff = AirplaneModeUtils.setAirplaneMode(AutoTaskService.this, false);
                            Log.d(TAG, "WSS set OFF result=" + okOff + ", after=" + AirplaneModeUtils.isAirplaneModeOn(AutoTaskService.this));
                        }, secureWaitMs);
                    } catch (Throwable t) {
                        Log.e(TAG, "Secure Settings 切换失败", t);
                    }
                } else {
                    triggerShowAssist("timed_toggle");
                }
                handler.postDelayed(this, intervalMillis);
            }
        };

        handler.post(timedToggleRunnable);
        return START_STICKY;
    }

    /**
     * 处理一次性定时切换命令（开启→等待2秒→关闭）
     */
    private void handleTimedToggleCommand(Intent intent) {
        Log.d(TAG, "处理一次性定时切换命令");
        
        String mode = intent.getStringExtra("mode");
        boolean useSecure = "secure".equals(mode);
        
        // 如果没有指定模式，从偏好设置读取
        if (mode == null) {
            SharedPreferences prefs = getSharedPreferences("airplane_mode_prefs", Context.MODE_PRIVATE);
            useSecure = prefs.getBoolean("control_mode_secure", false);
        }
        
        Log.d(TAG, "定时切换模式: " + (useSecure ? "SECURE" : "ASSISTANT"));
        
        if (useSecure) {
            executeSecureTimedToggle();
        } else {
            executeAssistantTimedToggle();
        }
    }
    
    /**
     * 执行Secure模式的定时切换（开启→等待2秒→关闭）
     */
    private void executeSecureTimedToggle() {
        try {
            Log.d(TAG, "开始Secure模式定时切换");
            
            // 1. 开启飞行模式
            boolean before = AirplaneModeUtils.isAirplaneModeOn(this);
            boolean okOn = AirplaneModeUtils.setAirplaneMode(this, true);
            
            Log.d(TAG, "Secure ON result=" + okOn + ", before=" + before);
            
            // 2. 等待2秒
            handler.postDelayed(() -> {
                try {
                    // 3. 关闭飞行模式
                    boolean okOff = AirplaneModeUtils.setAirplaneMode(this, false);
                    Log.d(TAG, "Secure OFF result=" + okOff + ", after=" + AirplaneModeUtils.isAirplaneModeOn(this));
                } catch (Throwable t) {
                    Log.e(TAG, "Secure模式关闭失败", t);
                }
            }, 2000L);
            
        } catch (Throwable t) {
            Log.e(TAG, "Secure模式定时切换失败", t);
        }
    }
    
    /**
     * 执行Assistant模式的定时切换（开启→等待2秒→关闭）
     */
    private void executeAssistantTimedToggle() {
        Log.d(TAG, "开始Assistant模式定时切换");
        
        // 1. 第一次调用showAssist（开启）
        triggerShowAssist("smart_toggle");
        
        // 2. 等待2秒后第二次调用showAssist（关闭）
        handler.postDelayed(() -> {
            Log.d(TAG, "2秒已过，执行第二次切换（关闭）");
            triggerShowAssist("smart_toggle");
        }, 2000L);
    }

    private void triggerShowAssist(String command) {
        Log.d(TAG, "从自动任务启动activity");
        Intent intent = new Intent(this, TransparentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("command", command);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timedToggleRunnable != null) {
            handler.removeCallbacks(timedToggleRunnable);
        }
        isTaskRunning = false;
        Log.d(TAG, "前台服务已销毁");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "自动任务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("飞行模式自动定时切换");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(int interval) {
        boolean secGranted = hasWriteSecureSettingsPermission();
        Intent notificationIntent = new Intent(this, AirplaneModeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("飞行模式自动任务")
                .setContentText("每 " + interval + " 分钟切换一次 · WSS权限:" + (secGranted ? "已授权" : "未授权"))
                .setSmallIcon(R.drawable.ic_developer_mode)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    /**
     * 检查WRITE_SECURE_SETTINGS权限状态
     */
    private boolean hasWriteSecureSettingsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) 
               == PackageManager.PERMISSION_GRANTED;
    }
}



