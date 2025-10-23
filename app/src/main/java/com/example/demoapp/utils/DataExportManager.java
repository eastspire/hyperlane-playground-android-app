package com.example.demoapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 数据导出管理器
 * 支持导出数据并唤起浏览器下载
 */
public class DataExportManager {
    private static final String TAG = "DataExportManager";
    
    /**
     * 导出数据到服务器并唤起浏览器下载
     * 
     * @param context 上下文
     * @param exportUrl 导出接口 URL（服务器端生成下载链接）
     * @param fileName 文件名（可选，用于显示）
     */
    public static void exportAndDownload(Context context, String exportUrl, String fileName) {
        Log.d(TAG, "========== 导出数据并下载 ==========");
        Log.d(TAG, "导出 URL: " + exportUrl);
        Log.d(TAG, "文件名: " + fileName);
        
        try {
            // 唤起浏览器下载
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(exportUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 检查是否有应用可以处理这个 Intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Toast.makeText(context, "正在打开浏览器下载...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "✅ 成功唤起浏览器");
            } else {
                // 如果没有浏览器，尝试使用选择器
                Intent chooser = Intent.createChooser(intent, "选择浏览器下载");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
                Log.d(TAG, "✅ 使用选择器唤起浏览器");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 唤起浏览器失败", e);
            Toast.makeText(context, "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 导出数据到服务器并唤起浏览器下载（带参数）
     * 
     * @param context 上下文
     * @param baseUrl 基础 URL
     * @param exportPath 导出路径
     * @param params 查询参数（如 format=json&type=all）
     */
    public static void exportAndDownloadWithParams(Context context, String baseUrl, String exportPath, String params) {
        String fullUrl = baseUrl + exportPath;
        if (params != null && !params.isEmpty()) {
            fullUrl += "?" + params;
        }
        exportAndDownload(context, fullUrl, null);
    }
    
    /**
     * 直接下载文件 URL
     * 
     * @param context 上下文
     * @param fileUrl 文件 URL（已经存在的文件）
     */
    public static void downloadFile(Context context, String fileUrl) {
        Log.d(TAG, "========== 下载文件 ==========");
        Log.d(TAG, "文件 URL: " + fileUrl);
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fileUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            Toast.makeText(context, "正在下载...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✅ 成功唤起下载");
        } catch (Exception e) {
            Log.e(TAG, "❌ 下载失败", e);
            Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 使用 Android DownloadManager 下载文件
     * 
     * @param context 上下文
     * @param fileUrl 文件 URL
     * @param fileName 保存的文件名
     * @param description 下载描述
     */
    public static void downloadWithDownloadManager(Context context, String fileUrl, String fileName, String description) {
        Log.d(TAG, "========== 使用 DownloadManager 下载 ==========");
        Log.d(TAG, "文件 URL: " + fileUrl);
        Log.d(TAG, "文件名: " + fileName);
        
        try {
            android.app.DownloadManager downloadManager = 
                (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            
            if (downloadManager == null) {
                Toast.makeText(context, "DownloadManager 不可用", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.app.DownloadManager.Request request = 
                new android.app.DownloadManager.Request(Uri.parse(fileUrl));
            
            // 设置下载标题和描述
            request.setTitle(fileName);
            request.setDescription(description != null ? description : "正在下载文件");
            
            // 设置通知可见性
            request.setNotificationVisibility(
                android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            
            // 设置保存路径（下载到 Downloads 目录）
            request.setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS, 
                fileName
            );
            
            // 允许在移动网络和 WiFi 下下载
            request.setAllowedNetworkTypes(
                android.app.DownloadManager.Request.NETWORK_MOBILE | 
                android.app.DownloadManager.Request.NETWORK_WIFI
            );
            
            // 开始下载
            long downloadId = downloadManager.enqueue(request);
            
            Toast.makeText(context, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✅ 下载任务已创建，ID: " + downloadId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ DownloadManager 下载失败", e);
            Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 导出聊天记录
     * 
     * @param context 上下文
     * @param baseUrl 服务器基础 URL
     * @param format 导出格式（json, txt, csv 等）
     */
    public static void exportChatHistory(Context context, String baseUrl, String format) {
        String exportUrl = baseUrl + "/chat/export?format=" + format;
        exportAndDownload(context, exportUrl, "chat_history." + format);
    }
    
    /**
     * 导出上传记录
     * 
     * @param context 上下文
     * @param baseUrl 服务器基础 URL
     * @param format 导出格式
     */
    public static void exportUploadHistory(Context context, String baseUrl, String format) {
        String exportUrl = baseUrl + "/upload/export?format=" + format;
        exportAndDownload(context, exportUrl, "upload_history." + format);
    }
    
    /**
     * 导出用户数据
     * 
     * @param context 上下文
     * @param baseUrl 服务器基础 URL
     * @param userId 用户 ID
     * @param format 导出格式
     */
    public static void exportUserData(Context context, String baseUrl, String userId, String format) {
        String exportUrl = baseUrl + "/user/" + userId + "/export?format=" + format;
        exportAndDownload(context, exportUrl, "user_data_" + userId + "." + format);
    }
    
    /**
     * 构建导出 URL
     * 
     * @param baseUrl 基础 URL
     * @param path 路径
     * @param params 参数键值对
     * @return 完整的导出 URL
     */
    public static String buildExportUrl(String baseUrl, String path, java.util.Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            url.append("/");
        }
        url.append(path);
        
        if (params != null && !params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                try {
                    url.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
                    url.append("=");
                    url.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
                } catch (Exception e) {
                    Log.e(TAG, "URL 编码失败", e);
                }
                first = false;
            }
        }
        
        return url.toString();
    }
}
