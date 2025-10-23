package com.example.demoapp.chat;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.demoapp.R;
import java.util.ArrayList;
import java.util.List;

public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.ViewHolder> {
    
    private List<OnlineUser> users = new ArrayList<>();
    private List<OnlineUser> filteredUsers = new ArrayList<>();
    private OnUserClickListener listener;
    private int selectedPosition = 0;
    
    public interface OnUserClickListener {
        void onUserClick(OnlineUser user);
    }
    
    public MentionAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }
    
    public void setUsers(List<OnlineUser> users) {
        this.users = users;
        this.filteredUsers = new ArrayList<>(users);
        selectedPosition = 0;
        notifyDataSetChanged();
    }
    
    public void filter(String query) {
        filteredUsers.clear();
        if (query == null || query.isEmpty()) {
            filteredUsers.addAll(users);
        } else {
            String lowerQuery = query.toLowerCase();
            for (OnlineUser user : users) {
                if (user.getUsername().toLowerCase().contains(lowerQuery)) {
                    filteredUsers.add(user);
                }
            }
        }
        selectedPosition = 0;
        notifyDataSetChanged();
    }
    
    public void setSelectedPosition(int position) {
        if (position >= 0 && position < filteredUsers.size()) {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
        }
    }
    
    public OnlineUser getSelectedUser() {
        if (selectedPosition >= 0 && selectedPosition < filteredUsers.size()) {
            return filteredUsers.get(selectedPosition);
        }
        return null;
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mention_user, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OnlineUser user = filteredUsers.get(position);
        holder.bind(user, position == selectedPosition);
    }
    
    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvName;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(filteredUsers.get(pos));
                }
            });
        }
        
        void bind(OnlineUser user, boolean isSelected) {
            tvName.setText(user.getUsername());
            tvAvatar.setText(user.getAvatarText());
            
            int color = getAvatarColor(user.getUsername());
            tvAvatar.setBackgroundColor(color);
            
            if (isSelected) {
                itemView.setBackgroundColor(Color.parseColor("#007BFF"));
                tvName.setTextColor(Color.WHITE);
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
                tvName.setTextColor(Color.parseColor("#2C3E50"));
            }
            
            if (user.isGpt()) {
                tvName.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#007BFF"));
            }
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
