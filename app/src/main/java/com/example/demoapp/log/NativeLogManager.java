package com.example.demoapp.log;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NativeLogManager {
    private static NativeLogManager instance;
    private final List<NativeLog> logs = new CopyOnWriteArrayList<>();
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 1000;
    
    public interface LogListener {
        void onLogAdded(NativeLog log);
    }
    
    private NativeLogManager() {}
    
    public static synchronized NativeLogManager getInstance() {
        if (instance == null) {
            instance = new NativeLogManager();
        }
        return instance;
    }
    
    public void addLog(NativeLog.Level level, String tag, String message) {
        NativeLog log = new NativeLog(level, tag, message);
        logs.add(log);
        
        // 限制日志数量
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
        
        // 同时输出到 Logcat
        switch (level) {
            case ERROR:
                Log.e(tag, message);
                break;
            case INFO:
                Log.i(tag, message);
                break;
            case DEBUG:
                Log.d(tag, message);
                break;
        }
        
        // 通知监听器
        for (LogListener listener : listeners) {
            listener.onLogAdded(log);
        }
    }
    
    public void e(String tag, String message) {
        addLog(NativeLog.Level.ERROR, tag, message);
    }
    
    public void i(String tag, String message) {
        addLog(NativeLog.Level.INFO, tag, message);
    }
    
    public void d(String tag, String message) {
        addLog(NativeLog.Level.DEBUG, tag, message);
    }
    
    public List<NativeLog> getLogs(NativeLog.Level level) {
        List<NativeLog> filteredLogs = new ArrayList<>();
        for (NativeLog log : logs) {
            if (log.getLevel() == level) {
                filteredLogs.add(log);
            }
        }
        // 按时间逆序排序
        Collections.sort(filteredLogs, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return filteredLogs;
    }
    
    public void addListener(LogListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
    
    public void clearLogs() {
        logs.clear();
    }
}
