package com.example.demoapp.chat;

import android.os.Handler;
import android.os.Looper;
import com.example.demoapp.log.NativeLogManager;
import com.example.demoapp.utils.UUIDHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.util.concurrent.TimeUnit;

public class WebSocketManager {
    
    private static final String TAG = "WebSocketManager";
    private static final String WS_URL = "ws://120.53.248.2:65002/api/chat?uuid=";
    private static final long PING_INTERVAL = 20000; // 20秒发送一次ping
    private static final long RECONNECT_INTERVAL = 3000;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private Gson gson;
    private WebSocketListener listener;
    private Handler handler;
    private Runnable pingRunnable;
    private int reconnectAttempts = 0;
    private boolean isConnected = false;
    
    public interface WebSocketListener {
        void onConnected();
        void onDisconnected();
        void onMessageReceived(ChatMessage message);
        void onError(String error);
        void onOnlineCountUpdate(String count);
    }
    
    public WebSocketManager(WebSocketListener listener) {
        this.listener = listener;
        this.gson = new Gson();
        this.handler = new Handler(Looper.getMainLooper());
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // 0表示无限制
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    public void connect() {
        String uuid = UUIDHelper.getUUID();
        String url = WS_URL + uuid;
        NativeLogManager.getInstance().i(TAG, "开始连接 WebSocket: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        webSocket = client.newWebSocket(request, new okhttp3.WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                reconnectAttempts = 0;
                NativeLogManager.getInstance().i(TAG, "WebSocket 连接成功");
                listener.onConnected();
                startPing();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                NativeLogManager.getInstance().d(TAG, "收到消息: " + text.substring(0, Math.min(100, text.length())));
                handleMessage(text);
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                NativeLogManager.getInstance().i(TAG, "WebSocket 正在关闭: code=" + code + ", reason=" + reason);
                stopPing();
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                NativeLogManager.getInstance().e(TAG, "WebSocket 已关闭: code=" + code + ", reason=" + reason);
                listener.onDisconnected();
                reconnect();
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                isConnected = false;
                NativeLogManager.getInstance().e(TAG, "WebSocket 连接失败: " + t.getMessage());
                listener.onError(t.getMessage());
                listener.onDisconnected();
                reconnect();
            }
        });
    }
    
    private void handleMessage(String text) {
        try {
            JsonObject json = gson.fromJson(text, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";
            
            if ("Pang".equals(type)) {
                NativeLogManager.getInstance().d(TAG, "收到 Pang 响应");
                return;
            }
            
            if ("OnlineCount".equals(type)) {
                String data = json.has("data") ? json.get("data").getAsString() : "";
                NativeLogManager.getInstance().i(TAG, "在线人数更新: " + data);
                listener.onOnlineCountUpdate(data);
                return;
            }
            
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String data = json.has("data") ? json.get("data").getAsString() : "";
            String time = json.has("time") ? json.get("time").getAsString() : "";
            
            NativeLogManager.getInstance().d(TAG, "处理消息: type=" + type + ", name=" + name);
            
            ChatMessage message = new ChatMessage(type, name, data, time);
            message.setSelf(name.equals(UUIDHelper.getUUID()));
            
            listener.onMessageReceived(message);
        } catch (Exception e) {
            NativeLogManager.getInstance().e(TAG, "解析消息失败: " + e.getMessage());
            listener.onError("Failed to parse message: " + e.getMessage());
        }
    }
    
    private void startPing() {
        stopPing();
        pingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isConnected && webSocket != null) {
                    sendPing();
                    handler.postDelayed(this, PING_INTERVAL);
                }
            }
        };
        handler.postDelayed(pingRunnable, PING_INTERVAL);
    }
    
    private void stopPing() {
        if (pingRunnable != null) {
            handler.removeCallbacks(pingRunnable);
            pingRunnable = null;
        }
    }
    
    private void sendPing() {
        try {
            if (webSocket != null && isConnected) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "Ping");
                json.addProperty("data", "");
                boolean sent = webSocket.send(json.toString());
                if (!sent) {
                    NativeLogManager.getInstance().e(TAG, "Ping 发送失败");
                } else {
                    NativeLogManager.getInstance().d(TAG, "Ping 发送成功");
                }
            }
        } catch (Exception e) {
            NativeLogManager.getInstance().e(TAG, "发送 Ping 异常: " + e.getMessage());
        }
    }
    
    public void sendTextMessage(String text) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "Text");
        json.addProperty("data", text);
        sendMessage(json.toString());
    }
    
    public void sendFileMessage(String fileUrl, String fileName) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "File");
        json.addProperty("data", fileUrl);
        json.addProperty("fileName", fileName);
        sendMessage(json.toString());
    }
    
    public void sendMarkdownMessage(String markdown) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "Markdown");
        json.addProperty("data", markdown);
        sendMessage(json.toString());
    }
    
    private boolean sendMessage(String message) {
        if (webSocket != null && isConnected) {
            try {
                // 检查消息大小，如果过大则警告
                if (message.length() > 50000) {
                    NativeLogManager.getInstance().e(TAG, "消息过大: " + message.length() + " bytes");
                }
                boolean result = webSocket.send(message);
                if (!result) {
                    NativeLogManager.getInstance().e(TAG, "消息发送失败");
                } else {
                    NativeLogManager.getInstance().i(TAG, "消息发送成功: " + message.substring(0, Math.min(50, message.length())));
                }
                return result;
            } catch (Exception e) {
                NativeLogManager.getInstance().e(TAG, "发送消息异常: " + e.getMessage());
                return false;
            }
        }
        NativeLogManager.getInstance().e(TAG, "无法发送消息: webSocket=" + (webSocket != null) + ", connected=" + isConnected);
        return false;
    }
    
    private void reconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            NativeLogManager.getInstance().i(TAG, "尝试重连 (" + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");
            handler.postDelayed(() -> connect(), RECONNECT_INTERVAL);
        } else {
            NativeLogManager.getInstance().e(TAG, "达到最大重连次数，停止重连");
        }
    }
    
    public void disconnect() {
        NativeLogManager.getInstance().i(TAG, "主动断开 WebSocket 连接");
        stopPing();
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        isConnected = false;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
}
