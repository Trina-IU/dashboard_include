package com.example.med_sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.med_sample.fragments.scan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Calendar;

public class DisplayScanned extends AppCompatActivity {

    // Move static block to class level [[6]]
    static {
        System.loadLibrary("opencv_java4");
    }

    private TextView resultTextView;
    private ImageView capturedImageView;
    private Bitmap capturedImage;
    private DatabaseReference dbRef;
    private DatabaseReference historyRef;
    private String userId;
    private Button processButton, retakeButton;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            if (capturedImage != null) {
                capturedImageView.setImageBitmap(capturedImage);
                this.capturedImage = capturedImage;
                recognizeText();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        resultTextView = findViewById(R.id.result_text);
        capturedImageView = findViewById(R.id.capturedImageView);
        processButton = findViewById(R.id.saveButton);
        retakeButton = findViewById(R.id.retakeButton);

        capturedImage = getIntent().getParcelableExtra("capturedImage");

        if (capturedImage == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        capturedImageView.setImageBitmap(capturedImage);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicine_schedule");
        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        recognizeText();

        retakeButton.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, 100);
            } else {
                Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            }
        });

        processButton.setOnClickListener(v -> {
            String extractedText = resultTextView.getText().toString();
            if (!extractedText.isEmpty()) {
                saveMedicineScheduleToDatabase("Prescription", extractedText);
                saveToHistory(extractedText);
                Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, Dashboard_main.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recognizeText() {
        Mat mat = new Mat();
        Utils.bitmapToMat(capturedImage, mat);

        mat = autoRotate(mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mat, mat, new org.opencv.core.Size(5, 5), 0);
        Imgproc.adaptiveThreshold(
                mat, mat, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 11, 2
        );

        double scale = 1200.0 / mat.cols();
        Size newSize = new Size(mat.cols() * scale, mat.rows() * scale);
        Imgproc.resize(mat, mat, newSize);

        Bitmap processedBitmap = Bitmap.createBitmap(
                mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888
        );
        Utils.matToBitmap(mat, processedBitmap);
        mat.release();

        InputImage image = InputImage.fromBitmap(processedBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(
                new TextRecognizerOptions.Builder()
                        .build()
        );

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    resultTextView.setText(extractedText);
                })
                .addOnFailureListener(e -> {
                    resultTextView.setText("OCR failed: " + e.getMessage());
                    Log.e("OCR", "Error: " + e.getMessage());
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

        public HistoryItem() {
        }

        public HistoryItem(String id, String text, long timestamp) {
            this.id = id;
            this.text = text;
            this.timestamp = timestamp;
        }

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
    }private Mat autoRotate(Mat mat) {
        // Detect orientation (simplified example)
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(getIntent().getStringExtra("imagePath"));
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Rotate based on EXIF data
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                Core.rotate(mat, mat, Core.ROTATE_180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;
        }
        return mat;
    }
}