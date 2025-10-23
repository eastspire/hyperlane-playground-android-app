package com.example.demoapp;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class MediaDemoActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private VideoView videoView;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_demo);

        ImageView imageView = findViewById(R.id.image_view);
        videoView = findViewById(R.id.video_view);
        Button btnPlayAudio = findViewById(R.id.btn_play_audio);
        Button btnStopAudio = findViewById(R.id.btn_stop_audio);
        Button btnPlayVideo = findViewById(R.id.btn_play_video);

        // 显示示例图片
        imageView.setImageResource(R.drawable.sample_image);

        btnPlayAudio.setOnClickListener(v -> playAudio());
        btnStopAudio.setOnClickListener(v -> stopAudio());
        btnPlayVideo.setOnClickListener(v -> playVideo());
    }

    private void playAudio() {
        Toast.makeText(this, "Audio demo - Add sample_audio.mp3 to res/raw/ folder", Toast.LENGTH_LONG).show();
        // To enable audio playback:
        // 1. Add sample_audio.mp3 to app/src/main/res/raw/
        // 2. Uncomment the code below
        /*
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.sample_audio);
            }
            if (mediaPlayer != null && !isPlaying) {
                mediaPlayer.start();
                isPlaying = true;
                Toast.makeText(this, "Playing audio", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Audio playback failed", Toast.LENGTH_SHORT).show();
        }
        */
    }

    private void stopAudio() {
        Toast.makeText(this, "Audio demo - Add media files to enable playback", Toast.LENGTH_SHORT).show();
        /*
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            isPlaying = false;
            Toast.makeText(this, "Audio stopped", Toast.LENGTH_SHORT).show();
        }
        */
    }

    private void playVideo() {
        Toast.makeText(this, "Video demo - Add sample_video.mp4 to res/raw/ folder", Toast.LENGTH_LONG).show();
        // To enable video playback:
        // 1. Add sample_video.mp4 to app/src/main/res/raw/
        // 2. Uncomment the code below
        /*
        try {
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.sample_video;
            videoView.setVideoURI(Uri.parse(videoPath));
            videoView.start();
            Toast.makeText(this, "Playing video", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Video playback failed", Toast.LENGTH_SHORT).show();
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
