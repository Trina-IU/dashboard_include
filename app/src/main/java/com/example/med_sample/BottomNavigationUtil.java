package com.example.med_sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.med_sample.fragments.home;
import com.example.med_sample.fragments.profile;
import com.example.med_sample.fragments.scan;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationUtil {

    private static final int CAMERA_REQUEST_CODE = 100;

    public static BottomNavigationView.OnItemSelectedListener getNavListener(FragmentActivity activity) {
        return item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new home();
            } else if (item.getItemId() == R.id.nav_scan) {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission(activity);
                } else {
                    selectedFragment = new scan(); // Navigate to scan fragment instead of opening camera directly
                }
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new profile();
            }

            if (selectedFragment != null) {
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        };
    }

    private static void requestCameraPermission(FragmentActivity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    public static void handlePermissionsResult(int requestCode, @NonNull int[] grantResults, FragmentActivity activity) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new scan())
                        .commit();
            } else {
                Toast.makeText(activity, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Keep this method for handling other camera-related activity results
    public static void handleActivityResult(int requestCode, int resultCode, Intent data, FragmentActivity activity) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == FragmentActivity.RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            if (capturedImage != null) {
                Intent intent = new Intent(activity, DisplayScanned.class);
                intent.putExtra("capturedImage", capturedImage);
                activity.startActivity(intent);
            }
        }
    }
}