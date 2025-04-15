package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.med_sample.fragments.home;
import com.example.med_sample.fragments.scan;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Dashboard_main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        if (savedInstanceState == null) {
            // Check if we should open the scan fragment
            if (getIntent().getBooleanExtra("open_scan", false)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new scan())
                        .commit();
                bottomNav.setSelectedItemId(R.id.nav_scan);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new home())
                        .commit();
            }
        }
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