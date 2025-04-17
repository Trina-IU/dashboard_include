package com.example.med_sample.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.med_sample.data.model.Medication;

@Database(entities = {Medication.class}, version = 2, exportSchema = false)
public abstract class MedicationDatabase extends RoomDatabase {

    private static volatile MedicationDatabase INSTANCE;

    public abstract MedicationDao medicationDao();

    public static MedicationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MedicationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MedicationDatabase.class, "medication_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}