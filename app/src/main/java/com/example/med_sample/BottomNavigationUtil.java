package com.example.med_sample;

import android.Manifest;
import android.content.Context;
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
                    openCamera(activity);
                }
                return true;
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

    private static void openCamera(FragmentActivity activity) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(activity, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    public static void handlePermissionsResult(int requestCode, @NonNull int[] grantResults, FragmentActivity activity) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(activity);
            } else {
                Toast.makeText(activity, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void handleActivityResult(int requestCode, int resultCode, Intent data, FragmentActivity activity) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == FragmentActivity.RESULT_OK) {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            Intent intent = new Intent(activity, DisplayScanned.class);
            intent.putExtra("capturedImage", capturedImage);
            activity.startActivity(intent);
        }
    }
}