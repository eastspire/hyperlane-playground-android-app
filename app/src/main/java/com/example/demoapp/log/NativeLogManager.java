package com.example.demoapp.log;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeLogManager {
    private static NativeLogManager instance;
    private final List<NativeLog> logs = new CopyOnWriteArrayList<>();
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 1000;
    private static final int MAX_DB_LOGS = 10000;
    private static final long LOG_RETENTION_DAYS = 7;
    
    private LogDatabase database;
    private ExecutorService executorService;
    private boolean initialized = false;
    
    public interface LogListener {
        void onLogAdded(NativeLog log);
    }
    
    private NativeLogManager() {
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized NativeLogManager getInstance() {
        if (instance == null) {
            instance = new NativeLogManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        if (initialized) return;
        
        database = LogDatabase.getInstance(context);
        initialized = true;
        
        // 异步加载历史日志
        executorService.execute(() -> {
            try {
                List<LogEntity> entities = database.logDao().getAllLogs(MAX_LOGS);
                for (LogEntity entity : entities) {
                    logs.add(NativeLog.fromEntity(entity));
                }
                
                // 清理过期日志
                long cutoffTime = System.currentTimeMillis() - (LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000);
                database.logDao().deleteOldLogs(cutoffTime);
            } catch (Exception e) {
                Log.e("NativeLogManager", "Failed to load logs", e);
            }
        });
    }
    
    public void addLog(NativeLog.Level level, String tag, String message) {
        NativeLog log = new NativeLog(level, tag, message);
        logs.add(log);
        
        // 限制内存中的日志数量
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
        
        // 异步保存到数据库
        if (initialized && database != null) {
            executorService.execute(() -> {
                try {
                    database.logDao().insert(log.toEntity());
                    
                    // 定期清理数据库中的旧日志
                    int count = database.logDao().getLogCount();
                    if (count > MAX_DB_LOGS) {
                        long cutoffTime = System.currentTimeMillis() - (LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000);
                        database.logDao().deleteOldLogs(cutoffTime);
                    }
                } catch (Exception e) {
                    Log.e("NativeLogManager", "Failed to save log", e);
                }
            });
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
        
        // 异步清空数据库
        if (initialized && database != null) {
            executorService.execute(() -> {
                try {
                    database.logDao().deleteAll();
                } catch (Exception e) {
                    Log.e("NativeLogManager", "Failed to clear logs", e);
                }
            });
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
