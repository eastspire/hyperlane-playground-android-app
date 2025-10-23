package com.example.demoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class InputDemoActivity extends AppCompatActivity {

    private EditText etName, etPassword, etMultiline;
    private RadioGroup radioGroup;
    private CheckBox cbOption1, cbOption2;
    private Switch switchOption;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_demo);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        etMultiline = findViewById(R.id.et_multiline);
        radioGroup = findViewById(R.id.radio_group);
        cbOption1 = findViewById(R.id.cb_option1);
        cbOption2 = findViewById(R.id.cb_option2);
        switchOption = findViewById(R.id.switch_option);
        tvResult = findViewById(R.id.tv_result);
        Button btnSubmit = findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> collectInputData());
    }

    private void setupListeners() {
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_male) {
                Toast.makeText(this, "选择: 男", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rb_female) {
                Toast.makeText(this, "选择: 女", Toast.LENGTH_SHORT).show();
            }
        });

        switchOption.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "开关: " + (isChecked ? "开" : "关"), Toast.LENGTH_SHORT).show()
        );
    }

    private void collectInputData() {
        String name = etName.getText().toString();
        String password = etPassword.getText().toString();
        String multiline = etMultiline.getText().toString();
        
        String gender = "";
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_male) gender = "男";
        else if (selectedId == R.id.rb_female) gender = "女";

        StringBuilder result = new StringBuilder();
        result.append("姓名: ").append(name).append("\n");
        result.append("密码: ").append(password.replaceAll(".", "*")).append("\n");
        result.append("性别: ").append(gender).append("\n");
        result.append("选项1: ").append(cbOption1.isChecked() ? "是" : "否").append("\n");
        result.append("选项2: ").append(cbOption2.isChecked() ? "是" : "否").append("\n");
        result.append("开关: ").append(switchOption.isChecked() ? "开" : "关").append("\n");
        result.append("备注: ").append(multiline);

        tvResult.setText(result.toString());
    }
}
