package com.example.med_sample;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if a user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // User is logged in, navigate to Dashboard or Profile
            startActivity(new Intent(this, Dashboard_main.class));
        } else {
            // User is not logged in, check first launch
            SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
            boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

            if (isFirstLaunch) {
                // First launch: Navigate to StartpageActivity
                startActivity(new Intent(this, StartpageActivity.class));

                // Update SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isFirstLaunch", false);
                editor.apply();
            } else {
                // Subsequent launches: Navigate to LoginActivity
                startActivity(new Intent(this, LoginActivity.class));
            }
        }

        // Close the current activity to prevent going back to it
        finish();
    }
}