package com.example.medicationreminder;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ReminderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        TextView reminderTextView = findViewById(R.id.reminderTextView);

        String medicationName = getIntent().getStringExtra("medicationName");
        if (medicationName == null) {
            medicationName = "Medication Reminder";
        }

        reminderTextView.setText("It's time to take: " + medicationName);
    }
}
