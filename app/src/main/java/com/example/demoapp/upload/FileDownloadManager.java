package com.example.demoapp.upload;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件下载管理器 - 完全对齐服务器端 UploadFileRoute 逻辑
 * 支持：
 * 1. URL 参数解析（dir, file）
 * 2. Range 请求（断点续传/流媒体播放）
 * 3. 响应头处理（Content-Type, Accept-Ranges, Cache-Control, Content-Range）
 * 4. 206 Partial Content 响应
 */
public class FileDownloadManager {
    private static final String TAG = "FileDownloadManager";
    private static final String CHARSET = "UTF-8";
    
    public interface DownloadCallback {
        void onProgress(long downloaded, long total);
        void onSuccess(byte[] data, String contentType);
        void onError(String error);
    }
    
    /**
     * 下载文件 - 支持 Range 请求
     * @param fileUrl 文件 URL（包含 dir 和 file 参数）
     * @param rangeStart Range 起始位置（null 表示不使用 Range）
     * @param rangeEnd Range 结束位置（null 表示到文件末尾）
     * @param callback 回调
     */
    public void downloadFile(String fileUrl, Long rangeStart, Long rangeEnd, DownloadCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // 解析 URL 参数
                Map<String, String> params = parseUrlParams(fileUrl);
                String dir = params.get("dir");
                String file = params.get("file");
                
                Log.d(TAG, "========== 下载文件 ==========");
                Log.d(TAG, "URL: " + fileUrl);
                Log.d(TAG, "Dir: " + dir);
                Log.d(TAG, "File: " + file);
                
                URL url = new URL(fileUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                
                // 设置 Range 请求头（如果需要）
                if (rangeStart != null) {
                    String rangeHeader = "bytes=" + rangeStart + "-";
                    if (rangeEnd != null) {
                        rangeHeader += rangeEnd;
                    }
                    conn.setRequestProperty("Range", rangeHeader);
                    Log.d(TAG, "Range 请求: " + rangeHeader);
                }
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "响应码: " + responseCode);
                
                // 检查响应码（200 或 206）
                if (responseCode != HttpURLConnection.HTTP_OK && 
                    responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    callback.onError("HTTP 错误: " + responseCode);
                    return;
                }
                
                // 读取响应头
                String contentType = conn.getHeaderField("Content-Type");
                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                String contentRange = conn.getHeaderField("Content-Range");
                String contentLength = conn.getHeaderField("Content-Length");
                String cacheControl = conn.getHeaderField("Cache-Control");
                String expires = conn.getHeaderField("Expires");
                
                Log.d(TAG, "========== 响应头 ==========");
                Log.d(TAG, "Content-Type: " + contentType);
                Log.d(TAG, "Accept-Ranges: " + acceptRanges);
                Log.d(TAG, "Content-Range: " + contentRange);
                Log.d(TAG, "Content-Length: " + contentLength);
                Log.d(TAG, "Cache-Control: " + cacheControl);
                Log.d(TAG, "Expires: " + expires);
                
                // 解析内容长度
                long totalSize = contentLength != null ? Long.parseLong(contentLength) : -1;
                
                // 如果是 206 响应，从 Content-Range 中获取总大小
                if (responseCode == HttpURLConnection.HTTP_PARTIAL && contentRange != null) {
                    // Content-Range: bytes 0-1023/2048
                    String[] parts = contentRange.split("/");
                    if (parts.length == 2) {
                        totalSize = Long.parseLong(parts[1]);
                    }
                }
                
                // 读取数据
                InputStream is = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                byte[] data = new byte[8192];
                int nRead;
                long downloaded = 0;
                
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                    downloaded += nRead;
                    
                    // 通知进度
                    long finalTotalSize = totalSize;
                    long finalDownloaded = downloaded;
                    callback.onProgress(finalDownloaded, finalTotalSize);
                }
                
                is.close();
                byte[] fileData = buffer.toByteArray();
                
                Log.d(TAG, "下载完成，大小: " + fileData.length);
                callback.onSuccess(fileData, contentType);
                
            } catch (Exception e) {
                Log.e(TAG, "下载错误", e);
                callback.onError(e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    
    /**
     * 下载文件（不使用 Range）
     */
    public void downloadFile(String fileUrl, DownloadCallback callback) {
        downloadFile(fileUrl, null, null, callback);
    }
    
    /**
     * 解析 URL 参数
     * 对齐服务器端的 URL 解码逻辑
     * 支持两种格式：
     * 1. 路由参数: /api/upload/file/{dir}/{file}
     * 2. 查询参数: /api/upload/file?dir=xxx&file=xxx
     */
    private Map<String, String> parseUrlParams(String fileUrl) {
        Map<String, String> params = new HashMap<>();
        try {
            // 先尝试解析查询参数
            int queryStart = fileUrl.indexOf('?');
            if (queryStart != -1) {
                String query = fileUrl.substring(queryStart + 1);
                String[] pairs = query.split("&");
                
                for (String pair : pairs) {
                    int idx = pair.indexOf('=');
                    if (idx > 0) {
                        String key = URLDecoder.decode(pair.substring(0, idx), CHARSET);
                        String value = URLDecoder.decode(pair.substring(idx + 1), CHARSET);
                        params.put(key, value);
                    }
                }
            } else {
                // 尝试解析路由参数 /api/upload/file/{dir}/{file}
                // 提取路径部分
                String path = fileUrl;
                int hashIndex = path.indexOf('#');
                if (hashIndex != -1) {
                    path = path.substring(0, hashIndex);
                }
                
                // 查找 /api/upload/file/ 后面的部分
                String pattern = "/api/upload/file/";
                int patternIndex = path.indexOf(pattern);
                if (patternIndex != -1) {
                    String pathParams = path.substring(patternIndex + pattern.length());
                    String[] parts = pathParams.split("/", 2);
                    
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        params.put("dir", URLDecoder.decode(parts[0], CHARSET));
                    }
                    if (parts.length >= 2 && !parts[1].isEmpty()) {
                        params.put("file", URLDecoder.decode(parts[1], CHARSET));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析 URL 参数错误", e);
        }
        return params;
    }
    
    /**
     * 检查服务器是否支持 Range 请求
     */
    public void checkRangeSupport(String fileUrl, RangeSupportCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(fileUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                int responseCode = conn.getResponseCode();
                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                String contentLength = conn.getHeaderField("Content-Length");
                
                boolean supportsRange = "bytes".equalsIgnoreCase(acceptRanges);
                long fileSize = contentLength != null ? Long.parseLong(contentLength) : -1;
                
                callback.onResult(supportsRange, fileSize);
                
            } catch (Exception e) {
                Log.e(TAG, "检查 Range 支持错误", e);
                callback.onResult(false, -1);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    
    public interface RangeSupportCallback {
        void onResult(boolean supportsRange, long fileSize);
    }
}
