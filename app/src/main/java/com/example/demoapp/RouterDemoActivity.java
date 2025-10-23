package com.example.demoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RouterDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_demo);

        TextView tvTitle = findViewById(R.id.tv_router_title);
        TextView tvInfo = findViewById(R.id.tv_router_info);
        Button btnGoPageA = findViewById(R.id.btn_go_page_a);
        Button btnBack = findViewById(R.id.btn_back);

        tvTitle.setText("路由演示 - 主页");
        tvInfo.setText("这是路由演示的主页面\n\n" +
                "点击下方按钮进入页面A，体验路由功能：\n" +
                "• 前进：跳转到新页面\n" +
                "• 后退：返回上一页面\n" +
                "• 替换：替换当前页面");

        btnGoPageA.setOnClickListener(v -> {
            Intent intent = new Intent(RouterDemoActivity.this, RouterPageAActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
