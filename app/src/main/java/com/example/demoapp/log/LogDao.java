package com.example.demoapp.log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface LogDao {
    @Insert
    void insert(LogEntity log);
    
    @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC LIMIT :limit")
    List<LogEntity> getLogsByLevel(String level, int limit);
    
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    List<LogEntity> getAllLogs(int limit);
    
    @Query("DELETE FROM logs WHERE timestamp < :timestamp")
    void deleteOldLogs(long timestamp);
    
    @Query("DELETE FROM logs")
    void deleteAll();
    
    @Query("SELECT COUNT(*) FROM logs")
    int getLogCount();
}
