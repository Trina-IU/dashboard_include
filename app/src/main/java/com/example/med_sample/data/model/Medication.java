package com.example.med_sample.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "medications")
public class Medication {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String dosage;
    private String frequency;
    private String startDate;
    private String endDate;
    private String instructions;
    private boolean taken;
    private String userId;
    private long timestamp;
    private boolean isSynced;
    private String genericName;
    private String standardDosage;

    // Empty constructor required for Room
    public Medication() {
    }

    @Ignore
    public Medication(String id, String name, String dosage, String frequency,
                      String startDate, String endDate, String instructions,
                      boolean taken, String userId) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.taken = taken;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.isSynced = false;
    }

    @Ignore
    public Medication(String id, String name, String genericName, String dosage,
                      String standardDosage, String instructions, String contraindications) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.dosage = dosage;
        this.standardDosage = standardDosage;
        this.instructions = instructions;
        this.timestamp = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isTaken() { return taken; }
    public void setTaken(boolean taken) { this.taken = taken; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getStandardDosage() { return standardDosage; }
    public void setStandardDosage(String standardDosage) { this.standardDosage = standardDosage; }

    public void setLastUpdated(long lastUpdated) {
        this.timestamp = lastUpdated;
    }

    // Convert to HashMap for Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("dosage", dosage);
        result.put("frequency", frequency);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("instructions", instructions);
        result.put("taken", taken);
        result.put("userId", userId);
        result.put("timestamp", timestamp);
        result.put("genericName", genericName);
        result.put("standardDosage", standardDosage);
        return result;
    }
}