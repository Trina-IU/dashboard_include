package com.example.med_sample.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medications")
public class Medication {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String genericName;
    private String dosageForms;
    private String standardDosages;
    private String instructions;
    private String contraindications;
    private long lastUpdated;
    private boolean isSynced;

    public Medication() {
        // Required empty constructor for Firebase
    }

    public Medication(@NonNull String id, String name, String genericName, String dosageForms,
                      String standardDosages, String instructions, String contraindications) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.dosageForms = dosageForms;
        this.standardDosages = standardDosages;
        this.instructions = instructions;
        this.contraindications = contraindications;
        this.lastUpdated = System.currentTimeMillis();
        this.isSynced = false;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDosageForms() { return dosageForms; }
    public void setDosageForms(String dosageForms) { this.dosageForms = dosageForms; }

    public String getStandardDosages() { return standardDosages; }
    public void setStandardDosages(String standardDosages) { this.standardDosages = standardDosages; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getContraindications() { return contraindications; }
    public void setContraindications(String contraindications) { this.contraindications = contraindications; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}