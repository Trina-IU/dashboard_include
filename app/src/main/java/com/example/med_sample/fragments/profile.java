package com.example.med_sample.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.HistoryActivity;
import com.example.med_sample.LoginActivity;
import com.example.med_sample.MedicinescheduleActivity;
import com.example.med_sample.R;
import com.example.med_sample.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class profile extends Fragment {

    private EditText nameEditText, ageEditText, emailEditText, passwordEditText;
    private Button logoutButton, editButton, saveButton;
    private LinearLayout historyLayout, scheduleLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isEditMode = false;
    private boolean previousNetworkState = false;

    // Constants for SharedPreferences
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String KEY_NAME = "name";
    private static final String KEY_AGE = "age";
    private static final String KEY_PENDING_SYNC = "pending_sync";

    private NetworkReceiver networkReceiver;

    public profile() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize header fragment
        Fragment headerFragment = new headerFragment();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.header_container, headerFragment)
                .commit();

        // Initialize UI components
        nameEditText = view.findViewById(R.id.name_profile);
        ageEditText = view.findViewById(R.id.name_age);
        emailEditText = view.findViewById(R.id.name_email);
        passwordEditText = view.findViewById(R.id.name_password);

        logoutButton = view.findViewById(R.id.logout);
        editButton = view.findViewById(R.id.btn_edit);
        saveButton = view.findViewById(R.id.btn_save);

        historyLayout = view.findViewById(R.id.linearLayout_profilehistory);
        scheduleLayout = view.findViewById(R.id.linearLayout_profileschedule);

        // Set initial state of fields to non-editable
        setFieldsEditable(false);
        loadUserData();

        // Set up click listeners
        setupClickListeners();

        // Register network receiver
        networkReceiver = new NetworkReceiver();

        // Store initial network state
        previousNetworkState = isNetworkAvailable();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register network change receiver
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(networkReceiver, filter);

            // Check for pending sync when resuming fragment
            checkForPendingSync();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister network receiver
        if (getActivity() != null && networkReceiver != null) {
            try {
                getActivity().unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered exception
            }
        }
    }

    private void setupClickListeners() {
        // Logout button listener
        logoutButton.setOnClickListener(v -> logout());

        // Edit button listener
        editButton.setOnClickListener(v -> {
            isEditMode = true;
            setFieldsEditable(true);
            editButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.VISIBLE);
        });

        // Save button listener
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveUserData();
                isEditMode = false;
                setFieldsEditable(false);
                saveButton.setVisibility(View.GONE);
                editButton.setVisibility(View.VISIBLE);
            }
        });

        // History layout listener
        historyLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        });

        // Schedule layout listener
        scheduleLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MedicinescheduleActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Set email from Firebase Auth
            emailEditText.setText(user.getEmail());

            // First try to load from Firestore if online
            if (isNetworkAvailable()) {
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                String age = documentSnapshot.getString("age");

                                nameEditText.setText(name != null ? name : "");
                                ageEditText.setText(age != null ? age : "");

                                // Also save to local storage for offline access
                                saveToLocalStorage(name, age, false);
                            }
                        })
                        .addOnFailureListener(e -> {
                            showToastIfAttached("Failed to load from server. Trying local data.");
                            loadFromLocalStorage();
                        });
            } else {
                // Load from local storage if offline
                loadFromLocalStorage();
            }
        }
    }

    private void loadFromLocalStorage() {
        if (getActivity() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_NAME, "");
        String age = prefs.getString(KEY_AGE, "");

        nameEditText.setText(name);
        ageEditText.setText(age);
    }

    private void saveUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = nameEditText.getText().toString().trim();
            String age = ageEditText.getText().toString().trim();

            // Always save to local storage
            saveToLocalStorage(name, age, !isNetworkAvailable());

            // If online, update Firestore as well
            if (isNetworkAvailable()) {
                db.collection("users").document(user.getUid())
                        .update(
                                "name", name,
                                "age", age
                        )
                        .addOnSuccessListener(aVoid -> {
                            showToastIfAttached("Profile updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            showToastIfAttached("Failed to update profile online");
                        });
            } else {
                showToastIfAttached("No internet connection. Changes saved locally and will sync when online.");
            }
        }
    }

    private void saveToLocalStorage(String name, String age, boolean pendingSync) {
        if (getActivity() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_AGE, age);
        editor.putBoolean(KEY_PENDING_SYNC, pendingSync);
        editor.apply();
    }

    private void checkForPendingSync() {
        if (getActivity() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        boolean pendingSync = prefs.getBoolean(KEY_PENDING_SYNC, false);

        if (pendingSync && isNetworkAvailable()) {
            String name = prefs.getString(KEY_NAME, "");
            String age = prefs.getString(KEY_AGE, "");

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .update(
                                "name", name,
                                "age", age
                        )
                        .addOnSuccessListener(aVoid -> {
                            // Mark as synced
                            saveToLocalStorage(name, age, false);
                            showToastIfAttached("Profile changes synced to server");
                        })
                        .addOnFailureListener(e -> {
                            showToastIfAttached("Failed to sync profile changes");
                        });
            }
        }
    }

    // Method to safely show a Toast
    private void showToastIfAttached(String message) {
        if (getActivity() != null && isAdded()) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Check network connectivity
    private boolean isNetworkAvailable() {
        if (getActivity() == null) return false;
        return NetworkUtils.isNetworkAvailable(getActivity());
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError("Name cannot be empty");
            isValid = false;
        }

        if (ageEditText.getText().toString().trim().isEmpty()) {
            ageEditText.setError("Age cannot be empty");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageEditText.getText().toString().trim());
                if (age <= 0 || age > 120) {
                    ageEditText.setError("Please enter a valid age");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageEditText.setError("Please enter a valid number");
                isValid = false;
            }
        }

        return isValid;
    }

    private void setFieldsEditable(boolean editable) {
        nameEditText.setEnabled(editable);
        ageEditText.setEnabled(editable);
        // Email is not editable through the profile screen
        emailEditText.setEnabled(false);
        // Password field is shown but not editable here
        passwordEditText.setEnabled(false);
        passwordEditText.setTransformationMethod(new PasswordTransformationMethod());
        if (!editable) {
            passwordEditText.setText("********"); // Mask the password
        }
    }

    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear any saved credentials
        if (getActivity() != null) {
            SharedPreferences preferences = getActivity().getSharedPreferences("loginPrefs", 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }

        // Navigate to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    // Network change receiver
    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isConnected = isNetworkAvailable();

                // Check if connection state changed from disconnected to connected
                if (isConnected && !previousNetworkState) {
                    showToastIfAttached("Internet connection restored");

                    // Check for pending sync operations
                    checkForPendingSync();
                } else if (!isConnected && previousNetworkState) {
                    showToastIfAttached("Internet connection lost");
                }

                // Update previous state
                previousNetworkState = isConnected;
            }
        }
    }
}