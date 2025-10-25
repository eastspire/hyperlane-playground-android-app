package com.example.demoapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import com.example.demoapp.log.NativeLog;
import com.example.demoapp.log.NativeLogAdapter;
import com.example.demoapp.log.NativeLogManager;
import java.util.List;

public class LogFragment extends Fragment implements NativeLogManager.LogListener {
    
    private static final String TAG = "LogFragment";
    
    private RecyclerView logRecyclerView;
    private NativeLogAdapter logAdapter;
    private RadioGroup logLevelGroup;
    private Button btnClearLogs;
    private NativeLog.Level currentLogLevel = NativeLog.Level.ERROR;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadInitialLogs();
        
        NativeLogManager.getInstance().i(TAG, "LogFragment 初始化完成");
    }
    
    private void initViews(View view) {
        logRecyclerView = view.findViewById(R.id.log_recycler_view);
        logLevelGroup = view.findViewById(R.id.log_level_group);
        btnClearLogs = view.findViewById(R.id.btn_clear_logs);
    }
    
    private void setupRecyclerView() {
        logAdapter = new NativeLogAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        logRecyclerView.setLayoutManager(layoutManager);
        logRecyclerView.setAdapter(logAdapter);
    }
    
    private void setupListeners() {
        // 注册日志监听器
        NativeLogManager.getInstance().addListener(this);
        
        // 日志级别切换
        logLevelGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_error) {
                currentLogLevel = NativeLog.Level.ERROR;
            } else if (checkedId == R.id.radio_info) {
                currentLogLevel = NativeLog.Level.INFO;
            } else if (checkedId == R.id.radio_debug) {
                currentLogLevel = NativeLog.Level.DEBUG;
            }
            loadLogs(currentLogLevel);
            NativeLogManager.getInstance().d(TAG, "切换日志级别: " + currentLogLevel);
        });
        
        // 清除日志
        btnClearLogs.setOnClickListener(v -> {
            NativeLogManager.getInstance().clearLogs();
            logAdapter.setLogs(new java.util.ArrayList<>());
            Toast.makeText(getContext(), "日志已清除", Toast.LENGTH_SHORT).show();
            NativeLogManager.getInstance().i(TAG, "日志已清除");
        });
    }
    
    private void loadInitialLogs() {
        loadLogs(currentLogLevel);
    }
    
    private void loadLogs(NativeLog.Level level) {
        List<NativeLog> logs = NativeLogManager.getInstance().getLogs(level);
        logAdapter.setLogs(logs);
    }
    
    @Override
    public void onLogAdded(NativeLog log) {
        if (log.getLevel() == currentLogLevel) {
            mainHandler.post(() -> {
                logAdapter.addLog(log);
                // 自动滚动到顶部显示最新日志
                logRecyclerView.smoothScrollToPosition(0);
            });
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NativeLogManager.getInstance().removeListener(this);
        NativeLogManager.getInstance().i(TAG, "LogFragment 销毁");
    }
}
