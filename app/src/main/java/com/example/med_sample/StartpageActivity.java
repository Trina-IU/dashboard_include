package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class StartpageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startpage);

        ConstraintLayout layout = findViewById(R.id.root_layout);
        layout.setOnClickListener(v -> {
            Intent intent = new Intent(StartpageActivity.this, TermsconditionActivity.class);
            startActivity(intent);
        });
    }
}

