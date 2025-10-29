package com.example.demoapp.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.example.demoapp.R;

public class VideoPlayerActivity extends AppCompatActivity {
    
    private PlayerView playerView;
    private ExoPlayer player;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        playerView = findViewById(R.id.player_view);
        
        String videoUrl = getIntent().getStringExtra("video_url");
        
        if (videoUrl != null && !videoUrl.isEmpty()) {
            initializePlayer(videoUrl);
        } else {
            Toast.makeText(this, "无效的视频链接", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializePlayer(String url) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(VideoPlayerActivity.this, "视频播放失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
