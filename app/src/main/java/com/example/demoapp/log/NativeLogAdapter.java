package com.example.demoapp.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NativeLogAdapter extends RecyclerView.Adapter<NativeLogAdapter.LogViewHolder> {
    
    private List<NativeLog> logs = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_native_log, parent, false);
        return new LogViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NativeLog log = logs.get(position);
        holder.bind(log);
    }
    
    @Override
    public int getItemCount() {
        return logs.size();
    }
    
    public void setLogs(List<NativeLog> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }
    
    public void addLog(NativeLog log) {
        logs.add(0, log);
        notifyItemInserted(0);
    }
    
    class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTimestamp;
        private TextView tvLevel;
        private TextView tvTag;
        private TextView tvMessage;
        
        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tv_log_timestamp);
            tvLevel = itemView.findViewById(R.id.tv_log_level);
            tvTag = itemView.findViewById(R.id.tv_log_tag);
            tvMessage = itemView.findViewById(R.id.tv_log_message);
        }
        
        void bind(NativeLog log) {
            tvTimestamp.setText(dateFormat.format(new Date(log.getTimestamp())));
            tvTag.setText(log.getTag());
            tvMessage.setText(log.getMessage());
            
            // 根据日志级别设置颜色
            int color;
            String levelText;
            switch (log.getLevel()) {
                case ERROR:
                    color = 0xFFE74C3C;
                    levelText = "E";
                    break;
                case INFO:
                    color = 0xFF3498DB;
                    levelText = "I";
                    break;
                case DEBUG:
                    color = 0xFF95A5A6;
                    levelText = "D";
                    break;
                default:
                    color = 0xFF000000;
                    levelText = "?";
            }
            tvLevel.setText(levelText);
            tvLevel.setTextColor(color);
        }
    }
}
