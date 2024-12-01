package com.example.medicationreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView medicationRecyclerView;
    private MedicationAdapter medicationAdapter;
    private ArrayList<Medication> medicationList;
    private DatabaseReference databaseReference;
    private Button addMedicationButton, clearRemindersButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request exact alarm permission for Android 12+
        requestExactAlarmPermission();

        // Initialize Views
        medicationRecyclerView = findViewById(R.id.recyclerView);
        addMedicationButton = findViewById(R.id.addMedicationButton);
        clearRemindersButton = findViewById(R.id.clearRemindersButton);

        // Initialize RecyclerView
        medicationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicationList = new ArrayList<>();
        medicationAdapter = new MedicationAdapter(this, medicationList);
        medicationRecyclerView.setAdapter(medicationAdapter);

        // Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("medications");

        // Load Medications from Firebase
        loadMedications();

        // Button Click Listeners
        addMedicationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMedicationActivity.class);
            startActivity(intent);
        });

        clearRemindersButton.setOnClickListener(v -> clearAllReminders());
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void loadMedications() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                medicationList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Medication medication = dataSnapshot.getValue(Medication.class);
                    if (medication != null) {
                        medicationList.add(medication);

                        // Schedule reminders
                        try {
                            String[] timeParts = medication.getTime().split(":");
                            int hour = Integer.parseInt(timeParts[0]);
                            int minute = Integer.parseInt(timeParts[1]);
                            scheduleReminder(medication.getName(), hour, minute);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing time: " + medication.getTime(), e);
                        }
                    }
                }
                medicationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading medications: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Failed to load medications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scheduleReminder(String medicationName, int hour, int minute) {
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        intent.putExtra("medicationName", medicationName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, medicationName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Reminder scheduled for " + medicationName + " at " + hour + ":" + minute);
            } catch (SecurityException e) {
                Log.e(TAG, "Exact alarm permission not granted", e);
                Toast.makeText(this, "Please enable exact alarm permission in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void clearAllReminders() {
        // Clear all alarms
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            for (Medication medication : medicationList) {
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, medication.getName().hashCode(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                alarmManager.cancel(pendingIntent);
            }
        }

        // Clear Firebase Database
        databaseReference.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "All reminders cleared!", Toast.LENGTH_SHORT).show();
                        medicationList.clear();
                        medicationAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to clear reminders.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
