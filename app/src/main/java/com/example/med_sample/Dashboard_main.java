package com.example.med_sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.med_sample.fragments.home;
import com.example.med_sample.fragments.profile;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Dashboard_main extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.dashboard_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new home())
                    .commit();
        }
    }

    private final BottomNavigationView.OnItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;

        if (item.getItemId() == R.id.nav_home) {
            selectedFragment = new home();
        } else if (item.getItemId() == R.id.nav_scan) {
            // Check for camera permission and request if not granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
            } else {
                // If permission is already granted, open the camera
                openCamera();
            }
            return true;
        } else if (item.getItemId() == R.id.nav_profile) {
            selectedFragment = new profile();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }
        return true;
    };

    private void requestCameraPermission() {
        // Request the camera permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            //the image capture is sent to DisplayScanned
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            Intent intent = new Intent(Dashboard_main.this, DisplayScanned.class);
            intent.putExtra("capturedImage", capturedImage); // Pass the image as a Bitmap
            startActivity(intent); // Launch DisplayScanned
        }
    }
}
