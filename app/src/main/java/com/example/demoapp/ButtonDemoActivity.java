package com.example.demoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ButtonDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_demo);

        Button btnNormal = findViewById(R.id.btn_normal);
        Button btnColored = findViewById(R.id.btn_colored);
        Button btnDisabled = findViewById(R.id.btn_disabled);
        FloatingActionButton fab = findViewById(R.id.fab);

        btnNormal.setOnClickListener(v -> 
            Toast.makeText(this, "普通按钮被点击", Toast.LENGTH_SHORT).show()
        );

        btnColored.setOnClickListener(v -> 
            Toast.makeText(this, "彩色按钮被点击", Toast.LENGTH_SHORT).show()
        );

        btnDisabled.setEnabled(false);

        fab.setOnClickListener(v -> 
            Toast.makeText(this, "悬浮按钮被点击", Toast.LENGTH_SHORT).show()
        );
    }
}
