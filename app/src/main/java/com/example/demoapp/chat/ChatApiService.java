package com.example.demoapp.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatApiService {
    
    private static final String BASE_URL = "http://120.53.248.2:65002";
    private static final String ONLINE_USERS_URL = BASE_URL + "/chat/users/online";
    private static final String UPLOAD_REGISTER_URL = BASE_URL + "/api/upload/register";
    private static final String UPLOAD_SAVE_URL = BASE_URL + "/api/upload/save";
    private static final String UPLOAD_MERGE_URL = BASE_URL + "/api/upload/merge";
    
    // 导出数据相关 URL
    private static final String EXPORT_CHAT_URL = BASE_URL + "/chat/export";
    private static final String EXPORT_UPLOAD_URL = BASE_URL + "/upload/export";
    private static final String EXPORT_USER_DATA_URL = BASE_URL + "/user/export";
    
    private OkHttpClient client;
    private Gson gson;
    
    public ChatApiService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    public interface OnlineUsersCallback {
        void onSuccess(List<OnlineUser> users);
        void onError(String error);
    }
    
    public void getOnlineUsers(OnlineUsersCallback callback) {
        Request request = new Request.Builder()
                .url(ONLINE_USERS_URL)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JsonObject result = gson.fromJson(json, JsonObject.class);
                        
                        if (result.has("code") && result.get("code").getAsInt() == 200) {
                            JsonObject data = result.getAsJsonObject("data");
                            if (data.has("users")) {
                                List<OnlineUser> users = new ArrayList<>();
                                data.getAsJsonArray("users").forEach(element -> {
                                    JsonObject userObj = element.getAsJsonObject();
                                    String username = userObj.get("username").getAsString();
                                    users.add(new OnlineUser(username));
                                });
                                callback.onSuccess(users);
                                return;
                            }
                        }
                        callback.onError("Invalid response format");
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError("Request failed: " + response.code());
                }
            }
        });
    }
    
    public interface UploadCallback {
        void onSuccess(String url);
        void onProgress(int progress);
        void onError(String error);
    }
    
    public void registerUpload(String fileId, String fileName, int totalChunks, UploadCallback callback) {
        try {
            Request request = new Request.Builder()
                    .url(UPLOAD_REGISTER_URL)
                    .post(RequestBody.create(new byte[0]))
                    .addHeader("X-File-Id", fileId)
                    .addHeader("X-File-Name", java.net.URLEncoder.encode(fileName, "UTF-8"))
                    .addHeader("X-Total-Chunks", String.valueOf(totalChunks))
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Register failed: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess("");
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        callback.onError("Register failed: " + response.code() + " - " + errorBody);
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Register error: " + e.getMessage());
        }
    }
    
    public void uploadChunk(String fileId, int chunkIndex, byte[] chunkData, UploadCallback callback) {
        RequestBody body = RequestBody.create(chunkData, MediaType.parse("application/octet-stream"));
        
        Request request = new Request.Builder()
                .url(UPLOAD_SAVE_URL)
                .post(body)
                .addHeader("X-File-Id", fileId)
                .addHeader("X-Chunk-Index", String.valueOf(chunkIndex))
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Upload chunk failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onError("Upload chunk failed: " + response.code() + " - " + errorBody);
                }
            }
        });
    }
    
    public void mergeFile(String fileId, UploadCallback callback) {
        Request request = new Request.Builder()
                .url(UPLOAD_MERGE_URL)
                .post(RequestBody.create(new byte[0]))
                .addHeader("X-File-Id", fileId)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Merge failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JsonObject result = gson.fromJson(json, JsonObject.class);
                        
                        if (result.has("url")) {
                            callback.onSuccess(result.get("url").getAsString());
                        } else {
                            callback.onError("No URL in response");
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError("Merge failed: " + response.code());
                }
            }
        });
    }
    
    /**
     * 获取导出数据的 URL
     * 
     * @param exportType 导出类型（chat, upload, user）
     * @param format 导出格式（json, txt, csv）
     * @return 导出 URL
     */
    public String getExportUrl(String exportType, String format) {
        String baseExportUrl;
        switch (exportType.toLowerCase()) {
            case "chat":
                baseExportUrl = EXPORT_CHAT_URL;
                break;
            case "upload":
                baseExportUrl = EXPORT_UPLOAD_URL;
                break;
            case "user":
                baseExportUrl = EXPORT_USER_DATA_URL;
                break;
            default:
                baseExportUrl = BASE_URL + "/export";
        }
        
        return baseExportUrl + "?format=" + format;
    }
    
    /**
     * 获取基础 URL
     */
    public String getBaseUrl() {
        return BASE_URL;
    }
    
    public interface ExportCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
    }
    
    /**
     * 请求导出数据（服务器生成下载链接）
     * 
     * @param exportType 导出类型
     * @param format 导出格式
     * @param callback 回调
     */
    public void requestExport(String exportType, String format, ExportCallback callback) {
        String url = getExportUrl(exportType, format);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("导出请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JsonObject result = gson.fromJson(json, JsonObject.class);
                        
                        if (result.has("code") && result.get("code").getAsInt() == 200) {
                            if (result.has("url")) {
                                String downloadUrl = result.get("url").getAsString();
                                callback.onSuccess(downloadUrl);
                            } else {
                                callback.onError("响应中没有下载链接");
                            }
                        } else {
                            String msg = result.has("msg") ? result.get("msg").getAsString() : "未知错误";
                            callback.onError("导出失败: " + msg);
                        }
                    } catch (Exception e) {
                        callback.onError("解析响应失败: " + e.getMessage());
                    }
                } else {
                    callback.onError("导出请求失败: " + response.code());
                }
            }
        });
    }
}
