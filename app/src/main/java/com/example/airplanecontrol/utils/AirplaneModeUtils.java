package com.example.airplanecontrol.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public final class AirplaneModeUtils {

    private static final String TAG = "AirplaneModeUtils";

    private AirplaneModeUtils() {}

    /**
     * 使用 WRITE_SECURE_SETTINGS 切换飞行模式。
     * 针对不同设备厂商的兼容性增强版本。
     */
    public static boolean setAirplaneMode(Context context, boolean enable) {
        try {
            ContentResolver resolver = context.getContentResolver();
            
            // 方法1：直接设置系统设置
            boolean ok = Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
            Log.d(TAG, "putInt AIRPLANE_MODE_ON=" + enable + ", result=" + ok);

            if (ok) {
                // 方法2：发送多种广播通知（针对不同厂商设备）
                sendAirplaneModeBroadcasts(context, enable);
                
                // 方法3：延迟验证并重试（针对某些设备需要时间生效）
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    boolean currentState = isAirplaneModeOn(context);
                    if (currentState != enable) {
                        Log.w(TAG, "飞行模式状态不一致，尝试重新设置。期望: " + enable + ", 实际: " + currentState);
                        // 重试一次
                        boolean retryOk = Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
                        if (retryOk) {
                            sendAirplaneModeBroadcasts(context, enable);
                        }
                        Log.d(TAG, "重试设置飞行模式: " + enable + ", 结果: " + retryOk);
                    }
                }, 1500); // 延迟1.5秒验证
            }

            // Log.d(TAG, "Flight mode set via WSS: " + enable);

            return ok;
        } catch (Throwable t) {
            Log.e(TAG, "setAirplaneMode failed", t);
            return false;
        }
    }

    /**
     * 发送多种飞行模式状态变化广播，兼容不同厂商设备
     */
    private static void sendAirplaneModeBroadcasts(Context context, boolean enable) {
        try {
            // 标准系统广播
            Intent standardIntent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            standardIntent.putExtra("state", enable);
            context.sendBroadcast(standardIntent);
            Log.d(TAG, "发送标准飞行模式广播: " + enable);

            // 某些设备需要的额外广播
            Intent radioIntent = new Intent("android.intent.action.AIRPLANE_MODE");
            radioIntent.putExtra("state", enable);
            context.sendBroadcast(radioIntent);
            Log.d(TAG, "发送额外飞行模式广播: " + enable);

            // 某些厂商设备需要的特定广播
            Intent vendorIntent = new Intent("com.android.internal.intent.action.AIRPLANE_MODE");
            vendorIntent.putExtra("state", enable);
            context.sendBroadcast(vendorIntent);
            Log.d(TAG, "发送厂商特定广播: " + enable);

            // 针对特定厂商的广播（如小米、华为等）
            String[] vendorActions = {
                "com.miui.intent.action.AIRPLANE_MODE",
                "com.huawei.intent.action.AIRPLANE_MODE",
                "com.samsung.intent.action.AIRPLANE_MODE",
                "com.oppo.intent.action.AIRPLANE_MODE",
                "com.vivo.intent.action.AIRPLANE_MODE"
            };

            for (String action : vendorActions) {
                try {
                    Intent intent = new Intent(action);
                    intent.putExtra("state", enable);
                    context.sendBroadcast(intent);
                    Log.d(TAG, "发送厂商广播 " + action + ": " + enable);
                } catch (Exception e) {
                    // 忽略某些厂商广播可能不存在的异常
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "发送飞行模式广播失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取当前飞行模式状态。
     */
    public static boolean isAirplaneModeOn(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        } catch (Throwable t) {
            Log.e(TAG, "isAirplaneModeOn failed", t);
            return false;
        }
    }

    /**
     * 获取设备信息和飞行模式相关状态（用于调试）
     */
    public static String getDeviceInfo(Context context) {
        StringBuilder info = new StringBuilder();
        try {
            info.append("设备信息:\n");
            info.append("厂商: ").append(android.os.Build.MANUFACTURER).append("\n");
            info.append("型号: ").append(android.os.Build.MODEL).append("\n");
            info.append("Android版本: ").append(android.os.Build.VERSION.RELEASE).append("\n");
            info.append("API级别: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
            
            info.append("\n飞行模式状态:\n");
            boolean isOn = isAirplaneModeOn(context);
            info.append("当前状态: ").append(isOn ? "开启" : "关闭").append("\n");
            
            // 检查其他相关设置
            ContentResolver resolver = context.getContentResolver();
            try {
                int wifiOn = Settings.Global.getInt(resolver, Settings.Global.WIFI_ON, 0);
                info.append("WiFi状态: ").append(wifiOn == 1 ? "开启" : "关闭").append("\n");
            } catch (Exception e) {
                info.append("WiFi状态: 无法读取\n");
            }
            
            try {
                int bluetoothOn = Settings.Global.getInt(resolver, Settings.Global.BLUETOOTH_ON, 0);
                info.append("蓝牙状态: ").append(bluetoothOn == 1 ? "开启" : "关闭").append("\n");
            } catch (Exception e) {
                info.append("蓝牙状态: 无法读取\n");
            }
            
            try {
                // 移动数据状态在不同Android版本中可能使用不同的常量
                int mobileDataOn = Settings.Global.getInt(resolver, "mobile_data", 0);
                info.append("移动数据: ").append(mobileDataOn == 1 ? "开启" : "关闭").append("\n");
            } catch (Exception e) {
                info.append("移动数据: 无法读取\n");
            }
            
        } catch (Exception e) {
            info.append("获取设备信息失败: ").append(e.getMessage());
        }
        return info.toString();
    }

    /**
     * 强制刷新飞行模式状态（某些设备需要）
     */
    public static void forceRefreshAirplaneMode(Context context) {
        try {
            // 读取当前状态
            boolean currentState = isAirplaneModeOn(context);
            Log.d(TAG, "强制刷新前状态: " + currentState);
            
            // 短暂切换状态再恢复
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, currentState ? 0 : 1);
            Thread.sleep(100); // 短暂等待
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, currentState ? 1 : 0);
            
            // 发送广播
            sendAirplaneModeBroadcasts(context, currentState);
            
            Log.d(TAG, "强制刷新完成，状态: " + currentState);
        } catch (Exception e) {
            Log.e(TAG, "强制刷新飞行模式失败: " + e.getMessage(), e);
        }
    }
}