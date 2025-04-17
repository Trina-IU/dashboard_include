package com.example.med_sample.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.med_sample.data.database.MedicationDatabase;
import com.example.med_sample.data.model.Medication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicationRepository {
    private static final String TAG = "MedicationRepository";

    private final MedicationDatabase database;
    private final DatabaseReference remoteDb;
    private final Context context;
    private final String userId;
    private final ExecutorService executor;
    private final Handler mainHandler;

    // Singleton instance
    private static MedicationRepository INSTANCE;

    public static MedicationRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MedicationRepository(context);
        }
        return INSTANCE;
    }

    private MedicationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = MedicationDatabase.getDatabase(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Initialize Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = user != null ? user.getUid() : "";
        this.remoteDb = FirebaseDatabase.getInstance().getReference("medications");

        // Initial sync from Firebase to SQLite if we have a logged-in user
        if (!userId.isEmpty() && isNetworkAvailable()) {
            syncFromFirebase();
        }
    }

    // Search medications - first check local, then remote if needed
    public void searchMedications(String query, MedicationCallback callback) {
        executor.execute(() -> {
            // Format the query for SQL LIKE
            String formattedQuery = "%" + query + "%";

            // First try local database
            List<Medication> localResults = database.medicationDao().searchMedications(formattedQuery);

            if (!localResults.isEmpty()) {
                // We have local results
                mainHandler.post(() -> callback.onMedicationsLoaded(localResults));
            } else {
                // If no local results, try Firebase (when online)
                mainHandler.post(() -> {
                    if (isNetworkAvailable()) {
                        searchFirebase(query, callback);
                    } else {
                        callback.onMedicationsLoaded(new ArrayList<>());
                    }
                });
            }
        });
    }

    // Search in Firebase
    private void searchFirebase(String query, MedicationCallback callback) {
        String formattedQuery = query.toLowerCase();

        remoteDb.orderByChild("name").startAt(formattedQuery).endAt(formattedQuery + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Medication> medications = new ArrayList<>();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Medication medication = dataSnapshot.getValue(Medication.class);
                            if (medication != null) {
                                medications.add(medication);
                                // Save to local database
                                executor.execute(() -> {
                                    medication.setSynced(true);
                                    database.medicationDao().insertMedication(medication);
                                });
                            }
                        }

                        callback.onMedicationsLoaded(medications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase search failed", error.toException());
                        callback.onError(error.toException());
                    }
                });
    }

    // Sync all available medications from Firebase to local SQLite
    public void syncFromFirebase() {
        remoteDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executor.execute(() -> {
                    List<Medication> medications = new ArrayList<>();

                    for (DataSnapshot medicationSnapshot : snapshot.getChildren()) {
                        Medication medication = medicationSnapshot.getValue(Medication.class);
                        if (medication != null) {
                            medication.setSynced(true);
                            medications.add(medication);
                        }
                    }

                    // Insert all medications to local database
                    if (!medications.isEmpty()) {
                        database.medicationDao().insertMedications(medications);
                        Log.d(TAG, "Synced " + medications.size() + " medications from Firebase");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase sync failed", error.toException());
            }
        });
    }

    // Upload any local changes to Firebase
    public void syncToFirebase() {
        if (!isNetworkAvailable()) {
            return;
        }

        executor.execute(() -> {
            List<Medication> unsyncedMedications = database.medicationDao().getUnsyncedMedications();

            for (Medication medication : unsyncedMedications) {
                remoteDb.child(medication.getId()).setValue(medication)
                        .addOnSuccessListener(aVoid -> {
                            // Mark as synced in local DB
                            executor.execute(() -> database.medicationDao().markAsSynced(medication.getId()));
                            Log.d(TAG, "Medication synced to Firebase: " + medication.getName());
                        })
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to sync medication to Firebase: " + e.getMessage())
                        );
            }
        });
    }

    // Add a new medication (to both local and Firebase if connected)
    public void addMedication(Medication medication, MedicationOperationCallback callback) {
        if (medication.getId() == null || medication.getId().isEmpty()) {
            medication.setId(UUID.randomUUID().toString());
        }

        medication.setLastUpdated(System.currentTimeMillis());

        // First add to local database
        executor.execute(() -> {
            database.medicationDao().insertMedication(medication);

            // Then try to sync to Firebase if online
            if (isNetworkAvailable()) {
                remoteDb.child(medication.getId()).setValue(medication)
                        .addOnSuccessListener(aVoid -> {
                            medication.setSynced(true);
                            database.medicationDao().updateMedication(medication);
                            mainHandler.post(() -> callback.onSuccess());
                        })
                        .addOnFailureListener(e -> {
                            mainHandler.post(() -> callback.onError(e));
                        });
            } else {
                // Successfully added locally, but not synced
                mainHandler.post(() -> callback.onSuccess());
            }
        });
    }

    // Get a medication by ID
    public void getMedicationById(String medicationId, MedicationItemCallback callback) {
        executor.execute(() -> {
            Medication medication = database.medicationDao().getMedicationById(medicationId);

            if (medication != null) {
                mainHandler.post(() -> callback.onMedicationLoaded(medication));
            } else if (isNetworkAvailable()) {
                // Try from Firebase
                remoteDb.child(medicationId).get()
                        .addOnSuccessListener(dataSnapshot -> {
                            Medication remoteMed = dataSnapshot.getValue(Medication.class);
                            if (remoteMed != null) {
                                executor.execute(() -> database.medicationDao().insertMedication(remoteMed));
                                mainHandler.post(() -> callback.onMedicationLoaded(remoteMed));
                            } else {
                                mainHandler.post(() -> callback.onMedicationNotFound());
                            }
                        })
                        .addOnFailureListener(e -> mainHandler.post(() -> callback.onError(e)));
            } else {
                mainHandler.post(() -> callback.onMedicationNotFound());
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Callbacks
    public interface MedicationCallback {
        void onMedicationsLoaded(List<Medication> medications);
        void onError(Exception e);
    }

    public interface MedicationItemCallback {
        void onMedicationLoaded(Medication medication);
        void onMedicationNotFound();
        void onError(Exception e);
    }

    public interface MedicationOperationCallback {
        void onSuccess();
        void onError(Exception e);
    }
}