package com.example.demoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RouterPageAActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_page);

        TextView tvTitle = findViewById(R.id.tv_page_title);
        TextView tvInfo = findViewById(R.id.tv_page_info);
        Button btnForward = findViewById(R.id.btn_forward);
        Button btnReplace = findViewById(R.id.btn_replace);
        Button btnBack = findViewById(R.id.btn_back_page);

        tvTitle.setText("页面 A");
        tvInfo.setText("当前在页面 A\n\n" +
                "路由栈：主页 → 页面A\n\n" +
                "操作说明：\n" +
                "• 前进到页面B：添加新页面到栈顶\n" +
                "• 替换为页面C：用页面C替换当前页面A\n" +
                "• 返回：回到主页");

        // 前进到页面B
        btnForward.setOnClickListener(v -> {
            Toast.makeText(this, "前进到页面B", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RouterPageAActivity.this, RouterPageBActivity.class);
            startActivity(intent);
        });

        // 替换为页面C
        btnReplace.setOnClickListener(v -> {
            Toast.makeText(this, "替换为页面C", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RouterPageAActivity.this, RouterPageCActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 关闭当前页面，实现替换效果
        });

        // 返回
        btnBack.setOnClickListener(v -> {
            Toast.makeText(this, "返回上一页", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
