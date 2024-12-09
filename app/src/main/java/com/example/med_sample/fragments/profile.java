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
import com.example.med_sample.R;

public class profile extends Fragment {

    public profile() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // this is the headerFragment
        Fragment headerFragment = new headerFragment();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.header_container, headerFragment)
                .commit();

        // Handle click listener for history LinearLayout
        View historyLayout = view.findViewById(R.id.linearLayout_history);
        historyLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        });


        return view;
    }
}