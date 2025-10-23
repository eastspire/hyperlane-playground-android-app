package com.example.demoapp;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.demoapp.adapters.ViewPagerAdapter;

public class MainActivityWithViewPager extends AppCompatActivity {

    private static final String TAG = "MainActivityViewPager";
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_main_with_viewpager);
            Log.d(TAG, "setContentView completed");

            viewPager = findViewById(R.id.view_pager);
            if (viewPager == null) {
                Log.e(TAG, "ViewPager is null!");
                return;
            }
            Log.d(TAG, "ViewPager found");
            
            adapter = new ViewPagerAdapter(this);
            Log.d(TAG, "Adapter created");
            
            viewPager.setAdapter(adapter);
            Log.d(TAG, "Adapter set");
            
            // 默认显示第一页（聊天页面）
            viewPager.setCurrentItem(0, false);
            
            // 禁用过度滚动效果
            viewPager.setOffscreenPageLimit(1);
            
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            throw e;
        }
    }
}
