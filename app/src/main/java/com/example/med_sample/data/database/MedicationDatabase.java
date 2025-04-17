package com.example.med_sample.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.med_sample.data.model.Medication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Medication.class}, version = 1)
public abstract class MedicationDatabase extends RoomDatabase {
    public abstract MedicationDao medicationDao();

    private static volatile MedicationDatabase INSTANCE;
    private static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4);

    public static MedicationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MedicationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MedicationDatabase.class,
                                    "medication_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Callback to pre-populate database with common medications
    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseExecutor.execute(() -> {
                MedicationDao dao = INSTANCE.medicationDao();
                if (dao.getCount() == 0) { // Only populate if empty
                    List<Medication> medications = getCommonMedications();
                    dao.insertMedications(medications);
                }
            });
        }
    };

    private static List<Medication> getCommonMedications() {
        List<Medication> medications = new ArrayList<>();

        // Common medications
        medications.add(new Medication(
                UUID.randomUUID().toString(),
                "Amoxicillin",
                "Amoxicillin",
                "Capsules: 250mg, 500mg; Suspension: 125mg/5mL, 250mg/5mL",
                "Adults: 250-500mg every 8 hours; Children: 20-90mg/kg/day divided every 8 hours",
                "Take with or without food. Complete full course of treatment.",
                "Allergy to penicillins or cephalosporins"
        ));

        medications.add(new Medication(
                UUID.randomUUID().toString(),
                "Paracetamol",
                "Acetaminophen",
                "Tablets: 500mg, 650mg; Syrup: 120mg/5mL, 250mg/5mL",
                "Adults: 500-1000mg every 4-6 hours (max 4g/day); Children: 10-15mg/kg every 4-6 hours",
                "Take with or without food. Do not exceed recommended dose.",
                "Liver disease, alcoholism"
        ));

        medications.add(new Medication(
                UUID.randomUUID().toString(),
                "Metformin",
                "Metformin hydrochloride",
                "Tablets: 500mg, 850mg, 1000mg; Extended-release: 500mg, 750mg",
                "Initial: 500mg twice daily; Maintenance: 1000-2000mg daily in divided doses",
                "Take with meals to reduce gastrointestinal side effects",
                "Renal impairment, metabolic acidosis, dehydration"
        ));

        // Add more common medications as needed

        return medications;
    }
}