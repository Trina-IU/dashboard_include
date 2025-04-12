package com.example.med_sample;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private DatabaseReference dbRef;
    private String userId;
    private List<String> historyList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        BottomNavigationView bottomNa = findViewById(R.id.bottom_navigation);
        bottomNa.setOnItemSelectedListener(BottomNavigationUtil.getNavListener(this));

        View backButton = findViewById(R.id.header_layout_history_back);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        historyListView = findViewById(R.id.history_list_view);
        historyList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        historyListView.setAdapter(adapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        loadHistory();
    }

    private void loadHistory() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MedicineSchedule.HistoryItem historyItem = dataSnapshot.getValue(MedicineSchedule.HistoryItem.class);
                    if (historyItem != null && historyItem.getText() != null) {
                        historyList.add(historyItem.getText());
                    } else {
                        historyList.add("Invalid entry");
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class MedicineSchedule {
        public String id;
        public String medicineName;
        public String intakeSchedule;
        public long timestamp;

        public MedicineSchedule() {
        }

        public MedicineSchedule(String id, String medicineName, String intakeSchedule, long timestamp) {
            this.id = id;
            this.medicineName = medicineName;
            this.intakeSchedule = intakeSchedule;
            this.timestamp = timestamp;
        }
        public static class HistoryItem {
            private String id;
            private String text;
            private long timestamp;

            public HistoryItem() {}

            public HistoryItem(String id, String text, long timestamp) {
                this.id = id;
                this.text = text;
                this.timestamp = timestamp;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public long getTimestamp() {
                return timestamp;
            }

            public void setTimestamp(long timestamp) {
                this.timestamp = timestamp;
            }
        }
    }
}