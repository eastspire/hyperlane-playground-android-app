package com.example.demoapp.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.demoapp.R;

public class ImageViewerActivity extends AppCompatActivity {
    
    private ImageView imageView;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        
        imageView = findViewById(R.id.image_view);
        progressBar = findViewById(R.id.progress_bar);
        
        String imageUrl = getIntent().getStringExtra("image_url");
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImage(imageUrl);
        } else {
            Toast.makeText(this, "无效的图片链接", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // 点击图片关闭
        imageView.setOnClickListener(v -> finish());
    }
    
    private void loadImage(String url) {
        progressBar.setVisibility(View.VISIBLE);
        
        Glide.with(this)
                .load(url)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImageViewerActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imageView);
    }
}
