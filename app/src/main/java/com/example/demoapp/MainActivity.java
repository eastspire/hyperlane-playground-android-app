package com.example.demoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButtons();
    }

    private void setupButtons() {
        Button btnButton = findViewById(R.id.btn_button_demo);
        Button btnDialog = findViewById(R.id.btn_dialog_demo);
        Button btnInput = findViewById(R.id.btn_input_demo);
        Button btnList = findViewById(R.id.btn_list_demo);
        Button btnMedia = findViewById(R.id.btn_media_demo);
        Button btnRouter = findViewById(R.id.btn_router_demo);
        Button btnBottomNav = findViewById(R.id.btn_bottom_nav_demo);

        btnButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ButtonDemoActivity.class)));
        btnDialog.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DialogDemoActivity.class)));
        btnInput.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, InputDemoActivity.class)));
        btnList.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ListDemoActivity.class)));
        btnMedia.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MediaDemoActivity.class)));
        btnRouter.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RouterDemoActivity.class)));
        btnBottomNav.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MainActivityWithNav.class)));
    }
}
