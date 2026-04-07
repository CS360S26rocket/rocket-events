package com.example.seprojectpart3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etName;
    TextView tvResult;
    OrganizerRepository orgRepo;
    RegistrationRepository regRepo;
    AuthRepository authRepo;

    TicketRepository ticketRepo;
    CsvExportRepository csvRepo;

    private static final String TEST_EVENT_ID = "0iMpdj3AGXuyTbgfFWw7";

    private String lastUid = null;

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
        orgRepo = new OrganizerRepository();

        ticketRepo = new TicketRepository();
        csvRepo = new CsvExportRepository();

        // REGISTER
        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();

            regRepo.registerUser(email, password, name, "campus_user",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            lastUid = uid;
                            tvResult.setText("Registered! UID: " + uid);
                        }

                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // LOGIN
        View btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            authRepo.loginUser(email, password,
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(String token, String uid) {
                            lastUid = uid;
                            tvResult.setText("Logged in! UID: " + uid);
                        }

                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // ORGANIZER REGISTER
        // ORGANIZER REGISTER
        View btnOrganizer = findViewById(R.id.btnOrganizer);

        btnOrganizer.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                tvResult.setText("Error: Please fill in email, password and name.");
                return;
            }

            // Step 1: Register Auth account + save to users collection
            regRepo.registerUser(email, password, name, "organizer",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            lastUid = uid;

                            // Step 2: Save to organizers collection
                            orgRepo.registerOrganizer(uid, name, email, "Test description",
                                    new OrganizerRepository.OrganizerCallback() {
                                        @Override
                                        public void onSuccess(String id) {
                                            tvResult.setText("Organizer registered! UID: " + uid);
                                        }
                                        @Override
                                        public void onFailure(String error) {
                                            tvResult.setText("Error saving organizer: " + error);
                                        }
                                    });
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // -------------------------
        // SPRINT 2 TESTS (no new buttons needed)
        //
        // Long-press Organizer button:
        //   1) setTicketType(TEST_EVENT_ID, "general", 500, 1)
        //   2) registerAttendee as logged-in user -> EXPECT CONFIRMED
        //
        // Long-press Login button:
        //   registerAttendee as fake user -> EXPECT WAITLIST (if capacity=1 and already sold)
        //
        // Long-press Send OTP button:
        //   exportAttendeeCsv(TEST_EVENT_ID)
        // -------------------------

        // Long-press: setup ticket type + book seat 1 (CONFIRM expected)
        btnOrganizer.setOnLongClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                tvResult.setText("Sprint2: Login first, then long-press Organizer.");
                return true;
            }

            String uid = user.getUid();

            ticketRepo.setTicketType(
                    TEST_EVENT_ID,
                    "general",
                    500,
                    1,
                    new TicketRepository.TicketCallback() {
                        @Override
                        public void onSuccess(String message) {
                            ticketRepo.registerAttendee(
                                    TEST_EVENT_ID,
                                    uid,
                                    "Tester One",
                                    "tester1@lums.edu.pk",
                                    "general",
                                    new TicketRepository.RegistrationCallback() {
                                        @Override
                                        public void onConfirmed(String ticketId) {
                                            tvResult.setText("Sprint2: CONFIRMED ✅ ticketId=" + ticketId);
                                        }

                                        @Override
                                        public void onWaitlisted(String waitlistId) {
                                            tvResult.setText("Sprint2: WAITLISTED (unexpected) id=" + waitlistId);
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            tvResult.setText("Sprint2: FAILED ❌ " + error);
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Sprint2: setTicketType FAILED ❌ " + error);
                        }
                    }
            );

            return true;
        });

        // Long-press: book seat 2 as fake user (WAITLIST expected)
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            authRepo.loginUser(email, password,
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(String token, String uid) {
                            lastUid = uid;

                            // Check the user's role in Firestore
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        String role = doc.getString("role");
                                        if ("organizer".equals(role)) {
                                            android.content.Intent intent = new android.content.Intent(
                                                    MainActivity.this, OrganizerDashboardActivity.class
                                            );
                                            intent.putExtra("uid", uid);
                                            startActivity(intent);
                                        } else {
                                            tvResult.setText("Logged in as campus user! UID: " + uid);
                                            // TODO: navigate to your campus user screen here
                                        }
                                    })
                                    .addOnFailureListener(e -> tvResult.setText("Error fetching role: " + e.getMessage()));
                        }

                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        // OTP
        OtpManager otpManager = new OtpManager();
        EditText etOtp = findViewById(R.id.etOtp);

        View btnSendOtp = findViewById(R.id.btnSendOtp);
        btnSendOtp.setOnClickListener(v -> {
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

        // Long-press: export CSV for Sprint2 (#39)  ✅ FIXED CALLBACK TYPE
        btnSendOtp.setOnLongClickListener(v -> {
            csvRepo.exportAttendeeCsv(this, TEST_EVENT_ID, new CsvExportRepository.CsvCallback() {
                @Override
                public void onSuccess(String filePath, int rowCount) {
                    tvResult.setText("Sprint2: CSV ✅ " + filePath + " (" + rowCount + " rows)");
                }

                @Override
                public void onFailure(String err) {
                    tvResult.setText("Sprint2: CSV FAIL ❌ " + err);
                }
            });
            return true;
        });

        View btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnVerifyOtp.setOnClickListener(v -> {
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
    }
}