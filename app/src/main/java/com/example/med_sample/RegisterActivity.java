package com.example.med_sample;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText inputName, inputEmail, inputPassword, confirmPassword;
    private DatePickerDialog datePickerDialog;
    private Button dateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initDatePicker();
        dateButton = findViewById(R.id.dateButton);
        dateButton.setText(getTodaysDate());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputName = findViewById(R.id.inputUsername);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        confirmPassword = findViewById(R.id.ConfirmPassword);
        CheckBox showPassword = findViewById(R.id.showPassword);

        showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                inputPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                confirmPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });

        View registerBtn = findViewById(R.id.btnLogin);
        registerBtn.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirmedPassword = confirmPassword.getText().toString().trim();

            if (!password.equals(confirmedPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isStrongPassword(password)) {
                Toast.makeText(this, "Password must be at least 8 characters long, include uppercase, lowercase, a number, and a special character.", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = mAuth.getCurrentUser().getUid();
                                user.sendEmailVerification()
                                        .addOnCompleteListener(emailTask -> {
                                            if (emailTask.isSuccessful()) {
                                                Toast.makeText(this, "Verification email sent. Please verify your email.", Toast.LENGTH_LONG).show();

                                                Intent intent = new Intent(this, LoginActivity.class);
                                                startActivity(intent);
                                                finish();

                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("name", name);
                                                userData.put("email", email);
                                                userData.put("password", password);
                                                userData.put("userId", user.getUid());
                                                userData.put("dateOfBirth", dateButton.getText().toString());

                                                db.collection("users").document(userId).set(userData)
                                                        .addOnSuccessListener(unused -> {
                                                            mAuth.signOut();
                                                            startActivity(new Intent(this, LoginActivity.class));
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {;
                                                            Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Toast.makeText(this, "Failed to send verification email: " + emailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        View loginBtn = findViewById(R.id.tAlreadyHaveAnAccount);
        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private String getTodaysDate(){
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        month = month + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return makeDateString(day, month, year);
    }

    private int calculateAge(String dateOfBirth) {
        String[] parts = dateOfBirth.split(" ");
        int day = Integer.parseInt(parts[1]);
        int month = getMonthNumber(parts[0]);
        int year = Integer.parseInt(parts[2]);

        Calendar dob = Calendar.getInstance();
        dob.set(year, month - 1, day);

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    private int getMonthNumber(String month) {
        switch (month) {
            case "Jan": return 1;
            case "Feb": return 2;
            case "Mar": return 3;
            case "Apr": return 4;
            case "May": return 5;
            case "Jun": return 6;
            case "Jul": return 7;
            case "Aug": return 8;
            case "Sep": return 9;
            case "Oct": return 10;
            case "Nov": return 11;
            case "Dec": return 12;
            default: return 1;
        }
    }

    private void initDatePicker(){
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month + 1;
                String date = makeDateString(dayOfMonth, month, year);
                dateButton.setText(date);
            }
        };
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int style = AlertDialog.THEME_HOLO_LIGHT;

        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);

    }

    private String makeDateString(int day, int month, int year) {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month){
        switch(month){
            case 1:
                return "Jan";
            case 2:
                return "Feb";
            case 3:
                return "Mar";
            case 4:
                return "Apr";
            case 5:
                return "May";
            case 6:
                return "Jun";
            case 7:
                return "Jul";
            case 8:
                return "Aug";
            case 9:
                return "Sep";
            case 10:
                return "Oct";
            case 11:
                return "Nov";
            case 12:
                return "Dec";
            default:
                return "Jan";
        }
    }

    public void openDatePicker(View view) {
        datePickerDialog.show();
    }

    private boolean isStrongPassword(String password) {
        // Password must be at least 8 characters long, include uppercase, lowercase, a number, and a special character
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}