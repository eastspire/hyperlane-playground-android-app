package com.example.demoapp.log;

public class NativeLog {
    public enum Level {
        ERROR, INFO, DEBUG
    }
    
    private Level level;
    private String message;
    private long timestamp;
    private String tag;
    
    public NativeLog(Level level, String tag, String message) {
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public NativeLog(Level level, String tag, String message, long timestamp) {
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public static NativeLog fromEntity(LogEntity entity) {
        Level level = Level.valueOf(entity.getLevel());
        return new NativeLog(level, entity.getTag(), entity.getMessage(), entity.getTimestamp());
    }
    
    public LogEntity toEntity() {
        return new LogEntity(level.name(), tag, message, timestamp);
    }
    
    public Level getLevel() {
        return level;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getTag() {
        return tag;
    }
}
