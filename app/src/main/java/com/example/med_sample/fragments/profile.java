package com.example.med_sample.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.HistoryActivity;
import com.example.med_sample.LoginActivity;
import com.example.med_sample.MedicinescheduleActivity;
import com.example.med_sample.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class profile extends Fragment {

    public profile() {
    }

    private EditText userNameEditText;
    private EditText userEmailEditText;
    private EditText userAgeEditText;
    private EditText userPasswordEditText;
    private String realPassword;
    private Button editButton, saveButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Dynamically add headerFragment (the header)
        Fragment headerFragment = new headerFragment();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.header_container, headerFragment)
                .commit();

        userNameEditText = view.findViewById(R.id.name_profile);
        userEmailEditText = view.findViewById(R.id.name_email);
        userAgeEditText = view.findViewById(R.id.name_age);
        userPasswordEditText = view.findViewById(R.id.name_password);
        editButton = view.findViewById(R.id.btn_edit);
        saveButton = view.findViewById(R.id.btn_save);
        logoutButton = view.findViewById(R.id.logout);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        setEditable(false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return view;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userNameEditText.setText(documentSnapshot.getString("name"));
                        userEmailEditText.setText(documentSnapshot.getString("email"));

                        // Dynamically calculate age from dateOfBirth
                        String dateOfBirth = documentSnapshot.getString("dateOfBirth");
                        if (dateOfBirth != null) {
                            int age = calculateAge(dateOfBirth);
                            userAgeEditText.setText(String.valueOf(age));
                        } else {
                            userAgeEditText.setText("N/A");
                        }
                        realPassword = documentSnapshot.getString("password");
                        userPasswordEditText.setText("********");
                    } else {
                        Toast.makeText(getContext(), "No user data found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error retrieving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Handle click listener for history LinearLayout
        View historyLayout = view.findViewById(R.id.linearLayout_profilehistory);
        historyLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        });

        View scheduleLayout = view.findViewById(R.id.linearLayout_profileschedule);
        scheduleLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MedicinescheduleActivity.class);
            startActivity(intent);
        });

        editButton.setOnClickListener(v -> {
            setEditable(true);
            saveButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);

            userPasswordEditText.setText(realPassword);
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            String updatedName = userNameEditText.getText().toString().trim();
            String updatedEmail = userEmailEditText.getText().toString().trim();
            String updatedAge = userAgeEditText.getText().toString().trim();
            String updatedPassword = userPasswordEditText.getText().toString().trim();

            if (updatedPassword.equals("********")) {
                updatedPassword = realPassword;
            }

            final String finalUpdatedPassword = updatedPassword;

            db.collection("users").document(userId)
                    .update("name", updatedName,
                            "email", updatedEmail,
                            "age", updatedAge,
                            "password", finalUpdatedPassword)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        setEditable(false);
                        saveButton.setVisibility(View.GONE);
                        editButton.setVisibility(View.VISIBLE);
                        logoutButton.setVisibility(View.VISIBLE);

                        realPassword = finalUpdatedPassword;
                        userPasswordEditText.setText("********");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }

    private void setEditable(boolean isEditable) {
        userNameEditText.setEnabled(isEditable);
        userEmailEditText.setEnabled(isEditable);
        userAgeEditText.setEnabled(isEditable);
        userPasswordEditText.setEnabled(isEditable);
    }

    private int calculateAge(String dateOfBirth) {
        String[] parts = dateOfBirth.split(" ");
        int day = Integer.parseInt(parts[1]);
        int month = getMonthNumber(parts[0]);
        int year = Integer.parseInt(parts[2]);

        Calendar dob = Calendar.getInstance();
        dob.set(year, month - 1, day);

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    private int getMonthNumber(String month) {
        switch (month) {
            case "Jan": return 1;
            case "Feb": return 2;
            case "Mar": return 3;
            case "Apr": return 4;
            case "May": return 5;
            case "Jun": return 6;
            case "Jul": return 7;
            case "Aug": return 8;
            case "Sep": return 9;
            case "Oct": return 10;
            case "Nov": return 11;
            case "Dec": return 12;
            default: return 1;
        }
    }
}