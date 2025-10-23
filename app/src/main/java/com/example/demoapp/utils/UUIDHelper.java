package com.example.demoapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class UUIDHelper {
    
    private static final String PREF_NAME = "chat_prefs";
    private static final String KEY_UUID = "browser_uuid";
    private static String cachedUUID = null;
    private static Context appContext = null;
    
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }
    
    public static String getUUID() {
        if (cachedUUID != null) {
            return cachedUUID;
        }
        
        if (appContext == null) {
            return generateUUID();
        }
        
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        cachedUUID = prefs.getString(KEY_UUID, null);
        
        if (cachedUUID == null) {
            cachedUUID = generateUUID();
            prefs.edit().putString(KEY_UUID, cachedUUID).apply();
        }
        
        return cachedUUID;
    }
    
    public static String getUsername() {
        return "User" + getUUID();
    }
    
    private static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
