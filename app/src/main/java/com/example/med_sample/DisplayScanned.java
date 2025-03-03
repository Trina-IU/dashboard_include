package com.example.med_sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;

public class DisplayScanned extends AppCompatActivity {

    private TextView resultTextView;
    private Bitmap capturedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_scanned);

        // Initialize UI elements
        resultTextView = findViewById(R.id.result_text);
        capturedImage = getIntent().getParcelableExtra("capturedImage");

        if (capturedImage == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Process image with ML Kit
        recognizeText();
    }

    private void recognizeText() {
        // Create InputImage from Bitmap
        InputImage image = InputImage.fromBitmap(capturedImage, 0);

        // Initialize TextRecognizer
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Process the image
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        String recognizedText = visionText.getText();
                        resultTextView.setText(recognizedText);

                        // Save to Firebase
                        saveToFirebase(capturedImage, recognizedText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        resultTextView.setText("Error: " + e.getMessage());
                    }
                });
    }

    private void saveToFirebase(Bitmap image, String text) {
        // Convert Bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        // Upload image to Firebase Storage
        String filename = "scans/" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(filename);

        storageRef.putBytes(imageData)
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.storage.UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.storage.UploadTask.TaskSnapshot taskSnapshot) {
                        // Get download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveHistoryToDatabase(imageUrl, text);
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DisplayScanned.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveHistoryToDatabase(String imageUrl, String text) {
        // Save scan history to Firebase Realtime Database
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("scans");
        String scanId = dbRef.push().getKey();

        ScanHistory scan = new ScanHistory(scanId, imageUrl, text, System.currentTimeMillis());
        dbRef.child(scanId).setValue(scan)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(DisplayScanned.this, "Scan saved successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DisplayScanned.this, "Failed to save scan", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Model class for Firebase
    private static class ScanHistory {
        public String id;
        public String imageUrl;
        public String text;
        public long timestamp;

        public ScanHistory() {} // Required for Firebase

        public ScanHistory(String id, String imageUrl, String text, long timestamp) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}