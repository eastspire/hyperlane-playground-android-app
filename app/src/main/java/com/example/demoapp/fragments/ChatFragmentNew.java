package com.example.demoapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import com.example.demoapp.chat.*;
import com.example.demoapp.utils.UUIDHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatFragment - 完全对齐 HTML 版本的聊天界面逻辑
 * 主要功能：
 * 1. @提及功能 - 输入@时显示在线用户列表
 * 2. 在线用户列表 - 从API获取
 * 3. 键盘导航 - 支持上下箭头、Enter、Escape
 * 4. HTTP文件上传 - 使用register/save/merge API
 * 5. Markdown渲染 - 支持代码高亮
 * 6. 提及高亮 - 自己被提及时特殊显示
 */
public class ChatFragmentNew extends Fragment {
    
    private static final int PICK_FILE_REQUEST = 1001;
    private static final int CHUNK_SIZE = 512 * 1024; // 512KB per chunk
    
    // UI Components
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText inputMessage;
    private ImageButton btnSend;
    private FloatingActionButton fabScrollBottom;
    private TextView tvConnectionStatus;
    private View connectionIndicator;
    
    // Mention Dropdown
    private PopupWindow mentionPopup;
    private RecyclerView mentionRecyclerView;
    private MentionAdapter mentionAdapter;
    private boolean showMentionDropdown = false;
    private int mentionStartIndex = -1;
    private String mentionFilter = "";
    
    // Services
    private WebSocketManager webSocketManager;
    private ChatApiService apiService;
    
    // Data
    private List<ChatMessage> messages = new ArrayList<>();
    private List<OnlineUser> onlineUsers = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isNearBottom = true;
    private int unreadCount = 0;
    private boolean loadingUsers = false;
    
    // History loading
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
        setupApiService();
        setupListeners();
        setupMentionDropdown();
        
        // 首次进入加载历史消息
        loadHistory();
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
                mainHandler.post(() -> Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onOnlineCountUpdate(String count) {
                mainHandler.post(() -> updatePlaceholder(count));
            }
        });
        
        webSocketManager.connect();
    }
    
    private void setupApiService() {
        apiService = new ChatApiService();
    }
    
    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        
        fabScrollBottom.setOnClickListener(v -> scrollToBottom());
        
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkMentionTrigger();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                btnSend.setEnabled(s.length() > 0 && webSocketManager.isConnected());
            }
        });
        
        // Handle keyboard events for mention dropdown
        inputMessage.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && showMentionDropdown) {
                return handleMentionKeyDown(keyCode);
            }
            return false;
        });
    }
    
    private void setupMentionDropdown() {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_mention, null);
        mentionRecyclerView = popupView.findViewById(R.id.mention_recycler_view);
        
        mentionAdapter = new MentionAdapter(user -> {
            selectMentionUser(user);
        });
        
        mentionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mentionRecyclerView.setAdapter(mentionAdapter);
        
        mentionPopup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false);
    }
    
    private void checkMentionTrigger() {
        int cursorPos = inputMessage.getSelectionStart();
        String text = inputMessage.getText().toString();
        
        int atIndex = -1;
        for (int i = cursorPos - 1; i >= 0; i--) {
            if (text.charAt(i) == '@') {
                atIndex = i;
                break;
            }
            if (text.charAt(i) == ' ' || text.charAt(i) == '\n') {
                break;
            }
        }
        
        if (atIndex != -1) {
            String filterText = text.substring(atIndex + 1, cursorPos);
            mentionFilter = filterText;
            mentionStartIndex = atIndex;
            
            if (!showMentionDropdown) {
                showMentionDropdown = true;
                fetchOnlineUsers();
            } else {
                mentionAdapter.filter(mentionFilter);
            }
            
            showMentionPopup();
        } else {
            closeMentionDropdown();
        }
    }
    
    private void showMentionPopup() {
        if (mentionPopup != null && !mentionPopup.isShowing()) {
            mentionPopup.showAsDropDown(inputMessage, 0, -inputMessage.getHeight());
        }
    }
    
    private void fetchOnlineUsers() {
        if (loadingUsers) return;
        
        loadingUsers = true;
        apiService.getOnlineUsers(new ChatApiService.OnlineUsersCallback() {
            @Override
            public void onSuccess(List<OnlineUser> users) {
                mainHandler.post(() -> {
                    loadingUsers = false;
                    onlineUsers = users;
                    mentionAdapter.setUsers(users);
                    mentionAdapter.filter(mentionFilter);
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingUsers = false;
                    Toast.makeText(getContext(), "Failed to load users: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private boolean handleMentionKeyDown(int keyCode) {
        if (!showMentionDropdown) return false;
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                int currentPos = mentionAdapter.getSelectedPosition();
                mentionAdapter.setSelectedPosition(currentPos + 1);
                return true;
                
            case KeyEvent.KEYCODE_DPAD_UP:
                int pos = mentionAdapter.getSelectedPosition();
                mentionAdapter.setSelectedPosition(pos - 1);
                return true;
                
            case KeyEvent.KEYCODE_ENTER:
                OnlineUser selectedUser = mentionAdapter.getSelectedUser();
                if (selectedUser != null) {
                    selectMentionUser(selectedUser);
                }
                return true;
                
            case KeyEvent.KEYCODE_ESCAPE:
                closeMentionDropdown();
                return true;
        }
        
        return false;
    }
    
    private void selectMentionUser(OnlineUser user) {
        String text = inputMessage.getText().toString();
        String beforeMention = text.substring(0, mentionStartIndex);
        String afterMention = text.substring(inputMessage.getSelectionStart());
        
        String newText = beforeMention + "@" + user.getUsername() + " " + afterMention;
        inputMessage.setText(newText);
        
        int newCursorPos = beforeMention.length() + user.getUsername().length() + 2;
        inputMessage.setSelection(newCursorPos);
        
        closeMentionDropdown();
    }
    
    private void closeMentionDropdown() {
        showMentionDropdown = false;
        mentionFilter = "";
        mentionStartIndex = -1;
        
        if (mentionPopup != null && mentionPopup.isShowing()) {
            mentionPopup.dismiss();
        }
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
        closeMentionDropdown();
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
                    uploadFileViaHttp(fileUri);
                }
            } else if (data.getData() != null) {
                Uri fileUri = data.getData();
                uploadFileViaHttp(fileUri);
            }
        }
    }
    
    private void uploadFileViaHttp(Uri fileUri) {
        new Thread(() -> {
            try {
                if (getContext() == null) return;
                
                InputStream inputStream = getContext().getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    mainHandler.post(() -> Toast.makeText(getContext(), "Cannot read file", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                String fileName = getFileName(fileUri);
                long fileSize = getFileSize(fileUri);
                
                if (fileSize <= 0 || fileSize > 10 * 1024 * 1024) { // 10MB limit
                    mainHandler.post(() -> Toast.makeText(getContext(), "File size invalid or too large (max 10MB)", Toast.LENGTH_SHORT).show());
                    inputStream.close();
                    return;
                }
                
                String fileId = java.util.UUID.randomUUID().toString();
                int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                
                mainHandler.post(() -> Toast.makeText(getContext(), "Uploading: " + fileName, Toast.LENGTH_SHORT).show());
                
                final boolean[] registerSuccess = {false};
                final boolean[] uploadSuccess = {true};
                final String[] finalUrl = {null};
                
                // Step 1: Register upload
                final Object registerLock = new Object();
                apiService.registerUpload(fileId, fileName, totalChunks, new ChatApiService.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        synchronized (registerLock) {
                            registerSuccess[0] = true;
                            registerLock.notify();
                        }
                    }
                    
                    @Override
                    public void onProgress(int progress) {}
                    
                    @Override
                    public void onError(String error) {
                        synchronized (registerLock) {
                            registerSuccess[0] = false;
                            registerLock.notify();
                        }
                        mainHandler.post(() -> Toast.makeText(getContext(), "Register failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
                
                // Wait for registration
                synchronized (registerLock) {
                    registerLock.wait(5000); // 5 second timeout
                }
                
                if (!registerSuccess[0]) {
                    inputStream.close();
                    return;
                }
                
                // Step 2: Upload chunks sequentially
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int chunkIndex = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1 && uploadSuccess[0]) {
                    byte[] chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                    
                    final int currentChunk = chunkIndex;
                    final int progress = (int) (((currentChunk + 1) * 100) / totalChunks);
                    final Object chunkLock = new Object();
                    final boolean[] chunkSuccess = {false};
                    
                    apiService.uploadChunk(fileId, chunkIndex, chunkData, new ChatApiService.UploadCallback() {
                        @Override
                        public void onSuccess(String url) {
                            synchronized (chunkLock) {
                                chunkSuccess[0] = true;
                                chunkLock.notify();
                            }
                            if (currentChunk % 5 == 0 || currentChunk == totalChunks - 1) {
                                mainHandler.post(() -> Toast.makeText(getContext(), "Progress: " + progress + "%", Toast.LENGTH_SHORT).show());
                            }
                        }
                        
                        @Override
                        public void onProgress(int p) {}
                        
                        @Override
                        public void onError(String error) {
                            synchronized (chunkLock) {
                                chunkSuccess[0] = false;
                                chunkLock.notify();
                            }
                            mainHandler.post(() -> Toast.makeText(getContext(), "Chunk " + currentChunk + " failed: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                    
                    // Wait for chunk upload
                    synchronized (chunkLock) {
                        chunkLock.wait(10000); // 10 second timeout per chunk
                    }
                    
                    if (!chunkSuccess[0]) {
                        uploadSuccess[0] = false;
                        break;
                    }
                    
                    chunkIndex++;
                    Thread.sleep(50); // Small delay between chunks
                }
                
                inputStream.close();
                
                if (!uploadSuccess[0]) {
                    mainHandler.post(() -> Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // Step 3: Merge file
                final Object mergeLock = new Object();
                final boolean[] mergeSuccess = {false};
                
                apiService.mergeFile(fileId, new ChatApiService.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        synchronized (mergeLock) {
                            mergeSuccess[0] = true;
                            finalUrl[0] = url;
                            mergeLock.notify();
                        }
                    }
                    
                    @Override
                    public void onProgress(int progress) {}
                    
                    @Override
                    public void onError(String error) {
                        synchronized (mergeLock) {
                            mergeSuccess[0] = false;
                            mergeLock.notify();
                        }
                        mainHandler.post(() -> Toast.makeText(getContext(), "Merge failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
                
                // Wait for merge
                synchronized (mergeLock) {
                    mergeLock.wait(10000); // 10 second timeout
                }
                
                if (mergeSuccess[0] && finalUrl[0] != null) {
                    mainHandler.post(() -> {
                        Toast.makeText(getContext(), "Upload complete!", Toast.LENGTH_SHORT).show();
                        // Send file link as Markdown message
                        String fileMessage = "[" + fileName + "](" + finalUrl[0] + ")";
                        webSocketManager.sendTextMessage(fileMessage);
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(getContext(), "Upload failed at merge step", Toast.LENGTH_SHORT).show());
                }
                
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private String getFileName(Uri uri) {
        String fileName = "unknown";
        if (getContext() == null) return fileName;
        
        android.database.Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
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
        return fileName;
    }
    
    private long getFileSize(Uri uri) {
        long size = 0;
        if (getContext() == null) return size;
        
        android.database.Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
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
        apiService.getChatHistory(sessionId, beforeId, historyLimit, new ChatApiService.HistoryCallback() {
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
                    } else {
                        hasMoreHistory = false;
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingHistory = false;
                    hideLoadingIndicator();
                    Toast.makeText(getContext(), "加载历史消息失败: " + error, Toast.LENGTH_SHORT).show();
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
        closeMentionDropdown();
        
        // 清理加载指示器
        if (historyLoadingIndicator != null && historyLoadingIndicator.getParent() != null) {
            ((ViewGroup) historyLoadingIndicator.getParent()).removeView(historyLoadingIndicator);
            historyLoadingIndicator = null;
        }
    }
}
