package com.example.demoapp.chat;

import android.content.Context;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    
    private static final int VIEW_TYPE_SELF = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_GPT = 3;
    
    private List<ChatMessage> messages;
    private Context context;
    private Markwon markwon;
    
    public ChatAdapter(List<ChatMessage> messages, Context context) {
        this.messages = messages;
        this.context = context;
        
        // 简化版本：只使用基础 Markdown 渲染，不使用语法高亮
        this.markwon = Markwon.builder(context)
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .build();
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isGptResponse()) {
            return VIEW_TYPE_GPT;
        } else if (message.isSelf()) {
            return VIEW_TYPE_SELF;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case VIEW_TYPE_SELF:
                layoutId = R.layout.item_message_self;
                break;
            case VIEW_TYPE_GPT:
                layoutId = R.layout.item_message_gpt;
                break;
            default:
                layoutId = R.layout.item_message_other;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvMessage;
        TextView tvTime;
        TextView tvAvatar;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_message_name);
            tvMessage = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
        }
        
        void bind(ChatMessage message) {
            if (tvName != null) {
                String displayName = message.isGptResponse() ? "GPT" : message.getName();
                tvName.setText(displayName);
            }
            
            if (tvAvatar != null) {
                String avatarText = getAvatarText(message);
                tvAvatar.setText(avatarText);
                int color = getAvatarColor(message.getName());
                tvAvatar.setBackgroundColor(color);
            }
            
            if (tvMessage != null) {
                // First render markdown
                markwon.setMarkdown(tvMessage, message.getData());
                
                // Then apply mention formatting
                CharSequence formattedText = MentionFormatter.formatMentions(tvMessage.getText().toString());
                tvMessage.setText(formattedText);
                
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            }
            
            if (tvTime != null) {
                tvTime.setText(message.getTime());
            }
        }
        
        private String getAvatarText(ChatMessage message) {
            if (message.isGptResponse()) {
                return "AI";
            }
            String name = message.getName();
            if (name.startsWith("User")) {
                return name.length() >= 6 ? name.substring(name.length() - 6, name.length() - 4) : "U";
            }
            return name.substring(0, 1).toUpperCase();
        }
        
        private int getAvatarColor(String name) {
            int[] colors = {
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"),
                Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7"),
                Color.parseColor("#DDA0DD"),
                Color.parseColor("#98D8C8"),
                Color.parseColor("#F7DC6F"),
                Color.parseColor("#BB8FCE"),
                Color.parseColor("#85C1E9")
            };
            
            int hash = 0;
            for (int i = 0; i < name.length(); i++) {
                hash = name.charAt(i) + ((hash << 5) - hash);
            }
            return colors[Math.abs(hash) % colors.length];
        }
    }
}
