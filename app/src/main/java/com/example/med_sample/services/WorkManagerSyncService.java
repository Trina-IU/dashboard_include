package com.example.med_sample.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.med_sample.data.repository.MedicationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

public class WorkManagerSyncService extends Worker {
    private static final String TAG = "WorkManagerSyncService";
    private static final String PERIODIC_SYNC_WORK_NAME = "periodic_medication_sync";
    private static final String IMMEDIATE_SYNC_WORK_NAME = "immediate_medication_sync";

    // How often to sync (in hours)
    private static final int SYNC_INTERVAL_HOURS = 12;

    public WorkManagerSyncService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting medication sync work");

        // Only sync if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Sync skipped - user not logged in");
            return Result.success();
        }

        try {
            // Get repository instance and perform sync
            MedicationRepository repository = MedicationRepository.getInstance(getApplicationContext());

            // First sync from Firebase to local DB
            repository.syncFromFirebase();

            // Then sync local changes to Firebase
            repository.syncToFirebase();

            Log.d(TAG, "Medication sync completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }

    /**
     * Initialize the background sync service
     */
    public static void initialize(Context context) {
        // Set up constraints - require network connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create periodic work request
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                WorkManagerSyncService.class,
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build();

        // Enqueue the periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncWorkRequest);

        Log.d(TAG, "Periodic sync initialized with interval: " + SYNC_INTERVAL_HOURS + " hours");
    }

    /**
     * Request an immediate sync
     */
    public static void requestImmediateSync(Context context) {
        // Skip if user not logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Immediate sync request ignored - user not logged in");
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(WorkManagerSyncService.class)
                .setConstraints(constraints)
                .build();

        // Use ExistingWorkPolicy for OneTimeWorkRequest (fixing the error)
        WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest);

        Log.d(TAG, "Immediate sync requested");
    }
}