package com.example.med_sample;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        String medicine = getIntent().getStringExtra("medicine");

        // Display the reminder details
        TextView reminderTextView = findViewById(R.id.reminder_text_view);
        reminderTextView.setText("Time to take your medicine: " + medicine);

        // Set up the back button click listener
        LinearLayout notification = findViewById(R.id.header_layout_nofication_back);
        notification.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationActivity.this, Dashboard_main.class);
            intent.putExtra("navigate_to", "home");
            startActivity(intent);
            finish();
        });
    }

    private void scheduleReminder(String medicine, String time) {
        // Parse time
        String[] timeParts = time.split(" ");
        String[] hourMinute = timeParts[0].split(":");
        int hour = Integer.parseInt(hourMinute[0]);
        int minute = Integer.parseInt(hourMinute[1]);
        boolean isPM = timeParts[1].equalsIgnoreCase("PM");
        if (isPM && hour != 12) hour += 12;

        // Set up calendar
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Set up AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra("medicine", medicine);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

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