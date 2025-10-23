package com.example.demoapp.upload;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 参数传递诊断工具
 * 用于验证客户端发送的所有参数是否完整
 */
public class ParameterDiagnostics {
    private static final String TAG = "ParamDiagnostics";
    
    /**
     * 诊断注册上传请求
     */
    public static void diagnoseRegisterRequest(String fileId, int totalChunks, String fileName, long fileSize) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "诊断：注册上传请求");
        Log.d(TAG, "========================================");
        
        // 检查必需参数
        boolean allValid = true;
        
        if (fileId == null || fileId.isEmpty()) {
            Log.e(TAG, "❌ X-File-Id 为空");
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-File-Id: " + fileId);
        }
        
        if (totalChunks <= 0) {
            Log.e(TAG, "❌ X-Total-Chunks 无效: " + totalChunks);
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-Total-Chunks: " + totalChunks);
        }
        
        if (fileName == null || fileName.isEmpty()) {
            Log.e(TAG, "❌ X-File-Name 为空");
            allValid = false;
        } else {
            try {
                String encoded = URLEncoder.encode(fileName, "UTF-8");
                Log.d(TAG, "✅ X-File-Name: " + fileName);
                Log.d(TAG, "   编码后: " + encoded);
            } catch (Exception e) {
                Log.e(TAG, "❌ X-File-Name 编码失败: " + e.getMessage());
                allValid = false;
            }
        }
        
        if (fileSize <= 0) {
            Log.e(TAG, "❌ X-File-Size 无效: " + fileSize);
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-File-Size: " + fileSize + " bytes");
        }
        
        if (allValid) {
            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ 所有参数有效");
            Log.d(TAG, "========================================");
        } else {
            Log.e(TAG, "========================================");
            Log.e(TAG, "❌ 存在无效参数");
            Log.e(TAG, "========================================");
        }
    }
    
    /**
     * 诊断分片上传请求
     */
    public static void diagnoseChunkRequest(String fileId, int chunkIndex, int totalChunks, int chunkSize) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "诊断：分片上传请求 #" + chunkIndex);
        Log.d(TAG, "========================================");
        
        boolean allValid = true;
        
        if (fileId == null || fileId.isEmpty()) {
            Log.e(TAG, "❌ X-File-Id 为空");
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-File-Id: " + fileId);
        }
        
        if (chunkIndex < 0) {
            Log.e(TAG, "❌ X-Chunk-Index 无效: " + chunkIndex);
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-Chunk-Index: " + chunkIndex);
        }
        
        if (totalChunks <= 0) {
            Log.e(TAG, "❌ X-Total-Chunks 无效: " + totalChunks);
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-Total-Chunks: " + totalChunks);
        }
        
        if (chunkSize <= 0) {
            Log.e(TAG, "❌ X-Chunk-Size 无效: " + chunkSize);
            allValid = false;
        } else {
            Log.d(TAG, "✅ X-Chunk-Size: " + chunkSize + " bytes");
        }
        
        if (chunkIndex >= totalChunks) {
            Log.e(TAG, "❌ 分片索引超出范围: " + chunkIndex + " >= " + totalChunks);
            allValid = false;
        }
        
        if (allValid) {
            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ 所有参数有效");
            Log.d(TAG, "========================================");
        } else {
            Log.e(TAG, "========================================");
            Log.e(TAG, "❌ 存在无效参数");
            Log.e(TAG, "========================================");
        }
    }
    
    /**
     * 诊断合并请求
     */
    public static void diagnoseMergeRequest(String fileId) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "诊断：合并文件请求");
        Log.d(TAG, "========================================");
        
        if (fileId == null || fileId.isEmpty()) {
            Log.e(TAG, "❌ X-File-Id 为空");
            Log.e(TAG, "========================================");
        } else {
            Log.d(TAG, "✅ X-File-Id: " + fileId);
            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ 所有参数有效");
            Log.d(TAG, "========================================");
        }
    }
    
    /**
     * 测试服务器连接
     */
    public static void testServerConnection(String serverUrl) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                Log.d(TAG, "========================================");
                Log.d(TAG, "测试服务器连接");
                Log.d(TAG, "========================================");
                Log.d(TAG, "服务器: " + serverUrl);
                
                URL url = new URL(serverUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "响应码: " + responseCode);
                
                if (responseCode == 200 || responseCode == 302 || responseCode == 404) {
                    Log.d(TAG, "✅ 服务器可访问");
                } else {
                    Log.e(TAG, "⚠ 服务器响应异常: " + responseCode);
                }
                
                Log.d(TAG, "========================================");
                
            } catch (Exception e) {
                Log.e(TAG, "❌ 服务器连接失败", e);
                Log.e(TAG, "========================================");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    
    /**
     * 打印所有请求头
     */
    public static void printRequestHeaders(Map<String, String> headers) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "请求头列表");
        Log.d(TAG, "========================================");
        
        if (headers == null || headers.isEmpty()) {
            Log.e(TAG, "❌ 请求头为空");
        } else {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                Log.d(TAG, entry.getKey() + ": " + entry.getValue());
            }
        }
        
        Log.d(TAG, "========================================");
    }
    
    /**
     * 验证 URL 格式
     */
    public static void validateUrl(String url) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "验证 URL 格式");
        Log.d(TAG, "========================================");
        Log.d(TAG, "URL: " + url);
        
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "❌ URL 为空");
            Log.d(TAG, "========================================");
            return;
        }
        
        boolean isValid = true;
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Log.e(TAG, "❌ URL 缺少协议 (http:// 或 https://)");
            isValid = false;
        }
        
        // 检查是否包含路由参数格式 /api/upload/file/{dir}/{file}
        if (url.contains("/api/upload/file/")) {
            Log.d(TAG, "✅ 检测到路由参数格式");
            String[] parts = url.split("/api/upload/file/");
            if (parts.length > 1) {
                String params = parts[1];
                String[] pathParts = params.split("/");
                Log.d(TAG, "   路径参数数量: " + pathParts.length);
                for (int i = 0; i < pathParts.length; i++) {
                    Log.d(TAG, "   参数[" + i + "]: " + pathParts[i]);
                }
            }
        }
        
        // 检查是否包含查询参数格式 ?dir=xxx&file=xxx
        if (url.contains("?")) {
            Log.d(TAG, "✅ 检测到查询参数格式");
            String[] parts = url.split("\\?");
            if (parts.length > 1) {
                String query = parts[1];
                String[] queryParts = query.split("&");
                Log.d(TAG, "   查询参数数量: " + queryParts.length);
                for (String param : queryParts) {
                    Log.d(TAG, "   参数: " + param);
                }
            }
        }
        
        if (isValid) {
            Log.d(TAG, "✅ URL 格式有效");
        } else {
            Log.e(TAG, "❌ URL 格式无效");
        }
        
        Log.d(TAG, "========================================");
    }
}
