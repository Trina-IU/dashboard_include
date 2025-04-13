package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MedicinescheduleActivity extends AppCompatActivity {
    private CalendarView calendarView;
    private DatabaseReference dbRef;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicineschedule);

        ImageView backButton = findViewById(R.id.backtodashboard_btn1h);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        BottomNavigationView bottomNa = findViewById(R.id.bottom_navigation);
        bottomNa.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        calendarView = findViewById(R.id.calendarView);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("medicine_schedule");

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            loadMedicineSchedulesForDate(selectedDate);
        });
    }

    private void loadMedicineSchedulesForDate(String date) {
        dbRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> schedules = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String medicineName = dataSnapshot.child("medicineName").getValue(String.class);
                    String intakeSchedule = dataSnapshot.child("intakeSchedule").getValue(String.class);
                    if (medicineName != null && intakeSchedule != null) {
                        schedules.add(medicineName + " - " + intakeSchedule);
                    }
                }
                if (schedules.isEmpty()) {
                    Toast.makeText(MedicinescheduleActivity.this, "No schedules for " + date, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MedicinescheduleActivity.this, "Schedules: " + schedules, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MedicinescheduleActivity.this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
            }
        });
    }
}