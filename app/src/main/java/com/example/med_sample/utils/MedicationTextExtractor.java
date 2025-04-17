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

public class MedicationTextExtractor {
    private static final String TAG = "MedicationExtractor";

    // Commonly used measurement units in prescriptions
    private static final String[] UNITS = {"mg", "ml", "g", "mcg", "IU", "tablet", "capsule", "tab", "cap"};

    // Commonly used time indicators
    private static final String[] TIME_INDICATORS = {"daily", "weekly", "monthly", "hourly", "once", "twice", "qd",
            "bid", "tid", "qid", "morning", "evening", "night", "bedtime", "before meals", "after meals"};

    private final MedicationRepository repository;

    public MedicationTextExtractor(Context context) {
        this.repository = MedicationRepository.getInstance(context);
    }

    /**
     * Process OCR text to extract potential medications
     */
    public void processMedicationText(String ocrText, MedicationExtractionListener listener) {
        // Extract potential medication names
        List<String> potentialMedNames = extractPotentialMedicationNames(ocrText);

        // Extract dosage information
        Map<String, String> dosageInfo = extractDosageInfo(ocrText);

        // Extract frequency information
        Map<String, String> frequencyInfo = extractFrequencyInfo(ocrText);

        // Create result map
        Map<String, MedicationInfo> extractedMedications = new HashMap<>();

        // Validate each potential med name against database
        for (String medName : potentialMedNames) {
            validateMedicationName(medName, new MedicationRepository.MedicationCallback() {
                @Override
                public void onMedicationsLoaded(List<Medication> medications) {
                    if (!medications.isEmpty()) {
                        Medication validMed = medications.get(0);

                        // Create medication info object
                        MedicationInfo info = new MedicationInfo(
                                validMed.getName(),
                                validMed.getGenericName(),
                                dosageInfo.getOrDefault(validMed.getName().toLowerCase(), "Unknown dosage"),
                                frequencyInfo.getOrDefault(validMed.getName().toLowerCase(), "Unknown frequency"),
                                validMed.getStandardDosages()
                        );

                        extractedMedications.put(validMed.getName(), info);

                        // If we've checked all medications, notify listener
                        if (extractedMedications.size() == potentialMedNames.size()) {
                            listener.onMedicationsExtracted(new ArrayList<>(extractedMedications.values()));
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error validating medication: " + e.getMessage());
                }
            });
        }

        // If no medications were found
        if (potentialMedNames.isEmpty()) {
            listener.onMedicationsExtracted(new ArrayList<>());
        }
    }

    /**
     * Extract potential medication names from OCR text
     */
    private List<String> extractPotentialMedicationNames(String text) {
        List<String> names = new ArrayList<>();

        // Normalize text (remove extra spaces, convert to lowercase)
        text = text.replaceAll("\\s+", " ").toLowerCase();

        // Simple algorithm: words starting with uppercase are potential medication names
        String[] words = text.split("\\s+");
        for (String word : words) {
            // Clean the word (remove punctuation)
            word = word.replaceAll("[^a-zA-Z]", "");

            // Only consider words with 4+ characters
            if (word.length() >= 4) {
                names.add(word);
            }
        }

        return names;
    }

    /**
     * Extract dosage information from OCR text
     */
    private Map<String, String> extractDosageInfo(String text) {
        Map<String, String> dosageMap = new HashMap<>();

        // Look for patterns like "X mg" or "X tablet"
        for (String unit : UNITS) {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*" + unit);
            Matcher matcher = pattern.matcher(text.toLowerCase());

            while (matcher.find()) {
                // Try to find the associated medicine name (simple approach: check words before)
                String beforeText = text.substring(0, matcher.start()).toLowerCase();
                String[] words = beforeText.split("\\s+");

                if (words.length > 0) {
                    String potentialMed = words[words.length - 1];
                    if (potentialMed.length() >= 4) {
                        dosageMap.put(potentialMed, matcher.group());
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

        for (String timeIndicator : TIME_INDICATORS) {
            if (text.toLowerCase().contains(timeIndicator)) {
                // Simple approach: find closest medication name
                int index = text.toLowerCase().indexOf(timeIndicator);
                String beforeText = text.substring(0, index).toLowerCase();
                String[] words = beforeText.split("\\s+");

                if (words.length > 0) {
                    String potentialMed = words[words.length - 1];
                    if (potentialMed.length() >= 4) {
                        frequencyMap.put(potentialMed, timeIndicator);
                    }
                }
            }
        }

        return frequencyMap;
    }

    /**
     * Validate medication name against database
     */
    private void validateMedicationName(String medName, MedicationRepository.MedicationCallback callback) {
        repository.searchMedications(medName, callback);
    }

    /**
     * Class to hold extracted medication information
     */
    public static class MedicationInfo {
        private String name;
        private String genericName;
        private String extractedDosage;
        private String extractedFrequency;
        private String standardDosage;

        public MedicationInfo(String name, String genericName, String extractedDosage,
                              String extractedFrequency, String standardDosage) {
            this.name = name;
            this.genericName = genericName;
            this.extractedDosage = extractedDosage;
            this.extractedFrequency = extractedFrequency;
            this.standardDosage = standardDosage;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getGenericName() { return genericName; }
        public void setGenericName(String genericName) { this.genericName = genericName; }

        public String getExtractedDosage() { return extractedDosage; }
        public void setExtractedDosage(String extractedDosage) { this.extractedDosage = extractedDosage; }

        public String getExtractedFrequency() { return extractedFrequency; }
        public void setExtractedFrequency(String extractedFrequency) { this.extractedFrequency = extractedFrequency; }

        public String getStandardDosage() { return standardDosage; }
        public void setStandardDosage(String standardDosage) { this.standardDosage = standardDosage; }

        @Override
        public String toString() {
            return "Medicine: " + name +
                    "\nGeneric: " + genericName +
                    "\nDosage: " + extractedDosage +
                    "\nFrequency: " + extractedFrequency +
                    "\nStandard Dosage: " + standardDosage;
        }
    }

    /**
     * Listener for medication extraction results
     */
    public interface MedicationExtractionListener {
        void onMedicationsExtracted(List<MedicationInfo> medications);
    }
}