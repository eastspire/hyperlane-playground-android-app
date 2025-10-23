package com.example.demoapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import com.example.demoapp.upload.FileUploadManager;
import com.example.demoapp.upload.UploadDatabase;
import com.example.demoapp.upload.UploadHistoryAdapter;
import com.example.demoapp.upload.UploadRecord;
import java.util.List;
import java.util.UUID;

public class UploadFragment extends Fragment {
    
    private static final int PICK_FILE_REQUEST = 2001;
    
    private RecyclerView recyclerView;
    private UploadHistoryAdapter adapter;
    private Button btnSelectFiles;
    private FileUploadManager uploadManager;
    private UploadDatabase database;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadUploadHistory();
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.upload_recycler_view);
        btnSelectFiles = view.findViewById(R.id.btn_select_files);
        Button btnClearHistory = view.findViewById(R.id.btn_clear_history);
        
        uploadManager = new FileUploadManager();
        database = new UploadDatabase(getContext());
        
        btnClearHistory.setOnClickListener(v -> {
            database.clearAll();
            loadUploadHistory();
            Toast.makeText(getContext(), "历史记录已清除", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupRecyclerView() {
        adapter = new UploadHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnSelectFiles.setOnClickListener(v -> openFilePicker());
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Files"), PICK_FILE_REQUEST);
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
        if (getContext() == null) return;
        
        String fileId = UUID.randomUUID().toString();
        
        // 创建初始记录
        UploadRecord record = new UploadRecord();
        record.setId(fileId);
        record.setName(getFileName(fileUri));
        record.setSize(getFileSize(fileUri));
        record.setProgress(0);
        record.setUrl("");
        record.setUploadTime(System.currentTimeMillis());
        
        database.insertOrUpdate(record);
        loadUploadHistory();
        
        // 开始上传
        uploadManager.uploadFile(getContext(), fileUri, fileId, new FileUploadManager.UploadCallback() {
            @Override
            public void onProgress(int progress) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        record.setProgress(progress);
                        database.insertOrUpdate(record);
                        loadUploadHistory();
                    });
                }
            }
            
            @Override
            public void onSuccess(String url, String fileName) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.d("UploadFragment", "上传成功回调 - URL: " + url + ", 文件名: " + fileName);
                        record.setProgress(100);
                        record.setUrl(url);
                        record.setName(fileName);
                        database.insertOrUpdate(record);
                        loadUploadHistory();
                        Toast.makeText(getContext(), "上传成功: " + fileName, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "上传失败: " + error, Toast.LENGTH_SHORT).show();
                        database.delete(fileId);
                        loadUploadHistory();
                    });
                }
            }
        });
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
    
    private void loadUploadHistory() {
        if (getContext() == null) return;
        
        List<UploadRecord> records = database.getAllRecords();
        adapter.setRecords(records);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (uploadManager != null) {
            uploadManager.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }
}
