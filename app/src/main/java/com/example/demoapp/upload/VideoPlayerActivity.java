package com.example.demoapp.upload;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.demoapp.R;
import java.util.HashMap;
import java.util.Map;

/**
 * 视频播放器 - 完全对齐服务器端 Range 请求逻辑
 * 支持：
 * 1. 流媒体播放（自动发送 Range 请求）
 * 2. 断点续传
 * 3. 206 Partial Content 响应处理
 * 4. 根据视频比例自动选择横屏/竖屏
 */
public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    
    private VideoView videoView;
    private String videoUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 隐藏状态栏和导航栏，全屏显示
        hideSystemUI();
        
        setContentView(R.layout.activity_video_player);
        
        videoView = findViewById(R.id.video_view);
        
        // 从 Intent 获取视频 URL
        videoUrl = getIntent().getStringExtra("video_url");
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "无效的视频 URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "========== 初始化视频播放器 ==========");
        Log.d(TAG, "视频 URL: " + videoUrl);
        
        setupVideoPlayer();
    }
    
    /**
     * 隐藏系统 UI（状态栏和导航栏）
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
    
    private void setupVideoPlayer() {
        // 设置媒体控制器
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        
        // 设置视频 URI
        // VideoView 会自动发送 Range 请求来支持流媒体播放
        Uri videoUri = Uri.parse(videoUrl);
        
        // 添加自定义请求头（如果需要）
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "video/*");
        headers.put("User-Agent", "Android-VideoPlayer");
        
        videoView.setVideoURI(videoUri, headers);
        
        // 设置监听器
        videoView.setOnPreparedListener(mp -> {
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            
            Log.d(TAG, "========== 视频准备完成 ==========");
            Log.d(TAG, "视频时长: " + mp.getDuration() + " ms");
            Log.d(TAG, "视频宽度: " + videoWidth);
            Log.d(TAG, "视频高度: " + videoHeight);
            
            // ✅ 根据视频比例自动选择屏幕方向
            setOrientationByVideoRatio(videoWidth, videoHeight);
            
            // 自动播放
            videoView.start();
        });
        
        videoView.setOnCompletionListener(mp -> {
            Log.d(TAG, "视频播放完成");
        });
        
        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "视频播放错误 - what: " + what + ", extra: " + extra);
            Toast.makeText(this, "视频播放失败", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        videoView.setOnInfoListener((mp, what, extra) -> {
            switch (what) {
                case android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Log.d(TAG, "开始缓冲");
                    break;
                case android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Log.d(TAG, "缓冲结束");
                    break;
                case android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    Log.d(TAG, "开始渲染视频");
                    break;
            }
            return false;
        });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
    
    /**
     * 根据视频宽高比自动设置屏幕方向
     * 
     * @param videoWidth 视频宽度
     * @param videoHeight 视频高度
     */
    private void setOrientationByVideoRatio(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) {
            Log.w(TAG, "无效的视频尺寸，使用默认方向");
            return;
        }
        
        float ratio = (float) videoWidth / videoHeight;
        
        Log.d(TAG, "========== 自动选择屏幕方向 ==========");
        Log.d(TAG, "视频宽高比: " + ratio);
        
        if (ratio > 1.0f) {
            // 横向视频（宽 > 高）
            Log.d(TAG, "检测到横向视频，设置为横屏模式");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (ratio < 1.0f) {
            // 纵向视频（高 > 宽）
            Log.d(TAG, "检测到纵向视频，设置为竖屏模式");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            // 正方形视频（宽 == 高）
            Log.d(TAG, "检测到正方形视频，保持当前方向");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // 配置改变时重新隐藏系统 UI
        hideSystemUI();
        
        Log.d(TAG, "========== 屏幕方向改变 ==========");
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "当前方向: 横屏");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "当前方向: 竖屏");
        }
    }
    
    @Override
    public void onBackPressed() {
        Log.d(TAG, "========== 返回键按下 ==========");
        
        // 停止播放
        if (videoView != null && videoView.isPlaying()) {
            Log.d(TAG, "停止视频播放");
            videoView.stopPlayback();
        }
        
        // 退出播放器
        Log.d(TAG, "退出视频播放器");
        super.onBackPressed();
        finish();
    }
}
