package com.example.med_sample;

import android.app.Application;

import com.example.med_sample.data.repository.MedicationRepository;
import com.example.med_sample.services.WorkManagerSyncService;
import com.example.med_sample.utils.NetworkUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Initialize repository
        MedicationRepository.getInstance(this);

        // Set up background sync
        WorkManagerSyncService.initialize(this);

        NetworkUtils.registerNetworkCallback(this);

        // Always try to sync when app starts if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            WorkManagerSyncService.requestImmediateSync(this);
        }
    }
}