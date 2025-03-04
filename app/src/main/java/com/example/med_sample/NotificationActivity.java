package com.example.med_sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        LinearLayout notification = findViewById(R.id.header_layout_nofication_back);
        notification.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationActivity.this, Dashboard_main.class);
            intent.putExtra("navigate_to", "home");
            startActivity(intent);
            finish();
        });

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