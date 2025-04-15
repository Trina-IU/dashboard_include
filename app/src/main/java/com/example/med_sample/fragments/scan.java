package com.example.med_sample.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.med_sample.DisplayScanned;
import com.example.med_sample.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class scan extends Fragment {
    // Load OpenCV library
    static {
        System.loadLibrary("opencv_java4");
    }

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    // UI elements for previewing the OCR result in this fragment.
    private ImageView previewImageView;
    private TextView ocrResultTextView;
    private Button confirmCaptureButton, captureButton;

    // Hold recognized text and image
    private String recognizedText = "";
    private Bitmap capturedBitmap = null;
    private boolean isAnalyzing = true;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                        File imageFile = createImageFile();
                        saveBitmapToFile(bitmap, imageFile);
                        processImageFile(imageFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed");
        }
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        previewView = view.findViewById(R.id.previewView);
        previewImageView = view.findViewById(R.id.previewImageView);
        ocrResultTextView = view.findViewById(R.id.ocrResultTextView);
        confirmCaptureButton = view.findViewById(R.id.confirmCaptureButton);
        captureButton = view.findViewById(R.id.captureButton);
        confirmCaptureButton.setVisibility(View.GONE); // Initially hidden

        cameraExecutor = Executors.newSingleThreadExecutor();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set up button listeners
        captureButton.setOnClickListener(v -> takePhoto());
        view.findViewById(R.id.uploadButton).setOnClickListener(v -> openGallery());
        confirmCaptureButton.setOnClickListener(v -> navigateToDisplayScanned(recognizedText, capturedBitmap));

        startCamera();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        File storageDir = requireContext().getExternalCacheDir();
        return File.createTempFile("captured_image", ".jpg", storageDir);
    }

    private void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        }
    }

    private void processImageFile(File imageFile) {
        // Process the image and run OCR.
        recognizeText(imageFile, new TextRecognitionCallback() {
            @Override
            public void onTextRecognized(String text) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        recognizedText = text;
                        ocrResultTextView.setText(text);
                        capturedBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        previewImageView.setImageBitmap(capturedBitmap);
                        confirmCaptureButton.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "OCR Failed", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void takePhoto() {
        try {
            isAnalyzing = false; // Pause real-time analysis
            File outputFile = createImageFile();
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(outputFile).build();

            imageCapture.takePicture(options, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                    processImageFile(outputFile);
                }
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    exception.printStackTrace();
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show();
                            isAnalyzing = true; // Resume real-time analysis
                        });
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            isAnalyzing = true; // Resume real-time analysis
        }
    }

    private void navigateToDisplayScanned(String extractedText, Bitmap bitmap) {
        Intent intent = new Intent(requireActivity(), DisplayScanned.class);
        intent.putExtra("extractedText", extractedText);
        intent.putExtra("capturedImage", bitmap);
        startActivity(intent);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up image capture use case
                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .build();

                // Set up image analysis use case for real-time text detection
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!isAnalyzing) {
                        imageProxy.close();
                        return;
                    }

                    try {
                        @SuppressWarnings("deprecation")
                        InputImage image = InputImage.fromMediaImage(
                                imageProxy.getImage(),
                                imageProxy.getImageInfo().getRotationDegrees()
                        );

                        textRecognizer.process(image)
                                .addOnSuccessListener(visionText -> {
                                    if (isAdded() && getActivity() != null && isAnalyzing) {
                                        String detectedText = visionText.getText();
                                        String processedText = postProcess(detectedText);

                                        requireActivity().runOnUiThread(() -> {
                                            if (isAnalyzing) {
                                                recognizedText = processedText;
                                                ocrResultTextView.setText(processedText);
                                            }
                                        });
                                    }
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TextRecognition", "Failed to process image", e);
                                    imageProxy.close();
                                });
                    } catch (Exception e) {
                        Log.e("TextRecognition", "Error analyzing image", e);
                        imageProxy.close();
                    }
                });

                // Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }

    private void recognizeText(File imageFile, TextRecognitionCallback callback) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            callback.onError(new Exception("Failed to decode bitmap"));
            return;
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        mat = autoRotate(mat, imageFile.getAbsolutePath());

        // Preprocess the image for OCR
        if (shouldPreprocess()) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);
            Imgproc.adaptiveThreshold(mat, mat, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 9, 2);
        }

        double targetWidth = 1600;
        double scale = Math.min(targetWidth / mat.cols(), 2.0);
        Size newSize = new Size(mat.cols() * scale, mat.rows() * scale);
        Imgproc.resize(mat, mat, newSize);

        Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, processedBitmap);
        mat.release();

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage image = InputImage.fromBitmap(processedBitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    String processedText = postProcess(rawText);
                    callback.onTextRecognized(processedText);
                })
                .addOnFailureListener(callback::onError);
    }

    interface TextRecognitionCallback {
        void onTextRecognized(String text);
        void onError(Exception e);
    }

    private Mat autoRotate(Mat mat, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mat;
    }

    private boolean shouldPreprocess() {
        return true;
    }

    private String postProcess(String rawText) {
        return rawText
                .replaceAll("([A-Za-z])\\1{2,}", "$1")
                .replace("0", "O")
                .replace("1", "l")
                .replace("5", "S")
                .replace("2", "Z")
                .replace("!", "I")
                .replaceAll("(?i)qid", "4 times a day")
                .replace("prn", "as needed")
                .replace("po", "by mouth")
                .replace("bid", "twice daily")
                .replace("tid", "three times daily")
                .replace("qod", "every other day")
                .replace("hs", "at bedtime")
                .replace("stat", "immediately")
                .replaceAll("\\s+", " ")
                .trim();
    }
}