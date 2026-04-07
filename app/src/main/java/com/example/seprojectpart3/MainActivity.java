package com.example.seprojectpart3;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.util.Calendar;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etName;
    TextView tvResult;
    RegistrationRepository regRepo;
    AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        tvResult = findViewById(R.id.tvResult);

        regRepo = new RegistrationRepository();
        authRepo = new AuthRepository();

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();

            regRepo.registerUser(email, password, name, "campus_user",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            tvResult.setText("Registered! UID: " + uid);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            authRepo.loginUser(email, password,
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(String token, String uid) {
                            tvResult.setText("Logged in! UID: " + uid);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        OrganizerRepository orgRepo = new OrganizerRepository();

        findViewById(R.id.btnOrganizer).setOnClickListener(v -> {
            String uid = "NQpNQiGInQhcA9AMsLeTfZjUo053"; // the UID you just got
            orgRepo.registerOrganizer(uid, "LUMS CS Society",
                    "cssociety@lums.edu.pk", "Computer Science Society",
                    new OrganizerRepository.OrganizerCallback() {
                        @Override
                        public void onSuccess(String id) {
                            tvResult.setText("Organizer registered!");
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        OtpManager otpManager = new OtpManager();
        EditText etOtp = findViewById(R.id.etOtp);

        findViewById(R.id.btnSendOtp).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            otpManager.sendOtp(email, new OtpManager.OtpCallback() {
                @Override
                public void onSuccess() {
                    tvResult.setText("OTP sent to " + email + "! Check your inbox.");
                }
                @Override
                public void onFailure(String error) {
                    tvResult.setText("Error: " + error);
                }
            });
        });

        findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String otp = etOtp.getText().toString().trim();
            otpManager.verifyOtp(email, otp, new OtpManager.OtpCallback() {
                @Override
                public void onSuccess() {
                    tvResult.setText("OTP verified! Password reset can proceed.");
                }
                @Override
                public void onFailure(String error) {
                    tvResult.setText("Error: " + error);
                }
            });
        });

        // Sprint 2 — M2 stories
        EventRepository eventRepo = new EventRepository();
        final String[] currentEventId = {""};
        final String[] selectedDate = {""};
        final String[] selectedSalesStart = {""};
        final String[] selectedSalesEnd = {""};

        EditText etEventTitle = findViewById(R.id.etEventTitle);
        EditText etEventDesc = findViewById(R.id.etEventDesc);

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
                "Bahria University - Islamabad",
                "Hajvery University - Lahore",
                "Virtual University - Lahore",
                "Szabist University - Karachi" ,
                "Gift University - Gujranwala" ,
                "Iqra University - Karachi" ,
                "Indus Valley - Karachi" ,
                "National College Of Arts - Lahore"
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

// Sales start date picker
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

// Sales end date picker
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

// Story #10 — Create event
        findViewById(R.id.btnCreateEvent).setOnClickListener(v -> {
            String title = etEventTitle.getText().toString().trim();
            String desc = etEventDesc.getText().toString().trim();
            String venue = spinnerVenue.getSelectedItem().toString();
            String organizerUid = "NQpNQiGInQhcA9AMsLeTfZjUo053";

            if (selectedDate[0].isEmpty()) {
                tvResult.setText("Please pick an event date!");
                return;
            }

            eventRepo.createEvent(organizerUid, title, desc, selectedDate[0], venue,
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

// Story #13 — Set sales timing
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
                            tvResult.setText("Sales timing set for event: " + eventId);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

// Story #14 — View attendees
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