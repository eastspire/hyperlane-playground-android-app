package com.example.demoapp.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        // 配置 Markwon 支持链接
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
                
                // 处理所有链接，使其在浏览器中打开
                makeLinkClickable(tvMessage);
                
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            }
            
            if (tvTime != null) {
                tvTime.setText(message.getTime());
            }
        }
        
        /**
         * 使 TextView 中的所有链接可点击，并在浏览器中打开
         */
        private void makeLinkClickable(TextView textView) {
            CharSequence text = textView.getText();
            if (text instanceof Spannable) {
                Spannable spannable = (Spannable) text;
                URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
                
                for (URLSpan span : spans) {
                    int start = spannable.getSpanStart(span);
                    int end = spannable.getSpanEnd(span);
                    int flags = spannable.getSpanFlags(span);
                    
                    // 移除原有的 URLSpan
                    spannable.removeSpan(span);
                    
                    // 创建新的 ClickableSpan，在浏览器中打开链接
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            String url = span.getURL();
                            openUrlInBrowser(url);
                        }
                    };
                    
                    spannable.setSpan(clickableSpan, start, end, flags);
                }
            } else {
                // 如果不是 Spannable，尝试检测纯文本中的 URL
                String plainText = text.toString();
                Spannable spannable = new SpannableString(plainText);
                
                // 匹配 URL 模式
                Pattern urlPattern = Pattern.compile(
                    "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
                    Pattern.CASE_INSENSITIVE
                );
                Matcher matcher = urlPattern.matcher(plainText);
                
                while (matcher.find()) {
                    final String url = matcher.group(1);
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            openUrlInBrowser(url);
                        }
                    };
                    
                    spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                
                textView.setText(spannable);
            }
        }
        
        /**
         * 在浏览器中打开 URL
         */
        private void openUrlInBrowser(String url) {
            try {
                // 确保 URL 有协议前缀
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
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
