package com.example.med_sample.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.med_sample.NotificationActivity;
import com.example.med_sample.R;

public class headerFragment extends Fragment {

    public headerFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_header, container, false);

        // Set up click listener for the notification icon
        View notificationIcon = view.findViewById(R.id.notification);
        notificationIcon.setOnClickListener(v -> {
            // Navigate to the NotificationActivity
            Intent intent = new Intent(getActivity(), NotificationActivity.class);
            startActivity(intent);
        });

        return view;
    }
}