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
import com.example.med_sample.MedicinescheduleActivity;
import com.example.med_sample.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class profile extends Fragment {

    public profile() {
    }
    private EditText userNameEditText;
    private EditText userEmailEditText;
    private EditText userAgeEditText;
    private EditText userPasswordEditText;
    private Button editButton, saveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Dynamically add headerFragment( the header!)
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

        setEditable(false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();


            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userNameEditText.setText(documentSnapshot.getString("name"));
                            userEmailEditText.setText(documentSnapshot.getString("email"));
                            userAgeEditText.setText(documentSnapshot.getString("age"));
                            userPasswordEditText.setText(documentSnapshot.getString("password"));
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
            });

            // Save button
            saveButton.setOnClickListener(v -> {
                String updatedName = userNameEditText.getText().toString().trim();
                String updatedEmail = userEmailEditText.getText().toString().trim();
                String updatedAge = userAgeEditText.getText().toString().trim();
                String updatedPassword = userPasswordEditText.getText().toString().trim();

                db.collection("users").document(userId)
                        .update("name", updatedName,
                                "email", updatedEmail,
                                "age", updatedAge,
                                "password", updatedPassword)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            setEditable(false);
                            saveButton.setVisibility(View.GONE);
                            editButton.setVisibility(View.VISIBLE);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        return view;
    }
    private void setEditable(boolean isEditable) {
        userNameEditText.setEnabled(isEditable);
        userEmailEditText.setEnabled(isEditable);
        userAgeEditText.setEnabled(isEditable);
        userPasswordEditText.setEnabled(isEditable);
    }
}