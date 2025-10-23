# 数据导出功能使用指南

## 功能概述

实现了完整的数据导出功能，支持唤起浏览器完成下载。

## 核心组件

### 1. DataExportManager.java

通用的数据导出管理器，提供多种导出方式。

#### 主要方法

##### 1.1 唤起浏览器下载

```java
// 最简单的方式：直接唤起浏览器下载
DataExportManager.exportAndDownload(
    context, 
    "http://120.53.248.2:65002/chat/export?format=json",
    "chat_history.json"
);
```

##### 1.2 带参数的导出

```java
// 使用参数构建 URL
Map<String, String> params = new HashMap<>();
params.put("format", "json");
params.put("type", "all");
params.put("startDate", "2024-01-01");
params.put("endDate", "2024-12-31");

String exportUrl = DataExportManager.buildExportUrl(
    "http://120.53.248.2:65002",
    "/chat/export",
    params
);

DataExportManager.exportAndDownload(context, exportUrl, "chat_history.json");
```

##### 1.3 使用 DownloadManager 下载

```java
// 使用 Android 系统的 DownloadManager
// 优点：显示下载进度、支持断点续传、自动管理下载
DataExportManager.downloadWithDownloadManager(
    context,
    "http://120.53.248.2:65002/files/report.pdf",
    "report.pdf",
    "下载报告文件"
);
```

##### 1.4 预定义的导出方法

```java
// 导出聊天记录
DataExportManager.exportChatHistory(context, baseUrl, "json");

// 导出上传记录
DataExportManager.exportUploadHistory(context, baseUrl, "csv");

// 导出用户数据
DataExportManager.exportUserData(context, baseUrl, userId, "json");
```

### 2. ChatApiService.java

扩展了 API 服务，支持导出数据请求。

#### 使用示例

```java
ChatApiService apiService = new ChatApiService();

// 方式 1: 直接获取导出 URL
String exportUrl = apiService.getExportUrl("chat", "json");
DataExportManager.exportAndDownload(context, exportUrl, "chat.json");

// 方式 2: 请求服务器生成下载链接
apiService.requestExport("chat", "json", new ChatApiService.ExportCallback() {
    @Override
    public void onSuccess(String downloadUrl) {
        // 服务器返回下载链接
        DataExportManager.downloadFile(context, downloadUrl);
    }
    
    @Override
    public void onError(String error) {
        Toast.makeText(context, "导出失败: " + error, Toast.LENGTH_SHORT).show();
    }
});
```

## 使用场景

### 场景 1: 导出聊天记录

```java
// 在 ChatFragment 中添加导出按钮
Button btnExport = view.findViewById(R.id.btn_export_chat);
btnExport.setOnClickListener(v -> {
    String exportUrl = "http://120.53.248.2:65002/chat/export?format=json";
    DataExportManager.exportAndDownload(getContext(), exportUrl, "chat_history.json");
});
```

### 场景 2: 导出上传记录

```java
// 在 UploadFragment 中添加导出功能
Button btnExportHistory = view.findViewById(R.id.btn_export_history);
btnExportHistory.setOnClickListener(v -> {
    Map<String, String> params = new HashMap<>();
    params.put("format", "csv");
    params.put("userId", getCurrentUserId());
    
    String exportUrl = DataExportManager.buildExportUrl(
        "http://120.53.248.2:65002",
        "/upload/export",
        params
    );
    
    DataExportManager.exportAndDownload(getContext(), exportUrl, "upload_history.csv");
});
```

### 场景 3: 下载已上传的文件

```java
// 在 UploadHistoryAdapter 中
holder.btnDownload.setOnClickListener(v -> {
    String fileUrl = record.getUrl();
    
    // 方式 1: 唤起浏览器下载
    DataExportManager.downloadFile(context, fileUrl);
    
    // 方式 2: 使用 DownloadManager
    DataExportManager.downloadWithDownloadManager(
        context,
        fileUrl,
        record.getName(),
        "下载文件"
    );
});
```

### 场景 4: 批量导出

```java
// 导出多种数据
Button btnExportAll = view.findViewById(R.id.btn_export_all);
btnExportAll.setOnClickListener(v -> {
    // 导出聊天记录
    DataExportManager.exportChatHistory(getContext(), baseUrl, "json");
    
    // 延迟一下，避免同时打开多个浏览器窗口
    new Handler().postDelayed(() -> {
        // 导出上传记录
        DataExportManager.exportUploadHistory(getContext(), baseUrl, "json");
    }, 1000);
    
    new Handler().postDelayed(() -> {
        // 导出用户数据
        DataExportManager.exportUserData(getContext(), baseUrl, userId, "json");
    }, 2000);
});
```

## 服务器端接口要求

### 1. 直接下载接口

服务器端应该支持直接下载，返回文件流：

```
GET /chat/export?format=json

响应头:
Content-Type: application/json
Content-Disposition: attachment; filename="chat_history.json"

响应体:
[文件内容]
```

### 2. 生成下载链接接口

服务器端生成临时下载链接：

```
GET /chat/export?format=json

响应:
{
  "code": 200,
  "msg": "OK",
  "url": "/download/temp/chat_history_123456.json"
}
```

客户端再访问返回的 URL 进行下载。

## 权限要求

### AndroidManifest.xml

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- 如果使用 DownloadManager，需要以下权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Android 10+ 需要 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

## 完整示例

### 在现有 Fragment 中添加导出功能

```java
public class ChatFragment extends Fragment {
    
    private ChatApiService apiService;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        apiService = new ChatApiService();
        
        // 添加导出按钮
        Button btnExport = view.findViewById(R.id.btn_export);
        btnExport.setOnClickListener(v -> showExportDialog());
        
        return view;
    }
    
    private void showExportDialog() {
        // 显示导出格式选择对话框
        String[] formats = {"JSON", "TXT", "CSV"};
        
        new AlertDialog.Builder(getContext())
            .setTitle("选择导出格式")
            .setItems(formats, (dialog, which) -> {
                String format = formats[which].toLowerCase();
                exportChatHistory(format);
            })
            .show();
    }
    
    private void exportChatHistory(String format) {
        Toast.makeText(getContext(), "正在准备导出...", Toast.LENGTH_SHORT).show();
        
        String exportUrl = apiService.getBaseUrl() + "/chat/export?format=" + format;
        DataExportManager.exportAndDownload(
            getContext(), 
            exportUrl, 
            "chat_history." + format
        );
    }
}
```

## 日志输出

导出时会输出详细日志：

```
========== 导出数据并下载 ==========
导出 URL: http://120.53.248.2:65002/chat/export?format=json
文件名: chat_history.json
✅ 成功唤起浏览器
```

使用 DownloadManager 时：

```
========== 使用 DownloadManager 下载 ==========
文件 URL: http://120.53.248.2:65002/files/report.pdf
文件名: report.pdf
✅ 下载任务已创建，ID: 12345
```

## 常见问题

### Q1: 浏览器没有打开？

**原因：** 设备上没有安装浏览器或浏览器被禁用

**解决：**
```java
try {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(exportUrl));
    
    if (intent.resolveActivity(context.getPackageManager()) != null) {
        context.startActivity(intent);
    } else {
        // 使用选择器
        Intent chooser = Intent.createChooser(intent, "选择浏览器");
        context.startActivity(chooser);
    }
} catch (Exception e) {
    Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show();
}
```

### Q2: 下载的文件在哪里？

**答：** 
- 使用浏览器下载：文件在浏览器的默认下载目录（通常是 `/sdcard/Download/`）
- 使用 DownloadManager：文件在系统下载目录，可以在通知栏查看

### Q3: 如何自定义下载位置？

**答：** 使用 DownloadManager 并指定路径：

```java
request.setDestinationInExternalPublicDir(
    Environment.DIRECTORY_DOWNLOADS,  // 或其他目录
    "custom_folder/filename.json"
);
```

### Q4: 如何显示下载进度？

**答：** 使用 DownloadManager 并监听下载进度：

```java
// 注册广播接收器监听下载完成
BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        // 下载完成
    }
};

context.registerReceiver(
    receiver, 
    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
);
```

## 总结

数据导出功能已完整实现，支持：

1. ✅ 唤起浏览器下载
2. ✅ 使用 DownloadManager 下载
3. ✅ 支持多种导出格式（JSON、TXT、CSV）
4. ✅ 支持参数化导出 URL
5. ✅ 预定义的导出方法（聊天记录、上传记录、用户数据）
6. ✅ 详细的日志输出
7. ✅ 错误处理和用户提示

使用非常简单，只需一行代码即可唤起浏览器下载：

```java
DataExportManager.exportAndDownload(context, exportUrl, fileName);
```
