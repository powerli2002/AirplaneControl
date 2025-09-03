package com.example.airplanecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * 开机自启动接收器：系统启动完成后启动飞行模式自动任务服务
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                Log.d(TAG, "接收到 BOOT_COMPLETED，检查飞行模式自动任务设置");

                // 检查飞行模式自动任务设置
                android.content.SharedPreferences airplanePrefs = context.getSharedPreferences("airplane_mode_prefs", Context.MODE_PRIVATE);
                boolean autoToggleEnabled = airplanePrefs.getBoolean("auto_toggle_enabled", false);
                int intervalMinutes = airplanePrefs.getInt("toggle_interval", 15);

                if (autoToggleEnabled) {
                    Intent svc = new Intent(context.getApplicationContext(), com.example.airplanecontrol.services.AutoTaskService.class);
                    svc.putExtra("interval_minutes", intervalMinutes);

                    // Android O+ 使用前台服务启动
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context.getApplicationContext(), svc);
                    } else {
                        context.getApplicationContext().startService(svc);
                    }
                    Log.d(TAG, "已在开机后启动自动飞行模式前台服务，间隔=" + intervalMinutes + " 分钟");
                } else {
                    Log.d(TAG, "开机后检测到自动飞行模式开关未启用，跳过启动");
                }
            } catch (Exception e) {
                Log.e(TAG, "启动飞行模式自动任务服务失败", e);
            }
        }
    }
}


