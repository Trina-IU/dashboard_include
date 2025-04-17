package com.example.med_sample.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.med_sample.data.model.Medication;

import java.util.List;

@Dao
public interface MedicationDao {
    @Query("SELECT * FROM medications")
    List<Medication> getAllMedications();

    @Query("SELECT * FROM medications WHERE name LIKE :query OR genericName LIKE :query")
    List<Medication> searchMedications(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedications(List<Medication> medications);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedication(Medication medication);

    @Update
    void updateMedication(Medication medication);

    @Query("SELECT * FROM medications WHERE isSynced = 0")
    List<Medication> getUnsyncedMedications();

    @Query("UPDATE medications SET isSynced = 1 WHERE id = :id")
    void markAsSynced(String id);

    @Query("SELECT COUNT(*) FROM medications")
    int getCount();

    @Query("SELECT * FROM medications WHERE id = :id")
    Medication getMedicationById(String id);
}