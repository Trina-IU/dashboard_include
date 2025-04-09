package com.example.med_sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Calendar;

public class DisplayScanned extends AppCompatActivity {

    private TextView resultTextView;
    private Bitmap capturedImage;
    private DatabaseReference dbRef;
    private DatabaseReference historyRef;
    private String userId;
    private Button processButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        resultTextView = findViewById(R.id.result_text);
        capturedImage = getIntent().getParcelableExtra("capturedImage");
        processButton = findViewById(R.id.saveButton);

        if (capturedImage == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicine_schedule");
        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        recognizeText();

        processButton.setOnClickListener(v -> {
            String extractedText = resultTextView.getText().toString();
            if (!extractedText.isEmpty()) {
                saveMedicineScheduleToDatabase("Prescription", extractedText);
                saveToHistory(extractedText);
                Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recognizeText() {
        InputImage image = InputImage.fromBitmap(capturedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    resultTextView.setText(extractedText);
                })
                .addOnFailureListener(e -> {
                    resultTextView.setText("OCR failed: " + e.getMessage());
                    Log.e("OCR", "Failed to process image: " + e.getMessage());
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
                        String dateKey = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
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

        public MedicineSchedule() {
        }

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

        // Default constructor required for Firebase
        public HistoryItem() {
        }

        // Constructor to initialize fields
        public HistoryItem(String id, String text, long timestamp) {
            this.id = id;
            this.text = text;
            this.timestamp = timestamp;
        }

        // Getters and setters for Firebase serialization
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}