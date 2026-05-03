package com.example.seprojectpart3;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private String organizerUid;
    private final EventRepository eventRepo = new EventRepository();
    private final List<Map<String, Object>> organizerEvents = new ArrayList<>();

    private TextView tvPortalTitle, tvPortalSubtitle;
    private TextView navDashboard, navEvents, navProofs, navProfile;
    private TextView tvProfileInitial, tvProfileName, tvProfileEmail, tvProfileDetails;
    private EditText etProfileName, etProfilePassword;
    private ScrollView sectionDashboard, sectionEvents, sectionProofs, sectionProfile;
    private LinearLayout listDashboardStats, listDashboardEvents, listManagedEvents, listProofEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        organizerUid = getIntent().getStringExtra("uid");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if ((organizerUid == null || organizerUid.isEmpty()) && user != null) {
            organizerUid = user.getUid();
        }

        bindViews();
        bindActions();
        renderProfile(user);
        showSection("dashboard");
        loadOrganizerEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    private void bindViews() {
        tvPortalTitle = findViewById(R.id.tvPortalTitle);
        tvPortalSubtitle = findViewById(R.id.tvPortalSubtitle);

        sectionDashboard = findViewById(R.id.sectionDashboard);
        sectionEvents = findViewById(R.id.sectionEvents);
        sectionProofs = findViewById(R.id.sectionProofs);
        sectionProfile = findViewById(R.id.sectionProfile);

        listDashboardStats = findViewById(R.id.listDashboardStats);
        listDashboardEvents = findViewById(R.id.listDashboardEvents);
        listManagedEvents = findViewById(R.id.listManagedEvents);
        listProofEvents = findViewById(R.id.listProofEvents);

        navDashboard = findViewById(R.id.navDashboard);
        navEvents = findViewById(R.id.navEvents);
        navProofs = findViewById(R.id.navProofs);
        navProfile = findViewById(R.id.navProfile);

        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileDetails = findViewById(R.id.tvProfileDetails);
        etProfileName = findViewById(R.id.etProfileName);
        etProfilePassword = findViewById(R.id.etProfilePassword);
    }

    private void bindActions() {
        findViewById(R.id.btnCreateEvent).setOnClickListener(v -> openCreateEvent());
        findViewById(R.id.btnCreateEventLarge).setOnClickListener(v -> openCreateEvent());
        findViewById(R.id.btnAddEventManagement).setOnClickListener(v -> openCreateEvent());

        navDashboard.setOnClickListener(v -> showSection("dashboard"));
        navEvents.setOnClickListener(v -> showSection("events"));
        navProofs.setOnClickListener(v -> showSection("proofs"));
        navProfile.setOnClickListener(v -> showSection("profile"));

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
    }

    private void showSection(String section) {
        sectionDashboard.setVisibility("dashboard".equals(section) ? View.VISIBLE : View.GONE);
        sectionEvents.setVisibility("events".equals(section) ? View.VISIBLE : View.GONE);
        sectionProofs.setVisibility("proofs".equals(section) ? View.VISIBLE : View.GONE);
        sectionProfile.setVisibility("profile".equals(section) ? View.VISIBLE : View.GONE);

        if ("events".equals(section)) {
            tvPortalTitle.setText("Event Management");
            tvPortalSubtitle.setText("Select an existing event to edit.");
        } else if ("proofs".equals(section)) {
            tvPortalTitle.setText("Payment Verification");
            tvPortalSubtitle.setText("Review verified mock Paymo payments by event.");
        } else if ("profile".equals(section)) {
            tvPortalTitle.setText("Profile");
            tvPortalSubtitle.setText("Your account details");
        } else {
            tvPortalTitle.setText("Dashboard");
            tvPortalSubtitle.setText("Organizer Portal");
        }

        setNavActive(navDashboard, "dashboard".equals(section));
        setNavActive(navEvents, "events".equals(section));
        setNavActive(navProofs, "proofs".equals(section));
        setNavActive(navProfile, "profile".equals(section));
    }

    private void setNavActive(TextView nav, boolean active) {
        int iconColor = getColor(active ? R.color.text_primary : R.color.text_secondary);
        nav.setTextColor(iconColor);
        nav.setCompoundDrawableTintList(ColorStateList.valueOf(iconColor));
        nav.setBackgroundResource(active ? R.drawable.bg_nav_item_active : 0);
    }

    private void loadOrganizerEvents() {
        eventRepo.getOrganizerEvents(organizerUid, new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                organizerEvents.clear();
                organizerEvents.addAll(events);
                renderDashboard();
                renderEventManagement();
                renderPaymentVerification();
            }

            @Override public void onFailure(String error) {
                listDashboardEvents.removeAllViews();
                listDashboardEvents.addView(makeInfoCard(error));
                listManagedEvents.removeAllViews();
                listManagedEvents.addView(makeInfoCard(error));
                listProofEvents.removeAllViews();
                listProofEvents.addView(makeInfoCard(error));
            }
        });
    }

    private void renderDashboard() {
        listDashboardStats.removeAllViews();
        listDashboardEvents.removeAllViews();

        long totalSold = 0;
        long totalRsvps = 0;
        for (Map<String, Object> event : organizerEvents) {
            totalSold += longValue(event.get("ticketsSold"));
            totalRsvps += longValue(event.get("rsvpCount"));
        }

        listDashboardStats.addView(makeStatCard("Events", String.valueOf(organizerEvents.size())));
        listDashboardStats.addView(makeStatCard("Sold", String.valueOf(totalSold)));
        listDashboardStats.addView(makeStatCard("RSVPs", String.valueOf(totalRsvps)));

        if (organizerEvents.isEmpty()) {
            listDashboardEvents.addView(makeInfoCard("No events yet. Create your first event."));
            return;
        }

        for (Map<String, Object> event : organizerEvents) {
            listDashboardEvents.addView(makeEventCard(event, "Manage", () -> openEditEvent(event)));
        }
    }

    private void renderEventManagement() {
        listManagedEvents.removeAllViews();
        if (organizerEvents.isEmpty()) {
            listManagedEvents.addView(makeInfoCard("No events available for editing yet."));
            return;
        }

        for (Map<String, Object> event : organizerEvents) {
            listManagedEvents.addView(makeEventCard(event, "Edit", () -> openEditEvent(event)));
        }
    }

    private void renderPaymentVerification() {
        listProofEvents.removeAllViews();
        if (organizerEvents.isEmpty()) {
            listProofEvents.addView(makeInfoCard("Create an event before reviewing payments."));
            return;
        }

        for (Map<String, Object> event : organizerEvents) {
            listProofEvents.addView(makeEventCard(event, "Review Payments", () -> openProofs(event)));
        }
    }

    private LinearLayout makeStatCard(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.bg_card_dark);

        TextView title = label(label, 12, R.color.text_secondary, false);
        TextView number = label(value, 20, R.color.text_primary, true);
        number.setPadding(0, dp(4), 0, 0);
        card.addView(title);
        card.addView(number);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(params);
        return card;
    }

    private View makeEventCard(Map<String, Object> event, String action, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_event_card);
        card.setClickable(true);
        card.setFocusable(true);

        card.addView(makeBannerThumb(event));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        textCol.addView(label(value(event, "title"), 15, R.color.text_primary, true));
        textCol.addView(label(value(event, "date") + "  |  " + value(event, "venue"),
                12, R.color.text_secondary, false));
        textCol.addView(label("RSVPs " + longValue(event.get("rsvpCount"))
                        + "  |  Sold " + longValue(event.get("ticketsSold")),
                11, R.color.hint_text, false));
        textCol.addView(label("Status: " + statusLabel(value(event, "status")),
                11, statusColor(value(event, "status")), true));
        card.addView(textCol);

        TextView actionView = label(action, 11, R.color.accent_lime, true);
        actionView.setGravity(android.view.Gravity.CENTER);
        actionView.setBackgroundResource(R.drawable.bg_chip_success);
        actionView.setPadding(dp(10), dp(7), dp(10), dp(7));
        card.addView(actionView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        card.setOnClickListener(v -> onClick.run());
        return card;
    }

    private View makeBannerThumb(Map<String, Object> event) {
        FrameLayout thumb = new FrameLayout(this);
        thumb.setBackgroundResource(R.drawable.bg_icon_button);
        thumb.setClipToOutline(true);

        String bannerUrl = value(event, "bannerImageUrl");
        if (hasBanner(bannerUrl)) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            Glide.with(this).load(bannerUrl).centerCrop().into(image);
        } else {
            TextView initial = label(initial(value(event, "title")), 18, R.color.accent_lime, true);
            initial.setGravity(android.view.Gravity.CENTER);
            thumb.addView(initial, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), dp(58));
        params.setMargins(0, 0, dp(12), 0);
        thumb.setLayoutParams(params);
        return thumb;
    }

    private TextView makeInfoCard(String message) {
        TextView card = label(message, 13, R.color.text_primary, false);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        return card;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setFontFeatureSettings("kern");
        view.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return view;
    }

    private void openCreateEvent() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("organizerUid", organizerUid);
        startActivity(intent);
    }

    private void openEditEvent(Map<String, Object> event) {
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra("eventId", value(event, "eventId"));
        intent.putExtra("organizerUid", organizerUid);
        startActivity(intent);
    }

    private void openProofs(Map<String, Object> event) {
        Intent intent = new Intent(this, PaymentTransactionsActivity.class);
        intent.putExtra(PaymentTransactionsActivity.EXTRA_EVENT_ID, value(event, "eventId"));
        intent.putExtra(PaymentTransactionsActivity.EXTRA_EVENT_TITLE, value(event, "title"));
        startActivity(intent);
    }

    private void renderProfile(FirebaseUser user) {
        String email = user == null || user.getEmail() == null ? "organizer@email.com" : user.getEmail();
        String name = user == null || user.getDisplayName() == null || user.getDisplayName().isEmpty()
                ? "Organizer" : user.getDisplayName();

        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        tvProfileInitial.setText(initial(name));
        etProfileName.setText(name);
        tvProfileDetails.setText("Full Name\n" + name + "\n\nEmail\n" + email + "\n\nRole\nOrganizer");
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String updatedName = etProfileName.getText().toString().trim();
        String updatedPassword = etProfilePassword.getText().toString().trim();

        if (updatedName.isEmpty()) return;

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(updatedName)
                .build();

        user.updateProfile(request)
                .addOnSuccessListener(v -> {
                    tvProfileName.setText(updatedName);
                    tvProfileInitial.setText(initial(updatedName));
                    if (updatedPassword.isEmpty()) {
                        openOrganizerSuccess("Profile updated", "Your organizer profile was saved successfully.");
                    }
                })
                .addOnFailureListener(e -> {
                    tvProfileDetails.setVisibility(View.VISIBLE);
                    tvProfileDetails.setText(e.getMessage() == null ? "Unable to update profile." : e.getMessage());
                });

        if (!updatedPassword.isEmpty()) {
            if (updatedPassword.length() < 6) {
                tvProfileDetails.setVisibility(View.VISIBLE);
                tvProfileDetails.setText("Password must be at least 6 characters.");
                return;
            }
            user.updatePassword(updatedPassword)
                    .addOnSuccessListener(v -> {
                        etProfilePassword.setText("");
                        openOrganizerSuccess("Profile updated", "Your organizer profile was saved successfully.");
                    })
                    .addOnFailureListener(e -> {
                        tvProfileDetails.setVisibility(View.VISIBLE);
                        tvProfileDetails.setText("Password update failed. Please log in again and retry.");
                    });
        }
    }

    private void openOrganizerSuccess(String title, String message) {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("target", "organizer");
        intent.putExtra("uid", organizerUid);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        startActivity(intent);
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private boolean hasBanner(String url) {
        return url != null && !url.trim().isEmpty() && !"-".equals(url) && !"null".equals(url);
    }

    private String statusLabel(String status) {
        if ("published".equals(status) || "active".equals(status)) return "Published";
        if ("sold_out".equals(status)) return "Sold out";
        if ("cancelled".equals(status)) return "Cancelled";
        if ("draft".equals(status)) return "Draft";
        return status == null || status.equals("-") ? "Draft" : status;
    }

    private int statusColor(String status) {
        if ("cancelled".equals(status)) return R.color.error_red;
        if ("sold_out".equals(status)) return R.color.warning_gold;
        if ("draft".equals(status)) return R.color.text_secondary;
        return R.color.accent_lime;
    }

    private String initial(String text) {
        if (text == null || text.trim().isEmpty() || "-".equals(text)) return "O";
        return text.trim().substring(0, 1).toUpperCase(java.util.Locale.US);
    }

    private long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? 0 : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
