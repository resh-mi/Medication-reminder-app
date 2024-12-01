package com.example.medicationreminder;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AddMedicationActivity extends AppCompatActivity {

    private EditText medicationName, medicationDosage;
    private TextView selectedTimeTextView;
    private Button saveMedicationButton, pickTimeButton;
    private int selectedHour = -1, selectedMinute = -1;
    private DatabaseReference databaseReference;

    private static final String TAG = "AddMedicationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        // Check notification state
        SharedPreferences preferences = getSharedPreferences("MedicationPrefs", MODE_PRIVATE);
        boolean notificationsEnabled = preferences.getBoolean("enable_notifications", false);

        if (!notificationsEnabled) {
            Toast.makeText(this, "Notifications are disabled. Please enable them in settings.", Toast.LENGTH_LONG).show();
        }

        // Initialize Views
        medicationName = findViewById(R.id.medicationName);
        medicationDosage = findViewById(R.id.medicationDosage);
        selectedTimeTextView = findViewById(R.id.selectedTimeTextView);
        saveMedicationButton = findViewById(R.id.saveMedicationButton);
        pickTimeButton = findViewById(R.id.pickTimeButton);

        // Firebase Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("medications");

        // Time Picker Button
        pickTimeButton.setOnClickListener(v -> openTimePickerDialog());

        // Save Button Listener
        saveMedicationButton.setOnClickListener(v -> saveMedication());
    }

    private void openTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute1;

                    // Format time in 24-hour format
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    selectedTimeTextView.setText("Selected Time: " + time);
                    Log.d(TAG, "Time selected: " + time);
                },
                hour,
                minute,
                true); // Use 24-hour format
        timePickerDialog.show();
    }


    private void saveMedication() {
        String name = medicationName.getText().toString().trim();
        String dosage = medicationDosage.getText().toString().trim();

        if (!name.isEmpty() && !dosage.isEmpty() && selectedHour != -1 && selectedMinute != -1) {
            // Format time in 24-hour format
            String time = String.format("%02d:%02d", selectedHour, selectedMinute);

            // Save to Firebase
            String medicationId = databaseReference.push().getKey();
            Medication medication = new Medication(name, dosage, time);
            Log.d(TAG, "Saving Medication: " + medication);
            databaseReference.child(medicationId).setValue(medication)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Medication Added!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error adding medication.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please fill all fields and select a time!", Toast.LENGTH_SHORT).show();
        }
    }

}
