package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TermsconditionActivity extends AppCompatActivity {
    private ScrollView scrollView; // Declare as a class-level variable
    private Button acceptBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termscondition);

        // Initialize views
        Button declineBtn = findViewById(R.id.decline_btn);
        acceptBtn = findViewById(R.id.accept_btn);
        scrollView = findViewById(R.id.scrollView2); // Assign to the class-level variable

        // Decline button action
        declineBtn.setOnClickListener(v -> showDeclineMessage());


        // Disable accept button initially
        acceptBtn.setEnabled(false);
        acceptBtn.setAlpha(0.5f);

        // Scroll listener to check if user reached the bottom
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isScrollViewAtBottom()) {
                acceptBtn.setEnabled(true);
                acceptBtn.setAlpha(1f);
            }
        });

        // Accept button action
        acceptBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TermsconditionActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isScrollViewAtBottom() {
        if (scrollView.getChildCount() > 0) {
            int diff = scrollView.getChildAt(0).getBottom() - (scrollView.getHeight() + scrollView.getScrollY());
            return diff <= 0;
        }
        return false;
    }
    private void showDeclineMessage() {
        Toast.makeText(this, "You must accept the terms and conditions to continue.:>", Toast.LENGTH_SHORT).show();
    }
}
