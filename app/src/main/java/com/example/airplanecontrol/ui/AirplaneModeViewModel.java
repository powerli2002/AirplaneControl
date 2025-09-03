package com.example.airplanecontrol.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 飞行模式控制界面的ViewModel
 * 简化版本，只包含必要的功能
 */
public class AirplaneModeViewModel extends AndroidViewModel {
    
    private static final String TAG = "AirplaneModeViewModel";
    private static final String PREFS_NAME = "airplane_mode_prefs";
    private static final String KEY_AUTO_TOGGLE_ENABLED = "auto_toggle_enabled";
    private static final String KEY_TOGGLE_INTERVAL = "toggle_interval";
    private static final String KEY_CONTROL_MODE_SECURE = "control_mode_secure";
    private static final String KEY_OPERATION_LOGS = "operation_logs";
    
    private final Context context;
    private final SharedPreferences prefs;
    
    // LiveData
    private final MutableLiveData<Boolean> airplaneModeStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> autoToggleEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> toggleInterval = new MutableLiveData<>();
    private final MutableLiveData<Boolean> permissionStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public AirplaneModeViewModel(@NonNull Application application) {
        super(application);
        this.context = application.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void initialize() {
        Log.d(TAG, "ViewModel初始化");
        refreshStatus();
    }
    
    public void refreshStatus() {
        refreshAirplaneModeStatus();
        checkPermissionStatus();
        
        // 从SharedPreferences加载设置
        autoToggleEnabled.setValue(prefs.getBoolean(KEY_AUTO_TOGGLE_ENABLED, false));
        toggleInterval.setValue(prefs.getInt(KEY_TOGGLE_INTERVAL, 15));
    }
    
    private void refreshAirplaneModeStatus() {
        try {
            boolean isOn = Settings.Global.getInt(context.getContentResolver(), 
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
            airplaneModeStatus.setValue(isOn);
            Log.d(TAG, "飞行模式状态: " + (isOn ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "获取飞行模式状态失败", e);
            airplaneModeStatus.setValue(false);
        }
    }
    
    private void checkPermissionStatus() {
        // 简化版本：检查数字助理权限
        boolean hasPermission = isVoiceInteractionServiceEnabled();
        permissionStatus.setValue(hasPermission);
        Log.d(TAG, "数字助理权限状态: " + hasPermission);
    }
    
    private boolean isVoiceInteractionServiceEnabled() {
        try {
            // 使用字符串常量，因为VOICE_INTERACTION_SERVICE在某些API级别可能不可用
            String currentService = Settings.Secure.getString(
                context.getContentResolver(), 
                "voice_interaction_service"
            );
            String packageName = context.getPackageName();
            boolean isEnabled = currentService != null && currentService.startsWith(packageName);
            Log.d(TAG, "当前数字助理服务: " + currentService + ", 是否启用: " + isEnabled);
            return isEnabled;
        } catch (Exception e) {
            Log.e(TAG, "检查数字助理权限失败", e);
            return false;
        }
    }
    
    public boolean isAirplaneModeOn() {
        try {
            return Settings.Global.getInt(context.getContentResolver(), 
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        } catch (Exception e) {
            Log.e(TAG, "检查飞行模式状态失败", e);
            return false;
        }
    }
    
    public void setAutoToggleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_TOGGLE_ENABLED, enabled).apply();
        autoToggleEnabled.setValue(enabled);
        Log.d(TAG, "自动切换设置: " + enabled);
    }
    
    public void setToggleInterval(int intervalMinutes) {
        if (isValidInterval(intervalMinutes)) {
            prefs.edit().putInt(KEY_TOGGLE_INTERVAL, intervalMinutes).apply();
            toggleInterval.setValue(intervalMinutes);
            Log.d(TAG, "切换间隔设置: " + intervalMinutes + " 分钟");
        }
    }
    
    public void setControlModeSecure(boolean secure) {
        prefs.edit().putBoolean(KEY_CONTROL_MODE_SECURE, secure).apply();
    }
    
    public boolean isControlModeSecure() {
        return prefs.getBoolean(KEY_CONTROL_MODE_SECURE, false);
    }
    
    public boolean isValidInterval(int intervalMinutes) {
        return intervalMinutes >= 1 && intervalMinutes <= 60;
    }
    
    public void executeSmartToggle() {
        Log.d(TAG, "执行智能切换");
        addOperationLog("执行智能切换");
    }
    
    public void executeTimedToggle() {
        Log.d(TAG, "执行定时切换");
        addOperationLog("执行定时切换");
    }
    
    public void executeTurnOn() {
        Log.d(TAG, "执行开启飞行模式");
        addOperationLog("执行开启飞行模式");
    }
    
    public void executeTurnOff() {
        Log.d(TAG, "执行关闭飞行模式");
        addOperationLog("执行关闭飞行模式");
    }
    
    private void addOperationLog(String operation) {
        String timestamp = java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
        String log = prefs.getString(KEY_OPERATION_LOGS, "");
        log = timestamp + ": " + operation + "\n" + log;
        prefs.edit().putString(KEY_OPERATION_LOGS, log).apply();
    }
    
    public String getOperationLogs() {
        return prefs.getString(KEY_OPERATION_LOGS, "暂无操作记录");
    }
    
    public void clearOperationLogs() {
        prefs.edit().remove(KEY_OPERATION_LOGS).apply();
    }
    
    // Getters
    public LiveData<Boolean> getAirplaneModeStatus() { return airplaneModeStatus; }
    public LiveData<Boolean> getAutoToggleEnabled() { return autoToggleEnabled; }
    public LiveData<Integer> getToggleInterval() { return toggleInterval; }
    public LiveData<Boolean> getPermissionStatus() { return permissionStatus; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
