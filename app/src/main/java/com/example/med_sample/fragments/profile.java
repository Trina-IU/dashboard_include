package com.example.med_sample.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.HistoryActivity;
import com.example.med_sample.MedicinescheduleActivity;
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
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");

                            userNameTextView.setText(name);
                            userEmailTextView.setText(email);
                        } else {
                            Toast.makeText(getContext(), "No user data found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error retrieving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    private void setEditable(boolean isEditable) {
        userNameEditText.setEnabled(isEditable);
        userEmailEditText.setEnabled(isEditable);
        userAgeEditText.setEnabled(isEditable);
        userPasswordEditText.setEnabled(isEditable);
    }
}

