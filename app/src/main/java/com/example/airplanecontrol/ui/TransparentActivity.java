package com.example.airplanecontrol.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.airplanecontrol.AppLifecycleObserver;

public class TransparentActivity extends Activity {

    private static final String TAG = "TransparentActivity";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean assistTriggered = false; // 防止重复触发

    boolean tag_showassist_first = Boolean.FALSE;
    boolean tag_showassist_second = Boolean.FALSE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("AutoTaskDebug", "TransparentActivity 已启动，准备调用 showAssist...");

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Log.d(TAG, "窗口焦点状态改变: " + hasFocus);

        // 当窗口获得焦点，并且我们还没有触发过 assist 时，执行操作
        if (hasFocus && !assistTriggered) {
            assistTriggered = true;
            Log.d(TAG, "窗口已获得焦点，准备调用 showAssist...");

            try {
                Bundle args = new Bundle();
                args.putString("command", "timed_toggle");
                tag_showassist_first = showAssist(args);
                // Log.d(TAG, "Flight mode toggle command sent");
                Log.d(TAG, "onWindowFocusChanged 调用 showAssist 完成,状态：" + tag_showassist_first);


                // 2. 延迟2秒后，执行第二次切换
                mainHandler.postDelayed(() -> {
                    // 检查 Activity 是否仍然有效
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "Activity 已被销毁，取消第二次切换。");
                        return;
                    }

                    Log.d(TAG, "2秒延迟结束，准备执行第二次飞行模式切换。");
                    tag_showassist_second = showAssist(args);
                    // Log.d(TAG, "Flight mode toggle command sent (second)");
                    Log.d(TAG, "onWindowFocusChanged 调用 showAssist 第二次 完成,状态：" + tag_showassist_second);

                    // 3. 在第二次切换指令发出后，再稍作延迟后关闭 Activity
                    //    给予系统足够的时间来处理最后一个请求
                    mainHandler.postDelayed(() -> {
                        Log.d(TAG, "整个序列执行完毕，销毁 Activity。");
                        try {
                            finishAndRemoveTask();
                        } catch (Throwable t) {
                            finish();
                        }
                    }, 2000); // 延迟 500ms 后销毁

                }, 2000); // 延迟 2000ms


            } catch (Exception e) {
                Log.e(TAG, "调用 showAssist 失败", e);
                finish(); // 即使失败也要结束Activity
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在 Activity 销毁时，移除 Handler 中所有待处理的消息和回调
        // 这是一个好习惯，可以防止内存泄漏
        mainHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "TransparentActivity 已销毁，Handler 回调已清理。");
    }
}



