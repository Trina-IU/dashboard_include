package com.example.med_sample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TermsconditionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termscondition);
        Button accept = (Button) findViewById(R.id.accept_btn);
        accept.setOnClickListener(v -> {;
            Intent intent = (new Intent(TermsconditionActivity.this, LoginActivity.class));
            startActivity(intent);
        });
    }
}

