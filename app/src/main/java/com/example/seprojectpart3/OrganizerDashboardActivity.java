package com.example.seprojectpart3;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Calendar;

public class OrganizerDashboardActivity extends AppCompatActivity {

    TextView tvResult;
    final String[] currentEventId = {""};
    final String[] selectedDate = {""};
    final String[] selectedSalesStart = {""};
    final String[] selectedSalesEnd = {""};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        tvResult = findViewById(R.id.tvDashboardResult);
        EditText etEventTitle = findViewById(R.id.etEventTitle);
        EditText etEventDesc = findViewById(R.id.etEventDesc);

        // Get organizer uid passed from login
        String organizerUid = getIntent().getStringExtra("uid");

        EventRepository eventRepo = new EventRepository();

        // University dropdown
        Spinner spinnerVenue = findViewById(R.id.spinnerVenue);
        String[] universities = {
                "LUMS - Lahore",
                "FAST NUCES - Lahore",
                "FAST NUCES - Islamabad",
                "FAST NUCES - Karachi",
                "NUST - Islamabad",
                "UET - Lahore",
                "UET - Peshawar",
                "COMSATS - Islamabad",
                "COMSATS - Lahore",
                "Aga Khan University - Karachi",
                "IBA - Karachi",
                "GIK Institute - Topi",
                "ITU - Lahore",
                "UMT - Lahore",
                "Bahria University - Islamabad"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                universities
        );
        spinnerVenue.setAdapter(adapter);

        // Event date picker
        findViewById(R.id.btnPickDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate[0] = year + "-" +
                        String.format("%02d", month + 1) + "-" +
                        String.format("%02d", day);
                ((Button) findViewById(R.id.btnPickDate))
                        .setText("Event Date: " + selectedDate[0]);
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Sales start picker
        findViewById(R.id.btnPickSalesStart).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedSalesStart[0] = year + "-" +
                        String.format("%02d", month + 1) + "-" +
                        String.format("%02d", day);
                ((Button) findViewById(R.id.btnPickSalesStart))
                        .setText("Sales Start: " + selectedSalesStart[0]);
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Sales end picker
        findViewById(R.id.btnPickSalesEnd).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedSalesEnd[0] = year + "-" +
                        String.format("%02d", month + 1) + "-" +
                        String.format("%02d", day);
                ((Button) findViewById(R.id.btnPickSalesEnd))
                        .setText("Sales End: " + selectedSalesEnd[0]);
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Create event
        findViewById(R.id.btnCreateEvent).setOnClickListener(v -> {
            String title = etEventTitle.getText().toString().trim();
            String desc = etEventDesc.getText().toString().trim();
            String venue = spinnerVenue.getSelectedItem().toString();

            if (selectedDate[0].isEmpty()) {
                tvResult.setText("Please pick an event date!");
                return;
            }

            eventRepo.createEvent(organizerUid, title, desc,
                    selectedDate[0], venue,
                    new EventRepository.EventCallback() {
                        @Override
                        public void onSuccess(String eventId) {
                            currentEventId[0] = eventId;
                            tvResult.setText("Event created! ID: " + eventId);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // Set sales timing
        findViewById(R.id.btnSetSales).setOnClickListener(v -> {
            if (currentEventId[0].isEmpty()) {
                tvResult.setText("Create an event first!");
                return;
            }
            if (selectedSalesStart[0].isEmpty() || selectedSalesEnd[0].isEmpty()) {
                tvResult.setText("Please pick both sales dates!");
                return;
            }
            eventRepo.setSalesTime(currentEventId[0],
                    selectedSalesStart[0], selectedSalesEnd[0],
                    new EventRepository.EventCallback() {
                        @Override
                        public void onSuccess(String eventId) {
                            tvResult.setText("Sales timing set!");
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // View attendees
        findViewById(R.id.btnViewAttendees).setOnClickListener(v -> {
            if (currentEventId[0].isEmpty()) {
                tvResult.setText("Create an event first!");
                return;
            }
            eventRepo.getAttendees(currentEventId[0],
                    new EventRepository.AttendeesCallback() {
                        @Override
                        public void onSuccess(String attendeeList) {
                            tvResult.setText(attendeeList);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });
    }
}