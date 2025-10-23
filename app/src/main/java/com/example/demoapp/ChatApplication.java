package com.example.demoapp;

import android.app.Application;
import com.example.demoapp.utils.UUIDHelper;

public class ChatApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        UUIDHelper.init(this);
    }
}
