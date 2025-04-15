package com.example.med_sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.med_sample.fragments.home;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class DisplayScanned extends AppCompatActivity {

    private TextView resultTextView;
    private ImageView capturedImageView, backToDashboardButton;
    private Button processButton, retakeButton;
    private String extractedText;
    private Bitmap capturedImage;
    private DatabaseReference dbRef;
    private DatabaseReference historyRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        // Retrieve extras from the Intent
        extractedText = getIntent().getStringExtra("extractedText");
        String imagePath = getIntent().getStringExtra("imagePath");

        resultTextView = findViewById(R.id.result_text);
        capturedImageView = findViewById(R.id.capturedImageView);
        processButton = findViewById(R.id.saveButton);
        retakeButton = findViewById(R.id.retakeButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        // Enable vertical scrolling for the TextView
        resultTextView.setMovementMethod(new ScrollingMovementMethod());

        backToDashboardButton.setOnClickListener(v -> {
            onBackPressed();
        });

        // Load image from file path instead of from intent
        if (imagePath != null) {
            capturedImage = BitmapFactory.decodeFile(imagePath);
            if (capturedImage != null) {
                capturedImageView.setImageBitmap(capturedImage);
            } else {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show();
        }

        if (extractedText != null && !extractedText.isEmpty()) {
            resultTextView.setText(extractedText);
        } else {
            resultTextView.setText("No text found");
        }

        // Retake: Return to the home (or scan) fragment
        retakeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Dashboard_main.class);
            intent.putExtra("open_scan", true);
            startActivity(intent);
            finish();
        });

        // Check for user authentication
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Firebase database references
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicine_schedule");
        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        // Process button: Confirm and save the result to Firebase
        processButton.setOnClickListener(v -> {
            String scannedText = resultTextView.getText().toString();
            if (scannedText.isEmpty()) {
                Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to save this text?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        saveMedicineScheduleToDatabase("Prescription", scannedText);
                        saveToHistory(scannedText);
                        Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, Dashboard_main.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void saveMedicineScheduleToDatabase(String medicineName, String intakeSchedule) {
        String key = dbRef.push().getKey();
        if (key == null) {
            Log.e("DatabaseError", "Failed to generate database key.");
            return;
        }
        MedicineSchedule schedule = new MedicineSchedule(key, medicineName, intakeSchedule, System.currentTimeMillis());
        if (intakeSchedule.toLowerCase().contains("once a day in")) {
            String[] parts = intakeSchedule.split("in");
            if (parts.length > 1) {
                String daysPart = parts[1].trim().split(" ")[0];
                try {
                    int numberOfDays = Integer.parseInt(daysPart);
                    Calendar calendar = Calendar.getInstance();
                    for (int i = 0; i < numberOfDays; i++) {
                        String dateKey = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1)
                                + "-" + calendar.get(Calendar.DAY_OF_MONTH);
                        dbRef.child(dateKey).child(key).setValue(schedule);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } catch (NumberFormatException e) {
                    Log.e("ScheduleParsing", "Invalid number format in intake schedule.");
                }
            } else {
                Log.e("ScheduleParsing", "Invalid intake schedule format.");
            }
        } else {
            dbRef.child("specific_date").child(key).setValue(schedule);
        }
    }

    private void saveToHistory(String extractedText) {
        String key = historyRef.push().getKey();
        if (key != null) {
            HistoryItem historyItem = new HistoryItem(key, extractedText, System.currentTimeMillis());
            historyRef.child(key).setValue(historyItem)
                    .addOnSuccessListener(aVoid -> Log.d("History", "History saved successfully."))
                    .addOnFailureListener(e -> Log.e("History", "Failed to save history: " + e.getMessage()));
        } else {
            Log.e("History", "Failed to generate history key.");
        }
    }

    public static class MedicineSchedule {
        public String id;
        public String medicineName;
        public String intakeSchedule;
        public long timestamp;

        public MedicineSchedule() {}

        public MedicineSchedule(String id, String medicineName, String intakeSchedule, long timestamp) {
            this.id = id;
            this.medicineName = medicineName;
            this.intakeSchedule = intakeSchedule;
            this.timestamp = timestamp;
        }
    }

    public static class HistoryItem {
        public String id;
        public String text;
        public long timestamp;

        public HistoryItem() {}

        public HistoryItem(String id, String text, long timestamp) {
            this.id = id;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}
