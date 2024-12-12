package com.example.med_sample.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.HistoryActivity;
import com.example.med_sample.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class profile extends Fragment {

    public profile() {
    }
    private TextView userNameTextView;
    private TextView userEmailTextView;

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

        userNameTextView = view.findViewById(R.id.name_profile);
        userEmailTextView = view.findViewById(R.id.name_email);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Handle click listener for history LinearLayout
            View historyLayout = view.findViewById(R.id.linearLayout_history);
            historyLayout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), HistoryActivity.class);
                startActivity(intent);
            });

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
}
