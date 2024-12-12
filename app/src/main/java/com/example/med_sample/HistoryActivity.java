package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.fragments.headerFragment;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Add Fragment to header_container
        Fragment headerFragment = new headerFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.header_container, headerFragment)
                .commit();

        initializeUI();
    }

    private void initializeUI() {
        // Set up the back button click listener
        View backButton = findViewById(R.id.backtodashboard);
        backButton.setOnClickListener(v -> {
            // Navigate back to dashboardaActivity to display the home fragment
            Intent intent = new Intent(HistoryActivity.this, Dashboard_main.class);
            intent.putExtra("navigate_to", "home");
            startActivity(intent);
            finish();
        });
    }
}