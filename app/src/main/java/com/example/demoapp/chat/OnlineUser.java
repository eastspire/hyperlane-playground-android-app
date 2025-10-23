package com.example.demoapp.chat;

public class OnlineUser {
    private String username;
    private boolean isGpt;
    
    public OnlineUser(String username) {
        this.username = username;
        this.isGpt = "gpt".equals(username);
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
        this.isGpt = "gpt".equals(username);
    }
    
    public boolean isGpt() {
        return isGpt;
    }
    
    public String getAvatarText() {
        if (isGpt) {
            return "🤖";
        }
        return username.length() > 0 ? username.substring(0, 1).toUpperCase() : "U";
    }
}
