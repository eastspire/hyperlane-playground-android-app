package com.example.demoapp.upload;

public class UploadRecord {
    private String id;
    private String name;
    private long size;
    private int progress;
    private String url;
    private long uploadTime;

    public UploadRecord() {
    }

    public UploadRecord(String id, String name, long size, int progress, String url, long uploadTime) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.progress = progress;
        this.url = url;
        this.uploadTime = uploadTime;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public long getUploadTime() { return uploadTime; }
    public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }
}
