package com.example.demoapp.chat;

import android.os.Handler;
import android.os.Looper;
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
        Request request = new Request.Builder()
                .url(WS_URL + uuid)
                .build();
        
        webSocket = client.newWebSocket(request, new okhttp3.WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                reconnectAttempts = 0;
                listener.onConnected();
                startPing();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                stopPing();
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                listener.onDisconnected();
                reconnect();
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                isConnected = false;
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
                return;
            }
            
            if ("OnlineCount".equals(type)) {
                String data = json.has("data") ? json.get("data").getAsString() : "";
                listener.onOnlineCountUpdate(data);
                return;
            }
            
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String data = json.has("data") ? json.get("data").getAsString() : "";
            String time = json.has("time") ? json.get("time").getAsString() : "";
            
            ChatMessage message = new ChatMessage(type, name, data, time);
            message.setSelf(name.equals(UUIDHelper.getUUID()));
            
            listener.onMessageReceived(message);
        } catch (Exception e) {
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
                    android.util.Log.w("WebSocketManager", "Ping send failed");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WebSocketManager", "Error sending ping", e);
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
                    android.util.Log.w("WebSocketManager", "Large message: " + message.length() + " bytes");
                }
                boolean result = webSocket.send(message);
                if (!result) {
                    android.util.Log.e("WebSocketManager", "Failed to send message");
                }
                return result;
            } catch (Exception e) {
                android.util.Log.e("WebSocketManager", "Exception sending message", e);
                return false;
            }
        }
        android.util.Log.w("WebSocketManager", "Cannot send: webSocket=" + (webSocket != null) + ", connected=" + isConnected);
        return false;
    }
    
    private void reconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            handler.postDelayed(() -> connect(), RECONNECT_INTERVAL);
        }
    }
    
    public void disconnect() {
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
