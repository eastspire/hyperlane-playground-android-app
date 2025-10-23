package com.example.demoapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.demoapp.R;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvErrorLog;
    private ExecutorService executorService;
    private String errorLogUrl = "http://120.53.248.2:65002/log/error";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        tvErrorLog = view.findViewById(R.id.tv_error_log);
        executorService = Executors.newSingleThreadExecutor();
        
        // 自动加载错误日志
        loadErrorLog();
        
        return view;
    }

    private void loadErrorLog() {
        // 显示加载状态
        tvErrorLog.setText("加载中...");
        tvErrorLog.setTextColor(getResources().getColor(android.R.color.black));
        
        // 异步执行网络请求
        executorService.execute(this::performNetworkRequest);
    }

    private void performNetworkRequest() {
        try {
            URL url = new URL(errorLogUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                String result = response.toString();
                updateUI(result, true);
                
            } else {
                updateUI("请求失败: " + responseCode + " " + connection.getResponseMessage(), false);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            updateUI("网络请求错误: " + e.getMessage(), false);
        }
    }

    private void updateUI(String resultText, boolean isSuccess) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvErrorLog.setText(resultText);
                tvErrorLog.setTextColor(getResources().getColor(
                        isSuccess ? android.R.color.black : android.R.color.darker_gray));
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
