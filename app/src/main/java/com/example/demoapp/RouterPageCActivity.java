package com.example.demoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RouterPageCActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_page);

        TextView tvTitle = findViewById(R.id.tv_page_title);
        TextView tvInfo = findViewById(R.id.tv_page_info);
        Button btnForward = findViewById(R.id.btn_forward);
        Button btnReplace = findViewById(R.id.btn_replace);
        Button btnBack = findViewById(R.id.btn_back_page);

        tvTitle.setText("页面 C");
        tvInfo.setText("当前在页面 C\n\n" +
                "这是最后一个演示页面\n\n" +
                "操作说明：\n" +
                "• 返回主页：清空所有页面，回到主页\n" +
                "• 返回：回到上一页");

        // 隐藏前进按钮
        btnForward.setVisibility(android.view.View.GONE);

        // 替换按钮改为返回主页
        btnReplace.setText("返回主页");
        btnReplace.setOnClickListener(v -> {
            Toast.makeText(this, "清空栈，返回主页", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RouterPageCActivity.this, RouterDemoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 返回
        btnBack.setOnClickListener(v -> {
            Toast.makeText(this, "返回上一页", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
