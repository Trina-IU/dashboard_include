package com.example.med_sample;

import static com.example.med_sample.R.id.retakeButton;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DisplayScanned extends AppCompatActivity {
    // For displaying the captured image
    private ImageView capturedImageView;
    private ImageView backToDashboardImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        capturedImageView = findViewById(R.id.capturedImageView);
        backToDashboardImageView = findViewById(R.id.backToDashboardButton);

        Intent intent = getIntent();
        Bitmap capturedImage = (Bitmap) intent.getParcelableExtra("capturedImage");

        if (capturedImage != null) {
            capturedImageView.setImageBitmap(capturedImage);
        } else {
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
        }

        // Set a click listener on the ImageView to go back to the dashboard
        backToDashboardImageView.setOnClickListener(v -> finish());
    }
}