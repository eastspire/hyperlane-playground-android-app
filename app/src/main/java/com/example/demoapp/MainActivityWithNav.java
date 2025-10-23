package com.example.demoapp;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.demoapp.fragments.ChatFragment;
import com.example.demoapp.fragments.ExploreFragment;
import com.example.demoapp.fragments.TraceFragment;

public class MainActivityWithNav extends AppCompatActivity {

    private LinearLayout navHome, navExplore, navFavorite;
    private ImageView iconHome, iconExplore, iconFavorite;
    private TextView textHome, textExplore, textFavorite;
    private int currentFragmentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_with_nav);

        initViews();
        setupClickListeners();
        
        // 默认显示文件上传页面
        currentFragmentIndex = 1;
        selectNavItem(1);
        loadFragment(new ExploreFragment(), false);
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navExplore = findViewById(R.id.nav_explore);
        navFavorite = findViewById(R.id.nav_favorite);

        iconHome = findViewById(R.id.nav_home_icon);
        iconExplore = findViewById(R.id.nav_explore_icon);
        iconFavorite = findViewById(R.id.nav_favorite_icon);

        textHome = findViewById(R.id.nav_home_text);
        textExplore = findViewById(R.id.nav_explore_text);
        textFavorite = findViewById(R.id.nav_favorite_text);
    }

    private void setupClickListeners() {
        navHome.setOnClickListener(v -> {
            if (currentFragmentIndex != 0) {
                selectNavItem(0);
                loadFragment(new ChatFragment(), currentFragmentIndex < 0);
                currentFragmentIndex = 0;
            }
        });

        navExplore.setOnClickListener(v -> {
            if (currentFragmentIndex != 1) {
                selectNavItem(1);
                loadFragment(new ExploreFragment(), currentFragmentIndex < 1);
                currentFragmentIndex = 1;
            }
        });

        navFavorite.setOnClickListener(v -> {
            if (currentFragmentIndex != 2) {
                selectNavItem(2);
                loadFragment(new TraceFragment(), currentFragmentIndex < 2);
                currentFragmentIndex = 2;
            }
        });
    }

    private void selectNavItem(int index) {
        // 重置所有导航项
        resetNavItem(navHome, iconHome, textHome);
        resetNavItem(navExplore, iconExplore, textExplore);
        resetNavItem(navFavorite, iconFavorite, textFavorite);

        // 选中当前导航项
        LinearLayout selectedNav = null;
        ImageView selectedIcon = null;
        TextView selectedText = null;

        switch (index) {
            case 0:
                selectedNav = navHome;
                selectedIcon = iconHome;
                selectedText = textHome;
                break;
            case 1:
                selectedNav = navExplore;
                selectedIcon = iconExplore;
                selectedText = textExplore;
                break;
            case 2:
                selectedNav = navFavorite;
                selectedIcon = iconFavorite;
                selectedText = textFavorite;
                break;
        }

        if (selectedNav != null) {
            selectedNav.setSelected(true);
            animateNavItem(selectedIcon, selectedText);
        }
    }

    private void resetNavItem(LinearLayout nav, ImageView icon, TextView text) {
        nav.setSelected(false);
        icon.setScaleX(1f);
        icon.setScaleY(1f);
        text.setScaleX(1f);
        text.setScaleY(1f);
    }

    private void animateNavItem(ImageView icon, TextView text) {
        // 图标缩放动画
        ObjectAnimator scaleXIcon = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleYIcon = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.2f, 1f);
        scaleXIcon.setDuration(300);
        scaleYIcon.setDuration(300);
        scaleXIcon.start();
        scaleYIcon.start();

        // 文字缩放动画
        ObjectAnimator scaleXText = ObjectAnimator.ofFloat(text, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleYText = ObjectAnimator.ofFloat(text, "scaleY", 1f, 1.1f, 1f);
        scaleXText.setDuration(300);
        scaleYText.setDuration(300);
        scaleXText.start();
        scaleYText.start();
    }

    private void loadFragment(Fragment fragment, boolean slideRight) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // 设置页面切换动画
        if (slideRight) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            );
        }
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
