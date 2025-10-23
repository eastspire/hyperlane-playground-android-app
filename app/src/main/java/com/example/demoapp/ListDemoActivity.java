package com.example.demoapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ListDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_demo);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> items = generateSampleData();
        ListAdapter adapter = new ListAdapter(items, position -> 
            Toast.makeText(this, "点击了: " + items.get(position), Toast.LENGTH_SHORT).show()
        );
        
        recyclerView.setAdapter(adapter);
    }

    private List<String> generateSampleData() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            items.add("列表项 " + i);
        }
        return items;
    }
}
