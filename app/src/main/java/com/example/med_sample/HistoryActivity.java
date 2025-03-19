package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        BottomNavigationView bottomNa = findViewById(R.id.bottom_navigation);
        bottomNa.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        // Set up the back button click listener
        View backButton = findViewById(R.id.header_layout_history_back);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        BottomNavigationUtil.handlePermissionsResult(requestCode, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BottomNavigationUtil.handleActivityResult(requestCode, resultCode, data, this);
    }
}





