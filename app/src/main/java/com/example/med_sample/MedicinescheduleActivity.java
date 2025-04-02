package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MedicinescheduleActivity extends AppCompatActivity {
    CalendarView calendarView;

    @Override
    protected void onCreate(Bundle SavedInstanceState) {
        super.onCreate(SavedInstanceState);
        setContentView(R.layout.activity_medicineschedule);
        calendarView = findViewById(R.id.calendarView);

        BottomNavigationView bottomNa = findViewById(R.id.bottom_navigation);
        bottomNa.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        // Set up the back button click listener
        View backButton = findViewById(R.id.header_layout_schedule_back);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                String selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            }

        });
    }

    //to handle permissions of the navigation bar in one code
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        BottomNavigationUtil.handlePermissionsResult(requestCode, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BottomNavigationUtil.handleActivityResult(requestCode, resultCode, data, this);
    }
}

