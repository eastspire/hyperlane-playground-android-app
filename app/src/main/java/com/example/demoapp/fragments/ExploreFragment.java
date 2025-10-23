package com.example.demoapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

public class ExploreFragment extends Fragment {
    private RecyclerView rvHistory;
    private TextView tvStatus;
    private Button btnSelectFiles, btnExport, btnImport;
    private UploadHistoryAdapter adapter;
    private UploadDatabase database;
    private FileUploadManager uploadManager;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleFileSelection(result.getData());
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleImport(result.getData().getData());
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupButtons();
        loadHistory();
        
        return view;
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rvHistory);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnSelectFiles = view.findViewById(R.id.btnSelectFiles);
        btnExport = view.findViewById(R.id.btnExport);
        btnImport = view.findViewById(R.id.btnImport);
        
        database = new UploadDatabase(requireContext());
        uploadManager = new FileUploadManager();
        adapter = new UploadHistoryAdapter();
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);
    }

    private void setupButtons() {
        btnSelectFiles.setOnClickListener(v -> openFilePicker());
        btnExport.setOnClickListener(v -> exportData());
        btnImport.setOnClickListener(v -> openImportPicker());
    }

    private void openFilePicker() {
        android.util.Log.d("ExploreFragment", "打开文件选择器");
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(intent, "选择文件"));
        } catch (Exception e) {
            android.util.Log.e("ExploreFragment", "打开文件选择器失败", e);
            Toast.makeText(requireContext(), "打开文件选择器失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openImportPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        importLauncher.launch(Intent.createChooser(intent, "选择导入文件"));
    }

    private void handleFileSelection(Intent data) {
        android.util.Log.d("ExploreFragment", "handleFileSelection called");
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            android.util.Log.d("ExploreFragment", "选择了 " + count + " 个文件");
            for (int i = 0; i < count; i++) {
                Uri fileUri = data.getClipData().getItemAt(i).getUri();
                android.util.Log.d("ExploreFragment", "文件 " + i + ": " + fileUri);
                uploadFile(fileUri);
            }
        } else if (data.getData() != null) {
            Uri fileUri = data.getData();
            android.util.Log.d("ExploreFragment", "选择了单个文件: " + fileUri);
            uploadFile(fileUri);
        } else {
            android.util.Log.e("ExploreFragment", "没有选择文件");
            Toast.makeText(requireContext(), "没有选择文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFile(Uri fileUri) {
        String fileId = UUID.randomUUID().toString();
        String fileName = getFileName(fileUri);
        long fileSize = getFileSize(fileUri);

        Toast.makeText(requireContext(), "开始上传: " + fileName, Toast.LENGTH_SHORT).show();
        
        UploadRecord record = new UploadRecord(fileId, fileName, fileSize, 0, "", System.currentTimeMillis());
        database.saveUploadRecord(record);
        loadHistory();

        uploadManager.uploadFile(requireContext(), fileUri, fileId, new FileUploadManager.UploadCallback() {
            @Override
            public void onProgress(int progress) {
                android.util.Log.d("ExploreFragment", "上传进度: " + progress + "%");
                record.setProgress(progress);
                database.saveUploadRecord(record);
                loadHistory();
            }

            @Override
            public void onSuccess(String url, String serverFileName) {
                android.util.Log.d("ExploreFragment", "上传成功: " + url);
                record.setProgress(100);
                record.setUrl(url);
                record.setName(serverFileName != null && !serverFileName.isEmpty() ? serverFileName : fileName);
                database.saveUploadRecord(record);
                loadHistory();
                showStatus("上传成功: " + record.getName(), true);
                Toast.makeText(requireContext(), "上传成功: " + record.getName(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("ExploreFragment", "上传失败: " + error);
                showStatus("上传失败: " + error, false);
                Toast.makeText(requireContext(), "上传失败: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
        }
        return "unknown_file";
    }

    private long getFileSize(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is != null) {
                long size = is.available();
                is.close();
                return size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void loadHistory() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            List<UploadRecord> records = database.getAllRecords();
            android.util.Log.d("ExploreFragment", "加载历史记录: " + records.size() + " 条");
            adapter.setRecords(records);
        });
    }

    private void exportData() {
        try {
            List<UploadRecord> records = database.getAllRecords();
            JSONArray jsonArray = new JSONArray();
            
            for (UploadRecord record : records) {
                JSONObject json = new JSONObject();
                json.put("id", record.getId());
                json.put("name", record.getName());
                json.put("size", record.getSize());
                json.put("progress", record.getProgress());
                json.put("url", record.getUrl());
                json.put("uploadTime", record.getUploadTime());
                jsonArray.put(json);
            }

            String fileName = "upload_history_" + System.currentTimeMillis() + ".json";
            FileOutputStream fos = requireContext().openFileOutput(fileName, Activity.MODE_PRIVATE);
            fos.write(jsonArray.toString(2).getBytes());
            fos.close();

            showStatus("导出成功: " + fileName, true);
        } catch (Exception e) {
            showStatus("导出失败: " + e.getMessage(), false);
        }
    }

    private void handleImport(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                UploadRecord record = new UploadRecord(
                        json.getString("id"),
                        json.getString("name"),
                        json.getLong("size"),
                        json.getInt("progress"),
                        json.getString("url"),
                        json.getLong("uploadTime")
                );
                database.saveUploadRecord(record);
            }

            loadHistory();
            showStatus("导入成功", true);
        } catch (Exception e) {
            showStatus("导入失败: " + e.getMessage(), false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        tvStatus.setText(message);
        tvStatus.setBackgroundColor(isSuccess ? 0xFFF0FFF4 : 0xFFFFF5F5);
        tvStatus.setTextColor(isSuccess ? 0xFF2F855A : 0xFFC53030);
        tvStatus.setVisibility(View.VISIBLE);
        
        tvStatus.postDelayed(() -> tvStatus.setVisibility(View.GONE), 3000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (uploadManager != null) {
            uploadManager.shutdown();
        }
    }
}
