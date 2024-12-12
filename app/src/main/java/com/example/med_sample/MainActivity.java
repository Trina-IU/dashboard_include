package com.example.med_sample;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MainActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}