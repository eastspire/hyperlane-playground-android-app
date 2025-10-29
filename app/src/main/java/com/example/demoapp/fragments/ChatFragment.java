package com.example.demoapp.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import com.example.demoapp.chat.ChatAdapter;
import com.example.demoapp.chat.ChatMessage;
import com.example.demoapp.chat.WebSocketManager;
import com.example.demoapp.log.NativeLogManager;
import com.example.demoapp.utils.UUIDHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    
    private static final int PICK_FILE_REQUEST = 1001;
    private static final String TAG = "ChatFragment";
    
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText inputMessage;
    private ImageButton btnSend;
    private FloatingActionButton fabScrollBottom;
    private TextView tvConnectionStatus;
    private View connectionIndicator;
    
    private WebSocketManager webSocketManager;
    private List<ChatMessage> messages = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isNearBottom = true;
    private int unreadCount = 0;
    
    // History loading
    private com.example.demoapp.chat.ChatApiService apiService;
    private Long beforeId = null;
    private int historyLimit = 20;
    private boolean loadingHistory = false;
    private boolean hasMoreHistory = true;
    private View historyLoadingIndicator;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupWebSocket();
        setupListeners();
        setupApiService();
        
        // 首次进入加载历史消息
        loadHistory();
    }
    
    private void setupApiService() {
        apiService = new com.example.demoapp.chat.ChatApiService();
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.chat_recycler_view);
        inputMessage = view.findViewById(R.id.input_message);
        btnSend = view.findViewById(R.id.btn_send);
        fabScrollBottom = view.findViewById(R.id.fab_scroll_bottom);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        connectionIndicator = view.findViewById(R.id.connection_indicator);
        
        NativeLogManager.getInstance().i(TAG, "ChatFragment 初始化完成");
    }
    
    private void setupRecyclerView() {
        adapter = new ChatAdapter(messages, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScrollPosition();
                
                // 检测是否滚动到顶部，触发加载历史消息
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findFirstVisibleItemPosition() == 0) {
                    if (!loadingHistory && hasMoreHistory) {
                        loadHistory();
                    }
                }
            }
        });
    }
    
    private void setupWebSocket() {
        webSocketManager = new WebSocketManager(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {
                mainHandler.post(() -> {
                    updateConnectionStatus("connected");
                    NativeLogManager.getInstance().i(TAG, "WebSocket 连接成功");
                });
            }
            
            @Override
            public void onDisconnected() {
                mainHandler.post(() -> {
                    updateConnectionStatus("disconnected");
                    NativeLogManager.getInstance().e(TAG, "WebSocket 连接断开");
                });
            }
            
            @Override
            public void onMessageReceived(ChatMessage message) {
                mainHandler.post(() -> {
                    messages.add(message);
                    adapter.notifyItemInserted(messages.size() - 1);
                    
                    NativeLogManager.getInstance().d(TAG, "收到消息: " + message.getData());
                    
                    if (isNearBottom || message.isSelf() || message.isGptResponse()) {
                        scrollToBottom();
                    } else {
                        unreadCount++;
                        updateScrollButton();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    String errorMsg = (error != null && !error.isEmpty()) ? error : "发生错误";
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    NativeLogManager.getInstance().e(TAG, "WebSocket 错误: " + errorMsg);
                });
            }
            
            @Override
            public void onOnlineCountUpdate(String count) {
                mainHandler.post(() -> {
                    updatePlaceholder(count);
                    NativeLogManager.getInstance().d(TAG, "在线人数更新: " + count);
                });
            }
        });
        
        webSocketManager.connect();
        NativeLogManager.getInstance().i(TAG, "开始连接 WebSocket");
    }
    

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        
        fabScrollBottom.setOnClickListener(v -> scrollToBottom());
        
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                btnSend.setEnabled(s.length() > 0 && webSocketManager.isConnected());
            }
        });
    }
    
    private void sendMessage() {
        String message = inputMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!webSocketManager.isConnected()) {
            Toast.makeText(getContext(), "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        webSocketManager.sendTextMessage(message);
        inputMessage.setText("");
        isNearBottom = true;
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri fileUri = data.getClipData().getItemAt(i).getUri();
                    uploadFile(fileUri);
                }
            } else if (data.getData() != null) {
                Uri fileUri = data.getData();
                uploadFile(fileUri);
            }
        }
    }
    
    private void uploadFile(Uri fileUri) {
        try {
            if (getContext() == null) return;
            
            android.content.ContentResolver resolver = getContext().getContentResolver();
            java.io.InputStream inputStream = resolver.openInputStream(fileUri);
            
            if (inputStream == null) {
                Toast.makeText(getContext(), "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取文件名
            String fileName = getFileName(fileUri);
            
            // 获取文件大小
            long fileSize = getFileSize(fileUri);
            
            if (fileSize <= 0) {
                Toast.makeText(getContext(), "文件大小无效", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }
            
            Toast.makeText(getContext(), "开始上传: " + fileName, Toast.LENGTH_SHORT).show();
            
            // 在后台线程执行文件上传
            new Thread(() -> {
                try {
                    uploadFileInChunks(inputStream, fileName, fileSize);
                } catch (Exception e) {
                    mainHandler.post(() -> 
                        Toast.makeText(getContext(), "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "文件处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getFileName(Uri uri) {
        String fileName = "unknown";
        if (getContext() == null) return fileName;
        
        android.database.Cursor cursor = getContext().getContentResolver().query(
            uri, null, null, null, null
        );
        
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        
        if ("unknown".equals(fileName)) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }
        
        return fileName;
    }
    
    private long getFileSize(Uri uri) {
        long size = 0;
        if (getContext() == null) return size;
        
        android.database.Cursor cursor = getContext().getContentResolver().query(
            uri, null, null, null, null
        );
        
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        
        return size;
    }
    
    private void uploadFileInChunks(java.io.InputStream inputStream, String fileName, long fileSize) throws Exception {
        // 文件上传已改为使用 HTTP API，不再通过 WebSocket 发送分块
        // 请使用 ChatFragmentNew.java 中的实现
        mainHandler.post(() -> 
            Toast.makeText(getContext(), "请使用新版本的文件上传功能", Toast.LENGTH_SHORT).show()
        );
    }
    
    private void scrollToBottom() {
        if (messages.size() > 0) {
            recyclerView.smoothScrollToPosition(messages.size() - 1);
            unreadCount = 0;
            updateScrollButton();
        }
    }
    
    private void checkScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = layoutManager.getItemCount();
            isNearBottom = (totalItemCount - lastVisiblePosition) <= 3;
            
            if (isNearBottom) {
                fabScrollBottom.hide();
                unreadCount = 0;
            } else {
                fabScrollBottom.show();
            }
        }
    }
    
    private void updateScrollButton() {
        if (unreadCount > 0 && !isNearBottom) {
            fabScrollBottom.show();
        } else if (isNearBottom) {
            fabScrollBottom.hide();
        }
    }
    
    private void updateConnectionStatus(String status) {
        switch (status) {
            case "connected":
                tvConnectionStatus.setText("Online");
                tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                connectionIndicator.setBackgroundResource(R.drawable.status_dot_connected);
                inputMessage.setEnabled(true);
                btnSend.setEnabled(inputMessage.getText().length() > 0);
                break;
            case "connecting":
                tvConnectionStatus.setText("Connecting");
                tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                connectionIndicator.setBackgroundResource(R.drawable.status_dot_connecting);
                inputMessage.setEnabled(false);
                btnSend.setEnabled(false);
                break;
            case "disconnected":
                tvConnectionStatus.setText("Offline");
                tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                connectionIndicator.setBackgroundResource(R.drawable.status_dot_disconnected);
                inputMessage.setEnabled(false);
                btnSend.setEnabled(false);
                break;
        }
    }
    
    private void updatePlaceholder(String onlineCount) {
        String placeholder = onlineCount + " (use @name to mention users)";
        inputMessage.setHint(placeholder);
    }
    
    /**
     * 加载历史消息
     */
    private void loadHistory() {
        if (loadingHistory || !hasMoreHistory) {
            return;
        }
        
        loadingHistory = true;
        showLoadingIndicator();
        
        String sessionId = UUIDHelper.getUUID();
        apiService.getChatHistory(sessionId, beforeId, historyLimit, new com.example.demoapp.chat.ChatApiService.HistoryCallback() {
            @Override
            public void onSuccess(List<ChatMessage> historyMessages, boolean hasMore) {
                mainHandler.post(() -> {
                    loadingHistory = false;
                    hideLoadingIndicator();
                    
                    if (!historyMessages.isEmpty()) {
                        // 获取当前第一个可见项的位置，用于保持滚动位置
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        int firstVisiblePosition = layoutManager != null ? layoutManager.findFirstVisibleItemPosition() : 0;
                        View firstVisibleView = layoutManager != null ? layoutManager.findViewByPosition(firstVisiblePosition) : null;
                        int offsetTop = firstVisibleView != null ? firstVisibleView.getTop() : 0;
                        
                        // 设置消息的isSelf属性
                        String currentUuid = UUIDHelper.getUUID();
                        for (ChatMessage msg : historyMessages) {
                            msg.setSelf(msg.getName().equals(currentUuid));
                        }
                        
                        // 在列表开头插入历史消息
                        messages.addAll(0, historyMessages);
                        adapter.notifyItemRangeInserted(0, historyMessages.size());
                        
                        // 更新beforeId为最旧消息的ID
                        beforeId = historyMessages.get(0).getId();
                        hasMoreHistory = hasMore;
                        
                        // 恢复滚动位置
                        if (layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(firstVisiblePosition + historyMessages.size(), offsetTop);
                        }
                        
                        NativeLogManager.getInstance().d(TAG, "加载了 " + historyMessages.size() + " 条历史消息");
                    } else {
                        hasMoreHistory = false;
                        NativeLogManager.getInstance().d(TAG, "没有更多历史消息");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingHistory = false;
                    hideLoadingIndicator();
                    Toast.makeText(getContext(), "加载历史消息失败: " + error, Toast.LENGTH_SHORT).show();
                    NativeLogManager.getInstance().e(TAG, "加载历史消息失败: " + error);
                });
            }
        });
    }
    
    /**
     * 显示加载指示器
     */
    private void showLoadingIndicator() {
        if (historyLoadingIndicator == null && getView() != null) {
            // 创建加载指示器
            historyLoadingIndicator = LayoutInflater.from(getContext()).inflate(R.layout.item_loading_history, null);
            
            // 将指示器添加到RecyclerView的父容器顶部
            ViewGroup parent = (ViewGroup) recyclerView.getParent();
            if (parent != null) {
                parent.addView(historyLoadingIndicator, 0);
            }
        }
        
        if (historyLoadingIndicator != null) {
            historyLoadingIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏加载指示器
     */
    private void hideLoadingIndicator() {
        if (historyLoadingIndicator != null) {
            historyLoadingIndicator.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
        
        // 清理加载指示器
        if (historyLoadingIndicator != null && historyLoadingIndicator.getParent() != null) {
            ((ViewGroup) historyLoadingIndicator.getParent()).removeView(historyLoadingIndicator);
            historyLoadingIndicator = null;
        }
        
        NativeLogManager.getInstance().i(TAG, "ChatFragment 销毁");
    }
}
