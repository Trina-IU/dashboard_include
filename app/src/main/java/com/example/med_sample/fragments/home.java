package com.example.med_sample.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.med_sample.HistoryActivity;
import com.example.med_sample.MedicinescheduleActivity;
import com.example.med_sample.R;

public class home extends Fragment {

    public home() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // this is where the headerFragment is
        Fragment headerFragment = new headerFragment();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.header_container, headerFragment)
                .commit();

        // Click listener for historyacitiviy
        View historyLayout = view.findViewById(R.id.linearLayout_homehistory);
        historyLayout.setOnClickListener(v -> {
            // Start HistoryActivity
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        });

        // Click listener for MedicineCalendarActivity
        View scheduleLayout = view.findViewById(R.id.linearLayout_homeschedule);
        scheduleLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MedicinescheduleActivity.class);
            startActivity(intent);
        });

        return view;
    }
}