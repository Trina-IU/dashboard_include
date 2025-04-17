package com.example.med_sample.utils;

import android.content.Context;
import android.util.Log;

import com.example.med_sample.data.model.Medication;
import com.example.med_sample.data.repository.MedicationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CountDownLatch;

public class MedicationTextExtractor {
    private static final String TAG = "MedicationExtractor";

    // Common medication units
    private static final String[] UNITS = {
            "mg", "ml", "g", "mcg", "μg", "IU", "tablet", "tablets", "capsule", "capsules",
            "tab", "tabs", "cap", "caps", "pill", "pills"
    };

    // Common frequency terms
    private static final String[] TIME_INDICATORS = {
            "daily", "weekly", "monthly", "hourly", "once", "twice", "three times", "qd",
            "bid", "tid", "qid", "morning", "noon", "evening", "night", "bedtime",
            "before meals", "after meals", "with food", "prn", "as needed", "q4h", "q6h",
            "q8h", "q12h", "every", "hours", "days"
    };

    private final MedicationRepository repository;
    private final Context context;

    public MedicationTextExtractor(Context context) {
        this.context = context;
        this.repository = MedicationRepository.getInstance(context);
    }

    /**
     * Process OCR text to extract potential medications
     */
    public void processMedicationText(String ocrText, MedicationExtractionListener listener) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            listener.onMedicationsExtracted(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Processing OCR text: " + ocrText);

        // Extract potential medication names first
        List<String> potentialMedNames = extractPotentialMedicationNames(ocrText);
        if (potentialMedNames.isEmpty()) {
            Log.d(TAG, "No potential medication names found");
            listener.onMedicationsExtracted(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Found potential medications: " + potentialMedNames);

        // Extract dosage and frequency information
        Map<String, String> dosageInfo = extractDosageInfo(ocrText);
        Map<String, String> frequencyInfo = extractFrequencyInfo(ocrText);

        List<MedicationInfo> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(potentialMedNames.size());

        for (String medName : potentialMedNames) {
            validateMedication(medName, dosageInfo, frequencyInfo, results, latch);
        }

        // Process results in a separate thread to not block UI
        new Thread(() -> {
            try {
                // Wait for all validations to complete or timeout after 5 seconds
                latch.await(5000, java.util.concurrent.TimeUnit.MILLISECONDS);

                // Return results on main thread
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> listener.onMedicationsExtracted(results));

            } catch (InterruptedException e) {
                Log.e(TAG, "Medication validation interrupted", e);
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> listener.onMedicationsExtracted(results));
            }
        }).start();
    }

    /**
     * Extract potential medication names from OCR text
     */
    private List<String> extractPotentialMedicationNames(String text) {
        List<String> names = new ArrayList<>();

        // Normalize text
        text = text.replaceAll("\\s+", " ").trim();

        // Known medication names patterns:
        // - Words with capital letters (often medication brands)
        // - Words followed by dosage units
        // - Common medication suffixes

        // First, check for words with capital letters
        Pattern capitalizedPattern = Pattern.compile("\\b[A-Z][a-z]{3,}\\b");
        Matcher matcher = capitalizedPattern.matcher(text);
        while (matcher.find()) {
            String potentialMed = matcher.group();
            if (!names.contains(potentialMed)) {
                names.add(potentialMed);
            }
        }

        // Check for words before dosage units
        for (String unit : UNITS) {
            Pattern dosagePattern = Pattern.compile("\\b([A-Za-z]{4,})\\s+\\d+\\s*" + unit + "\\b",
                    Pattern.CASE_INSENSITIVE);
            matcher = dosagePattern.matcher(text);
            while (matcher.find()) {
                String potentialMed = matcher.group(1);
                if (!names.contains(potentialMed)) {
                    names.add(potentialMed);
                }
            }
        }

        // Check for common medication names
        String[] commonMeds = {"amoxicillin", "ibuprofen", "acetaminophen", "lisinopril",
                "metformin", "atorvastatin", "levothyroxine", "metoprolol"};
        for (String med : commonMeds) {
            Pattern commonPattern = Pattern.compile("\\b" + med + "\\b", Pattern.CASE_INSENSITIVE);
            matcher = commonPattern.matcher(text);
            if (matcher.find() && !names.contains(med)) {
                names.add(med);
            }
        }

        return names;
    }

    /**
     * Extract dosage information from OCR text
     */
    private Map<String, String> extractDosageInfo(String text) {
        Map<String, String> dosageMap = new HashMap<>();
        text = text.toLowerCase();

        // Look for patterns like "X mg" or "X tablet"
        for (String unit : UNITS) {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*" + unit);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String dosage = matcher.group();

                // Try to find the closest medication name before this dosage
                String beforeText = text.substring(0, matcher.start());
                String[] words = beforeText.split("\\s+");

                if (words.length > 0) {
                    // Look for closest word that could be a medication name
                    for (int i = words.length - 1; i >= 0; i--) {
                        String word = words[i].replaceAll("[^a-zA-Z]", "");
                        if (word.length() >= 4) {
                            dosageMap.put(word, dosage);
                            break;
                        }
                    }
                }
            }
        }

        return dosageMap;
    }

    /**
     * Extract frequency information from OCR text
     */
    private Map<String, String> extractFrequencyInfo(String text) {
        Map<String, String> frequencyMap = new HashMap<>();
        text = text.toLowerCase();

        for (String timeIndicator : TIME_INDICATORS) {
            Pattern pattern = Pattern.compile("\\b" + timeIndicator +
                    "\\b([a-z\\s]{0,20}(meals|food|water|day|night))?");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String frequency = matcher.group();

                // Extract context (20 characters before)
                int startPoint = Math.max(0, matcher.start() - 30);
                String context = text.substring(startPoint, matcher.start());
                String[] words = context.split("\\s+");

                if (words.length > 0) {
                    // Try to find a medication name in the context
                    for (int i = words.length - 1; i >= 0; i--) {
                        String word = words[i].replaceAll("[^a-zA-Z]", "");
                        if (word.length() >= 4) {
                            frequencyMap.put(word, frequency);
                            break;
                        }
                    }
                }
            }
        }

        return frequencyMap;
    }

    /**
     * Validate medication against database
     */
    private void validateMedication(String medName, Map<String, String> dosageInfo,
                                    Map<String, String> frequencyInfo,
                                    List<MedicationInfo> results, CountDownLatch latch) {

        repository.searchMedications(medName, new MedicationRepository.MedicationCallback() {
            @Override
            public void onMedicationsLoaded(List<Medication> medications) {
                if (!medications.isEmpty()) {
                    Medication validMed = medications.get(0);

                    // Get dosage and frequency for this medication
                    String dosage = findBestMatch(medName, dosageInfo);
                    String frequency = findBestMatch(medName, frequencyInfo);

                    // Create medication info object with extracted data
                    MedicationInfo info = new MedicationInfo(
                            validMed.getName(),
                            dosage,
                            frequency
                    );

                    // Add to results
                    synchronized(results) {
                        results.add(info);
                    }

                    Log.d(TAG, "Validated medication: " + validMed.getName());
                } else {
                    // No validation from database, use as-is if we have dosage/frequency
                    String dosage = findBestMatch(medName, dosageInfo);
                    String frequency = findBestMatch(medName, frequencyInfo);

                    if (!dosage.equals("Unknown dosage") || !frequency.equals("Unknown frequency")) {
                        MedicationInfo info = new MedicationInfo(
                                capitalizeFirstLetter(medName),
                                dosage,
                                frequency
                        );

                        synchronized(results) {
                            results.add(info);
                        }

                        Log.d(TAG, "Added unvalidated medication with dosage/frequency: " + medName);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error validating medication: " + e.getMessage());
                latch.countDown();
            }
        });
    }

    private String findBestMatch(String medName, Map<String, String> infoMap) {
        // Try exact match first
        if (infoMap.containsKey(medName.toLowerCase())) {
            return infoMap.get(medName.toLowerCase());
        }

        // Try partial matches
        for (String key : infoMap.keySet()) {
            if (key.toLowerCase().contains(medName.toLowerCase()) ||
                    medName.toLowerCase().contains(key.toLowerCase())) {
                return infoMap.get(key);
            }
        }

        return medName.contains("dosage") ? "Unknown dosage" : "Unknown frequency";
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Class to hold extracted medication information
     */
    public static class MedicationInfo {
        private String name;
        private String extractedDosage;
        private String extractedFrequency;
        private String genericName;
        private String standardDosage;

        public MedicationInfo(String name, String extractedDosage, String extractedFrequency) {
            this.name = name;
            this.extractedDosage = extractedDosage;
            this.extractedFrequency = extractedFrequency;
            this.genericName = "";
            this.standardDosage = "";
        }

        // Getters
        public String getName() { return name; }
        public String getExtractedDosage() { return extractedDosage; }
        public String getExtractedFrequency() { return extractedFrequency; }
        public String getGenericName() { return genericName; }
        public String getStandardDosage() { return standardDosage; }

        // Setters
        public void setGenericName(String genericName) { this.genericName = genericName; }
        public void setStandardDosage(String standardDosage) { this.standardDosage = standardDosage; }

        @Override
        public String toString() {
            return "Medicine: " + name +
                    "\nGeneric Name: " + genericName +
                    "\nDosage: " + extractedDosage +
                    "\nStandard Dosage: " + standardDosage +
                    "\nFrequency: " + extractedFrequency;
        }
    }

    /**
     * Listener for medication extraction results
     */
    public interface MedicationExtractionListener {
        void onMedicationsExtracted(List<MedicationInfo> medications);
    }
}