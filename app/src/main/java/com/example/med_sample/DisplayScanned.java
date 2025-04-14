package com.example.med_sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class DisplayScanned extends AppCompatActivity {
    // Load OpenCV library
    static {
        System.loadLibrary("opencv_java4");
    }

    private TextView resultTextView;
    private ImageView capturedImageView, backButton;
    private Bitmap capturedImage;
    private DatabaseReference dbRef;
    private DatabaseReference historyRef;
    private String userId;
    private Button processButton, retakeButton;
    private File imageFile;
    private String imagePath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            capturedImage = (Bitmap) data.getExtras().get("data");
            if (capturedImage != null) {
                capturedImageView.setImageBitmap(capturedImage);
                recognizeText();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        // Initialize UI components
        imageFile = new File(getExternalCacheDir(), "captured_image.jpg");
        resultTextView = findViewById(R.id.result_text);
        capturedImageView = findViewById(R.id.capturedImageView);
        processButton = findViewById(R.id.saveButton);
        retakeButton = findViewById(R.id.retakeButton);
        backButton = findViewById(R.id.backToDashboardButton);

        // Back button functionality
        backButton.setOnClickListener(v -> onBackPressed());

        // Retrieve captured image and user ID
        capturedImage = getIntent().getParcelableExtra("capturedImage");
        imagePath = getIntent().getStringExtra("imagePath");

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

        // Firebase database references
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicine_schedule");
        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        // Recognize text from the captured image
        recognizeText();

        // Retake button functionality
        retakeButton.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, 100);
            } else {
                Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            }
        });

        // Process button functionality
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

        // Preprocessing toggle (experiment with/without)
        if (shouldPreprocess()) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0); // Reduced blur kernel
            Imgproc.adaptiveThreshold(
                    mat, mat, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 9, 2 // Adjusted block size
            );
        }

        // Dynamic scaling with aspect ratio preservation
        double targetWidth = 1600; // Increased width for better detail
        double scale = Math.min(targetWidth / mat.cols(), 2.0); // Max 2x scaling
        Size newSize = new Size(mat.cols() * scale, mat.rows() * scale);
        Imgproc.resize(mat, mat, newSize);

        // Convert back to Bitmap
        Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, processedBitmap);
        mat.release();

        // Use cloud model for better accuracy (requires internet)
        TextRecognizerOptions options = new TextRecognizerOptions.Builder()
                .build();

        TextRecognizer recognizer = TextRecognition.getClient(options);
        InputImage image = InputImage.fromBitmap(processedBitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    String processedText = postProcess(rawText); // Add post-processing
                    resultTextView.setText(processedText);
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

    private Mat autoRotate(Mat mat) {
        if (imagePath == null || imagePath.isEmpty()) {
            return mat;
        }

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    private boolean shouldPreprocess() {
        // Implement logic to determine preprocessing based on image characteristics
        return true; // Default to processing
    }

    private String postProcess(String rawText) {
        return rawText
                .replaceAll("([A-Za-z])\\1{2,}", "$1") // Remove repeated characters
                .replace("0", "O") // Common substitution
                .replace("1", "l")
                .replace("5", "S") // Example: "5ugar" → "Sugar"
                .replace("2", "Z") // Example: "2inc" → "Zinc"
                .replace("!", "I") // Example: "!buprofen" → "Ibuprofen"
                .replaceAll("(?i)qid", "4 times a day") // Case-insensitive medical terms
                .replace("prn", "as needed")
                .replace("po", "by mouth")
                .replace("bid", "twice daily")
                .replace("tid", "three times daily")
                .replace("qod", "every other day")
                .replace("hs", "at bedtime")
                .replace("stat", "immediately")
                .replaceAll("\\s+", " ") // Replace multiple spaces with a single space
                .trim();// Remove leading/trailing spaces;
    }
}