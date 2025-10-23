package com.example.demoapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DialogDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_demo);

        Button btnAlert = findViewById(R.id.btn_alert_dialog);
        Button btnConfirm = findViewById(R.id.btn_confirm_dialog);
        Button btnList = findViewById(R.id.btn_list_dialog);
        Button btnProgress = findViewById(R.id.btn_progress_dialog);

        btnAlert.setOnClickListener(v -> showAlertDialog());
        btnConfirm.setOnClickListener(v -> showConfirmDialog());
        btnList.setOnClickListener(v -> showListDialog());
        btnProgress.setOnClickListener(v -> showProgressDialog());
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("这是一个简单的提示对话框")
            .setPositiveButton("确定", (dialog, which) -> 
                Toast.makeText(this, "点击了确定", Toast.LENGTH_SHORT).show()
            )
            .show();
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("确认")
            .setMessage("确定要执行此操作吗？")
            .setPositiveButton("确定", (dialog, which) -> 
                Toast.makeText(this, "已确认", Toast.LENGTH_SHORT).show()
            )
            .setNegativeButton("取消", (dialog, which) -> 
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
            )
            .show();
    }

    private void showListDialog() {
        String[] items = {"选项1", "选项2", "选项3", "选项4"};
        new AlertDialog.Builder(this)
            .setTitle("选择一项")
            .setItems(items, (dialog, which) -> 
                Toast.makeText(this, "选择了: " + items[which], Toast.LENGTH_SHORT).show()
            )
            .show();
    }

    private void showProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("加载中");
        progressDialog.setMessage("请稍候...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 3秒后自动关闭
        new android.os.Handler().postDelayed(() -> {
            progressDialog.dismiss();
            Toast.makeText(this, "加载完成", Toast.LENGTH_SHORT).show();
        }, 3000);
    }
}
