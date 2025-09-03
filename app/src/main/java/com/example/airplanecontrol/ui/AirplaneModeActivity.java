package com.example.airplanecontrol.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.airplanecontrol.AppLifecycleObserver;
import com.example.airplanecontrol.R;
import com.example.airplanecontrol.ui.AirplaneModeViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.airplanecontrol.services.AutoTaskService;

/**
 * 飞行模式控制界面
 * 提供定时切换飞行模式功能和手动测试功能
 */
public class AirplaneModeActivity extends AppCompatActivity {
    
    private static final String TAG = "AirplaneModeActivity";
    
    private AirplaneModeViewModel viewModel;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // UI组件
    private TextView tvCurrentStatus;
    private TextView tvIntervalStatus;
    private TextView tvPermissionStatus;
    private TextView tvSecurePermStatus;
    private SwitchMaterial switchAutoToggle;
    private Button btnTestToggle;
    private Button btnSetInterval;

    private Button btnPermissionGuide;
    private Button btnViewLogs;
    private EditText etInterval;
    private RadioGroup rgControlMode;
    private RadioButton rbModeAssistant;
    private RadioButton rbModeSecure;
    private boolean suppressToggleCallback = false; // 防抖：避免观察者回写触发监听
    private ProgressBar progressBar;
    private MaterialCardView cardStatus;
    private MaterialCardView cardControl;
    private MaterialCardView cardPermission;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_airplane_mode);
        
        // 设置窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // 初始化UI
        initializeViews();
        setupToolbar();
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(AirplaneModeViewModel.class);
        
        // 设置观察者
        setupObservers();
        
        // 设置点击事件
        setupClickListeners();
        
        // 初始化数据
        viewModel.initialize();
        // 与持久化数据同步UI
        syncUiFromPrefs();
        // 刷新显示 WRITE_SECURE_SETTINGS 权限状态
        refreshSecurePermissionStatus();
        
        // 服务方案不再使用Worker派发命令
    }
    
    /**
     * 初始化视图组件
     */
    private void initializeViews() {
        tvCurrentStatus = findViewById(R.id.tv_current_status);
        tvIntervalStatus = findViewById(R.id.tv_interval_status);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        // may be null if old layout; guarded usage
        try { tvSecurePermStatus = findViewById(R.id.tv_secure_perm_status); } catch (Throwable ignore) {}
        switchAutoToggle = findViewById(R.id.switch_auto_toggle);
        btnTestToggle = findViewById(R.id.btn_test_toggle);
        btnSetInterval = findViewById(R.id.btn_set_interval);

        btnPermissionGuide = findViewById(R.id.btn_permission_guide);
        btnViewLogs = findViewById(R.id.btn_view_logs);
        progressBar = findViewById(R.id.progress_bar);
        cardStatus = findViewById(R.id.card_status);
        cardControl = findViewById(R.id.card_control);
        cardPermission = findViewById(R.id.card_permission);
        etInterval = findViewById(R.id.et_interval);
        rgControlMode = findViewById(R.id.rg_control_mode);
        rbModeAssistant = findViewById(R.id.rb_mode_assistant);
        rbModeSecure = findViewById(R.id.rb_mode_secure);
    }
    
    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("飞行模式控制");
    }
    
    /**
     * 设置观察者
     */
    private void setupObservers() {
        // 观察飞行模式状态
        viewModel.getAirplaneModeStatus().observe(this, status -> {
            tvCurrentStatus.setText("当前状态: " + (status ? "飞行模式开启" : "飞行模式关闭"));
        });
        
        // 观察自动切换状态（带防抖）
        viewModel.getAutoToggleEnabled().observe(this, enabled -> {
            boolean checked = enabled != null && enabled;
            if (switchAutoToggle.isChecked() != checked) {
                suppressToggleCallback = true;
                switchAutoToggle.setChecked(checked);
                suppressToggleCallback = false;
            }
            Log.d(TAG, "observer set auto-switch to: " + checked + ", suppress=" + suppressToggleCallback);
        });
        
        // 观察切换间隔
        viewModel.getToggleInterval().observe(this, interval -> {
            tvIntervalStatus.setText("Toggle Interval: Every " + interval + " minutes");
        });
        
        // 观察权限状态
        viewModel.getPermissionStatus().observe(this, hasPermission -> {
            String statusText = hasPermission ? "Digital Assistant Permission Configured" : "Digital Assistant Permission Required";
            tvPermissionStatus.setText(statusText);
            // 根据当前控制方式与权限动态控制可用性
            updateControlsEnabled(hasPermission);
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            updateControlsEnabled(Boolean.TRUE.equals(viewModel.getPermissionStatus().getValue()));
        });
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        // 自动切换开关
        switchAutoToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressToggleCallback) return;
            if (buttonView.isPressed()) { // 只在用户操作时响应
                viewModel.setAutoToggleEnabled(isChecked);
                Log.d(TAG, "user toggled auto-switch: " + isChecked + ", mode=" + (viewModel.isControlModeSecure()?"SECURE":"ASSISTANT"));
                if (isChecked) startAutoTaskService(); else stopAutoTaskService();
            }
        });

        // 控制方式选择与持久化
        rgControlMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean secure = checkedId == R.id.rb_mode_secure;
            viewModel.setControlModeSecure(secure);
            // 切换模式时根据权限刷新控件可用状态
            Boolean hasAssistant = viewModel.getPermissionStatus().getValue();
            updateControlsEnabled(hasAssistant != null && hasAssistant);
        });

        // 间隔输入持久化（点击“设置间隔”按钮）
        btnSetInterval.setOnClickListener(v -> {
            String input = etInterval.getText() != null ? etInterval.getText().toString().trim() : "";
            if (input.isEmpty()) {
                Toast.makeText(this, "Please enter interval (1-60)", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int val = Integer.parseInt(input);
                if (viewModel.isValidInterval(val)) {
                    viewModel.setToggleInterval(val);
                    Toast.makeText(this, "Interval set to " + val + " minutes", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "用户确认设置间隔: " + val + " 分钟, 尝试启动AutoTaskService");
                    // 立即启动/重启前台服务，使新的间隔立刻生效
                    startAutoTaskService();
                } else {
                    Toast.makeText(this, "Interval range 1-60", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        // 长按测试按钮显示设备信息
        btnTestToggle.setOnLongClickListener(v -> {
            showDeviceInfoDialog();
            return true;
        });
        
        // 统一“测试切换”按钮：根据模式选择
        btnTestToggle.setOnClickListener(v -> {
            boolean secure = viewModel.isControlModeSecure();
            if (secure) {
                testSecureToggleOnce();
            } else {
                executeSmartToggleWithAssist();
            }
        });
        
        // 注意：已在上方为 btnSetInterval 绑定了保存并启动服务的逻辑，这里不再重复覆盖监听
        
        // Permission setup button
        btnPermissionGuide.setOnClickListener(v -> {
            openVoiceInteractionSettings();
        });
        
        // Long press permission button to refresh status
        btnPermissionGuide.setOnLongClickListener(v -> {
            viewModel.refreshStatus();
            Toast.makeText(this, "Permission status refreshed", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // 查看日志按钮
        btnViewLogs.setOnClickListener(v -> {
            showOperationLogsDialog();
        });
        

        
        // 长按状态卡片刷新状态
        cardStatus.setOnLongClickListener(v -> {
            viewModel.refreshStatus();
            Toast.makeText(this, "Status refreshed", Toast.LENGTH_SHORT).show();
            return true;
        });
    }
    
    /**
     * 显示间隔设置对话框
     */
    private void showIntervalSettingDialog() {
        Integer currentInterval = viewModel.getToggleInterval().getValue();
        int defaultInterval = currentInterval != null ? currentInterval : 15;
        
        // 创建输入框
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setText(String.valueOf(defaultInterval));
        editText.setSelectAllOnFocus(true);
        editText.setHint("Enter a number between 1-60");
        
        // 设置输入框的布局参数
        editText.setPadding(50, 40, 50, 40);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Set Toggle Interval")
                .setMessage("Enter auto toggle interval time (minutes)\nRange: 1 - 60 minutes\n\nToggle logic: ON airplane mode → wait 2s → OFF airplane mode")
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    String input = editText.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Please enter a valid time interval", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    try {
                        int newInterval = Integer.parseInt(input);
                        if (viewModel.isValidInterval(newInterval)) {
                            viewModel.setToggleInterval(newInterval);
                            Toast.makeText(this, "Toggle interval set to " + newInterval + " minutes", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, 
                                "Interval time out of range\nPlease enter a number between 1 - 60 minutes", 
                                Toast.LENGTH_LONG).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Common Settings", (dialog, which) -> {
                    showQuickIntervalDialog();
                })
                .show();
    }
    
    /**
     * 显示快速间隔设置对话框
     */
    private void showQuickIntervalDialog() {
        String[] quickOptions = {"1 minute", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "60 minutes"};
        int[] quickValues = {1, 5, 10, 15, 30, 60};
        
        Integer currentInterval = viewModel.getToggleInterval().getValue();
        int selectedIndex = 3; // 默认选择15分钟
        
        if (currentInterval != null) {
            for (int i = 0; i < quickValues.length; i++) {
                if (quickValues[i] == currentInterval) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Common Interval Settings")
                .setSingleChoiceItems(quickOptions, selectedIndex, (dialog, which) -> {
                    int newInterval = quickValues[which];
                    viewModel.setToggleInterval(newInterval);
                    dialog.dismiss();
                    
                    Toast.makeText(this, "Toggle interval set to " + quickOptions[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Custom", (dialog, which) -> {
                    showIntervalSettingDialog();
                })
                .show();
    }
    
    /**
     * 显示权限指导对话框
     */
    private void showPermissionGuideDialog() {
                String guide = "Digital Assistant Permission Setup Guide\n\n" +
                "Setup Steps:\n" +
                "1. Click the [Open Assistant Settings] button below\n" +
                "2. Find [AirplaneControl Assistant] in the settings page\n" +
                "3. Select [AirplaneControl Assistant] as the default assistant\n" +
                "4. Return to the app to check permission status\n\n" +

                "If you can't find our app:\n" +
                "• Make sure the app is properly installed\n" +
                "• Try manually going to [System Settings] → [Apps] → [Default Apps] → [Digital Assistant App]\n" +
                "• Restart the app and try again\n\n" +

                "Important Notes:\n" +
                "• This permission is used to control system airplane mode\n" +
                "• Setup paths may vary between different phone brands\n" +
                "• Auto toggle function requires this permission to work\n" +
                "• Some phones may not support this feature\n\n" +

                "Testing Method:\n" +
                "After setup, click the [Test Toggle] button to verify functionality";
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Setup Guide")
                .setMessage(guide)
                .setPositiveButton("Got it", null)
                .setNeutralButton("Open Assistant Settings", (dialog, which) -> {
                    openVoiceInteractionSettings();
                })
                .setNegativeButton("Open App Settings", (dialog, which) -> {
                    openApplicationSettings();
                })
                .show();
    }
    
    /**
     * 打开语音交互设置页面
     */
    private void openVoiceInteractionSettings() {
        try {
            // 尝试打开语音交互设置
            Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "无法打开语音交互设置", e);
            try {
                // 备选方案：打开默认应用设置
                Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "无法打开默认应用设置", ex);
                openApplicationSettings();
            }
        }
    }
    
    /**
     * 打开应用设置页面
     */
    private void openApplicationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "无法打开应用设置", e);
            Toast.makeText(this, "无法打开设置页面，请手动前往：系统设置 → 应用 → 默认应用 → 数字助理应用", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 显示详细测试选项
     */
    private void showDetailedTestOptions() {
        String[] options = {
            "智能切换（检查状态后切换）",
            "强制开启飞行模式",
            "强制关闭飞行模式",
            "定时切换（开启→2秒→关闭）"
        };
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("测试选项")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            executeSmartToggleWithAssist();
                            break;
                        case 1:
                            executeTurnOnWithAssist();
                            break;
                        case 2:
                            executeTurnOffWithAssist();
                            break;
                        case 3:
                            executeTimedToggleWithAssist();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示操作日志对话框
     */
    private void showOperationLogsDialog() {
        String logs = viewModel.getOperationLogs();
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Operation Logs")
                .setMessage(logs.isEmpty() ? "No operation records" : logs)
                .setPositiveButton("OK", null)
                .setNeutralButton("Clear Logs", (dialog, which) -> {
                    viewModel.clearOperationLogs();
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 仅在需要时刷新状态，避免不必要的性能开销
        // 状态刷新主要通过用户手动操作触发
    }
    
    // 服务方案移除了 handleAutoCommand()
    
    /**
     * 执行智能切换（使用showAssist）
     */
    private void executeSmartToggleWithAssist() {
        Log.d(TAG, "执行智能切换");
        Log.d(TAG, "生命周期检查-isAppInForeground：" + AppLifecycleObserver.isAppInForeground);
        progressBar.setVisibility(View.VISIBLE);
        btnTestToggle.setEnabled(false);
        
        Bundle args = new Bundle();
        args.putString("command", "smart_toggle");
        showAssist(args);
        
        // 延迟刷新状态和重新启用按钮
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            viewModel.refreshStatus();
            progressBar.setVisibility(View.GONE);
            btnTestToggle.setEnabled(true);
        }, 1000);
    }
    
    /**
     * 执行定时切换（委托给后台服务处理）
     */
    private void executeTimedToggleWithAssist() {
        Log.d(TAG, "执行定时切换 - 委托给后台服务");
        progressBar.setVisibility(View.VISIBLE);
        btnTestToggle.setEnabled(false);
        
        Toast.makeText(this, "Starting timed toggle: ON → 2s → OFF", Toast.LENGTH_SHORT).show();

        // 发送命令给后台服务，由服务处理复杂的定时逻辑
        Intent serviceIntent = new Intent(this, AutoTaskService.class);
        serviceIntent.putExtra("command", "timed_toggle");
        serviceIntent.putExtra("mode", "assistant");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 延迟恢复UI状态
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            viewModel.refreshStatus();
            progressBar.setVisibility(View.GONE);
            btnTestToggle.setEnabled(true);
            Toast.makeText(this, "Timed toggle command sent", Toast.LENGTH_SHORT).show();
        }, 1000);
    }
    
    /**
     * 强制开启飞行模式（使用showAssist）
     */
    private void executeTurnOnWithAssist() {
        Log.d(TAG, "强制开启飞行模式");
        progressBar.setVisibility(View.VISIBLE);
        btnTestToggle.setEnabled(false);
        
        Bundle args = new Bundle();
        args.putString("command", "turn_on");
        showAssist(args);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            viewModel.refreshStatus();
            progressBar.setVisibility(View.GONE);
            btnTestToggle.setEnabled(true);
        }, 1000);
    }
    
    /**
     * 强制关闭飞行模式（使用showAssist）
     */
    private void executeTurnOffWithAssist() {
        Log.d(TAG, "强制关闭飞行模式");
        progressBar.setVisibility(View.VISIBLE);
        btnTestToggle.setEnabled(false);
        
        Bundle args = new Bundle();
        args.putString("command", "turn_off");
        showAssist(args);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            viewModel.refreshStatus();
            progressBar.setVisibility(View.GONE);
            btnTestToggle.setEnabled(true);
        }, 1000);
    }

    private void syncUiFromPrefs() {
        // 控制方式
        boolean secure = viewModel.isControlModeSecure();
        if (secure) {
            rbModeSecure.setChecked(true);
        } else {
            rbModeAssistant.setChecked(true);
        }
        // 间隔
        Integer currentInterval = viewModel.getToggleInterval().getValue();
        if (currentInterval != null) {
            etInterval.setText(String.valueOf(currentInterval));
        }
    }

    /**
     * 启动后台自动任务服务
     */
    private void startAutoTaskService() {
        boolean secure = viewModel.isControlModeSecure();
        boolean assistantPerm = Boolean.TRUE.equals(viewModel.getPermissionStatus().getValue());
        boolean wssGranted = hasWriteSecureSettingsPermission();

        // 按模式检查权限
        if (secure && !wssGranted) {
            Toast.makeText(this, "WSS not authorized, cannot start background task", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "startAutoTaskService blocked: mode=SECURE, wssGranted=false");
            suppressToggleCallback = true; switchAutoToggle.setChecked(false); suppressToggleCallback = false;
            return;
        }
        if (!secure && !assistantPerm) {
            Toast.makeText(this, "Digital assistant permission not configured, cannot start background task", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "startAutoTaskService blocked: mode=ASSISTANT, assistantPerm=false");
            suppressToggleCallback = true; switchAutoToggle.setChecked(false); suppressToggleCallback = false;
            return;
        }

        // Android 13+ 需要前台通知权限，否则前台服务会崩溃
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
                Toast.makeText(this, "Please allow notification permission first, then try again", Toast.LENGTH_SHORT).show();
                suppressToggleCallback = true; switchAutoToggle.setChecked(false); suppressToggleCallback = false;
                return;
            }
        }
        Intent serviceIntent = new Intent(this, AutoTaskService.class);
        Integer interval = viewModel.getToggleInterval().getValue();
        serviceIntent.putExtra("interval_minutes", interval != null ? interval : 15);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Background auto task started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "startAutoTaskService, mode=" + (secure?"SECURE":"ASSISTANT") + ", wssGranted=" + wssGranted + ", assistantPerm=" + assistantPerm);
    }

    /**
     * 停止后台自动任务服务
     */
    private void stopAutoTaskService() {
        Intent serviceIntent = new Intent(this, AutoTaskService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Background auto task stopped", Toast.LENGTH_SHORT).show();
    }

    private void testSecureToggleOnce() {
        try {
            boolean current = com.example.airplanecontrol.utils.AirplaneModeUtils.isAirplaneModeOn(this);
            boolean target = !current;
            boolean ok = com.example.airplanecontrol.utils.AirplaneModeUtils.setAirplaneMode(this, target);
            Toast.makeText(this, ok ? ("Switched to " + (target ? "ON" : "OFF")) : "Toggle failed", Toast.LENGTH_SHORT).show();
            // 回写状态
            new Handler(Looper.getMainLooper()).postDelayed(() -> viewModel.refreshStatus(), 800);
        } catch (Throwable t) {
            Toast.makeText(this, "Execution failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 检查WRITE_SECURE_SETTINGS权限状态
     */
    private boolean hasWriteSecureSettingsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) 
               == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshSecurePermissionStatus() {
        boolean granted = hasWriteSecureSettingsPermission();
        if (tvSecurePermStatus != null) {
            tvSecurePermStatus.setText("WRITE_SECURE_SETTINGS: " + (granted ? "已授权" : "未授权"));
        }
    }

    private void showDeviceInfoDialog() {
        String deviceInfo = com.example.airplanecontrol.utils.AirplaneModeUtils.getDeviceInfo(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Device Information & Status")
                .setMessage(deviceInfo)
                .setPositiveButton("OK", null)
                .setNeutralButton("Force Refresh", (dialog, which) -> {
                    com.example.airplanecontrol.utils.AirplaneModeUtils.forceRefreshAirplaneMode(this);
                    Toast.makeText(this, "Force refresh executed", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateControlsEnabled(boolean assistantPermission) {
        boolean secureMode = viewModel.isControlModeSecure();
        boolean wssGranted = hasWriteSecureSettingsPermission();
        
        // 当仅使用 Secure Settings 时，允许开关与测试；当使用Assistant时需依赖assistantPermission
        boolean enableAuto = secureMode ? wssGranted : assistantPermission;
        switchAutoToggle.setEnabled(enableAuto);
        btnTestToggle.setEnabled(assistantPermission); // 统一测试按钮，根据模式动态执行
    }
}
