package com.example.demoapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
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

public class TraceFragment extends Fragment {

    private EditText etTraceId;
    private ImageButton btnSearch;
    private TextView tvRequestUrl;
    private TextView tvResult;
    private TextView tvStatusMessage;
    private ScrollView scrollView;
    private ExecutorService executorService;
    private String baseUrl = "http://120.53.248.2:65002"; // Trace 查询服务器地址

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trace, container, false);
        
        initViews(view);
        setupListeners();
        executorService = Executors.newSingleThreadExecutor();
        
        return view;
    }

    private void initViews(View view) {
        etTraceId = view.findViewById(R.id.et_trace_id);
        btnSearch = view.findViewById(R.id.btn_search);
        tvRequestUrl = view.findViewById(R.id.tv_request_url);
        tvResult = view.findViewById(R.id.tv_result);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        scrollView = view.findViewById(R.id.scroll_view);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> searchTrace());
        
        // 支持回车键搜索
        etTraceId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchTrace();
                return true;
            }
            return false;
        });
        
        // 点击 URL 打开浏览器
        tvRequestUrl.setOnClickListener(v -> {
            String url = tvRequestUrl.getText().toString();
            if (!url.isEmpty() && url.startsWith("http")) {
                openUrlInBrowser(url);
            }
        });
    }

    private void searchTrace() {
        String traceId = etTraceId.getText().toString().trim();
        
        // 清除之前的状态消息
        tvStatusMessage.setVisibility(View.GONE);
        tvRequestUrl.setVisibility(View.GONE);
        
        if (traceId.isEmpty()) {
            tvResult.setText("Please enter a valid Trace ID");
            tvResult.setTextColor(getResources().getColor(android.R.color.darker_gray));
            return;
        }
        
        // 显示请求 URL
        String requestUrl = baseUrl + "/trace/" + traceId;
        tvRequestUrl.setText(requestUrl);
        tvRequestUrl.setVisibility(View.VISIBLE);
        
        // 显示加载状态
        tvResult.setText("Searching...");
        tvResult.setTextColor(getResources().getColor(android.R.color.black));
        btnSearch.setEnabled(false);
        
        // 异步执行网络请求
        executorService.execute(() -> performNetworkRequest(requestUrl, traceId));
    }

    private void performNetworkRequest(String requestUrl, String traceId) {
        try {
            URL url = new URL(requestUrl);
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
                updateUI(result, "Search completed successfully", true);
                
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                updateUI("Trace record not found", "Search failed: Trace record not found", false);
                
            } else {
                updateUI("Request failed: " + responseCode + " " + connection.getResponseMessage(),
                        "Search failed: " + responseCode + " " + connection.getResponseMessage(), false);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            updateUI("Network request error: " + e.getMessage(),
                    "Search failed: Network request error - " + e.getMessage(), false);
        }
    }

    private void updateUI(String resultText, String statusMessage, boolean isSuccess) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvResult.setText(resultText);
                tvResult.setTextColor(getResources().getColor(
                        isSuccess ? android.R.color.black : android.R.color.darker_gray));
                
                tvStatusMessage.setText(statusMessage);
                tvStatusMessage.setTextColor(getResources().getColor(
                        isSuccess ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
                tvStatusMessage.setVisibility(View.VISIBLE);
                
                btnSearch.setEnabled(true);
                
                // 滚动到顶部
                scrollView.smoothScrollTo(0, 0);
            });
        }
    }

    private void openUrlInBrowser(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            if (getActivity() != null) {
                android.widget.Toast.makeText(getActivity(), 
                    "Unable to open browser", android.widget.Toast.LENGTH_SHORT).show();
            }
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
