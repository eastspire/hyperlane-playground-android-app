package com.example.demoapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.demoapp.R;
import com.example.demoapp.chat.ChatApiService;
import com.example.demoapp.utils.DataExportManager;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据导出 Fragment
 * 演示如何导出数据并唤起浏览器下载
 */
public class ExportFragment extends Fragment {
    
    private ChatApiService apiService;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export, container, false);
        
        apiService = new ChatApiService();
        
        setupButtons(view);
        
        return view;
    }
    
    private void setupButtons(View view) {
        // 导出聊天记录（JSON）
        Button btnExportChatJson = view.findViewById(R.id.btn_export_chat_json);
        btnExportChatJson.setOnClickListener(v -> exportChatHistory("json"));
        
        // 导出聊天记录（TXT）
        Button btnExportChatTxt = view.findViewById(R.id.btn_export_chat_txt);
        btnExportChatTxt.setOnClickListener(v -> exportChatHistory("txt"));
        
        // 导出上传记录
        Button btnExportUpload = view.findViewById(R.id.btn_export_upload);
        btnExportUpload.setOnClickListener(v -> exportUploadHistory("json"));
        
        // 导出用户数据
        Button btnExportUserData = view.findViewById(R.id.btn_export_user_data);
        btnExportUserData.setOnClickListener(v -> exportUserData("json"));
        
        // 直接下载示例
        Button btnDirectDownload = view.findViewById(R.id.btn_direct_download);
        btnDirectDownload.setOnClickListener(v -> directDownloadExample());
    }
    
    /**
     * 导出聊天记录
     */
    private void exportChatHistory(String format) {
        if (getContext() == null) return;
        
        Toast.makeText(getContext(), "正在准备导出聊天记录...", Toast.LENGTH_SHORT).show();
        
        // 方式 1: 直接唤起浏览器（如果服务器支持直接下载）
        String exportUrl = apiService.getBaseUrl() + "/chat/export?format=" + format;
        DataExportManager.exportAndDownload(getContext(), exportUrl, "chat_history." + format);
        
        // 方式 2: 先请求服务器生成下载链接，再唤起浏览器
        /*
        apiService.requestExport("chat", format, new ChatApiService.ExportCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        DataExportManager.downloadFile(getContext(), downloadUrl);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "导出失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
        */
    }
    
    /**
     * 导出上传记录
     */
    private void exportUploadHistory(String format) {
        if (getContext() == null) return;
        
        Toast.makeText(getContext(), "正在准备导出上传记录...", Toast.LENGTH_SHORT).show();
        
        String exportUrl = apiService.getBaseUrl() + "/upload/export?format=" + format;
        DataExportManager.exportAndDownload(getContext(), exportUrl, "upload_history." + format);
    }
    
    /**
     * 导出用户数据
     */
    private void exportUserData(String format) {
        if (getContext() == null) return;
        
        Toast.makeText(getContext(), "正在准备导出用户数据...", Toast.LENGTH_SHORT).show();
        
        // 使用参数构建 URL
        Map<String, String> params = new HashMap<>();
        params.put("format", format);
        params.put("type", "all");
        params.put("includeHistory", "true");
        
        String exportUrl = DataExportManager.buildExportUrl(
            apiService.getBaseUrl(), 
            "/user/export", 
            params
        );
        
        DataExportManager.exportAndDownload(getContext(), exportUrl, "user_data." + format);
    }
    
    /**
     * 直接下载示例（已知文件 URL）
     */
    private void directDownloadExample() {
        if (getContext() == null) return;
        
        // 示例：下载一个已上传的文件
        String fileUrl = apiService.getBaseUrl() + "/api/upload/file/example/test.pdf";
        
        // 方式 1: 唤起浏览器下载
        DataExportManager.downloadFile(getContext(), fileUrl);
        
        // 方式 2: 使用 DownloadManager 下载到本地
        // DataExportManager.downloadWithDownloadManager(
        //     getContext(), 
        //     fileUrl, 
        //     "test.pdf", 
        //     "下载测试文件"
        // );
    }
}
