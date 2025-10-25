package com.example.demoapp;

import android.app.Application;
import com.example.demoapp.log.NativeLogManager;
import com.example.demoapp.utils.UUIDHelper;

public class ChatApplication extends Application {
    
    private static final String TAG = "ChatApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化日志系统
        NativeLogManager.getInstance().i(TAG, "应用启动");
        
        // 初始化 UUID
        UUIDHelper.init(this);
        NativeLogManager.getInstance().i(TAG, "UUID 初始化完成");
        
        // 记录应用版本信息
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            NativeLogManager.getInstance().i(TAG, "应用版本: " + versionName);
        } catch (Exception e) {
            NativeLogManager.getInstance().e(TAG, "获取版本信息失败: " + e.getMessage());
        }
        
        NativeLogManager.getInstance().d(TAG, "应用初始化完成");
    }
}
