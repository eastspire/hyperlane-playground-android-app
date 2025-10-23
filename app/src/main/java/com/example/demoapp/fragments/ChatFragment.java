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
import com.example.demoapp.utils.UUIDHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    
    private static final int PICK_FILE_REQUEST = 1001;
    
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
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.chat_recycler_view);
        inputMessage = view.findViewById(R.id.input_message);
        btnSend = view.findViewById(R.id.btn_send);
        fabScrollBottom = view.findViewById(R.id.fab_scroll_bottom);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        connectionIndicator = view.findViewById(R.id.connection_indicator);
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
            }
        });
    }
    
    private void setupWebSocket() {
        webSocketManager = new WebSocketManager(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {
                mainHandler.post(() -> updateConnectionStatus("connected"));
            }
            
            @Override
            public void onDisconnected() {
                mainHandler.post(() -> updateConnectionStatus("disconnected"));
            }
            
            @Override
            public void onMessageReceived(ChatMessage message) {
                mainHandler.post(() -> {
                    messages.add(message);
                    adapter.notifyItemInserted(messages.size() - 1);
                    
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
                });
            }
            
            @Override
            public void onOnlineCountUpdate(String count) {
                mainHandler.post(() -> updatePlaceholder(count));
            }
        });
        
        webSocketManager.connect();
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
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }
}
