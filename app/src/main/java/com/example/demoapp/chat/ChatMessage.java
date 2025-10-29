package com.example.demoapp.chat;

public class ChatMessage {
    private Long id;  // 用于分页加载历史消息
    private String type;
    private String name;
    private String data;
    private String time;
    private boolean isSelf;
    
    public ChatMessage(String type, String name, String data, String time) {
        this.type = type;
        this.name = name;
        this.data = data;
        this.time = time;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public boolean isSelf() {
        return isSelf;
    }
    
    public void setSelf(boolean self) {
        isSelf = self;
    }
    
    public boolean isGptResponse() {
        return "GptResponse".equals(type);
    }
    
    public boolean isOnlineCount() {
        return "OnlineCount".equals(type);
    }
}
