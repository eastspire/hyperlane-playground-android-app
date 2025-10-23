package com.example.demoapp.upload;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import java.util.ArrayList;
import java.util.List;

public class UploadHistoryAdapter extends RecyclerView.Adapter<UploadHistoryAdapter.ViewHolder> {
    private static final String BASE_URL = "http://120.53.248.2:65002";
    private List<UploadRecord> records = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upload_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UploadRecord record = records.get(position);
        holder.tvFileName.setText(record.getName());
        holder.tvFileSize.setText(formatFileSize(record.getSize()));
        holder.progressBar.setProgress(record.getProgress());

        // 调试日志
        android.util.Log.d("UploadAdapter", "Binding item: " + record.getName() + 
                           ", Progress: " + record.getProgress() + 
                           ", URL: " + record.getUrl());

        // 如果有URL且上传完成，设置点击事件
        if (record.getUrl() != null && !record.getUrl().isEmpty() && record.getProgress() >= 100) {
            android.util.Log.d("UploadAdapter", "Item is clickable: " + record.getName());
            
            // ✅ 所有文件都在浏览器中打开
            holder.itemView.setOnClickListener(v -> {
                android.util.Log.d("UploadAdapter", "Item clicked: " + record.getName());
                openUrlInBrowser(v, record.getUrl());
            });
            
            // 长按复制 URL
            holder.itemView.setOnLongClickListener(v -> {
                String fullUrl = buildFullUrl(record.getUrl());
                copyToClipboard(v.getContext(), fullUrl);
                android.widget.Toast.makeText(v.getContext(), 
                    "URL已复制: " + fullUrl, 
                    android.widget.Toast.LENGTH_SHORT).show();
                return true;
            });
            
            // 设置视觉反馈，表示可点击
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
        } else {
            android.util.Log.d("UploadAdapter", "Item NOT clickable: " + record.getName() + 
                               " (Progress: " + record.getProgress() + ", URL empty: " + 
                               (record.getUrl() == null || record.getUrl().isEmpty()) + ")");
            // 如果没有URL或未完成上传，移除点击事件
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setRecords(List<UploadRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    /**
     * 在浏览器中打开 URL（所有文件类型）
     */
    private void openUrlInBrowser(View view, String url) {
        try {
            // 构建完整的URL
            String fullUrl = buildFullUrl(url);
            
            // 调试日志
            android.util.Log.d("UploadAdapter", "========== 打开浏览器 ==========");
            android.util.Log.d("UploadAdapter", "原始 URL: " + url);
            android.util.Log.d("UploadAdapter", "完整 URL: " + fullUrl);
            
            // 创建Intent打开URL
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 检查是否有应用可以处理这个Intent
            if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                view.getContext().startActivity(intent);
                android.widget.Toast.makeText(view.getContext(), 
                    "正在打开浏览器...", 
                    android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // 如果没有浏览器，尝试使用选择器
                Intent chooser = Intent.createChooser(intent, "选择浏览器");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(chooser);
            }
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(view.getContext(), 
                "未找到浏览器应用", 
                android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("UploadAdapter", "打开浏览器失败", e);
            android.widget.Toast.makeText(view.getContext(), 
                "无法打开链接: " + e.getMessage(), 
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("File URL", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            android.util.Log.e("UploadAdapter", "Failed to copy to clipboard", e);
        }
    }
    
    private String buildFullUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        
        // 如果URL已经是完整的（以http://或https://开头），直接返回
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        // 如果URL以/开头，直接拼接
        if (url.startsWith("/")) {
            return BASE_URL + url;
        }
        
        // 否则，添加/再拼接
        return BASE_URL + "/" + url;
    }

    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 Bytes";
        int k = 1024;
        String[] sizes = {"Bytes", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
        return String.format("%.2f %s", bytes / Math.pow(k, i), sizes[i]);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
